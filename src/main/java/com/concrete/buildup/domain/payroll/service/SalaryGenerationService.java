package com.concrete.buildup.domain.payroll.service;

import com.concrete.buildup.domain.contract.entity.Contract;
import com.concrete.buildup.domain.contract.enums.ContractState;
import com.concrete.buildup.domain.contract.enums.EmpType;
import com.concrete.buildup.domain.contract.enums.PayPeriod;
import com.concrete.buildup.domain.contract.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * 급여 자동 생성 서비스
 *
 * 스케줄러에서 호출되어 근로자별 급여명세서를 자동으로 생성하고 PDF로 저장합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SalaryGenerationService {

    private final ContractRepository contractRepository;
    // TODO: 추가 Repository 및 Service 주입
    // private final AttendanceRepository attendanceRepository;
    // private final PayrollRepository payrollRepository;
    // private final PayrollCalculator payrollCalculator;
    // private final S3Service s3Service;

    /**
     * 상용직 월급 생성
     * 매월 10일 실행되며, 전월 근무 데이터를 기반으로 급여명세서를 생성합니다.
     */
    public void generateMonthlyPayrollForPermanent() {
        log.info("[급여 생성] 상용직 월급 생성 시작");

        // 1. 급여 대상 기간 계산 (전월)
        YearMonth targetMonth = YearMonth.now().minusMonths(1);
        LocalDate startDate = targetMonth.atDay(1);
        LocalDate endDate = targetMonth.atEndOfMonth();

        log.info("[급여 생성] 대상 기간: {} ~ {}", startDate, endDate);

        // 2. 상용직 대상자 조회
        List<Contract> contracts = contractRepository.findAll(); // TODO: 조건 쿼리 구현 필요

        // TODO: 필터링 로직 구현
        // - empType = PERMANENT
        // - contractState = FULLY_SIGNED
        // - employeeStartDate <= endDate
        // - employeeEndDate >= startDate (또는 NULL)

        log.info("[급여 생성] 상용직 대상자: {}명", contracts.size());

        // TODO: 각 대상자별 급여 생성 로직
        // for (Contract contract : contracts) {
        //     generatePayrollForContract(contract, targetMonth);
        // }

        log.info("[급여 생성] 상용직 월급 생성 완료");
    }

    /**
     * 일용직 월급 생성
     * 일용직 중 payPeriod = MONTHLY인 근로자 대상
     */
    public void generateMonthlyPayrollForDaily() {
        log.info("[급여 생성] 일용직 월급 생성 시작");

        YearMonth targetMonth = YearMonth.now().minusMonths(1);
        LocalDate startDate = targetMonth.atDay(1);
        LocalDate endDate = targetMonth.atEndOfMonth();

        log.info("[급여 생성] 대상 기간: {} ~ {}", startDate, endDate);

        // TODO: 일용직 월급 대상자 조회 및 생성
        // - empType = DAILY
        // - payPeriod = MONTHLY

        log.info("[급여 생성] 일용직 월급 생성 완료");
    }

    /**
     * 일용직 주급 생성
     * 매주 일요일 실행되며, 그 주(월~일)의 근무 데이터를 기반으로 급여명세서를 생성합니다.
     */
    public void generateWeeklyPayrollForDaily() {
        log.info("[급여 생성] 일용직 주급 생성 시작");

        // 지난 주(월~일) 계산
        LocalDate today = LocalDate.now();
        LocalDate lastMonday = today.minusWeeks(1).with(java.time.DayOfWeek.MONDAY);
        LocalDate lastSunday = lastMonday.plusDays(6);

        log.info("[급여 생성] 대상 기간: {} ~ {}", lastMonday, lastSunday);

        // TODO: 일용직 주급 대상자 조회 및 생성
        // - empType = DAILY
        // - payPeriod = WEEKLY

        log.info("[급여 생성] 일용직 주급 생성 완료");
    }

    /**
     * 일용직 일급 생성
     * 매일 자정 실행되며, 전일 근무 데이터를 기반으로 급여명세서를 생성합니다.
     */
    public void generateDailyPayrollForDaily() {
        log.info("[급여 생성] 일용직 일급 생성 시작");

        // 어제 날짜
        LocalDate yesterday = LocalDate.now().minusDays(1);

        log.info("[급여 생성] 대상 일자: {}", yesterday);

        // TODO: 일용직 일급 대상자 조회 및 생성
        // - empType = DAILY
        // - payPeriod = DAILY

        log.info("[급여 생성] 일용직 일급 생성 완료");
    }

    // TODO: 개별 계약에 대한 급여 생성 로직
    // private void generatePayrollForContract(Contract contract, YearMonth targetMonth) {
    //     1. 근무 기록 조회 (AttendanceRepository)
    //     2. 급여 계산 (PayrollCalculator)
    //     3. PayrollMaster/Detail 저장
    //     4. PDF 생성
    //     5. S3 업로드
    //     6. PayrollMaster.s3Key 업데이트
    // }
}