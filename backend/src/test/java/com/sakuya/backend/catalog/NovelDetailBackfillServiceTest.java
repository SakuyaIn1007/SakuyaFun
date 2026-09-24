package com.sakuya.backend.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sakuya.backend.wenku8.Wenku8GatewayService;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * NovelDetailBackfillServiceTest.java
 * 职责说明：验证按需详情补抓的触发条件、在途去重、失败隔离与写入语义，不访问上游。
 * 执行流程：Mock 网关与仓库 -> 调用 schedule -> 用超时断言等待异步任务 -> 校验调用次数与实体状态。
 */
class NovelDetailBackfillServiceTest {

    private Wenku8GatewayService gateway;
    private BookRepository books;
    private NovelDetailBackfillService service;
    private final Map<String, Book> store = new LinkedHashMap<>();

    @BeforeEach
    void setUp() {
        gateway = mock(Wenku8GatewayService.class);
        books = mock(BookRepository.class);
        // 补抓参数来自配置；测试用默认值（在途上限 64、冷却 60 秒），紧凑构造器负责归一。
        Wenku8CatalogSyncProperties properties = new Wenku8CatalogSyncProperties(false, false, "", "", 0, 0);
        service = new NovelDetailBackfillService(gateway, books, properties);
        store.clear();
        when(books.findById(anyString())).thenAnswer(invocation -> Optional.ofNullable(store.get(invocation.getArgument(0))));
        when(books.save(any(Book.class))).thenAnswer(invocation -> {
            Book book = invocation.getArgument(0);
            store.put(book.getId(), book);
            return book;
        });
    }

    private Book wenku8Book(String aid) {
        Book book = new Book("wenku8-" + aid, "书名", "作者", "", 0f, List.of(), "预览简介", null);
        book.refreshFromWenku8(aid, "书名", "作者", List.of(), "预览简介", "连载中", false, null, Instant.now());
        store.put(book.getId(), book);
        return book;
    }

    private Map<String, Object> detail(String description) {
        return Map.of("description", description, "status", "连载中");
    }

    @Test
    void 补抓成功后写入完整简介与标记() {
        wenku8Book("101");
        when(gateway.novel("101")).thenReturn(detail("完整简介"));
        service.schedule("wenku8-101");
        verify(books, timeout(5000).atLeastOnce()).save(any(Book.class));
        Book updated = store.get("wenku8-101");
        assertThat(updated.getDescription()).isEqualTo("完整简介");
        assertThat(updated.getDetailSyncedAt()).isNotNull();
    }

    /** 已补全的书不得再次触发——否则详情请求会退回成常态开销。 */
    @Test
    void 已补全的书不触发补抓() {
        Book book = wenku8Book("101");
        book.applyDetailDescription("完整简介", Instant.now());
        service.schedule("wenku8-101");
        verify(gateway, never()).novel(anyString());
    }

    /**
     * 只处理 Wenku8 来源的作品：本地书与 content-{UUID} 的来源标记不是 WENKU8，
     * 若无此判定，它们每次被打开都会排一次注定失败的补抓。
     */
    @Test
    void 非Wenku8来源的书不触发补抓() {
        // 本地书：sourceType 默认 LOCAL，sourceNovelId 为空
        Book local = new Book("local-1", "本地书", "作者", "出版社", 4.5f, List.of(), "简介", "分类");
        store.put(local.getId(), local);
        service.schedule("local-1");
        // content-{UUID}：由导入创建但未标记 WENKU8
        Book imported = new Book("content-abc", "导入书", "作者", "", 0f, List.of(), "简介", null);
        store.put(imported.getId(), imported);
        service.schedule("content-abc");
        verify(gateway, never()).novel(anyString());
    }

    @Test
    void 不存在的书不触发补抓() {
        service.schedule("wenku8-999");
        verify(gateway, never()).novel(anyString());
    }

