package com.concrete.buildup.domain.safetydoc.dto;

import com.concrete.buildup.domain.safetydoc.entity.SafetyEducationLog;
import com.concrete.buildup.domain.safetydoc.enums.EducationType;
import com.concrete.buildup.domain.safetydoc.enums.SafetyEducationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Schema(description = "안전교육일지 목록 항목")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SafetyEducationLogListResponse {

    @Schema(description = "안전교육일지 ID", example = "1")
    private Long id;

    @Schema(description = "교육 구분", example = "REGULAR")
    private EducationType educationType;

    @Schema(description = "교육 과목명", example = "추락 재해 예방 교육")
    private String educationSubject;

    @Schema(description = "교육 실시자 성명", example = "김안전")
    private String instructorName;

    @Schema(description = "안전교육일지 상태", example = "COMPLETED")
    private SafetyEducationStatus status;

    @Schema(description = "전체 교육 대상자 수", example = "10")
    private int totalAttendeeCount;

    @Schema(description = "서명 완료한 대상자 수", example = "8")
    private int signedAttendeeCount;

    @Schema(description = "생성일시", example = "2024-01-15T09:00:00")
    private LocalDateTime createdAt;

    @Schema(
            description = "PDF URL. 상태에 따라 초안/관리자 서명/최종 PDF URL이 반환됨",
            example = "https://bucket.s3.ap-northeast-2.amazonaws.com/safety-docs/1/2024-01-15/SE-2024-01-15-1.pdf"
    )
    private String pdfUrl;

    public static SafetyEducationLogListResponse from(
            SafetyEducationLog log,
            int totalCount,
            int signedCount
    ) {
        return SafetyEducationLogListResponse.builder()
                .id(log.getId())
                .educationType(log.getEducationType())
                .educationSubject(log.getEducationSubject())
                .instructorName(log.getInstructorName())
                .status(log.getStatus())
                .totalAttendeeCount(totalCount)
                .signedAttendeeCount(signedCount)
                .createdAt(log.getCreatedAt())
                .pdfUrl(log.getPdfUrl())
                .build();
    }
}
