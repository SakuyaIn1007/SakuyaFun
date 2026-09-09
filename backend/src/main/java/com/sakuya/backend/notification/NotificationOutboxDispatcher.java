package com.sakuya.backend.notification;

import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 定时认领 Outbox；同一动态两分钟内的点赞和收藏合并通知栏文案，历史记录保持逐条。 */
@Component
public class NotificationOutboxDispatcher {
    private final NotificationOutboxRepository outbox; private final AppNotificationRepository notifications; private final NotificationDeviceRepository devices; private final NotificationPreferenceRepository preferences; private final NotificationPushGateway gateway;
    public NotificationOutboxDispatcher(NotificationOutboxRepository o,AppNotificationRepository n,NotificationDeviceRepository d,NotificationPreferenceRepository p,NotificationPushGateway g){outbox=o;notifications=n;devices=d;preferences=p;gateway=g;}
    @Scheduled(fixedDelayString="${app.notifications.dispatch-delay-ms:15000}") @Transactional
    public void dispatch(){
        for(NotificationOutbox first:outbox.lockDue(Instant.now(),PageRequest.of(0,100))){
            if(first.getStatus()!=NotificationOutbox.Status.PENDING)continue;
            List<NotificationOutbox> batch=first.getAggregateKey()==null?List.of(first):outbox.findByAggregateKeyAndStatus(first.getAggregateKey(),NotificationOutbox.Status.PENDING);
            batch.forEach(NotificationOutbox::claim);
            try{
                List<AppNotification> values=batch.stream().map(x->notifications.findById(x.getNotificationId()).orElse(null)).filter(Objects::nonNull).toList();
                if(values.isEmpty()){batch.forEach(NotificationOutbox::sent);continue;}
                AppNotification anchor=values.get(0);NotificationPreference pref=preferences.findById(anchor.getRecipientId()).orElse(new NotificationPreference(anchor.getRecipientId()));
                boolean feed=anchor.getType().name().startsWith("FEED_");boolean chapter=anchor.getType()==AppNotification.Type.CHAPTER_UPDATE;boolean enabled=pref.isPushEnabled()&&(chapter|| (feed?pref.isFeedEnabled():pref.isFriendEnabled()));
                if(enabled){String title=values.size()>1?"动态收到新的互动":anchor.getTitle();String body=values.size()>1?values.size()+" 人赞了或收藏了你的动态":anchor.getContent();List<String> invalid=gateway.send(devices.findByUserId(anchor.getRecipientId()).stream().map(NotificationDevice::getFcmToken).toList(),title,body,anchor);if(!invalid.isEmpty())devices.deleteByFcmTokenIn(invalid);}
                batch.forEach(NotificationOutbox::sent);
            }catch(Exception e){batch.forEach(x->x.fail(e.getMessage()));}
        }
    }
}
