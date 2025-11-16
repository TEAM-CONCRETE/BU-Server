package com.concrete.buildup.domain.payroll.util;

import com.concrete.buildup.domain.payroll.entity.Payroll;
import com.lowagie.text.DocumentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

/**
 * 급여명세서 PDF 생성 유틸리티
 *
 * Thymeleaf 템플릿과 Flying Saucer를 사용하여 급여명세서 PDF를 생성합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PayrollPdfGenerator {

    private final TemplateEngine templateEngine;

    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getNumberInstance(Locale.KOREA);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");

    /**
     * 급여명세서 PDF 생성
     *
     * @param payroll 급여 정보
     * @param basePay 기본급
     * @param nightPay 야간근로수당
     * @param overtimePay 연장근로수당
     * @param holidayPay 휴일근로수당
     * @param weeklyHolidayPay 주휴수당
     * @param nationalPension 국민연금
     * @param healthInsurance 건강보험
     * @param workersCompInsurance 산재보험
     * @param employmentInsurance 고용보험
     * @param netPay 실수령액
     * @return PDF 바이트 배열
     */
    public byte[] generatePayrollPdf(Payroll payroll,
                                     BigDecimal basePay,
                                     BigDecimal nightPay,
                                     BigDecimal overtimePay,
                                     BigDecimal holidayPay,
                                     BigDecimal weeklyHolidayPay,
                                     BigDecimal nationalPension,
                                     BigDecimal healthInsurance,
                                     BigDecimal workersCompInsurance,
                                     BigDecimal employmentInsurance,
                                     BigDecimal netPay) {
        try {
            // 1. Thymeleaf Context 생성 및 데이터 설정
            Context context = new Context();

            // 기본 정보
            context.setVariable("empName", payroll.getEmpName());
            context.setVariable("empType", getEmpTypeText(payroll.getEmpType()));
            context.setVariable("salaryPeriod", getSalaryPeriodText(payroll));
            context.setVariable("payDueDate", payroll.getPayDueDate() != null
                    ? payroll.getPayDueDate().format(DATE_FORMAT) : "-");

            // 근무 정보
            context.setVariable("totalWorkHour", formatDecimal(payroll.getTotalWorkHour()));

            // 지급 항목
            context.setVariable("basePay", formatCurrency(basePay));
            context.setVariable("nightPay", formatCurrency(nightPay));
            context.setVariable("overtimePay", formatCurrency(overtimePay));
            context.setVariable("holidayPay", formatCurrency(holidayPay));
            context.setVariable("weeklyHolidayPay", formatCurrency(weeklyHolidayPay));
            context.setVariable("totalPay", formatCurrency(payroll.getTotalPay()));

            // 공제 항목
            context.setVariable("incomeTax", formatCurrency(payroll.getIncomeTax()));
            context.setVariable("residentTax", formatCurrency(payroll.getResidentTax()));
            context.setVariable("nationalPension", formatCurrency(nationalPension));
            context.setVariable("healthInsurance", formatCurrency(healthInsurance));
            context.setVariable("workersCompInsurance", formatCurrency(workersCompInsurance));
            context.setVariable("employmentInsurance", formatCurrency(employmentInsurance));

            // 총 공제액
            BigDecimal totalDeduction = safeAdd(
                    payroll.getIncomeTax(),
                    payroll.getResidentTax(),
                    nationalPension,
                    healthInsurance,
                    workersCompInsurance,
                    employmentInsurance
            );
            context.setVariable("totalDeduction", formatCurrency(totalDeduction));

            // 실수령액
            context.setVariable("netPay", formatCurrency(netPay));

            // 생성 일시
            context.setVariable("generatedAt", payroll.getGeneratedAt() != null
                    ? payroll.getGeneratedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                    : "-");

            // 2. Thymeleaf 템플릿 렌더링
            String htmlContent = templateEngine.process("payroll/payslip", context);

            // 3. HTML을 PDF로 변환
            return convertHtmlToPdf(htmlContent);

        } catch (Exception e) {
            log.error("[PDF 생성] 급여명세서 PDF 생성 실패 - payrollId: {}", payroll.getId(), e);
            throw new RuntimeException("급여명세서 PDF 생성 실패", e);
        }
    }

    /**
     * HTML을 PDF로 변환
     *
     * @param htmlContent HTML 문자열
     * @return PDF 바이트 배열
     */
    private byte[] convertHtmlToPdf(String htmlContent) throws IOException, DocumentException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();

            // 한글 폰트 등록 (나눔고딕)
            renderer.getFontResolver().addFont(
                    "/fonts/NanumGothic.ttf",
                    com.lowagie.text.pdf.BaseFont.IDENTITY_H,
                    com.lowagie.text.pdf.BaseFont.EMBEDDED
            );

            renderer.setDocumentFromString(htmlContent);
            renderer.layout();
            renderer.createPDF(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * 급여 기간 텍스트 생성
     */
    private String getSalaryPeriodText(Payroll payroll) {
        StringBuilder sb = new StringBuilder();
        sb.append(payroll.getSalaryYear()).append("년 ");
        sb.append(payroll.getSalaryMonth()).append("월");

        if (payroll.getSalaryWeek() != null) {
            sb.append(" ").append(payroll.getSalaryWeek()).append("주차");
        }

        if (payroll.getSalaryDay() != null) {
            sb.append(" ").append(payroll.getSalaryDay().format(DateTimeFormatter.ofPattern("dd일")));
        }

        return sb.toString();
    }

    /**
     * 근로자 유형 텍스트
     */
    private String getEmpTypeText(com.concrete.buildup.domain.contract.enums.EmpType empType) {
        if (empType == null) return "-";
        return switch (empType) {
            case PERMANENT -> "상용직";
            case DAILY -> "일용직";
        };
    }

    /**
     * 통화 포맷 (원 단위)
     */
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0원";
        return CURRENCY_FORMAT.format(amount.longValue()) + "원";
    }

    /**
     * 소수점 포맷
     */
    private String formatDecimal(BigDecimal value) {
        if (value == null) return "0";
        return value.stripTrailingZeros().toPlainString();
    }

    /**
     * null-safe BigDecimal 합산
     * null 값은 0으로 처리하여 NullPointerException 방지
     *
     * @param values 합산할 BigDecimal 값들
     * @return 합산 결과 (null이 없는 값들의 합)
     */
    private BigDecimal safeAdd(BigDecimal... values) {
        return Arrays.stream(values)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}