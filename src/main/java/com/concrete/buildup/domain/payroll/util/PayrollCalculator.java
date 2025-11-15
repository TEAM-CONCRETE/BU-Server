package com.concrete.buildup.domain.payroll.util;

import com.concrete.buildup.domain.contract.entity.ContractDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 급여 계산 유틸리티
 *
 * 계약 조건과 근무 기록을 기반으로 급여를 계산합니다.
 *
 * 주요 기능:
 * - 기본급 계산
 * - 각종 수당 계산 (야간, 연장, 휴일)
 * - 비과세 소득 계산
 * - 소득세 및 주민세 계산
 * - 4대보험 계산
 */
@Slf4j
@Component
public class PayrollCalculator {

    // 세율
    private static final BigDecimal INCOME_TAX_RATE = new BigDecimal("0.033"); // 소득세 3.3%
    private static final BigDecimal RESIDENT_TAX_RATE = new BigDecimal("0.003"); // 주민세 0.3%

    // 4대보험 요율 (근로자 부담분)
    private static final BigDecimal NATIONAL_PENSION_RATE = new BigDecimal("0.045"); // 국민연금 4.5%
    private static final BigDecimal HEALTH_INSURANCE_RATE = new BigDecimal("0.03545"); // 건강보험 3.545%
    private static final BigDecimal LONG_TERM_CARE_RATE = new BigDecimal("0.004591"); // 장기요양 0.4591%
    private static final BigDecimal EMPLOYMENT_INSURANCE_RATE = new BigDecimal("0.009"); // 고용보험 0.9%

    // 비과세 한도 (식대 등)
    private static final BigDecimal NON_TAXABLE_LIMIT = new BigDecimal("200000"); // 월 20만원

