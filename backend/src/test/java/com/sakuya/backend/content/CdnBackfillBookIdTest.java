package com.sakuya.backend.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sakuya.backend.catalog.Book;
import com.sakuya.backend.catalog.BookRepository;
import com.sakuya.backend.catalog.UserLibraryRepository;
import com.sakuya.backend.notification.NotificationService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * CdnBackfillBookIdTest.java
 * 职责说明：锁定「同一部作品的适配器与下载源必须落到同一个 bookId」这一契约。
 * 执行流程：以桩 provider 触发导入 -> 捕获 books.save 的实体 -> 断言 id 前缀与映射命名空间。
 *
 * 背景：WENKU8_CDN 曾因 provider 判等不匹配落到 content-{UUID} 分支，
 * 使 CDN 回填把整本正文写进一本新书，原 bookId 永远 404 并被反复整本重下。
 */
class CdnBackfillBookIdTest {

    /** 只用单卷单章跑通导入，避免测试依赖真实网络与切分样本。 */
    private ContentProvider stubProvider(String providerId) {
        return new ContentProvider() {
            @Override public String id() { return providerId; }
            @Override public List<ProviderCatalogItem> catalog(String mode) { return List.of(); }
            @Override public ProviderBook book(String externalBookId) {
                return new ProviderBook(externalBookId, "测试书", "作者", "简介", "连载", List.of(), false);
            }
            @Override public List<ProviderVolume> volumes(String externalBookId) {
                return List.of(new ProviderVolume("v1", "第一卷", List.of(new ProviderChapter("978", "第一章", 0))));
            }
            @Override public String chapterContent(String externalBookId, String externalChapterId) { return "正文内容"; }
            @Override public byte[] cover(String externalBookId) { return new byte[0]; }
        };
    }

    private ContentImportService serviceWith(ArgumentCaptor<Book> savedBook, ArgumentCaptor<ContentSourceMapping> savedMapping) {
        BookRepository books = mock(BookRepository.class);
        ContentSourceMappingRepository mappings = mock(ContentSourceMappingRepository.class);
        ContentVolumeRepository volumes = mock(ContentVolumeRepository.class);
        ContentChapterRepository chapters = mock(ContentChapterRepository.class);
        ContentStorage storage = mock(ContentStorage.class);

        when(books.findById(anyString())).thenReturn(Optional.empty());
        when(books.save(savedBook.capture())).thenAnswer(invocation -> invocation.getArgument(0));
        // 映射必须在导入时落库，否则下次解析仍会走到新建分支。
        when(mappings.findByProviderIgnoreCaseAndExternalBookId(anyString(), anyString())).thenReturn(Optional.empty());
        when(mappings.save(savedMapping.capture())).thenAnswer(invocation -> invocation.getArgument(0));
        when(volumes.findByBookIdOrderByDisplayOrderAsc(anyString())).thenReturn(List.of());
        when(chapters.findByBookIdOrderByDisplayOrderAsc(anyString())).thenReturn(List.of());
        when(storage.put(anyString(), any(byte[].class), anyString())).thenAnswer(invocation -> {
            byte[] content = invocation.getArgument(1);
            return new ContentStorage.StoredObject(invocation.getArgument(0), content.length,
                ContentHashes.sha256(content));
        });

        return new ContentImportService(books, mappings, volumes, chapters,
            mock(ContentCatalogEntryRepository.class), storage, mock(UserLibraryRepository.class),
            mock(NotificationService.class));
    }

    @Test
    void 下载源回填必须复用适配器的bookId() {
        ArgumentCaptor<Book> savedBook = ArgumentCaptor.forClass(Book.class);
        ArgumentCaptor<ContentSourceMapping> savedMapping = ArgumentCaptor.forClass(ContentSourceMapping.class);

        String bookId = serviceWith(savedBook, savedMapping).importBook(stubProvider("WENKU8_CDN"), "1234");

        assertThat(bookId).isEqualTo("wenku8-1234");
        assertThat(savedBook.getValue().getId()).isEqualTo("wenku8-1234");
        // 回归关键：绝不能落到 content-{UUID} 分支，否则原 bookId 永远读不到内容。
        assertThat(savedBook.getValue().getId()).doesNotStartWith("content-");
        // 映射写入同样使用归一后的命名空间，避免同一作品留下两条映射行。
        assertThat(savedMapping.getValue().getProvider()).isEqualTo("WENKU8");
        assertThat(savedMapping.getValue().getBookId()).isEqualTo("wenku8-1234");
    }

    @Test
    void 适配器路径bookId保持原有行为() {
        ArgumentCaptor<Book> savedBook = ArgumentCaptor.forClass(Book.class);
        ArgumentCaptor<ContentSourceMapping> savedMapping = ArgumentCaptor.forClass(ContentSourceMapping.class);

        String bookId = serviceWith(savedBook, savedMapping).importBook(stubProvider("WENKU8"), "1234");

        assertThat(bookId).isEqualTo("wenku8-1234");
        assertThat(savedMapping.getValue().getProvider()).isEqualTo("WENKU8");
    }

    @Test
    void 无上游来源的provider仍生成独立content前缀() {
        ArgumentCaptor<Book> savedBook = ArgumentCaptor.forClass(Book.class);
        ArgumentCaptor<ContentSourceMapping> savedMapping = ArgumentCaptor.forClass(ContentSourceMapping.class);

        String bookId = serviceWith(savedBook, savedMapping).importBook(stubProvider("UPLOAD"), "whatever");

        assertThat(bookId).startsWith("content-");
    }
}
