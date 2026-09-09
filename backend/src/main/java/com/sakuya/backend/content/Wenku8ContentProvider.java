package com.sakuya.backend.content;

import com.sakuya.backend.catalog.Wenku8CatalogFeed;
import com.sakuya.backend.wenku8.Wenku8GatewayService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Wenku8 只实现后台 ContentProvider；客户端读取路径由 ContentReadService 统一提供。 */
@Component
public class Wenku8ContentProvider implements ContentProvider {
    private final Wenku8GatewayService gateway;
    public Wenku8ContentProvider(Wenku8GatewayService gateway) { this.gateway = gateway; }
    @Override public String id() { return "WENKU8"; }
    @Override public List<ProviderCatalogItem> catalog(String mode) {
        Map<String, Set<String>> feeds = new LinkedHashMap<>();
        for (Wenku8CatalogFeed feed : Wenku8CatalogFeed.values()) {
            for (Map<String, Object> item : gateway.list(feed.sort()).items()) {
                String externalId = string(item.get("id"));
                if (!externalId.isBlank()) feeds.computeIfAbsent(externalId, ignored -> new LinkedHashSet<>()).add(feed.name());
            }
        }
        return feeds.entrySet().stream().map(entry -> new ProviderCatalogItem(entry.getKey(), Set.copyOf(entry.getValue()))).toList();
    }
    @Override public ProviderBook book(String externalBookId) {
        Map<String, Object> raw = gateway.novel(externalBookId);
        return new ProviderBook(externalBookId, string(raw.get("title")), string(raw.get("author")), string(raw.get("description")),
            string(raw.get("status")), strings(raw.get("tags")), raw.get("copyright") instanceof Boolean value && value);
    }
    @Override public List<ProviderVolume> volumes(String externalBookId) {
        Object rawVolumes = gateway.chapters(externalBookId).get("volumes");
        if (!(rawVolumes instanceof List<?> list)) return List.of();
        List<ProviderVolume> result = new ArrayList<>();
        for (Object value : list) {
            if (!(value instanceof Map<?, ?> volume)) continue;
            List<ProviderChapter> chapters = new ArrayList<>();
            if (volume.get("chapters") instanceof List<?> rawChapters) {
                int fallbackOrder = 0;
                for (Object rawChapter : rawChapters) {
                    if (!(rawChapter instanceof Map<?, ?> chapter)) continue;
                    int order = chapter.get("order") instanceof Number number ? number.intValue() : fallbackOrder;
                    chapters.add(new ProviderChapter(string(chapter.get("id")), string(chapter.get("title")), order));
                    fallbackOrder++;
                }
            }
            result.add(new ProviderVolume(string(volume.get("id")), string(volume.get("title")), List.copyOf(chapters)));
        }
        return List.copyOf(result);
    }
    @Override public String chapterContent(String externalBookId, String externalChapterId) {
        return string(gateway.content(externalBookId, externalChapterId).get("content"));
    }
    @Override public byte[] cover(String externalBookId) { return gateway.cover(externalBookId); }
    private String string(Object value) { return value == null ? "" : String.valueOf(value); }
    private List<String> strings(Object value) { return value instanceof List<?> items ? items.stream().map(String::valueOf).toList() : List.of(); }
}
