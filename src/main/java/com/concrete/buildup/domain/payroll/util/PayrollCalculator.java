package com.concrete.buildup.domain.payroll.util;

import com.concrete.buildup.domain.contract.entity.ContractDetail;
import com.concrete.buildup.domain.contract.enums.EmpType;
import com.concrete.buildup.domain.payroll.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

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
    private static final BigDecimal WORKERS_COMP_INSURANCE_RATE = new BigDecimal("0.007"); // 산재보험 0.7% (업종별 상이)
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
     * 산재보험 계산
     * 기준소득월액의 0.7% (업종별로 요율 상이, 건설업 평균 적용)
     *
     * @param baseSalary 기준소득월액
     * @param isApplicable 적용 여부
     * @return 산재보험
     */
    public BigDecimal calculateWorkersCompInsurance(BigDecimal baseSalary, boolean isApplicable) {
        if (!isApplicable || baseSalary == null || baseSalary.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return baseSalary.multiply(WORKERS_COMP_INSURANCE_RATE).setScale(0, RoundingMode.FLOOR);
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
     * @param workersCompInsurance 산재보험
     * @param employmentInsurance 고용보험
     * @return 실수령액
     */
    public BigDecimal calculateNetPay(BigDecimal totalPay, BigDecimal incomeTax, BigDecimal residentTax,
                                       BigDecimal nationalPension, BigDecimal healthInsurance,
                                       BigDecimal workersCompInsurance, BigDecimal employmentInsurance) {
        if (totalPay == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalDeduction = BigDecimal.ZERO;

        if (incomeTax != null) totalDeduction = totalDeduction.add(incomeTax);
        if (residentTax != null) totalDeduction = totalDeduction.add(residentTax);
        if (nationalPension != null) totalDeduction = totalDeduction.add(nationalPension);
        if (healthInsurance != null) totalDeduction = totalDeduction.add(healthInsurance);
        if (workersCompInsurance != null) totalDeduction = totalDeduction.add(workersCompInsurance);
        if (employmentInsurance != null) totalDeduction = totalDeduction.add(employmentInsurance);

        return totalPay.subtract(totalDeduction).setScale(0, RoundingMode.FLOOR);
    }

    // ========================================
    // v1.1 급여 계산 통합 메서드
    // ========================================

    /**
     * v1.1 급여 계산 (통합 메서드)
     *
     * <p>모든 급여 계산 로직을 통합하여 처리합니다.</p>
     * <p>가산수당 조건 판정, 4대보험, 세금 계산을 포함합니다.</p>
     *
     * @param input 급여 계산 입력 정보
     * @return 급여 계산 결과
     */
    public PayrollCalculationResult calculate(PayrollCalculationInput input) {
        log.info("v1.1 급여 계산 시작 - empType: {}, payPeriod: {}", input.getEmpType(), input.getPayPeriod());

        Map<String, Object> details = new HashMap<>();

        // 1) 기본급 계산
        BigDecimal totalWorkHours = input.getDailyWorkHours().multiply(new BigDecimal(input.getWorkDays()));
        BigDecimal basePay = input.getHourlyRate().multiply(totalWorkHours).setScale(0, RoundingMode.FLOOR);
        details.put("totalWorkHours", totalWorkHours);
        details.put("basePay", basePay);

        // 2) 가산수당 지급 가능 여부 판정
        boolean canPayOvertime = canPayOvertimeAllowance(input.getOvertimeConditions());
        boolean canPayNight = canPayNightAllowance(input.getOvertimeConditions());
        boolean canPayHoliday = canPayHolidayAllowance(input.getOvertimeConditions());

        details.put("canPayOvertime", canPayOvertime);
        details.put("canPayNight", canPayNight);
        details.put("canPayHoliday", canPayHoliday);

        // 3) 조건 미충족 시 시간 무효화
        BigDecimal overtimeHours = canPayOvertime ? nvl(input.getOvertimeHours()) : BigDecimal.ZERO;
        BigDecimal nightHours = canPayNight ? nvl(input.getNightHours()) : BigDecimal.ZERO;
        BigDecimal holidayHours = canPayHoliday ? nvl(input.getHolidayHours()) : BigDecimal.ZERO;

        // 4) 가산수당 계산 (중복가산 규칙 반영)
        BigDecimal overtimePay = input.getHourlyRate().multiply(overtimeHours).multiply(new BigDecimal("1.5"))
                .setScale(0, RoundingMode.FLOOR);
        BigDecimal nightPay = input.getHourlyRate().multiply(nightHours).multiply(new BigDecimal("0.5"))
                .setScale(0, RoundingMode.FLOOR);
        BigDecimal holidayPay = input.getHourlyRate().multiply(holidayHours).multiply(new BigDecimal("1.5"))
                .setScale(0, RoundingMode.FLOOR);

        // 5) 주휴수당 계산 (일용직만)
        BigDecimal weeklyHolidayPay = BigDecimal.ZERO;
        if (EmpType.DAILY.equals(input.getEmpType()) && Boolean.TRUE.equals(input.getWeeklyHolidayEligible())) {
            weeklyHolidayPay = input.getHourlyRate().multiply(input.getDailyWorkHours())
                    .setScale(0, RoundingMode.FLOOR);
        }

        // 6) 총 지급액
        BigDecimal totalPay = basePay.add(overtimePay).add(nightPay).add(holidayPay).add(weeklyHolidayPay);
        BigDecimal taxableIncome = totalPay; // 비과세 미적용

        details.put("taxableIncome", taxableIncome);

        // 7) 4대보험 계산
        InsuranceResult insurance = EmpType.DAILY.equals(input.getEmpType())
                ? calculateDailyInsurance(input, taxableIncome)
                : calculatePermanentInsurance(input, taxableIncome);

        // 8) 세금 계산
        TaxResult tax = EmpType.DAILY.equals(input.getEmpType())
                ? calculateDailyTax(input, totalPay)
                : calculatePermanentTax(input, taxableIncome);

        // 9) 총 공제액 및 실지급액
        BigDecimal totalDeduction = insurance.getTotal().add(tax.getTotal());
        BigDecimal netPay = totalPay.subtract(totalDeduction).setScale(0, RoundingMode.FLOOR);

        log.info("v1.1 급여 계산 완료 - totalPay: {}, totalDeduction: {}, netPay: {}",
                totalPay, totalDeduction, netPay);

        return PayrollCalculationResult.builder()
                .basePay(basePay)
                .overtimePay(overtimePay)
                .nightPay(nightPay)
                .holidayPay(holidayPay)
                .weeklyHolidayPay(weeklyHolidayPay)
                .totalPay(totalPay)
                .incomeTax(tax.getIncomeTax())
                .residentTax(tax.getResidentTax())
                .employmentInsurance(insurance.getEmploymentInsurance())
                .healthInsurance(insurance.getHealthInsurance())
                .longTermCareInsurance(insurance.getLongTermCareInsurance())
                .nationalPension(insurance.getNationalPension())
                .totalDeduction(totalDeduction)
                .netPay(netPay)
                .calculationDetails(details)
                .build();
    }

    /**
     * 연장수당 지급 가능 여부 판정
     */
    private boolean canPayOvertimeAllowance(OvertimeConditions conditions) {
        boolean isMandatory = conditions.getTotalEmployees() >= 5;
        boolean meetsCondition = Boolean.TRUE.equals(conditions.getExceedsDailyLimit())
                || Boolean.TRUE.equals(conditions.getExceedsWeeklyLimit());

        if (isMandatory) {
            return meetsCondition;
        } else {
            return Boolean.TRUE.equals(conditions.getAutoPayOvertime()) && meetsCondition;
        }
    }

    /**
     * 야간수당 지급 가능 여부 판정
     */
    private boolean canPayNightAllowance(OvertimeConditions conditions) {
        boolean isMandatory = conditions.getTotalEmployees() >= 5;
        boolean hasNightWork = Boolean.TRUE.equals(conditions.getHasNightWork());

        if (isMandatory) {
            return hasNightWork;
        } else {
            return Boolean.TRUE.equals(conditions.getAutoPayNight()) && hasNightWork;
        }
    }

    /**
     * 휴일수당 지급 가능 여부 판정
     */
    private boolean canPayHolidayAllowance(OvertimeConditions conditions) {
        boolean isMandatory = conditions.getTotalEmployees() >= 5;
        boolean hasHolidayWork = Boolean.TRUE.equals(conditions.getHasHolidayWork());

        if (isMandatory) {
            return hasHolidayWork;
        } else {
            return Boolean.TRUE.equals(conditions.getAutoPayHoliday()) && hasHolidayWork;
        }
    }

    /**
     * 일용직 4대보험 계산
     */
    private InsuranceResult calculateDailyInsurance(PayrollCalculationInput input, BigDecimal taxableIncome) {
        InsuranceEligibility eligibility = input.getInsurance();
        InsuranceRates rates = input.getRates();

        // 고용보험
        BigDecimal employmentIns = Boolean.TRUE.equals(eligibility.getEmploymentInsurance())
                ? taxableIncome.multiply(rates.getEmploymentInsurance()).setScale(0, RoundingMode.FLOOR)
                : BigDecimal.ZERO;

        // 월 근로일수 및 월 소득 기준 판정
        Integer monthlyDays = input.getMonthlyWorkDaysAccumulated() != null ? input.getMonthlyWorkDaysAccumulated() : 0;
        BigDecimal monthlyIncome = input.getMonthlyEstimatedIncome() != null ? input.getMonthlyEstimatedIncome() : BigDecimal.ZERO;

        boolean healthApplicable = Boolean.TRUE.equals(eligibility.getHealthInsurance()) && monthlyDays >= 8;
        boolean pensionApplicable = Boolean.TRUE.equals(eligibility.getNationalPension())
                && (monthlyDays >= 8 || monthlyIncome.compareTo(new BigDecimal("2200000")) >= 0);

        // 건강보험 및 장기요양
        BigDecimal healthIns = healthApplicable
                ? monthlyIncome.multiply(rates.getHealthInsurance()).setScale(0, RoundingMode.FLOOR)
                : BigDecimal.ZERO;
        BigDecimal longTermCareIns = healthApplicable
                ? healthIns.multiply(rates.getLongTermCare()).setScale(0, RoundingMode.FLOOR)
                : BigDecimal.ZERO;

        // 국민연금
        BigDecimal nationalPension = pensionApplicable
                ? monthlyIncome.multiply(rates.getNationalPension()).setScale(0, RoundingMode.FLOOR)
                : BigDecimal.ZERO;

        return new InsuranceResult(employmentIns, healthIns, longTermCareIns, nationalPension);
    }

    /**
     * 상용직 4대보험 계산
     */
    private InsuranceResult calculatePermanentInsurance(PayrollCalculationInput input, BigDecimal taxableIncome) {
        InsuranceRates rates = input.getRates();

        BigDecimal employmentIns = taxableIncome.multiply(rates.getEmploymentInsurance()).setScale(0, RoundingMode.FLOOR);
        BigDecimal healthIns = taxableIncome.multiply(rates.getHealthInsurance()).setScale(0, RoundingMode.FLOOR);
        BigDecimal longTermCareIns = healthIns.multiply(rates.getLongTermCare()).setScale(0, RoundingMode.FLOOR);
        BigDecimal nationalPension = taxableIncome.multiply(rates.getNationalPension()).setScale(0, RoundingMode.FLOOR);

        return new InsuranceResult(employmentIns, healthIns, longTermCareIns, nationalPension);
    }

    /**
     * 일용직 세금 계산 (일별 계산)
     */
    private TaxResult calculateDailyTax(PayrollCalculationInput input, BigDecimal totalPay) {
        // 일급 = 총 지급액 / 근로일수
        BigDecimal dailyPay = totalPay.divide(new BigDecimal(input.getWorkDays()), 0, RoundingMode.FLOOR);

        BigDecimal totalIncomeTax = BigDecimal.ZERO;
        BigDecimal totalResidentTax = BigDecimal.ZERO;

        // 각 근로일에 대해 세금 계산
        for (int i = 0; i < input.getWorkDays(); i++) {
            // 과세표준 = max(0, 일급 - 150,000)
            BigDecimal taxBase = dailyPay.subtract(new BigDecimal("150000")).max(BigDecimal.ZERO);

            // 산출세액 = 과세표준 × 6%
            BigDecimal calculatedTax = taxBase.multiply(new BigDecimal("0.06")).setScale(0, RoundingMode.FLOOR);

            // 세액공제 = 산출세액 × 55%
            BigDecimal taxCredit = calculatedTax.multiply(new BigDecimal("0.55")).setScale(0, RoundingMode.FLOOR);

            // 결정세액 = 산출세액 - 세액공제
            BigDecimal finalIncomeTax = calculatedTax.subtract(taxCredit);

            // 지방소득세 = 결정세액 × 10%
            BigDecimal residentTax = finalIncomeTax.multiply(new BigDecimal("0.10")).setScale(0, RoundingMode.FLOOR);

            // 총 세금
            BigDecimal dailyTax = finalIncomeTax.add(residentTax);

            // 1,000원 미만 면제
            if (dailyTax.compareTo(new BigDecimal("1000")) < 0) {
                dailyTax = BigDecimal.ZERO;
                finalIncomeTax = BigDecimal.ZERO;
                residentTax = BigDecimal.ZERO;
            }

            totalIncomeTax = totalIncomeTax.add(finalIncomeTax);
            totalResidentTax = totalResidentTax.add(residentTax);
        }

        return new TaxResult(totalIncomeTax, totalResidentTax);
    }

    /**
     * 상용직 세금 계산 (간이세액표 - 현재는 3.3% 적용)
     */
    private TaxResult calculatePermanentTax(PayrollCalculationInput input, BigDecimal taxableIncome) {
        // TODO: 간이세액표 적용 (현재는 단순 3.3%)
        BigDecimal incomeTax = taxableIncome.multiply(new BigDecimal("0.033")).setScale(0, RoundingMode.FLOOR);
        BigDecimal residentTax = incomeTax.multiply(new BigDecimal("0.10")).setScale(0, RoundingMode.FLOOR);

        return new TaxResult(incomeTax, residentTax);
    }

    /**
     * Null-safe BigDecimal 반환
     */
    private BigDecimal nvl(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    // ========== 내부 DTO 클래스 ==========

    /**
     * 보험 계산 결과
     */
    private static class InsuranceResult {
        private final BigDecimal employmentInsurance;
        private final BigDecimal healthInsurance;
        private final BigDecimal longTermCareInsurance;
        private final BigDecimal nationalPension;

        public InsuranceResult(BigDecimal employmentInsurance, BigDecimal healthInsurance,
                               BigDecimal longTermCareInsurance, BigDecimal nationalPension) {
            this.employmentInsurance = employmentInsurance;
            this.healthInsurance = healthInsurance;
            this.longTermCareInsurance = longTermCareInsurance;
            this.nationalPension = nationalPension;
        }

        public BigDecimal getEmploymentInsurance() { return employmentInsurance; }
        public BigDecimal getHealthInsurance() { return healthInsurance; }
        public BigDecimal getLongTermCareInsurance() { return longTermCareInsurance; }
        public BigDecimal getNationalPension() { return nationalPension; }

        public BigDecimal getTotal() {
            return employmentInsurance.add(healthInsurance).add(longTermCareInsurance).add(nationalPension);
        }
    }

    /**
     * 세금 계산 결과
     */
    private static class TaxResult {
        private final BigDecimal incomeTax;
        private final BigDecimal residentTax;

        public TaxResult(BigDecimal incomeTax, BigDecimal residentTax) {
            this.incomeTax = incomeTax;
            this.residentTax = residentTax;
        }

        public BigDecimal getIncomeTax() { return incomeTax; }
        public BigDecimal getResidentTax() { return residentTax; }

        public BigDecimal getTotal() {
            return incomeTax.add(residentTax);
        }
    }
}