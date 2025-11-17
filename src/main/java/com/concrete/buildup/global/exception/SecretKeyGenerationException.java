package com.concrete.buildup.global.exception;

/**
 * 시크릿키 생성 실패 예외
 *
 * <p>현장 등록 시 유니크한 시크릿키 생성에 실패했을 때 발생하는 예외입니다.</p>
 * <p>주로 최대 재시도 횟수를 초과했을 때 발생합니다.</p>
 *
 * <p>사용 예시:</p>
 * <pre>
 * if (attempt >= MAX_RETRY) {
 *     throw new SecretKeyGenerationException("유니크한 시크릿키 생성에 실패했습니다.");
 * }
 * </pre>
 *
 * @author Build-Up Team
 * @since 1.0
 */
public class SecretKeyGenerationException extends RuntimeException {

    public SecretKeyGenerationException(String message) {
        super(message);
    }

    public SecretKeyGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
