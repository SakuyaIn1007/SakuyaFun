package com.sakuya.backend.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sakuya.backend.catalog.BookRepository;
import com.sakuya.backend.catalog.UserLibraryRepository;
import com.sakuya.backend.notification.NotificationService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * ContentImportToleranceTest.java
 * 职责说明：锁定「单个空正文章节不得导致整本无法入库」这一容错契约。
 * 执行流程：以桩 provider 构造含插图章节的作品 -> 导入 -> 断言空章节被跳过而非中断。
 *
 * 背景：Wenku8 的「插图」章节只有图片链接没有正文，importBook 原先对它抛 503，
 * 而该方法带 @Transactional，异常会让整本回滚：bookId 恒为 404，用户每次打开都重新
 * 整本下载，最终留下 published=true 却零章节的空壳书（如 wenku8-3020、wenku8-4367）。
 */
class ContentImportToleranceTest {

    /** 按 externalChapterId 返回正文；值为空串即模拟无正文的插图章节。 */
    private ContentProvider providerWith(Map<String, String> chapters) {
        return new ContentProvider() {
            @Override public String id() { return "WENKU8"; }
            @Override public List<ProviderCatalogItem> catalog(String mode) { return List.of(); }
            @Override public ProviderBook book(String externalBookId) {
                return new ProviderBook(externalBookId, "测试书", "作者", "简介", "连载", List.of(), false);
            }
            @Override public List<ProviderVolume> volumes(String externalBookId) {
                List<ProviderChapter> items = new ArrayList<>();
                int order = 0;
                for (String id : chapters.keySet()) items.add(new ProviderChapter(id, "章节" + id, order++));
                return List.of(new ProviderVolume("v1", "第一卷", List.copyOf(items)));
            }
            @Override public String chapterContent(String externalBookId, String externalChapterId) {
                return chapters.get(externalChapterId);
            }
            @Override public byte[] cover(String externalBookId) { return new byte[0]; }
        };
    }

    /** 记录实际落库的章节，用于断言空章节确实未进入内容库。 */
    private List<ContentChapter> importAndCapture(ContentProvider provider) {
        BookRepository books = mock(BookRepository.class);
        ContentSourceMappingRepository mappings = mock(ContentSourceMappingRepository.class);
        ContentVolumeRepository volumes = mock(ContentVolumeRepository.class);
        ContentChapterRepository chapters = mock(ContentChapterRepository.class);
        ContentStorage storage = mock(ContentStorage.class);
        List<ContentChapter> saved = new ArrayList<>();

        when(books.findById(anyString())).thenReturn(Optional.empty());
        when(books.save(any(com.sakuya.backend.catalog.Book.class))).thenAnswer(i -> i.getArgument(0));
        when(mappings.findByProviderIgnoreCaseAndExternalBookId(anyString(), anyString())).thenReturn(Optional.empty());
        when(mappings.save(any(ContentSourceMapping.class))).thenAnswer(i -> i.getArgument(0));
        when(volumes.findByBookIdOrderByDisplayOrderAsc(anyString())).thenReturn(List.of());
        when(chapters.findByBookIdOrderByDisplayOrderAsc(anyString())).thenReturn(List.of());
        when(storage.put(anyString(), any(byte[].class), anyString())).thenAnswer(i -> {
            byte[] content = i.getArgument(1);
            return new ContentStorage.StoredObject(i.getArgument(0), content.length, ContentHashes.sha256(content));
        });
        when(chapters.saveAll(anyList())).thenAnswer(i -> {
            List<ContentChapter> arg = i.getArgument(0);
            saved.addAll(arg);
            return arg;
        });

        new ContentImportService(books, mappings, volumes, chapters,
            mock(ContentCatalogEntryRepository.class), storage, mock(UserLibraryRepository.class),
            mock(NotificationService.class)).importBook(provider, "1234");
        return saved;
    }

    @Test
    void 空正文章节被跳过而整本仍可入库() {
        List<ContentChapter> saved = importAndCapture(providerWith(new java.util.LinkedHashMap<>() {{
            put("c1", "第一章正文");
            put("c2", "");            // 插图章节：无正文
            put("c3", "第三章正文");
        }}));

        assertThat(saved).hasSize(2);
        assertThat(saved).extracting(ContentChapter::getTitle).containsExactly("章节c1", "章节c3");
        // 跳过的章节不得留下空正文对象。
        assertThat(saved).allSatisfy(chapter -> assertThat(chapter.getSha256()).isNotBlank());
    }

    @Test
    void 章节正文为null同样被跳过() {
        List<ContentChapter> saved = importAndCapture(providerWith(new java.util.LinkedHashMap<>() {{
            put("c1", "正文");
            put("c2", null);
        }}));

        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getTitle()).isEqualTo("章节c1");
    }

    @Test
    void 全部章节无正文时拒绝发布空书() {
        // 上游整体异常时若静默跳过，会产出另一本零章节的空壳书，必须显式失败。
        assertThatThrownBy(() -> importAndCapture(providerWith(new java.util.LinkedHashMap<>() {{
            put("c1", "");
            put("c2", null);
        }}))).isInstanceOf(com.sakuya.backend.common.BusinessException.class);
    }
}
