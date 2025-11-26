package com.concrete.buildup.domain.payroll.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 근무일지 정보 DTO
 *
 * <p>급여명세서에 포함되는 개별 근무일지(근무시간 및 수당) 정보를 담습니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@Builder
@Schema(description = "근무일지 정보")
public class AttendanceItemDto {

    @Schema(description = "근무일", example = "2025-09-01")
    private String searchDate;

    @Schema(description = "총 근무시간", example = "8.00")
    private BigDecimal totalWorkHour;

    @Schema(description = "야간 근무시간", example = "0.00")
    private BigDecimal nightWorkHour;

    @Schema(description = "연장 근무시간", example = "0.00")
    private BigDecimal additionalWorkHour;

    @Schema(description = "휴일 근무시간", example = "0.00")
    private BigDecimal holidayWorkHour;

    @Schema(description = "수당 금액", example = "150000")
    private BigDecimal allowanceAmount;
}