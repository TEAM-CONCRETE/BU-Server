package com.concrete.buildup.global.exception.errorcode;

import com.concrete.buildup.global.exception.BaseErrorCode;
import com.concrete.buildup.global.exception.ErrorCategory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 사원 관리 에러 코드 (2000-2999)
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@RequiredArgsConstructor
public enum EmployeeErrorCode implements BaseErrorCode {

    // 400 Bad Request
    INVALID_PAGE_NUMBER(HttpStatus.BAD_REQUEST, 2001, "페이지 번호는 1 이상이어야 합니다."),
    INVALID_PAGE_SIZE(HttpStatus.BAD_REQUEST, 2002, "페이지 크기는 1 이상 100 이하여야 합니다."),

    // 404 Not Found
    EMPLOYEE_NOT_FOUND(HttpStatus.NOT_FOUND, 2003, "사원을 찾을 수 없습니다."),
    EMPLOYEE_NOT_IN_SITE(HttpStatus.NOT_FOUND, 2004, "해당 현장에 소속된 사원이 아닙니다."),

    // 500 Internal Server Error
    DATA_CONVERSION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 2500, "데이터 변환 중 오류가 발생했습니다."),
    RESIDENT_NUM_PROCESSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 2501, "주민등록번호 처리 중 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final int codeNumber;
    private final String message;

    @Override
    public String getCode() {
        return ErrorCategory.EMPLOYEE.generate(codeNumber);
    }
}