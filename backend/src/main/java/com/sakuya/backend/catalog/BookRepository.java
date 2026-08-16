package com.sakuya.backend.catalog;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface BookRepository extends JpaRepository<Book,String>{List<Book> findByCategoryOrderByRatingDesc(String category);List<Book> findTop20ByOrderByRatingDesc();List<Book> findTop20ByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(String title,String author);}
