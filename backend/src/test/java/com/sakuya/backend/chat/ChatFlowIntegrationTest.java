package com.sakuya.backend.chat;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;

/**
 * ChatFlowIntegrationTest.java
 * 职责说明：验证聊天详情、历史搜索和消息回跳所依赖的后端公开契约及数据库状态。
 * 执行流程：注册隔离用户并创建两个群聊，写入文本/非文本消息 -> 验证关键词隔离和上下文窗口 ->
 * 分别读取成员偏好确认置顶、免打扰只影响当前用户 -> 退出群聊后验证成员关系已删除。
 */
@SpringBootTest(properties = "app.chat-media-dir=${java.io.tmpdir}/sakuya-chat-test")
@AutoConfigureMockMvc
class ChatFlowIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ChatService chatService;

    @Test
    void attachmentAndReplyArePersistedAndProtectedByMembership() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        TestUser owner = register("mediaowner" + suffix, "媒体发送者");
        TestUser member = register("mediamember" + suffix, "媒体接收者");
        TestUser outsider = register("mediaother" + suffix, "会话外用户");
        String conversationId = createGroup(owner.token(), "媒体回归群", member.userId());
        String originalId = send(owner.token(), conversationId, "需要回复的原消息", "text");
        MockMultipartFile image = new MockMultipartFile("file", "proof.png", "image/png", new byte[]{1, 2, 3, 4});
        String uploadBody = mvc.perform(multipart("/conversations/{id}/attachments", conversationId)
                .file(image).header("Authorization", bearer(owner.token())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.type").value("image"))
            .andReturn().getResponse().getContentAsString();
        String attachmentId = json.readTree(uploadBody).path("data").path("id").asText();
        String contentUrl = json.readTree(uploadBody).path("data").path("url").asText();
        String clientMessageId = "local-" + suffix;

        mvc.perform(get(contentUrl).header("Authorization", bearer(outsider.token())))
            .andExpect(status().isForbidden());
        mvc.perform(get(contentUrl).header("Authorization", bearer(member.token())))
            .andExpect(status().isOk()).andExpect(content().bytes(new byte[]{1, 2, 3, 4}));

        String messageBody = mvc.perform(post("/conversations/{id}/messages", conversationId)
                .header("Authorization", bearer(owner.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"\",\"messageType\":\"image\",\"attachmentIds\":[\"" + attachmentId
                    + "\"],\"replyToMessageId\":\"" + originalId + "\",\"clientMessageId\":\"" + clientMessageId + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.attachments[0].id").value(attachmentId))
            .andExpect(jsonPath("$.data.replyTo.messageId").value(originalId))
            .andExpect(jsonPath("$.data.replyTo.senderName").value("媒体发送者"))
            .andExpect(jsonPath("$.data.replyTo.preview").value("需要回复的原消息"))
            .andReturn().getResponse().getContentAsString();
        String messageId = json.readTree(messageBody).path("data").path("id").asText();

        // 响应丢失后的同 ID 重试必须返回同一服务端消息，不能重复发送。
        mvc.perform(post("/conversations/{id}/messages", conversationId)
                .header("Authorization", bearer(owner.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"\",\"messageType\":\"image\",\"attachmentIds\":[\"" + attachmentId
                    + "\"],\"replyToMessageId\":\"" + originalId + "\",\"clientMessageId\":\"" + clientMessageId + "\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(messageId));

        mvc.perform(get("/conversations/{id}/messages", conversationId)
                .header("Authorization", bearer(member.token())))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(messageId)))
            .andExpect(content().string(containsString(attachmentId)))
            .andExpect(content().string(containsString(originalId)));

        // 已绑定附件不能被第二条消息重复认领。
        mvc.perform(post("/conversations/{id}/messages", conversationId)
                .header("Authorization", bearer(owner.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"重复附件\",\"attachmentIds\":[\"" + attachmentId + "\"],\"clientMessageId\":\"other-" + suffix + "\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void concurrentDirectCreationAlwaysReusesOneConversation() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        TestUser first = register("directfirst" + suffix, "甲");
        TestUser second = register("directsecond" + suffix, "乙");
        int callers = 6;
        CountDownLatch ready = new CountDownLatch(callers);
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(callers);
        try {
            List<Future<String>> results = java.util.stream.IntStream.range(0, callers)
                .mapToObj(index -> executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return chatService.direct(UUID.fromString(first.userId()), UUID.fromString(second.userId())).id();
                }))
                .toList();
            ready.await();
            start.countDown();

            HashSet<String> conversationIds = new HashSet<>();
            for (Future<String> result : results) conversationIds.add(result.get());
            org.junit.jupiter.api.Assertions.assertEquals(1, conversationIds.size());
            String conversationId = conversationIds.iterator().next();
            // 同一实体从双方视角读取时，标题都应是对方昵称，不能把创建时的标题快照展示给接收方。
            org.junit.jupiter.api.Assertions.assertEquals("乙", chatService.detail(UUID.fromString(first.userId()), UUID.fromString(conversationId)).title());
            org.junit.jupiter.api.Assertions.assertEquals("甲", chatService.detail(UUID.fromString(second.userId()), UUID.fromString(conversationId)).title());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void searchContextPreferencesAndLeaveGroupFormAClosedLoop() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        TestUser owner = register("chatowner" + suffix, "群主");
        TestUser member = register("chatmember" + suffix, "成员");
        TestUser outsider = register("chatother" + suffix, "其他成员");
        String firstGroup = createGroup(owner.token(), "回归群一", member.userId());
        String secondGroup = createGroup(owner.token(), "回归群二", outsider.userId());

        send(owner.token(), firstGroup, "关键词前文", "text");
        String targetId = send(owner.token(), firstGroup, "唯一针点关键词", "text");
        send(member.token(), firstGroup, "关键词后文", "text");
        send(owner.token(), firstGroup, "唯一针点关键词", "image");
        send(owner.token(), secondGroup, "唯一针点关键词", "text");

        mvc.perform(get("/conversations/{id}/messages/search", firstGroup)
                .param("keyword", "唯一针点")
                .header("Authorization", bearer(owner.token())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(1)))
            .andExpect(jsonPath("$.data[0].id").value(targetId))
            .andExpect(jsonPath("$.data[0].messageType").value("text"));

        mvc.perform(get("/conversations/{id}/messages/context", firstGroup)
                .param("messageId", targetId)
                .param("around", "20")
                .header("Authorization", bearer(owner.token())))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("关键词前文")))
            .andExpect(content().string(containsString(targetId)))
            .andExpect(content().string(containsString("关键词后文")));

        mvc.perform(patch("/conversations/{id}/preferences", firstGroup)
                .header("Authorization", bearer(owner.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"pinned\":true,\"muted\":true}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.preferences.pinned").value(true))
            .andExpect(jsonPath("$.data.preferences.muted").value(true));

        mvc.perform(get("/conversations/{id}", firstGroup).header("Authorization", bearer(member.token())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.preferences.pinned").value(false))
            .andExpect(jsonPath("$.data.preferences.muted").value(false));

        mvc.perform(delete("/conversations/{id}/membership", firstGroup).header("Authorization", bearer(owner.token())))
            .andExpect(status().isOk());
        mvc.perform(get("/conversations/{id}", firstGroup).header("Authorization", bearer(owner.token())))
            .andExpect(status().isForbidden());
        mvc.perform(get("/conversations/{id}", firstGroup).header("Authorization", bearer(member.token())))
            .andExpect(status().isOk());
    }

    /** 注册测试专用账号，避免依赖演示数据或与其他集成测试共享会话状态。 */
    private TestUser register(String account, String nickname) throws Exception {
        String body = mvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"account\":\"" + account + "\",\"password\":\"password123\",\"nickname\":\"" + nickname + "\"}"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode data = json.readTree(body).path("data");
        return new TestUser(data.path("userId").asText(), data.path("token").asText());
    }

    /** 创建群聊并返回服务端生成的会话 ID。 */
    private String createGroup(String token, String title, String memberId) throws Exception {
        String body = mvc.perform(post("/conversations")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"" + title + "\",\"userIds\":[\"" + memberId + "\"]}"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).path("data").path("id").asText();
    }

    /** 写入指定类型消息并返回消息 ID，用于后续检索和上下文回跳断言。 */
    private String send(String token, String conversationId, String message, String type) throws Exception {
        String body = mvc.perform(post("/conversations/{id}/messages", conversationId)
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"" + message + "\",\"messageType\":\"" + type + "\"}"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).path("data").path("id").asText();
    }

    private String bearer(String token) { return "Bearer " + token; }
    private record TestUser(String userId, String token) {}
}
