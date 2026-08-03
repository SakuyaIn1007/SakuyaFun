package com.sakuya.backend.chat;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="conversations")
public class Conversation {
 @Id private UUID id; @Column(nullable=false) private String title; @Column(nullable=false) private Instant createdAt;
 protected Conversation(){} public Conversation(String title){id=UUID.randomUUID();this.title=title;createdAt=Instant.now();}
 public UUID getId(){return id;} public String getTitle(){return title;}
}
