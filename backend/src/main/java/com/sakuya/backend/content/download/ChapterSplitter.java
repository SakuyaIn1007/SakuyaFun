package com.sakuya.backend.content.download;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ChapterSplitter.java
 * 职责说明：把整本 TXT 切分为卷/章结构，并给出每章在全文中的字符偏移。
 * 执行流程：先用「已知章节标记」bootstrap 出全部卷名 -> 剪掉是其他卷名扩展的碎片 ->
 *           再用卷名把该卷下的所有章节标题行扩展出来。
 *
 * 为什么是两阶段：章节标题的命名方式无法穷举（实测存在「文学少女和恋爱的牛魔王」
 * 「★相逢的小故事 …」「“文学少女”和被杀的笨蛋」等自由形式），只用标记集合会漏掉
 * 36% 的章节。但每个卷都至少含一个「后记」或「插图」章——这两个标记是稳定的，
 * 因此标记法虽漏章节，却足以发现全部卷名；有了卷名，章节边界就确定了。
 *
 * 偏移规则必须与 ContentImportService 的全文拼接保持一致（卷名 + 空格 + 标题 + 换行），
 * 否则 full_text_offset 会错位、章节跳转落到错误位置。
 * 该算法已在《文学少女》真实 TXT 上验证：198 章 / 17 卷 / 偏移回读 0 失配。
 */
public final class ChapterSplitter {

    /** 用于 bootstrap 卷名的已知标记。不要求穷举章节标题，只要求每个卷至少命中一个。 */
    private static final String KNOWN_MARKERS =
        "序章|第[一二三四五六七八九十百零〇\\d]+章|终章|后记|插图|尾声|番外";

    /** bootstrap 用：<卷名> <已知标记> [<标题>]。 */
    private static final Pattern MARKER_LINE = Pattern.compile(
        "^(\\S.*?)[ \\t]+(" + KNOWN_MARKERS + ")(?:[ \\t]+(.*))?$");

    /**
     * 行首缩进：半角空格、制表符、全角空格（U+3000）、不换行空格（U+00A0）。
     * 不能依赖 \\S 排除缩进行：Java 正则的 \\S 只认 ASCII 空白，U+3000 与 U+00A0
     * 都会被当作非空白，导致缩进正文行被误判为章节标题。
     */
    private static final Pattern LEADING_INDENT = Pattern.compile("^[ \\t\\u3000\\u00A0]");

    /** 行长上限：超过则认为是正文而非章节标题行。 */
    private static final int MAX_HEADER_LENGTH = 80;

    /** 扩展阶段的行长上限更严：卷名已知时，过长的行是正文而非标题。 */
    private static final int MAX_CHAPTER_LENGTH = 60;

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
        // 按 \r?\n 切分，且行内容不含 \r：上游 TXT 的换行符不统一，实测既有 LF 也有 CRLF
        // （抽样 3 本全是 CRLF，而《文学少女》是 LF）。若只按 \n 切，CRLF 文件的每行末尾
        // 会残留 \r，使标题行正则的 $ 失配，导致整本降级为单章。
        // 偏移必须相对原文计算，故行起点由原文位置直接得出，不受 \r 剥离影响。
        List<String> lines = new ArrayList<>();
        List<Integer> offsets = new ArrayList<>();
        int cursor = 0;
        while (cursor <= text.length()) {
            int newline = text.indexOf('\n', cursor);
            int end = newline < 0 ? text.length() : newline;
            int contentEnd = end > cursor && text.charAt(end - 1) == '\r' ? end - 1 : end;
            offsets.add(cursor);
            lines.add(text.substring(cursor, contentEnd));
            if (newline < 0) break;
            cursor = newline + 1;
        }
        int[] lineOffsets = new int[offsets.size()];
        for (int i = 0; i < lineOffsets.length; i++) lineOffsets[i] = offsets.get(i);

        Set<String> volumes = bootstrapVolumes(lines);
        if (volumes.isEmpty()) return degraded();

        List<SplitChapter> chapters = expand(lines, lineOffsets, volumes);
        if (chapters.size() < MIN_CHAPTERS) return degraded();
        return new SplitResult(List.copyOf(chapters));
    }

    /** 阶段一：用已知标记行反推卷名，再剪掉「是其他卷名扩展」的碎片。 */
    private static Set<String> bootstrapVolumes(List<String> lines) {
        Set<String> found = new LinkedHashSet<>();
        for (String line : lines) {
            if (!isCandidateLine(line, MAX_HEADER_LENGTH)) continue;
            Matcher matcher = MARKER_LINE.matcher(line);
            if (matcher.matches()) {
                String volume = matcher.group(1).trim();
                if (!volume.isEmpty()) found.add(volume);
            }
        }
        // 剪枝：`外传三 见习生的毕业 ★文学少女 见习生的寂寞` 这类是「卷名 + 子标题」的碎片，
        // 若保留会让同一卷被拆成两个，且短卷名的那部分章节归错卷。
        Set<String> pruned = new LinkedHashSet<>(found);
        for (String candidate : found) {
            for (String other : found) {
                if (!candidate.equals(other) && candidate.startsWith(other + " ")) {
                    pruned.remove(candidate);
                    break;
                }
            }
        }
        return pruned;
    }

    /** 阶段二：按最短卷名匹配，剩余部分即章节标题；保证同一行只归入一个卷。 */
    private static List<SplitChapter> expand(List<String> lines, int[] lineOffsets, Set<String> volumes) {
        // 卷名按长度升序，使「外传三 见习生的毕业」优先于任何以它为前缀的更长的名字。
        List<String> ordered = new ArrayList<>(volumes);
        ordered.sort((a, b) -> Integer.compare(a.length(), b.length()));
        List<SplitChapter> chapters = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (!isCandidateLine(line, MAX_CHAPTER_LENGTH)) continue;
            for (String volume : ordered) {
                if (!line.startsWith(volume + " ")) continue;
                String title = line.substring(volume.length() + 1).trim();
                if (!title.isEmpty()) chapters.add(new SplitChapter(volume, title, lineOffsets[i]));
                break; // 最短匹配已命中，不再尝试更长的卷名
            }
        }
        return chapters;
    }

    private static boolean isCandidateLine(String line, int maxLength) {
        return !line.isEmpty() && line.length() <= maxLength && !LEADING_INDENT.matcher(line).find();
    }

    private static SplitResult degraded() {
        return new SplitResult(List.of(new SplitChapter("", "", 0)));
    }
}
