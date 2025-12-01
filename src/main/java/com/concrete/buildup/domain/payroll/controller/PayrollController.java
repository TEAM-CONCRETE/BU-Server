package com.concrete.buildup.domain.payroll.controller;

import com.concrete.buildup.domain.contract.enums.EmpType;
import com.concrete.buildup.domain.contract.enums.PayPeriod;
import com.concrete.buildup.domain.payroll.dto.PayrollDetailResponse;
import com.concrete.buildup.domain.payroll.dto.SalaryHistorySummaryResponse;
import com.concrete.buildup.domain.payroll.service.PayrollService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 급여 내역 조회 컨트롤러
 *
 * <p>급여 내역 기간별 조회 API를 제공합니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Slf4j
@Tag(name = "Payroll", description = "급여 내역 조회 API")
@RestController
@RequestMapping("/v1/payrolls")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CORPORATION')")
@Validated
public class PayrollController {

    private final PayrollService payrollService;

    /**
     * 상용직 기간별 급여 내역 조회
     *
     * <p>특정 현장의 상용직 근로자들에 대한 기간별 급여 내역을 조회합니다.</p>
     *
     * @param siteId 현장 ID (필수)
     * @param year 급여 대상 연도 (예: 2025)
     * @param month 급여 대상 월 (1~12)
     * @param page 페이지 번호 (기본값: 1)
     * @param size 페이지 크기 (기본값: 20)
     * @return 급여 내역 요약 정보 (통계 + 목록)
     */
    @Operation(
            summary = "상용직 기간별 급여 내역 조회",
            description = "특정 현장의 상용직 근로자들에 대한 기간별 급여 내역을 조회합니다. " +
                    "전체 통계(총 건수, 미지급 건수, 총 지급액)와 함께 페이징된 급여 목록을 반환합니다."
    )
    @GetMapping("/period/permanent")
    public ResponseEntity<SalaryHistorySummaryResponse> getPermanentSalaryHistory(
            @Parameter(description = "현장 ID", required = true, example = "1")
            @RequestParam @Min(1) Long siteId,

            @Parameter(description = "급여 대상 연도", required = true, example = "2025")
            @RequestParam @Min(2000) @Max(2100) Integer year,

            @Parameter(description = "급여 대상 월 (1~12)", required = true, example = "11")
            @RequestParam @Min(1) @Max(12) Integer month,

            @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
            @RequestParam(defaultValue = "1") @Min(1) Integer page,

            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size
    ) {
        log.info("상용직 급여 내역 조회 - siteId: {}, year: {}, month: {}, page: {}, size: {}",
                siteId, year, month, page, size);

        SalaryHistorySummaryResponse response = payrollService.getSalaryHistory(
                siteId,
                year,
                month,
                EmpType.PERMANENT,
                null, // 상용직은 payCycle이 항상 MONTHLY이므로 null
                PageRequest.of(page - 1, size)
        );

        log.info("상용직 급여 내역 조회 완료 - totalCount: {}, unpaidCount: {}",
                response.getTotalCount(), response.getUnpaidCount());

        return ResponseEntity.ok(response);
    }

    /**
     * 일용직 월급 기간별 급여 내역 조회
     *
     * <p>특정 현장의 일용직(월급) 근로자들에 대한 기간별 급여 내역을 조회합니다.</p>
     *
     * @param siteId 현장 ID (필수)
     * @param year 급여 대상 연도 (예: 2025)
     * @param month 급여 대상 월 (1~12)
     * @param page 페이지 번호 (기본값: 1)
     * @param size 페이지 크기 (기본값: 20)
     * @return 급여 내역 요약 정보 (통계 + 목록)
     */
    @Operation(
            summary = "일용직 월급 기간별 급여 내역 조회",
            description = "특정 현장의 일용직(월급) 근로자들에 대한 기간별 급여 내역을 조회합니다. " +
                    "전체 통계(총 건수, 미지급 건수, 총 지급액)와 함께 페이징된 급여 목록을 반환합니다."
    )
    @GetMapping("/period/daily/monthly")
    public ResponseEntity<SalaryHistorySummaryResponse> getDailyMonthlySalaryHistory(
            @Parameter(description = "현장 ID", required = true, example = "1")
            @RequestParam @Min(1) Long siteId,

            @Parameter(description = "급여 대상 연도", required = true, example = "2025")
            @RequestParam @Min(2000) @Max(2100) Integer year,

            @Parameter(description = "급여 대상 월 (1~12)", required = true, example = "11")
            @RequestParam @Min(1) @Max(12) Integer month,

            @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
            @RequestParam(defaultValue = "1") @Min(1) Integer page,

            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size
    ) {
        log.info("일용직 월급 급여 내역 조회 - siteId: {}, year: {}, month: {}, page: {}, size: {}",
                siteId, year, month, page, size);

        SalaryHistorySummaryResponse response = payrollService.getSalaryHistory(
                siteId,
                year,
                month,
                EmpType.DAILY,
                PayPeriod.MONTHLY,
                PageRequest.of(page - 1, size)
        );

        log.info("일용직 월급 급여 내역 조회 완료 - totalCount: {}, unpaidCount: {}",
                response.getTotalCount(), response.getUnpaidCount());

        return ResponseEntity.ok(response);
    }

