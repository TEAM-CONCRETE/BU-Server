package com.concrete.buildup.domain.site.dto;

import com.concrete.buildup.domain.safetydoc.enums.SafetyEducationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 날짜별 안전/작업 문서 정보 DTO
 *
 * <p>기업 관리자가 현장의 안전교육일지와 작업일보를 날짜별로 조회할 때 사용합니다.</p>
 */
@Schema(description = "날짜별 안전/작업 문서 정보")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SafetyWorkDocumentDto {

    @Schema(description = "문서 작성일", example = "2025-11-24")
    private LocalDate date;

    @Schema(description = "안전교육일지 정보 (해당 날짜에 없으면 null)")
    private SafetyEducationLogSummary safetyEducationLog;

    @Schema(description = "작업일보 정보 (해당 날짜에 없으면 null)")
    private WorkReportSummary workReport;

    /**
     * 안전교육일지 요약 정보
     */
    @Schema(description = "안전교육일지 요약 정보")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SafetyEducationLogSummary {

        @Schema(description = "안전교육일지 ID", example = "15")
        private Long logId;

        @Schema(description = "상태", example = "COMPLETED")
        private SafetyEducationStatus status;

        @Schema(description = "교육 과목명", example = "추락 재해 예방 교육")
        private String educationSubject;
    }

    /**
     * 작업일보 요약 정보
     */
    @Schema(description = "작업일보 요약 정보")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WorkReportSummary {

        @Schema(description = "작업일보 ID", example = "23")
        private Long workReportId;

        @Schema(description = "해당 날짜의 순번", example = "1")
        private int sequence;
    }
}
