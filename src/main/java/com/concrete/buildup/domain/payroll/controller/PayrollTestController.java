package com.concrete.buildup.domain.payroll.controller;

import com.concrete.buildup.domain.payroll.service.SalaryGenerationService;
import com.concrete.buildup.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 급여 생성 테스트 컨트롤러
 *
 * 스케줄러를 기다리지 않고 수동으로 급여를 생성하여 테스트할 수 있습니다.
 *
 * ⚠️ 주의: 개발/테스트 환경에서만 사용하세요!
 * 운영 환경에서는 이 컨트롤러를 비활성화하거나 제거해야 합니다.
 */
@Slf4j
@RestController
@RequestMapping("/v1/payroll/test")
@RequiredArgsConstructor
public class PayrollTestController {

    private final SalaryGenerationService salaryGenerationService;

    /**
     * 일용직 일급 생성 테스트
     *
     * @param date 생성할 날짜 (yyyy-MM-dd), 기본값: 어제
     * @return 생성 결과
     *
     * 사용 예시:
     * GET /api/payroll/test/daily
     * GET /api/payroll/test/daily?date=2025-01-15
     */
    @GetMapping("/daily")
    public ResponseEntity<ApiResponse<String>> generateDailyPayroll(
            @RequestParam(required = false) String date) {
        try {
            LocalDate targetDate = date != null
                    ? LocalDate.parse(date)
                    : LocalDate.now().minusDays(1);

            log.info("[테스트] 일용직 일급 생성 시작 - 대상 날짜: {}", targetDate);
            salaryGenerationService.generateDailyPayrollForDaily(targetDate);

            return ResponseEntity.ok(ApiResponse.success(
                    "일용직 일급 생성이 완료되었습니다. S3를 확인해주세요.",
                    "대상 날짜: " + targetDate
            ));
        } catch (Exception e) {
            log.error("[테스트] 일용직 일급 생성 실패", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("급여 생성 실패: " + e.getMessage()));
        }
    }

    /**
     * 일용직 주급 생성 테스트
     *
     * @return 생성 결과
     *
     * 사용 예시:
     * GET /api/payroll/test/weekly
     */
    @GetMapping("/weekly")
    public ResponseEntity<ApiResponse<String>> generateWeeklyPayroll() {
        try {
            log.info("[테스트] 일용직 주급 생성 시작");
            salaryGenerationService.generateWeeklyPayrollForDaily();

            return ResponseEntity.ok(ApiResponse.success(
                    "일용직 주급 생성이 완료되었습니다. S3를 확인해주세요.",
                    null
            ));
        } catch (Exception e) {
            log.error("[테스트] 일용직 주급 생성 실패", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("급여 생성 실패: " + e.getMessage()));
        }
    }

    /**
     * 일용직 월급 생성 테스트
     *
     * @return 생성 결과
     *
     * 사용 예시:
     * GET /api/payroll/test/monthly-daily
     */
    @GetMapping("/monthly-daily")
    public ResponseEntity<ApiResponse<String>> generateMonthlyPayrollForDaily() {
        try {
            log.info("[테스트] 일용직 월급 생성 시작");
            salaryGenerationService.generateMonthlyPayrollForDaily();

            return ResponseEntity.ok(ApiResponse.success(
                    "일용직 월급 생성이 완료되었습니다. S3를 확인해주세요.",
                    null
            ));
        } catch (Exception e) {
            log.error("[테스트] 일용직 월급 생성 실패", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("급여 생성 실패: " + e.getMessage()));
        }
    }

    /**
     * 상용직 월급 생성 테스트
     *
     * @return 생성 결과
     *
     * 사용 예시:
     * GET /api/payroll/test/monthly-permanent
     */
    @GetMapping("/monthly-permanent")
    public ResponseEntity<ApiResponse<String>> generateMonthlyPayrollForPermanent() {
        try {
            log.info("[테스트] 상용직 월급 생성 시작");
            salaryGenerationService.generateMonthlyPayrollForPermanent();

            return ResponseEntity.ok(ApiResponse.success(
                    "상용직 월급 생성이 완료되었습니다. S3를 확인해주세요.",
                    null
            ));
        } catch (Exception e) {
            log.error("[테스트] 상용직 월급 생성 실패", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("급여 생성 실패: " + e.getMessage()));
        }
    }

    /**
     * 전체 급여 생성 테스트 (모든 타입)
     *
     * @return 생성 결과
     *
     * 사용 예시:
     * GET /api/payroll/test/all
     */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<String>> generateAllPayrolls() {
        try {
            log.info("[테스트] 전체 급여 생성 시작");

            salaryGenerationService.generateDailyPayrollForDaily();
            salaryGenerationService.generateWeeklyPayrollForDaily();
            salaryGenerationService.generateMonthlyPayrollForDaily();
            salaryGenerationService.generateMonthlyPayrollForPermanent();

            return ResponseEntity.ok(ApiResponse.success(
                    "모든 급여 생성이 완료되었습니다. S3를 확인해주세요.",
                    "일급, 주급, 월급(일용직), 월급(상용직) 모두 생성됨"
            ));
        } catch (Exception e) {
            log.error("[테스트] 전체 급여 생성 실패", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("급여 생성 실패: " + e.getMessage()));
        }
    }
}
