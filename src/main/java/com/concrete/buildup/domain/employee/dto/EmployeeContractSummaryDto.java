package com.concrete.buildup.domain.employee.dto;

import com.concrete.buildup.domain.contract.entity.Contract;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/**
 * 사원의 근로계약서 요약 DTO
 */
@Getter
@Builder
public class EmployeeContractSummaryDto {

    private Long contractId;
    private String title;
    private LocalDate createdAt;
    private String status;

    public static EmployeeContractSummaryDto from(Contract contract) {
        return EmployeeContractSummaryDto.builder()
                .contractId(contract.getId())
                .title("근로계약서")
                .createdAt(contract.getWrittenAt().toLocalDate())
                .status(contract.getContractState().getDescription())
                .build();
    }
}