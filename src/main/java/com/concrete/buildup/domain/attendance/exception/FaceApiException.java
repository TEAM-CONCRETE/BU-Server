package com.concrete.buildup.domain.attendance.exception;

/**
 * Face API 호출 실패 예외
 * Face Similarity API 서버와의 통신 중 발생한 오류를 나타냅니다.
 */
public class FaceApiException extends RuntimeException {

    public FaceApiException(String message) {
        super(message);
    }

    public FaceApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
