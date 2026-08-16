package com.sakuya.backend.feed;

import com.sakuya.backend.common.ApiResponse;
import com.sakuya.backend.common.AuthSupport;
import com.sakuya.backend.common.BusinessException;
import com.sakuya.backend.social.FollowRepository;
import com.sakuya.backend.user.User;
import com.sakuya.backend.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
/**
 * FeedController.java
 * 职责说明：提供推荐与关注动态流、详情、评论、点赞和收藏接口。
 * 执行流程：鉴权用户进入接口 -> 根据 stream 确定动态作者范围 -> 数据库分页读取动态 -> 聚合评论与互动数 -> 返回客户端 PostDto。
 */
@RestController
@RequestMapping("/feed")
public class FeedController {
    private final FeedPostRepository posts;
    private final FeedCommentRepository comments;
    private final FeedEngagementRepository engagements;
    private final UserRepository users;
    private final FollowRepository follows;

    public FeedController(
        FeedPostRepository posts,
        FeedCommentRepository comments,
        FeedEngagementRepository engagements,
        UserRepository users,
        FollowRepository follows
    ) {
        this.posts = posts;
        this.comments = comments;
        this.engagements = engagements;
        this.users = users;
        this.follows = follows;
    }

    /**
     * 分页读取动态流。
     * 执行流程：recommended 直接读取全部动态；following 先从 FollowRepository 收集当前用户已关注作者，
     * 再由 FeedPostRepository 在数据库中按作者集合分页并执行 count 查询，确保 nextPage 基于筛选后的总数。
     */
    @GetMapping
    public ApiResponse<PageDto<PostDto>> list(
        Authentication authentication,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(50) int pageSize,
        @RequestParam(defaultValue = "recommended") String stream
    ) {
        UUID viewerId = AuthSupport.userId(authentication);
        Pageable pageable = PageRequest.of(page, pageSize);
        Page<FeedPost> postPage = "following".equalsIgnoreCase(stream)
            ? followingPosts(viewerId, pageable)
            : posts.findAllByOrderByCreatedAtDesc(pageable);
        return ApiResponse.ok(new PageDto<>(
            postPage.getContent().stream().map(post -> post(post, viewerId)).toList(),
            postPage.hasNext() ? postPage.getNumber() + 1 : null
        ));
    }

