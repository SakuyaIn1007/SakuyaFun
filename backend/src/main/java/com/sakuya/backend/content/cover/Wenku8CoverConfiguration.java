package com.sakuya.backend.content.cover;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wenku8CoverConfiguration.java
 * 职责说明：按部署配置创建唯一的封面直连客户端，业务代码不感知 HTTP 细节。
 * 执行流程：读取 Wenku8CoverProperties -> 构造带连接超时与重定向跟随的 HttpClient -> 装配 Wenku8CoverClient。
 *
 * 与 DownloadSourceConfiguration 同构，但**不复用同一个 HttpClient**：
 * 封面读取超时（10 秒）远短于正文下载（30 秒），合并会迫使两者取其一，
 * 且封面是并发取图、正文是单发大文件，连接池特征不同。
 */
@Configuration
public class Wenku8CoverConfiguration {

    @Bean
    Wenku8CoverClient wenku8CoverClient(Wenku8CoverProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(properties.connectTimeoutSeconds()))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
        return new Wenku8CoverClient(httpClient, properties);
    }
}
