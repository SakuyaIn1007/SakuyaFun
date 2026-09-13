package com.sakuya.backend.content.download;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sakuya.backend.content.ContentProvider;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * DownloadSourceContentProviderTest.java
 * 职责说明：验证下载源 provider 的 aid 识别、切分结果映射与降级行为，使用桩客户端避免真实网络。
 * 执行流程：以真实样本构造桩下载器 -> 调用 provider -> 断言卷章结构与偏移可回读。
 */
class DownloadSourceContentProviderTest {

    private static byte[] sampleBytes() throws Exception {
        try (InputStream in = DownloadSourceContentProviderTest.class.getResourceAsStream("/chapter-splitter/wenku8-1-head.txt")) {
            if (in == null) throw new IllegalStateException("测试样本缺失");
            return in.readAllBytes();
        }
    }

    /** 覆写 download 以避免真实网络；测试样本即为上游返回内容。 */
    private DownloadSourceContentProvider providerWith(byte[] payload) {
        DownloadSourceProperties properties = new DownloadSourceProperties(true, 5, 30, 64 * 1024 * 1024);
        DownloadSourceClient client = new DownloadSourceClient(HttpClient.newHttpClient(), properties) {
            @Override public byte[] download(int aid) { return payload; }
        };
        return new DownloadSourceContentProvider(client);
    }

    @Test
    void supportsAid只接受纯数字() {
        assertThat(DownloadSourceContentProvider.supportsAid("wenku8-1234")).isTrue();
        assertThat(DownloadSourceContentProvider.supportsAid("1234")).isTrue();
        assertThat(DownloadSourceContentProvider.supportsAid("biblia")).isFalse();
        assertThat(DownloadSourceContentProvider.supportsAid("danmachi")).isFalse();
        assertThat(DownloadSourceContentProvider.supportsAid(null)).isFalse();
        assertThat(DownloadSourceContentProvider.supportsAid("")).isFalse();
    }

    @Test
    void volumes按卷分组且章节顺序稳定() throws Exception {
        DownloadSourceContentProvider provider = providerWith(sampleBytes());
        List<ContentProvider.ProviderVolume> volumes = provider.volumes("1");
        assertThat(volumes).hasSize(2);
        assertThat(volumes.get(0).title()).isEqualTo("第一卷 渴望死亡的小丑");
        // 样本切分 22 章（第一卷 10、第二卷 12），其中每卷各含 1 个只有图片链接的「插图」章，
        // 过滤后剩 9 与 11。
        assertThat(volumes.get(0).chapters()).hasSize(9);
        assertThat(volumes.get(1).chapters()).hasSize(11);
    }

    @Test
    void 章节正文可回读且不含标题行() throws Exception {
        DownloadSourceContentProvider provider = providerWith(sampleBytes());
        ContentProvider.ProviderChapter first = provider.volumes("1").get(0).chapters().get(0);
        String content = provider.chapterContent("1", first.externalChapterId());
        assertThat(content).isNotBlank();
        assertThat(content).doesNotStartWith("第一卷");
    }

    @Test
    void catalog不支持目录枚举() {
        DownloadSourceContentProvider provider = providerWith(new byte[0]);
        assertThatThrownBy(() -> provider.catalog("lastupdate"))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("目录枚举");
    }

    @Test
    void 切分降级时返回单章整本() {
        byte[] plain = "没有任何章节标记的纯文本。".getBytes(StandardCharsets.UTF_8);
        DownloadSourceContentProvider provider = providerWith(plain);
        List<ContentProvider.ProviderVolume> volumes = provider.volumes("1");
        assertThat(volumes).hasSize(1);
        assertThat(volumes.get(0).chapters()).hasSize(1);
    }

    @Test
    void 非数字标识被拒绝() {
        DownloadSourceContentProvider provider = providerWith(new byte[0]);
        assertThatThrownBy(() -> provider.volumes("biblia"))
            .hasMessageContaining("下载标识");
    }

    @Test
    void id为稳定的来源标识() {
        assertThat(providerWith(new byte[0]).id()).isEqualTo("WENKU8_CDN");
    }

    private DownloadSourceContentProvider providerWithResource(String resource) throws Exception {
        try (InputStream in = DownloadSourceContentProviderTest.class.getResourceAsStream(resource)) {
            if (in == null) throw new IllegalStateException("测试样本缺失：" + resource);
            return providerWith(in.readAllBytes());
        }
    }

    @Test
    void 插图类章节被过滤() throws Exception {
        DownloadSourceContentProvider provider = providerWithResource("/chapter-splitter/with-image-chapters.txt");
        List<ContentProvider.ProviderChapter> all = provider.volumes("1").stream()
            .flatMap(volume -> volume.chapters().stream()).toList();
        // 样本共 4 个章节标记，其中「插图」正文仅为图片链接，必须被过滤后剩 3 个。
        assertThat(all).extracting(ContentProvider.ProviderChapter::title).doesNotContain("插图");
        assertThat(all).hasSize(3);
    }

    @Test
    void 图片链接章节被识别为无正文() throws Exception {
        DownloadSourceContentProvider provider = providerWithResource("/chapter-splitter/with-image-chapters.txt");
        for (ContentProvider.ProviderVolume volume : provider.volumes("1")) {
            for (ContentProvider.ProviderChapter chapter : volume.chapters()) {
                String body = provider.chapterContent("1", chapter.externalChapterId());
                assertThat(body).as("章节「%s」不应只含图片链接", chapter.title()).doesNotContain("<!--image-->");
            }
        }
    }
}
