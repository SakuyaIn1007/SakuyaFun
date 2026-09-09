package com.sakuya.backend.social;

import com.sakuya.backend.common.ApiResponse;
import com.sakuya.backend.common.AuthSupport;
import com.sakuya.backend.common.BusinessException;
import com.sakuya.backend.user.User;
import com.sakuya.backend.user.UserRepository;
import com.sakuya.backend.notification.AppNotification;
import com.sakuya.backend.notification.NotificationService;
import com.sakuya.backend.feed.FeedUpdateService;
import com.sakuya.backend.feed.FeedEngagementRepository;
import com.sakuya.backend.feed.FeedPostRepository;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * ProfileSocialController.java
 * 职责说明：提供他人主页、关注/粉丝分页列表，以及关注和取消关注接口。
 * 执行流程：请求先校验目标用户和隐私权限，再查询 Follow 表，最后统一包装为 ApiResponse 返回。
 * 说明：动态数和获赞与收藏数由动态仓储实时聚合，不在用户表保存易失真的冗余计数。
 */
@RestController
@RequestMapping("/profiles")
public class ProfileSocialController {
    private final UserRepository users;
    private final FollowRepository follows;
    private final NotificationService notificationService;
    private final FeedUpdateService feedUpdates;
    private final FeedPostRepository posts;
    private final FeedEngagementRepository engagements;

    public ProfileSocialController(UserRepository users, FollowRepository follows, NotificationService notificationService, FeedUpdateService feedUpdates, FeedPostRepository posts, FeedEngagementRepository engagements) {
        this.users = users;
        this.follows = follows;
        this.notificationService = notificationService;
        this.feedUpdates = feedUpdates;
        this.posts = posts;
        this.engagements = engagements;
    }

    /** 读取指定用户的公开主页、关系数据和动态聚合统计。 */
    @GetMapping("/{userId}")
    public ApiResponse<PublicProfileDto> getProfile(Authentication auth, @PathVariable UUID userId) {
        UUID viewerId = AuthSupport.userId(auth);
        User target = target(userId);
        ensureProfileVisible(viewerId, target);
        return ApiResponse.ok(PublicProfileDto.from(target, viewerId, follows, posts, engagements));
    }

    /** 分页读取指定用户关注的人；page 从 0 开始，pageSize 最大为 50。 */
    @GetMapping("/{userId}/following")
    public ApiResponse<RelationshipPageDto> getFollowing(
        Authentication auth,
        @PathVariable UUID userId,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(50) int pageSize
    ) {
        UUID viewerId = AuthSupport.userId(auth);
        User target = target(userId);
        ensureFollowListsVisible(viewerId, target);
        if(viewerId.equals(userId))feedUpdates.initialize(viewerId);
        return ApiResponse.ok(toPage(follows.findByFollowerIdOrderByCreatedAtDesc(userId, PageRequest.of(page, pageSize)), viewerId, true,viewerId.equals(userId)));
    }

    /** 分页读取关注指定用户的人；page 从 0 开始，pageSize 最大为 50。 */
    @GetMapping("/{userId}/followers")
    public ApiResponse<RelationshipPageDto> getFollowers(
        Authentication auth,
        @PathVariable UUID userId,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(50) int pageSize
    ) {
        UUID viewerId = AuthSupport.userId(auth);
        User target = target(userId);
        ensureFollowListsVisible(viewerId, target);
        return ApiResponse.ok(toPage(follows.findByFollowingIdOrderByCreatedAtDesc(userId, PageRequest.of(page, pageSize)), viewerId, false,false));
    }

    /** 当前登录用户关注指定用户；重复调用保持幂等。 */
    @PostMapping("/{userId}/follow")
    @Transactional
    public ApiResponse<RelationshipUserDto> follow(Authentication auth, @PathVariable UUID userId) {
        UUID viewerId = AuthSupport.userId(auth);
        if (viewerId.equals(userId)) throw new BusinessException(400, "不能关注自己");
        User target = target(userId);
        if (!target.isCanBeAddedByStrangers()) throw new BusinessException(403, "该用户已开启关注隐私设置");
        if (!follows.existsByFollowerIdAndFollowingId(viewerId, userId)) {
            feedUpdates.initialize(viewerId);
            Follow saved = follows.save(new Follow(viewerId, userId));
            notificationService.create(userId, viewerId, AppNotification.Type.NEW_FOLLOWER, AppNotification.TargetType.USER, viewerId.toString(), viewerId, viewerId + ":" + userId);
        }
        return ApiResponse.ok(RelationshipUserDto.from(target, true));
    }

