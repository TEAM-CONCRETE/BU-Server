package com.concrete.buildup.global.enums;

/**
 * 사용자 역할을 정의하는 Enum
 * <p>
 * 시스템 내에서 사용자가 가질 수 있는 역할을 정의합니다.
 * 각 역할은 특정 권한과 책임을 가지며, 메서드 레벨 보안에서 접근 제어에 사용됩니다.
 * </p>
 */
public enum UserRole {
    /**
     * 근로자
     * <p>
     * 권한: 자신의 근태 기록 조회, 급여 명세서 조회, 계약서 확인 등
     * </p>
     */
    EMPLOYEE("근로자"),

    /**
     * 현장 관리자
     * <p>
     * 권한: 담당 현장의 근로자 관리, 근태 관리, 작업일보 작성 등
     * </p>
     */
    MANAGER("현장 관리자"),

    /**
     * 기업 관리자
     * <p>
     * 권한: 전체 현장 관리, 현장 관리자 관리, 급여 관리, 계약 관리 등
     * </p>
     */
    CORPORATION("기업 관리자"),

    /**
     * 시스템 관리자
     * <p>
     * 권한: 모든 시스템 기능에 대한 전체 접근 권한
     * </p>
     */
    ADMIN("시스템 관리자");

    private final String description;

    UserRole(String description) {
        this.description = description;
    }

    /**
     * 역할의 설명을 반환합니다.
     *
     * @return 역할 설명
     */
    public String getDescription() {
        return description;
    }

    /**
     * Spring Security에서 사용하는 권한 문자열을 반환합니다.
     *
     * @return "ROLE_" 접두사가 붙은 역할 이름
     */
    public String getAuthority() {
        return "ROLE_" + this.name();
    }
}
