package com.concrete.buildup.domain.safetydoc.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 안전교육일지 상태 열거형
 */
@Schema(description = """
        안전교육일지 상태

        ## 상태 흐름
        ```
        DRAFT → MANAGER_SIGNING_PENDING → MANAGER_SIGNED → COMPLETED
        ```

        | 상태 | 설명 | 다음 액션 |
        |---|---|---|
        | DRAFT | 초안 (미사용) | - |
        | MANAGER_SIGNING_PENDING | 관리자 서명 대기 | 관리자 서명 API 호출 |
        | MANAGER_SIGNED | 관리자 서명 완료 | 참석자 서명 API 호출 |
        | COMPLETED | 완료 (모든 서명 완료) | - |
        """)
@Getter
@RequiredArgsConstructor
public enum SafetyEducationStatus {

    @Schema(description = "초안 상태 (현재 미사용)")
    DRAFT("초안"),

    @Schema(description = "관리자 서명 대기 - 안전교육일지 생성 직후 상태. 관리자 서명이 필요함")
    MANAGER_SIGNING_PENDING("관리자 서명 대기"),

    @Schema(description = "관리자 서명 완료 - 관리자가 서명을 완료한 상태. 참석자 서명이 필요함")
    MANAGER_SIGNED("관리자 서명 완료"),

    @Schema(description = "완료 - 모든 참석자의 서명이 완료되어 최종 PDF가 생성된 상태")
    COMPLETED("완료");

    private final String description;
}
