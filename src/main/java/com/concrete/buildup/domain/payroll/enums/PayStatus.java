package com.concrete.buildup.domain.payroll.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 급여 지급 상태
 */
@Getter
@RequiredArgsConstructor
public enum PayStatus {

    /**
     * 지급 대기
     */
    PENDING("지급 대기"),

    /**
     * 지급 완료
     */
    PAID("지급 완료"),

    /**
     * 지급 취소
     */
    CANCELLED("지급 취소");

    private final String description;
}