package com.sakuya.backend.wenku8;

import com.sakuya.backend.common.BusinessException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Wenku8GatewayService.java
 * 职责说明：对客户端暴露稳定的 Wenku8 读取用例，隔离适配服务 DTO 和第三方实现细节。
 * 执行流程：Controller 校验 HTTP 参数 -> 本服务约束业务参数 -> AdapterClient 获取内部数据 -> DTO 返回客户端。
 */
@Service
public class Wenku8GatewayService {
    private final Wenku8AdapterClient client;
    public Wenku8GatewayService(Wenku8AdapterClient client) { this.client = client; }
    public SearchPage search(String keyword, int page) { String q = keyword == null ? "" : keyword.trim(); if (q.isEmpty()) throw new BusinessException(400, "请输入搜索关键词"); Map<String, Object> raw = client.get("/novels/search?keyword=" + Wenku8AdapterClient.query(q) + "&page=" + page); return new SearchPage(list(raw.get("items")), integer(raw.get("nextPage"))); }
    public Map<String, Object> novel(String id) { return client.get("/novels/" + id(id)); }
    public SearchPage list(String sort) {
        String safeSort = switch (sort) { case "lastupdate", "weekvote", "allvote" -> sort; default -> throw new BusinessException(400, "不支持的 Wenku8 排序方式"); };
        Map<String, Object> raw = client.get("/catalog/novels?sort=" + safeSort + "&page=0");
        return new SearchPage(list(raw.get("items")), integer(raw.get("nextPage")));
    }
    public Map<String, Object> chapters(String id) { return client.get("/novels/" + id(id) + "/chapters"); }
    public Map<String, Object> content(String novelId, String chapterId) { return client.get("/chapters/" + id(chapterId) + "/content?novelId=" + Wenku8AdapterClient.query(id(novelId))); }
    /**
     * 连续阅读只读取 Python 内存中的全文，不写入 MySQL；将内部 Map 映射为固定 DTO，
     * 防止 pywenku8api 的字段变动直接泄露到 Android 契约。
     */
    public FullContentDocument fullContent(String novelId) {
        String safeNovelId = id(novelId);
        Map<String, Object> raw = client.getFullContent("/novels/" + safeNovelId + "/full-content");
        String content = requiredText(raw.get("content"), "Wenku8 全文内容为空");
        return new FullContentDocument(
            safeNovelId,
            requiredText(raw.get("title"), "Wenku8 全文响应缺少小说标题"),
            content,
            anchors(raw.get("chapters"))
        );
    }
    public String coverPath(String id) { return "/novels/" + id(id) + "/cover"; }
    public byte[] cover(String id) { return client.getBytes(coverPath(id)); }
    private String id(String value) { if (value == null || !value.matches("[A-Za-z0-9_-]{1,80}")) throw new BusinessException(400, "小说或章节标识不合法"); return value; }
    @SuppressWarnings("unchecked") private List<Map<String, Object>> list(Object value) { return value instanceof List<?> list ? (List<Map<String, Object>>) list : List.of(); }
    private Integer integer(Object value) { return value instanceof Number number ? number.intValue() : null; }
    private String requiredText(Object value, String message) {
        if (!(value instanceof String text) || text.isBlank()) throw new BusinessException(503, message);
        return text;
    }
    private List<ChapterAnchor> anchors(Object value) {
        if (!(value instanceof List<?> rawAnchors)) return List.of();
        List<ChapterAnchor> result = new ArrayList<>();
        for (Object rawAnchor : rawAnchors) {
            if (!(rawAnchor instanceof Map<?, ?> anchor)) continue;
            Object chapterId = anchor.get("chapterId");
            Object title = anchor.get("title");
            if (!(chapterId instanceof String chapter) || chapter.isBlank() || !(title instanceof String name) || name.isBlank()) continue;
            Object offset = anchor.get("offset");
            result.add(new ChapterAnchor(chapter, name, anchor.get("volumeTitle") instanceof String volume ? volume : "", offset instanceof Number number ? number.intValue() : -1));
        }
        return List.copyOf(result);
    }
    public record SearchPage(List<Map<String, Object>> items, Integer nextPage) { }
    /** Android 连续阅读的稳定响应；offset=-1 表示客户端应降级为当前章节正文。 */
    public record FullContentDocument(String novelId, String title, String content, List<ChapterAnchor> chapters) { }
    public record ChapterAnchor(String chapterId, String title, String volumeTitle, int offset) { }
}
