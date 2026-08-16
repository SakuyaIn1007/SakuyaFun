package com.sakuya.backend.catalog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Wenku8CatalogFallbackIntegrationTest.java
 * 职责说明：验证首页目录仅把 Wenku8 作为缓存增强，而非 Android 请求的必需依赖。
 * 执行流程：测试以关闭的适配器启动 -> 清空栏目缓存或执行一次失败同步 -> 携带登录令牌请求三个目录接口 ->
 * 断言接口仍返回本地书目和 200，防止首次部署、缓存丢失或上游故障回归为 503。
 */
@SpringBootTest(properties = {
        "wenku8.adapter.enabled=false",
        "wenku8.catalog.sync.enabled=false",
        "wenku8.catalog.sync.startup-enabled=false"
})
@AutoConfigureMockMvc
class Wenku8CatalogFallbackIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired Wenku8CatalogEntryRepository entries;
    @Autowired Wenku8CatalogCacheService cache;

    @BeforeEach
    void clearCatalogCache() {
        entries.deleteAll();
    }

    @Test
    void catalogEndpointsReturnLocalBooksWhenAdapterIsDisabled() throws Exception {
        assertAllCatalogEndpointsAreAvailable(token());
    }

    @Test
    void catalogEndpointsReturnLocalBooksWhenCacheIsEmpty() throws Exception {
        assertAllCatalogEndpointsAreAvailable(token());
    }

    @Test
    void catalogEndpointsReturnLocalBooksAfterSynchronizationFails() throws Exception {
        cache.refreshAll();

        org.junit.jupiter.api.Assertions.assertTrue(entries.findAll().isEmpty());
        assertAllCatalogEndpointsAreAvailable(token());
    }

    /** 目录契约保持原样：推荐、轻小说和榜单都返回可供 Android 渲染的非空数据。 */
    private void assertAllCatalogEndpointsAreAvailable(String token) throws Exception {
        for (String path : new String[]{"/home/recommendations", "/home/novels", "/home/rankings"}) {
            mvc.perform(get(path).header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isNotEmpty());
        }
    }

    private String token() throws Exception {
        String body = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account\":\"sakuya\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).path("data").path("token").asText();
    }
}
