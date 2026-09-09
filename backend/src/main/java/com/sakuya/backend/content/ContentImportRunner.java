package com.sakuya.backend.content;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 独立异步执行器确保 @Async 经过 Spring 代理，管理接口可以立即返回 jobId。 */
@Service
public class ContentImportRunner {
    private static final Logger log = LoggerFactory.getLogger(ContentImportRunner.class);
    private final ContentImportJobRepository jobs;
    private final Map<String, ContentProvider> providers;
    private final ContentImportService importer;
    public ContentImportRunner(ContentImportJobRepository jobs, List<ContentProvider> providers, ContentImportService importer) {
        this.jobs = jobs; this.importer = importer;
        this.providers = providers.stream().collect(Collectors.toMap(
            provider -> provider.id().toUpperCase(Locale.ROOT), provider -> provider));
    }
    public boolean supports(String provider) { return providers.containsKey(provider); }
    /** 兼容读取命中旧适配器后异步补齐单本作品，不阻塞当前用户响应。 */
    @Async
    public void backfill(String providerId, String externalBookId) {
        ContentProvider provider = providers.get(providerId.toUpperCase(Locale.ROOT));
        if (provider == null) return;
        try { importer.importBook(provider, externalBookId); }
        catch (Exception error) { log.warn("内容回填失败 provider={} externalId={}：{}", providerId, externalBookId, error.getMessage()); }
    }
    @Async
    public void execute(UUID jobId) {
        ContentImportJob job = jobs.findById(jobId).orElseThrow();
        job.start(); jobs.save(job);
        ContentProvider provider = providers.get(job.getProvider());
        int success = 0;
        List<String> errors = new ArrayList<>();
        Map<String, List<String>> feeds = new LinkedHashMap<>();
        try {
            for (ContentProvider.ProviderCatalogItem item : provider.catalog(job.getMode())) {
                try {
                    String bookId = importer.importBook(provider, item.externalBookId());
                    success++;
                    for (String feed : item.feeds()) feeds.computeIfAbsent(feed, ignored -> new ArrayList<>()).add(bookId);
                } catch (Exception error) { errors.add(item.externalBookId() + ": " + error.getMessage()); }
            }
            if (!feeds.isEmpty()) importer.replaceCatalog(feeds);
        } catch (Exception error) { errors.add(error.getMessage()); }
        String joined = String.join("\n", errors);
        job.complete(success, errors.size(), joined.isBlank() ? null : joined.substring(0, Math.min(8000, joined.length())));
        jobs.save(job);
    }
}
