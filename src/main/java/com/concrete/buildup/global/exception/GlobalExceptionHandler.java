package com.concrete.buildup.global.exception;

import com.concrete.buildup.domain.attendance.exception.*;
import com.concrete.buildup.global.common.ApiResponse;
import com.concrete.buildup.global.exception.errorcode.CommonErrorCode;
import com.concrete.buildup.global.exception.errorcode.S3ErrorCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

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
                .body(ApiResponse.error(e.getMessage(), e.getErrorCode().getCode()));
    }

    /**
     * FaceNotDetectedException 처리
     * 얼굴이 감지되지 않았을 때 발생하는 예외를 처리합니다.
     * Face API에서 422 Unprocessable Entity 응답 시 발생합니다.
     */
    @ExceptionHandler(FaceNotDetectedException.class)
    public ResponseEntity<ApiResponse<Void>> handleFaceNotDetectedException(FaceNotDetectedException e) {
        log.warn("[FaceNotDetectedException] message={}", e.getMessage());

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error(e.getMessage(), "FACE_001"));
    }

    /**
     * FaceApiException 처리
     * Face Similarity API 호출 중 발생하는 예외를 처리합니다.
     * API 연결 실패, 타임아웃, 서버 오류 등을 포함합니다.
     */
    @ExceptionHandler(FaceApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleFaceApiException(FaceApiException e) {
        log.error("[FaceApiException] message={}", e.getMessage(), e);

        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.error(
                        "얼굴 인식 서비스 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
                        "FACE_002"
                ));
    }

    /**
     * MultipleFacesDetectedException 처리
     * 여러 명의 얼굴이 감지되었을 때 발생하는 예외를 처리합니다.
     */
    @ExceptionHandler(MultipleFacesDetectedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMultipleFacesDetected(MultipleFacesDetectedException e) {
        log.warn("[MultipleFacesDetectedException] message={}", e.getMessage());

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error(e.getMessage(), "FACE_003"));
    }

    /**
     * DuplicateAttendanceException 처리
     * 중복 출퇴근 시도 시 발생하는 예외를 처리합니다.
     */
    @ExceptionHandler(DuplicateAttendanceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateAttendance(DuplicateAttendanceException e) {
        log.warn("[DuplicateAttendanceException] message={}", e.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(e.getMessage(), "ATTENDANCE_001"));
    }

    /**
     * AttendanceNotFoundException 처리
     * 출퇴근 기록을 찾을 수 없을 때 발생하는 예외를 처리합니다.
     */
    @ExceptionHandler(AttendanceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleAttendanceNotFound(AttendanceNotFoundException e) {
        log.error("[AttendanceNotFoundException] message={}", e.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage(), "ATTENDANCE_002"));
    }

    /**
     * FaceImageNotRegisteredException 처리
     * 사원의 얼굴 이미지가 등록되지 않았을 때 발생하는 예외를 처리합니다.
     */
    @ExceptionHandler(FaceImageNotRegisteredException.class)
    public ResponseEntity<ApiResponse<Void>> handleFaceImageNotRegistered(FaceImageNotRegisteredException e) {
        log.error("[FaceImageNotRegisteredException] message={}", e.getMessage());

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error(
                        "얼굴 이미지가 등록되지 않았습니다. 먼저 얼굴을 등록해주세요.",
                        "ATTENDANCE_003"
                ));
    }

    /**
     * MissingServletRequestParameterException 처리
     * 필수 요청 파라미터가 누락된 경우 발생하는 예외를 처리합니다.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameter(
            MissingServletRequestParameterException e) {
        log.error("[MissingServletRequestParameterException] parameterName={}, parameterType={}",
                  e.getParameterName(), e.getParameterType());

        String message = String.format("필수 파라미터가 누락되었습니다: %s (%s)",
                                       e.getParameterName(), e.getParameterType());

        return ResponseEntity
                .status(CommonErrorCode.MISSING_REQUEST_PARAMETER.getHttpStatus())
                .body(ApiResponse.error(message, CommonErrorCode.MISSING_REQUEST_PARAMETER.getCode()));
    }

    /**
     * HttpMessageNotReadableException 처리
     * JSON 파싱 오류 등 요청 본문을 읽을 수 없는 경우 발생하는 예외를 처리합니다.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(
            HttpMessageNotReadableException e) {
        log.error("[HttpMessageNotReadableException] message={}", e.getMessage());

        String message = "요청 본문을 읽을 수 없습니다. JSON 형식을 확인해주세요.";

        return ResponseEntity
                .status(CommonErrorCode.INVALID_INPUT_VALUE.getHttpStatus())
                .body(ApiResponse.error(message, CommonErrorCode.INVALID_INPUT_VALUE.getCode()));
    }

    /**
     * MethodArgumentTypeMismatchException 처리
     * 요청 파라미터의 타입이 일치하지 않는 경우 발생하는 예외를 처리합니다.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException e) {
        log.error("[MethodArgumentTypeMismatchException] parameterName={}, requiredType={}",
                  e.getName(), e.getRequiredType());

        String message = String.format("파라미터 타입이 올바르지 않습니다: %s (필요한 타입: %s)",
                                       e.getName(), e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "unknown");

        return ResponseEntity
                .status(CommonErrorCode.INVALID_TYPE_VALUE.getHttpStatus())
                .body(ApiResponse.error(message, CommonErrorCode.INVALID_TYPE_VALUE.getCode()));
    }

    /**
     * HttpRequestMethodNotSupportedException 처리
     * 지원하지 않는 HTTP 메서드로 요청한 경우 발생하는 예외를 처리합니다.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException e) {
        log.error("[HttpRequestMethodNotSupportedException] method={}, supportedMethods={}",
                  e.getMethod(), e.getSupportedHttpMethods());

        String message = String.format("지원하지 않는 HTTP 메서드입니다: %s (지원하는 메서드: %s)",
                                       e.getMethod(), e.getSupportedHttpMethods());

        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error(message, "COMMON_405"));
    }

    /**
     * NoResourceFoundException 처리
     * 요청한 리소스(URL)를 찾을 수 없는 경우 발생하는 예외를 처리합니다.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(NoResourceFoundException e) {
        log.error("[NoResourceFoundException] resourcePath={}", e.getResourcePath());

        return ResponseEntity
                .status(CommonErrorCode.RESOURCE_NOT_FOUND.getHttpStatus())
                .body(ApiResponse.error(
                        CommonErrorCode.RESOURCE_NOT_FOUND.getMessage(),
                        CommonErrorCode.RESOURCE_NOT_FOUND.getCode()));
    }

    /**
     * ConstraintViolationException 처리
     * @Validated 어노테이션의 제약 조건 위반 시 발생하는 예외를 처리합니다.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleConstraintViolation(
            ConstraintViolationException e) {
        Map<String, String> errors = e.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage,
                        (existing, replacement) -> existing + ", " + replacement  // 중복 키 병합: 메시지 연결
                ));

        log.error("[ConstraintViolationException] errors={}", errors);

        return ResponseEntity
                .status(CommonErrorCode.FIELD_VALIDATION_ERROR.getHttpStatus())
                .body(ApiResponse.error(
                        CommonErrorCode.FIELD_VALIDATION_ERROR.getMessage(),
                        CommonErrorCode.FIELD_VALIDATION_ERROR.getCode(),
                        errors));
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
                .body(ApiResponse.error("접근 권한이 없습니다.", "COMMON_403"));
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
                .status(CommonErrorCode.FIELD_VALIDATION_ERROR.getHttpStatus())
                .body(ApiResponse.error(
                        CommonErrorCode.FIELD_VALIDATION_ERROR.getMessage(),
                        CommonErrorCode.FIELD_VALIDATION_ERROR.getCode(),
                        errors));
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
     * SecretKeyGenerationException 처리
     * 시크릿키 생성 실패 시 발생하는 예외를 처리합니다.
     */
    @ExceptionHandler(SecretKeyGenerationException.class)
    public ResponseEntity<ApiResponse<Void>> handleSecretKeyGenerationException(SecretKeyGenerationException e) {
        log.error("[SecretKeyGenerationException] Secret key generation failed: {}", e.getMessage(), e);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                        "시스템 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
                        "COMMON_500"));
    }

    /**
     * IllegalStateException 처리
     * 시스템 상태 오류 시 발생하는 예외를 처리합니다.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalStateException(IllegalStateException e) {
        log.error("[IllegalStateException] System state error: {}", e.getMessage(), e);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                        "시스템 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
                        "COMMON_500"));
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