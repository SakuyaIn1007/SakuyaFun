package com.sakuya.backend.content.download;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * ChapterSplitterTest.java
 * 职责说明：用《文学少女》真实 TXT 样本锁定切分规则，防止正则与偏移计算被无意改坏。
 * 执行流程：加载测试样本 -> 切分 -> 断言章节数、卷名、偏移递增，以及按偏移回读的文本以卷名加标题开头。
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
        List<Integer> offsets = result.chapters().stream().map(ChapterSplitter.SplitChapter::offset).toList();
        assertThat(offsets).doesNotHaveDuplicates();
        // 同时锁定「标题确实会重复」这一事实，防止有人改用标题做标识。
        List<String> titles = result.chapters().stream().map(ChapterSplitter.SplitChapter::title).toList();
        assertThat(titles.size()).isGreaterThan(Set.copyOf(titles).size());
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
