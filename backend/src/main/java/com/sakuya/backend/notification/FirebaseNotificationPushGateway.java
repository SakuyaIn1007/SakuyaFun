package com.sakuya.backend.notification;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.*;
import com.google.firebase.messaging.*;
import java.io.*;
import java.util.*;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.Component;

/** 使用 Firebase Admin SDK 发送 notification+data 消息；凭证只从运行环境读取，不进入仓库。 */
@Component @ConditionalOnProperty(name="app.notifications.firebase.enabled",havingValue="true")
class FirebaseNotificationPushGateway implements NotificationPushGateway {
    FirebaseNotificationPushGateway(@Value("${app.notifications.firebase.credentials-path:}")String path)throws IOException{
        if(FirebaseApp.getApps().isEmpty()){
            GoogleCredentials credentials=path.isBlank()?GoogleCredentials.getApplicationDefault():GoogleCredentials.fromStream(new FileInputStream(path));
            FirebaseApp.initializeApp(FirebaseOptions.builder().setCredentials(credentials).build());
        }
    }
    public List<String> send(List<String> tokens,String title,String content,AppNotification n){
        if(tokens.isEmpty())return List.of();
        MulticastMessage message=MulticastMessage.builder().addAllTokens(tokens).setNotification(Notification.builder().setTitle(title).setBody(content).build())
            .putData("notificationId",n.getId().toString()).putData("type",n.getType().name()).putData("targetId",n.getTargetId())
            .putData("targetUserId",n.getTargetUserId()==null?"":n.getTargetUserId().toString())
            .setAndroidConfig(AndroidConfig.builder().setPriority(AndroidConfig.Priority.HIGH).setNotification(AndroidNotification.builder().setChannelId("social_updates").setTag(n.getAggregateKey()==null?n.getId().toString():n.getAggregateKey()).build()).build()).build();
        try{BatchResponse response=FirebaseMessaging.getInstance().sendEachForMulticast(message);List<String> invalid=new ArrayList<>();for(int i=0;i<response.getResponses().size();i++){SendResponse result=response.getResponses().get(i);if(!result.isSuccessful()&&result.getException()!=null&&result.getException().getMessagingErrorCode()==MessagingErrorCode.UNREGISTERED)invalid.add(tokens.get(i));}return invalid;}catch(FirebaseMessagingException e){throw new IllegalStateException(e.getMessage(),e);}
    }
}

@Configuration
class NotificationPushFallbackConfiguration {
    @Bean @ConditionalOnMissingBean(NotificationPushGateway.class) NotificationPushGateway noop(){return (tokens,title,content,notification)->List.of();}
}
