package com.sakuya.backend.social;

import com.sakuya.backend.common.ApiResponse;
import com.sakuya.backend.common.AuthSupport;
import com.sakuya.backend.common.BusinessException;
import com.sakuya.backend.user.User;
import com.sakuya.backend.user.UserRepository;
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
 * 说明：获赞与收藏统计预留为 0，待动态模块的数据表落地后由聚合查询替换，不会伪造客户端数据。
 */
@RestController
@RequestMapping("/profiles")
public class ProfileSocialController {
    private final UserRepository users;
    private final FollowRepository follows;

    public ProfileSocialController(UserRepository users, FollowRepository follows) {
        this.users = users;
        this.follows = follows;
    }

    /** 读取指定用户的公开主页和三项统计。 */
    @GetMapping("/{userId}")
    public ApiResponse<PublicProfileDto> getProfile(Authentication auth, @PathVariable UUID userId) {
        UUID viewerId = AuthSupport.userId(auth);
        User target = target(userId);
        ensureProfileVisible(viewerId, target);
        return ApiResponse.ok(PublicProfileDto.from(target, viewerId, follows));
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
        return ApiResponse.ok(toPage(follows.findByFollowerIdOrderByCreatedAtDesc(userId, PageRequest.of(page, pageSize)), viewerId, true));
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
        return ApiResponse.ok(toPage(follows.findByFollowingIdOrderByCreatedAtDesc(userId, PageRequest.of(page, pageSize)), viewerId, false));
    }

    /** 当前登录用户关注指定用户；重复调用保持幂等。 */
    @PostMapping("/{userId}/follow")
    @Transactional
    public ApiResponse<RelationshipUserDto> follow(Authentication auth, @PathVariable UUID userId) {
        UUID viewerId = AuthSupport.userId(auth);
        if (viewerId.equals(userId)) throw new BusinessException(400, "不能关注自己");
        User target = target(userId);
        if (!target.isCanBeAddedByStrangers()) throw new BusinessException(403, "该用户已开启关注隐私设置");
        if (!follows.existsByFollowerIdAndFollowingId(viewerId, userId)) follows.save(new Follow(viewerId, userId));
        return ApiResponse.ok(RelationshipUserDto.from(target, true));
    }

    /** 当前登录用户取消对指定用户的关注；未关注时同样返回成功。 */
    @DeleteMapping("/{userId}/follow")
    @Transactional
    public ApiResponse<RelationshipUserDto> unfollow(Authentication auth, @PathVariable UUID userId) {
        UUID viewerId = AuthSupport.userId(auth);
        User target = target(userId);
        follows.deleteByFollowerIdAndFollowingId(viewerId, userId);
        return ApiResponse.ok(RelationshipUserDto.from(target, false));
    }

    private RelationshipPageDto toPage(Page<Follow> relationPage, UUID viewerId, boolean followingList) {
        List<RelationshipUserDto> users = relationPage.getContent().stream()
            .map(relation -> target(followingList ? relation.getFollowingId() : relation.getFollowerId()))
            .map(user -> RelationshipUserDto.from(user, follows.existsByFollowerIdAndFollowingId(viewerId, user.getId())))
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
     * followingCount、followerCount、likesAndFavoritesCount 分别用于三项统计；
     * isFollowing 表示当前登录用户是否已关注该主页用户。
     */
    public record PublicProfileDto(
        String userId, String avatarUrl, String nickname, String signature,
        long followingCount, long followerCount, long likesAndFavoritesCount, boolean isFollowing
    ) {
        static PublicProfileDto from(User user, UUID viewerId, FollowRepository follows) {
            return new PublicProfileDto(user.getId().toString(), user.getAvatarUrl(), user.getNickname(), user.getSignature(),
                follows.countByFollowerId(user.getId()), follows.countByFollowingId(user.getId()), 0L,
                follows.existsByFollowerIdAndFollowingId(viewerId, user.getId()));
        }
    }

    /**
     * 关系列表项字段说明：
     * userId 为跳转用户主页的标识；name、initial、description、avatarColor 用于列表渲染；
     * isFollowing 表示当前登录用户对该列表项用户的关注状态。
     */
    public record RelationshipUserDto(String userId, String name, String initial, String description, long avatarColor, boolean isFollowing) {
        static RelationshipUserDto from(User user, boolean isFollowing) {
            String name = user.getNickname();
            String initial = name == null || name.isBlank() ? "?" : name.substring(0, 1).toUpperCase();
            return new RelationshipUserDto(user.getId().toString(), name, initial, user.getSignature(), 0L, isFollowing);
        }
    }

    /** users 为当前页数据，nextPage 为下一页索引；为 null 时表示已没有更多数据。 */
    public record RelationshipPageDto(List<RelationshipUserDto> users, Integer nextPage) {}
}
