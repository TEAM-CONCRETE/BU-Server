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
     * 일용직 일급 생성
     * 매일 자정 실행되며, 전일 근무 데이터를 기반으로 급여명세서를 생성합니다.
     */
    public void generateDailyPayrollForDaily() {
        log.info("[급여 생성] 일용직 일급 생성 시작");

        // 1. 어제 날짜
        LocalDate yesterday = LocalDate.now().minusDays(1);

        log.info("[급여 생성] 대상 일자: {}", yesterday);

        // 2. 일용직 일급 대상자 조회 (empType = DAILY, payPeriod = DAILY)
        List<Contract> contracts = contractRepository.findDailyContractsForPayrollByPeriod(
                yesterday, yesterday, PayPeriod.DAILY
        );

        log.info("[급여 생성] 일용직 일급 대상자: {}명", contracts.size());

        // 3. 대상 월 계산
        YearMonth targetMonth = YearMonth.from(yesterday);

        // 4. 각 대상자별 급여 생성
        int successCount = 0;
        int failCount = 0;

        for (Contract contract : contracts) {
            try {
                generatePayrollForContract(contract, targetMonth, PayPeriod.DAILY, null, yesterday);
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
            LocalDate startDate = targetMonth.atDay(1);
            LocalDate endDate = targetMonth.atEndOfMonth();

            List<Attendance> attendances = attendanceRepository.findNormalAttendancesForPayroll(
                    contract.getEmployeeId(),
                    startDate,
                    endDate
            );

            log.info("[급여 생성] 근태 기록 조회 - employeeId: {}, 정상 출근 일수: {}일",
                    contract.getEmployeeId(), attendances.size());

            // 4. 급여 계산
            BigDecimal hourlyRate = contractDetail.getWorkPay(); // 시급
            BigDecimal workHours = BigDecimal.ZERO;
            BigDecimal nightHours = BigDecimal.ZERO;
            BigDecimal overtimeHours = BigDecimal.ZERO;
            BigDecimal holidayHours = BigDecimal.ZERO;
            BigDecimal weeklyWorkHours = BigDecimal.ZERO;

            // 출퇴근 기록이 있으면 실제 근무시간 집계
            if (!attendances.isEmpty()) {
                for (Attendance attendance : attendances) {
                    BigDecimal dailyWorkHour = attendance.getTotalWorkHour() != null
                            ? attendance.getTotalWorkHour() : BigDecimal.ZERO;

                    workHours = workHours.add(dailyWorkHour);
                    weeklyWorkHours = weeklyWorkHours.add(dailyWorkHour); // 주휴수당 계산용
                    nightHours = nightHours.add(attendance.getNightWorkHour() != null
                            ? attendance.getNightWorkHour() : BigDecimal.ZERO);
                    overtimeHours = overtimeHours.add(attendance.getAdditionalWorkHour() != null
                            ? attendance.getAdditionalWorkHour() : BigDecimal.ZERO);
                    holidayHours = holidayHours.add(attendance.getHolidayWorkHour() != null
                            ? attendance.getHolidayWorkHour() : BigDecimal.ZERO);
                }

                log.info("[급여 생성] 근무시간 집계 - 총: {}h, 주간: {}h, 야간: {}h, 연장: {}h, 휴일: {}h",
                        workHours, weeklyWorkHours, nightHours, overtimeHours, holidayHours);
            } else {
                // 출퇴근 기록이 없는 경우
                log.warn("[급여 생성] 근태 기록 없음 - contractId: {}, employeeId: {}, empType: {}, payCycle: {}",
                        contract.getId(), contract.getEmployeeId(), contract.getEmpType(), payCycle);

                // 상용직 월급은 월 209시간 기준 (주 40시간 * 4.345주)
                if (contract.getEmpType() == com.concrete.buildup.domain.contract.enums.EmpType.PERMANENT
                        && payCycle == PayPeriod.MONTHLY) {
                    log.info("[급여 생성] 상용직 월급 기본값 적용 - 209시간");
                    workHours = new BigDecimal("209");
                    weeklyWorkHours = new BigDecimal("40"); // 주 40시간 기준
                } else {
                    // 일용직은 근태 기록이 없으면 급여를 생성하지 않음
                    log.error("[급여 생성] 일용직 근태 기록 없음 - 급여 생성 중단 (contractId: {}, employeeId: {})",
                            contract.getId(), contract.getEmployeeId());
                    return;
                }
            }

            // 급여 항목별 계산
            BigDecimal basePay = payrollCalculator.calculateBasePay(hourlyRate, workHours);
            BigDecimal nightPay = payrollCalculator.calculateNightWorkPay(hourlyRate, nightHours);
            BigDecimal overtimePay = payrollCalculator.calculateOvertimePay(hourlyRate, overtimeHours);
            BigDecimal holidayPay = payrollCalculator.calculateHolidayPay(hourlyRate, holidayHours);
            BigDecimal weeklyHolidayPay = payrollCalculator.calculateWeeklyHolidayPay(hourlyRate, weeklyWorkHours);

            // 총 지급액 계산
            BigDecimal totalPay = basePay.add(nightPay).add(overtimePay)
                    .add(holidayPay).add(weeklyHolidayPay);

            // 비과세 소득 계산
            BigDecimal nonTaxIncome = payrollCalculator.calculateNonTaxableIncome(contractDetail);

            // 과세 소득 = 총 지급액 - 비과세 소득
            BigDecimal taxableIncome = totalPay.subtract(nonTaxIncome);

            // 세금 계산
            BigDecimal incomeTax = payrollCalculator.calculateIncomeTax(taxableIncome);
            BigDecimal residentTax = payrollCalculator.calculateResidentTax(taxableIncome);

            // 4대보험 계산
            boolean hasNationalPension = Boolean.TRUE.equals(contractDetail.getIsNpsApplicable());
            boolean hasHealthInsurance = Boolean.TRUE.equals(contractDetail.getIsNhiApplicable());
            boolean hasWorkersCompInsurance = Boolean.TRUE.equals(contractDetail.getIsWciApplicable());
            boolean hasEmploymentInsurance = Boolean.TRUE.equals(contractDetail.getIsEoiApplicable());

            BigDecimal nationalPension = payrollCalculator.calculateNationalPension(totalPay, hasNationalPension);
            BigDecimal healthInsurance = payrollCalculator.calculateHealthInsurance(totalPay, hasHealthInsurance);
            BigDecimal workersCompInsurance = payrollCalculator.calculateWorkersCompInsurance(totalPay, hasWorkersCompInsurance);
            BigDecimal employmentInsurance = payrollCalculator.calculateEmploymentInsurance(totalPay, hasEmploymentInsurance);

            // 실수령액 계산
            BigDecimal netPay = payrollCalculator.calculateNetPay(
                    totalPay, incomeTax, residentTax,
                    nationalPension, healthInsurance, workersCompInsurance, employmentInsurance
            );

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
                    basePay, nightPay, overtimePay,
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
     * - 지급 항목: 기본급, 연장근로수당, 야간근로수당
     * - 공제 항목: 소득세, 주민세, 4대보험 (국민연금, 건강보험, 산재보험, 고용보험)
     *
     * @param payrollId 급여 ID
     * @param effectiveDate 적용 날짜
     * @param basePay 기본급
     * @param nightPay 야간근로수당
     * @param overtimePay 연장근로수당
     * @param incomeTax 소득세
     * @param residentTax 주민세
     * @param nationalPension 국민연금
     * @param healthInsurance 건강보험
     * @param workersCompInsurance 산재보험
     * @param employmentInsurance 고용보험
     */
    private void savePayslipItems(Long payrollId, LocalDate effectiveDate,
                                   BigDecimal basePay, BigDecimal nightPay, BigDecimal overtimePay,
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