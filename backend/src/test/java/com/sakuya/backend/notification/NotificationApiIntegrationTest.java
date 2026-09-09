package com.sakuya.backend.notification;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * NotificationApiIntegrationTest.java
 * 职责说明：覆盖好友事件产生通知、接收者授权、未读状态、设备注册与分类偏好。
 * 执行流程：注册隔离账号 -> 发送并接受好友申请 -> 两侧读取通知 -> 验证跨用户访问被拒绝。
 */
@SpringBootTest @AutoConfigureMockMvc
class NotificationApiIntegrationTest {
    @Autowired MockMvc mvc; @Autowired ObjectMapper json;

    @Test void friendLifecycleCreatesPrivateReadableNotifications() throws Exception {
        String suffix=UUID.randomUUID().toString().replace("-","").substring(0,10);
        UserSession sender=register("notis"+suffix,"通知发送者");UserSession receiver=register("notir"+suffix,"通知接收者");
        mvc.perform(post("/notifications/devices").header("Authorization","Bearer "+receiver.token()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"installationId\":\"install-"+suffix+"\",\"token\":\"token-"+suffix+"\",\"platform\":\"android\"}"))
            .andExpect(status().isOk());
        mvc.perform(put("/notifications/preferences").header("Authorization","Bearer "+receiver.token()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"pushEnabled\":true,\"feedEnabled\":false,\"friendEnabled\":true}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.feedEnabled").value(false));
        mvc.perform(post("/friends/requests").header("Authorization","Bearer "+sender.token()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"userId\":\""+receiver.userId()+"\",\"message\":\"测试通知\"}"))
            .andExpect(status().isOk());
        String notificationBody=mvc.perform(get("/notifications").header("Authorization","Bearer "+receiver.token()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].type").value("FRIEND_REQUEST")).andReturn().getResponse().getContentAsString();
        String notificationId=json.readTree(notificationBody).path("data").path("items").get(0).path("id").asText();
        String requestBody=mvc.perform(get("/friends/requests").header("Authorization","Bearer "+receiver.token())).andReturn().getResponse().getContentAsString();
        String requestId=json.readTree(requestBody).path("data").get(0).path("id").asText();
        mvc.perform(put("/notifications/"+notificationId+"/read").header("Authorization","Bearer "+sender.token())).andExpect(status().isNotFound());
        mvc.perform(put("/notifications/"+notificationId+"/read").header("Authorization","Bearer "+receiver.token())).andExpect(status().isOk());
        mvc.perform(get("/notifications/unread-count").header("Authorization","Bearer "+receiver.token())).andExpect(status().isOk()).andExpect(jsonPath("$.data.count").value(0));
        mvc.perform(put("/friends/requests/"+requestId+"/accept").header("Authorization","Bearer "+receiver.token())).andExpect(status().isOk());
        mvc.perform(get("/notifications").header("Authorization","Bearer "+sender.token())).andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].type").value("FRIEND_ACCEPTED"));
        mvc.perform(delete("/notifications/devices/install-"+suffix).header("Authorization","Bearer "+receiver.token())).andExpect(status().isOk());
    }

