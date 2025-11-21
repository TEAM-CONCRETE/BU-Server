package com.concrete.buildup.domain.employee.dto;

import com.concrete.buildup.domain.payroll.entity.Payroll;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/**
 * 사원의 급여명세서 요약 DTO
 */
@Getter
@Builder
public class EmployeePayslipSummaryDto {

    private Long payrollId;
    private String title;
    private LocalDate createdAt;
    private String status;

    public static EmployeePayslipSummaryDto from(Payroll payroll) {
        return EmployeePayslipSummaryDto.builder()
                .payrollId(payroll.getId())
                .title("급여명세서")
                .createdAt(payroll.getSearchDate())
                .status(payroll.getPayStatus().getDescription())
                .build();
    }
}
