package com.concrete.buildup.global.exception.errorcode;

import com.concrete.buildup.global.exception.BaseErrorCode;
import com.concrete.buildup.global.exception.ErrorCategory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 문서 관련 에러 코드 (9000-9999)
 */
@Getter
@RequiredArgsConstructor
public enum DocumentErrorCode implements BaseErrorCode {

    // 404 Not Found
    CONTRACT_NOT_FOUND(HttpStatus.NOT_FOUND, 9001, "근로계약서를 찾을 수 없습니다."),
    PAYROLL_NOT_FOUND(HttpStatus.NOT_FOUND, 9002, "급여명세서를 찾을 수 없습니다."),
    DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, 9003, "문서 파일을 찾을 수 없습니다."),
    SAFETY_EDUCATION_LOG_NOT_FOUND(HttpStatus.NOT_FOUND, 9004, "안전교육일지를 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final int codeNumber;
    private final String message;

    @Override
    public String getCode() {
        return ErrorCategory.DOCUMENT.generate(codeNumber);
    }
}