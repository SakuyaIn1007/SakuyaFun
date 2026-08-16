package com.sakuya.backend.feed;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** 负责按动态和创建时间读取评论，并提供评论数统计。 */
public interface FeedCommentRepository extends JpaRepository<FeedComment, UUID> {
    Page<FeedComment> findByPostIdOrderByCreatedAtAsc(UUID postId, Pageable pageable);

    long countByPostId(UUID postId);
}
