package com.concrete.buildup.global.common;

import lombok.Builder;
import lombok.Getter;

/**
 * API 응답 공통 포맷
 *
 * 모든 API 응답은 이 형식을 따릅니다:
 * {
 *   "success": true/false,
 *   "message": "메시지",
 *   "data": { ... }
 * }
 *
 * 사용 예시:
 * <pre>
 * // 성공 응답 (데이터만)
 * return ResponseEntity.ok(ApiResponse.success(data));
 *
 * // 성공 응답 (데이터 + 메시지)
 * return ResponseEntity.ok(ApiResponse.success(data, "조회에 성공했습니다"));
 *
 * // 201 Created
 * return ResponseEntity
 *     .status(HttpStatus.CREATED)
 *     .body(ApiResponse.success(data, "생성되었습니다"));
 *
 * // 에러 응답 (GlobalExceptionHandler에서 사용)
 * return ResponseEntity
 *     .status(HttpStatus.BAD_REQUEST)
 *     .body(ApiResponse.error("입력값이 올바르지 않습니다"));
 * </pre>
 */
@Getter
@Builder
public class ApiResponse<T> {

    /**
     * 성공 여부
     */
    private final boolean success;

    /**
     * 응답 메시지
     */
    private final String message;

    /**
     * 응답 데이터
     */
    private final T data;

    /**
     * 성공 응답 생성 (데이터만)
     *
     * @param data 응답 데이터
     * @return ApiResponse
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .build();
    }

    /**
     * 성공 응답 생성 (데이터 + 메시지)
     *
     * @param data 응답 데이터
     * @param message 성공 메시지
     * @return ApiResponse
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * 성공 응답 생성 (메시지만, 데이터 없음)
     *
     * @param message 성공 메시지
     * @return ApiResponse
     */
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .build();
    }

    /**
     * 에러 응답 생성 (메시지만)
     * GlobalExceptionHandler에서 주로 사용
     *
     * @param message 에러 메시지
     * @return ApiResponse
     */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .build();
    }

    /**
     * 에러 응답 생성 (메시지 + 데이터)
     * Validation 에러 등에서 필드별 에러 정보를 포함할 때 사용
     *
     * @param message 에러 메시지
     * @param data 에러 상세 정보 (예: 필드별 에러)
     * @return ApiResponse
     */
    public static <T> ApiResponse<T> error(String message, T data) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(data)
                .build();
    }
}
