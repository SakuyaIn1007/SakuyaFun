package com.sakuya.backend.feed;

import java.util.Collection;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * FeedPostRepository.java
 * 职责说明：按发布时间为推荐流和指定作者集合的关注流提供数据库分页查询。
 * 执行流程：FeedController 根据 stream 确定查询范围 -> 本仓库执行内容查询及总数统计 -> Page 的 hasNext 用于生成 nextPage。
 */
public interface FeedPostRepository extends JpaRepository<FeedPost, UUID> {
    Page<FeedPost> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** 只查询给定作者集合发布的动态，排序和 count 查询均在数据库中完成。 */
    Page<FeedPost> findByUserIdInOrderByCreatedAtDesc(Collection<UUID> userIds, Pageable pageable);
}
