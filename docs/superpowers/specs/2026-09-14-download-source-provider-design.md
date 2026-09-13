# 下载源 ContentProvider 与离线章节切分 — 设计

日期：2026-09-14
范围：仅后端（A 部分）。客户端的「正本阅读 / 章节阅读」两种方式单独走一轮设计。

## 背景

截至 2026-09-14，库中 86 本书有 1 本有正文，其余 85 本 `full_content_object_key IS NULL`，
用户点进去只能等到超时后 504。根因有两条，均已实测确认：

1. **Cloudflare 拦截。** 适配器抓取 `www.wenku8.net` 时被拒。适配器日志中
   `RateLimitException` 6 次、`Cloudflare 质询在限时内未解决` 0 次，且响应体含
   `Access denied` 6 次、`cf-alert cf-alert-error` 6 次 —— 页面是**封禁页**而非可交互质询页，
   手动验证无法解除。同一份日志显示请求序列为连续 5 次 200 后第 6 次 503，是典型的频率触发。
2. **请求量过大。** `ContentImportService.importBook` 按章循环抓取，一章一次 HTTP。
   以《文学少女》为例有 90 章，即 90+ 次请求，而实测约 5 次后即被拦。

已验证的替代路径：整本 TXT 位于 CDN `https://dl{1,2}.wenku8.com/txtutf8/{aid//1000}/{aid}.txt`，
`_fetch_binary` 仅带 `User-Agent`，**无 Cookie、无登录、无浏览器**，不经过 Cloudflare 质询。

## 目标

让没有本地正文的书能按需（用户点进去时）从 CDN 下载整本 TXT，离线切分出章节结构，
走现有落库链路入库。全程不访问被 CF 防护的 `www.wenku8.net`。

非目标：客户端的两种阅读方式（下一轮）、批量导入（下载源无目录枚举能力，见「约束」）。

## 约束

- **下载源没有目录枚举接口。** CDN 只能按 aid 取单本，无法「列出所有小说」。
  因此新 provider 的 `catalog(String mode)` 无法实现，它只支持单本懒加载，
  不支持 `ContentImportRunner.execute`（其内部调用 `provider.catalog(...)`）。
  批量导入仍只能走适配器。这不是缺陷，是下载源的固有限制。
- **aid 可得性。** 86 本书中 73 本 `id` 形如 `wenku8-N`，其 `source_novel_id` 均为纯数字
  （实测 73/73），可直接拼接 CDN URL。其余 13 本为早期手工导入的短 slug
  （如 `biblia`、`danmachi`），其 `source_novel_id` 为 `NULL`，**无法下载**。
  因此可下载范围是 73 本，非全部 86 本。provider 必须校验 aid 为纯数字，
  非纯数字时明确返回不支持，不得静默失败。
- **不写入凭据。** 沿用现有原则，CDN 请求不需要任何凭据。

## 架构

新增 `DownloadSourceContentProvider implements ContentProvider`，与现有
`Wenku8ContentProvider` 并列注册为 Spring Bean。

```
用户点进书
  → ContentReadService.fullContent 查无正文 → BusinessException(404)
  → ContentFallbackFacade 捕获 404（现有逻辑，不改）
  → DownloadSourceContentProvider（新增）
       book(aid)      → 元数据；本地已有则跳过
       volumes(aid)   → 下载 TXT → 离线切分 → 卷/章结构
       chapterContent → 按切分偏移截取
  → ContentImportService.importBook（完全不动）
  → full.txt + 章节文件 + full_text_offset 落库
```

**关键点：`ContentImportService` 一行都不用改。** 它只依赖 `ContentProvider` 接口，
新 provider 产出的 `ProviderVolume`/`ProviderChapter` 与 `Wenku8ContentProvider` 同构。
这验证了既有的「写侧已可插拔，加数据源不需要动读侧」结论。

## 组件

### 1. `DownloadSourceContentProvider`

职责：实现 `ContentProvider`，把 CDN 整本 TXT 适配成统一的卷/章结构。

- `id()` 返回 `"WENKU8_CDN"`（与现有 `"WENKU8"` 区分，便于指标与排障）
- `volumes(externalBookId)`：下载 → 切分 → 组装
- `chapterContent(bookId, chapterId)`：从切分结果的字符偏移截取，不重新下载
- `catalog(mode)`：抛 `UnsupportedOperationException`，并注明原因
- `book(bookId)`：从本地库读取元数据；本地无记录时返回最小可用结构

依赖：一个 `DownloadSourceClient`（HTTP 取整本 TXT）+ 一个 `ChapterSplitter`（纯函数）。三者分离，便于各自单测。

### 2. `DownloadSourceClient`

职责：按 aid 取整本 TXT。

