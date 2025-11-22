package com.concrete.buildup.domain.safetydoc.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 안전교육일지 상태 열거형
 */
@Getter
@RequiredArgsConstructor
public enum SafetyEducationStatus {
    DRAFT("초안"),
    MANAGER_SIGNING_PENDING("관리자 서명 대기"),
    MANAGER_SIGNED("관리자 서명 완료"),
    COMPLETED("완료");

    private final String description;
}
