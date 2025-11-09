package com.concrete.buildup.domain.contract.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 계약서의 상태를 나타내는 Enum
 *
 * <p>계약서의 생명주기를 관리하며, 각 단계별 상태를 표현합니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@RequiredArgsConstructor
public enum ContractState {

    /**
     * 초안 상태 - 작성 중이거나 아직 발송되지 않은 계약서
     */
    DRAFT("초안"),

    /**
     * 관리자 서명 대기 - 초안 PDF 생성 후 관리자 서명을 기다리는 상태
     */
    MANAGER_SIGNING_PENDING("관리자 서명 대기"),

    /**
     * 근로자 서명 대기 - 관리자 서명 완료 후 근로자 서명을 기다리는 상태
     */
    EMPLOYEE_SIGNING_PENDING("근로자 서명 대기"),

    /**
     * 발송됨 - 근로자에게 전송되었으나 아직 서명되지 않은 상태
     */
    SENT("발송됨"),

    /**
     * 현장 관리자 서명 완료 - 현장 관리자의 서명이 완료된 상태
     */
    ADMIN_SIGNED("관리자 서명 완료"),

    /**
     * 완전 서명 완료 - 모든 당사자의 서명이 완료된 상태
     */
    FULLY_SIGNED("완전 서명 완료"),

    /**
     * 종료됨 - 계약이 종료되거나 해지된 상태
     */
    TERMINATED("종료됨"),

    /**
     * 무효화됨 - 계약이 무효화된 상태
     */
    VOID("무효화됨");

    /**
     * 상태에 대한 한글 설명
     */
    private final String description;
}