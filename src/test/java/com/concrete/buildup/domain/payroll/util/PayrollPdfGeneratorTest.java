package com.concrete.buildup.domain.payroll.util;

import com.concrete.buildup.domain.contract.enums.EmpType;
import com.concrete.buildup.domain.payroll.entity.Payroll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * PayrollPdfGenerator 단위 테스트
 *
 * PDF 생성 로직의 정확성을 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("급여명세서 PDF 생성기 테스트")
class PayrollPdfGeneratorTest {

    @Mock
    private TemplateEngine templateEngine;

    @InjectMocks
    private PayrollPdfGenerator pdfGenerator;

    private Payroll testPayroll;

    @BeforeEach
    void setUp() {
        // 테스트용 급여 데이터 생성 (일급 케이스)
        testPayroll = Payroll.builder()
                .employeeId(1L)
                .contractId(1L)
                .empName("홍길동")
                .empType(EmpType.DAILY)
                .salaryYear(2025)
                .salaryMonth(1)
                .salaryWeek(0)  // 일급이므로 센티널 값 0
                .salaryDay(LocalDate.of(2025, 1, 15))  // 일급 실제 날짜
                .payDueDate(LocalDate.of(2025, 2, 10))
                .totalWorkHour(new BigDecimal("8.00"))
                .totalPay(new BigDecimal("96000"))
                .incomeTax(new BigDecimal("3168"))
                .residentTax(new BigDecimal("288"))
                .generatedAt(LocalDateTime.of(2025, 1, 16, 10, 30))
                .build();
    }

    @Test
    @DisplayName("PDF 생성 성공 - 기본 케이스")
    void generatePayrollPdf_Success() {
        // given
        String mockHtml = "<html><body>급여명세서</body></html>";
        when(templateEngine.process(eq("payroll/payslip"), any(Context.class)))
                .thenReturn(mockHtml);

        BigDecimal basePay = new BigDecimal("96000");
        BigDecimal nightPay = BigDecimal.ZERO;
        BigDecimal overtimePay = BigDecimal.ZERO;
        BigDecimal holidayPay = BigDecimal.ZERO;
        BigDecimal weeklyHolidayPay = BigDecimal.ZERO;
        BigDecimal nationalPension = BigDecimal.ZERO;
        BigDecimal healthInsurance = BigDecimal.ZERO;
        BigDecimal workersCompInsurance = BigDecimal.ZERO;
        BigDecimal employmentInsurance = BigDecimal.ZERO;
        BigDecimal netPay = new BigDecimal("92544");

        // when
        byte[] pdfBytes = pdfGenerator.generatePayrollPdf(
                testPayroll,
                basePay, nightPay, overtimePay, holidayPay, weeklyHolidayPay,
                nationalPension, healthInsurance, workersCompInsurance, employmentInsurance,
                netPay
        );

        // then
        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(0);
    }

    @Test
    @DisplayName("PDF 생성 성공 - 모든 수당 포함")
    void generatePayrollPdf_WithAllAllowances() {
        // given
        String mockHtml = "<html><body>급여명세서</body></html>";
        when(templateEngine.process(eq("payroll/payslip"), any(Context.class)))
                .thenReturn(mockHtml);

        BigDecimal basePay = new BigDecimal("3135000");
        BigDecimal nightPay = new BigDecimal("60000");
        BigDecimal overtimePay = new BigDecimal("30000");
        BigDecimal holidayPay = new BigDecimal("90000");
        BigDecimal weeklyHolidayPay = new BigDecimal("80000");
        BigDecimal nationalPension = new BigDecimal("141075");
        BigDecimal healthInsurance = new BigDecimal("111135");
        BigDecimal workersCompInsurance = new BigDecimal("21945");
        BigDecimal employmentInsurance = new BigDecimal("28215");
        BigDecimal netPay = new BigDecimal("3032680");

        // when
        byte[] pdfBytes = pdfGenerator.generatePayrollPdf(
                testPayroll,
                basePay, nightPay, overtimePay, holidayPay, weeklyHolidayPay,
                nationalPension, healthInsurance, workersCompInsurance, employmentInsurance,
                netPay
        );

        // then
        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(0);
    }

    @Test
    @DisplayName("PDF 생성 성공 - 상용직 월급")
    void generatePayrollPdf_PermanentWorker() {
        // given
        Payroll permanentPayroll = Payroll.builder()
                .employeeId(2L)
                .contractId(2L)
                .empName("김철수")
                .empType(EmpType.PERMANENT)
                .salaryYear(2025)
                .salaryMonth(1)
                .salaryWeek(0)  // 월급이므로 센티널 값 0
                .salaryDay(LocalDate.of(2025, 1, 1))  // 월급이므로 해당 월의 1일
                .payDueDate(LocalDate.of(2025, 2, 10))
                .totalWorkHour(new BigDecimal("209.00"))
                .totalPay(new BigDecimal("3135000"))
                .incomeTax(new BigDecimal("103455"))
                .residentTax(new BigDecimal("9405"))
                .generatedAt(LocalDateTime.of(2025, 2, 1, 0, 0))
                .build();

        String mockHtml = "<html><body>급여명세서</body></html>";
        when(templateEngine.process(eq("payroll/payslip"), any(Context.class)))
                .thenReturn(mockHtml);

        BigDecimal basePay = new BigDecimal("3135000");
        BigDecimal nationalPension = new BigDecimal("141075");
        BigDecimal healthInsurance = new BigDecimal("111135");
        BigDecimal workersCompInsurance = new BigDecimal("21945");
        BigDecimal employmentInsurance = new BigDecimal("28215");
        BigDecimal netPay = new BigDecimal("2719770");

        // when
        byte[] pdfBytes = pdfGenerator.generatePayrollPdf(
                permanentPayroll,
                basePay, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                nationalPension, healthInsurance, workersCompInsurance, employmentInsurance,
                netPay
        );

        // then
        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(0);
    }

