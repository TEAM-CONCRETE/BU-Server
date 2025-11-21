package com.concrete.buildup.domain.employee.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 사원의 근로계약서 목록 응답 DTO
 */
@Getter
@Builder
public class EmployeeContractListResponseDto {

    private Long employeeId;
    private List<EmployeeContractSummaryDto> contracts;

    public static EmployeeContractListResponseDto of(Long employeeId, List<EmployeeContractSummaryDto> contracts) {
        return EmployeeContractListResponseDto.builder()
                .employeeId(employeeId)
                .contracts(contracts)
                .build();
    }
}