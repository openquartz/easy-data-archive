package com.openquartz.easyarchive.starter.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * API响应包装类
 */
@Data
public class ApiResponse<T> implements Serializable {

    private String code;
    private String message;
    private String requestId;
    private T data;

    public ApiResponse() {
    }

    public ApiResponse(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>("SUCCESS", "成功", null);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("SUCCESS", "成功", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>("SUCCESS", message, data);
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>("SYSTEM_ERROR", message, null);
    }
}