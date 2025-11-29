package com.concrete.buildup.domain.payroll.util;

import com.concrete.buildup.domain.contract.enums.EmpType;
import com.concrete.buildup.domain.contract.enums.PayPeriod;
import com.concrete.buildup.domain.payroll.dto.InsuranceEligibility;
import com.concrete.buildup.domain.payroll.dto.InsuranceRates;
import com.concrete.buildup.domain.payroll.dto.OvertimeConditions;
import com.concrete.buildup.domain.payroll.dto.PayrollCalculationInput;
import com.concrete.buildup.domain.payroll.dto.PayrollCalculationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PayrollCalculator 단위 테스트
 *
 * 급여 계산 로직의 정확성을 검증합니다.
 */
@DisplayName("급여 계산기 테스트")
class PayrollCalculatorTest {

    private PayrollCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new PayrollCalculator();
    }

    @Test
    @DisplayName("기본급 계산 - 정상 케이스")
    void calculateBasePay_Success() {
        // given
        BigDecimal hourlyRate = new BigDecimal("10000"); // 시급 10,000원
        BigDecimal workHours = new BigDecimal("8"); // 8시간 근무

        // when
        BigDecimal basePay = calculator.calculateBasePay(hourlyRate, workHours);

        // then
        assertThat(basePay).isEqualByComparingTo(new BigDecimal("80000.00")); // 80,000원
    }

    @Test
    @DisplayName("기본급 계산 - null 입력 시 0 반환")
    void calculateBasePay_NullInput_ReturnsZero() {
        // when & then
        assertThat(calculator.calculateBasePay(null, new BigDecimal("8")))
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(calculator.calculateBasePay(new BigDecimal("10000"), null))
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("야간근로 수당 계산 - 50% 가산")
    void calculateNightWorkPay_Success() {
        // given
        BigDecimal hourlyRate = new BigDecimal("10000"); // 시급 10,000원
        BigDecimal nightHours = new BigDecimal("4"); // 야간 4시간

        // when
        BigDecimal nightPay = calculator.calculateNightWorkPay(hourlyRate, nightHours);

        // then
        // 10,000 * 1.5 * 4 = 60,000원
        assertThat(nightPay).isEqualByComparingTo(new BigDecimal("60000.00"));
    }

    @Test
    @DisplayName("연장근로 수당 계산 - 50% 가산")
    void calculateOvertimePay_Success() {
        // given
        BigDecimal hourlyRate = new BigDecimal("10000"); // 시급 10,000원
        BigDecimal overtimeHours = new BigDecimal("2"); // 연장 2시간

        // when
        BigDecimal overtimePay = calculator.calculateOvertimePay(hourlyRate, overtimeHours);

        // then
        // 10,000 * 1.5 * 2 = 30,000원
        assertThat(overtimePay).isEqualByComparingTo(new BigDecimal("30000.00"));
    }

    @Test
    @DisplayName("휴일근로 수당 계산 - 8시간 이내 (50% 가산)")
    void calculateHolidayPay_Within8Hours() {
        // given
        BigDecimal hourlyRate = new BigDecimal("10000");
        BigDecimal holidayHours = new BigDecimal("6"); // 6시간

        // when
        BigDecimal holidayPay = calculator.calculateHolidayPay(hourlyRate, holidayHours);

        // then
        // 10,000 * 1.5 * 6 = 90,000원
        assertThat(holidayPay).isEqualByComparingTo(new BigDecimal("90000.00"));
    }

    @Test
    @DisplayName("휴일근로 수당 계산 - 8시간 초과 (8시간까지 50%, 초과분 100% 가산)")
    void calculateHolidayPay_Over8Hours() {
        // given
        BigDecimal hourlyRate = new BigDecimal("10000");
        BigDecimal holidayHours = new BigDecimal("10"); // 10시간

        // when
        BigDecimal holidayPay = calculator.calculateHolidayPay(hourlyRate, holidayHours);

        // then
        // 8시간: 10,000 * 1.5 * 8 = 120,000
        // 2시간: 10,000 * 2.0 * 2 = 40,000
        // 합계: 160,000원
        assertThat(holidayPay).isEqualByComparingTo(new BigDecimal("160000.00"));
    }

    @Test
    @DisplayName("주휴수당 계산 - 주 15시간 이상 근무")
    void calculateWeeklyHolidayPay_Over15Hours() {
        // given
        BigDecimal hourlyRate = new BigDecimal("10000");
        BigDecimal weeklyWorkHours = new BigDecimal("40"); // 주 40시간

        // when
        BigDecimal weeklyHolidayPay = calculator.calculateWeeklyHolidayPay(hourlyRate, weeklyWorkHours);

        // then
        // (40 / 40) * 8 * 10,000 = 80,000원
        assertThat(weeklyHolidayPay).isEqualByComparingTo(new BigDecimal("80000.00"));
    }

    @Test
    @DisplayName("주휴수당 계산 - 주 15시간 미만이면 0원")
    void calculateWeeklyHolidayPay_Under15Hours_ReturnsZero() {
        // given
        BigDecimal hourlyRate = new BigDecimal("10000");
        BigDecimal weeklyWorkHours = new BigDecimal("10"); // 주 10시간

        // when
        BigDecimal weeklyHolidayPay = calculator.calculateWeeklyHolidayPay(hourlyRate, weeklyWorkHours);

        // then
        assertThat(weeklyHolidayPay).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("소득세 계산 - 3.3%")
    void calculateIncomeTax_Success() {
        // given
        BigDecimal taxableIncome = new BigDecimal("1000000"); // 100만원

        // when
        BigDecimal incomeTax = calculator.calculateIncomeTax(taxableIncome);

        // then
        // 1,000,000 * 0.033 = 33,000원
        assertThat(incomeTax).isEqualByComparingTo(new BigDecimal("33000"));
    }

    @Test
    @DisplayName("소득세 계산 - 음수 또는 0이면 0원")
    void calculateIncomeTax_ZeroOrNegative_ReturnsZero() {
        // when & then
        assertThat(calculator.calculateIncomeTax(BigDecimal.ZERO))
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(calculator.calculateIncomeTax(new BigDecimal("-1000")))
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("주민세 계산 - 0.3%")
    void calculateResidentTax_Success() {
        // given
        BigDecimal taxableIncome = new BigDecimal("1000000"); // 100만원

        // when
        BigDecimal residentTax = calculator.calculateResidentTax(taxableIncome);

        // then
        // 1,000,000 * 0.003 = 3,000원
        assertThat(residentTax).isEqualByComparingTo(new BigDecimal("3000"));
    }

    @Test
    @DisplayName("국민연금 계산 - 4.5%")
    void calculateNationalPension_Success() {
        // given
        BigDecimal baseSalary = new BigDecimal("2000000"); // 200만원
        boolean isApplicable = true;

        // when
        BigDecimal nationalPension = calculator.calculateNationalPension(baseSalary, isApplicable);

        // then
        // 2,000,000 * 0.045 = 90,000원
        assertThat(nationalPension).isEqualByComparingTo(new BigDecimal("90000"));
    }

    @Test
    @DisplayName("국민연금 계산 - 미적용 시 0원")
    void calculateNationalPension_NotApplicable_ReturnsZero() {
        // given
        BigDecimal baseSalary = new BigDecimal("2000000");
        boolean isApplicable = false;

        // when
        BigDecimal nationalPension = calculator.calculateNationalPension(baseSalary, isApplicable);

        // then
        assertThat(nationalPension).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("건강보험 계산 - 3.545%")
    void calculateHealthInsurance_Success() {
        // given
        BigDecimal baseSalary = new BigDecimal("2000000"); // 200만원
        boolean isApplicable = true;

        // when
        BigDecimal healthInsurance = calculator.calculateHealthInsurance(baseSalary, isApplicable);

        // then
        // 2,000,000 * 0.03545 = 70,900원
        assertThat(healthInsurance).isEqualByComparingTo(new BigDecimal("70900"));
    }

    @Test
    @DisplayName("산재보험 계산 - 0.7%")
    void calculateWorkersCompInsurance_Success() {
        // given
        BigDecimal baseSalary = new BigDecimal("2000000"); // 200만원
        boolean isApplicable = true;

        // when
        BigDecimal workersCompInsurance = calculator.calculateWorkersCompInsurance(baseSalary, isApplicable);

        // then
        // 2,000,000 * 0.007 = 14,000원
        assertThat(workersCompInsurance).isEqualByComparingTo(new BigDecimal("14000"));
    }

    @Test
    @DisplayName("산재보험 계산 - 미적용 시 0원")
    void calculateWorkersCompInsurance_NotApplicable_ReturnsZero() {
        // given
        BigDecimal baseSalary = new BigDecimal("2000000");
        boolean isApplicable = false;

        // when
        BigDecimal workersCompInsurance = calculator.calculateWorkersCompInsurance(baseSalary, isApplicable);

        // then
        assertThat(workersCompInsurance).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("고용보험 계산 - 0.9%")
    void calculateEmploymentInsurance_Success() {
        // given
        BigDecimal baseSalary = new BigDecimal("2000000"); // 200만원
        boolean isApplicable = true;

        // when
        BigDecimal employmentInsurance = calculator.calculateEmploymentInsurance(baseSalary, isApplicable);

        // then
        // 2,000,000 * 0.009 = 18,000원
        assertThat(employmentInsurance).isEqualByComparingTo(new BigDecimal("18000"));
    }

    @Test
    @DisplayName("실수령액 계산 - 전체 공제 적용")
    void calculateNetPay_Success() {
        // given
        BigDecimal totalPay = new BigDecimal("2000000"); // 총 지급액 200만원
        BigDecimal incomeTax = new BigDecimal("66000"); // 소득세
        BigDecimal residentTax = new BigDecimal("6000"); // 주민세
        BigDecimal nationalPension = new BigDecimal("90000"); // 국민연금
        BigDecimal healthInsurance = new BigDecimal("70900"); // 건강보험
        BigDecimal workersCompInsurance = new BigDecimal("14000"); // 산재보험
        BigDecimal employmentInsurance = new BigDecimal("18000"); // 고용보험

        // when
        BigDecimal netPay = calculator.calculateNetPay(
                totalPay, incomeTax, residentTax,
                nationalPension, healthInsurance, workersCompInsurance, employmentInsurance
        );

        // then
        // 2,000,000 - (66,000 + 6,000 + 90,000 + 70,900 + 14,000 + 18,000) = 1,735,100원
        assertThat(netPay).isEqualByComparingTo(new BigDecimal("1735100"));
    }

    @Test
    @DisplayName("실수령액 계산 - 일부 공제만 적용")
    void calculateNetPay_PartialDeductions() {
        // given
        BigDecimal totalPay = new BigDecimal("1000000");
        BigDecimal incomeTax = new BigDecimal("33000");
        BigDecimal residentTax = new BigDecimal("3000");
        BigDecimal nationalPension = BigDecimal.ZERO; // 미적용
        BigDecimal healthInsurance = BigDecimal.ZERO; // 미적용
        BigDecimal workersCompInsurance = BigDecimal.ZERO; // 미적용
        BigDecimal employmentInsurance = BigDecimal.ZERO; // 미적용

        // when
        BigDecimal netPay = calculator.calculateNetPay(
                totalPay, incomeTax, residentTax,
                nationalPension, healthInsurance, workersCompInsurance, employmentInsurance
        );

        // then
        // 1,000,000 - (33,000 + 3,000) = 964,000원
        assertThat(netPay).isEqualByComparingTo(new BigDecimal("964000"));
    }

    @Test
    @DisplayName("실수령액 계산 - null 공제 항목 처리")
    void calculateNetPay_NullDeductions() {
        // given
        BigDecimal totalPay = new BigDecimal("1000000");

        // when
        BigDecimal netPay = calculator.calculateNetPay(
                totalPay, null, null, null, null, null, null
        );

        // then
        assertThat(netPay).isEqualByComparingTo(new BigDecimal("1000000"));
    }

    @Test
    @DisplayName("실제 급여 계산 시나리오 - 일용직 일급")
    void realWorldScenario_DailyWorker() {
        // given - 일용직 근로자, 하루 8시간 근무, 시급 12,000원
        BigDecimal hourlyRate = new BigDecimal("12000");
        BigDecimal workHours = new BigDecimal("8");

        // when - 기본급 계산
        BigDecimal basePay = calculator.calculateBasePay(hourlyRate, workHours);
        // 12,000 * 8 = 96,000원

        // when - 세금 계산
        BigDecimal incomeTax = calculator.calculateIncomeTax(basePay);
        BigDecimal residentTax = calculator.calculateResidentTax(basePay);

        // when - 4대보험 미적용
        BigDecimal netPay = calculator.calculateNetPay(
                basePay, incomeTax, residentTax,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        );

        // then
        assertThat(basePay).isEqualByComparingTo(new BigDecimal("96000.00"));
        assertThat(incomeTax).isEqualByComparingTo(new BigDecimal("3168")); // 96,000 * 0.033
        assertThat(residentTax).isEqualByComparingTo(new BigDecimal("288")); // 96,000 * 0.003
        assertThat(netPay).isEqualByComparingTo(new BigDecimal("92544")); // 96,000 - 3,168 - 288
    }

    @Test
    @DisplayName("실제 급여 계산 시나리오 - 상용직 월급 (4대보험 적용)")
    void realWorldScenario_PermanentWorker() {
        // given - 상용직, 월 209시간 근무, 시급 15,000원
        BigDecimal hourlyRate = new BigDecimal("15000");
        BigDecimal workHours = new BigDecimal("209");

        // when - 기본급 계산
        BigDecimal basePay = calculator.calculateBasePay(hourlyRate, workHours);
        // 15,000 * 209 = 3,135,000원

        // when - 세금 및 4대보험 계산
        BigDecimal incomeTax = calculator.calculateIncomeTax(basePay);
        BigDecimal residentTax = calculator.calculateResidentTax(basePay);
        BigDecimal nationalPension = calculator.calculateNationalPension(basePay, true);
        BigDecimal healthInsurance = calculator.calculateHealthInsurance(basePay, true);
        BigDecimal workersCompInsurance = calculator.calculateWorkersCompInsurance(basePay, true);
        BigDecimal employmentInsurance = calculator.calculateEmploymentInsurance(basePay, true);

        BigDecimal netPay = calculator.calculateNetPay(
                basePay, incomeTax, residentTax,
                nationalPension, healthInsurance, workersCompInsurance, employmentInsurance
        );

        // then
        assertThat(basePay).isEqualByComparingTo(new BigDecimal("3135000.00"));
        assertThat(incomeTax).isEqualByComparingTo(new BigDecimal("103455")); // 3,135,000 * 0.033
        assertThat(residentTax).isEqualByComparingTo(new BigDecimal("9405")); // 3,135,000 * 0.003
        assertThat(nationalPension).isEqualByComparingTo(new BigDecimal("141075")); // 3,135,000 * 0.045
        assertThat(healthInsurance).isEqualByComparingTo(new BigDecimal("111135")); // 3,135,000 * 0.03545
        assertThat(workersCompInsurance).isEqualByComparingTo(new BigDecimal("21945")); // 3,135,000 * 0.007
        assertThat(employmentInsurance).isEqualByComparingTo(new BigDecimal("28215")); // 3,135,000 * 0.009

        // 실수령액: 3,135,000 - (103,455 + 9,405 + 141,075 + 111,135 + 21,945 + 28,215) = 2,719,770원
        assertThat(netPay).isEqualByComparingTo(new BigDecimal("2719770"));
    }

    // ========================================
    // v1.1 통합 계산 테스트
    // ========================================

    @Nested
    @DisplayName("v1.1 통합 급여 계산 테스트")
    class CalculateV1_1Test {

        @Test
        @DisplayName("일용직 일급 계산 - 기본 케이스 (가산수당 없음)")
        void calculateDaily_Basic() {
            // given - 일용직, 시급 12,000원, 8시간 근무, 가산수당 없음
            PayrollCalculationInput input = PayrollCalculationInput.builder()
                    .empType(EmpType.DAILY)
                    .payPeriod(PayPeriod.DAILY)
                    .hourlyRate(new BigDecimal("12000"))
                    .dailyWorkHours(new BigDecimal("8"))
                    .workDays(1)
                    .overtimeHours(BigDecimal.ZERO)
                    .nightHours(BigDecimal.ZERO)
                    .holidayHours(BigDecimal.ZERO)
                    .weeklyHolidayEligible(false)
                    .insurance(InsuranceEligibility.builder()
                            .employmentInsurance(false)
                            .healthInsurance(false)
                            .nationalPension(false)
                            .build())
                    .dependents(0)
                    .rates(InsuranceRates.defaultRates2025())
                    .overtimeConditions(OvertimeConditions.builder()
                            .totalEmployees(5)
                            .autoPayOvertime(true)
                            .autoPayNight(true)
                            .autoPayHoliday(true)
                            .exceedsDailyLimit(false)
                            .exceedsWeeklyLimit(false)
                            .hasNightWork(false)
                            .hasHolidayWork(false)
                            .build())
                    .monthlyWorkDaysAccumulated(1)  // 일용직 일급: workDays 값 사용
                    .monthlyEstimatedIncome(BigDecimal.ZERO)
                    .build();

            // when
            PayrollCalculationResult result = calculator.calculate(input);

            // then
            // 기본급: 12,000 * 8 = 96,000원
            assertThat(result.getBasePay()).isEqualByComparingTo(new BigDecimal("96000"));
            assertThat(result.getOvertimePay()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getNightPay()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getHolidayPay()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getWeeklyHolidayPay()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getTotalPay()).isEqualByComparingTo(new BigDecimal("96000"));

            // 일용직 일별 세금: (96,000 - 150,000) < 0 이므로 0원
            assertThat(result.getIncomeTax()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getResidentTax()).isEqualByComparingTo(BigDecimal.ZERO);

            // 4대보험: 미가입
            assertThat(result.getEmploymentInsurance()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getHealthInsurance()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getNationalPension()).isEqualByComparingTo(BigDecimal.ZERO);

            // 실수령액: 96,000원
            assertThat(result.getNetPay()).isEqualByComparingTo(new BigDecimal("96000"));
        }

        @Test
        @DisplayName("일용직 일급 계산 - 15만원 초과 시 세금 발생")
        void calculateDaily_WithTax() {
            // given - 일용직, 시급 20,000원, 10시간 근무 (200,000원)
            PayrollCalculationInput input = PayrollCalculationInput.builder()
                    .empType(EmpType.DAILY)
                    .payPeriod(PayPeriod.DAILY)
                    .hourlyRate(new BigDecimal("20000"))
                    .dailyWorkHours(new BigDecimal("10"))
                    .workDays(1)
                    .overtimeHours(BigDecimal.ZERO)
                    .nightHours(BigDecimal.ZERO)
                    .holidayHours(BigDecimal.ZERO)
                    .weeklyHolidayEligible(false)
                    .insurance(InsuranceEligibility.builder()
                            .employmentInsurance(false)
                            .healthInsurance(false)
                            .nationalPension(false)
                            .build())
                    .dependents(0)
                    .rates(InsuranceRates.defaultRates2025())
                    .overtimeConditions(OvertimeConditions.builder()
                            .totalEmployees(5)
                            .autoPayOvertime(true)
                            .autoPayNight(true)
                            .autoPayHoliday(true)
                            .exceedsDailyLimit(false)
                            .exceedsWeeklyLimit(false)
                            .hasNightWork(false)
                            .hasHolidayWork(false)
                            .build())
                    .monthlyWorkDaysAccumulated(1)  // 일용직 일급: workDays 값 사용
                    .monthlyEstimatedIncome(BigDecimal.ZERO)
                    .build();

            // when
            PayrollCalculationResult result = calculator.calculate(input);

            // then
            // 기본급: 20,000 * 10 = 200,000원
            assertThat(result.getBasePay()).isEqualByComparingTo(new BigDecimal("200000"));
            assertThat(result.getTotalPay()).isEqualByComparingTo(new BigDecimal("200000"));

            // 일용직 세금 계산:
            // 과세표준 = 200,000 - 150,000 = 50,000원
            // 산출세액 = 50,000 × 6% = 3,000원
            // 세액공제 = 3,000 × 55% = 1,650원
            // 결정세액 (소득세) = 3,000 - 1,650 = 1,350원
            // 지방소득세 = 1,350 × 10% = 135원
            // 총 세금 = 1,350 + 135 = 1,485원 (1,000원 이상이므로 과세)
            assertThat(result.getIncomeTax()).isEqualByComparingTo(new BigDecimal("1350"));
            assertThat(result.getResidentTax()).isEqualByComparingTo(new BigDecimal("135"));

            // 실수령액: 200,000 - 1,350 - 135 = 198,515원
            assertThat(result.getNetPay()).isEqualByComparingTo(new BigDecimal("198515"));
        }

        @Test
        @DisplayName("일용직 일급 계산 - 가산수당 포함 (연장 + 야간)")
        void calculateDaily_WithOvertimeAndNight() {
            // given - 일용직, 시급 12,000원, 통상 8시간 + 연장 2시간 + 야간 3시간
            PayrollCalculationInput input = PayrollCalculationInput.builder()
                    .empType(EmpType.DAILY)
                    .payPeriod(PayPeriod.DAILY)
                    .hourlyRate(new BigDecimal("12000"))
                    .dailyWorkHours(new BigDecimal("8"))
                    .workDays(1)
                    .overtimeHours(new BigDecimal("2"))  // 연장 2시간
                    .nightHours(new BigDecimal("3"))      // 야간 3시간
                    .holidayHours(BigDecimal.ZERO)
                    .weeklyHolidayEligible(false)
                    .insurance(InsuranceEligibility.builder()
                            .employmentInsurance(false)
                            .healthInsurance(false)
                            .nationalPension(false)
                            .build())
                    .dependents(0)
                    .rates(InsuranceRates.defaultRates2025())
                    .overtimeConditions(OvertimeConditions.builder()
                            .totalEmployees(5)  // 5인 이상 사업장
                            .autoPayOvertime(true)
                            .autoPayNight(true)
                            .autoPayHoliday(true)
                            .exceedsDailyLimit(true)  // 일 8시간 초과
                            .exceedsWeeklyLimit(false)
                            .hasNightWork(true)
                            .hasHolidayWork(false)
                            .build())
                    .monthlyWorkDaysAccumulated(1)  // 일용직 일급: workDays 값 사용
                    .monthlyEstimatedIncome(BigDecimal.ZERO)
                    .build();

            // when
            PayrollCalculationResult result = calculator.calculate(input);

            // then
            // 기본급: 12,000 * 8 = 96,000원
            assertThat(result.getBasePay()).isEqualByComparingTo(new BigDecimal("96000"));

            // 연장수당: 12,000 * 2 * 1.5 = 36,000원
            assertThat(result.getOvertimePay()).isEqualByComparingTo(new BigDecimal("36000"));

            // 야간수당: 12,000 * 3 * 0.5 = 18,000원
            assertThat(result.getNightPay()).isEqualByComparingTo(new BigDecimal("18000"));

            // 총 지급액: 96,000 + 36,000 + 18,000 = 150,000원
            assertThat(result.getTotalPay()).isEqualByComparingTo(new BigDecimal("150000"));

            // 세금: 150,000원 = 공제 기준선이므로 0원
            assertThat(result.getIncomeTax()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getResidentTax()).isEqualByComparingTo(BigDecimal.ZERO);

            // 실수령액: 150,000원
            assertThat(result.getNetPay()).isEqualByComparingTo(new BigDecimal("150000"));
        }

        @Test
        @DisplayName("일용직 일급 계산 - 5인 미만 사업장, 가산수당 자율 지급")
        void calculateDaily_SmallBusiness_AutoPay() {
            // given - 4인 사업장, 가산수당 자율 지급
            PayrollCalculationInput input = PayrollCalculationInput.builder()
                    .empType(EmpType.DAILY)
                    .payPeriod(PayPeriod.DAILY)
                    .hourlyRate(new BigDecimal("12000"))
                    .dailyWorkHours(new BigDecimal("8"))
                    .workDays(1)
                    .overtimeHours(new BigDecimal("2"))
                    .nightHours(new BigDecimal("2"))
                    .holidayHours(BigDecimal.ZERO)
                    .weeklyHolidayEligible(false)
                    .insurance(InsuranceEligibility.builder()
                            .employmentInsurance(false)
                            .healthInsurance(false)
                            .nationalPension(false)
                            .build())
                    .dependents(0)
                    .rates(InsuranceRates.defaultRates2025())
                    .overtimeConditions(OvertimeConditions.builder()
                            .totalEmployees(4)  // 5인 미만 사업장
                            .autoPayOvertime(true)  // 연장수당 자율 지급
                            .autoPayNight(true)     // 야간수당 자율 지급
                            .autoPayHoliday(true)
                            .exceedsDailyLimit(true)
                            .exceedsWeeklyLimit(false)
                            .hasNightWork(true)
                            .hasHolidayWork(false)
                            .build())
                    .monthlyWorkDaysAccumulated(1)  // 일용직 일급: workDays 값 사용
                    .monthlyEstimatedIncome(BigDecimal.ZERO)
                    .build();

            // when
            PayrollCalculationResult result = calculator.calculate(input);

            // then - 5인 미만이지만 자율 지급하므로 가산수당 지급
            assertThat(result.getBasePay()).isEqualByComparingTo(new BigDecimal("96000"));
            assertThat(result.getOvertimePay()).isEqualByComparingTo(new BigDecimal("36000"));  // 12,000 * 2 * 1.5
            assertThat(result.getNightPay()).isEqualByComparingTo(new BigDecimal("12000"));     // 12,000 * 2 * 0.5
            assertThat(result.getTotalPay()).isEqualByComparingTo(new BigDecimal("144000"));
        }

        @Test
        @DisplayName("일용직 일급 계산 - 5인 미만 사업장, 가산수당 미지급")
        void calculateDaily_SmallBusiness_NoPay() {
            // given - 4인 사업장, 가산수당 미지급
            PayrollCalculationInput input = PayrollCalculationInput.builder()
                    .empType(EmpType.DAILY)
                    .payPeriod(PayPeriod.DAILY)
                    .hourlyRate(new BigDecimal("12000"))
                    .dailyWorkHours(new BigDecimal("8"))
                    .workDays(1)
                    .overtimeHours(new BigDecimal("2"))
                    .nightHours(new BigDecimal("2"))
                    .holidayHours(BigDecimal.ZERO)
                    .weeklyHolidayEligible(false)
                    .insurance(InsuranceEligibility.builder()
                            .employmentInsurance(false)
                            .healthInsurance(false)
                            .nationalPension(false)
                            .build())
                    .dependents(0)
                    .rates(InsuranceRates.defaultRates2025())
                    .overtimeConditions(OvertimeConditions.builder()
                            .totalEmployees(4)  // 5인 미만 사업장
                            .autoPayOvertime(false)  // 연장수당 미지급
                            .autoPayNight(false)     // 야간수당 미지급
                            .autoPayHoliday(false)
                            .exceedsDailyLimit(true)
                            .exceedsWeeklyLimit(false)
                            .hasNightWork(true)
                            .hasHolidayWork(false)
                            .build())
                    .monthlyWorkDaysAccumulated(1)  // 일용직 일급: workDays 값 사용
                    .monthlyEstimatedIncome(BigDecimal.ZERO)
                    .build();

            // when
            PayrollCalculationResult result = calculator.calculate(input);

            // then - 5인 미만이고 자율 미지급하므로 가산수당 0원
            assertThat(result.getBasePay()).isEqualByComparingTo(new BigDecimal("96000"));
            assertThat(result.getOvertimePay()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getNightPay()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getTotalPay()).isEqualByComparingTo(new BigDecimal("96000"));
        }

        @Test
        @DisplayName("일용직 월급 계산 - 4대보험 가입 조건 충족 (월 8일, 월소득 220만원)")
        void calculateDaily_Monthly_WithInsurance() {
            // given - 일용직 월급, 월 10일 근무, 시급 25,000원 (월 200만원 예상)
            PayrollCalculationInput input = PayrollCalculationInput.builder()
                    .empType(EmpType.DAILY)
                    .payPeriod(PayPeriod.MONTHLY)
                    .hourlyRate(new BigDecimal("25000"))
                    .dailyWorkHours(new BigDecimal("8"))
                    .workDays(10)  // 월 10일 근무
                    .overtimeHours(BigDecimal.ZERO)
                    .nightHours(BigDecimal.ZERO)
                    .holidayHours(BigDecimal.ZERO)
                    .weeklyHolidayEligible(false)
                    .insurance(InsuranceEligibility.builder()
                            .employmentInsurance(true)
                            .healthInsurance(true)
                            .nationalPension(true)
                            .build())
                    .dependents(0)
                    .rates(InsuranceRates.defaultRates2025())
                    .overtimeConditions(OvertimeConditions.builder()
                            .totalEmployees(5)
                            .autoPayOvertime(true)
                            .autoPayNight(true)
                            .autoPayHoliday(true)
                            .exceedsDailyLimit(false)
                            .exceedsWeeklyLimit(false)
                            .hasNightWork(false)
                            .hasHolidayWork(false)
                            .build())
                    .monthlyWorkDaysAccumulated(10)  // 월 10일 근무
                    .monthlyEstimatedIncome(new BigDecimal("2500000"))  // 월소득 250만원 (220만원 초과)
                    .build();

            // when
            PayrollCalculationResult result = calculator.calculate(input);

            // then
            // 기본급: 25,000 * 8 * 10 = 2,000,000원
            assertThat(result.getBasePay()).isEqualByComparingTo(new BigDecimal("2000000"));
            assertThat(result.getTotalPay()).isEqualByComparingTo(new BigDecimal("2000000"));

            // 4대보험: 월 8일 이상 + 월소득 220만원 이상 조건 충족
            // 고용보험: 2,000,000 * 0.009 = 18,000원
            assertThat(result.getEmploymentInsurance()).isEqualByComparingTo(new BigDecimal("18000"));
            // 건강보험: 2,000,000 * 0.03545 = 70,900원
            assertThat(result.getHealthInsurance()).isEqualByComparingTo(new BigDecimal("70900"));
            // 장기요양: 70,900 * 0.1281 = 9,082.29 → 9,080원 (10원 단위 절사)
            assertThat(result.getLongTermCareInsurance()).isEqualByComparingTo(new BigDecimal("9080"));
            // 국민연금: 2,000,000 * 0.045 = 90,000원
            assertThat(result.getNationalPension()).isEqualByComparingTo(new BigDecimal("90000"));

            // 총 공제액 계산 확인
            BigDecimal totalDeduction = result.calculateTotalDeduction();
            assertThat(totalDeduction.compareTo(BigDecimal.ZERO)).isGreaterThan(0);
        }

        @Test
        @DisplayName("일용직 월급 계산 - 4대보험 미가입 (월 8일 미만)")
        void calculateDaily_Monthly_NoInsurance_LessThan8Days() {
            // given - 일용직 월급, 월 5일 근무
            PayrollCalculationInput input = PayrollCalculationInput.builder()
                    .empType(EmpType.DAILY)
                    .payPeriod(PayPeriod.MONTHLY)
                    .hourlyRate(new BigDecimal("30000"))
                    .dailyWorkHours(new BigDecimal("8"))
                    .workDays(5)  // 월 5일 근무 (8일 미만)
                    .overtimeHours(BigDecimal.ZERO)
                    .nightHours(BigDecimal.ZERO)
                    .holidayHours(BigDecimal.ZERO)
                    .weeklyHolidayEligible(false)
                    .insurance(InsuranceEligibility.builder()
                            .employmentInsurance(true)
                            .healthInsurance(true)
                            .nationalPension(true)
                            .build())
                    .dependents(0)
                    .rates(InsuranceRates.defaultRates2025())
                    .overtimeConditions(OvertimeConditions.builder()
                            .totalEmployees(5)
                            .autoPayOvertime(true)
                            .autoPayNight(true)
                            .autoPayHoliday(true)
                            .exceedsDailyLimit(false)
                            .exceedsWeeklyLimit(false)
                            .hasNightWork(false)
                            .hasHolidayWork(false)
                            .build())
                    .monthlyWorkDaysAccumulated(5)  // 월 5일 근무
                    .monthlyEstimatedIncome(new BigDecimal("3000000"))  // 월소득은 충분하지만
                    .build();

            // when
            PayrollCalculationResult result = calculator.calculate(input);

            // then - 월 8일 미만이므로 4대보험 미가입
            assertThat(result.getEmploymentInsurance()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getHealthInsurance()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getLongTermCareInsurance()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getNationalPension()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("상용직 월급 계산 - 4대보험 전체 적용")
        void calculatePermanent_Monthly_WithFullInsurance() {
            // given - 상용직, 월 209시간, 시급 15,000원
            PayrollCalculationInput input = PayrollCalculationInput.builder()
                    .empType(EmpType.PERMANENT)
                    .payPeriod(PayPeriod.MONTHLY)
                    .hourlyRate(new BigDecimal("15000"))
                    .dailyWorkHours(new BigDecimal("8"))
                    .workDays(20)
                    .overtimeHours(BigDecimal.ZERO)
                    .nightHours(BigDecimal.ZERO)
                    .holidayHours(BigDecimal.ZERO)
                    .weeklyHolidayEligible(true)
                    .insurance(InsuranceEligibility.builder()
                            .employmentInsurance(true)
                            .healthInsurance(true)
                            .nationalPension(true)
                            .build())
                    .dependents(1)  // 부양가족 1명
                    .rates(InsuranceRates.defaultRates2025())
                    .overtimeConditions(OvertimeConditions.builder()
                            .totalEmployees(10)
                            .autoPayOvertime(true)
                            .autoPayNight(true)
                            .autoPayHoliday(true)
                            .exceedsDailyLimit(false)
                            .exceedsWeeklyLimit(false)
                            .hasNightWork(false)
                            .hasHolidayWork(false)
                            .build())
                    .monthlyWorkDaysAccumulated(20)
                    .monthlyEstimatedIncome(new BigDecimal("3000000"))
                    .build();

            // when
            PayrollCalculationResult result = calculator.calculate(input);

            // then
            // 기본급: 15,000 * 8 * 20 = 2,400,000원
            assertThat(result.getBasePay()).isEqualByComparingTo(new BigDecimal("2400000"));

            // 주휴수당: 주 4회 * (15,000 * 8) = 480,000원
            assertThat(result.getWeeklyHolidayPay()).isEqualByComparingTo(new BigDecimal("480000"));

            // 총 지급액: 2,400,000 + 480,000 = 2,880,000원
            assertThat(result.getTotalPay()).isEqualByComparingTo(new BigDecimal("2880000"));

            // 상용직 세금: 간이세액표 (현재는 3.3% 적용)
            // 2,880,000 * 0.033 = 95,040원
            assertThat(result.getIncomeTax()).isEqualByComparingTo(new BigDecimal("95040"));
            // 주민세: 95,040 * 0.1 = 9,504원
            assertThat(result.getResidentTax()).isEqualByComparingTo(new BigDecimal("9504"));

            // 4대보험
            // 고용보험: 2,880,000 * 0.009 = 25,920원
            assertThat(result.getEmploymentInsurance()).isEqualByComparingTo(new BigDecimal("25920"));
            // 건강보험: 2,880,000 * 0.03545 = 102,096원
            assertThat(result.getHealthInsurance()).isEqualByComparingTo(new BigDecimal("102096"));
            // 장기요양: 102,096 * 0.1281 = 13,078.49 → 13,070원
            assertThat(result.getLongTermCareInsurance()).isEqualByComparingTo(new BigDecimal("13070"));
            // 국민연금: 2,880,000 * 0.045 = 129,600원
            assertThat(result.getNationalPension()).isEqualByComparingTo(new BigDecimal("129600"));

            // 실수령액 검증
            BigDecimal expectedDeduction = new BigDecimal("95040")
                    .add(new BigDecimal("9504"))
                    .add(new BigDecimal("25920"))
                    .add(new BigDecimal("102096"))
                    .add(new BigDecimal("13070"))
                    .add(new BigDecimal("129600"));
            BigDecimal expectedNetPay = new BigDecimal("2880000").subtract(expectedDeduction);
            assertThat(result.getNetPay()).isEqualByComparingTo(expectedNetPay);
        }

        @Test
        @DisplayName("휴일근로 + 야간근로 중복가산 계산")
        void calculateDaily_HolidayAndNight_OverlappingAllowance() {
            // given - 휴일에 야간근로 4시간
            PayrollCalculationInput input = PayrollCalculationInput.builder()
                    .empType(EmpType.DAILY)
                    .payPeriod(PayPeriod.DAILY)
                    .hourlyRate(new BigDecimal("10000"))
                    .dailyWorkHours(new BigDecimal("8"))
                    .workDays(1)
                    .overtimeHours(BigDecimal.ZERO)
                    .nightHours(new BigDecimal("4"))      // 야간 4시간
                    .holidayHours(new BigDecimal("4"))    // 휴일 4시간 (야간과 중복)
                    .weeklyHolidayEligible(false)
                    .insurance(InsuranceEligibility.builder()
                            .employmentInsurance(false)
                            .healthInsurance(false)
                            .nationalPension(false)
                            .build())
                    .dependents(0)
                    .rates(InsuranceRates.defaultRates2025())
                    .overtimeConditions(OvertimeConditions.builder()
                            .totalEmployees(5)
                            .autoPayOvertime(true)
                            .autoPayNight(true)
                            .autoPayHoliday(true)
                            .exceedsDailyLimit(false)
                            .exceedsWeeklyLimit(false)
                            .hasNightWork(true)
                            .hasHolidayWork(true)
                            .build())
                    .monthlyWorkDaysAccumulated(1)  // 일용직 일급: workDays 값 사용
                    .monthlyEstimatedIncome(BigDecimal.ZERO)
                    .build();

            // when
            PayrollCalculationResult result = calculator.calculate(input);

            // then
            // 기본급: 10,000 * 8 = 80,000원
            assertThat(result.getBasePay()).isEqualByComparingTo(new BigDecimal("80000"));

            // 휴일수당: 10,000 * 4 * 1.5 = 60,000원
            assertThat(result.getHolidayPay()).isEqualByComparingTo(new BigDecimal("60000"));

            // 야간수당: 10,000 * 4 * 0.5 = 20,000원 (휴일과 중복되는 시간에 추가)
            assertThat(result.getNightPay()).isEqualByComparingTo(new BigDecimal("20000"));

            // 총 지급액: 80,000 + 60,000 + 20,000 = 160,000원
            assertThat(result.getTotalPay()).isEqualByComparingTo(new BigDecimal("160000"));
        }
    }
}