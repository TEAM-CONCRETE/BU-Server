package com.concrete.buildup.domain.safetydoc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeListForSafetyEducationResponse {

    private List<EmployeeForSafetyEducationDto> items;
    private EmployeeSummaryDto summary;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EmployeeSummaryDto {
        private int totalCount;
        private int permanentCount;
        private int dailyCount;
    }
}
