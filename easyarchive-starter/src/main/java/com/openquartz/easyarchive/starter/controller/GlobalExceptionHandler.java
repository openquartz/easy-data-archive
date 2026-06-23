package com.openquartz.easyarchive.starter.controller;

import com.openquartz.easyarchive.starter.exception.StarterErrorCode;
import com.openquartz.easyarchive.starter.exception.StarterManageException;
import com.openquartz.easyarchive.starter.model.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Global exception handler that returns proper HTTP status codes
 * and clean error responses instead of 500 for all errors.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Pre-compute the prefixed error codes for fast matching
    private static final Set<String> NOT_FOUND_CODES = Set.of(
            StarterErrorCode.ARCHIVE_GROUP_NOT_FOUND.getErrorCode(),
            StarterErrorCode.TASK_NOT_FOUND.getErrorCode(),
            StarterErrorCode.USER_NOT_FOUND.getErrorCode(),
            StarterErrorCode.DATASOURCE_NOT_FOUND.getErrorCode(),
            StarterErrorCode.ARCHIVE_GROUP_ITEM_NOT_FOUND.getErrorCode()
    );

    private static final Set<String> VALIDATION_CODES = Set.of(
            StarterErrorCode.OWNER_USER_DISABLED.getErrorCode(),
            StarterErrorCode.OWNER_USER_INVALID.getErrorCode(),
            StarterErrorCode.OWNER_USER_NOT_CREATED_BY_YOU.getErrorCode(),
            StarterErrorCode.OWNER_UPDATE_NOT_ALLOWED.getErrorCode(),
            StarterErrorCode.USER_ROLE_INVALID.getErrorCode(),
            StarterErrorCode.USER_ROLE_INVALID_FOR_CREATOR.getErrorCode()
    );

    private static final Set<String> FORBIDDEN_CODES = Set.of(
            StarterErrorCode.DATASOURCE_ACCESS_DENIED.getErrorCode(),
            StarterErrorCode.ADMIN_PERMISSION_REQUIRED.getErrorCode()
    );

    // ------------------------- handlers -------------------------

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("AUTH_INVALID", "用户名或密码错误"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("VALIDATION_FAILED",
                        message.isEmpty() ? "参数校验失败" : message));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Object>> handleBindException(BindException ex) {
        String message = ex.getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("VALIDATION_FAILED",
                        message.isEmpty() ? "参数绑定失败" : message));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Object>> handleMissingParam(MissingServletRequestParameterException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("VALIDATION_FAILED",
                        "缺少必填参数: " + ex.getParameterName()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("VALIDATION_FAILED",
                        "参数类型错误: " + ex.getName()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("VALIDATION_FAILED", "请求体格式不正确"));
    }

    @ExceptionHandler(StarterManageException.class)
    public ResponseEntity<ApiResponse<Object>> handleStarterManage(StarterManageException ex) {
        String errorCodeStr = ex.getErrorCode() != null ? ex.getErrorCode().getErrorCode() : null;
        if (errorCodeStr == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("BUSINESS_ERROR",
                            ex.getMessage() != null ? ex.getMessage() : "业务处理失败"));
        }

        String message = ex.getMessage() != null ? ex.getMessage() : ex.getErrorCode().getErrorMsg();

        if (NOT_FOUND_CODES.contains(errorCodeStr)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("RESOURCE_NOT_FOUND", message));
        }

        if (VALIDATION_CODES.contains(errorCodeStr)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("VALIDATION_FAILED", message));
        }

        if (StarterErrorCode.ARCHIVE_TASK_TERMINAL_CANNOT_CANCEL.getErrorCode().equals(errorCodeStr)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("INVALID_STATUS", message));
        }

        if (FORBIDDEN_CODES.contains(errorCodeStr)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("ACCESS_DENIED", message));
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(errorCodeStr, message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("VALIDATION_FAILED",
                        ex.getMessage() != null ? ex.getMessage() : "参数不合法"));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("INVALID_STATUS",
                        ex.getMessage() != null ? ex.getMessage() : "状态不合法"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("SYSTEM_ERROR", "服务器内部错误，请稍后重试"));
    }
}
