package com.concrete.buildup.domain.payroll.dto;

import com.concrete.buildup.domain.contract.enums.PayPeriod;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 급여 내역 조회 - 개별 항목 응답 DTO
 *
 * <p>급여 내역 목록의 각 항목을 나타냅니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@Builder
public class SalaryHistoryItemResponse {

    /**
     * 사원 ID
     */
    private Long employeeId;

    /**
     * 사원 이름
     */
    private String name;

    /**
     * 주민등록번호 (마스킹 처리)
     * 예: 920315-2******
     */
    private String residentId;

    /**
     * 지급일
     * 예: 25.09.12
     */
    private String payDate;

    /**
     * 총 지급액
     */
    private BigDecimal totalPay;

    /**
     * 비과세 소득
     */
    private BigDecimal nonTaxIncome;

    /**
     * 소득세
     */
    private BigDecimal taxIncome;

    /**
     * 주민세
     */
    private BigDecimal localTax;

    /**
     * 지급 완료 여부
     */
    private Boolean paid;

    /**
     * 급여명세서 PDF 존재 여부
     */
    private Boolean payslipAvailable;

    /**
     * 급여 주기
     * 상용직: MONTHLY (고정)
     * 일용직: MONTHLY/WEEKLY/DAILY
     */
    private PayPeriod payCycle;
}