package com.sakuya.backend.content.download;

import static org.assertj.core.api.Assertions.assertThat;

import com.sakuya.backend.content.ContentImportRunner;
import java.net.http.HttpClient;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * DownloadSourceFallbackTest.java
 * 职责说明：验证新 provider 已注册进导入器，且其 id 与适配器 provider 互不冲突。
 * 执行流程：构造仅含新 provider 的导入器 -> 断言 supports 判定正确。
 *
 * 注意 supports() 的契约是「调用方负责规范化」：ContentImportCoordinator 传入的是
 * 已转大写的 normalizedProvider，因此这里只用大写标识断言，不假设大小写不敏感。
 */
class DownloadSourceFallbackTest {

    @Test
    void 导入器识别下载源provider标识() {
        DownloadSourceProperties properties = new DownloadSourceProperties(true, 5, 30, 64 * 1024 * 1024);
        DownloadSourceClient client = new DownloadSourceClient(HttpClient.newHttpClient(), properties);
        ContentImportRunner runner = new ContentImportRunner(
            null, List.of(new DownloadSourceContentProvider(client, null)), null);
        assertThat(runner.supports("WENKU8_CDN")).isTrue();
        // 适配器 provider 的标识仍独立可用，二者不冲突。
        assertThat(runner.supports("WENKU8")).isFalse();
    }
}
