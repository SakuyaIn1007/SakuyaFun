package com.sakuya.backend.social;

import java.util.UUID;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * FollowRepository.java
 * 职责说明：提供关注关系的存在性、数量和按时间倒序分页查询。
 * 执行流程：ProfileSocialController 通过本仓库组装他人主页统计、关系列表和关注状态。
 */
public interface FollowRepository extends JpaRepository<Follow, UUID> {
    boolean existsByFollowerIdAndFollowingId(UUID followerId, UUID followingId);
    long countByFollowerId(UUID followerId);
    long countByFollowingId(UUID followingId);
    Page<Follow> findByFollowerIdOrderByCreatedAtDesc(UUID followerId, Pageable pageable);
    /** 查询当前用户关注的全部关系，供关注动态流收集作者 ID，不自行分页以免影响动态流的总数。 */
    List<Follow> findByFollowerId(UUID followerId);
    Page<Follow> findByFollowingIdOrderByCreatedAtDesc(UUID followingId, Pageable pageable);
    void deleteByFollowerIdAndFollowingId(UUID followerId, UUID followingId);
}
