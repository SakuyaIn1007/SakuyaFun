package com.sakuya.backend.notification;

import com.sakuya.backend.common.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * NotificationController.java
 * 职责说明：提供通知历史、未读状态、设备 Token 与推送偏好 API。
 * 安全约束：所有通知、设备和偏好操作均从 Authentication 取得用户，拒绝跨用户访问。
 */
@RestController @RequestMapping("/notifications")
public class NotificationController {
    public enum UnreadCategory { FEED_INTERACTION, NEW_FOLLOWER, FRIEND_RELATION }
    private final AppNotificationRepository notifications; private final NotificationDeviceRepository devices; private final NotificationPreferenceRepository preferences;
    public NotificationController(AppNotificationRepository n,NotificationDeviceRepository d,NotificationPreferenceRepository p){notifications=n;devices=d;preferences=p;}

    @GetMapping ApiResponse<PageDto> list(Authentication auth,@RequestParam(defaultValue="0")@Min(0)int page,@RequestParam(defaultValue="20")@Min(1)@Max(50)int pageSize){
        UUID me=AuthSupport.userId(auth);var result=notifications.findByRecipientIdOrderByCreatedAtDesc(me,PageRequest.of(page,pageSize));
        return ApiResponse.ok(new PageDto(result.getContent().stream().map(ItemDto::from).toList(),result.hasNext()?page+1:null));
    }
    @GetMapping("/unread-count") ApiResponse<UnreadDto> unread(Authentication auth){return ApiResponse.ok(new UnreadDto(notifications.countByRecipientIdAndReadAtIsNull(AuthSupport.userId(auth))));}
    /** 按稳定业务分类返回未读数，客户端据此组合不同入口的小红点。 */
    @GetMapping("/unread-summary") ApiResponse<UnreadSummaryDto> unreadSummary(Authentication auth){
        UUID me=AuthSupport.userId(auth);
        return ApiResponse.ok(new UnreadSummaryDto(
            notifications.countByRecipientIdAndReadAtIsNull(me),
            notifications.countByRecipientIdAndReadAtIsNullAndTypeIn(me,types(UnreadCategory.FEED_INTERACTION)),
            notifications.countByRecipientIdAndReadAtIsNullAndTypeIn(me,types(UnreadCategory.NEW_FOLLOWER)),
            notifications.countByRecipientIdAndReadAtIsNullAndTypeIn(me,types(UnreadCategory.FRIEND_RELATION))
        ));
    }
    @PutMapping("/{id}/read") @Transactional ApiResponse<Void> read(Authentication auth,@PathVariable UUID id){UUID me=AuthSupport.userId(auth);AppNotification n=notifications.findById(id).orElseThrow(()->new BusinessException(404,"通知不存在"));if(!n.getRecipientId().equals(me))throw new BusinessException(404,"通知不存在");n.markRead();return ApiResponse.ok();}
    @PutMapping("/read-all") @Transactional ApiResponse<Void> readAll(Authentication auth){notifications.markAllRead(AuthSupport.userId(auth),Instant.now());return ApiResponse.ok();}
    /** 进入对应业务内容后只清除该分类，不误伤其他仍未查看的通知。 */
    @PutMapping("/read-category") @Transactional ApiResponse<Void> readCategory(Authentication auth,@Valid@RequestBody ReadCategoryRequest request){
        notifications.markTypesRead(AuthSupport.userId(auth),types(request.category()),Instant.now());
        return ApiResponse.ok();
    }

    @PostMapping("/devices") @Transactional ApiResponse<Void> register(Authentication auth,@Valid@RequestBody DeviceRequest request){UUID me=AuthSupport.userId(auth);NotificationDevice device=devices.findByInstallationId(request.installationId()).orElseGet(()->new NotificationDevice(me,request.installationId(),request.token(),request.platform()));device.update(me,request.token(),request.platform());devices.save(device);return ApiResponse.ok();}
    @DeleteMapping("/devices/{installationId}") @Transactional ApiResponse<Void> unregister(Authentication auth,@PathVariable String installationId){devices.deleteByInstallationIdAndUserId(installationId,AuthSupport.userId(auth));return ApiResponse.ok();}
    @GetMapping("/preferences") ApiResponse<PreferenceDto> preference(Authentication auth){UUID me=AuthSupport.userId(auth);return ApiResponse.ok(PreferenceDto.from(preferences.findById(me).orElseGet(()->preferences.save(new NotificationPreference(me)))));}
    @PutMapping("/preferences") @Transactional ApiResponse<PreferenceDto> updatePreference(Authentication auth,@Valid@RequestBody PreferenceDto request){UUID me=AuthSupport.userId(auth);NotificationPreference p=preferences.findById(me).orElseGet(()->new NotificationPreference(me));p.update(request.pushEnabled(),request.feedEnabled(),request.friendEnabled());return ApiResponse.ok(PreferenceDto.from(preferences.save(p)));}

    public record DeviceRequest(@NotBlank@Size(max=100)String installationId,@NotBlank@Size(max=512)String token,@Pattern(regexp="android")String platform){}
    public record PreferenceDto(boolean pushEnabled,boolean feedEnabled,boolean friendEnabled){static PreferenceDto from(NotificationPreference p){return new PreferenceDto(p.isPushEnabled(),p.isFeedEnabled(),p.isFriendEnabled());}}
    public record UnreadDto(long count){}
    public record UnreadSummaryDto(long total,long feedInteraction,long newFollowers,long friendRelation){}
    public record ReadCategoryRequest(@NotNull UnreadCategory category){}
    public record PageDto(List<ItemDto> items,Integer nextPage){}
    public record ItemDto(String id,String actorId,String type,String title,String content,String targetType,String targetId,String targetUserId,String createdAt,String readAt){static ItemDto from(AppNotification n){return new ItemDto(n.getId().toString(),n.getActorId().toString(),n.getType().name(),n.getTitle(),n.getContent(),n.getTargetType().name(),n.getTargetId(),n.getTargetUserId()==null?null:n.getTargetUserId().toString(),n.getCreatedAt().toString(),n.getReadAt()==null?null:n.getReadAt().toString());}}

    private static Set<AppNotification.Type> types(UnreadCategory category){return switch(category){
        case FEED_INTERACTION -> EnumSet.of(AppNotification.Type.FEED_COMMENT,AppNotification.Type.FEED_REPLY,AppNotification.Type.FEED_LIKE,AppNotification.Type.FEED_FAVORITE);
        case NEW_FOLLOWER -> EnumSet.of(AppNotification.Type.NEW_FOLLOWER);
        case FRIEND_RELATION -> EnumSet.of(AppNotification.Type.FRIEND_REQUEST,AppNotification.Type.FRIEND_ACCEPTED);
    };}
}
