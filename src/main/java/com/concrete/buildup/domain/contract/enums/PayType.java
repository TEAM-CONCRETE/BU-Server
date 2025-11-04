package com.concrete.buildup.domain.contract.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 급여 지급 방법을 나타내는 Enum
 *
 * <p>근로자에게 급여를 지급하는 방식을 정의합니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@RequiredArgsConstructor
public enum PayType {

    /**
     * 현금 - 현금으로 직접 지급
     */
    CASH("현금"),

    /**
     * 계좌이체 - 은행 계좌로 이체
     */
    TRANSFER("계좌이체");

    /**
     * 지급 방법에 대한 한글 설명
     */
    private final String description;
}