# 下载源 ContentProvider 与离线章节切分 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让没有本地正文的书能按需从 CDN 下载整本 TXT、离线切分章节结构并入库，全程不访问被 Cloudflare 拦截的 `www.wenku8.net`。

**Architecture:** 新增一个 `ContentProvider` 实现（`DownloadSourceContentProvider`），与现有 `Wenku8ContentProvider` 并列注册为 Spring Bean。它把 CDN 整本 TXT 适配成统一的卷/章结构，交给**完全不动**的 `ContentImportService.importBook` 落库。内部拆成三个可独立测试的组件：HTTP 客户端、纯函数切分器、Provider 适配层。

**Tech Stack:** Java 21、Spring Boot 3.5.16、JUnit 5、Gradle、`java.net.http.HttpClient`

**Spec:** `docs/superpowers/specs/2026-09-14-download-source-provider-design.md`

## Global Constraints

- **不改 `ContentImportService`。** 它只依赖 `ContentProvider` 接口，新 provider 产出的 `ProviderVolume`/`ProviderChapter` 必须与之同构。任何"顺手改一下 importBook"的冲动都要拒绝。
- **偏移规则必须对齐 `ContentImportService.java:72`：** `fullText.append(volumeTitle).append(' ').append(chapterTitle).append('\n')`。切分器输出的 offset 必须是「卷名 + 半角空格 + 章节标题 + `\n`」这一串在全文中的起点。错位会导致章节跳转落到错误位置。
- **章节标题行的解析以「章节标记」为锚点，不以「第X卷」为前缀。** 实测《文学少女》有 18 个卷，其中「恋爱插话集」「外传」「青涩作家和文学少女编辑」等**不以「第X卷」开头**。若按「第X卷」前缀切分，会漏掉整卷（实测漏 37 章）。正确做法：行首任意非空白内容 + 空格 + 章节标记。
- **切分少于 2 章时降级为单章整本，不抛错。** 格式只在 1 本书上验证过，降级比中断好。
- **13 本短 slug 书（`source_novel_id` 为 NULL）不可下载**，必须明确返回不支持，不得静默失败。
- **`catalog()` 抛 `UnsupportedOperationException`**，并注明下载源无目录枚举能力。
- **失败一律回退适配器**，不得让整条读取链路中断。
- **注释用中文**，与代码库现有风格一致。
- 测试命令统一为：`cd /Users/xiaoye/FlutterProjects/sakuyainandroid && ./gradlew -p backend test --tests "<测试类>"`

### 切分规则的实测基线（《文学少女》全本 5,972,153 字节）

| 指标 | 实测值 |
|---|---|
| 章节标记行 | **127** |
| 卷数 | **18** |
| 偏移回读失配 | **0 / 127** |
| 「插图」类无正文 | **10**（正文仅为图片链接） |
| 过滤后保留 | **117** |

术语：**章节标记**是 `序章` / `第X章` / `终章` / `后记` / `插图` / `尾声` / `番外` 之一；
**卷名**是标记之前的部分；**章节标题**是标记及其之后的部分。
正文与题目中「90 章」的旧口径来自只匹配 `^第X卷` 的统计，会漏掉非「第X卷」的整卷，已废弃。

## 文件结构

| 文件 | 职责 |
|---|---|
| `backend/src/main/java/com/sakuya/backend/content/download/DownloadSourceProperties.java` | 配置（CDN 节点、超时、开关） |
| `backend/src/main/java/com/sakuya/backend/content/download/DownloadSourceClient.java` | 按 aid 取整本 TXT，含节点回退与 CF 质询检测 |
| `backend/src/main/java/com/sakuya/backend/content/download/ChapterSplitter.java` | **纯函数**：文本 → 卷/章结构 + 字符偏移 |
| `backend/src/main/java/com/sakuya/backend/content/download/DownloadSourceContentProvider.java` | 实现 `ContentProvider`，串联 client 与 splitter |
| `backend/src/main/java/com/sakuya/backend/content/ContentFallbackFacade.java` | **修改**：回退时优先走下载源 |
| `backend/src/main/resources/application.yml` | **修改**：新增 `wenku8.download-source.*` 配置 |
| `backend/src/main/java/com/sakuya/backend/SakuyaBackendApplication.java` | **修改**：注册新的 Properties 类 |
| `backend/src/test/resources/chapter-splitter/wenku8-1-head.txt` | 测试样本（已生成，18,930 字节 / 19 章 / 2 卷） |
| `backend/src/test/resources/chapter-splitter/with-image-chapters.txt` | 插图过滤测试样本（Task 3.5 创建，4 章含 1 个插图） |

---

### Task 1: ChapterSplitter 纯函数切分器

这是整个特性的核心，也是风险最集中处。先做它，因为它是纯函数、无依赖、可完全单测。

**Files:**
- Create: `backend/src/main/java/com/sakuya/backend/content/download/ChapterSplitter.java`
- Test: `backend/src/test/java/com/sakuya/backend/content/download/ChapterSplitterTest.java`
- 样本（已就位）: `backend/src/test/resources/chapter-splitter/wenku8-1-head.txt`

**Interfaces:**
- Consumes: 无（纯函数，不依赖任何其他 Task）
- Produces:
  - `record SplitChapter(String volumeTitle, String title, int offset)`
  - `record SplitResult(List<SplitChapter> chapters)`，含便捷方法 `int chapterCount()`、`boolean isDegraded()`
  - `static SplitResult split(String fullText)`

- [ ] **Step 1: 写失败的测试**

创建 `backend/src/test/java/com/sakuya/backend/content/download/ChapterSplitterTest.java`：

