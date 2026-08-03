package com.sakuya.backend.catalog;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="user_library",uniqueConstraints=@UniqueConstraint(columnNames={"user_id","book_id"}))
public class UserLibraryBook {
 @Id private UUID id; @Column(name="user_id",nullable=false) private UUID userId; @Column(name="book_id",nullable=false) private String bookId; @Column(nullable=false) private Instant collectedAt; private float progress;
 protected UserLibraryBook(){} public UserLibraryBook(UUID userId,String bookId){id=UUID.randomUUID();this.userId=userId;this.bookId=bookId;collectedAt=Instant.now();}
 public UUID getUserId(){return userId;} public String getBookId(){return bookId;} public Instant getCollectedAt(){return collectedAt;} public float getProgress(){return progress;} public void setProgress(float value){progress=Math.max(0,Math.min(1,value));}
}