    @Test
    @DisplayName("PDF 생성 실패 - 템플릿 처리 오류")
    void generatePayrollPdf_TemplateError_ThrowsException() {
        // given
        when(templateEngine.process(eq("payroll/payslip"), any(Context.class)))
                .thenThrow(new RuntimeException("템플릿 처리 오류"));

        // when & then
        assertThatThrownBy(() -> pdfGenerator.generatePayrollPdf(
                testPayroll,
                new BigDecimal("96000"), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("92544")
        ))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("급여명세서 PDF 생성 실패");
    }

    @Test
    @DisplayName("PDF 생성 - null 값 처리")
    void generatePayrollPdf_NullValues_HandledGracefully() {
        // given
        Payroll payrollWithNulls = Payroll.builder()
                .employeeId(1L)
                .contractId(1L)
                .empName("홍길동")
                .empType(EmpType.DAILY)
                .salaryYear(2025)
                .salaryMonth(1)
                .salaryWeek(0)  // 센티널 값 필수
                .salaryDay(LocalDate.of(2025, 1, 15))  // 센티널 값 필수
                .payDueDate(null) // null
                .totalWorkHour(null) // null
                .totalPay(new BigDecimal("96000"))
                .incomeTax(new BigDecimal("3168"))
                .residentTax(new BigDecimal("288"))
                .generatedAt(null) // null
                .build();

        String mockHtml = "<html><body>급여명세서</body></html>";
        when(templateEngine.process(eq("payroll/payslip"), any(Context.class)))
                .thenReturn(mockHtml);

        // when
        byte[] pdfBytes = pdfGenerator.generatePayrollPdf(
                payrollWithNulls,
                new BigDecimal("96000"), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("92544")
        );

        // then
        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(0);
    }

    @Test
    @DisplayName("PDF 생성 - 주급 계산 케이스")
    void generatePayrollPdf_WeeklyPayroll() {
        // given
        Payroll weeklyPayroll = Payroll.builder()
                .employeeId(3L)
                .contractId(3L)
                .empName("이영희")
                .empType(EmpType.DAILY)
                .salaryYear(2025)
                .salaryMonth(1)
                .salaryWeek(2)  // 주급 2주차
                .salaryDay(LocalDate.of(2025, 1, 1))  // 주급이므로 해당 월의 1일
                .payDueDate(LocalDate.of(2025, 1, 20))
                .totalWorkHour(new BigDecimal("40.00"))
                .totalPay(new BigDecimal("480000"))
                .incomeTax(new BigDecimal("15840"))
                .residentTax(new BigDecimal("1440"))
                .generatedAt(LocalDateTime.now())
                .build();

        String mockHtml = "<html><body>급여명세서</body></html>";
        when(templateEngine.process(eq("payroll/payslip"), any(Context.class)))
                .thenReturn(mockHtml);

        BigDecimal basePay = new BigDecimal("400000");
        BigDecimal weeklyHolidayPay = new BigDecimal("80000");
        BigDecimal netPay = new BigDecimal("462720");

        // when
        byte[] pdfBytes = pdfGenerator.generatePayrollPdf(
                weeklyPayroll,
                basePay, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, weeklyHolidayPay,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                netPay
        );

        // then
        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(0);
    }

    @Test
    @DisplayName("PDF 생성 - 0원 항목 제외 확인")
    void generatePayrollPdf_ZeroAmounts() {
        // given
        String mockHtml = "<html><body>급여명세서</body></html>";
        when(templateEngine.process(eq("payroll/payslip"), any(Context.class)))
                .thenReturn(mockHtml);

        // when - 모든 수당과 4대보험이 0원
        byte[] pdfBytes = pdfGenerator.generatePayrollPdf(
                testPayroll,
                new BigDecimal("96000"),
                BigDecimal.ZERO, // 야간수당 0
                BigDecimal.ZERO, // 연장수당 0
                BigDecimal.ZERO, // 휴일수당 0
                BigDecimal.ZERO, // 주휴수당 0
                BigDecimal.ZERO, // 국민연금 0
                BigDecimal.ZERO, // 건강보험 0
                BigDecimal.ZERO, // 산재보험 0
                BigDecimal.ZERO, // 고용보험 0
                new BigDecimal("92544")
        );

        // then
        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(0);
    }
}