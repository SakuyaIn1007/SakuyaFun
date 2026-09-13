package com.sakuya.backend.content;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sakuya.backend.catalog.Book;
import com.sakuya.backend.catalog.BookRepository;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
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
 * ContentWithdrawalIntegrationTest.java
 * 职责说明：验证「主动下架」与「尚未入库」在回退读模式下不会被混为一谈。
 * 执行流程：以开启回退的读模式创建一本未发布书目 -> 读取目录 -> 断言得到明确的拒绝而不是回退到上游适配器。
 *
 * 背景：下架曾经与「该书目录尚未入库」共用 404，而回退门面只对 404 回退，
 * 导致被下架的正文会绕过权利门禁、改由上游适配器重新提供。本测试锁定修复后的语义。
 */
@SpringBootTest
@AutoConfigureMockMvc
class ContentWithdrawalIntegrationTest {
    @TempDir static Path contentDir;
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.content-storage.type", () -> "local");
        registry.add("app.content-storage.local-dir", contentDir::toString);
        registry.add("app.content-management.token", () -> "content-test-token");
        // 必须开启回退：漏洞只在 DB_FIRST_WITH_ADAPTER_FALLBACK 下成立。
        registry.add("app.content-management.read-mode", () -> "DB_FIRST_WITH_ADAPTER_FALLBACK");
        // 适配器关闭，因此任何一次回退都会以 503「未启用」暴露出来，可据此判断是否发生了回退。
        registry.add("wenku8.adapter.enabled", () -> false);
        registry.add("wenku8.catalog.sync.enabled", () -> false);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired BookRepository books;

    @Test
    void withdrawnBookIsRefusedInsteadOfFallingBackToAdapter() throws Exception {
        String bookId = createBook(false);

        // 修复前：未发布 -> 404 -> 回退门面视为「尚未入库」-> 访问适配器 -> 503（正文被重新分发）。
        // 修复后：未发布是明确的拒绝，直接 403，不进入回退分支。
        mvc.perform(get("/content/novels/" + bookId + "/chapters").header("Authorization", "Bearer " + login()))
            .andExpect(status().isForbidden());
    }

    @Test
    void unknownBookStillFallsBackToAdapter() throws Exception {
        // 反向保护：真正不存在的书必须继续走回退，否则会把合法的上游兜底一起修坏。
        mvc.perform(get("/content/novels/never-imported-book/chapters").header("Authorization", "Bearer " + login()))
            .andExpect(status().isServiceUnavailable());
    }

    @Test
    void restrictedRightsStatusNoLongerBlocksReading() throws Exception {
        // Rights 门禁已删除：rights_status 退化为信息字段，发布状态是唯一的读取开关。
        // 上游把作品标记为版权受限时导入会写入 RESTRICTED，此时仍必须能读到已发布内容。
        String bookId = createBook(false);
        MockMultipartFile document = new MockMultipartFile("file", "restricted.txt", "text/plain",
            "受限作品的正文仍然可读。".getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart("/admin/content/books/" + bookId + "/document").file(document)
                .header("Authorization", "Bearer " + login())
                .header("X-Content-Management-Token", "content-test-token"))
            .andExpect(status().isOk());

        // 直接落库绕过管理端「只有 AUTHORIZED 才能发布」的校验，构造 published + RESTRICTED 组合。
        Book book = books.findById(bookId).orElseThrow();
        book.updateContentPublication(true, "RESTRICTED", "来源标记为版权受限", book.getFullContentObjectKey(), null);
        books.save(book);

        mvc.perform(get("/content/novels/" + bookId + "/chapters").header("Authorization", "Bearer " + login()))
            .andExpect(status().isOk());
        mvc.perform(get("/content/novels/" + bookId + "/full-content").header("Authorization", "Bearer " + login()))
            .andExpect(status().isOk());
    }

    @Test
    void unpublishedBookStaysBlockedEvenWhenAuthorized() throws Exception {
        // 反向保护：删掉 rights 门禁不能把下架语义一起放松，未发布必须仍然 403。
        String bookId = createBook(false);
        Book book = books.findById(bookId).orElseThrow();
        book.updateContentPublication(false, "AUTHORIZED", "", null, null);
        books.save(book);

        mvc.perform(get("/content/novels/" + bookId + "/chapters").header("Authorization", "Bearer " + login()))
            .andExpect(status().isForbidden());
    }

    @Test
    void restrictedRightsStatusDoesNotBlockPublishing() throws Exception {
        // 管理端的发布校验不再看 rights_status：只要已上传正文即可发布。
        // 修复前这里会返回 400「只有已上传正文且确认授权的书籍才能发布」。
        String bookId = createBookWithRights(false, "RESTRICTED");
        uploadDocument(bookId, "受限作品正文。");

        mvc.perform(put("/admin/content/books/" + bookId)
                .header("Authorization", "Bearer " + login())
                .header("X-Content-Management-Token", "content-test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookJson(true, "RESTRICTED")))
            .andExpect(status().isOk());
    }

    @Test
    void rightsStatusIsOptionalBecauseItNoLongerGatesAnything() throws Exception {
        // 该字段已退化为纯展示信息，管理端不应强制填写。
        String bookId = createBookWithRights(false, null);
        uploadDocument(bookId, "未标注权利状态的正文。");

        mvc.perform(put("/admin/content/books/" + bookId)
                .header("Authorization", "Bearer " + login())
                .header("X-Content-Management-Token", "content-test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookJson(true, null)))
            .andExpect(status().isOk());
    }

    @Test
    void publishingStillRequiresUploadedContent() throws Exception {
        // 反向保护：去掉 rights 条件后，「必须有正文」这条约束不能被一起放松。
        String bookId = createBookWithRights(false, "AUTHORIZED");
        mvc.perform(put("/admin/content/books/" + bookId)
                .header("Authorization", "Bearer " + login())
                .header("X-Content-Management-Token", "content-test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookJson(true, "AUTHORIZED")))
            .andExpect(status().isBadRequest());
    }

    private String createBook(boolean published) throws Exception {
        return createBookWithRights(published, "AUTHORIZED");
    }

    private String createBookWithRights(boolean published, String rightsStatus) throws Exception {
        String created = mvc.perform(post("/admin/content/books")
                .header("Authorization", "Bearer " + login())
                .header("X-Content-Management-Token", "content-test-token")
                .contentType(MediaType.APPLICATION_JSON).content(bookJson(published, rightsStatus)))
            .andReturn().getResponse().getContentAsString();
        return json.readTree(created).path("data").path("id").asText();
    }

    private String bookJson(boolean published, String rightsStatus) {
        String rights = rightsStatus == null ? "" : ",\"rightsStatus\":\"" + rightsStatus + "\"";
        return """
            {"title":"下架语义测试书","author":"测试作者","publisher":"自有内容","rating":4.0,
             "tags":["测试"],"description":"验证下架与未入库的区分","status":"连载中"%s,
             "licenseNote":"测试授权内容","published":%s}
            """.formatted(rights, published);
    }

    private void uploadDocument(String bookId, String text) throws Exception {
        MockMultipartFile document = new MockMultipartFile("file", "body.txt", "text/plain",
            text.getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart("/admin/content/books/" + bookId + "/document").file(document)
                .header("Authorization", "Bearer " + login())
                .header("X-Content-Management-Token", "content-test-token"))
            .andExpect(status().isOk());
    }

    private String login() throws Exception {
        String response = mvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"account\":\"sakuya\",\"password\":\"password123\"}"))
            .andReturn().getResponse().getContentAsString();
        return json.readTree(response).path("data").path("token").asText();
    }
}
