package com.concrete.buildup.domain.payroll.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 급여항목 상세 DTO
 *
 * <p>급여명세서의 개별 급여항목(지급/공제) 정보를 담습니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@Builder
@Schema(description = "급여항목 상세")
public class PayslipItemDto {

    @Schema(description = "항목 ID", example = "1")
    private Long itemId;

    @Schema(description = "항목명", example = "기본급")
    private String itemName;

    @Schema(description = "항목 유형 (ALLOWANCE: 지급, DEDUCTION: 공제)", example = "ALLOWANCE")
    private String itemType;

    @Schema(description = "금액", example = "2000000")
    private BigDecimal amount;

    @Schema(description = "적용일", example = "2025-09-01")
    private String effectiveDate;
}