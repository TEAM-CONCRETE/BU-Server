package com.concrete.buildup.domain.payroll.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 급여 집계 정보 DTO
 *
 * <p>급여명세서에 표시되는 총 근무시간, 총 지급액 등의 집계 정보를 담습니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@Builder
@Schema(description = "급여 집계 정보")
public class PayrollSummaryDto {

    @Schema(description = "총 근무시간", example = "160.00")
    private BigDecimal totalWorkHour;

    @Schema(description = "총 지급액", example = "2509000")
    private BigDecimal totalPay;

    @Schema(description = "총 근무일수", example = "20")
    private Integer totalWorkDays;
}