package com.sakuya.backend.search;

import com.sakuya.backend.catalog.Book;
import com.sakuya.backend.catalog.BookRepository;
import com.sakuya.backend.common.BusinessException;
import com.sakuya.backend.feed.FeedPost;
import com.sakuya.backend.feed.FeedPostRepository;
import com.sakuya.backend.user.User;
import com.sakuya.backend.user.UserRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SearchService.java
 * 职责说明：在同一边界内组合已发布小说、公开用户和动态搜索，并输出统一分页协议。
 * 执行流程：标准化关键词/类型 -> 交给各领域 Repository 在数据库查询 -> 统一映射并稳定分页。
 */
@Service
public class SearchService {
    private final BookRepository books;
    private final FeedPostRepository posts;
    private final UserRepository users;

    public SearchService(BookRepository books, FeedPostRepository posts, UserRepository users) {
        this.books = books;
        this.posts = posts;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public SearchPage search(UUID viewerId, String rawKeyword, String rawType, int page, int pageSize) {
        String keyword = rawKeyword == null ? "" : rawKeyword.trim();
        if (keyword.isBlank()) throw new BusinessException(400, "请输入搜索关键词");
        SearchType type = SearchType.parse(rawType);
        if (type != null) return searchSingle(viewerId, keyword, type, page, pageSize);

        // 综合页从每个领域读取到当前偏移所需的最小窗口，再交错合并，避免按类型整段排列。
        int fetchSize = Math.multiplyExact(page + 1, pageSize);
        Page<Book> novelPage = novelPage(keyword, fetchSize);
        Page<FeedPost> dynamicPage = posts.search(keyword, PageRequest.of(0, fetchSize));
        Page<User> userPage = users.searchVisible(keyword, viewerId, PageRequest.of(0, fetchSize));
        List<SearchResult> merged = interleave(List.of(
            novelPage.getContent().stream().map(this::novel).toList(),
            dynamicPage.getContent().stream().map(this::dynamic).toList(),
            userPage.getContent().stream().map(this::user).toList()
        ));
        int from = page * pageSize;
        int to = Math.min(from + pageSize, merged.size());
        List<SearchResult> items = from >= merged.size() ? List.of() : merged.subList(from, to);
        long total = novelPage.getTotalElements() + dynamicPage.getTotalElements() + userPage.getTotalElements();
        return new SearchPage(List.copyOf(items), ((long) (page + 1) * pageSize < total) ? page + 1 : null);
    }

    private SearchPage searchSingle(UUID viewerId, String keyword, SearchType type, int page, int pageSize) {
        return switch (type) {
            case NOVEL -> page(novelPage(keyword, page, pageSize).map(this::novel), page);
            case DYNAMIC -> page(posts.search(keyword, PageRequest.of(page, pageSize)).map(this::dynamic), page);
            case USER -> page(users.searchVisible(keyword, viewerId, PageRequest.of(page, pageSize)).map(this::user), page);
        };
    }

    private Page<Book> novelPage(String keyword, int fetchSize) {
        return novelPage(keyword, 0, fetchSize);
    }

    private Page<Book> novelPage(String keyword, int page, int pageSize) {
        return books.findByPublishedTrueAndTitleContainingIgnoreCaseOrPublishedTrueAndAuthorContainingIgnoreCase(
            keyword, keyword, PageRequest.of(page, pageSize, Sort.by("title").ascending())
        );
    }

    private SearchPage page(Page<SearchResult> result, int page) {
        return new SearchPage(result.getContent(), result.hasNext() ? page + 1 : null);
    }

    /** 热词从书籍和最近动态标签的实际频次产生，无标签时再回退高评分书名。 */
    @Transactional(readOnly = true)
    public List<String> hotKeywords() {
        Map<String, Integer> frequency = new LinkedHashMap<>();
        books.findTop20ByOrderByRatingDesc().forEach(book -> book.getTags().forEach(tag -> addKeyword(frequency, tag)));
        posts.findTop20ByOrderByCreatedAtDesc().forEach(post -> Arrays.stream(post.getTags().split("\\|"))
            .forEach(tag -> addKeyword(frequency, tag)));
        List<String> result = frequency.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()).thenComparing(Map.Entry::getKey))
            .limit(10).map(Map.Entry::getKey).toList();
        if (!result.isEmpty()) return result;
        return books.findTop20ByOrderByRatingDesc().stream().map(Book::getTitle).filter(title -> !title.isBlank()).distinct().limit(10).toList();
    }

    private void addKeyword(Map<String, Integer> frequency, String raw) {
        String keyword = raw == null ? "" : raw.trim().replaceFirst("^#", "");
        if (!keyword.isBlank()) frequency.merge(keyword, 1, Integer::sum);
    }

    private List<SearchResult> interleave(List<List<SearchResult>> groups) {
        List<SearchResult> result = new ArrayList<>();
        int max = groups.stream().mapToInt(List::size).max().orElse(0);
        for (int index = 0; index < max; index++) {
            for (List<SearchResult> group : groups) if (index < group.size()) result.add(group.get(index));
        }
        return result;
    }

    private SearchResult novel(Book book) {
        return new SearchResult(book.getId(), book.getTitle(), book.getAuthor() + " · " + book.getDescription(), "novel", List.copyOf(book.getTags()));
    }

    private SearchResult dynamic(FeedPost post) {
        return new SearchResult(post.getId().toString(), post.getTitle(), post.getContent(), "dynamic", tags(post.getTags()));
    }

    private SearchResult user(User user) {
        return new SearchResult(user.getId().toString(), user.getNickname(), user.getSignature() == null ? "" : user.getSignature(), "user", List.of());
    }

    private List<String> tags(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return Arrays.stream(raw.split("\\|")).map(String::trim).filter(value -> !value.isBlank())
            .map(value -> value.replaceFirst("^#", "")).toList();
    }

    private enum SearchType {
        NOVEL, DYNAMIC, USER;
        static SearchType parse(String raw) {
            if (raw == null || raw.isBlank()) return null;
            try { return valueOf(raw.trim().toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException error) { throw new BusinessException(400, "不支持的搜索类型"); }
        }
    }

    public record SearchPage(List<SearchResult> items, Integer nextPage) { }
    public record SearchResult(String id, String title, String summary, String type, List<String> tags) { }
}
