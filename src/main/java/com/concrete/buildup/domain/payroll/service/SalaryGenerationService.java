package com.concrete.buildup.domain.payroll.service;

import com.concrete.buildup.domain.contract.entity.Contract;
import com.concrete.buildup.domain.contract.entity.ContractDetail;
import com.concrete.buildup.domain.contract.enums.PayPeriod;
import com.concrete.buildup.domain.contract.repository.ContractRepository;
import com.concrete.buildup.domain.payroll.entity.Payroll;
import com.concrete.buildup.domain.payroll.entity.PayslipItem;
import com.concrete.buildup.domain.payroll.enums.ItemType;
import com.concrete.buildup.domain.payroll.enums.PayStatus;
import com.concrete.buildup.domain.payroll.repository.PayrollRepository;
import com.concrete.buildup.domain.payroll.repository.PayslipItemRepository;
import com.concrete.buildup.domain.payroll.util.PayrollCalculator;
import com.concrete.buildup.domain.payroll.util.PayrollPdfGenerator;
import com.concrete.buildup.domain.payroll.dto.PayrollCalculationInput;
import com.concrete.buildup.domain.payroll.dto.PayrollCalculationResult;
import com.concrete.buildup.domain.payroll.dto.OvertimeConditions;
import com.concrete.buildup.domain.payroll.dto.InsuranceEligibility;
import com.concrete.buildup.domain.payroll.dto.InsuranceRates;
import com.concrete.buildup.domain.upload.service.S3Service;
import com.concrete.buildup.domain.attendance.entity.Attendance;
import com.concrete.buildup.domain.attendance.repository.AttendanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

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
    private final PayrollRepository payrollRepository;
    private final PayslipItemRepository payslipItemRepository;
    private final PayrollCalculator payrollCalculator;
    private final PayrollPdfGenerator pdfGenerator;
    private final Optional<S3Service> s3Service;
    private final AttendanceRepository attendanceRepository;

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
        List<Contract> contracts = contractRepository.findPermanentContractsForPayroll(startDate, endDate);

        log.info("[급여 생성] 상용직 대상자: {}명", contracts.size());

        // 3. 각 대상자별 급여 생성
        int successCount = 0;
        int failCount = 0;

        for (Contract contract : contracts) {
            try {
                generatePayrollForContract(contract, targetMonth, PayPeriod.MONTHLY, null, null);
                successCount++;
            } catch (Exception e) {
                log.error("[급여 생성] 급여 생성 실패 - contractId: {}, employeeId: {}",
                        contract.getId(), contract.getEmployeeId(), e);
                failCount++;
            }
        }

        log.info("[급여 생성] 상용직 월급 생성 완료 - 성공: {}명, 실패: {}명", successCount, failCount);
    }

    /**
     * 일용직 월급 생성
     * 일용직 중 payPeriod = MONTHLY인 근로자 대상
     */
    public void generateMonthlyPayrollForDaily() {
        log.info("[급여 생성] 일용직 월급 생성 시작");

        // 1. 급여 대상 기간 계산 (전월)
        YearMonth targetMonth = YearMonth.now().minusMonths(1);
        LocalDate startDate = targetMonth.atDay(1);
        LocalDate endDate = targetMonth.atEndOfMonth();

        log.info("[급여 생성] 대상 기간: {} ~ {}", startDate, endDate);

        // 2. 일용직 월급 대상자 조회 (empType = DAILY, payPeriod = MONTHLY)
        List<Contract> contracts = contractRepository.findDailyContractsForPayrollByPeriod(
                startDate, endDate, PayPeriod.MONTHLY
        );

        log.info("[급여 생성] 일용직 월급 대상자: {}명", contracts.size());

        // 3. 각 대상자별 급여 생성
        int successCount = 0;
        int failCount = 0;

        for (Contract contract : contracts) {
            try {
                generatePayrollForContract(contract, targetMonth, PayPeriod.MONTHLY, null, null);
                successCount++;
            } catch (Exception e) {
                log.error("[급여 생성] 급여 생성 실패 - contractId: {}, employeeId: {}",
                        contract.getId(), contract.getEmployeeId(), e);
                failCount++;
            }
        }

        log.info("[급여 생성] 일용직 월급 생성 완료 - 성공: {}명, 실패: {}명", successCount, failCount);
    }

    /**
     * 일용직 주급 생성
     * 매주 일요일 실행되며, 그 주(월~일)의 근무 데이터를 기반으로 급여명세서를 생성합니다.
     */
    public void generateWeeklyPayrollForDaily() {
        log.info("[급여 생성] 일용직 주급 생성 시작");

        // 1. 지난 주(월~일) 계산
        LocalDate today = LocalDate.now();
        LocalDate lastMonday = today.minusWeeks(1).with(java.time.DayOfWeek.MONDAY);
        LocalDate lastSunday = lastMonday.plusDays(6);

        log.info("[급여 생성] 대상 기간: {} ~ {}", lastMonday, lastSunday);

        // 2. 일용직 주급 대상자 조회 (empType = DAILY, payPeriod = WEEKLY)
        List<Contract> contracts = contractRepository.findDailyContractsForPayrollByPeriod(
                lastMonday, lastSunday, PayPeriod.WEEKLY
        );

        log.info("[급여 생성] 일용직 주급 대상자: {}명", contracts.size());

        // 3. 주차 계산 (해당 주가 속한 월의 몇 번째 주인지)
        YearMonth targetMonth = YearMonth.from(lastMonday);
        int weekOfMonth = calculateWeekOfMonth(lastMonday);

        // 4. 각 대상자별 급여 생성
        int successCount = 0;
        int failCount = 0;

        for (Contract contract : contracts) {
            try {
                generatePayrollForContract(contract, targetMonth, PayPeriod.WEEKLY, weekOfMonth, null);
                successCount++;
            } catch (Exception e) {
                log.error("[급여 생성] 급여 생성 실패 - contractId: {}, employeeId: {}",
                        contract.getId(), contract.getEmployeeId(), e);
                failCount++;
            }
        }

        log.info("[급여 생성] 일용직 주급 생성 완료 - 성공: {}명, 실패: {}명", successCount, failCount);
    }

    /**
     * 일용직 일급 생성 (스케줄러용)
     * 매일 자정 실행되며, 전일 근무 데이터를 기반으로 급여명세서를 생성합니다.
     */
    public void generateDailyPayrollForDaily() {
        generateDailyPayrollForDaily(LocalDate.now().minusDays(1));
    }

    /**
     * 일용직 일급 생성 (특정 날짜 지정)
     * 테스트 및 수동 실행용
     *
     * @param targetDate 급여 생성 대상 일자
     */
    public void generateDailyPayrollForDaily(LocalDate targetDate) {
        log.info("[급여 생성] 일용직 일급 생성 시작");
        log.info("[급여 생성] 대상 일자: {}", targetDate);

        // 2. 일용직 일급 대상자 조회 (empType = DAILY, payPeriod = DAILY)
        List<Contract> contracts = contractRepository.findDailyContractsForPayrollByPeriod(
                targetDate, targetDate, PayPeriod.DAILY
        );

        log.info("[급여 생성] 일용직 일급 대상자: {}명", contracts.size());

        // 3. 대상 월 계산
        YearMonth targetMonth = YearMonth.from(targetDate);

        // 4. 각 대상자별 급여 생성
        int successCount = 0;
        int failCount = 0;

        for (Contract contract : contracts) {
            try {
                generatePayrollForContract(contract, targetMonth, PayPeriod.DAILY, null, targetDate);
                successCount++;
            } catch (Exception e) {
                log.error("[급여 생성] 급여 생성 실패 - contractId: {}, employeeId: {}",
                        contract.getId(), contract.getEmployeeId(), e);
                failCount++;
            }
        }

        log.info("[급여 생성] 일용직 일급 생성 완료 - 성공: {}명, 실패: {}명", successCount, failCount);
    }

    /**
     * 개별 계약에 대한 급여 생성
     *
     * @param contract 근로계약
     * @param targetMonth 급여 대상 월
     * @param payCycle 급여 주기
     * @param week 주차 (주급인 경우)
     * @param day 일자 (일급인 경우)
     */
    private void generatePayrollForContract(Contract contract, YearMonth targetMonth,
                                           PayPeriod payCycle, Integer week, LocalDate day) {
        try {
            // 1. 중복 체크
            Optional<Payroll> existing = payrollRepository.findByEmployeeAndPeriod(
                    contract.getEmployeeId(),
                    targetMonth.getYear(),
                    targetMonth.getMonthValue(),
                    payCycle,
                    week,
                    day
            );

            if (existing.isPresent()) {
                log.warn("[급여 생성] 이미 생성된 급여 존재 - employeeId: {}, period: {}-{}",
                        contract.getEmployeeId(), targetMonth.getYear(), targetMonth.getMonthValue());
                return;
            }

            // 2. 계약 상세 정보 조회
            ContractDetail contractDetail = contract.getContractDetail();
            if (contractDetail == null) {
                log.error("[급여 생성] 계약 상세 정보 없음 - contractId: {}", contract.getId());
                return;
            }

            // 3. 근무 기록 조회 및 집계
            LocalDate startDate;
            LocalDate endDate;

            if (payCycle == PayPeriod.DAILY && day != null) {
                // 일급: 해당 일자만 조회
                startDate = day;
                endDate = day;
            } else if (payCycle == PayPeriod.WEEKLY && week != null) {
                // 주급: 해당 주차의 월요일~일요일 조회
                LocalDate firstDay = targetMonth.atDay(1);
                int firstDayOfWeek = firstDay.getDayOfWeek().getValue();
                int daysToFirstMonday = (8 - firstDayOfWeek) % 7;
                LocalDate firstMonday = firstDay.plusDays(daysToFirstMonday);
                startDate = firstMonday.plusWeeks(week - 1);
                endDate = startDate.plusDays(6);
            } else {
                // 월급: 해당 월 전체 조회
                startDate = targetMonth.atDay(1);
                endDate = targetMonth.atEndOfMonth();
            }

            List<Attendance> attendances = attendanceRepository.findNormalAttendancesForPayroll(
                    contract.getEmployeeId(),
                    startDate,
                    endDate
            );

            log.info("[급여 생성] 근태 기록 조회 - employeeId: {}, 조회 기간: {} ~ {}, 정상 출근 일수: {}일",
                    contract.getEmployeeId(), startDate, endDate, attendances.size());

            // 4. 급여 계산
            BigDecimal hourlyRate = contractDetail.getWorkPay(); // 시급
            BigDecimal workHours = BigDecimal.ZERO;
            BigDecimal nightHours = BigDecimal.ZERO;
            BigDecimal overtimeHours = BigDecimal.ZERO;
            BigDecimal holidayHours = BigDecimal.ZERO;
            BigDecimal weeklyHolidayPay = BigDecimal.ZERO;

            // 출퇴근 기록이 있으면 실제 근무시간 집계
            if (!attendances.isEmpty()) {
                // 주차별 근무시간 집계를 위한 Map (week -> hours)
                java.util.Map<Integer, BigDecimal> weeklyHoursMap = new java.util.HashMap<>();

                for (Attendance attendance : attendances) {
                    BigDecimal dailyWorkHour = attendance.getTotalWorkHour() != null
                            ? attendance.getTotalWorkHour() : BigDecimal.ZERO;

                    workHours = workHours.add(dailyWorkHour);
                    nightHours = nightHours.add(attendance.getNightWorkHour() != null
                            ? attendance.getNightWorkHour() : BigDecimal.ZERO);
                    overtimeHours = overtimeHours.add(attendance.getAdditionalWorkHour() != null
                            ? attendance.getAdditionalWorkHour() : BigDecimal.ZERO);
                    holidayHours = holidayHours.add(attendance.getHolidayWorkHour() != null
                            ? attendance.getHolidayWorkHour() : BigDecimal.ZERO);

                    // 주차별 근무시간 집계
                    if (attendance.getSearchDate() != null) {
                        int weekNum = calculateWeekOfMonth(attendance.getSearchDate());
                        weeklyHoursMap.merge(weekNum, dailyWorkHour, BigDecimal::add);
                    }
                }

                // 주휴수당은 주차별로 계산하여 합산
                for (BigDecimal weekHours : weeklyHoursMap.values()) {
                    weeklyHolidayPay = weeklyHolidayPay.add(
                            payrollCalculator.calculateWeeklyHolidayPay(hourlyRate, weekHours)
                    );
                }

                log.info("[급여 생성] 근무시간 집계 - 총: {}h, 야간: {}h, 연장: {}h, 휴일: {}h",
                        workHours, nightHours, overtimeHours, holidayHours);
                log.info("[급여 생성] 주차별 근무시간: {}, 주휴수당 합계: {}원",
                        weeklyHoursMap, weeklyHolidayPay);
            } else {
                // 출퇴근 기록이 없는 경우
                log.warn("[급여 생성] 근태 기록 없음 - contractId: {}, employeeId: {}, empType: {}, payCycle: {}",
                        contract.getId(), contract.getEmployeeId(), contract.getEmpType(), payCycle);

                // 상용직 월급은 월 209시간 기준 (주 40시간 * 4.345주)
                if (contract.getEmpType() == com.concrete.buildup.domain.contract.enums.EmpType.PERMANENT
                        && payCycle == PayPeriod.MONTHLY) {
                    log.info("[급여 생성] 상용직 월급 기본값 적용 - 209시간");
                    workHours = new BigDecimal("209");
                    // 상용직은 주 40시간 기준으로 4주치 주휴수당 계산
                    weeklyHolidayPay = payrollCalculator.calculateWeeklyHolidayPay(
                            hourlyRate, new BigDecimal("40")
                    ).multiply(new BigDecimal("4"));
                } else {
                    // 일용직은 근태 기록이 없으면 급여를 생성하지 않음
                    log.error("[급여 생성] 일용직 근태 기록 없음 - 급여 생성 중단 (contractId: {}, employeeId: {})",
                            contract.getId(), contract.getEmployeeId());
                    return;
                }
            }

            // ========== v1.1 급여 계산 ==========

            // 4대보험 가입 여부 조회
            boolean hasNationalPension = Boolean.TRUE.equals(contractDetail.getIsNpsApplicable());
            boolean hasHealthInsurance = Boolean.TRUE.equals(contractDetail.getIsNhiApplicable());
            boolean hasEmploymentInsurance = Boolean.TRUE.equals(contractDetail.getIsEoiApplicable());

            // 가산수당 발생 조건 판단
            boolean exceedsDailyLimit = overtimeHours.compareTo(BigDecimal.ZERO) > 0;
            boolean exceedsWeeklyLimit = false; // 주 40시간 초과 여부는 주차별 집계가 필요하므로 기본값 false
            boolean hasNightWork = nightHours.compareTo(BigDecimal.ZERO) > 0;
            boolean hasHolidayWork = holidayHours.compareTo(BigDecimal.ZERO) > 0;

            // 일용직의 경우: monthlyWorkDaysAccumulated는 workDays 값 사용 (4대보험 가입 조건 판단용)
            // 상용직의 경우: monthlyWorkDaysAccumulated는 사용하지 않으므로 0으로 설정
            int workDaysCount = attendances.size();
            int monthlyWorkDaysAccumulated = contract.getEmpType() == com.concrete.buildup.domain.contract.enums.EmpType.DAILY
                    ? workDaysCount  // 일용직: 해당 기간의 근로일수 사용
                    : 0;  // 상용직: 사용하지 않음

            // v1.1 계산 입력 DTO 생성
            PayrollCalculationInput calculationInput = PayrollCalculationInput.builder()
                    .empType(contract.getEmpType())
                    .payPeriod(payCycle)
                    .hourlyRate(hourlyRate)
                    .dailyWorkHours(new BigDecimal("8")) // 기본 일 근무시간 (계약상 기준)
                    .workDays(workDaysCount)
                    .overtimeHours(overtimeHours)
                    .nightHours(nightHours)
                    .holidayHours(holidayHours)
                    .weeklyHolidayEligible(true) // 주휴수당 자격 기본값 true
                    .insurance(InsuranceEligibility.builder()
                            .employmentInsurance(hasEmploymentInsurance)
                            .healthInsurance(hasHealthInsurance)
                            .nationalPension(hasNationalPension)
                            .build())
                    .dependents(0) // 부양가족 수 기본값 0 (향후 Employee 정보에서 가져올 수 있음)
                    .rates(InsuranceRates.defaultRates2025())
                    .overtimeConditions(OvertimeConditions.builder()
                            .totalEmployees(5) // DEFAULT: 5인 이상 사업장으로 가정 (가산수당 의무 지급)
                            .autoPayOvertime(true) // DEFAULT: 연장수당 자율 지급
                            .autoPayNight(true) // DEFAULT: 야간수당 자율 지급
                            .autoPayHoliday(true) // DEFAULT: 휴일수당 자율 지급
                            .exceedsDailyLimit(exceedsDailyLimit)
                            .exceedsWeeklyLimit(exceedsWeeklyLimit)
                            .hasNightWork(hasNightWork)
                            .hasHolidayWork(hasHolidayWork)
                            .build())
                    .monthlyWorkDaysAccumulated(monthlyWorkDaysAccumulated) // 일용직: workDays 값 사용, 상용직: 0
                    .monthlyEstimatedIncome(BigDecimal.ZERO) // DEFAULT: 월 추정소득 (향후 계산 가능)
                    .build();

            // v1.1 급여 계산 실행
            PayrollCalculationResult calculationResult = payrollCalculator.calculate(calculationInput);

            // 계산 결과 추출
            BigDecimal basePay = calculationResult.getBasePay();
            BigDecimal nightPay = calculationResult.getNightPay();
            BigDecimal overtimePay = calculationResult.getOvertimePay();
            BigDecimal holidayPay = calculationResult.getHolidayPay();
            // v1.1에서 계산된 주휴수당 사용
            weeklyHolidayPay = calculationResult.getWeeklyHolidayPay();
            BigDecimal totalPay = calculationResult.getTotalPay();

            // 공제 항목 추출
            BigDecimal incomeTax = calculationResult.getIncomeTax();
            BigDecimal residentTax = calculationResult.getResidentTax();
            BigDecimal nationalPension = calculationResult.getNationalPension();
            BigDecimal healthInsurance = calculationResult.getHealthInsurance();
            BigDecimal longTermCareInsurance = calculationResult.getLongTermCareInsurance();
            BigDecimal employmentInsurance = calculationResult.getEmploymentInsurance();

            // 실수령액
            BigDecimal netPay = calculationResult.getNetPay();

            // 비과세 소득 (기존 로직 유지 - v1.1에는 없음)
            BigDecimal nonTaxIncome = payrollCalculator.calculateNonTaxableIncome(contractDetail);

            // 산재보험은 v1.1에 없으므로 기존 로직 사용
            boolean hasWorkersCompInsurance = Boolean.TRUE.equals(contractDetail.getIsWciApplicable());
            BigDecimal workersCompInsurance = payrollCalculator.calculateWorkersCompInsurance(totalPay, hasWorkersCompInsurance);

            log.info("[급여 생성] v1.1 계산 완료 - basePay: {}, overtimePay: {}, nightPay: {}, holidayPay: {}, weeklyHolidayPay: {}, totalPay: {}, netPay: {}",
                    basePay, overtimePay, nightPay, holidayPay, weeklyHolidayPay, totalPay, netPay);

            // 5. Payroll 엔티티 생성
            LocalDate searchDate = targetMonth.atDay(1); // 지급 기준월
            LocalDate payDueDate = calculatePayDueDate(targetMonth, payCycle); // 지급 예정일

            // 센티널 값 적용
            Integer finalWeek = (week != null) ? week : 0; // 월급/일급인 경우 0
            LocalDate finalDay = (day != null) ? day : targetMonth.atDay(1); // 월급/주급인 경우 해당 월의 1일

            Payroll payroll = Payroll.builder()
                    .employeeId(contract.getEmployeeId())
                    .contractId(contract.getId())
                    .corporationId(contract.getCorporationId())
                    .salaryYear(targetMonth.getYear())
                    .salaryMonth(targetMonth.getMonthValue())
                    .salaryWeek(finalWeek)
                    .salaryDay(finalDay)
                    .searchDate(searchDate)
                    .payDueDate(payDueDate)
                    .empType(contract.getEmpType())
                    .empName(contractDetail.getEmpName())
                    .residentNum(null) // TODO: Employee 정보에서 가져오기
                    .totalWorkHour(workHours)
                    .totalPay(totalPay)
                    .totalPayByDay(basePay) // 일급 기준 지급액
                    .noneTaxIncome(nonTaxIncome)
                    .incomeTax(incomeTax)
                    .residentTax(residentTax)
                    .payCycle(payCycle)
                    .payStatus(PayStatus.PENDING)
                    .generatedAt(LocalDateTime.now())
                    .build();

            // 6. Payroll 저장
            Payroll savedPayroll = payrollRepository.save(payroll);

            log.info("[급여 생성] 급여 저장 완료 - payrollId: {}, employeeId: {}, totalPay: {}, netPay: {}",
                    savedPayroll.getId(), contract.getEmployeeId(), totalPay, netPay);

            // 7. PayslipItem 저장 (급여 명세 항목)
            savePayslipItems(
                    savedPayroll.getId(),
                    searchDate,
                    basePay, nightPay, overtimePay, holidayPay, weeklyHolidayPay,
                    incomeTax, residentTax,
                    nationalPension, healthInsurance, workersCompInsurance, employmentInsurance
            );

            // 8. PDF 생성 및 S3 업로드
            if (s3Service.isPresent()) {
                try {
                    String s3Key = generateAndUploadPdf(
                            savedPayroll, basePay, nightPay, overtimePay,
                            holidayPay, weeklyHolidayPay,
                            nationalPension, healthInsurance, workersCompInsurance, employmentInsurance,
                            netPay
                    );
                    savedPayroll.updateS3Key(s3Key);

                    log.info("[급여 생성] PDF 생성 및 S3 업로드 완료 - payrollId: {}, s3Key: {}",
                            savedPayroll.getId(), s3Key);
                } catch (Exception e) {
                    log.error("[급여 생성] PDF 생성 실패 - payrollId: {}", savedPayroll.getId(), e);
                    // PDF 생성 실패해도 급여 데이터는 저장되었으므로 예외를 던지지 않음
                }
            } else {
                log.warn("[급여 생성] S3 서비스가 비활성화되어 PDF 업로드를 건너뜁니다 - payrollId: {}", savedPayroll.getId());
                log.warn("[급여 생성] S3를 활성화하려면 AWS_S3_ENABLED=true 환경 변수를 설정하세요.");
            }

        } catch (Exception e) {
            log.error("[급여 생성] 급여 생성 중 오류 발생 - contractId: {}, employeeId: {}",
                    contract.getId(), contract.getEmployeeId(), e);
            throw e;
        }
    }

    /**
     * PDF 생성 및 S3 업로드
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
     * @return S3 키 (파일 경로)
     */
    private String generateAndUploadPdf(Payroll payroll,
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
        // 1. PDF 생성
        byte[] pdfBytes = pdfGenerator.generatePayrollPdf(
                payroll, basePay, nightPay, overtimePay, holidayPay, weeklyHolidayPay,
                nationalPension, healthInsurance, workersCompInsurance, employmentInsurance, netPay
        );

        // 2. S3 키 생성
        // payroll/EMP{employeeId}/{year}/{month}/payslip-{payrollId}.pdf
        String s3Key = String.format("payroll/EMP%d/%d/%02d/payslip-%d.pdf",
                payroll.getEmployeeId(),
                payroll.getSalaryYear(),
                payroll.getSalaryMonth(),
                payroll.getId()
        );

        // 3. S3 업로드
        s3Service.ifPresent(service -> service.uploadPdf(s3Key, pdfBytes));

        return s3Key;
    }

    /**
     * 지급 예정일 계산
     *
     * @param targetMonth 급여 대상 월
     * @param payCycle 급여 주기
     * @return 지급 예정일
     */
    private LocalDate calculatePayDueDate(YearMonth targetMonth, PayPeriod payCycle) {
        // 일반적으로 급여는 다음달 10일에 지급
        YearMonth nextMonth = targetMonth.plusMonths(1);
        return nextMonth.atDay(10);
    }

    /**
     * 해당 날짜가 속한 월의 몇 번째 주인지 계산
     *
     * @param date 날짜
     * @return 주차 (1~5)
     */
    private int calculateWeekOfMonth(LocalDate date) {
        // 해당 월의 첫 날
        LocalDate firstDayOfMonth = date.withDayOfMonth(1);

        // 첫 날부터 해당 날짜까지의 일수
        int daysSinceFirstDay = date.getDayOfMonth();

        // 첫 날의 요일 (월요일 = 1, 일요일 = 7)
        int firstDayOfWeek = firstDayOfMonth.getDayOfWeek().getValue();

        // 주차 계산 (첫 주는 1주차)
        // (일수 + 첫날요일 - 1) / 7 + 1
        return (daysSinceFirstDay + firstDayOfWeek - 2) / 7 + 1;
    }

    /**
     * 급여 명세 항목 저장
     *
     * PayslipItem 테이블에 급여 세부 항목들을 저장합니다.
     * - 지급 항목: 기본급, 야간근로수당, 연장근로수당, 휴일근로수당, 주휴수당
     * - 공제 항목: 소득세, 주민세, 4대보험 (국민연금, 건강보험, 산재보험, 고용보험)
     *
     * @param payrollId 급여 ID
     * @param effectiveDate 적용 날짜
     * @param basePay 기본급
     * @param nightPay 야간근로수당
     * @param overtimePay 연장근로수당
     * @param holidayPay 휴일근로수당
     * @param weeklyHolidayPay 주휴수당
     * @param incomeTax 소득세
     * @param residentTax 주민세
     * @param nationalPension 국민연금
     * @param healthInsurance 건강보험
     * @param workersCompInsurance 산재보험
     * @param employmentInsurance 고용보험
     */
    private void savePayslipItems(Long payrollId, LocalDate effectiveDate,
                                   BigDecimal basePay, BigDecimal nightPay, BigDecimal overtimePay,
                                   BigDecimal holidayPay, BigDecimal weeklyHolidayPay,
                                   BigDecimal incomeTax, BigDecimal residentTax,
                                   BigDecimal nationalPension, BigDecimal healthInsurance,
                                   BigDecimal workersCompInsurance, BigDecimal employmentInsurance) {

        List<PayslipItem> items = new java.util.ArrayList<>();

        // ========== 지급 항목 (EARNING) ==========

        // 기본급 (항상 저장)
        items.add(PayslipItem.builder()
                .payrollId(payrollId)
                .itemName("기본급")
                .itemType(ItemType.EARNING)
                .amount(basePay)
                .effectiveDate(effectiveDate)
                .build());

        // 야간근로수당 (0보다 크면 저장)
        if (nightPay.compareTo(BigDecimal.ZERO) > 0) {
            items.add(PayslipItem.builder()
                    .payrollId(payrollId)
                    .itemName("야간근로수당")
                    .itemType(ItemType.EARNING)
                    .amount(nightPay)
                    .effectiveDate(effectiveDate)
                    .build());
        }

        // 연장근로수당 (0보다 크면 저장)
        if (overtimePay.compareTo(BigDecimal.ZERO) > 0) {
            items.add(PayslipItem.builder()
                    .payrollId(payrollId)
                    .itemName("연장근로수당")
                    .itemType(ItemType.EARNING)
                    .amount(overtimePay)
                    .effectiveDate(effectiveDate)
                    .build());
        }

        // 휴일근로수당 (0보다 크면 저장)
        if (holidayPay.compareTo(BigDecimal.ZERO) > 0) {
            items.add(PayslipItem.builder()
                    .payrollId(payrollId)
                    .itemName("휴일근로수당")
                    .itemType(ItemType.EARNING)
                    .amount(holidayPay)
                    .effectiveDate(effectiveDate)
                    .build());
        }

        // 주휴수당 (0보다 크면 저장)
        if (weeklyHolidayPay.compareTo(BigDecimal.ZERO) > 0) {
            items.add(PayslipItem.builder()
                    .payrollId(payrollId)
                    .itemName("주휴수당")
                    .itemType(ItemType.EARNING)
                    .amount(weeklyHolidayPay)
                    .effectiveDate(effectiveDate)
                    .build());
        }

        // ========== 공제 항목 (DEDUCTION) ==========

        // 소득세 (항상 저장)
        items.add(PayslipItem.builder()
                .payrollId(payrollId)
                .itemName("소득세")
                .itemType(ItemType.DEDUCTION)
                .amount(incomeTax)
                .effectiveDate(effectiveDate)
                .build());

        // 주민세 (항상 저장)
        items.add(PayslipItem.builder()
                .payrollId(payrollId)
                .itemName("주민세")
                .itemType(ItemType.DEDUCTION)
                .amount(residentTax)
                .effectiveDate(effectiveDate)
                .build());

        // 국민연금 (0보다 크면 저장)
        if (nationalPension.compareTo(BigDecimal.ZERO) > 0) {
            items.add(PayslipItem.builder()
                    .payrollId(payrollId)
                    .itemName("국민연금")
                    .itemType(ItemType.DEDUCTION)
                    .amount(nationalPension)
                    .effectiveDate(effectiveDate)
                    .build());
        }

        // 건강보험 (0보다 크면 저장)
        if (healthInsurance.compareTo(BigDecimal.ZERO) > 0) {
            items.add(PayslipItem.builder()
                    .payrollId(payrollId)
                    .itemName("건강보험")
                    .itemType(ItemType.DEDUCTION)
                    .amount(healthInsurance)
                    .effectiveDate(effectiveDate)
                    .build());
        }

        // 산재보험 (0보다 크면 저장)
        if (workersCompInsurance.compareTo(BigDecimal.ZERO) > 0) {
            items.add(PayslipItem.builder()
                    .payrollId(payrollId)
                    .itemName("산재보험")
                    .itemType(ItemType.DEDUCTION)
                    .amount(workersCompInsurance)
                    .effectiveDate(effectiveDate)
                    .build());
        }

        // 고용보험 (0보다 크면 저장)
        if (employmentInsurance.compareTo(BigDecimal.ZERO) > 0) {
            items.add(PayslipItem.builder()
                    .payrollId(payrollId)
                    .itemName("고용보험")
                    .itemType(ItemType.DEDUCTION)
                    .amount(employmentInsurance)
                    .effectiveDate(effectiveDate)
                    .build());
        }

        // 일괄 저장
        payslipItemRepository.saveAll(items);

        log.info("[급여 생성] 급여 명세 항목 저장 완료 - payrollId: {}, 항목 수: {}개",
                payrollId, items.size());
    }
}