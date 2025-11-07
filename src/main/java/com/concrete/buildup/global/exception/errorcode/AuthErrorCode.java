package com.concrete.buildup.global.exception.errorcode;

import com.concrete.buildup.global.exception.BaseErrorCode;
import com.concrete.buildup.global.exception.ErrorCategory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 인증/인가 에러 코드 (1000-1999)
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements BaseErrorCode {

    // 400 Bad Request
    DUPLICATE_USER_ID(HttpStatus.BAD_REQUEST, 1001, "이미 사용 중인 아이디입니다."),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, 1002, "비밀번호가 일치하지 않습니다."),

    // 404 Not Found
    ROLE_NOT_FOUND(HttpStatus.NOT_FOUND, 1003, "역할을 찾을 수 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, 1004, "사용자를 찾을 수 없습니다."),
    REGISTRATION_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, 1009, "등록 토큰을 찾을 수 없습니다."),

    // 401 Unauthorized
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, 1005, "아이디 또는 비밀번호가 올바르지 않습니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, 1006, "만료된 토큰입니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, 1007, "유효하지 않은 토큰입니다."),
    REGISTRATION_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, 1010, "등록 토큰이 만료되었습니다. 1단계부터 다시 진행해주세요."),

    // 403 Forbidden
    ACCESS_DENIED(HttpStatus.FORBIDDEN, 1008, "접근 권한이 없습니다.");

    private final HttpStatus httpStatus;
    private final int codeNumber;
    private final String message;

    @Override
    public String getCode() {
        return ErrorCategory.AUTH.generate(codeNumber);
    }
}
