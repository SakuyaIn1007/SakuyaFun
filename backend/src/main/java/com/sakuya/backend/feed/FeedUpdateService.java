package com.sakuya.backend.feed;

import com.sakuya.backend.common.BusinessException;
import com.sakuya.backend.social.Follow;
import com.sakuya.backend.social.FollowRepository;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FeedUpdateService.java
 * 职责说明：计算关注用户新动态并维护整体、单作者两级查看游标。
 * 执行流程：读取关注关系 -> 取整体/作者/关注建立时间中的最新值 -> 统计该时间后的动态；已读操作幂等推进游标。
 * 兼容策略：旧账号首次访问时以当前时间建立整体基线，避免升级后把全部历史动态误判为新内容。
 */
@Service
public class FeedUpdateService {
    private static final String GLOBAL_KEY="*";
    private final FeedUpdateViewRepository views; private final FeedPostRepository posts; private final FollowRepository follows;
    public FeedUpdateService(FeedUpdateViewRepository views,FeedPostRepository posts,FollowRepository follows){this.views=views;this.posts=posts;this.follows=follows;}

    @Transactional
    public UnseenSummary summary(UUID userId){
        Instant global=initializeIfMissing(userId);
        long postCount=0;long authorCount=0;
        for(Follow follow:follows.findByFollowerId(userId)){
            long count=posts.countByUserIdAndCreatedAtAfter(follow.getFollowingId(),effectiveCursor(userId,follow,global));
            postCount+=count;if(count>0)authorCount++;
        }
        return new UnseenSummary(postCount,authorCount);
    }

    @Transactional(readOnly=true)
    public boolean hasUnseenPosts(UUID userId,Follow follow){
        Optional<FeedUpdateView> global=views.findByUserIdAndScopeAndTargetKey(userId,FeedUpdateView.Scope.FOLLOWING_FEED,GLOBAL_KEY);
        if(global.isEmpty())return false;
        return posts.countByUserIdAndCreatedAtAfter(follow.getFollowingId(),effectiveCursor(userId,follow,global.get().getReadAt()))>0;
    }

    /** 新建关注前确保整体基线存在，使之后发布的动态能够被准确识别。 */
    @Transactional public void initialize(UUID userId){initializeIfMissing(userId);}
    @Transactional public void markAllRead(UUID userId){upsert(userId,FeedUpdateView.Scope.FOLLOWING_FEED,null,Instant.now());}
    @Transactional public void markAuthorRead(UUID userId,UUID authorId){
        if(!follows.existsByFollowerIdAndFollowingId(userId,authorId))throw new BusinessException(404,"关注关系不存在");
        upsert(userId,FeedUpdateView.Scope.FOLLOWING_AUTHOR,authorId,Instant.now());
    }
    @Transactional public void removeAuthor(UUID userId,UUID authorId){views.deleteByUserIdAndScopeAndTargetKey(userId,FeedUpdateView.Scope.FOLLOWING_AUTHOR,authorId.toString());}

    private Instant initializeIfMissing(UUID userId){
        return views.findByUserIdAndScopeAndTargetKey(userId,FeedUpdateView.Scope.FOLLOWING_FEED,GLOBAL_KEY).map(FeedUpdateView::getReadAt)
            .orElseGet(()->{Instant now=Instant.now();views.save(new FeedUpdateView(userId,FeedUpdateView.Scope.FOLLOWING_FEED,null,now));return now;});
    }
    private Instant effectiveCursor(UUID userId,Follow follow,Instant global){
        Instant author=views.findByUserIdAndScopeAndTargetKey(userId,FeedUpdateView.Scope.FOLLOWING_AUTHOR,follow.getFollowingId().toString()).map(FeedUpdateView::getReadAt).orElse(Instant.EPOCH);
        return Collections.max(List.of(global,author,follow.getCreatedAt()));
    }
    private void upsert(UUID userId,FeedUpdateView.Scope scope,UUID target,Instant now){
        String key=target==null?GLOBAL_KEY:target.toString();FeedUpdateView value=views.findByUserIdAndScopeAndTargetKey(userId,scope,key).orElseGet(()->new FeedUpdateView(userId,scope,target,now));value.markRead(now);views.save(value);
    }
    public record UnseenSummary(long postCount,long authorCount){}
}
