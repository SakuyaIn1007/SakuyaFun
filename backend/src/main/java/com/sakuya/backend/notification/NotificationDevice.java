package com.sakuya.backend.notification;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** 用户与某次应用安装的 FCM Token 绑定；同一安装切换账号时覆盖归属，避免向旧账号泄漏通知。 */
@Entity @Table(name="notification_devices", uniqueConstraints={@UniqueConstraint(name="uk_notification_installation",columnNames="installation_id"),@UniqueConstraint(name="uk_notification_token",columnNames="fcm_token")})
public class NotificationDevice {
    @Id private UUID id;
    @Column(name="user_id",nullable=false) private UUID userId;
    @Column(name="installation_id",nullable=false,length=100) private String installationId;
    @Column(name="fcm_token",nullable=false,length=512) private String fcmToken;
    @Column(nullable=false,length=20) private String platform;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    protected NotificationDevice(){}
    public NotificationDevice(UUID userId,String installationId,String fcmToken,String platform){id=UUID.randomUUID();update(userId,fcmToken,platform);this.installationId=installationId;}
    public void update(UUID userId,String token,String platform){this.userId=userId;this.fcmToken=token;this.platform=platform;updatedAt=Instant.now();}
    public UUID getId(){return id;} public UUID getUserId(){return userId;} public String getFcmToken(){return fcmToken;}
}
