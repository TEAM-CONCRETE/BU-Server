package com.concrete.buildup.global.exception.errorcode;

import com.concrete.buildup.global.exception.BaseErrorCode;
import com.concrete.buildup.global.exception.ErrorCategory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 급여 관리 에러 코드 (5000-5999)
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@RequiredArgsConstructor
public enum PayrollErrorCode implements BaseErrorCode {

    // 404 Not Found
    PAYROLL_NOT_FOUND(HttpStatus.NOT_FOUND, 5001, "급여를 찾을 수 없거나 접근 권한이 없습니다."),

    // 403 Forbidden
    PAYROLL_ACCESS_DENIED(HttpStatus.FORBIDDEN, 5002, "해당 급여에 접근할 권한이 없습니다.");

    private final HttpStatus httpStatus;
    private final int codeNumber;
    private final String message;

    @Override
    public String getCode() {
        return ErrorCategory.PAYROLL.generate(codeNumber);
    }
}

