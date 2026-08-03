package com.sakuya.backend.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByAccountIgnoreCase(String account);
    boolean existsByAccountIgnoreCase(String account);
    List<User> findTop20ByNicknameContainingIgnoreCaseOrAccountContainingIgnoreCase(String nickname, String account);
}
