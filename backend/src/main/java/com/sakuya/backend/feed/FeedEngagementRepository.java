package com.sakuya.backend.feed;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** 负责判断、统计和移除点赞或收藏关系。 */
public interface FeedEngagementRepository extends JpaRepository<FeedEngagement, UUID> {
    boolean existsByPostIdAndUserIdAndType(UUID postId, UUID userId, FeedEngagement.Type type);

    long countByPostIdAndType(UUID postId, FeedEngagement.Type type);

    void deleteByPostIdAndUserIdAndType(UUID postId, UUID userId, FeedEngagement.Type type);
}
