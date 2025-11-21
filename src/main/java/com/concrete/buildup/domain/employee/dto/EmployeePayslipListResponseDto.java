package com.concrete.buildup.domain.employee.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 사원의 급여명세서 목록 응답 DTO
 */
@Getter
@Builder
public class EmployeePayslipListResponseDto {

    private Long employeeId;
    private List<EmployeePayslipSummaryDto> payslips;

    public static EmployeePayslipListResponseDto of(Long employeeId, List<EmployeePayslipSummaryDto> payslips) {
        return EmployeePayslipListResponseDto.builder()
                .employeeId(employeeId)
                .payslips(payslips)
                .build();
    }
}