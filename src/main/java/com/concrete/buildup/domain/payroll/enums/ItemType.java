package com.concrete.buildup.domain.payroll.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 급여 항목 구분
 */
@Getter
@RequiredArgsConstructor
public enum ItemType {

    /**
     * 지급 항목 (기본급, 수당 등)
     */
    EARNING("지급"),

    /**
     * 공제 항목 (세금, 4대보험 등)
     */
    DEDUCTION("공제");

    private final String description;
}