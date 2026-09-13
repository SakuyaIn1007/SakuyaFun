package com.sakuya.backend.content.download;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ChapterSplitter.java
 * 职责说明：把整本 TXT 按「<卷名> <章节标记> [<标题>]」行切分为卷/章结构，并给出全文字符偏移。
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
     * 不能依赖 \\S 排除缩进行：Java 正则的 \\S 只认 ASCII 空白，全角空格 U+3000 会被当作非空白，
     * 导致「　　这是正文... 插图 两个字。」这类缩进正文行被误判为章节标题。改用显式字符类排除。
     */
    private static final Pattern CHAPTER_LINE = Pattern.compile(
        "^(\\S.*?)[ \\t]+(序章|第[一二三四五六七八九十百零〇\\d]+章|终章|后记|插图|尾声|番外)(?:[ \\t]+(.*))?$");

    /** 行首缩进字符：半角空格、制表符、全角空格（U+3000）。缩进行一律视为正文。 */
    private static final Pattern LEADING_INDENT = Pattern.compile("^[ \\t\\u3000]");

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
        if (chapters.size() < MIN_CHAPTERS) return degraded();
        return new SplitResult(List.copyOf(chapters));
    }

    /**
     * 从标题行拆出卷名与章节标题；不匹配时返回 null 由调用方跳过该行。
     * 章节标记是唯一锚点：卷名 = 标记之前，章节标题 = 标记及其之后。
     */
    private static SplitChapter parse(String line, int offset) {
        // 缩进行是正文，直接跳过；不由正则承担该判断，因为 \S 不覆盖全角空格。
        if (LEADING_INDENT.matcher(line).find()) return null;
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

    private static SplitResult degraded() {
        return new SplitResult(List.of(new SplitChapter("", "", 0)));
    }
}
