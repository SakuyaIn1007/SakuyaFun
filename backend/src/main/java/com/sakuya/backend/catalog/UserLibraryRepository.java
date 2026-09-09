package com.sakuya.backend.catalog;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface UserLibraryRepository extends JpaRepository<UserLibraryBook,UUID>{List<UserLibraryBook> findByUserIdOrderByCollectedAtDesc(UUID userId);List<UserLibraryBook> findByBookId(String bookId);Optional<UserLibraryBook> findByUserIdAndBookId(UUID userId,String bookId);boolean existsByUserIdAndBookId(UUID userId,String bookId);void deleteByUserIdAndBookId(UUID userId,String bookId);}
