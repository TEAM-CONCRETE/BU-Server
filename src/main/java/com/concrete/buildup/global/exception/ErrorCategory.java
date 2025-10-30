package com.concrete.buildup.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 에러 코드 카테고리
 * Build-Up Platform의 도메인 구조에 맞춘 카테고리입니다.
 *
 * 각 도메인별로 1000 단위로 번호 범위를 할당합니다:
 * - COMMON: 0-999
 * - AUTH: 1000-1999
 * - EMPLOYEE: 2000-2999
 * - SITE: 3000-3999
 * - CONTRACT: 4000-4999
 * - PAYROLL: 5000-5999
 * - ATTENDANCE: 6000-6999
 * - WORKREPORT: 7000-7999
 * - SAFETYDOC: 8000-8999
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCategory {
    COMMON("COMMON_"),              // 0-999: 공통 에러
    AUTH("AUTH_"),                  // 1000-1999: 인증/인가
    EMPLOYEE("EMPLOYEE_"),          // 2000-2999: 사원 관리
    SITE("SITE_"),                  // 3000-3999: 현장 관리
    CONTRACT("CONTRACT_"),          // 4000-4999: 계약 관리
    PAYROLL("PAYROLL_"),            // 5000-5999: 급여 관리
    ATTENDANCE("ATTENDANCE_"),      // 6000-6999: 근태 관리
    WORKREPORT("WORKREPORT_"),      // 7000-7999: 작업일보
    SAFETYDOC("SAFETYDOC_");        // 8000-8999: 안전교육일지

    private final String prefix;

    /**
     * 에러 코드 생성
     * @param codeNumber 도메인별 에러 번호
     * @return 전체 에러 코드 (예: "AUTH_1001")
     */
    public String generate(int codeNumber) {
        return prefix + codeNumber;
    }
}