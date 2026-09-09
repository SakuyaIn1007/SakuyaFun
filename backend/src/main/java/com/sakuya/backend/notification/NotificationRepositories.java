package com.sakuya.backend.notification;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

interface AppNotificationRepository extends JpaRepository<AppNotification,UUID>{
    Page<AppNotification> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId,Pageable pageable);
    long countByRecipientIdAndReadAtIsNull(UUID recipientId);
    long countByRecipientIdAndReadAtIsNullAndTypeIn(UUID recipientId,Collection<AppNotification.Type> types);
    boolean existsBySourceKey(String sourceKey);
    @Modifying @Query("update AppNotification n set n.readAt=:now where n.recipientId=:user and n.readAt is null") int markAllRead(@Param("user")UUID user,@Param("now")Instant now);
    @Modifying @Query("update AppNotification n set n.readAt=:now where n.recipientId=:user and n.readAt is null and n.type in :types")
    int markTypesRead(@Param("user")UUID user,@Param("types")Collection<AppNotification.Type> types,@Param("now")Instant now);
}
interface NotificationDeviceRepository extends JpaRepository<NotificationDevice,UUID>{
    Optional<NotificationDevice> findByInstallationId(String installationId);
    List<NotificationDevice> findByUserId(UUID userId);
    void deleteByInstallationIdAndUserId(String installationId,UUID userId);
    void deleteByFcmTokenIn(Collection<String> tokens);
}
interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference,UUID>{}
interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox,UUID>{
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from NotificationOutbox o where o.status='PENDING' and o.deliverAfter<=:now order by o.deliverAfter asc")
    List<NotificationOutbox> lockDue(@Param("now")Instant now,Pageable pageable);
    List<NotificationOutbox> findByAggregateKeyAndStatus(String aggregateKey,NotificationOutbox.Status status);
}
