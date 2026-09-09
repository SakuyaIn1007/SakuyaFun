package com.sakuya.backend.feed;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** 关注动态查看游标仓储；targetKey 使用星号表示整体游标，避免数据库对空值唯一约束的差异。 */
interface FeedUpdateViewRepository extends JpaRepository<FeedUpdateView,UUID> {
    Optional<FeedUpdateView> findByUserIdAndScopeAndTargetKey(UUID userId,FeedUpdateView.Scope scope,String targetKey);
    void deleteByUserIdAndScopeAndTargetKey(UUID userId,FeedUpdateView.Scope scope,String targetKey);
}