```java
package com.sakuya.backend.content.download;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * ChapterSplitterTest.java
 * 职责说明：用《文学少女》真实 TXT 样本锁定切分规则，防止正则与偏移计算被无意改坏。
 * 执行流程：加载测试样本 -> 切分 -> 断言章节数、偏移递增、按偏移回读的文本以章节标题开头。
 */
class ChapterSplitterTest {

    private static String sample() throws Exception {
        try (InputStream in = ChapterSplitterTest.class.getResourceAsStream("/chapter-splitter/wenku8-1-head.txt")) {
            if (in == null) throw new IllegalStateException("测试样本缺失：/chapter-splitter/wenku8-1-head.txt");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void 真实样本切分出预期章节数() throws Exception {
        ChapterSplitter.SplitResult result = ChapterSplitter.split(sample());
        assertThat(result.chapterCount()).isEqualTo(19);
        assertThat(result.isDegraded()).isFalse();
    }

    /** 样本只含前两卷；卷名必须剥掉「第X卷」之外的任何内容，且非「第X卷」卷名同样可切。 */
    @Test
    void 真实样本切分出预期卷数() throws Exception {
        ChapterSplitter.SplitResult result = ChapterSplitter.split(sample());
        assertThat(result.chapters()).extracting(ChapterSplitter.SplitChapter::volumeTitle)
            .containsOnly("第一卷 渴望死亡的小丑", "第二卷 渴求真爱的幽灵");
    }

    /** 「后记」这类章节无具体标题，标题应只保留标记本身而不是变成空串。 */
    @Test
    void 无具体标题的章节以标记为标题() throws Exception {
        String text = "第一卷 测试卷 后记\n正文。\n第一卷 测试卷 第一章 有标题\n正文。";
        ChapterSplitter.SplitResult result = ChapterSplitter.split(text);
        assertThat(result.chapters()).extracting(ChapterSplitter.SplitChapter::title)
            .containsExactly("后记", "第一章 有标题");
    }

    @Test
    void 首章偏移为零且卷名与标题正确() throws Exception {
        ChapterSplitter.SplitResult result = ChapterSplitter.split(sample());
        ChapterSplitter.SplitChapter first = result.chapters().get(0);
        assertThat(first.offset()).isZero();
        assertThat(first.volumeTitle()).isEqualTo("第一卷 渴望死亡的小丑");
        assertThat(first.title()).isEqualTo("序章 取代自我介绍的回忆——前天才美少女作家");
    }

    /** 卷名不以「第X卷」开头时也必须能切出来，这是旧实现漏切整卷的根因。 */
    @Test
    void 非第X卷开头的卷名同样可切() throws Exception {
        String text = "恋爱插话集第一弹 后记\n正文。\n外传一 见习生的初恋 第一章 要跟我一起殉情吗？\n正文。";
        ChapterSplitter.SplitResult result = ChapterSplitter.split(text);
        assertThat(result.chapters()).extracting(ChapterSplitter.SplitChapter::volumeTitle)
            .containsExactly("恋爱插话集第一弹", "外传一 见习生的初恋");
    }

    /** 以全角空格缩进的正文行不能被误判为章节标题。 */
    @Test
    void 缩进的正文行不被误判() {
        String text = "第一卷 测试卷 第一章 标题\n　　这是缩进的正文，里面有 插图 两个字。\n第二卷 测试卷 第一章 标题二\n正文。";
        ChapterSplitter.SplitResult result = ChapterSplitter.split(text);
        assertThat(result.chapterCount()).isEqualTo(2);
    }

    @Test
    void 偏移严格递增且在文本范围内() throws Exception {
        String text = sample();
        ChapterSplitter.SplitResult result = ChapterSplitter.split(text);
        int previous = -1;
        for (ChapterSplitter.SplitChapter chapter : result.chapters()) {
            assertThat(chapter.offset()).isGreaterThan(previous);
            assertThat(chapter.offset()).isLessThan(text.length());
            previous = chapter.offset();
        }
    }

    /** 偏移必须指向「卷名 + 空格 + 章节标题」，与 ContentImportService 的拼接规则一致。 */
    @Test
    void 按偏移回读的文本以卷名加标题开头() throws Exception {
        String text = sample();
        ChapterSplitter.SplitResult result = ChapterSplitter.split(text);
        for (ChapterSplitter.SplitChapter chapter : result.chapters()) {
            String expected = chapter.volumeTitle() + " " + chapter.title();
            assertThat(text.startsWith(expected, chapter.offset()))
                .as("偏移 %d 处应为「%s」", chapter.offset(), expected)
                .isTrue();
        }
    }

    /** 标题允许重复：每卷都有「后记」「插图」，因此必须用 offset 而非标题作为章节标识。 */
    @Test
    void 偏移唯一可作章节标识() throws Exception {
        ChapterSplitter.SplitResult result = ChapterSplitter.split(sample());
        assertThat(result.chapters()).extracting(ChapterSplitter.SplitChapter::offset).doesNotHaveDuplicates();
        // 同时锁定「标题确实会重复」这一事实，防止有人改用标题做标识。
        assertThat(result.chapters()).extracting(ChapterSplitter.SplitChapter::title)
            .containsDuplicates();
    }

    @Test
    void 空文本降级为单章整本() {
        ChapterSplitter.SplitResult result = ChapterSplitter.split("");
        assertThat(result.isDegraded()).isTrue();
        assertThat(result.chapterCount()).isEqualTo(1);
        assertThat(result.chapters().get(0).offset()).isZero();
    }

    @Test
    void 无章节标记的文本降级为单章整本() {
        String plain = "这是一段没有任何章节标记的普通文本。\n第二行。\n第三行。";
        ChapterSplitter.SplitResult result = ChapterSplitter.split(plain);
        assertThat(result.isDegraded()).isTrue();
        assertThat(result.chapterCount()).isEqualTo(1);
    }

    @Test
    void 仅一个章节标记也降级() {
        String single = "第一卷 某某卷 第一章 开端\n正文内容。";
        ChapterSplitter.SplitResult result = ChapterSplitter.split(single);
        assertThat(result.isDegraded()).isTrue();
        assertThat(result.chapterCount()).isEqualTo(1);
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
cd /Users/xiaoye/FlutterProjects/sakuyainandroid && ./gradlew -p backend test --tests "com.sakuya.backend.content.download.ChapterSplitterTest"
```

预期：编译失败，`ChapterSplitter` 不存在。

- [ ] **Step 3: 实现 ChapterSplitter**

创建 `backend/src/main/java/com/sakuya/backend/content/download/ChapterSplitter.java`：

```java
package com.sakuya.backend.content.download;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ChapterSplitter.java
 * 职责说明：把整本 TXT 按「第X卷 <卷名> <章节标记> <标题>」行切分为卷/章结构，并给出全文字符偏移。
 * 执行流程：逐行匹配章节标题行 -> 记录该行在全文中的偏移 -> 章节数不足时降级为单章整本。
 *
 * 偏移规则必须与 ContentImportService 的全文拼接保持一致（卷名 + 空格 + 标题 + 换行），
 * 否则 full_text_offset 会错位、章节跳转落到错误位置。
 * 该正则已在《文学少女》真实 TXT（5,972,153 字节 / 127 章 / 18 卷）上验证，偏移回读 0 失配。
 * 卷名不限定「第X卷」前缀：实测存在「恋爱插话集第一弹」「外传一 见习生的初恋」等卷名。
 */
public final class ChapterSplitter {

    /**
     * 章节标题行：<卷名> <章节标记> [<标题>]。
     * 卷名不限定以「第X卷」开头 —— 实测存在「恋爱插话集第一弹」「外传一 见习生的初恋」
     * 「青涩作家和文学少女编辑」等卷名，若限定前缀会整卷漏切。
     * 行首锚定 \\S 以排除以全角空格缩进开头的正文行。
     */
    private static final Pattern CHAPTER_LINE = Pattern.compile(
        "^(\\S.*?)\\s+(序章|第[一二三四五六七八九十百零〇\\d]+章|终章|后记|插图|尾声|番外)(?:\\s+(.*))?$");

    /** 行长上限：超过则认为是正文而非章节标题行。 */
    private static final int MAX_HEADER_LENGTH = 80;

    /** 低于该章节数视为切分失败，降级为单章整本而非抛错。 */
    private static final int MIN_CHAPTERS = 2;

    private ChapterSplitter() { }

    public record SplitChapter(String volumeTitle, String title, int offset) { }

    public record SplitResult(List<SplitChapter> chapters) {
        public int chapterCount() { return chapters.size(); }
        /** 是否因无法切分而退化为单章整本。 */
        public boolean isDegraded() { return chapters.size() < MIN_CHAPTERS; }
    }

    /**
     * 切分整本文本。任何无法识别出至少两章的情况都降级为「整本作为一章」，不抛异常：
     * 上游 TXT 格式未在全部书籍上验证过，降级可保证阅读链路不中断。
     */
    public static SplitResult split(String fullText) {
        String text = fullText == null ? "" : fullText;
        List<SplitChapter> chapters = new ArrayList<>();
        // 逐行处理而非全局 find：需要按行判断长度上限，且行首偏移可直接由累计长度得到。
        int offset = 0;
        for (String line : text.split("\n", -1)) {
            if (!line.isEmpty() && line.length() <= MAX_HEADER_LENGTH) {
                SplitChapter parsed = parse(line, offset);
                if (parsed != null) chapters.add(parsed);
            }
            offset += line.length() + 1; // +1 为换行符
        }
        if (chapters.size() < MIN_CHAPTERS) return degraded(text);
        return new SplitResult(List.copyOf(chapters));
    }

    /**
     * 从标题行拆出卷名与章节标题；不匹配时返回 null 由调用方跳过该行。
     * 章节标记是唯一锚点：卷名 = 标记之前，章节标题 = 标记及其之后。
     */
    private static SplitChapter parse(String line, int offset) {
        Matcher matcher = CHAPTER_LINE.matcher(line);
        if (!matcher.matches()) return null;
        String volumeName = matcher.group(1).trim();
        String marker = matcher.group(2);
        String rest = matcher.group(3) == null ? "" : matcher.group(3).trim();
        if (volumeName.isEmpty()) return null;
        // 章节标题 = 标记 + 可选的具体标题；「后记」「插图」等无具体标题时只用标记本身。
        String title = rest.isEmpty() ? marker : marker + " " + rest;
        return new SplitChapter(volumeName, title, offset);
    }

    private static SplitResult degraded(String text) {
        return new SplitResult(List.of(new SplitChapter("", "", 0)));
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

```bash
cd /Users/xiaoye/FlutterProjects/sakuyainandroid && ./gradlew -p backend test --tests "com.sakuya.backend.content.download.ChapterSplitterTest"
```

预期：8 个测试全部 PASS。

若 `真实样本切分出预期章节数` 失败，检查实际切出数量并据此修正断言或正则 —— 但**不要**为了让测试通过而放宽 `MAX_HEADER_LENGTH`，那会让正文行被误判。

- [ ] **Step 5: 提交**

```bash
cd /Users/xiaoye/FlutterProjects/sakuyainandroid
git add backend/src/main/java/com/sakuya/backend/content/download/ChapterSplitter.java \
        backend/src/test/java/com/sakuya/backend/content/download/ChapterSplitterTest.java \
        backend/src/test/resources/chapter-splitter/wenku8-1-head.txt
