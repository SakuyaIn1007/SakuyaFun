package com.sakuya.backend.search;

import com.sakuya.backend.common.ApiResponse;
import com.sakuya.backend.common.AuthSupport;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * SearchController.java
 * 职责说明：暴露统一搜索和数据驱动热词协议。
 * 执行流程：验证分页参数 -> 解析当前用户 -> SearchService 聚合领域结果 -> ApiResponse。
 */
@Validated
@RestController
@RequestMapping("/search")
public class SearchController {
    private final SearchService service;
    public SearchController(SearchService service) { this.service = service; }

    @GetMapping
    public ApiResponse<SearchService.SearchPage> search(
        Authentication authentication,
        @RequestParam String keyword,
        @RequestParam(required = false) String type,
        @RequestParam(defaultValue = "0") @Min(0) @Max(100) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(50) int pageSize
    ) {
        return ApiResponse.ok(service.search(AuthSupport.userId(authentication), keyword, type, page, pageSize));
    }

    @GetMapping("/hot-keywords")
    public ApiResponse<java.util.List<String>> hotKeywords(Authentication authentication) {
        AuthSupport.userId(authentication);
        return ApiResponse.ok(service.hotKeywords());
    }
}
