package com.concrete.buildup.domain.contract.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 급여 지급 주기를 나타내는 Enum
 *
 * <p>근로자에게 급여를 지급하는 주기를 정의합니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@RequiredArgsConstructor
public enum PayPeriod {

    /**
     * 일급 - 일 단위로 급여 지급
     */
    DAILY("일급"),

    /**
     * 주급 - 주 단위로 급여 지급
     */
    WEEKLY("주급"),

    /**
     * 월급 - 월 단위로 급여 지급
     */
    MONTHLY("월급");

    /**
     * 지급 주기에 대한 한글 설명
     */
    private final String description;
}