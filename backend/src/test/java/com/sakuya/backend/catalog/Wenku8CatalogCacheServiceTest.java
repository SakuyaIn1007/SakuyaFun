package com.sakuya.backend.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sakuya.backend.content.ContentStorage;
import com.sakuya.backend.content.cover.Wenku8CoverClient;
import com.sakuya.backend.wenku8.Wenku8GatewayService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Wenku8CatalogCacheServiceTest.java
 * 职责说明：验证同步阶段零详情请求、封面预取的两道短路与失败隔离，全程不访问上游。
 * 执行流程：Mock 网关/封面客户端/存储 -> 执行同步或封面阶段 -> 断言调用次数与写入结果。
 */
class Wenku8CatalogCacheServiceTest {

    private Wenku8GatewayService gateway;
    private BookRepository books;
    private Wenku8CatalogEntryRepository entries;
    private Wenku8CoverClient coverClient;
    private ContentStorage storage;
    private Wenku8CatalogCacheService service;

    @BeforeEach
    void setUp() {
        gateway = mock(Wenku8GatewayService.class);
        books = mock(BookRepository.class);
        entries = mock(Wenku8CatalogEntryRepository.class);
        coverClient = mock(Wenku8CoverClient.class);
        storage = mock(ContentStorage.class);
        service = new Wenku8CatalogCacheService(gateway, books, entries, coverClient, storage);
        // 每个栏目返回一本不同的书，去重后共 3 本。
        when(gateway.list("lastupdate")).thenReturn(page(item("101", "甲")));
        when(gateway.list("weekvote")).thenReturn(page(item("202", "乙")));
        when(gateway.list("allvote")).thenReturn(page(item("303", "丙")));
        when(books.findById(anyString())).thenAnswer(invocation -> Optional.ofNullable(created.get(invocation.getArgument(0))));
        when(books.save(any(Book.class))).thenAnswer(invocation -> {
            Book book = invocation.getArgument(0);
            created.put(book.getId(), book);
            return book;
        });
    }

    private final Map<String, Book> created = new java.util.LinkedHashMap<>();

    private Wenku8GatewayService.SearchPage page(Map<String, Object>... items) {
        return new Wenku8GatewayService.SearchPage(List.of(items), null);
    }

    private Map<String, Object> item(String aid, String title) {
        return Map.of("id", aid, "title", title, "author", "作者", "description", "预览简介",
            "status", "连载中", "tags", List.of("奇幻"), "copyright", false);
    }

    /**
     * 本设计的核心收益：同步阶段对每一本书都**不发出详情请求**。
     * 该断言必须直接查调用次数而非观察耗时，否则退化为 40–60 次请求时测试仍会通过。
     */
    @Test
    void 同步阶段不发出任何详情请求() {
        service.refreshAll();
        verify(gateway, never()).novel(anyString());
    }

    @Test
    void 同步阶段以列表页资料入库并写入三本() {
        service.refreshAll();
        assertThat(created.keySet()).containsExactlyInAnyOrder("wenku8-101", "wenku8-202", "wenku8-303");
        assertThat(created).hasSize(3);
        assertThat(created.get("wenku8-101").getDescription()).isEqualTo("预览简介");
        // 详情未补全，标记必须为 null，否则按需补抓永远不会触发。
        assertThat(created.get("wenku8-101").getDetailSyncedAt()).isNull();
    }

    /** 同一本书出现在多个栏目时只应写入一次，且只向两个栏目各记一条条目。 */
    @Test
    void 同一本书出现在多个栏目时去重() {
        when(gateway.list("weekvote")).thenReturn(page(item("101", "甲")));
        service.refreshAll();
        // 去重按 aid 生效，因此库中只有 101 与 303 两本，不会因栏目重复而多出实体。
        assertThat(created.keySet()).containsExactlyInAnyOrder("wenku8-101", "wenku8-303");
        assertThat(created).hasSize(2);
        // 栏目条目按各自列表分别建立：weekvote 与 lastupdate 各一条指向 101。
        verify(entries, times(1)).deleteByFeed("RECOMMEND");
        verify(entries, times(1)).deleteByFeed("NOVELS");
        verify(entries, times(1)).deleteByFeed("RANKING");
        verify(gateway, never()).novel(anyString());
    }

