package com.concrete.buildup.domain.safetydoc.dto;

import com.concrete.buildup.domain.safetydoc.enums.SafetyEducationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "안전교육일지 생성 응답")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSafetyEducationLogResponse {

    @Schema(
            description = "생성된 안전교육일지 ID",
            example = "1"
    )
    private Long safetyEducationLogId;

    @Schema(
            description = "안전교육일지 상태. 생성 직후에는 MANAGER_SIGNING_PENDING(관리자 서명 대기)",
            example = "MANAGER_SIGNING_PENDING"
    )
    private SafetyEducationStatus status;

    @Schema(
            description = "생성된 초안 PDF URL (S3)",
            example = "https://bucket.s3.ap-northeast-2.amazonaws.com/safety-docs/1/2024-01-15/SE-2024-01-15-1.pdf"
    )
    private String pdfUrl;

    @Schema(
            description = "교육 대상자 수",
            example = "5"
    )
    private int attendeeCount;
}
