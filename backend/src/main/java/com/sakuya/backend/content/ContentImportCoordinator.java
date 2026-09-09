package com.sakuya.backend.content;

import com.sakuya.backend.common.BusinessException;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** 创建、执行并记录后台导入任务；异步边界与单本事务边界相互独立。 */
@Service
public class ContentImportCoordinator {
    private final ContentImportJobRepository jobs;
    private final ContentImportRunner runner;

    public ContentImportCoordinator(ContentImportJobRepository jobs, ContentImportRunner runner) { this.jobs = jobs; this.runner = runner; }
    public UUID create(String provider, String mode) {
        String normalizedProvider = provider == null ? "" : provider.trim().toUpperCase(Locale.ROOT);
        String normalizedMode = mode == null || mode.isBlank() ? "INCREMENTAL" : mode.trim().toUpperCase(Locale.ROOT);
        if (!runner.supports(normalizedProvider)) throw new BusinessException(400, "不支持的内容来源");
        ContentImportJob job = jobs.save(new ContentImportJob(normalizedProvider, normalizedMode));
        runner.execute(job.getId());
        return job.getId();
    }
    public ContentImportJob get(UUID id) { return jobs.findById(id).orElseThrow(() -> new BusinessException(404, "导入任务不存在")); }

}