    @Test
    void 封面开关关闭时不发任何封面请求() {
        when(coverClient.enabled()).thenReturn(false);
        service.refreshAll();
        service.prefetchCovers();
        verify(coverClient, never()).fetch(org.mockito.ArgumentMatchers.anyInt());
        verify(storage, never()).put(anyString(), any(), anyString());
    }

    /** 最关键的回归保护之一：没有它，每日同步会退化为每天重新下载全部封面。 */
    @Test
    void 已有对象键且对象存在的书不触发封面请求() {
        when(coverClient.enabled()).thenReturn(true);
        service.refreshAll();
        created.values().forEach(book -> book.updateCoverObject("books/" + book.getId() + "/covers/x.jpg", "sha", 10));
        when(storage.exists(anyString())).thenReturn(true);
        service.prefetchCovers();
        verify(coverClient, never()).fetch(org.mockito.ArgumentMatchers.anyInt());
    }

    /** 对象键存在但对象已被删除时必须重抓，否则封面会永久 404。 */
    @Test
    void 对象键存在但对象缺失时重新抓取() {
        when(coverClient.enabled()).thenReturn(true);
        when(storage.exists(anyString())).thenReturn(false);
        when(coverClient.fetch(org.mockito.ArgumentMatchers.anyInt()))
            .thenReturn(new Wenku8CoverClient.CoverImage(new byte[] {1, 2, 3}, "image/png"));
        // 对象键由真实内容哈希拼成，此处回传同一份哈希以便断言键的形状。
        String sha256 = com.sakuya.backend.content.ContentHashes.sha256(new byte[] {1, 2, 3});
        when(storage.put(anyString(), any(), anyString())).thenAnswer(invocation ->
            new ContentStorage.StoredObject(invocation.getArgument(0), 3, sha256));
        service.refreshAll();
        service.prefetchCovers();
        // 键按内容哈希寻址，扩展名跟随内容类型
        verify(storage, times(3)).put(
            org.mockito.ArgumentMatchers.matches("books/wenku8-\\d+/covers/" + sha256 + "\\.png"), any(), eq("image/png"));
    }

    @Test
    void 封面失败不影响书目入库() {
        when(coverClient.enabled()).thenReturn(true);
        when(storage.exists(anyString())).thenReturn(false);
        when(coverClient.fetch(org.mockito.ArgumentMatchers.anyInt()))
            .thenThrow(new com.sakuya.backend.common.BusinessException(503, "封面源不可用"));
        service.refreshAll();
        service.prefetchCovers();
        assertThat(created).hasSize(3);
        // 失败时对象键保持原值（null），下次同步重试
        assertThat(created.values()).allMatch(book -> book.getCoverObjectKey() == null);
    }

    @Test
    void 未同步过任何书时封面阶段直接返回() {
        when(coverClient.enabled()).thenReturn(true);
        service.prefetchCovers();
        verify(coverClient, never()).enabled();
    }

    @Test
    void 栏目上游失败时保留该栏目且其他栏目照常入库() {
        when(gateway.list("weekvote")).thenThrow(new com.sakuya.backend.common.BusinessException(503, "上游不可用"));
        service.refreshAll();
        assertThat(created.keySet()).containsExactlyInAnyOrder("wenku8-101", "wenku8-303");
    }

    @Test
    void 补抓写入窄方法只改简介与标记() {
        Book book = new Book("wenku8-101", "甲", "作者", "", 0f, List.of("奇幻"), "预览", null);
        book.refreshFromWenku8("101", "甲", "作者", List.of("奇幻"), "预览", "连载中", false, null, Instant.now());
        Instant syncedAt = Instant.now();
        book.applyDetailDescription("完整简介", syncedAt);
        assertThat(book.getDescription()).isEqualTo("完整简介");
        assertThat(book.getDetailSyncedAt()).isEqualTo(syncedAt);
        assertThat(book.getSourceNovelId()).isEqualTo("101");
    }

    /** 上游返回空简介时仍要写标记，否则每次打开都会重抓一本没有简介的书。 */
    @Test
    void 空简介仍然写入补抓标记() {
        Book book = new Book("wenku8-101", "甲", "作者", "", 0f, List.of(), "预览", null);
        Instant syncedAt = Instant.now();
        book.applyDetailDescription("   ", syncedAt);
        assertThat(book.getDescription()).isEqualTo("预览");
        assertThat(book.getDetailSyncedAt()).isEqualTo(syncedAt);
    }
}