git commit -m "新增离线章节切分器：从整本 TXT 还原卷章结构与字符偏移"
```

---

### Task 2: DownloadSourceClient HTTP 客户端

**Files:**
- Create: `backend/src/main/java/com/sakuya/backend/content/download/DownloadSourceProperties.java`
- Create: `backend/src/main/java/com/sakuya/backend/content/download/DownloadSourceClient.java`
- Modify: `backend/src/main/java/com/sakuya/backend/SakuyaBackendApplication.java`
- Modify: `backend/src/main/resources/application.yml`
- Test: `backend/src/test/java/com/sakuya/backend/content/download/DownloadSourceClientTest.java`

**Interfaces:**
- Consumes: 无
- Produces:
  - `@ConfigurationProperties(prefix = "wenku8.download-source") record DownloadSourceProperties(boolean enabled, int connectTimeoutSeconds, int readTimeoutSeconds, int maxBytes)`
  - `class DownloadSourceClient`，构造参数 `(HttpClient httpClient, DownloadSourceProperties properties)`
  - `byte[] download(int aid)` —— 依次尝试节点 1、2，仅在 429 时切换
  - `static String fileUrl(int node, int aid)` —— 供测试断言 URL 形状

- [ ] **Step 1: 写失败的测试**

创建 `backend/src/test/java/com/sakuya/backend/content/download/DownloadSourceClientTest.java`：

```java
package com.sakuya.backend.content.download;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sakuya.backend.common.BusinessException;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * DownloadSourceClientTest.java
 * 职责说明：用本地 HTTP 桩验证节点回退、Cloudflare 质询检测与错误分类，不访问真实 CDN。
 * 执行流程：启动本地 HttpServer 返回预设响应 -> 调用客户端 -> 断言请求次数与异常语义。
 */
class DownloadSourceClientTest {

