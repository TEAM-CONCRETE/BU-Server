package com.concrete.buildup.domain.safetydoc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "안전교육 대상자용 근로자 목록 응답")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeListForSafetyEducationResponse {

    @Schema(description = "근로자 목록")
    private List<EmployeeForSafetyEducationDto> items;

    @Schema(description = "근로자 유형별 통계 정보")
    private EmployeeSummaryDto summary;

    @Schema(description = "근로자 통계 정보")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EmployeeSummaryDto {

        @Schema(description = "전체 근로자 수", example = "15")
        private int totalCount;

        @Schema(description = "상용직 근로자 수", example = "10")
        private int permanentCount;

        @Schema(description = "일용직 근로자 수", example = "5")
        private int dailyCount;
    }
}
