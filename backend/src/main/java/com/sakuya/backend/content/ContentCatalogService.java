package com.sakuya.backend.content;

import com.sakuya.backend.catalog.Book;
import com.sakuya.backend.catalog.BookRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

/** 首页栏目只读取通用栏目表和已发布书目，来源导入器不会出现在用户请求链路中。 */
@Service
public class ContentCatalogService {
    private final ContentCatalogEntryRepository entries;
    private final BookRepository books;
    public ContentCatalogService(ContentCatalogEntryRepository entries, BookRepository books) { this.entries = entries; this.books = books; }
    public List<Book> books(String feed) {
        return entries.findByFeedOrderByDisplayOrderAsc(feed).stream()
            .map(entry -> books.findById(entry.getBookId()).orElse(null))
            .filter(Objects::nonNull).filter(Book::isPublished).toList();
    }
}
