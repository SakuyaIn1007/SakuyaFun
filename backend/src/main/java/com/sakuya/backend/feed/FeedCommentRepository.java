package com.sakuya.backend.feed;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** 负责按动态和创建时间读取评论，并提供评论数统计。 */
public interface FeedCommentRepository extends JpaRepository<FeedComment, UUID> {
    Page<FeedComment> findByPostIdOrderByCreatedAtAsc(UUID postId, Pageable pageable);

    long countByPostId(UUID postId);

    /**
     * 演示种子数据专用的自然键判断，使重复启动不会重复插入评论。
     * 注意：这是弱自然键，同一作者对同一条动态发表内容完全相同的两条真实评论会被误判为已存在，
     * 因此只能用于种子导入，不得用于业务去重。
     */
    boolean existsByPostIdAndAuthorIdAndContent(UUID postId, UUID authorId, String content);
}
