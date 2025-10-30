package com.concrete.buildup.global.exception.errorcode;

import com.concrete.buildup.global.exception.BaseErrorCode;
import com.concrete.buildup.global.exception.ErrorCategory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 공통 에러 코드 (0-999)
 */
@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements BaseErrorCode {

    // 400 Bad Request
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, 400, "입력값이 올바르지 않습니다."),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, 401, "입력 타입이 올바르지 않습니다."),
    FIELD_VALIDATION_ERROR(HttpStatus.BAD_REQUEST, 402, "필드 검증에 실패했습니다."),
    MISSING_REQUEST_PARAMETER(HttpStatus.BAD_REQUEST, 403, "필수 파라미터가 누락되었습니다."),

    // 404 Not Found
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "요청한 리소스를 찾을 수 없습니다."),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 500, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final int codeNumber;
    private final String message;

    @Override
    public String getCode() {
        return ErrorCategory.COMMON.generate(codeNumber);
    }
}