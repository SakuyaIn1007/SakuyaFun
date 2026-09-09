package com.sakuya.backend.notification;

import java.util.List;

/** 隔离 FCM SDK，未配置 Firebase 的开发和测试环境使用 Noop 实现，仍可完整验证通知中心。 */
public interface NotificationPushGateway {
    /** 返回 FCM 已确认失效的 Token，调用方负责从设备表清理。 */
    List<String> send(List<String> tokens,String title,String content,AppNotification notification);
}