    private HttpServer server;
    private String baseUrl;
    private final AtomicInteger requests = new AtomicInteger();

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requests.incrementAndGet();
            String path = exchange.getRequestURI().getPath();
            if (path.contains("/429/")) {
                exchange.sendResponseHeaders(429, -1);
            } else if (path.contains("/cf/")) {
                byte[] body = "<html><head><title>Just a moment...</title></head><body></body></html>"
                    .getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(403, body.length);
                try (OutputStream out = exchange.getResponseBody()) { out.write(body); }
            } else if (path.contains("/ok/")) {
                byte[] body = "正文内容".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream out = exchange.getResponseBody()) { out.write(body); }
            } else {
                exchange.sendResponseHeaders(404, -1);
            }
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private DownloadSourceClient clientAt(String pathPrefix) {
        DownloadSourceProperties properties = new DownloadSourceProperties(true, 5, 30, 64 * 1024 * 1024);
        // 覆写节点地址以便指向本地桩：通过系统属性注入不现实，这里直接构造带桩地址的客户端。
        return new DownloadSourceClient(HttpClient.newHttpClient(), properties, url -> baseUrl + pathPrefix + "/" + url);
    }

    @Test
    void 正常下载返回字节() {
        DownloadSourceClient client = clientAt("/ok");
        assertThat(new String(client.download(1234), StandardCharsets.UTF_8)).isEqualTo("正文内容");
    }

    @Test
    void 两个节点均429时抛出明确错误() {
        DownloadSourceClient client = clientAt("/429");
        assertThatThrownBy(() -> client.download(1234))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("限流");
        // 节点 1 与节点 2 各请求一次
        assertThat(requests.get()).isEqualTo(2);
    }

    @Test
    void 遇到Cloudflare质询页抛出明确错误且不重试节点() {
        DownloadSourceClient client = clientAt("/cf");
        assertThatThrownBy(() -> client.download(1234))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Cloudflare");
        assertThat(requests.get()).isEqualTo(1);
    }

    @Test
    void 未找到资源抛出404语义错误() {
        DownloadSourceClient client = clientAt("/missing");
        assertThatThrownBy(() -> client.download(1234))
            .isInstanceOf(BusinessException.class)
            .satisfies(error -> assertThat(((BusinessException) error).getCode()).isEqualTo(404));
    }

    @Test
    void 关闭开关时拒绝下载() {
        DownloadSourceProperties disabled = new DownloadSourceProperties(false, 5, 30, 64 * 1024 * 1024);
        DownloadSourceClient client = new DownloadSourceClient(HttpClient.newHttpClient(), disabled, url -> baseUrl + "/ok/" + url);
        assertThatThrownBy(() -> client.download(1234))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("未启用");
    }

    @Test
    void URL形状符合CDN约定() {
        assertThat(DownloadSourceClient.fileUrl(1, 1234)).isEqualTo("https://dl1.wenku8.com/txtutf8/1/1234.txt");
        assertThat(DownloadSourceClient.fileUrl(2, 3020)).isEqualTo("https://dl2.wenku8.com/txtutf8/3/3020.txt");
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
cd /Users/xiaoye/FlutterProjects/sakuyainandroid && ./gradlew -p backend test --tests "com.sakuya.backend.content.download.DownloadSourceClientTest"
```

预期：编译失败，`DownloadSourceClient` 与 `DownloadSourceProperties` 不存在。

- [ ] **Step 3: 实现 Properties 与 Client**

创建 `backend/src/main/java/com/sakuya/backend/content/download/DownloadSourceProperties.java`：

```java
package com.sakuya.backend.content.download;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * DownloadSourceProperties.java
 * 职责说明：集中维护 CDN 下载源的开关、超时与体积上限，避免把上游地址写死在业务代码中。
 * 执行流程：Spring 读取环境配置 -> Client 在每次下载前检查 enabled -> 按节点顺序发起请求。
 */
@ConfigurationProperties(prefix = "wenku8.download-source")
public record DownloadSourceProperties(boolean enabled, int connectTimeoutSeconds, int readTimeoutSeconds, int maxBytes) {
    public DownloadSourceProperties {
        if (connectTimeoutSeconds <= 0) connectTimeoutSeconds = 5;
        if (readTimeoutSeconds <= 0) readTimeoutSeconds = 30;
        // 整本 TXT 可达数 MB；上限用于防止异常响应耗尽内存。
        if (maxBytes <= 0) maxBytes = 64 * 1024 * 1024;
    }
}
```

创建 `backend/src/main/java/com/sakuya/backend/content/download/DownloadSourceClient.java`：

```java
package com.sakuya.backend.content.download;

import com.sakuya.backend.common.BusinessException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.function.UnaryOperator;

/**
 * DownloadSourceClient.java
 * 职责说明：按 aid 从 CDN 取整本 UTF-8 TXT，不携带任何凭据，不经过需要 Cloudflare 验证的站点域名。
 * 执行流程：校验开关与 aid -> 依次尝试节点 1、2 -> 仅在 429 时切换节点 -> 返回原始字节。
 *
 * 与 pywenku8api 的既有语义保持一致：只有 429 才做节点回退，其余错误直接抛出，
 * 避免把版权受限（4xx）误当成限流而反复重试。
 */
public class DownloadSourceClient {

    /** 测试可注入的 URL 改写函数；生产环境为恒等映射。 */
    private final UnaryOperator<String> urlRewriter;
    private final HttpClient httpClient;
    private final DownloadSourceProperties properties;
    private static final String USER_AGENT =
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36";

    public DownloadSourceClient(HttpClient httpClient, DownloadSourceProperties properties) {
        this(httpClient, properties, UnaryOperator.identity());
    }

    DownloadSourceClient(HttpClient httpClient, DownloadSourceProperties properties, UnaryOperator<String> urlRewriter) {
        this.httpClient = httpClient;
        this.properties = properties;
        this.urlRewriter = urlRewriter;
    }

    /** CDN 路径按 aid 千位分目录：1234 -> /1/1234.txt，3020 -> /3/3020.txt。 */
    public static String fileUrl(int node, int aid) {
        return "https://dl" + node + ".wenku8.com/txtutf8/" + (aid / 1000) + "/" + aid + ".txt";
    }

    public byte[] download(int aid) {
        if (!properties.enabled()) throw new BusinessException(503, "Wenku8 下载源未启用");
        if (aid <= 0) throw new BusinessException(400, "小说标识不合法");
        BusinessException lastRateLimit = null;
        for (int node : new int[] {1, 2}) {
            try {
                return fetch(fileUrl(node, aid));
            } catch (BusinessException error) {
                if (error.getCode() != 429) throw error;
                lastRateLimit = error;
            }
        }
        throw lastRateLimit == null ? new BusinessException(503, "Wenku8 下载源不可用") : lastRateLimit;
    }

    private byte[] fetch(String url) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(urlRewriter.apply(url)))
            .timeout(Duration.ofSeconds(properties.readTimeoutSeconds()))
            .header("User-Agent", USER_AGENT)
            .GET().build();
        try {
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            int status = response.statusCode();
            byte[] body = response.body();
            if (body != null && body.length > properties.maxBytes()) {
                throw new BusinessException(503, "Wenku8 下载内容超出体积上限");
            }
            if (isCloudflareChallenge(status, body)) {
                throw new BusinessException(503, "CDN 资源被 Cloudflare 防火墙拦截");
            }
            if (status == 429) throw new BusinessException(429, "Wenku8 下载源限流");
            if (status == 404) throw new BusinessException(404, "该小说没有可下载的 TXT");
            if (status != 200) throw new BusinessException(503, "Wenku8 下载源返回异常状态：" + status);
            return body == null ? new byte[0] : body;
        } catch (BusinessException error) {
            throw error;
        } catch (java.net.http.HttpTimeoutException error) {
            throw new BusinessException(504, "Wenku8 下载源响应超时");
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new BusinessException(503, "Wenku8 下载被中断");
        } catch (Exception error) {
            throw new BusinessException(503, "Wenku8 下载源不可用：" + error.getMessage());
        }
    }

    /** CF 质询/封禁页会以 403/503 返回 HTML，检测特征串即可与正常响应区分。 */
    private boolean isCloudflareChallenge(int status, byte[] body) {
        if (status != 403 && status != 503) return false;
        if (body == null || body.length == 0) return false;
        String head = new String(body, 0, Math.min(body.length, 4096), java.nio.charset.StandardCharsets.UTF_8).toLowerCase();
        return head.contains("just a moment") || head.contains("cf-error-details")
            || head.contains("access denied") || head.contains("used cloudflare to restrict access");
    }
}
```

- [ ] **Step 4: 注册配置类**

修改 `backend/src/main/java/com/sakuya/backend/SakuyaBackendApplication.java`：加入 import 与注册。

```java
import com.sakuya.backend.content.download.DownloadSourceProperties;
```

```java
@EnableConfigurationProperties({
    Wenku8Properties.class, ReleaseManagementProperties.class, Wenku8CatalogSyncProperties.class,
    ContentManagementProperties.class, ContentStorageProperties.class, DownloadSourceProperties.class
})
```

- [ ] **Step 5: 新增配置项**

修改 `backend/src/main/resources/application.yml`，在 `wenku8:` 段落下新增（与现有 `adapter:` 同级）：

```yaml
  # CDN 下载源：按 aid 直接取整本 UTF-8 TXT，不经过需要 Cloudflare 验证的站点域名。
  # 默认开启：它只在「本地无正文」时被调用，且不携带凭据、不产生登录行为。
  download-source:
    enabled: ${WENKU8_DOWNLOAD_SOURCE_ENABLED:true}
    connect-timeout-seconds: ${WENKU8_DOWNLOAD_CONNECT_TIMEOUT_SECONDS:5}
    read-timeout-seconds: ${WENKU8_DOWNLOAD_READ_TIMEOUT_SECONDS:30}
    max-bytes: ${WENKU8_DOWNLOAD_MAX_BYTES:67108864}
```

- [ ] **Step 6: 运行测试确认通过**

```bash
cd /Users/xiaoye/FlutterProjects/sakuyainandroid && ./gradlew -p backend test --tests "com.sakuya.backend.content.download.*"
```

预期：`ChapterSplitterTest` 8 个 + `DownloadSourceClientTest` 6 个全部 PASS。

- [ ] **Step 7: 提交**

```bash
cd /Users/xiaoye/FlutterProjects/sakuyainandroid
git add backend/src/main/java/com/sakuya/backend/content/download/ \
        backend/src/main/java/com/sakuya/backend/SakuyaBackendApplication.java \
        backend/src/main/resources/application.yml \
        backend/src/test/java/com/sakuya/backend/content/download/DownloadSourceClientTest.java
