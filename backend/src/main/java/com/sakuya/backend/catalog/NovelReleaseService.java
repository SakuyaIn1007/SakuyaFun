package com.sakuya.backend.catalog;

import com.sakuya.backend.common.BusinessException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * NovelReleaseService.java
 * 职责说明：处理人工时间表的读取和维护，确保每条记录都关联现有本地书目。
 * 执行流程：Controller 校验维护令牌/请求体 -> Service 校验书目 -> Repository 持久化 -> 统一 DTO 返回。
 */
@Service
public class NovelReleaseService {
    private final NovelReleaseRepository releases; private final BookRepository books;
    public NovelReleaseService(NovelReleaseRepository releases, BookRepository books) { this.releases = releases; this.books = books; }
    public List<NovelRelease> list() { return releases.findAllByOrderByReleaseDateAscVolumeNameAsc(); }
    @Transactional public NovelRelease create(String bookId, LocalDate date, String volumeName, boolean recommended) { requireBook(bookId); return releases.save(new NovelRelease(bookId, date, volumeName, recommended)); }
    @Transactional public NovelRelease update(UUID id, String bookId, LocalDate date, String volumeName, boolean recommended) { NovelRelease release = releases.findById(id).orElseThrow(() -> new BusinessException(404, "时间表记录不存在")); requireBook(bookId); release.update(bookId, date, volumeName, recommended); return release; }
    @Transactional public void delete(UUID id) { if (!releases.existsById(id)) throw new BusinessException(404, "时间表记录不存在"); releases.deleteById(id); }
    private void requireBook(String bookId) { if (!books.existsById(bookId)) throw new BusinessException(400, "书目不存在，不能创建时间表记录"); }
}
