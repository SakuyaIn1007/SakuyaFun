# 2026-09-14 工作总结：绕开 Cloudflare 的内容读取链路

本文件记录本次工作的完整过程：踩到的坑、根因、解决方式，以及最终实现的内容。

起点问题：**65+ 本书能列出目录，但点进去读不了，等很久后报 504。**

---

## 一、踩过的坑总览

| # | 坑 | 表面现象 | 真实根因 | 解决方式 |
|---|---|---|---|---|
| 1 | 端口占用误诊 | 报「安全关闭校验失败」而非端口冲突 | 端口上的旧进程会应答 `/health`，事后校验存活不可靠 | 改为**启动前** `lsof` 前置检查 |
| 2 | 开关失效 | `WENKU8_ADAPTER_ENABLED` 设了没反应 | Spring profile 配置优先级：`application-dev.yml` 覆盖 `application.yml` | 保留占位符 `${...}`，不写字面量 |
| 3 | 「能列书读不了」 | 目录正常，正文 503 | 内容存储用相对路径 `./content-store`，两种启动方式工作目录不同 | 脚本里钉成绝对路径 |
| 4 | 卡在 80% | 启动日志停在 `80% EXECUTING` | Gradle rich console 对 `bootRun` 无法细分进度 | `--console=plain` |
| 5 | Cloudflare 拦截 | 请求超时后 504 | 上游 `www.wenku8.net` 封禁（`Access denied` 6 次，非可交互质询页） | **改走 CDN 下载源**，完全不碰该域名 |
| 6 | 请求频率触发限流 | 连续 5 次成功后第 6 次被拒 | `_semaphore` 只限并发数，不限单位时间请求数 | 新增 `UpstreamThrottle`（间隔 + 冷却） |
| 7 | 切分算法方向错误 | 只切出 127/198 章，漏 36% | 「封闭标记集合」无法穷举自由形式的章节标题 | 改为**两阶段卷名发现** |
| 8 | CRLF 换行 | 抽样 3 本**全部**降级为单章 | 上游 TXT 换行符不统一，`\r` 残留使正则 `$` 失配 | 按 `\r?\n` 切分并剥离 `\r` |
| 9 | 异步竞态 | 回退后仍 404 | `backfill` 是 `@Async`，触发后立刻读库必然读不到 | 同步返回内容 + 异步回填 |
| 10 | 细节：全角空格 | 缩进正文被误判为章节标题 | Java 正则 `\S` 只认 ASCII 空白，U+3000/U+00A0 不算 | 显式字符类排除 |
| 11 | 内存累积 | 无（审查时发现） | 单例上的缓存 Map 永不清理，每本 5MB | 容量 4 的 LRU + 同步 |
| 12 | 环境：subagent 不可用 | 派发全部失败 | `CLAUDE_CODE_SUBAGENT_MODEL` 指向无效模型 | 改用会话内执行 |

---

## 二、坑的详细分析与解决

### 坑 1：端口占用被误诊为「安全关闭校验失败」

**现象**：重启脚本时报「已确认 Wenku8 上游保持关闭」校验失败，而非提示端口被占。

**根因**：我最初写的是**事后**检查（启动后 `kill -0` 看进程是否还活着）。但端口上的旧进程应答 `/health` 极快，轮询循环第一次就 `break` 了，此时新的 uvicorn 还没被系统回收，`kill -0` 仍然成功 → 误判为自己启动成功。

**解决**：改为**前置** `lsof` 检查，端口被占直接退出并打印占用者 PID：

```bash
if lsof -nP -iTCP:"${adapter_port}" -sTCP:LISTEN >/dev/null 2>&1; then
  occupied="$(lsof ... | awk '{print $1" (PID "$2")"}' | paste -sd', ' -)"
  echo "端口 ${adapter_port} 已被占用，无法启动新的适配器：${occupied}" >&2
  exit 1
fi
```

**验证**：确实拦住，输出 `端口 8000 已被占用：Python (PID 52880)`。

### 坑 2：环境变量开关形同虚设

**现象**：文档和脚本都宣传 `WENKU8_ADAPTER_ENABLED` 可控制开关，实际设了没反应。

