package com.concrete.buildup.global.exception;

import com.concrete.buildup.global.exception.errorcode.CommonErrorCode;

/**
 * 권한이 없을 때 발생하는 예외 (HTTP 403)
 *
 * 사용 시점:
 * - 인증은 되었지만 권한 부족
 * - 리소스 소유권 확인 실패
 * - 역할 기반 접근 제어 실패
 *
 * 사용 예시:
 * <pre>
 * // 리소스 소유권 확인
 * if (!post.getOwnerId().equals(userId)) {
 *     throw new AuthorizationException(userId, "Post", "delete");
 * }
 *
 * // 현장 접근 권한 확인 (Build-Up Platform 특화)
 * if (!siteService.hasAccessToSite(userId, siteId)) {
 *     throw new AuthorizationException(userId, siteId);
 * }
 * </pre>
 */
public class AuthorizationException extends BusinessException {

    // 기본 생성자 - CommonErrorCode 사용 (AuthErrorCode가 아직 없을 경우)
    public AuthorizationException() {
        super(CommonErrorCode.RESOURCE_NOT_FOUND, "권한이 없습니다.");
    }

    // 리소스별 권한 확인
    public AuthorizationException(Long userId, String resource, String action) {
        super(CommonErrorCode.RESOURCE_NOT_FOUND,
              String.format("User %d does not have permission to %s %s", userId, action, resource));
    }

    // 역할 기반 권한 확인
    public AuthorizationException(Long userId, String requiredRole) {
        super(CommonErrorCode.RESOURCE_NOT_FOUND,
              String.format("User %d requires %s role", userId, requiredRole));
    }

    // 현장별 권한 확인 (Build-Up Platform 특화)
    public AuthorizationException(Long userId, Long siteId) {
        super(CommonErrorCode.RESOURCE_NOT_FOUND,
              String.format("User %d does not have access to site %d", userId, siteId));
    }

    // 커스텀 에러 코드 사용
    public AuthorizationException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}