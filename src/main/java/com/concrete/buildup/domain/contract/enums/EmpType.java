package com.concrete.buildup.domain.contract.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 근로자 유형을 나타내는 Enum
 *
 * <p>고용 형태에 따른 근로자의 분류를 정의합니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@RequiredArgsConstructor
public enum EmpType {

    /**
     * 미계약 - 아직 근로계약을 체결하지 않은 근로자
     * DB에서 emp_type이 NULL인 경우를 필터링할 때 사용
     */
    UNCONTRACTED("미계약"),

    /**
     * 일용직 - 일 단위로 고용되는 단기 근로자
     */
    DAILY("일용직"),

    /**
     * 상용직 - 계속적·정기적으로 고용되는 근로자
     */
    PERMANENT("상용직");

    /**
     * 근로자 유형에 대한 한글 설명
     */
    private final String description;
}
