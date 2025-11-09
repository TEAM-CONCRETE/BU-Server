package com.concrete.buildup.global.util;

import com.concrete.buildup.global.enums.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * Spring Security 관련 유틸리티 클래스
 * <p>
 * 현재 인증된 사용자 정보 조회 및 권한 검증 기능을 제공합니다.
 * </p>
 */
@Component
public class SecurityUtil {

    /**
     * 현재 인증된 사용자의 userId를 반환합니다.
     *
     * @return 사용자 ID (로그인 ID)
     */
    public static String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication.getName();
    }

    /**
     * 현재 인증된 사용자의 PK(Long 타입 ID)를 반환합니다.
     * JWT 토큰에서 추출한 사용자 ID를 반환합니다.
     *
     * @return 사용자 PK
     */
    public static Long getCurrentUserPk() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        // JWT에서 사용자 PK를 Principal에 저장했다고 가정
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long) {
            return (Long) principal;
        }
        return null;
    }

    /**
     * 현재 사용자가 특정 userId와 동일한지 확인합니다.
     *
     * @param userId 비교할 사용자 ID
     * @return 동일하면 true, 아니면 false
     */
    public static boolean isCurrentUser(String userId) {
        String currentUserId = getCurrentUserId();
        return currentUserId != null && currentUserId.equals(userId);
    }

    /**
     * 현재 사용자가 특정 PK와 동일한지 확인합니다.
     *
     * @param userPk 비교할 사용자 PK
     * @return 동일하면 true, 아니면 false
     */
    public static boolean isCurrentUser(Long userPk) {
        Long currentUserPk = getCurrentUserPk();
        return currentUserPk != null && currentUserPk.equals(userPk);
    }

    /**
     * 현재 인증된 사용자의 권한(Role) 목록을 반환합니다.
     *
     * @return 권한 목록
     */
    public static Collection<? extends GrantedAuthority> getCurrentUserAuthorities() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        return authentication.getAuthorities();
    }

    /**
     * 현재 사용자가 특정 역할을 가지고 있는지 확인합니다.
     *
     * @param role 확인할 역할
     * @return 역할을 가지고 있으면 true, 아니면 false
     */
    public static boolean hasRole(UserRole role) {
        Collection<? extends GrantedAuthority> authorities = getCurrentUserAuthorities();
        if (authorities == null) {
            return false;
        }
        String roleAuthority = role.getAuthority();
        return authorities.stream()
                .anyMatch(authority -> authority.getAuthority().equals(roleAuthority));
    }

    /**
     * 현재 사용자가 ADMIN 역할을 가지고 있는지 확인합니다.
     *
     * @return ADMIN이면 true, 아니면 false
     */
    public static boolean isAdmin() {
        return hasRole(UserRole.ADMIN);
    }

    /**
     * 현재 사용자가 CORPORATION 역할을 가지고 있는지 확인합니다.
     *
     * @return CORPORATION이면 true, 아니면 false
     */
    public static boolean isCorporation() {
        return hasRole(UserRole.CORPORATION);
    }

    /**
     * 현재 사용자가 MANAGER 역할을 가지고 있는지 확인합니다.
     *
     * @return MANAGER이면 true, 아니면 false
     */
    public static boolean isManager() {
        return hasRole(UserRole.MANAGER);
    }

    /**
     * 현재 사용자가 EMPLOYEE 역할을 가지고 있는지 확인합니다.
     *
     * @return EMPLOYEE이면 true, 아니면 false
     */
    public static boolean isEmployee() {
        return hasRole(UserRole.EMPLOYEE);
    }

    /**
     * 현재 사용자가 본인이거나 ADMIN 또는 CORPORATION 권한을 가지고 있는지 확인합니다.
     * <p>
     * EMPLOYEE가 본인 정보를 조회하거나, ADMIN/CORPORATION이 모든 정보를 조회할 수 있도록 허용
     * </p>
     *
     * @param targetUserId 대상 사용자 ID
     * @return 본인이거나 ADMIN/CORPORATION이면 true
     */
    public static boolean canAccessUserData(Long targetUserId) {
        return isCurrentUser(targetUserId) || isAdmin() || isCorporation();
    }

    /**
     * 현재 사용자가 MANAGER이거나 상위 권한(CORPORATION, ADMIN)을 가지고 있는지 확인합니다.
     *
     * @return MANAGER 이상의 권한이면 true
     */
    public static boolean isManagerOrAbove() {
        return isManager() || isCorporation() || isAdmin();
    }

    /**
     * 현재 사용자가 CORPORATION이거나 ADMIN 권한을 가지고 있는지 확인합니다.
     *
     * @return CORPORATION 이상의 권한이면 true
     */
    public static boolean isCorporationOrAdmin() {
        return isCorporation() || isAdmin();
    }
}
