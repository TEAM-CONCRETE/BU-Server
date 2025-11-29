package com.concrete.buildup.domain.payroll.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 급여 계산 결과 DTO
 *
 * <p>급여 계산의 최종 결과를 담는 DTO입니다.</p>
 * <p>지급 항목, 공제 항목, 실지급액을 모두 포함합니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "급여 계산 결과")
public class PayrollCalculationResult {

    // ========== 지급 항목 ==========

    @Schema(description = "기본급 (시급 × 총 통상시간)", example = "1600000")
    private BigDecimal basePay;

    @Schema(description = "연장수당 (시급 × 연장시간 × 1.5)", example = "150000")
    private BigDecimal overtimePay;

    @Schema(description = "야간수당 (시급 × 야간시간 × 0.5)", example = "50000")
    private BigDecimal nightPay;

    @Schema(description = "휴일수당 (시급 × 휴일시간 × 1.5)", example = "120000")
    private BigDecimal holidayPay;

    @Schema(description = "주휴수당 (일용직 조건 충족 시)", example = "80000")
    private BigDecimal weeklyHolidayPay;

    @Schema(description = "총 지급액 (모든 지급 항목의 합)", example = "2000000")
    private BigDecimal totalPay;

    // ========== 공제 항목 ==========

    @Schema(description = "소득세 (일용직: 일별 계산, 상용직: 간이세액표)", example = "33000")
    private BigDecimal incomeTax;

    @Schema(description = "지방소득세 (주민세, 소득세의 10%)", example = "3300")
    private BigDecimal residentTax;

    @Schema(description = "고용보험 (과세대상금액 × 0.9%)", example = "18000")
    private BigDecimal employmentInsurance;

    @Schema(description = "건강보험 (과세대상금액 × 3.545%)", example = "70900")
    private BigDecimal healthInsurance;

    @Schema(description = "장기요양보험 (건강보험료 × 12.81%)", example = "9084")
    private BigDecimal longTermCareInsurance;

    @Schema(description = "국민연금 (과세대상금액 × 4.5%)", example = "90000")
    private BigDecimal nationalPension;

    @Schema(description = "총 공제액 (모든 공제 항목의 합)", example = "224284")
    private BigDecimal totalDeduction;

    // ========== 최종 금액 ==========

    @Schema(description = "실지급액 (총 지급액 - 총 공제액)", example = "1775716")
    private BigDecimal netPay;

    // ========== 계산 상세 (디버깅/로그용) ==========

    @Schema(description = "계산 상세 정보 (디버깅용)", example = "{\"totalWorkHours\": 160, \"taxableIncome\": 2000000}")
    private Map<String, Object> calculationDetails;

    /**
     * 총 지급액 계산
     *
     * @return 모든 지급 항목의 합
     */
    public BigDecimal calculateTotalPay() {
        BigDecimal total = BigDecimal.ZERO;
        if (basePay != null) total = total.add(basePay);
        if (overtimePay != null) total = total.add(overtimePay);
        if (nightPay != null) total = total.add(nightPay);
        if (holidayPay != null) total = total.add(holidayPay);
        if (weeklyHolidayPay != null) total = total.add(weeklyHolidayPay);
        return total;
    }

    /**
     * 총 공제액 계산
     *
     * @return 모든 공제 항목의 합
     */
    public BigDecimal calculateTotalDeduction() {
        BigDecimal total = BigDecimal.ZERO;
        if (incomeTax != null) total = total.add(incomeTax);
        if (residentTax != null) total = total.add(residentTax);
        if (employmentInsurance != null) total = total.add(employmentInsurance);
        if (healthInsurance != null) total = total.add(healthInsurance);
        if (longTermCareInsurance != null) total = total.add(longTermCareInsurance);
        if (nationalPension != null) total = total.add(nationalPension);
        return total;
    }

    /**
     * 실지급액 계산
     *
     * @return 총 지급액 - 총 공제액
     */
    public BigDecimal calculateNetPay() {
        BigDecimal total = totalPay != null ? totalPay : calculateTotalPay();
        BigDecimal deduction = totalDeduction != null ? totalDeduction : calculateTotalDeduction();
        return total.subtract(deduction);
    }
}