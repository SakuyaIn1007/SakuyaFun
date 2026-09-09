package com.sakuya.backend.search;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sakuya.backend.catalog.Book;
import com.sakuya.backend.catalog.BookRepository;
import com.sakuya.backend.user.UserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * SearchApiIntegrationTest.java
 * 职责说明：验证小说、动态、用户和综合搜索的统一协议。
 * 执行流程：创建隔离用户/书籍/动态 -> 分类查询 -> 综合查询 -> 验证数据驱动热词。
 */
@SpringBootTest
@AutoConfigureMockMvc
class SearchApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired BookRepository books;
    @Autowired UserRepository users;

    @Test
    void unifiedSearchReturnsNovelDynamicAndVisibleUserResults() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String keyword = "统一检索" + suffix;
        TestUser author = register("searchauthor" + suffix, keyword);
        TestUser viewer = register("searchviewer" + suffix, "搜索访问者" + suffix);
        String bookId = "search-book-" + suffix;
        String bodyKeyword = "正文搜索内容" + suffix;
        String tagKeyword = "统一标签" + suffix;
        books.save(new Book(bookId, keyword + "小说", "测试作者", "", 5f, List.of(tagKeyword), "搜索小说简介", null));
        String postBody = mvc.perform(post("/feed").header("Authorization", "Bearer " + author.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"" + keyword + "动态\",\"content\":\"" + bodyKeyword
                    + "\",\"topics\":[\"" + tagKeyword + "\"]}"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String postId = json.readTree(postBody).path("data").path("id").asText();

        mvc.perform(get("/search").param("keyword", keyword).param("type", "novel")
                .header("Authorization", "Bearer " + viewer.token()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].id").value(bookId))
            .andExpect(jsonPath("$.data.items[0].type").value("novel"));
        mvc.perform(get("/search").param("keyword", keyword).param("type", "dynamic")
                .header("Authorization", "Bearer " + viewer.token()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].id").value(postId))
            .andExpect(jsonPath("$.data.items[0].type").value("dynamic"));
        mvc.perform(get("/search").param("keyword", bodyKeyword).param("type", "dynamic")
                .header("Authorization", "Bearer " + viewer.token()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].id").value(postId));
        mvc.perform(get("/search").param("keyword", tagKeyword).param("type", "dynamic")
                .header("Authorization", "Bearer " + viewer.token()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].id").value(postId));
        mvc.perform(get("/search").param("keyword", keyword).param("type", "user")
                .header("Authorization", "Bearer " + viewer.token()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].id").value(author.userId()))
            .andExpect(jsonPath("$.data.items[0].type").value("user"));
        mvc.perform(get("/search").param("keyword", keyword).param("pageSize", "20")
                .header("Authorization", "Bearer " + viewer.token()))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"type\":\"novel\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"type\":\"dynamic\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"type\":\"user\"")));
        mvc.perform(get("/search/hot-keywords").header("Authorization", "Bearer " + viewer.token()))
            .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString(tagKeyword)));

        // 隐私主页不能通过搜索绕过访问控制，但用户本人仍能检索自己的账号或昵称。
        var hiddenAuthor = users.findById(UUID.fromString(author.userId())).orElseThrow();
        hiddenAuthor.setShowProfileToStrangers(false);
        users.saveAndFlush(hiddenAuthor);
        mvc.perform(get("/search").param("keyword", keyword).param("type", "user")
                .header("Authorization", "Bearer " + viewer.token()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.items").isEmpty());
        mvc.perform(get("/search").param("keyword", keyword).param("type", "user")
                .header("Authorization", "Bearer " + author.token()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].id").value(author.userId()));
    }

    private TestUser register(String account, String nickname) throws Exception {
        String body = mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"account\":\"" + account + "\",\"password\":\"password123\",\"nickname\":\"" + nickname + "\"}"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return new TestUser(json.readTree(body).path("data").path("userId").asText(), json.readTree(body).path("data").path("token").asText());
    }

    private record TestUser(String userId, String token) { }
}
