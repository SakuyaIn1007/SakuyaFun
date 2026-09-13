package com.sakuya.backend.content.download;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 根据部署配置创建唯一的下载源客户端，业务代码不感知 HTTP 细节。 */
@Configuration
public class DownloadSourceConfiguration {
    @Bean
    DownloadSourceClient downloadSourceClient(DownloadSourceProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(properties.connectTimeoutSeconds()))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
        return new DownloadSourceClient(httpClient, properties);
    }
}
