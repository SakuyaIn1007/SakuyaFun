package com.sakuya.backend.content;

import java.util.List;
import java.util.Set;

/**
 * ContentProvider.java
 * 职责说明：定义后台导入所需的来源无关协议，禁止 Controller 在用户请求线程直接依赖具体爬虫。
 * 执行流程：导入任务枚举目录 -> 读取作品详情和章节 -> 保存到统一内容库。
 */
public interface ContentProvider {
    String id();
    List<ProviderCatalogItem> catalog(String mode);
    ProviderBook book(String externalBookId);
    List<ProviderVolume> volumes(String externalBookId);
    String chapterContent(String externalBookId, String externalChapterId);
    byte[] cover(String externalBookId);

    record ProviderCatalogItem(String externalBookId, Set<String> feeds) { }
    record ProviderBook(String externalBookId, String title, String author, String description,
                        String status, List<String> tags, boolean copyrightRestricted) { }
    record ProviderVolume(String externalVolumeId, String title, List<ProviderChapter> chapters) { }
    record ProviderChapter(String externalChapterId, String title, int order) { }
}
