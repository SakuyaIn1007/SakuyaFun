package com.sakuya.backend.reading;

import com.sakuya.backend.common.ApiResponse;
import com.sakuya.backend.common.AuthSupport;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ReadingSyncController.java
 * 职责说明：提供登录用户唯一的阅读同步 HTTP 边界，不接受客户端传入 userId。
 * 执行流程：Spring Security 校验 JWT -> AuthSupport 提取用户 -> Service 合并变更并返回完整快照。
 */
@RestController
@RequestMapping("/reading")
public class ReadingSyncController {
    private final ReadingSyncService service;
    public ReadingSyncController(ReadingSyncService service) { this.service = service; }

    @PostMapping("/sync")
    public ApiResponse<ReadingSyncResponse> sync(Authentication authentication, @Valid @RequestBody ReadingSyncRequest request) {
        return ApiResponse.ok(service.sync(AuthSupport.userId(authentication), request));
    }
}