    /**
     * 기본급 계산
     *
     * @param hourlyRate 시급
     * @param workHours 근무시간
     * @return 기본급
     */
    public BigDecimal calculateBasePay(BigDecimal hourlyRate, BigDecimal workHours) {
        if (hourlyRate == null || workHours == null) {
            return BigDecimal.ZERO;
        }
        return hourlyRate.multiply(workHours).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 야간근로 수당 계산
     * 기본 시급의 50% 가산
     *
     * @param hourlyRate 시급
     * @param nightHours 야간근무시간
     * @return 야간근로 수당
     */
    public BigDecimal calculateNightWorkPay(BigDecimal hourlyRate, BigDecimal nightHours) {
        if (hourlyRate == null || nightHours == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal nightRate = hourlyRate.multiply(new BigDecimal("1.5"));
        return nightRate.multiply(nightHours).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 연장근로 수당 계산
     * 기본 시급의 50% 가산
     *
     * @param hourlyRate 시급
     * @param overtimeHours 연장근무시간
     * @return 연장근로 수당
     */
    public BigDecimal calculateOvertimePay(BigDecimal hourlyRate, BigDecimal overtimeHours) {
        if (hourlyRate == null || overtimeHours == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal overtimeRate = hourlyRate.multiply(new BigDecimal("1.5"));
        return overtimeRate.multiply(overtimeHours).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 휴일근로 수당 계산
     * 기본 시급의 50% 가산 (8시간 이내)
     * 8시간 초과 시 100% 가산
     *
     * @param hourlyRate 시급
     * @param holidayHours 휴일근무시간
     * @return 휴일근로 수당
     */
    public BigDecimal calculateHolidayPay(BigDecimal hourlyRate, BigDecimal holidayHours) {
        if (hourlyRate == null || holidayHours == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = BigDecimal.ZERO;

        // 8시간 이내: 50% 가산
        BigDecimal normalHolidayHours = holidayHours.min(new BigDecimal("8"));
        BigDecimal normalHolidayRate = hourlyRate.multiply(new BigDecimal("1.5"));
        total = total.add(normalHolidayRate.multiply(normalHolidayHours));

        // 8시간 초과: 100% 가산
        if (holidayHours.compareTo(new BigDecimal("8")) > 0) {
            BigDecimal excessHours = holidayHours.subtract(new BigDecimal("8"));
            BigDecimal excessRate = hourlyRate.multiply(new BigDecimal("2.0"));
            total = total.add(excessRate.multiply(excessHours));
        }

        return total.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 주휴수당 계산
     * 주 15시간 이상 근무 시 지급
     * (주간 근무시간 / 40) * 8시간 * 시급
     *
     * @param hourlyRate 시급
     * @param weeklyWorkHours 주간 근무시간
     * @return 주휴수당
     */
    public BigDecimal calculateWeeklyHolidayPay(BigDecimal hourlyRate, BigDecimal weeklyWorkHours) {
        if (hourlyRate == null || weeklyWorkHours == null) {
            return BigDecimal.ZERO;
        }

        // 주 15시간 미만이면 지급 안함
        if (weeklyWorkHours.compareTo(new BigDecimal("15")) < 0) {
            return BigDecimal.ZERO;
        }

        // (주간 근무시간 / 40) * 8시간 * 시급
        BigDecimal ratio = weeklyWorkHours.divide(new BigDecimal("40"), 4, RoundingMode.HALF_UP);
        BigDecimal holidayHours = new BigDecimal("8");
        return hourlyRate.multiply(holidayHours).multiply(ratio).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 비과세 소득 계산
     * 식대 등 (월 20만원 한도)
     *
     * @param contractDetail 계약 상세 정보
     * @return 비과세 소득
     */
    public BigDecimal calculateNonTaxableIncome(ContractDetail contractDetail) {
        // TODO: 실제 비과세 항목 계산 로직 구현
        // 현재는 기본적으로 0 반환
        return BigDecimal.ZERO;
    }

    /**
     * 소득세 계산
     * 과세 소득의 3.3%
     *
     * @param taxableIncome 과세 소득
     * @return 소득세
     */
    public BigDecimal calculateIncomeTax(BigDecimal taxableIncome) {
        if (taxableIncome == null || taxableIncome.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return taxableIncome.multiply(INCOME_TAX_RATE).setScale(0, RoundingMode.FLOOR);
    }

    /**
     * 주민세 계산
     * 소득세의 10% (과세 소득의 0.3%)
     *
     * @param taxableIncome 과세 소득
     * @return 주민세
     */
    public BigDecimal calculateResidentTax(BigDecimal taxableIncome) {
        if (taxableIncome == null || taxableIncome.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return taxableIncome.multiply(RESIDENT_TAX_RATE).setScale(0, RoundingMode.FLOOR);
    }

    /**
     * 국민연금 계산
     * 기준소득월액의 4.5%
     *
     * @param baseSalary 기준소득월액
     * @param isApplicable 적용 여부
     * @return 국민연금
     */
    public BigDecimal calculateNationalPension(BigDecimal baseSalary, boolean isApplicable) {
        if (!isApplicable || baseSalary == null || baseSalary.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return baseSalary.multiply(NATIONAL_PENSION_RATE).setScale(0, RoundingMode.FLOOR);
    }

    /**
     * 건강보험 계산
     * 기준소득월액의 3.545%
     *
     * @param baseSalary 기준소득월액
     * @param isApplicable 적용 여부
     * @return 건강보험
     */
    public BigDecimal calculateHealthInsurance(BigDecimal baseSalary, boolean isApplicable) {
        if (!isApplicable || baseSalary == null || baseSalary.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return baseSalary.multiply(HEALTH_INSURANCE_RATE).setScale(0, RoundingMode.FLOOR);
    }

    /**
     * 장기요양보험 계산
     * 건강보험의 12.95% (기준소득월액의 0.4591%)
     *
     * @param baseSalary 기준소득월액
     * @param isApplicable 적용 여부
     * @return 장기요양보험
     */
    public BigDecimal calculateLongTermCare(BigDecimal baseSalary, boolean isApplicable) {
        if (!isApplicable || baseSalary == null || baseSalary.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return baseSalary.multiply(LONG_TERM_CARE_RATE).setScale(0, RoundingMode.FLOOR);
    }

    /**
     * 고용보험 계산
     * 기준소득월액의 0.9%
     *
     * @param baseSalary 기준소득월액
     * @param isApplicable 적용 여부
     * @return 고용보험
     */
    public BigDecimal calculateEmploymentInsurance(BigDecimal baseSalary, boolean isApplicable) {
        if (!isApplicable || baseSalary == null || baseSalary.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return baseSalary.multiply(EMPLOYMENT_INSURANCE_RATE).setScale(0, RoundingMode.FLOOR);
    }

    /**
     * 실수령액 계산
     * 총 지급액 - (소득세 + 주민세 + 4대보험)
     *
     * @param totalPay 총 지급액
     * @param incomeTax 소득세
     * @param residentTax 주민세
     * @param nationalPension 국민연금
     * @param healthInsurance 건강보험
     * @param longTermCare 장기요양보험
     * @param employmentInsurance 고용보험
     * @return 실수령액
     */
    public BigDecimal calculateNetPay(BigDecimal totalPay, BigDecimal incomeTax, BigDecimal residentTax,
                                       BigDecimal nationalPension, BigDecimal healthInsurance,
                                       BigDecimal longTermCare, BigDecimal employmentInsurance) {
        if (totalPay == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalDeduction = BigDecimal.ZERO;

        if (incomeTax != null) totalDeduction = totalDeduction.add(incomeTax);
        if (residentTax != null) totalDeduction = totalDeduction.add(residentTax);
        if (nationalPension != null) totalDeduction = totalDeduction.add(nationalPension);
        if (healthInsurance != null) totalDeduction = totalDeduction.add(healthInsurance);
        if (longTermCare != null) totalDeduction = totalDeduction.add(longTermCare);
        if (employmentInsurance != null) totalDeduction = totalDeduction.add(employmentInsurance);

        return totalPay.subtract(totalDeduction).setScale(0, RoundingMode.FLOOR);
    }
}