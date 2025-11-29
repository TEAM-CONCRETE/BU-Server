package com.concrete.buildup.domain.payroll.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 4대보험 요율 DTO
 *
 * <p>4대보험의 근로자 부담 요율 정보를 담는 DTO입니다.</p>
 * <p>2025년 기준 요율: 국민연금 4.5%, 건강보험 3.545%, 장기요양 12.81%, 고용보험 0.9%</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "4대보험 요율 정보")
public class InsuranceRates {

    @Schema(description = "국민연금 요율 (2025년 기준: 0.045)", example = "0.045", required = true)
    private BigDecimal nationalPension;

    @Schema(description = "건강보험 요율 (2025년 기준: 0.03545)", example = "0.03545", required = true)
    private BigDecimal healthInsurance;

    @Schema(description = "장기요양보험 요율 (건강보험료의 비율, 2025년 기준: 0.1281)", example = "0.1281", required = true)
    private BigDecimal longTermCare;

    @Schema(description = "고용보험 요율 (2025년 기준: 0.009)", example = "0.009", required = true)
    private BigDecimal employmentInsurance;

    // 산재보험은 사업주 전액 부담이므로 제외

    /**
     * 2025년 기준 기본 요율로 초기화된 InsuranceRates 생성
     *
     * @return 2025년 기준 요율
     */
    public static InsuranceRates defaultRates2025() {
        return InsuranceRates.builder()
                .nationalPension(new BigDecimal("0.045"))
                .healthInsurance(new BigDecimal("0.03545"))
                .longTermCare(new BigDecimal("0.1281"))
                .employmentInsurance(new BigDecimal("0.009"))
                .build();
    }
}