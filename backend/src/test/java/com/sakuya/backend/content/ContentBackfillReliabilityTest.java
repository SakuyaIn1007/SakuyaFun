package com.sakuya.backend.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * ContentBackfillReliabilityTest.java
 * 职责说明：锁定按需回填的三项可靠性契约——失败落库、退避抑制、并发去重。
 * 执行流程：以桩 importer 触发 backfill -> 断言状态表写入与导入调用次数。
 *
 * 背景：回填原先只打日志，失败没有任何持久痕迹，同一本书在每次用户打开时都会
 * 重新整本下载，永远无法收敛。
 */
class ContentBackfillReliabilityTest {

    private ContentProvider provider(String id) {
        return new ContentProvider() {
            @Override public String id() { return id; }
            @Override public List<ProviderCatalogItem> catalog(String mode) { return List.of(); }
            @Override public ProviderBook book(String externalBookId) { return null; }
            @Override public List<ProviderVolume> volumes(String externalBookId) { return List.of(); }
            @Override public String chapterContent(String externalBookId, String externalChapterId) { return ""; }
            @Override public byte[] cover(String externalBookId) { return new byte[0]; }
        };
    }

    private ContentImportRunner runnerWith(ContentImportService importer, ContentImportStateRepository states) {
        return new ContentImportRunner(mock(ContentImportJobRepository.class), states,
            List.of(provider("WENKU8")), importer);
    }

    @Test
    void 回填失败必须落库并可被后续抑制() {
        ContentImportService importer = mock(ContentImportService.class);
        ContentImportStateRepository states = mock(ContentImportStateRepository.class);
        ContentImportState state = new ContentImportState("WENKU8", "3020");
        when(states.findByProviderAndExternalBookId("WENKU8", "3020")).thenReturn(Optional.of(state));
        when(states.save(any(ContentImportState.class))).thenAnswer(i -> i.getArgument(0));
        when(importer.importBook(any(), anyString())).thenThrow(new RuntimeException("章节正文为空"));

        runnerWith(importer, states).backfill("WENKU8", "3020");

        assertThat(state.getStatus()).isEqualTo("FAILED");
        assertThat(state.getLastError()).isEqualTo("章节正文为空");
        // 失败后必须写入下次可重试时刻，否则每次读取都会立刻重新整本下载。
        assertThat(state.retryAllowedAt(Instant.now())).isFalse();
    }

    @Test
    void 退避期内重复触发被抑制() {
        ContentImportService importer = mock(ContentImportService.class);
        ContentImportStateRepository states = mock(ContentImportStateRepository.class);
        ContentImportState state = new ContentImportState("WENKU8", "3020");
        state.markFailed("上游不可用");
        when(states.findByProviderAndExternalBookId("WENKU8", "3020")).thenReturn(Optional.of(state));
        when(states.save(any(ContentImportState.class))).thenAnswer(i -> i.getArgument(0));

        ContentImportRunner runner = runnerWith(importer, states);
        runner.backfill("WENKU8", "3020");
        runner.backfill("WENKU8", "3020");

        // 退避未到期时不得再打上游，这是消除「每次打开都重下」的关键。
        verify(importer, never()).importBook(any(), anyString());
    }

    @Test
    void 并发回填同一本书只实际导入一次() throws Exception {
        ContentImportService importer = mock(ContentImportService.class);
        ContentImportStateRepository states = mock(ContentImportStateRepository.class);
        when(states.findByProviderAndExternalBookId(anyString(), anyString())).thenReturn(Optional.empty());
        when(states.save(any(ContentImportState.class))).thenAnswer(i -> i.getArgument(0));

        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        when(importer.importBook(any(), anyString())).thenAnswer(i -> {
            calls.incrementAndGet();
            entered.countDown();
            release.await(5, TimeUnit.SECONDS);
            return "wenku8-3020";
        });

        ContentImportRunner runner = runnerWith(importer, states);
        Thread first = new Thread(() -> runner.backfill("WENKU8", "3020"));
        first.start();
        assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();

        // 首个导入仍在进行中，其余并发请求必须被去重。
        runner.backfill("WENKU8", "3020");
        release.countDown();
        first.join(5000);

        assertThat(calls.get()).isEqualTo(1);
        verify(importer, times(1)).importBook(any(), anyString());
    }

    @Test
    void 导入成功后记录状态以便观测() {
        ContentImportService importer = mock(ContentImportService.class);
        ContentImportStateRepository states = mock(ContentImportStateRepository.class);
        ContentImportState state = new ContentImportState("WENKU8", "3020");
        when(states.findByProviderAndExternalBookId("WENKU8", "3020")).thenReturn(Optional.of(state));
        when(states.save(any(ContentImportState.class))).thenAnswer(i -> i.getArgument(0));
        when(importer.importBook(any(), anyString())).thenReturn("wenku8-3020");

        runnerWith(importer, states).backfill("WENKU8", "3020");

        assertThat(state.getStatus()).isEqualTo("SUCCESS");
        assertThat(state.getNextRetryAt()).isNull();
    }
}
