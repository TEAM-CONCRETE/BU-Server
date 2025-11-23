package com.concrete.buildup.global.exception.errorcode;

import com.concrete.buildup.global.exception.BaseErrorCode;
import com.concrete.buildup.global.exception.ErrorCategory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 안전교육일지 도메인 에러 코드 (8000-8999)
 */
@Getter
@RequiredArgsConstructor
public enum SafetyDocErrorCode implements BaseErrorCode {

    // 404 Not Found
    SAFETY_EDUCATION_LOG_NOT_FOUND(HttpStatus.NOT_FOUND, 8001, "안전교육일지를 찾을 수 없습니다."),
    ATTENDEE_NOT_FOUND(HttpStatus.NOT_FOUND, 8002, "교육 대상자를 찾을 수 없습니다."),
    SITE_NOT_FOUND(HttpStatus.NOT_FOUND, 8003, "현장을 찾을 수 없습니다."),
    EMPLOYEE_NOT_FOUND(HttpStatus.NOT_FOUND, 8004, "근로자를 찾을 수 없습니다."),

    // 400 Bad Request
    INVALID_EDUCATION_DATA(HttpStatus.BAD_REQUEST, 8010, "교육 정보가 올바르지 않습니다."),
    INVALID_SAFETY_EDUCATION_STATUS(HttpStatus.BAD_REQUEST, 8011, "현재 상태에서는 해당 작업을 수행할 수 없습니다."),
    SIGNATURE_HASH_MISMATCH(HttpStatus.BAD_REQUEST, 8012, "서명 이미지 해시값이 일치하지 않습니다."),
    FINAL_PDF_ALREADY_SET(HttpStatus.BAD_REQUEST, 8013, "이미 최종 PDF가 설정되어 있습니다."),
    EMPTY_ATTENDEE_LIST(HttpStatus.BAD_REQUEST, 8014, "교육 대상자 목록이 비어있습니다."),
    ALREADY_SIGNED(HttpStatus.BAD_REQUEST, 8015, "이미 서명이 완료되었습니다."),
    NOT_ALL_ATTENDEES_SIGNED(HttpStatus.BAD_REQUEST, 8016, "모든 대상자가 서명하지 않았습니다."),

    // 403 Forbidden
    MANAGER_NOT_AUTHORIZED(HttpStatus.FORBIDDEN, 8020, "해당 현장에 대한 권한이 없습니다."),
    EMPLOYEE_NOT_AUTHORIZED(HttpStatus.FORBIDDEN, 8021, "해당 교육의 참석자가 아닙니다."),

    // 500 Internal Server Error
    PDF_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 8030, "PDF 생성 중 오류가 발생했습니다."),
    S3_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 8031, "S3 업로드 중 오류가 발생했습니다."),
    SIGNATURE_PROCESSING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 8032, "서명 처리 중 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final int codeNumber;
    private final String message;

    @Override
    public String getCode() {
        return ErrorCategory.SAFETYDOC.generate(codeNumber);
    }
}
