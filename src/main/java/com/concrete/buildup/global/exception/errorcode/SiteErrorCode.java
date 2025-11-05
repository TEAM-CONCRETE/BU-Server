package com.concrete.buildup.global.exception.errorcode;

import com.concrete.buildup.global.exception.BaseErrorCode;
import com.concrete.buildup.global.exception.ErrorCategory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 현장 관리 에러 코드 (3000-3999)
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@RequiredArgsConstructor
public enum SiteErrorCode implements BaseErrorCode {

    // 422 Unprocessable Entity
    SECRET_KEY_NOT_FOUND_OR_EXPIRED(HttpStatus.UNPROCESSABLE_ENTITY, 3001, "시크릿키를 찾을 수 없거나 만료되었습니다."),

    // 409 Conflict
    SECRET_KEY_ALREADY_USED(HttpStatus.CONFLICT, 3002, "이미 사용 중인 시크릿키입니다."),

    // 404 Not Found
    SITE_NOT_FOUND(HttpStatus.NOT_FOUND, 3003, "현장을 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final int codeNumber;
    private final String message;

    @Override
    public String getCode() {
        return ErrorCategory.SITE.generate(codeNumber);
    }
}
