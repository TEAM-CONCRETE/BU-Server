package com.concrete.buildup.domain.employee.dto;

import com.concrete.buildup.domain.payroll.entity.Payroll;
import com.concrete.buildup.domain.payroll.enums.PayStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 근로자 본인 급여 내역 목록 응답 DTO
 *
 * <p>최근 급여 상세 정보와 급여 내역 목록을 포함합니다.</p>
 */
@Schema(description = "근로자 본인 급여 내역 목록 응답")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyPayrollListResponse {

    @Schema(description = "최근 급여 상세 정보 (급여 내역이 없으면 null)")
    private LatestPayrollDetail latestPayroll;

    @Schema(description = "급여 내역 목록")
    private List<MyPayrollSummary> content;

    @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
    private int pageNumber;

    @Schema(description = "페이지 크기", example = "20")
    private int pageSize;

    @Schema(description = "전체 항목 수", example = "50")
    private long totalElements;

    @Schema(description = "전체 페이지 수", example = "3")
    private int totalPages;

    @Schema(description = "첫 페이지 여부", example = "true")
    private boolean first;

    @Schema(description = "마지막 페이지 여부", example = "false")
    private boolean last;

    /**
     * 최근 급여 상세 정보
     */
    @Schema(description = "최근 급여 상세 정보")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LatestPayrollDetail {

        @Schema(description = "급여 ID", example = "1")
        private Long payrollId;

        @Schema(description = "현장명", example = "강남 아파트 신축현장")
        private String siteName;

        @Schema(description = "급여 대상 연도", example = "2025")
        private Integer salaryYear;

        @Schema(description = "급여 대상 월", example = "1")
        private Integer salaryMonth;

        @Schema(description = "지급 기준일", example = "2025-01-25")
        private LocalDate payDate;

        @Schema(description = "지급 상태", example = "PAID")
        private PayStatus payStatus;

        // ========== 지급 항목 ==========

        @Schema(description = "실지급액 (총 지급액)", example = "3500000")
        private BigDecimal netPay;

        @Schema(description = "기본급", example = "3000000")
        private BigDecimal basePay;

        @Schema(description = "연장근로수당", example = "300000")
        private BigDecimal overtimePay;

        @Schema(description = "야간근로수당", example = "150000")
        private BigDecimal nightPay;

        // ========== 공제 항목 ==========

        @Schema(description = "소득세", example = "120000")
        private BigDecimal incomeTax;

        @Schema(description = "주민세", example = "12000")
        private BigDecimal residentTax;

        @Schema(description = "4대보험료 합계 (국민연금 + 건강보험 + 고용보험 + 산재보험)", example = "280000")
        private BigDecimal insuranceTotal;

        @Schema(description = "급여명세서 PDF 존재 여부", example = "true")
        private Boolean hasPdf;
    }

    /**
     * 급여 내역 요약 정보 (목록용)
     */
    @Schema(description = "급여 내역 요약 정보")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MyPayrollSummary {

        @Schema(description = "급여 ID", example = "1")
        private Long payrollId;

        @Schema(description = "현장명", example = "강남 아파트 신축현장")
        private String siteName;

        @Schema(description = "급여 대상 연도", example = "2025")
        private Integer salaryYear;

        @Schema(description = "급여 대상 월", example = "1")
        private Integer salaryMonth;

        @Schema(description = "지급 기준일", example = "2025-01-25")
        private LocalDate payDate;

        @Schema(description = "실지급액 (총 지급액)", example = "3500000")
        private BigDecimal netPay;

        @Schema(description = "지급 상태", example = "PAID")
        private PayStatus payStatus;

        @Schema(description = "급여명세서 PDF 존재 여부", example = "true")
        private Boolean hasPdf;

        /**
         * Payroll 엔티티와 현장명으로 요약 정보 생성
         */
        public static MyPayrollSummary from(Payroll payroll, String siteName) {
            return MyPayrollSummary.builder()
                    .payrollId(payroll.getId())
                    .siteName(siteName)
                    .salaryYear(payroll.getSalaryYear())
                    .salaryMonth(payroll.getSalaryMonth())
                    .payDate(payroll.getSearchDate())
                    .netPay(payroll.getTotalPay())
                    .payStatus(payroll.getPayStatus())
                    .hasPdf(payroll.getS3Key() != null && !payroll.getS3Key().isEmpty())
                    .build();
        }
    }

    /**
     * Page 객체와 최근 급여 정보로 응답 생성
     */
    public static MyPayrollListResponse from(Page<MyPayrollSummary> page, LatestPayrollDetail latestPayroll) {
        return MyPayrollListResponse.builder()
                .latestPayroll(latestPayroll)
                .content(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
