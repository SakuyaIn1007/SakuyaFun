package com.sakuya.backend.friend;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="friend_requests")
public class FriendRequest {
    public enum Status { PENDING, ACCEPTED, REJECTED }
    @Id private UUID id;
    @Column(nullable=false) private UUID senderId;
    @Column(nullable=false) private UUID receiverId;
    @Column(length=200) private String message;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private Status status;
    @Column(nullable=false) private Instant createdAt;
    protected FriendRequest() {}
    public FriendRequest(UUID senderId,UUID receiverId,String message){id=UUID.randomUUID();this.senderId=senderId;this.receiverId=receiverId;this.message=message;status=Status.PENDING;createdAt=Instant.now();}
    public UUID getId(){return id;} public UUID getSenderId(){return senderId;} public UUID getReceiverId(){return receiverId;} public String getMessage(){return message;} public Status getStatus(){return status;} public Instant getCreatedAt(){return createdAt;}
    public void accept(){status=Status.ACCEPTED;} public void reject(){status=Status.REJECTED;}
}
