package com.sakuya.backend;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*; import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*; import com.fasterxml.jackson.databind.ObjectMapper; import org.junit.jupiter.api.Test; import org.springframework.beans.factory.annotation.Autowired; import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc; import org.springframework.boot.test.context.SpringBootTest; import org.springframework.http.MediaType; import org.springframework.test.web.servlet.MockMvc;
@SpringBootTest @AutoConfigureMockMvc
class AuthApiIntegrationTest {
 @Autowired MockMvc mvc;
 @Autowired ObjectMapper json;
 @Test void demoUserCanLogin() throws Exception {mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"account\":\"alice\",\"password\":\"password123\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200)).andExpect(jsonPath("$.data.token").isNotEmpty());}
 @Test void rejectsBadPassword() throws Exception {mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"account\":\"alice\",\"password\":\"bad\"}")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(401));}
 @Test void authenticatedApisMatchClientEnvelope() throws Exception {
  String body=mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"account\":\"alice\",\"password\":\"password123\"}")).andReturn().getResponse().getContentAsString();
  String token=json.readTree(body).path("data").path("token").asText();
  for(String path:new String[]{"/profile","/friends","/conversations","/home/recommendations","/library"})
   mvc.perform(get(path).header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200)).andExpect(jsonPath("$.data").exists());
 }
 @Test void unauthorizedMessageKeepsChineseUtf8() throws Exception {
  mvc.perform(get("/profile")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("登录已过期，请重新登录"));
 }
 @Test void directConversationUsesConversationIdAndReadingClearsUnread() throws Exception {
  String alice=token("alice"); String bob=token("bob");
  String friends=mvc.perform(get("/friends").header("Authorization","Bearer "+alice)).andReturn().getResponse().getContentAsString();
  String bobId=json.readTree(friends).path("data").get(0).path("id").asText();
  String direct=mvc.perform(post("/friends/"+bobId+"/conversation").header("Authorization","Bearer "+alice)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  String conversationId=json.readTree(direct).path("data").path("id").asText();
  mvc.perform(post("/conversations/"+conversationId+"/messages").header("Authorization","Bearer "+bob).contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"新消息\",\"messageType\":\"text\"}")).andExpect(status().isOk());
  mvc.perform(get("/conversations").header("Authorization","Bearer "+alice)).andExpect(jsonPath("$.data[0].unreadCount").value(org.hamcrest.Matchers.greaterThan(0)));
  mvc.perform(get("/conversations/"+conversationId+"/messages").header("Authorization","Bearer "+alice)).andExpect(status().isOk());
  mvc.perform(get("/conversations").header("Authorization","Bearer "+alice)).andExpect(jsonPath("$.data[0].unreadCount").value(0));
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
}
