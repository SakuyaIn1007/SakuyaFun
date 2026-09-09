package com.sakuya.backend.notification;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * NotificationOutbox.java
 * 职责说明：在业务事务中记录待派发任务，避免数据库成功但 FCM 调用失败导致通知永久丢失。
 * 执行流程：通知服务写入 PENDING -> 调度器认领并发送 -> 成功标记 SENT，临时失败指数退避，五次后 FAILED。
 */
@Entity @Table(name="notification_outbox",indexes=@Index(name="idx_outbox_due",columnList="status,deliver_after"))
public class NotificationOutbox {
    public enum Status { PENDING, SENDING, SENT, FAILED }
    @Id private UUID id;
    @Column(name="notification_id",nullable=false) private UUID notificationId;
    @Column(name="aggregate_key",length=160) private String aggregateKey;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=12) private Status status=Status.PENDING;
    @Column(name="deliver_after",nullable=false) private Instant deliverAfter;
    @Column(nullable=false) private int attempts=0;
    @Column(name="last_error",length=500) private String lastError;
    protected NotificationOutbox(){}
    public NotificationOutbox(UUID notificationId,String aggregateKey,Instant deliverAfter){id=UUID.randomUUID();this.notificationId=notificationId;this.aggregateKey=aggregateKey;this.deliverAfter=deliverAfter;}
    public UUID getId(){return id;} public UUID getNotificationId(){return notificationId;} public String getAggregateKey(){return aggregateKey;} public Status getStatus(){return status;}
    public void claim(){status=Status.SENDING;}
    public void sent(){status=Status.SENT;}
    public void fail(String error){attempts++;lastError=error==null?"FCM 派发失败":error.substring(0,Math.min(500,error.length()));if(attempts>=5)status=Status.FAILED;else{status=Status.PENDING;deliverAfter=Instant.now().plusSeconds(Math.min(300,1L<<attempts));}}
}
