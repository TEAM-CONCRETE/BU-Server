package com.concrete.buildup.domain.payroll.service;

import com.concrete.buildup.domain.contract.enums.EmpType;
import com.concrete.buildup.domain.contract.enums.PayPeriod;
import com.concrete.buildup.domain.payroll.dto.SalaryHistorySummaryResponse;
import com.concrete.buildup.domain.payroll.entity.Payroll;
import com.concrete.buildup.domain.payroll.enums.PayStatus;
import com.concrete.buildup.domain.payroll.repository.PayrollRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * PayrollService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PayrollService 단위 테스트")
class PayrollServiceTest {

    @Mock
    private PayrollRepository payrollRepository;

    @InjectMocks
    private PayrollService payrollService;

    @Test
    @DisplayName("상용직 급여 내역 조회 - 성공")
    void getSalaryHistory_Permanent_Success() {
        // given
        Long siteId = 1L;
        Integer year = 2025;
        Integer month = 11;
        Pageable pageable = PageRequest.of(0, 20);

        List<Payroll> payrolls = createTestPayrolls();
        Page<Payroll> payrollPage = new PageImpl<>(payrolls, pageable, payrolls.size());

        given(payrollRepository.findBySiteAndPeriodAndType(
                eq(siteId), eq(year), eq(month), eq(EmpType.PERMANENT), isNull(), eq(pageable)
        )).willReturn(payrollPage);

        given(payrollRepository.countUnpaidBySiteAndPeriodAndType(
                eq(siteId), eq(year), eq(month), eq(EmpType.PERMANENT), isNull()
        )).willReturn(1L);

        given(payrollRepository.sumTotalPaidAmountBySiteAndPeriodAndType(
                eq(siteId), eq(year), eq(month), eq(EmpType.PERMANENT), isNull()
        )).willReturn(new BigDecimal("5000000"));

        // when
        SalaryHistorySummaryResponse response = payrollService.getSalaryHistory(
                siteId, year, month, EmpType.PERMANENT, null, pageable
        );

        // then
        assertThat(response).isNotNull();
        assertThat(response.getTotalCount()).isEqualTo(3);
        assertThat(response.getUnpaidCount()).isEqualTo(1); // 전체 기준 미지급 건수
        assertThat(response.getTotalPaidAmount()).isEqualByComparingTo(new BigDecimal("5000000")); // 전체 기준 지급액
        assertThat(response.getData()).hasSize(3);

        verify(payrollRepository).findBySiteAndPeriodAndType(
                siteId, year, month, EmpType.PERMANENT, null, pageable
        );
        verify(payrollRepository).countUnpaidBySiteAndPeriodAndType(
                siteId, year, month, EmpType.PERMANENT, null
        );
        verify(payrollRepository).sumTotalPaidAmountBySiteAndPeriodAndType(
                siteId, year, month, EmpType.PERMANENT, null
        );
    }

    @Test
    @DisplayName("급여 내역 조회 - 빈 목록")
    void getSalaryHistory_EmptyList() {
        // given
        Long siteId = 1L;
        Integer year = 2025;
        Integer month = 11;
        Pageable pageable = PageRequest.of(0, 20);

        Page<Payroll> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        given(payrollRepository.findBySiteAndPeriodAndType(
                eq(siteId), eq(year), eq(month), eq(EmpType.PERMANENT), isNull(), eq(pageable)
        )).willReturn(emptyPage);

        given(payrollRepository.countUnpaidBySiteAndPeriodAndType(
                eq(siteId), eq(year), eq(month), eq(EmpType.PERMANENT), isNull()
        )).willReturn(0L);

        given(payrollRepository.sumTotalPaidAmountBySiteAndPeriodAndType(
                eq(siteId), eq(year), eq(month), eq(EmpType.PERMANENT), isNull()
        )).willReturn(BigDecimal.ZERO);

        // when
        SalaryHistorySummaryResponse response = payrollService.getSalaryHistory(
                siteId, year, month, EmpType.PERMANENT, null, pageable
        );

        // then
        assertThat(response).isNotNull();
        assertThat(response.getTotalCount()).isZero();
        assertThat(response.getUnpaidCount()).isZero();
        assertThat(response.getTotalPaidAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getData()).isEmpty();
    }

