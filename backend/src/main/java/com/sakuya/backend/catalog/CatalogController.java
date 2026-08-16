package com.sakuya.backend.catalog;
import com.sakuya.backend.common.*; import jakarta.validation.constraints.*; import java.time.ZoneId; import java.time.format.DateTimeFormatter; import java.util.*; import org.springframework.security.core.Authentication; import org.springframework.transaction.annotation.Transactional; import org.springframework.web.bind.annotation.*;
/**
 * CatalogController.java
 * 职责说明：提供本地书目、书架和可由 Wenku8 缓存增强的首页栏目。
 * 执行流程：首页优先查询最近一次成功同步的栏目缓存；首次同步尚未成功时回退到本地书目，
 * 不在 Android 请求线程访问 Wenku8，因此适配器关闭或同步失败不会把目录页变成 503。
 */
@RestController
public class CatalogController {
 private final BookRepository books; private final UserLibraryRepository library; private final Wenku8CatalogCacheService wenku8Cache; public CatalogController(BookRepository books,UserLibraryRepository library,Wenku8CatalogCacheService wenku8Cache){this.books=books;this.library=library;this.wenku8Cache=wenku8Cache;}
 @GetMapping("/home/recommendations") ApiResponse<List<ContentItem>> recommendations(Authentication auth){return ApiResponse.ok(items(loadWenku8(Wenku8CatalogFeed.RECOMMEND),AuthSupport.userId(auth)));}
 @GetMapping("/home/novels") ApiResponse<List<ContentItem>> novels(Authentication auth){return ApiResponse.ok(items(loadWenku8(Wenku8CatalogFeed.NOVELS),AuthSupport.userId(auth)));}
 @GetMapping("/home/rankings") ApiResponse<List<RankingItem>> rankings(Authentication auth){List<ContentItem> list=items(loadWenku8(Wenku8CatalogFeed.RANKING),AuthSupport.userId(auth));return ApiResponse.ok(java.util.stream.IntStream.range(0,list.size()).mapToObj(i->new RankingItem(i+1,list.get(i))).toList());}
 /**
  * 职责说明：提供已公布的轻小说更新资料。
  * 执行流程：维护端给出小说 ID、公布日期和卷信息 -> 控制器匹配现有书目 -> 客户端按日期分组展示时间表。
  */
 @GetMapping("/books/search") ApiResponse<List<ContentItem>> search(Authentication auth,@RequestParam String keyword){return ApiResponse.ok(items(books.findTop20ByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(keyword,keyword),AuthSupport.userId(auth)));}
 @GetMapping("/books/{id}") ApiResponse<BookDetail> detail(Authentication auth,@PathVariable String id){Book b=book(id);return ApiResponse.ok(new BookDetail(b.getId(),b.getTitle(),b.getAuthor(),b.getPublisher(),b.getRating(),List.copyOf(b.getTags()),b.getDescription(),library.existsByUserIdAndBookId(AuthSupport.userId(auth),id),b.getSourceType(),b.getCoverPath(),b.getStatus(),b.isCopyrightRestricted()));}
 @GetMapping(value="/books/{id}/content.txt",produces="text/plain;charset=UTF-8") String content(@PathVariable String id){Book b=book(id);return b.getTitle()+"\n\n作者："+b.getAuthor()+"\n出版社："+b.getPublisher()+"\n\n"+b.getDescription()+"\n\n这是开发环境提供的示例阅读内容。正式内容可替换为对象存储中的 EPUB/TXT 文件。";}
 @GetMapping("/library") ApiResponse<List<LibraryItem>> library(Authentication auth){UUID me=AuthSupport.userId(auth);return ApiResponse.ok(library.findByUserIdOrderByCollectedAtDesc(me).stream().map(x->toLibrary(x,book(x.getBookId()))).toList());}
 @PostMapping("/library/{bookId}") @Transactional ApiResponse<LibraryItem> add(Authentication auth,@PathVariable String bookId){UUID me=AuthSupport.userId(auth);Book b=book(bookId);UserLibraryBook item=library.findByUserIdAndBookId(me,bookId).orElseGet(()->library.save(new UserLibraryBook(me,bookId)));return ApiResponse.ok(toLibrary(item,b));}
 @PutMapping("/library/{bookId}/progress") @Transactional ApiResponse<Void> progress(Authentication auth,@PathVariable String bookId,@RequestBody ProgressRequest r){UserLibraryBook item=library.findByUserIdAndBookId(AuthSupport.userId(auth),bookId).orElseThrow(()->new BusinessException(404,"书架中没有该书"));item.setProgress(r.progress());return ApiResponse.ok();}
 @DeleteMapping("/library/{bookId}") @Transactional ApiResponse<Void> remove(Authentication auth,@PathVariable String bookId){library.deleteByUserIdAndBookId(AuthSupport.userId(auth),bookId);return ApiResponse.ok();}
 private Book book(String id){return books.findById(id).orElseThrow(()->new BusinessException(404,"图书不存在"));} private List<ContentItem> items(List<Book> source,UUID user){return source.stream().map(b->item(b,user)).toList();} private ContentItem item(Book b,UUID user){return new ContentItem(b.getId(),b.getTitle(),b.getAuthor(),b.getPublisher(),b.getRating(),List.copyOf(b.getTags()),b.getDescription(),library.existsByUserIdAndBookId(user,b.getId()),b.getSourceType(),b.getSourceNovelId(),b.getCoverPath(),b.getStatus(),b.isCopyrightRestricted());}
 /**
  * 目录读取的降级顺序：栏目缓存 -> 与栏目对应的本地演示/人工书目 -> 空列表。
  * 空列表同样是正常成功响应，使 Android 能展示已有的空态和刷新入口；这里绝不能触发同步，
  * 以免上游不可用时把用户请求阻塞或转换成 503。
  */
 private List<Book> loadWenku8(Wenku8CatalogFeed feed){List<Book> cached=wenku8Cache.cached(feed);return cached.isEmpty()?localFallback(feed):cached;}
 private List<Book> localFallback(Wenku8CatalogFeed feed){return switch(feed){case RECOMMEND->books.findByCategoryOrderByRatingDesc("recommend");case NOVELS->books.findByCategoryOrderByRatingDesc("novel");case RANKING->books.findTop20ByOrderByRatingDesc();};}
 private LibraryItem toLibrary(UserLibraryBook x,Book b){String contentPath="/books/"+b.getId()+"/content.txt";return new LibraryItem(b.getId(),b.getTitle(),b.getAuthor()+" · "+b.getPublisher(),b.getRating(),List.copyOf(b.getTags()),"TXT",DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(x.getCollectedAt().atZone(ZoneId.systemDefault())),contentPath,x.getProgress());}
 public record ContentItem(String id,String title,String author,String publisher,float rating,List<String> tags,String description,boolean isCollected,String source,String sourceNovelId,String coverUrl,String status,boolean copyrightRestricted){} public record RankingItem(int rank,ContentItem item){} public record BookDetail(String id,String title,String author,String publisher,float rating,List<String> tags,String description,boolean isCollected,String source,String coverUrl,String status,boolean copyrightRestricted){} public record LibraryItem(String id,String title,String subtitle,float rating,List<String> tags,String type,String collectedAt,String filePath,float progress){} public record ProgressRequest(@DecimalMin("0") @DecimalMax("1") float progress){}
}
