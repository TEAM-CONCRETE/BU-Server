package com.concrete.buildup.domain.contract.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 계약서 서명자의 역할을 나타내는 Enum
 *
 * <p>계약서에 서명하는 당사자의 역할을 정의합니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@RequiredArgsConstructor
public enum SignerRole {

    /**
     * 근로자 - 계약의 피고용인
     */
    EMPLOYEE("근로자"),

    /**
     * 관리자 - 현장 관리자 또는 담당자
     */
    MANAGER("관리자"),

    /**
     * 법인 - 사업주 또는 법인 대표
     */
    CORPORATION("법인");

    /**
     * 역할에 대한 한글 설명
     */
    private final String description;
}