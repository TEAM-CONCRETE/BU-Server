package com.concrete.buildup.domain.payroll.scheduler;

import com.concrete.buildup.domain.payroll.service.SalaryGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 급여명세서 자동 생성 스케줄러
 *
 * 상용직, 일용직(일급/주급/월급)에 따라 급여명세서를 자동으로 생성하고 S3에 저장합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PayrollScheduler {

    private final SalaryGenerationService salaryGenerationService;

    /**
     * 매월 10일 00:00 - 상용직 월급 생성
     * Cron: "초 분 시 일 월 요일"
     */
    @Scheduled(cron = "0 0 0 10 * *")
    public void generateMonthlyPayrollForPermanentWorkers() {
        log.info("[스케줄러] 상용직 월급 생성 시작 - 매월 10일 00:00");

        try {
            salaryGenerationService.generateMonthlyPayrollForPermanent();
            log.info("[스케줄러] 상용직 월급 생성 완료");
        } catch (Exception e) {
            log.error("[스케줄러] 상용직 월급 생성 실패", e);
        }
    }

    /**
     * 매월 10일 00:00 - 일용직 월급 생성
     * 일용직 중 payPeriod = MONTHLY인 근로자 대상
     */
    @Scheduled(cron = "0 0 0 10 * *")
    public void generateMonthlyPayrollForDailyWorkers() {
        log.info("[스케줄러] 일용직 월급 생성 시작 - 매월 10일 00:00");

        try {
            salaryGenerationService.generateMonthlyPayrollForDaily();
            log.info("[스케줄러] 일용직 월급 생성 완료");
        } catch (Exception e) {
            log.error("[스케줄러] 일용직 월급 생성 실패", e);
        }
    }

    /**
     * 매주 일요일 00:00 - 일용직 주급 생성
     * 일용직 중 payPeriod = WEEKLY인 근로자 대상
     * 그 주(월~일)의 근무 데이터 합산
     */
    @Scheduled(cron = "0 0 0 * * SUN")
    public void generateWeeklyPayrollForDailyWorkers() {
        log.info("[스케줄러] 일용직 주급 생성 시작 - 매주 일요일 00:00");

        try {
            salaryGenerationService.generateWeeklyPayrollForDaily();
            log.info("[스케줄러] 일용직 주급 생성 완료");
        } catch (Exception e) {
            log.error("[스케줄러] 일용직 주급 생성 실패", e);
        }
    }

    /**
     * 매일 자정 00:00 - 일용직 일급 생성
     * 일용직 중 payPeriod = DAILY인 근로자 대상
     * 하루 단위로 생성 (근태 기록 기반)
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void generateDailyPayrollForDailyWorkers() {
        log.info("[스케줄러] 일용직 일급 생성 시작 - 매일 00:00");

        try {
            salaryGenerationService.generateDailyPayrollForDaily();
            log.info("[스케줄러] 일용직 일급 생성 완료");
        } catch (Exception e) {
            log.error("[스케줄러] 일용직 일급 생성 실패", e);
        }
    }
}