    /**
     * 将关注关系转换为关注流的作者范围。
     * 执行流程：查询当前用户发起的 Follow 记录 -> 提取 followingId -> 空集合直接返回零总数分页 -> 否则交给动态仓储查询。
     */
    private Page<FeedPost> followingPosts(UUID viewerId, Pageable pageable) {
        List<UUID> followedAuthorIds = follows.findByFollowerId(viewerId).stream()
            .map(follow -> follow.getFollowingId())
            .toList();
        return followedAuthorIds.isEmpty()
            ? Page.empty(pageable)
            : posts.findByUserIdInOrderByCreatedAtDesc(followedAuthorIds, pageable);
    }
    @GetMapping("/{id}")
    public ApiResponse<PostDto> detail(Authentication a,@PathVariable UUID id){return ApiResponse.ok(post(find(id),AuthSupport.userId(a)));}
    @PostMapping
    @Transactional
    public ApiResponse<PostDto> publish(Authentication a,@Valid@RequestBody PublishRequest r){FeedPost p=posts.save(new FeedPost(AuthSupport.userId(a),r.title(),r.content(),String.join("|",r.topics()==null?List.of():r.topics())));return ApiResponse.ok(post(p,p.getUserId()));}
    @PutMapping("/{id}")
    @Transactional
    public ApiResponse<PostDto> update(Authentication a,@PathVariable UUID id,@Valid@RequestBody PublishRequest r){FeedPost p=find(id);ensureOwner(a,p);p.update(r.title(),r.content(),String.join("|",r.topics()==null?List.of():r.topics()));return ApiResponse.ok(post(p,AuthSupport.userId(a)));}
 @DeleteMapping("/{id}") @Transactional public ApiResponse<Void> delete(Authentication a,@PathVariable UUID id){FeedPost p=find(id);ensureOwner(a,p);posts.delete(p);return ApiResponse.ok();}
 @GetMapping("/{id}/comments") public ApiResponse<PageDto<CommentDto>> comments(Authentication a,@PathVariable UUID id,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int pageSize){AuthSupport.userId(a);find(id);Page<FeedComment>x=comments.findByPostIdOrderByCreatedAtAsc(id,PageRequest.of(page,Math.min(Math.max(pageSize,1),50)));return ApiResponse.ok(new PageDto<>(x.getContent().stream().map(this::comment).toList(),x.hasNext()?page+1:null));}
 @PostMapping("/{id}/comments") @Transactional public ApiResponse<CommentDto> comment(Authentication a,@PathVariable UUID id,@Valid@RequestBody CommentRequest r){find(id);UUID reply=r.replyToCommentId()==null?null:UUID.fromString(r.replyToCommentId());return ApiResponse.ok(comment(comments.save(new FeedComment(id,AuthSupport.userId(a),r.content(),reply))));}
 @PostMapping("/{id}/{kind:like|favorite}") @Transactional public ApiResponse<PostDto> engagement(Authentication a,@PathVariable UUID id,@PathVariable String kind,@RequestBody BooleanRequest r){UUID u=AuthSupport.userId(a);find(id);FeedEngagement.Type t=kind.equals("like")?FeedEngagement.Type.LIKE:FeedEngagement.Type.FAVORITE;if(r.enabled()){if(!engagements.existsByPostIdAndUserIdAndType(id,u,t))engagements.save(new FeedEngagement(id,u,t));}else engagements.deleteByPostIdAndUserIdAndType(id,u,t);return ApiResponse.ok(post(find(id),u));}
 private FeedPost find(UUID id){return posts.findById(id).orElseThrow(()->new BusinessException(404,"动态不存在"));} private void ensureOwner(Authentication a,FeedPost p){if(!p.getUserId().equals(AuthSupport.userId(a)))throw new BusinessException(403,"无权操作该动态");}
 private PostDto post(FeedPost p,UUID viewer){User u=users.findById(p.getUserId()).orElseThrow(()->new BusinessException(404,"作者不存在"));return new PostDto(p.getId().toString(),p.getUserId().toString(),u.getNickname(),initial(u.getNickname()),0L,p.getTitle(),p.getContent(),List.of(),tags(p.getTags()),p.getCreatedAt().toString(),(int)comments.countByPostId(p.getId()),(int)engagements.countByPostIdAndType(p.getId(),FeedEngagement.Type.LIKE),(int)engagements.countByPostIdAndType(p.getId(),FeedEngagement.Type.FAVORITE),engagements.existsByPostIdAndUserIdAndType(p.getId(),viewer,FeedEngagement.Type.LIKE),engagements.existsByPostIdAndUserIdAndType(p.getId(),viewer,FeedEngagement.Type.FAVORITE),p.getUserId().equals(viewer));}
 private CommentDto comment(FeedComment c){User u=users.findById(c.getAuthorId()).orElseThrow(()->new BusinessException(404,"评论用户不存在"));return new CommentDto(c.getId().toString(),c.getPostId().toString(),c.getAuthorId().toString(),u.getNickname(),initial(u.getNickname()),c.getContent(),c.getCreatedAt().toString(),c.getReplyToCommentId()==null?null:c.getReplyToCommentId().toString(),null,0,false);} private String initial(String s){return s==null||s.isBlank()?"?":s.substring(0,1); } /** 兼容早期数据库不带 # 的标签，统一输出 UI 所需的 #标签 格式。 */ private List<String> tags(String s){return s==null||s.isBlank()?List.of():Arrays.stream(s.split("\\|")).filter(x->!x.isBlank()).map(x->x.startsWith("#")?x:"#"+x).toList();}
 public record PageDto<T>(List<T> items,Integer nextPage){} public record PublishRequest(@NotBlank@Size(max=120)String title,@NotBlank String content,List<String> topics,List<Object> attachments,String relatedNovelId){} public record CommentRequest(@NotBlank String content,String replyToCommentId){} public record BooleanRequest(boolean enabled){} public record PostDto(String id,String userId,String authorName,String authorInitial,long authorColor,String title,String content,List<Object> attachments,List<String> tags,String publishedAt,int commentCount,int likeCount,int favoriteCount,boolean isLiked,boolean isFavorited,boolean isMine){} public record CommentDto(String id,String postId,String authorId,String authorName,String authorInitial,String content,String publishedAt,String replyToCommentId,String replyToAuthorName,int likeCount,boolean isLiked){}
}
