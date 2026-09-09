package com.sakuya.backend.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.sakuya.backend.user.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** 验证章节更新提醒使用稳定 sourceKey 去重，并生成可供 Android 跳转的受控目标。 */
class ChapterUpdateNotificationTest {
    @Test void createsBookChapterNotificationOnce() {
        AppNotificationRepository notifications=mock(AppNotificationRepository.class);
        NotificationOutboxRepository outbox=mock(NotificationOutboxRepository.class);
        UserRepository users=mock(UserRepository.class);
        when(notifications.existsBySourceKey(any())).thenReturn(false);
        when(notifications.save(any())).thenAnswer(invocation->invocation.getArgument(0));
        NotificationService service=new NotificationService(notifications,outbox,users);
        UUID reader=UUID.randomUUID();
        service.createChapterUpdate(reader,"book-1","chapter-2","测试书","第二章");
        ArgumentCaptor<AppNotification> captor=ArgumentCaptor.forClass(AppNotification.class);
        verify(notifications).save(captor.capture());
        assertEquals(AppNotification.Type.CHAPTER_UPDATE,captor.getValue().getType());
        assertEquals(AppNotification.TargetType.BOOK_CHAPTER,captor.getValue().getTargetType());
        assertEquals("book-1|chapter-2|第二章",captor.getValue().getTargetId());
        verify(outbox).save(any());
    }
}
