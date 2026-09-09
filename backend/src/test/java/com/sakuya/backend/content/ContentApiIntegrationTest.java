package com.sakuya.backend.content;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * ContentApiIntegrationTest.java
 * 职责说明：验证管理写入与客户端读取在禁用爬虫时仍形成完整闭环。
 * 执行流程：登录 -> 创建未发布书目 -> 上传 TXT -> 授权发布 -> 搜索、目录、单章和全文读取。
 */
@SpringBootTest
@AutoConfigureMockMvc
class ContentApiIntegrationTest {
    @TempDir static Path contentDir;
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.content-storage.type", () -> "local");
        registry.add("app.content-storage.local-dir", contentDir::toString);
        registry.add("app.content-management.token", () -> "content-test-token");
        registry.add("app.content-management.read-mode", () -> "DB_ONLY");
        registry.add("wenku8.adapter.enabled", () -> false);
        registry.add("wenku8.catalog.sync.enabled", () -> false);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ContentImportService importer;

    @Test
    void uploadedTxtCanBeReadWithoutOnlineAdapter() throws Exception {
        String token = login();
        String draft = """
            {"title":"后端内容库测试书","author":"测试作者","publisher":"自有内容","rating":4.5,
             "tags":["测试","离线"],"description":"验证内容库读取","status":"连载中",
             "rightsStatus":"AUTHORIZED","licenseNote":"测试授权内容","published":false}
            """;
        String created = mvc.perform(post("/admin/content/books")
                .header("Authorization", "Bearer " + token)
                .header("X-Content-Management-Token", "content-test-token")
                .contentType(MediaType.APPLICATION_JSON).content(draft))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.published").value(false))
            .andReturn().getResponse().getContentAsString();
        String bookId = json.readTree(created).path("data").path("id").asText();

        MockMultipartFile document = new MockMultipartFile("file", "owned.txt", "text/plain",
            "这是完全存放在后端内容库中的正文。".getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart("/admin/content/books/" + bookId + "/document").file(document)
                .header("Authorization", "Bearer " + token)
                .header("X-Content-Management-Token", "content-test-token"))
            .andExpect(status().isOk());
        MockMultipartFile cover = new MockMultipartFile("file", "owned.png", "image/png", new byte[] {1, 2, 3, 4});
        mvc.perform(multipart("/admin/content/books/" + bookId + "/cover").file(cover)
                .header("Authorization", "Bearer " + token)
                .header("X-Content-Management-Token", "content-test-token"))
            .andExpect(status().isOk());

        String published = draft.replace("\"published\":false", "\"published\":true");
        mvc.perform(put("/admin/content/books/" + bookId)
                .header("Authorization", "Bearer " + token)
                .header("X-Content-Management-Token", "content-test-token")
                .contentType(MediaType.APPLICATION_JSON).content(published))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.published").value(true));

        mvc.perform(post("/admin/content/source-mappings")
                .header("Authorization", "Bearer " + token)
                .header("X-Content-Management-Token", "content-test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"provider\":\"WENKU8\",\"externalBookId\":\"legacy-owned\",\"bookId\":\"" + bookId + "\"}"))
            .andExpect(status().isOk());
        mvc.perform(put("/admin/content/feeds/RECOMMEND")
                .header("Authorization", "Bearer " + token)
                .header("X-Content-Management-Token", "content-test-token")
                .contentType(MediaType.APPLICATION_JSON).content("[\"" + bookId + "\"]"))
            .andExpect(status().isOk());

        mvc.perform(get("/content/novels?keyword=内容库&page=0&pageSize=20").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].id").value(bookId));
        String index = mvc.perform(get("/content/novels/" + bookId + "/chapters").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.volumes[0].chapters[0].id").isNotEmpty())
            .andReturn().getResponse().getContentAsString();
        String chapterId = json.readTree(index).path("data").path("volumes").get(0).path("chapters").get(0).path("id").asText();
        mvc.perform(get("/content/chapters/" + chapterId + "?bookId=" + bookId).header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.content").value("这是完全存放在后端内容库中的正文。"));
        mvc.perform(get("/content/novels/" + bookId + "/full-content").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.novelId").value(bookId))
            .andExpect(jsonPath("$.data.content").value(org.hamcrest.Matchers.containsString("后端内容库")));
        mvc.perform(get("/content/novels/" + bookId + "/cover").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andExpect(header().string("Content-Type", "image/png"));
        mvc.perform(get("/wenku8/novels/legacy-owned").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(bookId));
        mvc.perform(get("/wenku8/novels/legacy-owned/chapters").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.bookId").value(bookId));
        mvc.perform(get("/wenku8/chapters/0/content?novelId=legacy-owned").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.novelId").value(bookId))
            .andExpect(jsonPath("$.data.content").value("这是完全存放在后端内容库中的正文。"));
        mvc.perform(get("/home/recommendations").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].id").value(bookId));
    }

    @Test
    void managementTokenIsRequiredForWrites() throws Exception {
        mvc.perform(post("/admin/content/books")
                .header("Authorization", "Bearer " + login())
                .header("X-Content-Management-Token", "wrong")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"测试\",\"author\":\"作者\",\"rightsStatus\":\"AUTHORIZED\",\"published\":false}"))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void providerImportPublishesStableIdsAndRejectsCrossBookChapterReads() throws Exception {
        String firstBookId = importer.importBook(provider("provider-first", "第一本正文"), "provider-first");
        String secondBookId = importer.importBook(provider("provider-second", "第二本正文"), "provider-second");
        String token = login();

        String index = mvc.perform(get("/content/novels/" + firstBookId + "/chapters")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.bookId").value(firstBookId))
            .andExpect(jsonPath("$.data.volumes[0].chapters[0].id").value(org.hamcrest.Matchers.startsWith("chapter-")))
            .andReturn().getResponse().getContentAsString();
        String chapterId = json.readTree(index).path("data").path("volumes").get(0).path("chapters").get(0).path("id").asText();

        mvc.perform(get("/content/chapters/" + chapterId + "?bookId=" + firstBookId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.novelId").value(firstBookId))
            .andExpect(jsonPath("$.data.content").value("第一本正文"));
        mvc.perform(get("/content/novels/" + firstBookId).header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.coverUrl").value("/content/novels/" + firstBookId + "/cover"));
        mvc.perform(get("/content/novels/" + firstBookId + "/full-content")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.chapters[0].chapterId").value(chapterId))
            .andExpect(jsonPath("$.data.content").value(org.hamcrest.Matchers.containsString("第一本正文")));

        // chapterId 即使有效，也不能和另一 bookId 组合读取，避免目录/导航映射错误串书。
        mvc.perform(get("/content/chapters/" + chapterId + "?bookId=" + secondBookId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("章节不属于指定图书"));
    }

    /** 构造最小来源适配器，验证导入器而非管理上传路径形成的目录、正文和封面。 */
    private ContentProvider provider(String externalBookId, String content) {
        return new ContentProvider() {
            @Override public String id() { return "TEST"; }
            @Override public List<ProviderCatalogItem> catalog(String mode) { return List.of(new ProviderCatalogItem(externalBookId, Set.of("NOVELS"))); }
            @Override public ProviderBook book(String ignored) {
                return new ProviderBook(externalBookId, "导入测试书 " + externalBookId, "导入作者", "导入说明", "完结", List.of("测试"), false);
            }
            @Override public List<ProviderVolume> volumes(String ignored) {
                return List.of(new ProviderVolume("volume-1", "第一卷", List.of(new ProviderChapter("chapter-1", "第一章", 0))));
            }
            @Override public String chapterContent(String ignoredBookId, String ignoredChapterId) { return content; }
            @Override public byte[] cover(String ignored) { return new byte[] {9, 8, 7}; }
        };
    }

    private String login() throws Exception {
        String body = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"account\":\"alice\",\"password\":\"password123\"}"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).path("data").path("token").asText();
    }
}