**根因**：Spring 的 **profile 专属配置优先于非 profile 配置**。`application-dev.yml` 里写的是字面量 `enabled: true`，它覆盖了 `application.yml` 里的 `${WENKU8_ADAPTER_ENABLED:false}`，占位符永远不生效。

**解决**：profile 配置里也必须保留占位符：

```yaml
wenku8:
  adapter:
    enabled: ${WENKU8_ADAPTER_ENABLED:true}
```

### 坑 3：能列书但读不了正文（相对路径陷阱）

**现象**：目录能列出来，点进去正文 503。

**排查过程**（走了两步才定位）：
1. 先发现后端短路成 503（`AdapterClient` 的 `!properties.enabled()`）
2. 修好后错误信息从「未启用」变成「**尚**未启用」—— 文案多了一个「尚」字，说明请求真的打到了 Python 适配器。这是关键线索。
3. 查数据库发现根因与适配器无关：`CONTENT_STORAGE_LOCAL_DIR` 默认值 `./content-store` 是**相对路径**，跟随**进程工作目录**解析。IDEA 启动时工作目录是仓库根，脚本经 `gradlew -p backend` 启动时是 `backend/`，两者读到不同目录。

**解决**：脚本里钉成绝对路径：

```bash
export CONTENT_STORAGE_LOCAL_DIR="${CONTENT_STORAGE_LOCAL_DIR:-$repo_root/content-store}"
```

**验证**：`/full-content` 从 503 变为 **200，6,134,584 字节**。

### 坑 4：Spring Boot 启动「卡在 80%」

**现象**：日志停在 `<==========---> 80% EXECUTING [5m 38s]`。

**根因**：那个百分比是 **Gradle 的任务完成度**，不是 Spring 的启动进度。`bootRun` 直到进程结束才返回，Gradle 无从细分进度，rich console 就在 80% 上永久停住并持续刷新。

**解决**：加 `--console=plain`。

**验证**：用 Python `pty.fork()` 捕获真实 TTY 输出（macOS 自带 `script` 会在 4096 字节处截断，导致误判）。

### 坑 5：Cloudflare 拦截（核心问题）

**现象**：请求等待很久后 504。

**排查过程**：适配器日志显示 6 次 `RateLimitException`、**0 次**「Cloudflare 质询在限时内未解决」，且响应体含 `Access denied` 与 `cf-alert cf-alert-error`。说明页面是**封禁页**而非可交互质询页 —— **手动过验证解决不了**。

**转折点**：`/health?verify=true` 返回 `sessionVerified: true` 一度让我误判「上游正常」。实际上 `main.py:75` 的 `if _api.is_logged_in and _session_verified: return _api` 直接短路返回，**零网络请求**，那个标记只代表「本进程启动时登录成功过」。

**关键发现**（用户提示的方向）：整本 TXT 位于 CDN：

```
https://dl{1,2}.wenku8.com/txtutf8/{aid//1000}/{aid}.txt
```

`_fetch_binary` 只带 `User-Agent`，**无 Cookie、无登录、无浏览器**，不经过 Cloudflare 质询。封面能读正是同理（走 `img.wenku8.com`，不同域名）。

**验证**：抽样 3 本全部 HTTP 200，各 5MB 左右。

**解决**：新增下载源 provider，完全绕开 `www.wenku8.net`。

### 坑 6：请求频率触发限流

**证据**：日志显示请求序列为**连续 5 次 200 后第 6 次 503**。

**根因**：`pywenku8api` 除等待页面导航的 `sleep` 外没有任何限流；`_semaphore` 限制的是**并发数**（默认 2），不限制**单位时间请求数**。串行导入会背靠背发送。

**解决**：新增 `app/throttle.py`，可注入时钟便于测试：

- `acquire()`：补足距上次请求的**剩余**间隔（已流逝时间抵扣）
- `note_rate_limited()`：命中限流后进入冷却（默认 900 秒），期内**直接拒绝、不再打上游**
- **只对 `UPSTREAM_BLOCKED` 开启冷却** —— 超时等与 IP 信誉无关的错误不触发，否则一次网络抖动会让适配器僵死 15 分钟

**验证**：8 个单元测试用假时钟覆盖，零真实等待；端到端 5 项断言全部通过。

### 坑 7：切分算法方向错误（本次最大的坑）