git commit -m "新增 CDN 下载源客户端：按 aid 取整本 TXT 并按节点回退"
```

---

### Task 3: DownloadSourceContentProvider 适配层

**Files:**
- Create: `backend/src/main/java/com/sakuya/backend/content/download/DownloadSourceContentProvider.java`
- Test: `backend/src/test/java/com/sakuya/backend/content/download/DownloadSourceContentProviderTest.java`

**Interfaces:**
- Consumes:
  - `ChapterSplitter.split(String) -> SplitResult`（Task 1）
  - `DownloadSourceClient.download(int) -> byte[]`（Task 2）
- Produces:
  - `@Component class DownloadSourceContentProvider implements ContentProvider`
  - `id()` 返回 `"WENKU8_CDN"`
  - `static boolean supportsAid(String bookId)` —— 供 FallbackFacade 判断
  - `book` / `volumes` / `chapterContent` / `catalog` / `cover`

- [ ] **Step 1: 写失败的测试**

创建 `backend/src/test/java/com/sakuya/backend/content/download/DownloadSourceContentProviderTest.java`：

```java
package com.sakuya.backend.content.download;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sakuya.backend.content.ContentProvider;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * DownloadSourceContentProviderTest.java
 * 职责说明：验证下载源 provider 的 aid 识别、切分结果映射与降级行为，使用桩客户端避免真实网络。
 * 执行流程：以真实样本构造桩下载器 -> 调用 provider -> 断言卷章结构与偏移可回读。
 */
class DownloadSourceContentProviderTest {

    private static byte[] sampleBytes() throws Exception {
        try (InputStream in = DownloadSourceContentProviderTest.class.getResourceAsStream("/chapter-splitter/wenku8-1-head.txt")) {
            return in.readAllBytes();
        }
    }

    private DownloadSourceContentProvider providerWith(byte[] payload) {
        DownloadSourceProperties properties = new DownloadSourceProperties(true, 5, 30, 64 * 1024 * 1024);
        DownloadSourceClient client = new DownloadSourceClient(java.net.http.HttpClient.newHttpClient(), properties) {
            @Override public byte[] download(int aid) { return payload; }
        };
        return new DownloadSourceContentProvider(client);
    }

    @Test
    void supportsAid只接受纯数字() {
        assertThat(DownloadSourceContentProvider.supportsAid("wenku8-1234")).isTrue();
        assertThat(DownloadSourceContentProvider.supportsAid("1234")).isTrue();
        assertThat(DownloadSourceContentProvider.supportsAid("biblia")).isFalse();
        assertThat(DownloadSourceContentProvider.supportsAid("danmachi")).isFalse();
        assertThat(DownloadSourceContentProvider.supportsAid(null)).isFalse();
    }

    @Test
    void volumes按卷分组且章节顺序稳定() throws Exception {
        DownloadSourceContentProvider provider = providerWith(sampleBytes());
        List<ContentProvider.ProviderVolume> volumes = provider.volumes("1");
        assertThat(volumes).hasSize(2);
        assertThat(volumes.get(0).title()).isEqualTo("第一卷 渴望死亡的小丑");
        // 样本切分 19 章（第一卷 8、第二卷 11）。样本每章正文被截到 300 字符，
        // 其中的「插图」章内容是下一章开头的残留而非图片链接，因此不会被过滤，数字与切分数一致。
        assertThat(volumes.get(0).chapters()).hasSize(8);
        assertThat(volumes.get(1).chapters()).hasSize(11);
    }

    @Test
    void 章节正文可回读且不含标题行() throws Exception {
        DownloadSourceContentProvider provider = providerWith(sampleBytes());
        ContentProvider.ProviderChapter first = provider.volumes("1").get(0).chapters().get(0);
        String content = provider.chapterContent("1", first.externalChapterId());
        assertThat(content).isNotBlank();
        assertThat(content).doesNotStartWith("第一卷");
    }

    @Test
    void catalog不支持目录枚举() {
        DownloadSourceContentProvider provider = providerWith(new byte[0]);
        assertThatThrownBy(() -> provider.catalog("lastupdate"))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("目录枚举");
    }

    @Test
    void 切分降级时返回单章整本() {
        byte[] plain = "没有任何章节标记的纯文本。".getBytes(StandardCharsets.UTF_8);
        DownloadSourceContentProvider provider = providerWith(plain);
        List<ContentProvider.ProviderVolume> volumes = provider.volumes("1");
        assertThat(volumes).hasSize(1);
        assertThat(volumes.get(0).chapters()).hasSize(1);
    }

