package com.sakuya.backend.common;

public record ApiResponse<T>(int code, String message, T data) {
    public static <T> ApiResponse<T> ok(T data) { return new ApiResponse<>(200, "success", data); }
    public static ApiResponse<Void> ok() { return new ApiResponse<>(200, "success", null); }
    public static <T> ApiResponse<T> error(int code, String message) { return new ApiResponse<>(code, message, null); }
}