**现象**：真实全文只切出 127 章，按卷名统计实际有 **198 章，漏 36%**。

**根因**：我用「封闭章节标记集合」（`序章|第X章|终章|后记|插图|尾声|番外`）做锚点。但真实 TXT 里存在大量**自由形式**的章节标题：

```
“文学少女”登上石像怪与笨蛋的阶梯 “文学少女”和被杀的笨蛋
恋爱插话集第一弹 文学少女和恋爱的牛魔王
外传一 见习生的初恋 ★相逢的小故事 文学少女见习生的初恋
```

**没有封闭集合能穷举这些。**

**解决**：改为**两阶段卷名发现**：

1. 用已知标记 bootstrap 出候选卷名（每个卷至少含一个「后记」或「插图」章，这两个标记是稳定的）
2. 剪掉「卷名 + 子标题」形态的碎片
3. 用卷名把该卷下**所有**章节标题行扩展出来

**验证**：真实全文 198 章 / 17 卷 / 偏移回读 0 失配。

### 坑 8：CRLF 换行（最隐蔽的坑）

**现象**：抽样下载 3 本书验证泛化性，**全部降级为单章**（切不出任何章节），而《文学少女》正常。

**根因**：**换行符不统一**。抽样 3 本全是 CRLF，文学少女是 LF。原实现只按 `\n` 切行，CRLF 文件每行末尾残留 `\r`，使标题行正则的 `$` 锚点失配。

**解决**：按 `\r?\n` 切分并剥离行尾 `\r`，行偏移仍相对原文计算。

**验证**（真实 CDN 下载 + 真实切分器）：

| 书 | 字节 | 章节 | 卷 | 偏移失配 |
|---|---|---|---|---|
| aid=1011 | 5.9 MB | 274 | 21 | 0 |
| aid=3020 | 5.2 MB | 168 | 12 | 0 |
| aid=2580 | 5.1 MB | 295 | 16 | 0 |
| 文学少女 | 6.0 MB | 198 | 17 | 0 |

**意外收获**：此前记录的「`短篇` 卷无法被发现」缺口**随之消失** —— 那个缺口其实是 CRLF 的连带现象，不是算法缺陷。

### 坑 9：异步竞态

**问题**：计划里写的是「触发 `backfill` 后立刻读库」，但 `backfill` 标了 `@Async`，导入（下载数 MB + 切分）根本来不及完成，必然再次 404。

**解决**：同步返回下载结果本身，同时异步回填写入数据库使后续读取命中 DB。

**连带修正**：`book()` 原本返回空标题，而 `ContentImportService` 对空标题会抛 `来源书目缺少标题`，导致回填静默失败。改为从本地库取元数据。

### 坑 10-11：全角空格与内存

**全角空格**：Java 正则的 `\S` **只认 ASCII 空白**，全角空格 U+3000 和不换行空格 U+00A0 会被当作非空白，导致缩进正文行被误判为章节标题。改用显式字符类 `[ \t　 ]` 排除。

**内存**：审查时发现 `downloadCache` 是单例上永不清理的 `Map`，而整本 TXT 可达 5MB，遍历 73 本会累积数百 MB。改为容量 4 的 LRU + `synchronized`（`LinkedHashMap` 非线程安全，而回退链路会被并发调用）。

---

## 三、实现的内容

### 3.1 后端：下载源 provider（新增 5 个类）

```
backend/src/main/java/com/sakuya/backend/content/download/
├── DownloadSourceProperties.java        配置（开关/超时/体积上限）
├── DownloadSourceConfiguration.java     Bean 装配
├── DownloadSourceClient.java            按 aid 取整本 TXT，节点回退
├── ChapterSplitter.java                 两阶段卷名发现（纯函数）
└── DownloadSourceContentProvider.java   适配为统一 ContentProvider 协议
```

**数据流**：

```
用户点进书 → ContentReadService 查无正文 → 404
   ↓
ContentFallbackFacade 捕获 404
   ↓
下载源优先（不碰 CF）→ 失败才回退适配器
   ↓
ContentImportService.importBook（一行未改）
   ↓
full.txt + 章节文件 + full_text_offset 落库
```

