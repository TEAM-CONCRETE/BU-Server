package com.concrete.buildup.global.exception;

import com.concrete.buildup.global.common.ApiResponse;
import com.concrete.buildup.global.exception.errorcode.S3ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.HashMap;
import java.util.Map;

/**
 * 전역 예외 처리 핸들러
 *
 * 모든 예외를 일관된 형식(ApiResponse)으로 처리하여 클라이언트에 반환합니다.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * BusinessException 처리
     * 비즈니스 로직에서 발생하는 예외를 처리합니다.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        log.error("[BusinessException] code={}, message={}",
                  e.getErrorCode().getCode(), e.getMessage());

        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(ApiResponse.error(e.getMessage()));
    }

    /**
     * AccessDeniedException 처리
     * 인가(권한) 실패 시 발생하는 예외를 처리합니다.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException e) {
        log.error("[AccessDeniedException] Access denied: {}", e.getMessage());

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("접근 권한이 없습니다."));
    }

    /**
     * Validation 예외 처리
     * @Valid, @Validated 어노테이션 사용 시 발생하는 예외를 처리합니다.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException e) {

        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.error("[ValidationException] errors={}", errors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("필드 검증에 실패했습니다.", errors));
    }

    /**
     * S3 NoSuchKey 예외 처리
     * S3에서 파일을 찾을 수 없을 때 발생하는 예외를 처리합니다.
     */
    @ExceptionHandler(NoSuchKeyException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoSuchKeyException(NoSuchKeyException e) {
        log.error("[S3Exception] File not found in S3: {}", e.getMessage());

        return ResponseEntity
                .status(S3ErrorCode.FILE_NOT_FOUND.getHttpStatus())
                .body(ApiResponse.error(S3ErrorCode.FILE_NOT_FOUND.getMessage()));
    }

    /**
     * S3 예외 처리
     * S3 작업 중 발생하는 예외를 처리합니다.
     */
    @ExceptionHandler(S3Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleS3Exception(S3Exception e) {
        log.error("[S3Exception] S3 operation failed: statusCode={}, message={}",
                  e.statusCode(), e.awsErrorDetails().errorMessage());

        String errorMessage = switch (e.statusCode()) {
            case 403 -> "S3 접근 권한이 없습니다.";
            case 404 -> S3ErrorCode.FILE_NOT_FOUND.getMessage();
            default -> "S3 작업 중 오류가 발생했습니다.";
        };

        return ResponseEntity
                .status(HttpStatus.valueOf(e.statusCode()))
                .body(ApiResponse.error(errorMessage));
    }

    /**
     * AWS SDK 예외 처리
     * AWS SDK 레벨에서 발생하는 예외를 처리합니다 (연결 실패 등).
     */
    @ExceptionHandler(SdkException.class)
    public ResponseEntity<ApiResponse<Void>> handleSdkException(SdkException e) {
        log.error("[SdkException] AWS SDK error occurred: {}", e.getMessage(), e);

        return ResponseEntity
                .status(S3ErrorCode.S3_CONNECTION_FAILED.getHttpStatus())
                .body(ApiResponse.error(S3ErrorCode.S3_CONNECTION_FAILED.getMessage()));
    }

    /**
     * 예상치 못한 예외 처리
     * 모든 예외의 최종 처리자입니다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception e) {
        log.error("[UnexpectedException] Unexpected exception occurred", e);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("서버 내부 오류가 발생했습니다."));
    }
}