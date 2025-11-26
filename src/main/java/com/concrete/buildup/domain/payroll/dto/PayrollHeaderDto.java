package com.concrete.buildup.domain.payroll.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 급여명세서 헤더 정보 DTO
 *
 * <p>급여명세서 상단에 표시되는 기본 정보를 담습니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@Builder
@Schema(description = "급여명세서 헤더 정보")
public class PayrollHeaderDto {

    @Schema(description = "급여 ID", example = "1")
    private Long payrollId;

    @Schema(description = "근로자 이름", example = "박승희")
    private String employeeName;

    @Schema(description = "주민등록번호 (마스킹)", example = "920315-2******")
    private String residentNum;

    @Schema(description = "지급 예정일", example = "2025-09-12")
    private String payDueDate;

    @Schema(description = "총 지급액", example = "2509000")
    private BigDecimal totalPay;

    @Schema(description = "비과세 소득", example = "180000")
    private BigDecimal noneTaxIncome;
}