    @Test void feedInteractionsCreateExpectedNotificationsAndSuppressSelfActions() throws Exception {
        String suffix=UUID.randomUUID().toString().replace("-","").substring(0,10);
        UserSession author=register("notia"+suffix,"动态作者"),actor=register("notib"+suffix,"互动用户"),replier=register("notic"+suffix,"回复用户");
        String postBody=mvc.perform(post("/feed").header("Authorization","Bearer "+author.token()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"通知动态\",\"content\":\"测试内容\",\"topics\":[]}"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String postId=json.readTree(postBody).path("data").path("id").asText();
        String commentBody=mvc.perform(post("/feed/"+postId+"/comments").header("Authorization","Bearer "+actor.token()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"content\":\"第一条评论\"}"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String commentId=json.readTree(commentBody).path("data").path("id").asText();
        mvc.perform(post("/feed/"+postId+"/like").header("Authorization","Bearer "+actor.token()).contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":true}")) .andExpect(status().isOk());
        mvc.perform(post("/feed/"+postId+"/favorite").header("Authorization","Bearer "+actor.token()).contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":true}")) .andExpect(status().isOk());
        mvc.perform(post("/profiles/"+author.userId()+"/follow").header("Authorization","Bearer "+actor.token())).andExpect(status().isOk());
        mvc.perform(post("/feed/"+postId+"/comments").header("Authorization","Bearer "+replier.token()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"content\":\"回复内容\",\"replyToCommentId\":\""+commentId+"\"}")) .andExpect(status().isOk());
        mvc.perform(post("/feed/"+postId+"/like").header("Authorization","Bearer "+author.token()).contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":true}")) .andExpect(status().isOk());
        String authorNotifications=mvc.perform(get("/notifications?page=0&pageSize=50").header("Authorization","Bearer "+author.token())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String actorNotifications=mvc.perform(get("/notifications?page=0&pageSize=50").header("Authorization","Bearer "+actor.token())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String authorText=json.readTree(authorNotifications).path("data").path("items").toString();
        org.junit.jupiter.api.Assertions.assertTrue(authorText.contains("FEED_COMMENT"));org.junit.jupiter.api.Assertions.assertTrue(authorText.contains("FEED_LIKE"));org.junit.jupiter.api.Assertions.assertTrue(authorText.contains("FEED_FAVORITE"));org.junit.jupiter.api.Assertions.assertTrue(authorText.contains("NEW_FOLLOWER"));
        org.junit.jupiter.api.Assertions.assertTrue(json.readTree(actorNotifications).path("data").path("items").toString().contains("FEED_REPLY"));
        long authorLikeCount=java.util.stream.StreamSupport.stream(json.readTree(authorNotifications).path("data").path("items").spliterator(),false).filter(x->"FEED_LIKE".equals(x.path("type").asText())).count();
        org.junit.jupiter.api.Assertions.assertEquals(1,authorLikeCount,"作者自己的点赞不得产生通知");
    }

    @Test void unreadSummaryAndCategoryReadKeepOtherCategoriesUnread() throws Exception {
        String suffix=UUID.randomUUID().toString().replace("-","").substring(0,10);
        UserSession author=register("summa"+suffix,"汇总作者"),actor=register("summb"+suffix,"汇总互动者");
        String postId=publish(author,"分类汇总动态");
        mvc.perform(post("/feed/"+postId+"/comments").header("Authorization","Bearer "+actor.token()).contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"分类评论\"}")) .andExpect(status().isOk());
        mvc.perform(post("/profiles/"+author.userId()+"/follow").header("Authorization","Bearer "+actor.token())).andExpect(status().isOk());
        mvc.perform(get("/notifications/unread-summary").header("Authorization","Bearer "+author.token()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(2)).andExpect(jsonPath("$.data.feedInteraction").value(1)).andExpect(jsonPath("$.data.newFollowers").value(1));
        mvc.perform(put("/notifications/read-category").header("Authorization","Bearer "+author.token()).contentType(MediaType.APPLICATION_JSON).content("{\"category\":\"NEW_FOLLOWER\"}")) .andExpect(status().isOk());
        mvc.perform(get("/notifications/unread-summary").header("Authorization","Bearer "+author.token()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1)).andExpect(jsonPath("$.data.feedInteraction").value(1)).andExpect(jsonPath("$.data.newFollowers").value(0));
    }

    @Test void followingUpdatesSupportAuthorAndGlobalReadCursorsWithoutLeakingState() throws Exception {
        String suffix=UUID.randomUUID().toString().replace("-","").substring(0,10);
        UserSession author=register("cursa"+suffix,"游标作者"),viewer=register("cursb"+suffix,"游标读者"),outsider=register("cursc"+suffix,"其他读者");
        mvc.perform(get("/feed/following/unseen-summary").header("Authorization","Bearer "+viewer.token())).andExpect(status().isOk()).andExpect(jsonPath("$.data.postCount").value(0));
        mvc.perform(post("/profiles/"+author.userId()+"/follow").header("Authorization","Bearer "+viewer.token())).andExpect(status().isOk());
        publish(author,"关注后的新动态");
        mvc.perform(get("/feed/following/unseen-summary").header("Authorization","Bearer "+viewer.token()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.postCount").value(1)).andExpect(jsonPath("$.data.authorCount").value(1));
        mvc.perform(get("/profiles/"+viewer.userId()+"/following").header("Authorization","Bearer "+viewer.token()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.users[0].hasUnseenPosts").value(true));
        mvc.perform(get("/profiles/"+viewer.userId()+"/following").header("Authorization","Bearer "+outsider.token()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.users[0].hasUnseenPosts").value(false));
        mvc.perform(put("/feed/following/authors/"+author.userId()+"/read").header("Authorization","Bearer "+viewer.token())).andExpect(status().isOk());
        mvc.perform(get("/feed/following/unseen-summary").header("Authorization","Bearer "+viewer.token())).andExpect(status().isOk()).andExpect(jsonPath("$.data.postCount").value(0));
        publish(author,"第二条新动态");
        mvc.perform(put("/feed/following/read").header("Authorization","Bearer "+viewer.token())).andExpect(status().isOk());
        mvc.perform(get("/feed/following/unseen-summary").header("Authorization","Bearer "+viewer.token())).andExpect(status().isOk()).andExpect(jsonPath("$.data.postCount").value(0));
        mvc.perform(get("/profile").header("Authorization","Bearer "+viewer.token())).andExpect(status().isOk()).andExpect(jsonPath("$.data.followingCount").value(1));
        mvc.perform(get("/profile").header("Authorization","Bearer "+author.token())).andExpect(status().isOk()).andExpect(jsonPath("$.data.followerCount").value(1));
    }

    private String publish(UserSession author,String title)throws Exception{
        String body=mvc.perform(post("/feed").header("Authorization","Bearer "+author.token()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\""+title+"\",\"content\":\"测试内容\",\"topics\":[]}")) .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).path("data").path("id").asText();
    }

    private UserSession register(String account,String nickname)throws Exception{
        String body=mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content("{\"account\":\""+account+"\",\"password\":\"password123\",\"nickname\":\""+nickname+"\"}"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();JsonNode data=json.readTree(body).path("data");return new UserSession(data.path("userId").asText(),data.path("token").asText());
    }
    private record UserSession(String userId,String token){}
}