- URL：`https://dl{node}.wenku8.com/txtutf8/{aid/1000}/{aid}.txt`，`node` 依次尝试 1、2
- 仅在 429 时切换节点，其他错误直接抛出（对齐 pywenku8api 的既有语义）
- 带 `User-Agent`，无其他头部
- 解码 UTF-8（已验证文学少女的 full.txt 为 UTF-8 无 BOM）
- 超时可配，默认沿用较宽松值

### 3. `ChapterSplitter`

职责：从整本 TXT 文本切分出卷与章节，并给出每章在全文中的字符偏移。**纯函数，无 IO，可直接单测。**

切分规则（在《文学少女》5,972,153 字节 / 90 章上验证，0 例外）：

```
^第[一二三四五六七八九十百零〇\d]+卷\s+\S+.*$
```

该行同时包含卷名与章节标记，形如：

```
第一卷 渴望死亡的小丑 序章 取代自我介绍的回忆——前天才美少女作家
第一卷 渴望死亡的小丑 第一章 远子学姐是位美食家
```

卷名 = `第X卷 <卷名>`（第一个空格前的「第X卷」+ 紧随的部分，直至章节标记前）
章节标题 = 该行去除卷名前缀后的剩余部分
章节标记集合（封闭）：`序章`、`第X章`、`终章`、`后记`、`插图`

**偏移规则必须与 `ContentImportService.java:72` 一致**，否则 `full_text_offset` 错位、
章节跳转会落到错误位置：

```java
fullText.append(volumeTitle).append(' ').append(chapterTitle).append('\n')...
```

即切分器输出的 `offset` 必须是「`卷名 + 空格 + 章节标题 + \n`」这一串在全文中的起点。

**降级策略（保守，不抛错）：** 若切分出的章节数 < 2，则不报错，退化为**单章整本**
（整本作为一个章节）。此时「正本阅读」可用，「章节阅读」只有一章。
理由：格式只在 1 本书上验证过，其他书的 TXT 可能结构不同；降级比中断好。

## 数据流

1. 用户点进无正文的书 → `ContentReadService` 抛 404
2. `ContentFallbackFacade` 捕获 404，当前会调 `gateway.fullContent()`（适配器，被 CF 挡）
   → **改为优先走下载源**，下载源失败才降级到适配器
3. 下载源成功 → `imports.backfill("WENKU8_CDN", aid)` 异步入库
4. 下次读取直接命中 DB

## 错误处理

| 情况 | 行为 |
|---|---|
| aid 非纯数字（13 本短 slug 书） | 不支持下载，回退适配器路径 |
| CDN 404（该本无 TXT） | 回退适配器路径 |
| CDN 429 | 切换节点 1→2；两个都 429 才失败 |
| CDN 返回 CF 质询页 | 抛明确错误，不重试 |
| 切分章节数 < 2 | 降级为单章整本，正常入库 |
| 网络超时 | 回退适配器路径 |

**所有失败都必须回退到适配器，不得让整条读取链路中断** —— 适配器虽受 CF 影响，
但在 CDN 缺资源时仍是唯一可用路径。

## 测试

复用现有后端测试设施（48 个测试已可运行）。

**`ChapterSplitterTest`（核心，纯函数）**
- 以《文学少女》真实 full.txt 为样本，断言切分出的章节数 = 90
- 断言首章标题、卷名、offset = 0
- 断言所有 offset 严格递增且在文本范围内
- 断言按 offset 截取的文本以「卷名 章节标题」开头
- 边界：空文本、无任何章节标记、仅 1 个标记 → 均降级为单章整本

**`DownloadSourceContentProviderTest`**
- aid 为非数字 slug → 明确不支持
- CDN 429 → 切换节点
- 切分降级路径

测试样本文件放 `backend/src/test/resources/`，用真实 TXT（可截取前若干章以控制仓库体积，
但**必须保留至少 2 个卷**以覆盖卷切分逻辑）。

## 未决风险

**切分正则的泛化性。** 仅在 1 本书上验证（本地只有 `wenku8-1` 有全文，
`wenku8-3020` 只有单章文件）。其余 71 本可下载的书可能结构不同（不同的章节标记、
无卷结构、行内格式差异）。降级策略确保不中断，但可能出现「切出来的章节结构不对」
这类静默问题。缓解：实现后抽样验证若干本，并在切分结果异常时记日志。

**运行中的适配器未加载节流改动。** 已完成的上游节流与冷却（`app/throttle.py`）尚未在运行实例中生效：
运行中的适配器 PID 74022 启动于 02:32:22，而 `throttle.py` 修改于 02:39:13，晚于启动时间。
需重启适配器后该防护才生效。此项独立于本次设计，但影响「回退到适配器」路径的实际安全性。