**关键点**：`ContentImportService` **一行都不用改** —— 它只依赖 `ContentProvider` 接口，新 provider 产出的 `ProviderVolume`/`ProviderChapter` 与原有实现同构。这验证了「写侧已可插拔，加数据源不需要动读侧」的架构判断。

**新增配置**：

```yaml
wenku8:
  download-source:
    enabled: ${WENKU8_DOWNLOAD_SOURCE_ENABLED:true}
    connect-timeout-seconds: ${WENKU8_DOWNLOAD_CONNECT_TIMEOUT_SECONDS:5}
    read-timeout-seconds: ${WENKU8_DOWNLOAD_READ_TIMEOUT_SECONDS:30}
    max-bytes: ${WENKU8_DOWNLOAD_MAX_BYTES:67108864}
```

### 3.2 适配器：请求节流（新增 1 个模块）

```
services/wenku8-adapter/
├── app/throttle.py               UpstreamThrottle（间隔 + 冷却）
└── tests/test_throttle.py        8 个测试（假时钟）
```

接入点是 `_cached`（所有上游调用的唯一收口）：

```python
async with _semaphore:
    # 节流必须在信号量内部：否则被放行的并发请求会各自算出同样的时间点而同时发出
    try:
        await _throttle.acquire()
    except CooldownActive as error:
        raise UpstreamError("UPSTREAM_BLOCKED", "Wenku8 被 Cloudflare 或限流拦截") from error
    ...
    # 仅对真正的限流拦截开启冷却；超时、解析失败等与 IP 信誉无关
    if mapped.code == "UPSTREAM_BLOCKED": _throttle.note_rate_limited()
```

### 3.3 前端：详情页阅读方式选择

**改动文件**：

| 文件 | 改动 |
|---|---|
| `Wenku8DetailScreen.kt` | 封面比例 + 两个阅读按钮 + 就地下拉目录 |
| `Wenku8DetailViewModel.kt` | 目录懒加载状态机 |
| `Wenku8DetailScreen` 的导航 | 接上 `onFullReadClick` |
| `ReaderRoutes.kt` | 新增 `WENKU8_FULL_READER_ROUTE` |
| `ReaderNavigation.kt` | 注册整本阅读路由 |
| `ReaderRepository.kt` | `targetChapterId` 改为可空 |
| `ReaderViewModel.kt` | 三处签名改为可空 |

**封面适配**：

```kotlin
// 原来：横向拉满 + Crop，2:3 的竖版封面被裁掉大半
Modifier.fillMaxWidth().height(220.dp)

// 现在：固定高度 + 竖版比例，居中
Modifier.height(240.dp).aspectRatio(2f / 3f)
```

**两种阅读方式**：

| 按钮 | 行为 |
|---|---|
| 整本阅读 | 走新路由，`chapterId=null`，连续阅读，不做目标章定位 |
| 章节阅读 ▼ | **就地展开目录**，点具体章节才进阅读器 |

**目录懒加载**（这是本次前端的关键设计）：

```
进详情页    → 只调 novel()，读本地元数据，不触发任何下载
选章节阅读  → 才调 chapters()，此时后端才可能触发下载
点某章      → 才请求该章内容
```

**为什么要懒加载**：后端在目录缺失时会回退到下载源抓取整本 TXT（数 MB）。若进页即拉目录，用户每打开一个详情页都会触发一次完整下载。

**收起再展开不重复下载** —— 数据保留在 state 里。

---

## 四、测试与验证

### 测试规模

```
后端：81 个测试全部通过（原有 48 + 新增 33）
适配器：8 个单元测试
前端：全量编译通过 + reader 模块测试通过
```

新增测试文件：

```
backend/src/test/java/com/sakuya/backend/content/download/
├── ChapterSplitterTest.java                 15 个（切分规则、CRLF、降级、边界）
├── DownloadSourceClientTest.java             6 个（本地 HTTP 桩，节点回退、CF 检测）
├── DownloadSourceContentProviderTest.java    9 个（aid 识别、卷章映射、插图过滤）
└── DownloadSourceFallbackTest.java           1 个（provider 注册）
services/wenku8-adapter/tests/test_throttle.py  8 个（假时钟）
```

测试样本：`wenku8-1-head.txt`（18,930 字节 / 22 章 / 2 卷，从真实 TXT 截取前两卷）

