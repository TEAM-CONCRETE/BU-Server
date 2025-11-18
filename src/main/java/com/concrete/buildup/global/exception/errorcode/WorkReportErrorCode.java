package com.concrete.buildup.global.exception.errorcode;

import com.concrete.buildup.global.exception.BaseErrorCode;
import com.concrete.buildup.global.exception.ErrorCategory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 작업일보 에러 코드 (7000-7999)
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@RequiredArgsConstructor
public enum WorkReportErrorCode implements BaseErrorCode {

    // 400 Bad Request
    DUPLICATE_WORK_REPORT(HttpStatus.BAD_REQUEST, 7001, "해당 날짜에 이미 작업일보가 작성되었습니다."),
    INVALID_WORK_DATE(HttpStatus.BAD_REQUEST, 7002, "유효하지 않은 작업일자입니다."),

    // 403 Forbidden
    MANAGER_NOT_AUTHORIZED(HttpStatus.FORBIDDEN, 7003, "해당 현장에 대한 권한이 없습니다."),

    // 404 Not Found
    WORK_REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, 7004, "작업일보를 찾을 수 없습니다."),
    SITE_NOT_FOUND(HttpStatus.NOT_FOUND, 7005, "현장을 찾을 수 없습니다."),

    // 500 Internal Server Error
    PDF_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 7006, "PDF 생성 중 오류가 발생했습니다."),
    S3_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 7007, "S3 업로드 중 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final int codeNumber;
    private final String message;

    @Override
    public String getCode() {
        return ErrorCategory.WORKREPORT.generate(codeNumber);
    }
}