    /** 当前登录用户取消对指定用户的关注；未关注时同样返回成功。 */
    @DeleteMapping("/{userId}/follow")
    @Transactional
    public ApiResponse<RelationshipUserDto> unfollow(Authentication auth, @PathVariable UUID userId) {
        UUID viewerId = AuthSupport.userId(auth);
        User target = target(userId);
        follows.deleteByFollowerIdAndFollowingId(viewerId, userId);
        feedUpdates.removeAuthor(viewerId,userId);
        return ApiResponse.ok(RelationshipUserDto.from(target, false));
    }

    private RelationshipPageDto toPage(Page<Follow> relationPage, UUID viewerId, boolean followingList,boolean includeUnseen) {
        List<RelationshipUserDto> users = relationPage.getContent().stream()
            .map(relation -> RelationshipUserDto.from(target(followingList ? relation.getFollowingId() : relation.getFollowerId()), follows.existsByFollowerIdAndFollowingId(viewerId,followingList?relation.getFollowingId():relation.getFollowerId()),includeUnseen&&feedUpdates.hasUnseenPosts(viewerId,relation)))
            .toList();
        Integer nextPage = relationPage.hasNext() ? relationPage.getNumber() + 1 : null;
        return new RelationshipPageDto(users, nextPage);
    }

    private User target(UUID userId) { return users.findById(userId).orElseThrow(() -> new BusinessException(404, "用户不存在")); }
    private void ensureProfileVisible(UUID viewerId, User target) {
        if (!viewerId.equals(target.getId()) && !target.isShowProfileToStrangers()) throw new BusinessException(403, "该用户已开启主页隐私设置");
    }
    private void ensureFollowListsVisible(UUID viewerId, User target) {
        if (!viewerId.equals(target.getId()) && !target.isShowFollowLists()) throw new BusinessException(403, "该用户已开启关注列表隐私设置");
    }

    /**
     * 他人主页响应字段说明：
     * userId 为用户唯一标识；avatarUrl、nickname、signature 用于主页头部；
     * followingCount、followerCount、postCount、likesAndFavoritesCount 用于主页统计；
     * isFollowing 表示当前登录用户是否已关注该主页用户。
     */
    public record PublicProfileDto(
        String userId, String avatarUrl, String nickname, String signature,
        long followingCount, long followerCount, long postCount, long likesAndFavoritesCount, boolean isFollowing
    ) {
        static PublicProfileDto from(User user, UUID viewerId, FollowRepository follows, FeedPostRepository posts, FeedEngagementRepository engagements) {
            return new PublicProfileDto(user.getId().toString(), user.getAvatarUrl(), user.getNickname(), user.getSignature(),
                follows.countByFollowerId(user.getId()), follows.countByFollowingId(user.getId()), posts.countByUserId(user.getId()), engagements.countReceivedByAuthorId(user.getId()),
                follows.existsByFollowerIdAndFollowingId(viewerId, user.getId()));
        }
    }

    /**
     * 关系列表项字段说明：
     * userId 为跳转用户主页的标识；name、initial、description、avatarColor 用于列表渲染；
     * isFollowing 表示当前登录用户对该列表项用户的关注状态。
     */
    public record RelationshipUserDto(String userId, String name, String initial, String description, long avatarColor, boolean isFollowing,boolean hasUnseenPosts) {
        static RelationshipUserDto from(User user, boolean isFollowing) {return from(user,isFollowing,false);}
        static RelationshipUserDto from(User user, boolean isFollowing,boolean hasUnseenPosts) {
            String name = user.getNickname();
            String initial = name == null || name.isBlank() ? "?" : name.substring(0, 1).toUpperCase();
            return new RelationshipUserDto(user.getId().toString(), name, initial, user.getSignature(), 0L, isFollowing,hasUnseenPosts);
        }
    }

    /** users 为当前页数据，nextPage 为下一页索引；为 null 时表示已没有更多数据。 */
    public record RelationshipPageDto(List<RelationshipUserDto> users, Integer nextPage) {}
}
