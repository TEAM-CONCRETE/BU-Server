package com.concrete.buildup.global.exception;

/**
 * 리소스를 찾을 수 없을 때 발생하는 예외 (HTTP 404)
 *
 * 사용 시점:
 * - 데이터베이스 조회 결과 없음
 * - 존재하지 않는 리소스 접근
 * - findById().orElseThrow() 사용 시
 *
 * 사용 예시:
 * <pre>
 * User user = userRepository.findById(id)
 *         .orElseThrow(() -> new ResourceNotFoundException(UserErrorCode.USER_NOT_FOUND));
 * </pre>
 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(BaseErrorCode errorCode) {
        super(errorCode);
    }

    public ResourceNotFoundException(BaseErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }
}