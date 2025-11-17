package com.concrete.buildup.domain.payroll.controller;

import com.concrete.buildup.domain.contract.enums.EmpType;
import com.concrete.buildup.domain.contract.enums.PayPeriod;
import com.concrete.buildup.domain.payroll.dto.SalaryHistoryItemResponse;
import com.concrete.buildup.domain.payroll.dto.SalaryHistorySummaryResponse;
import com.concrete.buildup.domain.payroll.service.PayrollService;
import com.concrete.buildup.global.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * PayrollController 단위 테스트
 */
@WebMvcTest(PayrollController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("PayrollController 단위 테스트")
class PayrollControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PayrollService payrollService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("상용직 급여 내역 조회 - 성공")
    void getPermanentSalaryHistory_Success() throws Exception {
        // given
        Long siteId = 1L;
        Integer year = 2025;
        Integer month = 11;
        Integer page = 1;
        Integer size = 20;

        SalaryHistorySummaryResponse mockResponse = createMockResponse();

        given(payrollService.getSalaryHistory(
                eq(siteId), eq(year), eq(month), eq(EmpType.PERMANENT), isNull(), eq(PageRequest.of(0, 20))
        )).willReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/v1/payrolls/period/permanent")
                        .param("siteId", siteId.toString())
                        .param("year", year.toString())
                        .param("month", month.toString())
                        .param("page", page.toString())
                        .param("size", size.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(3))
                .andExpect(jsonPath("$.unpaidCount").value(1))
                .andExpect(jsonPath("$.totalPaidAmount").value(5000000))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].employeeId").value(1))
                .andExpect(jsonPath("$.data[0].name").value("김철수"))
                .andExpect(jsonPath("$.data[0].residentId").value("850101-1******"))
                .andExpect(jsonPath("$.data[0].paid").value(true))
                .andExpect(jsonPath("$.data[0].payCycle").value("MONTHLY"));

        verify(payrollService).getSalaryHistory(
                siteId, year, month, EmpType.PERMANENT, null, PageRequest.of(0, 20)
        );
    }

    @Test
    @DisplayName("상용직 급여 내역 조회 - 기본 페이징 값")
    void getPermanentSalaryHistory_DefaultPagination() throws Exception {
        // given
        Long siteId = 1L;
        Integer year = 2025;
        Integer month = 11;

        SalaryHistorySummaryResponse mockResponse = createMockResponse();

        given(payrollService.getSalaryHistory(
                eq(siteId), eq(year), eq(month), eq(EmpType.PERMANENT), isNull(), eq(PageRequest.of(0, 20))
        )).willReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/v1/payrolls/period/permanent")
                        .param("siteId", siteId.toString())
                        .param("year", year.toString())
                        .param("month", month.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(3));

        verify(payrollService).getSalaryHistory(
                siteId, year, month, EmpType.PERMANENT, null, PageRequest.of(0, 20)
        );
    }

    @Test
    @DisplayName("상용직 급여 내역 조회 - 필수 파라미터 누락 시 400 에러")
    void getPermanentSalaryHistory_MissingRequiredParam() throws Exception {
        // when & then
        mockMvc.perform(get("/v1/payrolls/period/permanent")
                        .param("year", "2025")
                        .param("month", "11")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("상용직 급여 내역 조회 - 커스텀 페이징")
    void getPermanentSalaryHistory_CustomPagination() throws Exception {
        // given
        Long siteId = 1L;
        Integer year = 2025;
        Integer month = 11;
        Integer page = 2;
        Integer size = 10;

        SalaryHistorySummaryResponse mockResponse = createMockResponse();

        given(payrollService.getSalaryHistory(
                eq(siteId), eq(year), eq(month), eq(EmpType.PERMANENT), isNull(), eq(PageRequest.of(1, 10))
        )).willReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/v1/payrolls/period/permanent")
                        .param("siteId", siteId.toString())
                        .param("year", year.toString())
                        .param("month", month.toString())
                        .param("page", page.toString())
                        .param("size", size.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());

        verify(payrollService).getSalaryHistory(
                siteId, year, month, EmpType.PERMANENT, null, PageRequest.of(1, 10)
        );
    }

    @Test
    @DisplayName("상용직 급여 내역 조회 - 빈 결과")
    void getPermanentSalaryHistory_EmptyResult() throws Exception {
        // given
        Long siteId = 1L;
        Integer year = 2025;
        Integer month = 11;

        SalaryHistorySummaryResponse emptyResponse = SalaryHistorySummaryResponse.builder()
                .totalCount(0)
                .unpaidCount(0)
                .totalPaidAmount(BigDecimal.ZERO)
                .data(List.of())
                .build();

        given(payrollService.getSalaryHistory(
                eq(siteId), eq(year), eq(month), eq(EmpType.PERMANENT), isNull(), any(PageRequest.class)
        )).willReturn(emptyResponse);

        // when & then
        mockMvc.perform(get("/v1/payrolls/period/permanent")
                        .param("siteId", siteId.toString())
                        .param("year", year.toString())
                        .param("month", month.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.unpaidCount").value(0))
                .andExpect(jsonPath("$.totalPaidAmount").value(0))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("일용직 월급 급여 내역 조회 - 성공")
    void getDailyMonthlySalaryHistory_Success() throws Exception {
        // given
        Long siteId = 1L;
        Integer year = 2025;
        Integer month = 11;
        Integer page = 1;
        Integer size = 20;

        SalaryHistorySummaryResponse mockResponse = createMockResponse();

        given(payrollService.getSalaryHistory(
                eq(siteId), eq(year), eq(month), eq(EmpType.DAILY), eq(PayPeriod.MONTHLY), eq(PageRequest.of(0, 20))
        )).willReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/v1/payrolls/period/daily/monthly")
                        .param("siteId", siteId.toString())
                        .param("year", year.toString())
                        .param("month", month.toString())
                        .param("page", page.toString())
                        .param("size", size.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(3))
                .andExpect(jsonPath("$.unpaidCount").value(1))
                .andExpect(jsonPath("$.totalPaidAmount").value(5000000));

        verify(payrollService).getSalaryHistory(
                siteId, year, month, EmpType.DAILY, PayPeriod.MONTHLY, PageRequest.of(0, 20)
        );
    }

    @Test
    @DisplayName("일용직 주급 급여 내역 조회 - 성공")
    void getDailyWeeklySalaryHistory_Success() throws Exception {
        // given
        Long siteId = 1L;
        Integer year = 2025;
        Integer month = 11;
        Integer page = 1;
        Integer size = 20;

        SalaryHistorySummaryResponse mockResponse = createMockResponse();

        given(payrollService.getSalaryHistory(
                eq(siteId), eq(year), eq(month), eq(EmpType.DAILY), eq(PayPeriod.WEEKLY), eq(PageRequest.of(0, 20))
        )).willReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/v1/payrolls/period/daily/weekly")
                        .param("siteId", siteId.toString())
                        .param("year", year.toString())
                        .param("month", month.toString())
                        .param("page", page.toString())
                        .param("size", size.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(3))
                .andExpect(jsonPath("$.unpaidCount").value(1))
                .andExpect(jsonPath("$.totalPaidAmount").value(5000000));

        verify(payrollService).getSalaryHistory(
                siteId, year, month, EmpType.DAILY, PayPeriod.WEEKLY, PageRequest.of(0, 20)
        );
    }

    @Test
    @DisplayName("일용직 일급 급여 내역 조회 - 성공")
    void getDailyDailySalaryHistory_Success() throws Exception {
        // given
        Long siteId = 1L;
        Integer year = 2025;
        Integer month = 11;
        Integer page = 1;
        Integer size = 20;

        SalaryHistorySummaryResponse mockResponse = createMockResponse();

        given(payrollService.getSalaryHistory(
                eq(siteId), eq(year), eq(month), eq(EmpType.DAILY), eq(PayPeriod.DAILY), eq(PageRequest.of(0, 20))
        )).willReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/v1/payrolls/period/daily/daily")
                        .param("siteId", siteId.toString())
                        .param("year", year.toString())
                        .param("month", month.toString())
                        .param("page", page.toString())
                        .param("size", size.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(3))
                .andExpect(jsonPath("$.unpaidCount").value(1))
                .andExpect(jsonPath("$.totalPaidAmount").value(5000000));

        verify(payrollService).getSalaryHistory(
                siteId, year, month, EmpType.DAILY, PayPeriod.DAILY, PageRequest.of(0, 20)
        );
    }

    @Test
    @DisplayName("일용직 월급 급여 내역 조회 - 기본 페이징 값")
    void getDailyMonthlySalaryHistory_DefaultPagination() throws Exception {
        // given
        Long siteId = 1L;
        Integer year = 2025;
        Integer month = 11;

        SalaryHistorySummaryResponse mockResponse = createMockResponse();

        given(payrollService.getSalaryHistory(
                eq(siteId), eq(year), eq(month), eq(EmpType.DAILY), eq(PayPeriod.MONTHLY), eq(PageRequest.of(0, 20))
        )).willReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/v1/payrolls/period/daily/monthly")
                        .param("siteId", siteId.toString())
                        .param("year", year.toString())
                        .param("month", month.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(3));

        verify(payrollService).getSalaryHistory(
                siteId, year, month, EmpType.DAILY, PayPeriod.MONTHLY, PageRequest.of(0, 20)
        );
    }

    @Test
    @DisplayName("일용직 급여 내역 조회 - 필수 파라미터 누락 시 400 에러")
    void getDailySalaryHistory_MissingRequiredParam() throws Exception {
        // when & then
        mockMvc.perform(get("/v1/payrolls/period/daily/monthly")
                        .param("year", "2025")
                        .param("month", "11")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    // ========== 테스트 데이터 생성 헬퍼 메서드 ==========

    private SalaryHistorySummaryResponse createMockResponse() {
        List<SalaryHistoryItemResponse> items = Arrays.asList(
                createItem(1L, "김철수", "850101-1******", new BigDecimal("2500000"), true),
                createItem(2L, "이영희", "900315-2******", new BigDecimal("2500000"), true),
                createItem(3L, "박민수", "920520-1******", new BigDecimal("3000000"), false)
        );

        return SalaryHistorySummaryResponse.builder()
                .totalCount(3)
                .unpaidCount(1)
                .totalPaidAmount(new BigDecimal("5000000"))
                .data(items)
                .build();
    }

    private SalaryHistoryItemResponse createItem(Long employeeId, String name, String residentId,
                                                  BigDecimal totalPay, boolean paid) {
        return SalaryHistoryItemResponse.builder()
                .employeeId(employeeId)
                .name(name)
                .residentId(residentId)
                .payDate("25.11.25")
                .totalPay(totalPay)
                .nonTaxIncome(new BigDecimal("100000"))
                .taxIncome(new BigDecimal("50000"))
                .localTax(new BigDecimal("5000"))
                .paid(paid)
                .payslipAvailable(true)
                .payCycle(PayPeriod.MONTHLY)
                .build();
    }
}
