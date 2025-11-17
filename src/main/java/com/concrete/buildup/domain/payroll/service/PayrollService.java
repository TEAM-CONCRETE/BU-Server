package com.concrete.buildup.domain.payroll.service;

import com.concrete.buildup.domain.contract.enums.EmpType;
import com.concrete.buildup.domain.contract.enums.PayPeriod;
import com.concrete.buildup.domain.payroll.dto.SalaryHistoryItemResponse;
import com.concrete.buildup.domain.payroll.dto.SalaryHistorySummaryResponse;
import com.concrete.buildup.domain.payroll.entity.Payroll;
import com.concrete.buildup.domain.payroll.enums.PayStatus;
import com.concrete.buildup.domain.payroll.repository.PayrollRepository;
import com.concrete.buildup.domain.payroll.util.ResidentNoMasker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 급여 조회 서비스
 *
 * <p>급여 내역 조회 및 통계 정보를 제공합니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PayrollService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yy.MM.dd");

    private final PayrollRepository payrollRepository;

    /**
     * 현장별 기간별 급여 내역 조회 (상용직/일용직 공통)
     *
     * <p>급여 내역 목록과 함께 전체 통계 정보(총 건수, 미지급 건수, 총 지급액)를 반환합니다.</p>
     *
     * @param siteId 현장 ID
     * @param year 급여 대상 연도
     * @param month 급여 대상 월
     * @param empType 근로자 유형 (PERMANENT: 상용직, DAILY: 일용직)
     * @param payCycle 급여 주기 (상용직: null, 일용직: MONTHLY/WEEKLY/DAILY)
     * @param pageable 페이징 정보
     * @return 급여 내역 요약 정보 (통계 + 목록)
     */
    public SalaryHistorySummaryResponse getSalaryHistory(
            Long siteId,
            Integer year,
            Integer month,
            EmpType empType,
            @Nullable PayPeriod payCycle,
            Pageable pageable
    ) {
        log.debug("급여 내역 조회 - siteId: {}, year: {}, month: {}, empType: {}, payCycle: {}",
                siteId, year, month, empType, payCycle);

        // 1. 급여 내역 조회
        Page<Payroll> payrollPage = payrollRepository.findBySiteAndPeriodAndType(
                siteId, year, month, empType, payCycle, pageable
        );

        // 2. DTO 변환
        List<SalaryHistoryItemResponse> items = payrollPage.getContent().stream()
                .map(this::toItemResponse)
                .collect(Collectors.toList());

        // 3. 통계 계산
        int totalCount = (int) payrollPage.getTotalElements();
        int unpaidCount = calculateUnpaidCount(payrollPage.getContent());
        BigDecimal totalPaidAmount = calculateTotalPaidAmount(payrollPage.getContent());

        log.debug("급여 내역 조회 완료 - totalCount: {}, unpaidCount: {}, totalPaidAmount: {}",
                totalCount, unpaidCount, totalPaidAmount);

        return SalaryHistorySummaryResponse.builder()
                .totalCount(totalCount)
                .unpaidCount(unpaidCount)
                .totalPaidAmount(totalPaidAmount)
                .data(items)
                .build();
    }

    /**
     * Payroll 엔티티를 SalaryHistoryItemResponse DTO로 변환
     *
     * @param payroll 급여 엔티티
     * @return 급여 내역 항목 DTO
     */
    private SalaryHistoryItemResponse toItemResponse(Payroll payroll) {
        return SalaryHistoryItemResponse.builder()
                .employeeId(payroll.getEmployeeId())
                .name(payroll.getEmpName())
                .residentId(ResidentNoMasker.mask(payroll.getResidentNum()))
                .payDate(formatPayDate(payroll.getPayDueDate()))
                .totalPay(payroll.getTotalPay())
                .nonTaxIncome(payroll.getNoneTaxIncome())
                .taxIncome(payroll.getIncomeTax())
                .localTax(payroll.getResidentTax())
                .paid(payroll.getPayStatus() == PayStatus.PAID)
                .payslipAvailable(payroll.getS3Key() != null && !payroll.getS3Key().isEmpty())
                .payCycle(payroll.getPayCycle())
                .build();
    }

    /**
     * 지급일 포맷팅 (yy.MM.dd)
     *
     * @param payDueDate 지급 예정일
     * @return 포맷팅된 날짜 문자열 (null이면 null 반환)
     */
    private String formatPayDate(java.time.LocalDate payDueDate) {
        if (payDueDate == null) {
            return null;
        }
        return payDueDate.format(DATE_FORMATTER);
    }

    /**
     * 미지급 건수 계산
     *
     * @param payrolls 급여 목록
     * @return 미지급 건수
     */
    private int calculateUnpaidCount(List<Payroll> payrolls) {
        return (int) payrolls.stream()
                .filter(p -> p.getPayStatus() != PayStatus.PAID)
                .count();
    }

    /**
     * 총 지급액 계산 (지급 완료된 금액의 합계)
     *
     * @param payrolls 급여 목록
     * @return 총 지급액
     */
    private BigDecimal calculateTotalPaidAmount(List<Payroll> payrolls) {
        return payrolls.stream()
                .filter(p -> p.getPayStatus() == PayStatus.PAID)
                .map(Payroll::getTotalPay)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
