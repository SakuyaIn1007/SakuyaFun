package com.sakuya.backend.reading;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * ReadingSyncIntegrationTest.java
 * 职责说明：验证阅读同步的认证隔离、幂等冲突、删除墓碑和参数校验。
 * 执行流程：使用两个演示账号提交同一书籍的离线变更，再检查各账号快照互不泄漏。
 */
@SpringBootTest
@AutoConfigureMockMvc
class ReadingSyncIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void progressAndBookmarksAreIsolatedAndIdempotent() throws Exception {
        String alice = login("alice");
        String bob = login("bob");
        UUID bookmarkId = UUID.randomUUID();
        Instant modified = Instant.parse("2026-01-01T00:00:00Z");
        Map<String, Object> body = request("alice-phone",
            List.of(progress("sword-art-online", 0.42f, modified, false)),
            List.of(bookmark(bookmarkId, "sword-art-online", 0.42f, modified, false)));

        mvc.perform(post("/reading/sync").header("Authorization", "Bearer " + alice)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.progresses[0].progress").value(0.42))
            .andExpect(jsonPath("$.data.bookmarks[0].id").value(bookmarkId.toString()));

        // 相同变更重复发送不能创建重复数据。
        mvc.perform(post("/reading/sync").header("Authorization", "Bearer " + alice)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.progresses.length()").value(1))
            .andExpect(jsonPath("$.data.bookmarks.length()").value(1));

        mvc.perform(post("/reading/sync").header("Authorization", "Bearer " + bob)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(request("bob-phone", List.of(), List.of()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.progresses.length()").value(0))
            .andExpect(jsonPath("$.data.bookmarks.length()").value(0));
    }

    @Test
    void newerMutationWinsAndTombstoneRejectsStaleResurrection() throws Exception {
        String token = login("sakuya");
        UUID bookmarkId = UUID.randomUUID();
        Instant first = Instant.parse("2026-01-02T00:00:00Z");
        Instant newer = first.plusSeconds(10);

        sync(token, request("device-a", List.of(progress("rezero", 0.2f, first, false)),
            List.of(bookmark(bookmarkId, "rezero", 0.2f, first, false))));
        sync(token, request("device-b", List.of(progress("rezero", 0.8f, newer, false)),
            List.of(bookmark(bookmarkId, "rezero", 0.8f, newer, true))));

        // 晚到的旧设备仍携带旧时间，不能覆盖较新进度或复活已删除书签。
        String snapshot = sync(token, request("device-a", List.of(progress("rezero", 0.3f, first, false)),
            List.of(bookmark(bookmarkId, "rezero", 0.3f, first, false))));
        var data = json.readTree(snapshot).path("data");
        org.junit.jupiter.api.Assertions.assertEquals(0.8, data.path("progresses").get(0).path("progress").asDouble(), 0.0001);
        org.junit.jupiter.api.Assertions.assertEquals(0, data.path("bookmarks").size());
    }

    @Test
    void authenticationAndPositionValidationAreEnforced() throws Exception {
        byte[] body = json.writeValueAsBytes(request("device", List.of(progress("sword-art-online", 1.2f, Instant.now(), false)), List.of()));
        mvc.perform(post("/reading/sync").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isUnauthorized());
        mvc.perform(post("/reading/sync").header("Authorization", "Bearer " + login("alice"))
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("阅读位置超出有效范围"));

        byte[] missingBook = json.writeValueAsBytes(request("device", List.of(
            progress("missing-online-book", 0.5f, Instant.now(), false)), List.of()));
        mvc.perform(post("/reading/sync").header("Authorization", "Bearer " + login("alice"))
                .contentType(MediaType.APPLICATION_JSON).content(missingBook))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("图书不存在"));
    }

    private String sync(String token, Map<String, Object> request) throws Exception {
        return mvc.perform(post("/reading/sync").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(request)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private Map<String, Object> request(String deviceId, List<Map<String, Object>> progresses, List<Map<String, Object>> bookmarks) {
        return Map.of("deviceId", deviceId, "progressChanges", progresses, "bookmarkChanges", bookmarks);
    }

    private Map<String, Object> progress(String bookId, float value, Instant modifiedAt, boolean deleted) {
        return Map.of("bookId", bookId, "contentType", "WENKU8", "progress", value, "chapterIndex", 0,
            "chapterProgress", value, "clientModifiedAt", modifiedAt.toString(), "deleted", deleted);
    }

    private Map<String, Object> bookmark(UUID id, String bookId, float value, Instant modifiedAt, boolean deleted) {
        return Map.of("id", id.toString(), "bookId", bookId, "title", "测试书签", "progress", value, "note", "",
            "createdAt", modifiedAt.toString(), "chapterProgress", value, "clientModifiedAt", modifiedAt.toString(), "deleted", deleted);
    }

    private String login(String account) throws Exception {
        String body = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"account\":\"" + account + "\",\"password\":\"password123\"}"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).path("data").path("token").asText();
    }
}
