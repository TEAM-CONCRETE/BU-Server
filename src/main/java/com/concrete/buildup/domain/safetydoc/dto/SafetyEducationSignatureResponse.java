package com.concrete.buildup.domain.safetydoc.dto;

import com.concrete.buildup.domain.safetydoc.enums.SafetyEducationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Schema(description = "서명 처리 응답")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SafetyEducationSignatureResponse {

    @Schema(
            description = "안전교육일지 ID",
            example = "1"
    )
    private Long safetyEducationLogId;

    @Schema(
            description = """
                    서명 후 안전교육일지 상태
                    - 관리자 서명 후: MANAGER_SIGNED
                    - 모든 참석자 서명 완료 후: COMPLETED
                    """,
            example = "MANAGER_SIGNED"
    )
    private SafetyEducationStatus status;

    @Schema(
            description = """
                    현재 PDF URL
                    - 관리자 서명 후: 관리자 서명이 스탬핑된 PDF
                    - 모든 참석자 서명 완료 후: 최종 PDF (모든 서명 포함)
                    """,
            example = "https://bucket.s3.ap-northeast-2.amazonaws.com/safety-docs/1/2024-01-15/SE-2024-01-15-1-manager-signed.pdf"
    )
    private String pdfUrl;

    @Schema(
            description = """
                    최종 PDF 해시값 (SHA256).
                    모든 참석자 서명 완료 시에만 값이 존재하며, PDF 무결성 검증에 사용.
                    참석자 서명이 완료되지 않은 경우 null.
                    """,
            example = "a1b2c3d4e5f6789...",
            nullable = true
    )
    private String pdfHash;

    @Schema(
            description = "서명 처리 일시",
            example = "2024-01-15T10:30:00"
    )
    private LocalDateTime signedAt;
}
