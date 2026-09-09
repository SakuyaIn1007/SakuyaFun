package com.sakuya.backend.feed;

import java.util.Collection;
import java.util.UUID;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * FeedPostRepository.java
 * 职责说明：按发布时间为推荐流和指定作者集合的关注流提供数据库分页查询。
 * 执行流程：FeedController 根据 stream 确定查询范围 -> 本仓库执行内容查询及总数统计 -> Page 的 hasNext 用于生成 nextPage。
 */
public interface FeedPostRepository extends JpaRepository<FeedPost, UUID> {
    Page<FeedPost> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** 只查询给定作者集合发布的动态，排序和 count 查询均在数据库中完成。 */
    Page<FeedPost> findByUserIdInOrderByCreatedAtDesc(Collection<UUID> userIds, Pageable pageable);

    /** 作者主页使用独立分页查询，不复用推荐流或关注流的筛选语义。 */
    Page<FeedPost> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /** 统一搜索按标题、正文和标签匹配，排序稳定为最新优先。 */
    @Query("select p from FeedPost p where lower(p.title) like lower(concat('%', :keyword, '%')) or lower(cast(p.content as String)) like lower(concat('%', :keyword, '%')) or lower(cast(p.tags as String)) like lower(concat('%', :keyword, '%')) order by p.createdAt desc")
    Page<FeedPost> search(@Param("keyword") String keyword, Pageable pageable);

    /** 热词聚合只读取最近动态，避免扫描全表。 */
    java.util.List<FeedPost> findTop20ByOrderByCreatedAtDesc();

    /** 统计作者已发布动态数，用于主页数据面板。 */
    long countByUserId(UUID userId);

    long countByUserIdAndCreatedAtAfter(UUID userId, Instant createdAt);
}