    @Test
    @DisplayName("급여 내역 조회 - 모두 미지급")
    void getSalaryHistory_AllUnpaid() {
        // given
        Long siteId = 1L;
        Integer year = 2025;
        Integer month = 11;
        Pageable pageable = PageRequest.of(0, 20);

        List<Payroll> payrolls = Arrays.asList(
                createPayroll(1L, "김철수", "850101-1******", new BigDecimal("2500000"), PayStatus.PENDING),
                createPayroll(2L, "이영희", "900315-2******", new BigDecimal("2500000"), PayStatus.PENDING)
        );
        Page<Payroll> payrollPage = new PageImpl<>(payrolls, pageable, payrolls.size());

        given(payrollRepository.findBySiteAndPeriodAndType(
                eq(siteId), eq(year), eq(month), eq(EmpType.PERMANENT), isNull(), eq(pageable)
        )).willReturn(payrollPage);

        given(payrollRepository.countUnpaidBySiteAndPeriodAndType(
                eq(siteId), eq(year), eq(month), eq(EmpType.PERMANENT), isNull()
        )).willReturn(2L);

        given(payrollRepository.sumTotalPaidAmountBySiteAndPeriodAndType(
                eq(siteId), eq(year), eq(month), eq(EmpType.PERMANENT), isNull()
        )).willReturn(BigDecimal.ZERO);

        // when
        SalaryHistorySummaryResponse response = payrollService.getSalaryHistory(
                siteId, year, month, EmpType.PERMANENT, null, pageable
        );

        // then
        assertThat(response.getTotalCount()).isEqualTo(2);
        assertThat(response.getUnpaidCount()).isEqualTo(2);
        assertThat(response.getTotalPaidAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("급여 내역 조회 - 일용직 월급")
    void getSalaryHistory_DailyMonthly() {
        // given
        Long siteId = 1L;
        Integer year = 2025;
        Integer month = 11;
        Pageable pageable = PageRequest.of(0, 20);

        List<Payroll> payrolls = createTestPayrolls();
        Page<Payroll> payrollPage = new PageImpl<>(payrolls, pageable, payrolls.size());

        given(payrollRepository.findBySiteAndPeriodAndType(
                eq(siteId), eq(year), eq(month), eq(EmpType.DAILY), eq(PayPeriod.MONTHLY), eq(pageable)
        )).willReturn(payrollPage);

        given(payrollRepository.countUnpaidBySiteAndPeriodAndType(
                eq(siteId), eq(year), eq(month), eq(EmpType.DAILY), eq(PayPeriod.MONTHLY)
        )).willReturn(1L);

        given(payrollRepository.sumTotalPaidAmountBySiteAndPeriodAndType(
                eq(siteId), eq(year), eq(month), eq(EmpType.DAILY), eq(PayPeriod.MONTHLY)
        )).willReturn(new BigDecimal("5000000"));

        // when
        SalaryHistorySummaryResponse response = payrollService.getSalaryHistory(
                siteId, year, month, EmpType.DAILY, PayPeriod.MONTHLY, pageable
        );

        // then
        assertThat(response).isNotNull();
        assertThat(response.getTotalCount()).isEqualTo(3);
        verify(payrollRepository).findBySiteAndPeriodAndType(
                siteId, year, month, EmpType.DAILY, PayPeriod.MONTHLY, pageable
        );
        verify(payrollRepository).countUnpaidBySiteAndPeriodAndType(
                siteId, year, month, EmpType.DAILY, PayPeriod.MONTHLY
        );
        verify(payrollRepository).sumTotalPaidAmountBySiteAndPeriodAndType(
                siteId, year, month, EmpType.DAILY, PayPeriod.MONTHLY
        );
    }

    @Test
    @DisplayName("DTO 변환 - 주민번호 마스킹 확인")
    void toItemResponse_ResidentNoMasking() {
        // given
        Long siteId = 1L;
        Integer year = 2025;
        Integer month = 11;
        Pageable pageable = PageRequest.of(0, 20);

        Payroll payroll = createPayroll(1L, "김철수", "850101-1234567", new BigDecimal("2500000"), PayStatus.PAID);
        Page<Payroll> payrollPage = new PageImpl<>(List.of(payroll), pageable, 1);

        given(payrollRepository.findBySiteAndPeriodAndType(
                eq(siteId), eq(year), eq(month), eq(EmpType.PERMANENT), isNull(), eq(pageable)
        )).willReturn(payrollPage);

        given(payrollRepository.countUnpaidBySiteAndPeriodAndType(
                eq(siteId), eq(year), eq(month), eq(EmpType.PERMANENT), isNull()
        )).willReturn(0L);

        given(payrollRepository.sumTotalPaidAmountBySiteAndPeriodAndType(
                eq(siteId), eq(year), eq(month), eq(EmpType.PERMANENT), isNull()
        )).willReturn(new BigDecimal("2500000"));

        // when
        SalaryHistorySummaryResponse response = payrollService.getSalaryHistory(
                siteId, year, month, EmpType.PERMANENT, null, pageable
        );

        // then
        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().get(0).getResidentId()).isEqualTo("850101-1******");
    }

    // ========== 테스트 데이터 생성 헬퍼 메서드 ==========

    private List<Payroll> createTestPayrolls() {
        return Arrays.asList(
                createPayroll(1L, "김철수", "850101-1******", new BigDecimal("2500000"), PayStatus.PAID),
                createPayroll(2L, "이영희", "900315-2******", new BigDecimal("2500000"), PayStatus.PAID),
                createPayroll(3L, "박민수", "920520-1******", new BigDecimal("3000000"), PayStatus.PENDING)
        );
    }

    private Payroll createPayroll(Long employeeId, String name, String residentNum,
                                   BigDecimal totalPay, PayStatus payStatus) {
        return Payroll.builder()
                .employeeId(employeeId)
                .contractId(1L)
                .corporationId(1L)
                .siteId(1L)
                .salaryYear(2025)
                .salaryMonth(11)
                .salaryWeek(0)
                .salaryDay(LocalDate.of(2025, 11, 1))
                .searchDate(LocalDate.of(2025, 11, 1))
                .payDueDate(LocalDate.of(2025, 11, 25))
                .empType(EmpType.PERMANENT)
                .empName(name)
                .residentNum(residentNum)
                .totalPay(totalPay)
                .noneTaxIncome(new BigDecimal("100000"))
                .incomeTax(new BigDecimal("50000"))
                .residentTax(new BigDecimal("5000"))
                .payCycle(PayPeriod.MONTHLY)
                .payStatus(payStatus)
                .s3Key("payroll/test/2025/11/salary-" + employeeId + ".pdf")
                .build();
    }
}
