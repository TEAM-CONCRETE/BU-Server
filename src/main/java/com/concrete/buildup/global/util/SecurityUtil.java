package com.concrete.buildup.global.util;

import com.concrete.buildup.global.enums.UserRole;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;

/**
 * Spring Security 관련 유틸리티 클래스
 * <p>
 * 현재 인증된 사용자 정보 조회 및 권한 검증 기능을 제공합니다.
 * 모든 메서드가 static이므로 인스턴스 생성 없이 사용 가능합니다.
 * </p>
 */
public class SecurityUtil {

    // 인스턴스화 방지
    private SecurityUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 현재 인증된 사용자의 userId를 반환합니다.
     * <p>
     * 익명 사용자(AnonymousAuthenticationToken)는 인증되지 않은 것으로 간주하여 null을 반환합니다.
     * </p>
     *
     * @return 사용자 ID (로그인 ID), 인증되지 않았거나 익명 사용자인 경우 null
     */
    public static String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        // 익명 사용자는 인증되지 않은 것으로 간주
        if (authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return authentication.getName();
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
     * 현재 인증된 사용자의 권한(Role) 목록을 반환합니다.
     * <p>
     * 익명 사용자(AnonymousAuthenticationToken)는 인증되지 않은 것으로 간주하여 null을 반환합니다.
     * </p>
     *
     * @return 권한 목록, 인증되지 않았거나 익명 사용자인 경우 null
     */
    public static Collection<? extends GrantedAuthority> getCurrentUserAuthorities() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        // 익명 사용자는 인증되지 않은 것으로 간주
        if (authentication instanceof AnonymousAuthenticationToken) {
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
     * @param targetUserId 대상 사용자 ID (로그인 ID)
     * @return 본인이거나 ADMIN/CORPORATION이면 true
     */
    public static boolean canAccessUserData(String targetUserId) {
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
