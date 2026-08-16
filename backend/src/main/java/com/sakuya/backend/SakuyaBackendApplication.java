package com.sakuya.backend;

import com.sakuya.backend.wenku8.Wenku8Properties;
import com.sakuya.backend.catalog.ReleaseManagementProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.sakuya.backend.catalog.Wenku8CatalogSyncProperties;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableConfigurationProperties({Wenku8Properties.class, ReleaseManagementProperties.class, Wenku8CatalogSyncProperties.class})
@EnableAsync
@EnableScheduling
public class SakuyaBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(SakuyaBackendApplication.class, args);
    }
}
