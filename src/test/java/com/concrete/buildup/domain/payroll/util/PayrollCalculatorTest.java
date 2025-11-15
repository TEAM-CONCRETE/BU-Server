package com.concrete.buildup.domain.payroll.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
}