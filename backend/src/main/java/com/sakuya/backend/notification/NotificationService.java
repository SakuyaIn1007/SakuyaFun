package com.sakuya.backend.notification;

import com.sakuya.backend.user.*;
import java.time.*;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * NotificationService.java
 * 职责说明：把业务动作转换为持久化通知和同事务 Outbox，不在 Controller 中执行推送网络调用。
 * 执行流程：校验接收者与去重键 -> 保存通知 -> 根据互动类型决定立即派发或两分钟聚合派发。
 */
@Service
public class NotificationService {
    private final AppNotificationRepository notifications; private final NotificationOutboxRepository outbox; private final UserRepository users;
    public NotificationService(AppNotificationRepository notifications,NotificationOutboxRepository outbox,UserRepository users){this.notifications=notifications;this.outbox=outbox;this.users=users;}

    public void create(UUID recipient,UUID actor,AppNotification.Type type,AppNotification.TargetType targetType,String targetId,UUID targetUserId,String sourceId){
        if(recipient.equals(actor))return;
        String sourceKey=recipient+":"+type+":"+sourceId;
        if(notifications.existsBySourceKey(sourceKey))return;
        String actorName=users.findById(actor).map(User::getNickname).orElse("有人");
        String[] text=text(type,actorName);
        boolean aggregate=type==AppNotification.Type.FEED_LIKE||type==AppNotification.Type.FEED_FAVORITE;
        String aggregateKey=aggregate?recipient+":FEED_ENGAGEMENT:"+targetId:null;
        try{
            AppNotification saved=notifications.save(new AppNotification(recipient,actor,type,text[0],text[1],targetType,targetId,targetUserId,aggregateKey,sourceKey));
            outbox.save(new NotificationOutbox(saved.getId(),aggregateKey,Instant.now().plusSeconds(aggregate?120:0)));
        }catch(DataIntegrityViolationException ignored){/* 并发重复请求由唯一键兜底，业务事务保持幂等。 */}
    }

    /** 内容导入完成后为书架用户创建幂等更新提醒；系统事件复用接收者作为不可导航 actor。 */
    public void createChapterUpdate(UUID recipient,String bookId,String chapterId,String bookTitle,String chapterTitle){
        String sourceKey=recipient+":CHAPTER_UPDATE:"+chapterId;
        if(notifications.existsBySourceKey(sourceKey))return;
        String targetId=bookId+"|"+chapterId+"|"+chapterTitle;
        try{
            AppNotification saved=notifications.save(new AppNotification(recipient,recipient,AppNotification.Type.CHAPTER_UPDATE,
                "《"+bookTitle+"》更新了",chapterTitle,AppNotification.TargetType.BOOK_CHAPTER,targetId,null,null,sourceKey));
            outbox.save(new NotificationOutbox(saved.getId(),null,Instant.now()));
        }catch(DataIntegrityViolationException ignored){/* 多个导入任务并发时由 source_key 保证同章只提醒一次。 */}
    }

    private String[] text(AppNotification.Type type,String actor){return switch(type){
        case FEED_COMMENT -> new String[]{"动态有新评论",actor+"评论了你的动态"};
        case FEED_REPLY -> new String[]{"评论有新回复",actor+"回复了你的评论"};
        case FEED_LIKE -> new String[]{"动态收到点赞",actor+"赞了你的动态"};
        case FEED_FAVORITE -> new String[]{"动态被收藏",actor+"收藏了你的动态"};
        case NEW_FOLLOWER -> new String[]{"新增关注",actor+"关注了你"};
        case FRIEND_REQUEST -> new String[]{"新的好友申请",actor+"请求添加你为好友"};
        case FRIEND_ACCEPTED -> new String[]{"好友申请已通过",actor+"通过了你的好友申请"};
        case CHAPTER_UPDATE -> new String[]{"章节更新","你收藏的作品有新章节"};
    };}
}