### 真实数据验证

| 验证项 | 结果 |
|---|---|
| CDN 可达性 | 抽样 3 本 HTTP 200，各 ~5MB |
| 切分泛化性 | 4/4 成功，274/168/295/198 章，偏移失配全 0 |
| 章节正文完整性 | `volumes()` 产出无空正文，`importBook` 不会中断 |
| 安全边界 | 适配器日志中 `pywenku8`/`login`/`chrome` 均为 0 |

---

## 五、未完成事项

| 项 | 状态 |
|---|---|
| **端到端真机验证** | **未做** —— 验证到了「Provider 产出的章节无空正文」，但没有真机点进去读 |
| 运行中服务是旧代码 | 后端 PID 74073、适配器 PID 74022 均早于本次改动，需重启才生效 |
| 适配器节流未生效 | 运行实例启动早于 `throttle.py` 修改 7 分钟 |
| 批量导入 | 下载源无目录枚举能力，批量导入仍只能走适配器 |
| 文字符号代替图标 | `▲▼` 而非 `ExpandLess/ExpandMore`，避免引入 `material-icons-extended` |
| `bookdetail` 模块未改 | 本次只动了在线小说详情页；本地书详情页无此需求 |

---

## 六、过程中的判断修正（我犯错并纠正的地方）

诚实记录，便于复盘：

1. **端口检查**：先写了事后校验，实测无效，改为前置检查
2. **`sessionVerified`**：一度据此判断「上游正常」，实际是短路返回的假阳性
3. **过滤插图章**：先断言「样本无图片链接所以过滤不生效」，实测推翻了该断言
4. **「90 章」口径**：反复使用的这个数字来自只匹配 `^第X卷` 的统计，漏掉非该前缀的整卷，已废弃
5. **「短篇卷缺失」**：曾判定为算法缺陷并记录为已知缺口，后证明是 CRLF 的连带现象
6. **`book()` 返回空标题**：计划原文如此，实现时发现会被 `ContentImportService` 拒绝导库
7. **异步竞态**：计划原文写「触发 backfill 后读库」，实现时发现 `@Async` 下必然失败
8. **Subagent 执行**：SDD 流程三次派发全部失败（环境变量指向无效模型），改用会话内执行

---

## 七、变更统计

```
27 个文件变更，+3384 / -49 行

后端      新增 5 个类 + 4 个测试类 + 2 个测试样本
适配器    新增 1 个模块 + 1 个测试文件
前端      修改 7 个文件
文档      1 份设计 spec + 1 份实施计划
```

**提交历史**（12 个）：

```
4591bf6a 新增下载源 provider 与离线章节切分设计
5902abc9 新增下载源 provider 实施计划
3e9c42cd 为适配器加请求节流与限流冷却
8b0a4723 新增离线章节切分器：从整本 TXT 还原卷章结构与字符偏移
a283be39 新增 CDN 下载源客户端：按 aid 取整本 TXT 并按节点回退
9cb16d66 新增下载源 ContentProvider：整本 TXT 适配为统一卷章协议
ec3d410f 切分器改为两阶段卷名发现，并过滤无正文章节
fca6aaa4 回退链路改为下载源优先：CDN 拿不到时才走适配器
23f27efa 修复 CRLF 换行导致整本降级为单章
042dd08b 补充切分器的泛化性抽样验证结果
0c4b0d80 修复下载缓存无上限导致的内存累积
1da2c313 详情页新增阅读方式选择，封面改为竖版比例
```

---

## 附：三个最值得记住的教训

1. **验证要覆盖多种输入形态。** 切分器只在 1 本书上验证时看起来完全正常，抽样 3 本就全军覆没（CRLF）。单一数据源的验证会给出虚假信心。

2. **错误信息的细微差异是重要线索。** 「未启用」vs「**尚**未启用」这一个字的差别，把问题从「后端配置错误」定位到了「请求确实到达了适配器」，是排查的关键转折。

3. **修一层可能暴露下一层。** 切分问题连续三轮：封闭标记集合 → 两阶段卷名发现 → CRLF。前两轮都在「打补丁」，直到第三轮才发现真正原因是换行符。**当同类问题连续出现时，应该停下来质疑假设，而不是继续修补。**
