package com.concrete.buildup.global.exception.errorcode;

import com.concrete.buildup.global.exception.BaseErrorCode;
import com.concrete.buildup.global.exception.ErrorCategory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 계약 관리 에러 코드 (4000-4999)
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@RequiredArgsConstructor
public enum ContractErrorCode implements BaseErrorCode {

    // 404 Not Found
    CONTRACT_NOT_FOUND(HttpStatus.NOT_FOUND, 4001, "계약을 찾을 수 없습니다."),
    EMPLOYEE_NOT_FOUND(HttpStatus.NOT_FOUND, 4002, "근로자를 찾을 수 없습니다."),
    CORPORATION_NOT_FOUND(HttpStatus.NOT_FOUND, 4003, "기업을 찾을 수 없습니다."),
    MANAGER_NOT_FOUND(HttpStatus.NOT_FOUND, 4004, "관리자를 찾을 수 없습니다."),
    SITE_NOT_FOUND(HttpStatus.NOT_FOUND, 4006, "현장을 찾을 수 없습니다."),
    CONTRACT_DETAIL_NOT_FOUND(HttpStatus.NOT_FOUND, 4012, "계약 상세 정보를 찾을 수 없습니다."),

    // 400 Bad Request
    INVALID_PAY_DAY(HttpStatus.BAD_REQUEST, 4008, "급여 지급일은 1일에서 31일 사이여야 합니다."),
    INVALID_EMP_TYPE_FOR_ENDPOINT(HttpStatus.BAD_REQUEST, 4009, "엔드포인트와 근로자 유형이 일치하지 않습니다."),
    INVALID_CONTRACT_STATE(HttpStatus.BAD_REQUEST, 4010, "현재 계약 상태에서는 해당 작업을 수행할 수 없습니다."),
    SIGNATURE_HASH_MISMATCH(HttpStatus.BAD_REQUEST, 4011, "서명 이미지 해시값이 일치하지 않습니다."),
    FINAL_PDF_ALREADY_SET(HttpStatus.BAD_REQUEST, 4013, "이미 최종 PDF가 설정되어 있습니다. 완결된 계약서는 변경할 수 없습니다."),

    // 403 Forbidden
    MANAGER_NOT_AUTHORIZED(HttpStatus.FORBIDDEN, 4007, "해당 현장의 관리자가 아닙니다."),

    // 422 Unprocessable Entity
    CONFLICTING_EMP_TYPE(HttpStatus.UNPROCESSABLE_ENTITY, 4005, "이미 다른 유형의 계약이 존재합니다. 기존 계약을 종료한 후 진행해주세요.");

    private final HttpStatus httpStatus;
    private final int codeNumber;
    private final String message;

    @Override
    public String getCode() {
        return ErrorCategory.CONTRACT.generate(codeNumber);
    }
}