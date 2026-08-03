package com.sakuya.backend.friend;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="friendships", uniqueConstraints=@UniqueConstraint(name="uk_friend_pair", columnNames={"user_id","friend_id"}))
public class Friendship {
    @Id private UUID id;
    @Column(name="user_id",nullable=false) private UUID userId;
    @Column(name="friend_id",nullable=false) private UUID friendId;
    @Column(nullable=false) private Instant createdAt;
    protected Friendship() {}
    public Friendship(UUID userId, UUID friendId) { id=UUID.randomUUID(); this.userId=userId; this.friendId=friendId; createdAt=Instant.now(); }
    public UUID getUserId(){return userId;} public UUID getFriendId(){return friendId;}
}
