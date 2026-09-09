package com.sakuya.backend.feed;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 负责判断、统计和移除点赞或收藏关系。 */
public interface FeedEngagementRepository extends JpaRepository<FeedEngagement, UUID> {
    boolean existsByPostIdAndUserIdAndType(UUID postId, UUID userId, FeedEngagement.Type type);

    long countByPostIdAndType(UUID postId, FeedEngagement.Type type);

    /**
     * 统计指定作者所有动态收到的点赞与收藏总数。
     * 执行流程：先按作者限定动态集合，再由数据库完成聚合，避免主页在应用层逐条累加。
     */
    @Query("select count(e) from FeedEngagement e where e.postId in (select p.id from FeedPost p where p.userId = :authorId)")
    long countReceivedByAuthorId(@Param("authorId") UUID authorId);

    void deleteByPostIdAndUserIdAndType(UUID postId, UUID userId, FeedEngagement.Type type);
}