    /** 同一 aid 的连续调度只应发出一次上游请求，这是避免「等完 5 秒再等 5 秒」的关键。 */
    @Test
    void 在途去重使连续调度只发一次请求() throws Exception {
        wenku8Book("101");
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        when(gateway.novel("101")).thenAnswer(invocation -> {
            entered.countDown();
            // 卡住上游调用，让后续 schedule 落在「在途」窗口内
            if (!release.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("等待超时");
            return detail("完整简介");
        });
        service.schedule("wenku8-101");
        assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
        for (int i = 0; i < 5; i++) service.schedule("wenku8-101");
        release.countDown();
        verify(gateway, timeout(5000).times(1)).novel("101");
        // 注销由任务完成回调驱动，需等它执行完再断言，避免竞态。
        waitUntilIdle();
        assertThat(service.inflightCount()).isZero();
    }

    /** 补抓失败不得向上抛，且不得写入 detailSyncedAt，下次打开可重试。 */
    @Test
    void 补抓失败不抛出且不写标记() {
        wenku8Book("101");
        when(gateway.novel("101")).thenThrow(new com.sakuya.backend.common.BusinessException(503, "上游不可用"));
        service.schedule("wenku8-101");
        verify(books, never()).save(any(Book.class));
        assertThat(store.get("wenku8-101").getDetailSyncedAt()).isNull();
    }

    /**
     * 失败冷却：上游被持续拦截时，用户来回切页会把 5 秒/次的节流额度全部吃光。
     * 60 秒内的重复调度必须被吞掉。
     */
    @Test
    void 失败后冷却期内不再重排() throws Exception {
        wenku8Book("101");
        when(gateway.novel("101")).thenThrow(new com.sakuya.backend.common.BusinessException(503, "上游不可用"));
        service.schedule("wenku8-101");
        // 等第一次失败落定
        verify(gateway, timeout(5000).times(1)).novel("101");
        waitUntilIdle();
        for (int i = 0; i < 5; i++) service.schedule("wenku8-101");
        Thread.sleep(200);
        verify(gateway, times(1)).novel("101");
    }

    /** 上游返回空简介时仍要写标记，否则每次打开都会重抓一本没有简介的书。 */
    @Test
    void 详情返回空简介仍写入标记() {
        wenku8Book("101");
        when(gateway.novel("101")).thenReturn(detail(""));
        service.schedule("wenku8-101");
        verify(books, timeout(5000).atLeastOnce()).save(any(Book.class));
        Book updated = store.get("wenku8-101");
        assertThat(updated.getDescription()).isEqualTo("预览简介");
        assertThat(updated.getDetailSyncedAt()).isNotNull();
    }

    /** 非数字 aid（异常数据）不应触发上游请求。 */
    @Test
    void 非法aid不触发补抓() {
        Book book = new Book("wenku8-abc", "书名", "作者", "", 0f, List.of(), "简介", null);
        book.refreshFromWenku8("abc", "书名", "作者", List.of(), "简介", "连载中", false, null, Instant.now());
        store.put(book.getId(), book);
        service.schedule("wenku8-abc");
        verify(gateway, never()).novel(anyString());
    }

    /**
     * 冷却时长必须来自配置而非写死：把它提为可配置就是为了让部署方能按上游风控强度调整，
     * 若仍用常量，改配置将毫无效果而无人察觉。
     */
    @Test
    void 冷却时长由配置决定() throws Exception {
        BookRepository repo = mock(BookRepository.class);
        Wenku8GatewayService gw = mock(Wenku8GatewayService.class);
        // 冷却 1 秒，便于在测试内等它自然过期。
        Wenku8CatalogSyncProperties properties = new Wenku8CatalogSyncProperties(false, false, "", "", 64, 1);
        NovelDetailBackfillService shortCooldown = new NovelDetailBackfillService(gw, repo, properties);
        when(repo.findById(anyString())).thenAnswer(invocation -> Optional.ofNullable(store.get(invocation.getArgument(0))));
        when(gw.novel("101")).thenThrow(new com.sakuya.backend.common.BusinessException(503, "上游不可用"));
        wenku8Book("101");
        shortCooldown.schedule("wenku8-101");
        verify(gw, timeout(5000).times(1)).novel("101");
        // 冷却期内不重排
        shortCooldown.schedule("wenku8-101");
        Thread.sleep(150);
        verify(gw, times(1)).novel("101");
        // 冷却结束后应允许重试
        Thread.sleep(1100);
        shortCooldown.schedule("wenku8-101");
        verify(gw, timeout(5000).times(2)).novel("101");
    }

    /** 在途上限同样来自配置；这里只验证紧凑构造器对非法值做了归一，避免队列容量为 0 时永久拒收。 */
    @Test
    void 配置非法值被归一为默认值() {
        Wenku8CatalogSyncProperties properties = new Wenku8CatalogSyncProperties(false, false, "", "", 0, 0);
        assertThat(properties.detailBackfillMaxInflight()).isEqualTo(64);
        assertThat(properties.detailBackfillCooldownSeconds()).isEqualTo(60);
    }

    private void waitUntilIdle() throws Exception {
        for (int i = 0; i < 50 && service.inflightCount() > 0; i++) Thread.sleep(20);
    }
}
