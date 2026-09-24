package com.sakuya.backend.content;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
    private final ContentImportStateRepository states;
    private final Map<String, ContentProvider> providers;
    private final ContentImportService importer;

    /**
     * 正在回填的目标集合。回填是异步的，同一本未入库的书在导入完成前会被多个用户反复触发，
     * 每次都整本下载全部章节。本集合把并发请求收敛为一次实际导入。
     * 进程内去重即可满足单实例部署；多实例部署下同一本书至多被重复导入一次，无害。
     */
    private final Set<String> inFlight = ConcurrentHashMap.newKeySet();

    public ContentImportRunner(ContentImportJobRepository jobs, ContentImportStateRepository states,
            List<ContentProvider> providers, ContentImportService importer) {
        this.jobs = jobs; this.states = states; this.importer = importer;
        this.providers = providers.stream().collect(Collectors.toMap(
            provider -> provider.id().toUpperCase(Locale.ROOT), provider -> provider));
    }
    public boolean supports(String provider) { return providers.containsKey(provider); }

    /**
     * 兼容读取命中旧适配器后异步补齐单本作品，不阻塞当前用户响应。
     * 三重抑制：正在导入则跳过、失败退避期内则跳过、导入成功则记状态备查。
     */
    @Async
    public void backfill(String providerId, String externalBookId) {
        if (providerId == null || externalBookId == null || externalBookId.isBlank()) return;
        String normalized = providerId.toUpperCase(Locale.ROOT);
        ContentProvider provider = providers.get(normalized);
        if (provider == null) return;

        String target = normalized + ":" + externalBookId;
        // 并发去重必须早于读状态：状态查询与导入之间仍有窗口，只有 inFlight 能真正互斥。
        if (!inFlight.add(target)) return;
        try {
            if (!retryAllowed(normalized, externalBookId)) return;
            markRunning(normalized, externalBookId);
            importer.importBook(provider, externalBookId);
            markSucceeded(normalized, externalBookId);
        } catch (Exception error) {
            // 失败必须落库：只打日志会让「导入必失败」的作品在每次读取时重新整本下载，永不收敛。
            markFailed(normalized, externalBookId, error);
        } finally {
            inFlight.remove(target);
        }
    }

    private boolean retryAllowed(String providerId, String externalBookId) {
        return states.findByProviderAndExternalBookId(providerId, externalBookId)
            .map(state -> state.retryAllowedAt(Instant.now()))
            .orElse(true);
    }

    /** 状态写入失败不得影响导入本身；导入成功与否才是用户可感知的结果。 */
    private void markRunning(String providerId, String externalBookId) {
        try {
            states.findByProviderAndExternalBookId(providerId, externalBookId)
                .ifPresentOrElse(ContentImportState::markRunning,
                    () -> states.save(new ContentImportState(providerId, externalBookId)));
        } catch (Exception error) {
            log.warn("回填状态写入失败 provider={} externalId={}：{}", providerId, externalBookId, error.getMessage());
        }
    }

    private void markSucceeded(String providerId, String externalBookId) {
        try {
            states.findByProviderAndExternalBookId(providerId, externalBookId)
                .ifPresent(state -> { state.markSucceeded(); states.save(state); });
        } catch (Exception error) {
            log.warn("回填状态写入失败 provider={} externalId={}：{}", providerId, externalBookId, error.getMessage());
        }
    }

    private void markFailed(String providerId, String externalBookId, Exception error) {
        log.warn("内容回填失败 provider={} externalId={}：{}", providerId, externalBookId, error.getMessage());
        try {
            ContentImportState state = states.findByProviderAndExternalBookId(providerId, externalBookId)
                .orElseGet(() -> new ContentImportState(providerId, externalBookId));
            state.markFailed(error.getMessage());
            states.save(state);
        } catch (Exception nested) {
            log.warn("回填失败状态落库异常 provider={} externalId={}：{}", providerId, externalBookId, nested.getMessage());
        }
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
