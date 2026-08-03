package com.sakuya.backend.catalog;
import jakarta.persistence.*; import java.util.*;
@Entity @Table(name="books")
public class Book {
 @Id private String id; @Column(nullable=false) private String title; @Column(nullable=false) private String author; @Column(nullable=false) private String publisher; private float rating; @ElementCollection(fetch=FetchType.EAGER) @CollectionTable(name="book_tags",joinColumns=@JoinColumn(name="book_id")) @Column(name="tag") private Set<String> tags=new LinkedHashSet<>(); @Column(length=2000) private String description; private String category;
 protected Book(){} public Book(String id,String title,String author,String publisher,float rating,List<String> tags,String description,String category){this.id=id;this.title=title;this.author=author;this.publisher=publisher;this.rating=rating;this.tags.addAll(tags);this.description=description;this.category=category;}
 public String getId(){return id;} public String getTitle(){return title;} public String getAuthor(){return author;} public String getPublisher(){return publisher;} public float getRating(){return rating;} public Set<String> getTags(){return tags;} public String getDescription(){return description;} public String getCategory(){return category;}
}
