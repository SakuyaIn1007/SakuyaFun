package com.sakuya.backend.common;

import java.util.UUID;
import org.springframework.security.core.Authentication;

public final class AuthSupport {
    private AuthSupport() {}
    public static UUID userId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(401, "请先登录");
        }
        return UUID.fromString(authentication.getName());
    }
}
