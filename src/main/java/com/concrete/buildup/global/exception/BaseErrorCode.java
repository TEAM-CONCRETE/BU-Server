package com.concrete.buildup.global.exception;

import org.springframework.http.HttpStatus;

/**
 * 에러 코드 기본 인터페이스
 * 모든 ErrorCode enum은 이 인터페이스를 구현해야 합니다.
 */
public interface BaseErrorCode {

    /**
     * HTTP 상태 코드 반환
     */
    HttpStatus getHttpStatus();

    /**
     * 에러 코드 반환 (예: "AUTH_1001")
     */
    String getCode();

    /**
     * 에러 메시지 반환
     */
    String getMessage();
}