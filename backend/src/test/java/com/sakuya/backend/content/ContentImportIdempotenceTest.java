package com.sakuya.backend.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sakuya.backend.catalog.Book;
import com.sakuya.backend.catalog.BookRepository;
import com.sakuya.backend.catalog.UserLibraryRepository;
import com.sakuya.backend.notification.NotificationService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * ContentImportIdempotenceTest.java
 * 职责说明：锁定「正文未变化时重复导入必须是无副作用的」这一契约。
 * 执行流程：首轮导入使 Book 记住全文哈希 -> 二轮导入相同内容 -> 断言不再重写存储与目录。
 *
 * 背景：每次导入都新建 UUID revision 并全量重写全部章节与 full.txt。
 * 定时任务每天对全部作品重跑一次，于是存储里堆满无人引用的 revision，
 * 且任一章失败回滚后这些已写文件也不会被回收（实测 wenku8-3020 留下 5 份残留）。
 */
class ContentImportIdempotenceTest {

    private final BookRepository books = mock(BookRepository.class);
    private final ContentVolumeRepository volumes = mock(ContentVolumeRepository.class);
    private final ContentChapterRepository chapters = mock(ContentChapterRepository.class);
    private final ContentStorage storage = mock(ContentStorage.class);
    private final AtomicInteger storageWrites = new AtomicInteger();

    private ContentImportService service(Book existing) {
        ContentSourceMappingRepository mappings = mock(ContentSourceMappingRepository.class);
        when(books.findById(anyString())).thenReturn(Optional.ofNullable(existing));
        when(books.save(any(Book.class))).thenAnswer(i -> i.getArgument(0));
        when(mappings.findByProviderIgnoreCaseAndExternalBookId(anyString(), anyString())).thenReturn(Optional.empty());
        when(mappings.save(any(ContentSourceMapping.class))).thenAnswer(i -> i.getArgument(0));
        when(volumes.findByBookIdOrderByDisplayOrderAsc(anyString())).thenReturn(List.of());
        when(chapters.findByBookIdOrderByDisplayOrderAsc(anyString())).thenReturn(List.of());
        when(storage.put(anyString(), any(byte[].class), anyString())).thenAnswer(i -> {
            storageWrites.incrementAndGet();
            byte[] content = i.getArgument(1);
            return new ContentStorage.StoredObject(i.getArgument(0), content.length, ContentHashes.sha256(content));
        });
        return new ContentImportService(books, mappings, volumes, chapters,
            mock(ContentCatalogEntryRepository.class), storage, mock(UserLibraryRepository.class),
            mock(NotificationService.class));
    }

    private ContentProvider provider() {
        return new ContentProvider() {
            @Override public String id() { return "WENKU8"; }
            @Override public List<ProviderCatalogItem> catalog(String mode) { return List.of(); }
            @Override public ProviderBook book(String externalBookId) {
                return new ProviderBook(externalBookId, "测试书", "作者", "简介", "连载", List.of(), false);
            }
            @Override public List<ProviderVolume> volumes(String externalBookId) {
                return List.of(new ProviderVolume("v1", "第一卷", List.of(
                    new ProviderChapter("c1", "第一章", 0),
                    new ProviderChapter("c2", "第二章", 1))));
            }
            @Override public String chapterContent(String externalBookId, String externalChapterId) {
                return new LinkedHashMap<>(java.util.Map.of("c1", "第一章正文", "c2", "第二章正文")).get(externalChapterId);
            }
            @Override public byte[] cover(String externalBookId) { return new byte[0]; }
        };
    }

    @Test
    void 正文未变化时重复导入不重写存储与目录() {
        Book book = new Book("wenku8-1234", "测试书", "作者", "", 0f, List.of(), "", null);
        ContentImportService service = service(book);

        service.importBook(provider(), "1234");
        int writesAfterFirst = storageWrites.get();
        assertThat(writesAfterFirst).isGreaterThan(0);

        // 首轮的删除调用属于正常全量导入，清空记录后才能断言第二轮是否真的没动目录。
        clearInvocations(volumes, chapters);
        // 第二轮内容完全相同：不得再写任何对象，也不得清空/重建目录。
        service.importBook(provider(), "1234");

        assertThat(storageWrites.get()).isEqualTo(writesAfterFirst);
        verify(volumes, never()).deleteByBookId("wenku8-1234");
        verify(chapters, never()).deleteByBookId("wenku8-1234");
    }

    @Test
    void 正文变化时正常重写() {
        Book book = new Book("wenku8-1234", "测试书", "作者", "", 0f, List.of(), "", null);
        ContentImportService service = service(book);
        service.importBook(provider(), "1234");
        int writesAfterFirst = storageWrites.get();

        ContentProvider changed = new ContentProvider() {
            @Override public String id() { return "WENKU8"; }
            @Override public List<ProviderCatalogItem> catalog(String mode) { return List.of(); }
            @Override public ProviderBook book(String externalBookId) {
                return new ProviderBook(externalBookId, "测试书", "作者", "简介", "连载", List.of(), false);
            }
            @Override public List<ProviderVolume> volumes(String externalBookId) {
                return List.of(new ProviderVolume("v1", "第一卷", List.of(new ProviderChapter("c1", "第一章", 0))));
            }
            @Override public String chapterContent(String externalBookId, String externalChapterId) {
                return "改写后的正文";
            }
            @Override public byte[] cover(String externalBookId) { return new byte[0]; }
        };
        clearInvocations(chapters);
        service.importBook(changed, "1234");

        // 上游确实变了就必须重写，否则用户永远读不到更新。
        assertThat(storageWrites.get()).isGreaterThan(writesAfterFirst);
        verify(chapters).deleteByBookId("wenku8-1234");
    }

    @Test
    void 导入失败不留下半成品对象() {
        // fetch 阶段先全部读完再落盘：任一章节读取失败时，存储里不应出现任何新对象。
        Book book = new Book("wenku8-1234", "测试书", "作者", "", 0f, List.of(), "", null);
        int before = storageWrites.get();
        ContentProvider failing = new ContentProvider() {
            @Override public String id() { return "WENKU8"; }
            @Override public List<ProviderCatalogItem> catalog(String mode) { return List.of(); }
            @Override public ProviderBook book(String externalBookId) {
                return new ProviderBook(externalBookId, "测试书", "作者", "简介", "连载", List.of(), false);
            }
            @Override public List<ProviderVolume> volumes(String externalBookId) {
                return List.of(new ProviderVolume("v1", "第一卷", List.of(
                    new ProviderChapter("c1", "第一章", 0),
                    new ProviderChapter("c2", "第二章", 1))));
            }
            @Override public String chapterContent(String externalBookId, String externalChapterId) {
                if ("c2".equals(externalChapterId)) throw new RuntimeException("上游读取失败");
                return "第一章正文";
            }
            @Override public byte[] cover(String externalBookId) { return new byte[0]; }
        };

        try { service(book).importBook(failing, "1234"); } catch (Exception ignored) { }

        assertThat(storageWrites.get()).isEqualTo(before);
    }
}
