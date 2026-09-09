package com.sakuya.backend.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByAccountIgnoreCase(String account);
    boolean existsByAccountIgnoreCase(String account);
    List<User> findTop20ByNicknameContainingIgnoreCaseOrAccountContainingIgnoreCase(String nickname, String account);

    /**
     * 统一搜索只返回对当前访问者可见的用户。
     * 执行流程：匹配昵称/账号 -> 过滤隐私主页 -> 按昵称稳定分页。
     */
    @Query("select u from User u where (lower(u.nickname) like lower(concat('%', :keyword, '%')) or lower(u.account) like lower(concat('%', :keyword, '%'))) and (u.showProfileToStrangers = true or u.id = :viewerId) order by u.nickname asc")
    Page<User> searchVisible(@Param("keyword") String keyword, @Param("viewerId") UUID viewerId, Pageable pageable);
}
