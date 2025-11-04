package com.concrete.buildup.domain.contract.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 서명 검증 상태를 나타내는 Enum
 *
 * <p>전자서명의 검증 프로세스 상태를 관리합니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@RequiredArgsConstructor
public enum VerificationStatus {

    /**
     * 대기 중 - 검증이 아직 시작되지 않음
     */
    PENDING("대기 중"),

    /**
     * 검증 완료 - 서명이 성공적으로 검증됨
     */
    VERIFIED("검증 완료"),

    /**
     * 검증 실패 - 서명 검증에 실패함
     */
    FAILED("검증 실패");

    /**
     * 상태에 대한 한글 설명
     */
    private final String description;
}