package com.concrete.buildup.global.exception;

import lombok.Getter;

/**
 * 비즈니스 로직 예외의 기본 클래스
 *
 * 사용 시점:
 * - 비즈니스 규칙 위반
 * - 사용자 입력 오류
 * - 예측 가능한 예외 상황
 *
 * 사용 예시:
 * <pre>
 * if (userRepository.existsByUsername(username)) {
 *     throw new BusinessException(UserErrorCode.DUPLICATE_USERNAME);
 * }
 * </pre>
 */
@Getter
public class BusinessException extends RuntimeException {

    private final BaseErrorCode errorCode;

    public BusinessException(BaseErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(BaseErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }

    public BusinessException(BaseErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
}