    /**
     * 일용직 주급 기간별 급여 내역 조회
     *
     * <p>특정 현장의 일용직(주급) 근로자들에 대한 기간별 급여 내역을 조회합니다.</p>
     *
     * @param siteId 현장 ID (필수)
     * @param year 급여 대상 연도 (예: 2025)
     * @param month 급여 대상 월 (1~12)
     * @param page 페이지 번호 (기본값: 1)
     * @param size 페이지 크기 (기본값: 20)
     * @return 급여 내역 요약 정보 (통계 + 목록)
     */
    @Operation(
            summary = "일용직 주급 기간별 급여 내역 조회",
            description = "특정 현장의 일용직(주급) 근로자들에 대한 기간별 급여 내역을 조회합니다. " +
                    "전체 통계(총 건수, 미지급 건수, 총 지급액)와 함께 페이징된 급여 목록을 반환합니다."
    )
    @GetMapping("/period/daily/weekly")
    public ResponseEntity<SalaryHistorySummaryResponse> getDailyWeeklySalaryHistory(
            @Parameter(description = "현장 ID", required = true, example = "1")
            @RequestParam @Min(1) Long siteId,

            @Parameter(description = "급여 대상 연도", required = true, example = "2025")
            @RequestParam @Min(2000) @Max(2100) Integer year,

            @Parameter(description = "급여 대상 월 (1~12)", required = true, example = "11")
            @RequestParam @Min(1) @Max(12) Integer month,

            @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
            @RequestParam(defaultValue = "1") @Min(1) Integer page,

            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size
    ) {
        log.info("일용직 주급 급여 내역 조회 - siteId: {}, year: {}, month: {}, page: {}, size: {}",
                siteId, year, month, page, size);

        SalaryHistorySummaryResponse response = payrollService.getSalaryHistory(
                siteId,
                year,
                month,
                EmpType.DAILY,
                PayPeriod.WEEKLY,
                PageRequest.of(page - 1, size)
        );

        log.info("일용직 주급 급여 내역 조회 완료 - totalCount: {}, unpaidCount: {}",
                response.getTotalCount(), response.getUnpaidCount());

        return ResponseEntity.ok(response);
    }

    /**
     * 일용직 일급 기간별 급여 내역 조회
     *
     * <p>특정 현장의 일용직(일급) 근로자들에 대한 기간별 급여 내역을 조회합니다.</p>
     *
     * @param siteId 현장 ID (필수)
     * @param year 급여 대상 연도 (예: 2025)
     * @param month 급여 대상 월 (1~12)
     * @param page 페이지 번호 (기본값: 1)
     * @param size 페이지 크기 (기본값: 20)
     * @return 급여 내역 요약 정보 (통계 + 목록)
     */
    @Operation(
            summary = "일용직 일급 기간별 급여 내역 조회",
            description = "특정 현장의 일용직(일급) 근로자들에 대한 기간별 급여 내역을 조회합니다. " +
                    "전체 통계(총 건수, 미지급 건수, 총 지급액)와 함께 페이징된 급여 목록을 반환합니다."
    )
    @GetMapping("/period/daily/daily")
    public ResponseEntity<SalaryHistorySummaryResponse> getDailyDailySalaryHistory(
            @Parameter(description = "현장 ID", required = true, example = "1")
            @RequestParam @Min(1) Long siteId,

            @Parameter(description = "급여 대상 연도", required = true, example = "2025")
            @RequestParam @Min(2000) @Max(2100) Integer year,

            @Parameter(description = "급여 대상 월 (1~12)", required = true, example = "11")
            @RequestParam @Min(1) @Max(12) Integer month,

            @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
            @RequestParam(defaultValue = "1") @Min(1) Integer page,

            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size
    ) {
        log.info("일용직 일급 급여 내역 조회 - siteId: {}, year: {}, month: {}, page: {}, size: {}",
                siteId, year, month, page, size);

        SalaryHistorySummaryResponse response = payrollService.getSalaryHistory(
                siteId,
                year,
                month,
                EmpType.DAILY,
                PayPeriod.DAILY,
                PageRequest.of(page - 1, size)
        );

        log.info("일용직 일급 급여 내역 조회 완료 - totalCount: {}, unpaidCount: {}",
                response.getTotalCount(), response.getUnpaidCount());

        return ResponseEntity.ok(response);
    }

    /**
     * 급여명세서 상세 조회
     *
     * <p>급여 ID로 급여명세서의 전체 정보를 조회합니다.</p>
     *
     * @param payrollId 급여 ID (필수)
     * @param page 근무일지 페이지 번호 (기본값: 0)
     * @param size 근무일지 페이지 크기 (기본값: 10)
     * @return 급여명세서 상세 정보
     */
    @Operation(
            summary = "급여명세서 상세 조회",
            description = "급여 ID로 급여명세서의 전체 정보를 조회합니다. " +
                    "헤더 정보, 집계 정보, 급여항목 리스트, 근무일지 리스트(페이징)를 반환합니다."
    )
    @GetMapping("/{payrollId}")
    public ResponseEntity<PayrollDetailResponse> getPayrollDetail(
            @Parameter(description = "급여 ID", required = true, example = "1")
            @PathVariable @Min(1) Long payrollId,

            @Parameter(description = "근무일지 페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) Integer page,

            @Parameter(description = "근무일지 페이지 크기", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer size
    ) {
        log.info("급여명세서 상세 조회 - payrollId: {}, page: {}, size: {}", payrollId, page, size);

        PayrollDetailResponse response = payrollService.getPayrollDetail(
                payrollId,
                PageRequest.of(page, size)
        );

        log.info("급여명세서 상세 조회 완료 - payrollId: {}, 근무일지: {}건",
                payrollId, response.getAttendances().getContent().size());

        return ResponseEntity.ok(response);
    }
}