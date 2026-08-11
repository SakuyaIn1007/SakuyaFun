package com.sakuya.backend;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*; import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*; import com.fasterxml.jackson.databind.ObjectMapper; import org.junit.jupiter.api.Test; import org.springframework.beans.factory.annotation.Autowired; import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc; import org.springframework.boot.test.context.SpringBootTest; import org.springframework.http.MediaType; import org.springframework.test.web.servlet.MockMvc;
@SpringBootTest @AutoConfigureMockMvc
class AuthApiIntegrationTest {
 @Autowired MockMvc mvc;
 @Autowired ObjectMapper json;
 @Test void demoUserCanLogin() throws Exception {mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"account\":\"alice\",\"password\":\"password123\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200)).andExpect(jsonPath("$.data.token").isNotEmpty());}
 @Test void sakuyaAccountHasCompleteDemoData() throws Exception {
  String token=token("sakuya");
  mvc.perform(get("/profile").header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("$.data.nickname").value("咲夜")).andExpect(jsonPath("$.data.email").value("sakuya@example.test"));
  mvc.perform(get("/friends").header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(2));
  mvc.perform(get("/library").header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(3));
  mvc.perform(get("/conversations").header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(2));
  mvc.perform(get("/friends/requests").header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("$.data[0].name").value("凛"));
 }
 @Test void rejectsBadPassword() throws Exception {mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"account\":\"alice\",\"password\":\"bad\"}")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(401));}
 @Test void authenticatedApisMatchClientEnvelope() throws Exception {
  String body=mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"account\":\"alice\",\"password\":\"password123\"}")).andReturn().getResponse().getContentAsString();
  String token=json.readTree(body).path("data").path("token").asText();
  for(String path:new String[]{"/profile","/friends","/conversations","/home/recommendations","/novels/releases","/library"})
   mvc.perform(get(path).header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200)).andExpect(jsonPath("$.data").exists());
 }
 @Test void publishedNovelReleaseScheduleIncludesDateAndNovel() throws Exception {
  String token=token("sakuya");
  mvc.perform(get("/novels/releases").header("Authorization","Bearer "+token)).andExpect(status().isOk())
   .andExpect(jsonPath("$.data.length()").value(10)).andExpect(jsonPath("$.data[0].releaseDate").value("2025-12-25"))
   .andExpect(jsonPath("$.data[1].novel.id").value("86")).andExpect(jsonPath("$.data[1].isRecommended").value(true));
 }
 @Test void unauthorizedMessageKeepsChineseUtf8() throws Exception {
  mvc.perform(get("/profile")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("登录已过期，请重新登录"));
 }
 @Test void directConversationUsesConversationIdAndReadingClearsUnread() throws Exception {
  String alice=token("alice"); String bob=token("bob");
  String bobProfile=mvc.perform(get("/profile").header("Authorization","Bearer "+bob)).andReturn().getResponse().getContentAsString();
  String bobId=json.readTree(bobProfile).path("data").path("userId").asText();
  String direct=mvc.perform(post("/friends/"+bobId+"/conversation").header("Authorization","Bearer "+alice)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  String conversationId=json.readTree(direct).path("data").path("id").asText();
  mvc.perform(post("/conversations/"+conversationId+"/messages").header("Authorization","Bearer "+bob).contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"新消息\",\"messageType\":\"text\"}")).andExpect(status().isOk());
  String unreadConversations=mvc.perform(get("/conversations").header("Authorization","Bearer "+alice)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  org.junit.jupiter.api.Assertions.assertTrue(conversationUnreadCount(unreadConversations,conversationId)>0);
  mvc.perform(get("/conversations/"+conversationId+"/messages").header("Authorization","Bearer "+alice)).andExpect(status().isOk());
  String readConversations=mvc.perform(get("/conversations").header("Authorization","Bearer "+alice)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  org.junit.jupiter.api.Assertions.assertEquals(0,conversationUnreadCount(readConversations,conversationId));
 }
 @Test void cloudLibraryReturnsReadableContentUrl() throws Exception {
  String alice=token("alice");
  String added=mvc.perform(post("/library/sword-art-online").header("Authorization","Bearer "+alice)).andExpect(status().isOk()).andExpect(jsonPath("$.data.filePath").isNotEmpty()).andReturn().getResponse().getContentAsString();
  String contentPath=java.net.URI.create(json.readTree(added).path("data").path("filePath").asText()).getPath();
  mvc.perform(get(contentPath).header("Authorization","Bearer "+alice)).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("刀剑神域")));
 }
 @Test void profileUpdateIsPersistedAndReturnedWithClientFields() throws Exception {
  String alice=token("alice");
  String request="""
   {"avatarUrl":"https://example.test/avatar.png","nickname":"新的爱丽丝","signature":"正在阅读","gender":2,
    "birthday":"2000-01-02","regionCode":"CN-31","phoneNumber":"13800138000","email":"alice@example.test",
    "pokeText":"拍了拍肩膀","ringtoneName":"星河","privacySettings":{"canBeAddedByStrangers":false,
    "showProfileToStrangers":true,"muteMessagesFromUnknown":true}}
   """;
  mvc.perform(put("/profile").header("Authorization","Bearer "+alice).contentType(MediaType.APPLICATION_JSON).content(request))
   .andExpect(status().isOk()).andExpect(jsonPath("$.data.nickname").value("新的爱丽丝"))
   .andExpect(jsonPath("$.data.privacySettings.canBeAddedByStrangers").value(false));
  mvc.perform(get("/profile").header("Authorization","Bearer "+alice)).andExpect(status().isOk())
   .andExpect(jsonPath("$.data.signature").value("正在阅读")).andExpect(jsonPath("$.data.regionCode").value("CN-31"))
   .andExpect(jsonPath("$.data.ringtoneName").value("星河"));
 }
 @Test void profileRejectsInvalidClientData() throws Exception {
  String alice=token("alice");
  mvc.perform(put("/profile").header("Authorization","Bearer "+alice).contentType(MediaType.APPLICATION_JSON)
   .content("{\"nickname\":\"Alice\",\"gender\":9,\"phoneNumber\":\"123\"}"))
   .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400));
 }
 /**
  * 覆盖他人主页、分页关系列表和关注生命周期。
  * 执行流程：alice 读取 bob 主页 -> 关注 bob -> 校验 bob 粉丝与 alice 关注列表 -> 取消关注并校验状态回写。
  */
 @Test void publicProfileAndFollowApisUseProfilesPath() throws Exception {
  String alice=token("alice"); String bob=token("bob");
  String bobId=json.readTree(mvc.perform(get("/profile").header("Authorization","Bearer "+bob)).andReturn().getResponse().getContentAsString()).path("data").path("userId").asText();
  mvc.perform(get("/profiles/"+bobId).header("Authorization","Bearer "+alice)).andExpect(status().isOk())
   .andExpect(jsonPath("$.data.userId").value(bobId)).andExpect(jsonPath("$.data.followingCount").isNumber())
   .andExpect(jsonPath("$.data.followerCount").isNumber()).andExpect(jsonPath("$.data.likesAndFavoritesCount").value(0));
  mvc.perform(post("/profiles/"+bobId+"/follow").header("Authorization","Bearer "+alice)).andExpect(status().isOk())
   .andExpect(jsonPath("$.data.userId").value(bobId)).andExpect(jsonPath("$.data.isFollowing").value(true));
  mvc.perform(get("/profiles/"+bobId+"/followers?page=0&pageSize=20").header("Authorization","Bearer "+alice)).andExpect(status().isOk())
   .andExpect(jsonPath("$.data.users[0].userId").exists()).andExpect(jsonPath("$.data.nextPage").doesNotExist());
  String aliceId=json.readTree(mvc.perform(get("/profile").header("Authorization","Bearer "+alice)).andReturn().getResponse().getContentAsString()).path("data").path("userId").asText();
  mvc.perform(get("/profiles/"+aliceId+"/following?page=0&pageSize=20").header("Authorization","Bearer "+alice)).andExpect(status().isOk())
   .andExpect(jsonPath("$.data.users[0].userId").value(bobId));
  mvc.perform(delete("/profiles/"+bobId+"/follow").header("Authorization","Bearer "+alice)).andExpect(status().isOk())
   .andExpect(jsonPath("$.data.isFollowing").value(false));
 }
 @Test void friendRequestOnlyBecomesFriendAfterServerAcceptance() throws Exception {
  String suffix=java.util.UUID.randomUUID().toString().replace("-","").substring(0,10);
  String account="charlie"+suffix;
  String registered=mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
   .content("{\"account\":\""+account+"\",\"password\":\"password123\",\"nickname\":\"查理\"}"))
   .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  String charlieToken=json.readTree(registered).path("data").path("token").asText();
  String charlieId=json.readTree(registered).path("data").path("userId").asText();
  String bobToken=token("bob");
  String bobProfile=mvc.perform(get("/profile").header("Authorization","Bearer "+bobToken)).andReturn().getResponse().getContentAsString();
  String bobId=json.readTree(bobProfile).path("data").path("userId").asText();
  mvc.perform(post("/friends/requests").header("Authorization","Bearer "+charlieToken).contentType(MediaType.APPLICATION_JSON)
   .content("{\"userId\":\""+bobId+"\",\"message\":\"一起读书吧\"}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
  String pending=mvc.perform(get("/friends/requests").header("Authorization","Bearer "+bobToken)).andExpect(status().isOk())
   .andReturn().getResponse().getContentAsString();
  var pendingItems=json.readTree(pending).path("data");
  String requestId="";
  for(var item:pendingItems)if(charlieId.equals(item.path("userId").asText()))requestId=item.path("id").asText();
  org.junit.jupiter.api.Assertions.assertFalse(requestId.isBlank());
  mvc.perform(put("/friends/requests/"+requestId+"/accept").header("Authorization","Bearer "+bobToken))
   .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(charlieId));
  mvc.perform(get("/friends").header("Authorization","Bearer "+charlieToken)).andExpect(status().isOk())
   .andExpect(content().string(org.hamcrest.Matchers.containsString(bobId)));
 }
 @Test void expiredOrInvalidTokenReturnsHttp401Envelope() throws Exception {
  mvc.perform(get("/profile").header("Authorization","Bearer invalid.jwt.token"))
   .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(401));
 }
 @Test void unsupportedMessageTypeIsRejectedInsteadOfStored() throws Exception {
  String alice=token("alice");
  String bobId=json.readTree(mvc.perform(get("/friends").header("Authorization","Bearer "+alice)).andReturn().getResponse().getContentAsString())
   .path("data").get(0).path("id").asText();
  String direct=mvc.perform(post("/friends/"+bobId+"/conversation").header("Authorization","Bearer "+alice)).andReturn().getResponse().getContentAsString();
  String conversationId=json.readTree(direct).path("data").path("id").asText();
  mvc.perform(post("/conversations/"+conversationId+"/messages").header("Authorization","Bearer "+alice)
   .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"not real\",\"messageType\":\"unknown\"}"))
   .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400));
 }
 private String token(String account) throws Exception {
  String body=mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"account\":\""+account+"\",\"password\":\"password123\"}")).andReturn().getResponse().getContentAsString();
  return json.readTree(body).path("data").path("token").asText();
 }
 private int conversationUnreadCount(String body,String conversationId) throws Exception {
  for(var item:json.readTree(body).path("data"))if(conversationId.equals(item.path("id").asText()))return item.path("unreadCount").asInt();
  throw new AssertionError("找不到会话："+conversationId);
 }
}