    @Test
    void id为稳定的来源标识() {
        assertThat(providerWith(new byte[0]).id()).isEqualTo("WENKU8_CDN");
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
cd /Users/xiaoye/FlutterProjects/sakuyainandroid && ./gradlew -p backend test --tests "com.sakuya.backend.content.download.DownloadSourceContentProviderTest"
```

预期：编译失败，`DownloadSourceContentProvider` 不存在，且 `DownloadSourceClient` 需可被继承（去掉 `final`，其 `download` 已是 `public`）。

- [ ] **Step 3: 实现 Provider**

创建 `backend/src/main/java/com/sakuya/backend/content/download/DownloadSourceContentProvider.java`：

```java
package com.sakuya.backend.content.download;

import com.sakuya.backend.common.BusinessException;
import com.sakuya.backend.content.ContentProvider;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * DownloadSourceContentProvider.java
 * 职责说明：把 CDN 整本 TXT 适配为统一的 ContentProvider 卷章协议，供 ContentImportService 落库。
 * 执行流程：下载整本 -> 离线切分 -> 按卷分组为 ProviderVolume -> 按偏移截取章节正文。
 *
 * 与该 provider 并列的 Wenku8ContentProvider 走内部适配器（受 Cloudflare 影响），
 * 本实现直接访问 CDN，不携带凭据，用于「本地无正文」时的按需回填。
 * 下载源没有目录枚举接口，因此 catalog() 无法实现，批量导入仍只能走适配器。
 */
@Component
public class DownloadSourceContentProvider implements ContentProvider {

    private final DownloadSourceClient client;

    public DownloadSourceContentProvider(DownloadSourceClient client) { this.client = client; }

    @Override public String id() { return "WENKU8_CDN"; }

    /** 下载源只能按数字 aid 取单本；13 本短 slug 书没有 aid，必须明确排除。 */
    public static boolean supportsAid(String bookId) {
        if (bookId == null || bookId.isBlank()) return false;
        String digits = bookId.startsWith("wenku8-") ? bookId.substring("wenku8-".length()) : bookId;
        return digits.matches("\\d{1,9}");
    }

    static int aidOf(String bookId) {
        String digits = bookId.startsWith("wenku8-") ? bookId.substring("wenku8-".length()) : bookId;
        return Integer.parseInt(digits);
    }

    @Override
    public List<ProviderCatalogItem> catalog(String mode) {
        throw new UnsupportedOperationException("下载源没有目录枚举接口，无法批量导入；请使用适配器 provider");
    }

    /** 元数据以本地库为准，这里只提供最简结构，避免为导入而多发一次上游请求。 */
    @Override
    public ProviderBook book(String externalBookId) {
        return new ProviderBook(externalBookId, "", "", "", "", List.of(), false);
    }

    @Override
    public List<ProviderVolume> volumes(String externalBookId) {
        if (!supportsAid(externalBookId)) throw new BusinessException(400, "该小说没有可用的下载标识");
        String text = new String(client.download(aidOf(externalBookId)), StandardCharsets.UTF_8);
        ChapterSplitter.SplitResult result = ChapterSplitter.split(text);
        // 按卷名分组，保持首次出现顺序，使目录顺序与原文一致。
        Map<String, List<ChapterSplitter.SplitChapter>> grouped = new LinkedHashMap<>();
        for (ChapterSplitter.SplitChapter chapter : result.chapters()) {
            grouped.computeIfAbsent(chapter.volumeTitle(), ignored -> new ArrayList<>()).add(chapter);
        }
        List<ProviderVolume> volumes = new ArrayList<>();
        int volumeOrder = 0;
        for (Map.Entry<String, List<ChapterSplitter.SplitChapter>> entry : grouped.entrySet()) {
            List<ProviderChapter> chapters = new ArrayList<>();
            int chapterOrder = 0;
            for (ChapterSplitter.SplitChapter chapter : entry.getValue()) {
                chapters.add(new ProviderChapter(String.valueOf(chapter.offset()), chapter.title(), chapterOrder++));
            }
            String volumeTitle = entry.getKey().isBlank() ? "" : entry.getKey();
            volumes.add(new ProviderVolume("v" + volumeOrder++, volumeTitle, List.copyOf(chapters)));
        }
        return List.copyOf(volumes);
    }

    /**
     * 章节正文 = 从本章偏移起、到下一章偏移止的文本，去掉首行的「卷名 标题」标题行。
     * 依赖切分结果的偏移，不重新下载，因此同一本书的多章读取只产生一次网络请求。
     */
    @Override
    public String chapterContent(String externalBookId, String externalChapterId) {
        downloadCache.computeIfAbsent(externalBookId, id -> new String(client.download(aidOf(id)), StandardCharsets.UTF_8));
        String text = downloadCache.get(externalBookId);
        ChapterSplitter.SplitResult result = ChapterSplitter.split(text);
        int offset = Integer.parseInt(externalChapterId);
        int end = text.length();
        for (ChapterSplitter.SplitChapter chapter : result.chapters()) {
            if (chapter.offset() > offset) { end = chapter.offset(); break; }
        }
        String block = text.substring(offset, end);
        int newline = block.indexOf('\n');
        return newline >= 0 ? block.substring(newline + 1).trim() : block.trim();
    }

    @Override
    public byte[] cover(String externalBookId) { return new byte[0]; }

    /** 单次导入内的下载缓存，避免 volumes 与 chapterContent 重复下载同一本。 */
    private final Map<String, String> downloadCache = new java.util.concurrent.ConcurrentHashMap<>();
}
```

- [ ] **Step 4: 运行测试确认通过**

```bash
cd /Users/xiaoye/FlutterProjects/sakuyainandroid && ./gradlew -p backend test --tests "com.sakuya.backend.content.download.*"
```

预期：全部 PASS（8 + 6 + 6 = 20 个）。

- [ ] **Step 5: 提交**

```bash
cd /Users/xiaoye/FlutterProjects/sakuyainandroid
git add backend/src/main/java/com/sakuya/backend/content/download/DownloadSourceContentProvider.java \
        backend/src/test/java/com/sakuya/backend/content/download/DownloadSourceContentProviderTest.java
git commit -m "新增下载源 ContentProvider：整本 TXT 适配为统一卷章协议"
```

---

### Task 3.5: 过滤无正文的「插图」类章节

实测发现《文学少女》第 21 章是「插图」，其"正文"只有 `<!--image-->https://...` 图片链接，没有真实文本。
`ContentImportService.java:70` 对空正文会抛 `章节正文为空` 并**中断整本导入**，因此必须在 provider 层过滤掉这类章节。

**Files:**
- Modify: `backend/src/main/java/com/sakuya/backend/content/download/DownloadSourceContentProvider.java`
- Modify: `backend/src/test/java/com/sakuya/backend/content/download/DownloadSourceContentProviderTest.java`

**Interfaces:**
- Consumes: Task 3 的 `DownloadSourceContentProvider`
- Produces: `volumes()` 不再返回正文为空的章节；过滤后仍不足 2 章则整本作为单章

**样本说明：** 现有的 `wenku8-1-head.txt` 只含前 2 卷、且每章正文被截到 300 字符，
其中的「插图」章内容是下一章开头的残留，**无法用于测试过滤逻辑**。
因此本任务需**新增一个专门的小样本**，直接构造含图片链接章节的文本。

- [ ] **Step 1: 新增过滤测试样本**

创建 `backend/src/test/resources/chapter-splitter/with-image-chapters.txt`：

```
第一卷 测试卷 第一章 有正文的一章
这是第一章的正文内容，足够长以通过非空校验。

第一卷 测试卷 插图
<!--image-->https://pic.example.xyz/0/1/1.jpg<!--image-->

第一卷 测试卷 第二章 另一章正文
这是第二章的正文内容，同样非空。

第二卷 第二卷名 第一章 跨卷章节
第二卷的正文内容。
```

- [ ] **Step 2: 写失败的测试**

在 `DownloadSourceContentProviderTest.java` 中新增：

```java
    private DownloadSourceContentProvider providerWithResource(String resource) throws Exception {
        try (InputStream in = DownloadSourceContentProviderTest.class.getResourceAsStream(resource)) {
            if (in == null) throw new IllegalStateException("测试样本缺失：" + resource);
            return providerWith(in.readAllBytes());
        }
    }

    @Test
    void 插图类章节被过滤() throws Exception {
        DownloadSourceContentProvider provider = providerWithResource("/chapter-splitter/with-image-chapters.txt");
        List<ContentProvider.ProviderChapter> all = provider.volumes("1").stream()
            .flatMap(volume -> volume.chapters().stream()).toList();
        // 样本共 4 个章节标记，其中「插图」正文仅为图片链接，必须被过滤后剩 3 个。
        assertThat(all).extracting(ContentProvider.ProviderChapter::title).doesNotContain("插图");
        assertThat(all).hasSize(3);
    }

    @Test
    void 图片链接章节被识别为无正文() throws Exception {
        DownloadSourceContentProvider provider = providerWithResource("/chapter-splitter/with-image-chapters.txt");
        for (ContentProvider.ProviderVolume volume : provider.volumes("1")) {
            for (ContentProvider.ProviderChapter chapter : volume.chapters()) {
                String body = provider.chapterContent("1", chapter.externalChapterId());
                assertThat(body).as("章节「%s」不应只含图片链接", chapter.title()).doesNotContain("<!--image-->");
            }
        }
    }
```

- [ ] **Step 3: 运行测试确认失败**

```bash
cd /Users/xiaoye/FlutterProjects/sakuyainandroid && ./gradlew -p backend test --tests "com.sakuya.backend.content.download.DownloadSourceContentProviderTest"
```

预期：`插图类章节被过滤` 失败（实际 4 章、含「插图」），确认过滤逻辑尚未实现。

- [ ] **Step 3: 实现过滤**

在 `DownloadSourceContentProvider.java` 中，把 `volumes()` 的章节组装改为先取正文再判空。将原循环体：

```java
            for (ChapterSplitter.SplitChapter chapter : entry.getValue()) {
                chapters.add(new ProviderChapter(String.valueOf(chapter.offset()), chapter.title(), chapterOrder++));
            }
```

替换为：

```java
            String text = cachedText(externalBookId);
            for (ChapterSplitter.SplitChapter chapter : entry.getValue()) {
                // 「插图」类章节只有图片链接、没有正文，而 importBook 对空正文会中断整本导入，
                // 必须在 provider 层丢弃，否则一本书里出现一张插图就会导致整本无法入库。
                String body = extractBody(text, result, chapter);
                if (hasNoReadableText(body)) continue;
                chapters.add(new ProviderChapter(String.valueOf(chapter.offset()), chapter.title(), chapterOrder++));
            }
```

并抽出两个私有方法，供 `volumes` 与 `chapterContent` 共用（消除重复下载与重复截取）：

```java
    /** 单次导入内的下载缓存，避免 volumes 与 chapterContent 重复下载同一本。 */
    private final Map<String, String> downloadCache = new java.util.concurrent.ConcurrentHashMap<>();

    private String cachedText(String externalBookId) {
        return downloadCache.computeIfAbsent(externalBookId,
            id -> new String(client.download(aidOf(id)), StandardCharsets.UTF_8));
    }

    /** 截取某章正文：从本章偏移到下一章偏移，去掉首行的「卷名 标题」标题行。 */
    private String extractBody(String text, ChapterSplitter.SplitResult result, ChapterSplitter.SplitChapter chapter) {
        int end = text.length();
        for (ChapterSplitter.SplitChapter candidate : result.chapters()) {
            if (candidate.offset() > chapter.offset()) { end = candidate.offset(); break; }
        }
        String block = text.substring(chapter.offset(), end);
        int newline = block.indexOf('\n');
        return newline >= 0 ? block.substring(newline + 1).trim() : block.trim();
    }

    /**
     * 判断章节是否没有可阅读文本。
     * 实测《文学少女》每卷都有一个「插图」章节，其内容形如
     * {@code <!--image-->https://pic.example/1.jpg<!--image-->}，只有图片链接而无正文。
     * 这类章节若进入导入流程，会因正文为空而中断整本入库，必须提前丢弃。
     */
    private boolean hasNoReadableText(String body) {
        if (body.isBlank()) return true;
        // 去掉全部图片标记与链接后若再无内容，则视为无可阅读文本。
        String stripped = body.replaceAll("<!--image-->", "")
                             .replaceAll("https?://\\S+", "")
                             .replaceAll("\\s", "");
        return stripped.isEmpty();
    }
```

把 `chapterContent` 改为复用：

```java
    @Override
    public String chapterContent(String externalBookId, String externalChapterId) {
        String text = cachedText(externalBookId);
        ChapterSplitter.SplitResult result = ChapterSplitter.split(text);
        int offset = Integer.parseInt(externalChapterId);
        for (ChapterSplitter.SplitChapter chapter : result.chapters()) {
            if (chapter.offset() == offset) return extractBody(text, result, chapter);
        }
        return "";
    }
```

同时把 Task 3 中加入的 `downloadCache` 字段声明**删除**（已移到上面的方法旁，避免重复声明）。

- [ ] **Step 4: 运行测试确认通过**

```bash
cd /Users/xiaoye/FlutterProjects/sakuyainandroid && ./gradlew -p backend test --tests "com.sakuya.backend.content.download.*"
```

预期：全部 PASS（Task 1 的 12 个 + Task 2 的 6 个 + Task 3/3.5 的 8 个）。

- [ ] **Step 5: 在真实全文上验证过滤效果**

用原始全文（非样本）统计过滤前后的章节数：

```bash
cd /Users/xiaoye/FlutterProjects/sakuyainandroid && python3 - <<'PY'
import re
text = open('content-store/books/wenku8-1/revisions/39379624-bf1c-41fc-a1ff-890a2689e9d5/full.txt', encoding='utf-8').read()
PAT = re.compile(r"^(\S.*?)\s+(序章|第[一二三四五六七八九十百零〇\d]+章|终章|后记|插图|尾声|番外)(?:\s+(.*))?$")
ch, off = [], 0
for line in text.split('\n'):
    if line and len(line) <= 80:
        m = PAT.match(line)
        if m and m.group(1).strip():
            r = (m.group(3) or '').strip()
            ch.append((m.group(1).strip(), m.group(2) + (' ' + r if r else ''), off))
    off += len(line) + 1

def body(o):
    end = len(text)
    for _, _, x in ch:
        if x > o: end = x; break
    b = text[o:end]; nl = b.find('\n')
    return (b[nl+1:].strip() if nl >= 0 else b.strip())

no_text = lambda b: not re.sub(r'https?://\S+|\s|<!--image-->', '', b)
kept = [t for _, t, o in ch if not no_text(body(o))]
dropped = [t for _, t, o in ch if no_text(body(o))]
print(f"原始章节数: {len(ch)}   卷数: {len({v for v, _, _ in ch})}")
print(f"保留: {len(kept)}   丢弃: {len(dropped)} -> {sorted(set(dropped))}")
PY
```

预期：原始 **127 章 / 18 卷**，丢弃 **10 章**（全部为「插图」），保留 **117 章**。

若丢弃数不是 10，检查是否还有其他类型的无正文内容，并据此调整 `hasNoReadableText`。

- [ ] **Step 6: 提交**

```bash
cd /Users/xiaoye/FlutterProjects/sakuyainandroid
git add backend/src/main/java/com/sakuya/backend/content/download/DownloadSourceContentProvider.java \
        backend/src/test/java/com/sakuya/backend/content/download/DownloadSourceContentProviderTest.java
git commit -m "过滤插图类无正文章节，避免整本导入被中断"
```

- [ ] **Step 5: 提交**

```bash
cd /Users/xiaoye/FlutterProjects/sakuyainandroid
git add backend/src/main/java/com/sakuya/backend/content/download/DownloadSourceContentProvider.java \
        backend/src/test/java/com/sakuya/backend/content/download/DownloadSourceContentProviderTest.java
git commit -m "新增下载源 ContentProvider：整本 TXT 适配为统一卷章协议"
```

---

### Task 4: 接入回退链路

**Files:**
- Modify: `backend/src/main/java/com/sakuya/backend/content/ContentFallbackFacade.java`
- Modify: `backend/src/main/java/com/sakuya/backend/content/ContentImportRunner.java`（注册新 provider，无需改逻辑）
- Test: `backend/src/test/java/com/sakuya/backend/content/download/DownloadSourceFallbackTest.java`

**Interfaces:**
- Consumes: `DownloadSourceContentProvider`（Task 3）、`ContentFallbackFacade`（现有）
- Produces: 回退顺序变为「下载源优先，适配器兜底」

- [ ] **Step 1: 写失败的测试**

创建 `backend/src/test/java/com/sakuya/backend/content/download/DownloadSourceFallbackTest.java`：

```java
package com.sakuya.backend.content.download;

import static org.assertj.core.api.Assertions.assertThat;

import com.sakuya.backend.content.ContentImportRunner;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * DownloadSourceFallbackTest.java
 * 职责说明：验证新 provider 已注册进导入器，且其 id 与适配器 provider 互不冲突。
 * 执行流程：构造仅含新 provider 的导入器 -> 断言 supports 判定正确。
 */
class DownloadSourceFallbackTest {

    @Test
    void 导入器识别下载源provider标识() {
        DownloadSourceProperties properties = new DownloadSourceProperties(true, 5, 30, 64 * 1024 * 1024);
        DownloadSourceClient client = new DownloadSourceClient(java.net.http.HttpClient.newHttpClient(), properties);
        ContentImportRunner runner = new ContentImportRunner(null, List.of(new DownloadSourceContentProvider(client)), null);
        assertThat(runner.supports("WENKU8_CDN")).isTrue();
        assertThat(runner.supports("wenku8_cdn")).isTrue();
        // 适配器 provider 的标识仍独立可用，二者不冲突。
        assertThat(runner.supports("WENKU8")).isFalse();
    }
}
```

- [ ] **Step 2: 运行测试确认通过**

```bash
cd /Users/xiaoye/FlutterProjects/sakuyainandroid && ./gradlew -p backend test --tests "com.sakuya.backend.content.download.DownloadSourceFallbackTest"
```

预期：PASS。`ContentImportRunner` 构造器已接受 `List<ContentProvider>` 并按键大写建索引，`@Component` 会让新 provider 自动被注入 —— 这一步主要是锁定该行为不被破坏。

- [ ] **Step 3: 修改 FallbackFacade 优先走下载源**

修改 `backend/src/main/java/com/sakuya/backend/content/ContentFallbackFacade.java`。

先在构造器注入新依赖，把字段与构造参数各加一项：

```java
    private final DownloadSourceContentProvider downloadSource;
```

```java
    public ContentFallbackFacade(ContentReadService database, Wenku8GatewayService gateway, ContentImportRunner imports,
            ContentManagementProperties properties, MeterRegistry metrics, DownloadSourceContentProvider downloadSource) {
        this.database = database; this.gateway = gateway; this.imports = imports; this.properties = properties; this.metrics = metrics;
        this.downloadSource = downloadSource;
    }
```

新增 import：

```java
import com.sakuya.backend.content.download.DownloadSourceContentProvider;
```

把 `fullContent` 方法改为下载源优先：

```java
    public Object fullContent(String bookId) {
        try { Object result = database.fullContent(bookId); hit("full"); return result; }
        catch (BusinessException error) {
            if (error.getCode() != 404 || !fallbackEnabled() || !canFallback(bookId)) throw error;
            // 下载源直连 CDN，不经过 Cloudflare 验证；只有它拿不到时才回退到受 CF 影响的适配器。
            if (DownloadSourceContentProvider.supportsAid(externalId(bookId))) {
                try {
                    imports.backfill("WENKU8_CDN", externalId(bookId));
                    fallback("full");
                    return database.fullContent(bookId);
                } catch (Exception ignored) {
                    // 下载源不可用时继续走适配器，绝不让回退链路整体中断。
                }
            }
            String external = externalId(bookId); var raw = gateway.fullContent(external);
            var anchors = raw.chapters().stream().map(anchor -> new ContentReadService.ChapterAnchorDto(
                anchor.chapterId(), anchor.title(), anchor.volumeTitle(), anchor.offset())).toList();
            imports.backfill("WENKU8", external); fallback("full");
            return new ContentReadService.FullContentDto(canonicalBookId(external), raw.title(), raw.content(), anchors);
        }
    }
```

- [ ] **Step 4: 运行全部 content 测试确认无回归**

```bash
cd /Users/xiaoye/FlutterProjects/sakuyainandroid && ./gradlew -p backend test --tests "com.sakuya.backend.content.*"
```

预期：全部 PASS，包括既有的 `ContentWithdrawalIntegrationTest`（7 个）。该测试断言"适配器关闭时任何回退都以 503 暴露"，新增的下载源分支在 aid 不可解析时不会触发，因此不影响它。

- [ ] **Step 5: 运行完整测试套件**

```bash
cd /Users/xiaoye/FlutterProjects/sakuyainandroid && ./gradlew -p backend test
```

预期：原有 48 个测试 + 新增 21 个 = 69 个全部 PASS，0 failures。

- [ ] **Step 6: 提交**

```bash
cd /Users/xiaoye/FlutterProjects/sakuyainandroid
git add backend/src/main/java/com/sakuya/backend/content/ContentFallbackFacade.java \
        backend/src/test/java/com/sakuya/backend/content/download/DownloadSourceFallbackTest.java
git commit -m "回退链路改为下载源优先：CDN 拿不到时才走适配器"
```

---

### Task 5: 端到端验证（人工，需网络）

前三步是自动化测试，这一步验证真实 CDN 可达性与正则泛化性 —— 这是 spec 中标记的最大未决风险。

**Files:** 无代码改动；产出验证记录。

- [ ] **Step 1: 确认测试通过后的后端可启动**

```bash
cd /Users/xiaoye/FlutterProjects/sakuyainandroid && ./gradlew -p backend test
```

预期：69 个测试全部 PASS。

- [ ] **Step 2: 抽样验证切分正则的泛化性**

对 3 本不同的书各下载一次并统计切分结果，确认非《文学少女》的书也能正确切分：

```bash
cd /tmp && for aid in 1011 3020 2580; do
  url="https://dl1.wenku8.com/txtutf8/$((aid/1000))/$aid.txt"
  code=$(curl -s -o "/tmp/wk-$aid.txt" -w '%{http_code}' -A 'Mozilla/5.0' "$url")
  echo "aid=$aid HTTP=$code 字节=$(wc -c < /tmp/wk-$aid.txt 2>/dev/null)"
done
```

预期：HTTP 200，各文件数 MB。

- [ ] **Step 3: 用切分器统计每本的章节数**

对每本书运行切分并打印章节数与卷数，确认不是 0 或 1（那意味着降级）：

```bash
cd /Users/xiaoye/FlutterProjects/sakuyainandroid
for aid in 1011 3020 2580; do
  printf "aid=%-6s 章节数=%-5s 卷数=%s\n" "$aid" \
    "$(grep -cE '^第[一二三四五六七八九十百零〇0-9]+卷 ' /tmp/wk-$aid.txt)" \
    "$(grep -oE '^第[一二三四五六七八九十百零〇0-9]+卷 ' /tmp/wk-$aid.txt | sort -u | wc -l | tr -d ' ')"
done
```

预期：章节数明显大于 1。若某本为 0 或 1，说明该本格式与《文学少女》不同，会走降级路径 —— **记录该书的 aid 与 TXT 特征行**，作为后续修正正则的依据，但不必阻塞本次交付。

- [ ] **Step 4: 端到端验证一本书能读**

启动后端（由用户在**自己的终端**执行，脚本会保留凭据在环境变量中）：

```bash
cd /Users/xiaoye/FlutterProjects/sakuyainandroid && ./backend/scripts/run-wenku8-local.sh
```

然后在 App 或 curl 中读取一本此前无正文的书：

```bash
# 触发回退与异步入库
curl -s -m 120 'http://127.0.0.1:8080/content/novels/wenku8-3020/full-content' | head -c 200
# 等待异步导入完成后复查数据库
mysql -uroot -p123456 sakuya -N -e "SELECT id, full_content_byte_size FROM books WHERE id='wenku8-3020';"
```

预期：首次请求触发下载与入库；`full_content_byte_size` 从 0 变为数 MB。

- [ ] **Step 5: 记录验证结果**

把 §Step 2/3 的实测数据补充到 spec 的「未决风险」一节，替换掉"仅在 1 本书上验证"的表述，改为实际的抽样本数与结论。

```bash
cd /Users/xiaoye/FlutterProjects/sakuyainandroid
git add docs/superpowers/specs/2026-09-14-download-source-provider-design.md
git commit -m "补充切分正则的抽样验证结果"
```

---

## 自检记录

**Spec 覆盖：**
- 新增 `DownloadSourceContentProvider` → Task 3
- `DownloadSourceClient`（节点回退、CF 检测、UTF-8） → Task 2
- `ChapterSplitter`（纯函数、偏移规则、降级） → Task 1
- 偏移对齐 `ContentImportService.java:72` → Task 1 Step 1 的 `按偏移回读的文本以卷名加标题开头` 测试
- 13 本短 slug 不可下载 → Task 3 `supportsAid` 及其测试
- `catalog()` 抛 `UnsupportedOperationException` → Task 3
- 失败回退适配器 → Task 4 Step 3
- 切分降级不抛错 → Task 1 `degraded`
- 测试样本 → 已生成于 `backend/src/test/resources/chapter-splitter/wenku8-1-head.txt`
- 泛化性风险验证 → Task 5

**类型一致性：** `SplitChapter(volumeTitle, title, offset)` 与 `ProviderChapter(externalChapterId, title, order)` 的字段在 Task 1/3 间一致；`supportsAid` 在 Task 3 定义、Task 4 使用，签名一致；`DownloadSourceClient.download(int)` 在 Task 2 定义、Task 3 覆写，签名一致。
