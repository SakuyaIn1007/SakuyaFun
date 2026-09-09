package com.sakuya.backend.notification;

import jakarta.persistence.*;
import java.util.UUID;

/** 保存跨设备系统推送偏好；关闭推送不影响通知记录与未读数。 */
@Entity @Table(name="notification_preferences")
public class NotificationPreference {
    @Id @Column(name="user_id") private UUID userId;
    @Column(name="push_enabled",nullable=false) private boolean pushEnabled=true;
    @Column(name="feed_enabled",nullable=false) private boolean feedEnabled=true;
    @Column(name="friend_enabled",nullable=false) private boolean friendEnabled=true;
    protected NotificationPreference(){}
    public NotificationPreference(UUID userId){this.userId=userId;}
    public void update(boolean push,boolean feed,boolean friend){pushEnabled=push;feedEnabled=feed;friendEnabled=friend;}
    public boolean isPushEnabled(){return pushEnabled;} public boolean isFeedEnabled(){return feedEnabled;} public boolean isFriendEnabled(){return friendEnabled;}
}
