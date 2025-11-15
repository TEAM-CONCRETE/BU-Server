package com.concrete.buildup.domain.payroll.service;

import com.concrete.buildup.domain.contract.entity.Contract;
import com.concrete.buildup.domain.contract.entity.ContractDetail;
import com.concrete.buildup.domain.contract.enums.PayPeriod;
import com.concrete.buildup.domain.contract.repository.ContractRepository;
import com.concrete.buildup.domain.payroll.entity.Payroll;
import com.concrete.buildup.domain.payroll.enums.PayStatus;
import com.concrete.buildup.domain.payroll.repository.PayrollRepository;
import com.concrete.buildup.domain.payroll.util.PayrollCalculator;
import com.concrete.buildup.domain.payroll.util.PayrollPdfGenerator;
import com.concrete.buildup.domain.upload.service.S3Service;
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
    private final PayrollCalculator payrollCalculator;
    private final PayrollPdfGenerator pdfGenerator;
    private final S3Service s3Service;
    // TODO: AttendanceRepository 구현 후 주입
    // private final AttendanceRepository attendanceRepository;

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

            // TODO: 3. 근무 기록 조회 (AttendanceRepository 구현 후)
            // List<Attendance> attendances = attendanceRepository.findByEmployeeAndPeriod(...);
            // 현재는 계약 상세의 기본 정보로 급여 계산

            // 4. 급여 계산
            BigDecimal hourlyRate = contractDetail.getWorkPay(); // 시급
            BigDecimal workHours = BigDecimal.ZERO; // TODO: 실제 근무시간으로 대체
            BigDecimal nightHours = BigDecimal.ZERO;
            BigDecimal overtimeHours = BigDecimal.ZERO;
            BigDecimal holidayHours = BigDecimal.ZERO;
            BigDecimal weeklyWorkHours = BigDecimal.ZERO;

            // 임시: 상용직 월급은 월 209시간 기준 (주 40시간 * 4.345주)
            if (contract.getEmpType() == com.concrete.buildup.domain.contract.enums.EmpType.PERMANENT
                    && payCycle == PayPeriod.MONTHLY) {
                workHours = new BigDecimal("209");
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
            boolean hasEmploymentInsurance = Boolean.TRUE.equals(contractDetail.getIsEoiApplicable());

            BigDecimal nationalPension = payrollCalculator.calculateNationalPension(totalPay, hasNationalPension);
            BigDecimal healthInsurance = payrollCalculator.calculateHealthInsurance(totalPay, hasHealthInsurance);
            BigDecimal longTermCare = payrollCalculator.calculateLongTermCare(totalPay, hasHealthInsurance);
            BigDecimal employmentInsurance = payrollCalculator.calculateEmploymentInsurance(totalPay, hasEmploymentInsurance);

            // 실수령액 계산
            BigDecimal netPay = payrollCalculator.calculateNetPay(
                    totalPay, incomeTax, residentTax,
                    nationalPension, healthInsurance, longTermCare, employmentInsurance
            );

            // 5. Payroll 엔티티 생성
            LocalDate searchDate = targetMonth.atDay(1); // 지급 기준월
            LocalDate payDueDate = calculatePayDueDate(targetMonth, payCycle); // 지급 예정일

            Payroll payroll = Payroll.builder()
                    .employeeId(contract.getEmployeeId())
                    .contractId(contract.getId())
                    .corporationId(contract.getCorporationId())
                    .salaryYear(targetMonth.getYear())
                    .salaryMonth(targetMonth.getMonthValue())
                    .salaryWeek(week)
                    .salaryDay(day)
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

            // TODO: 7. PayslipItem 저장 (급여 명세 항목)
            // savePayslipItems(savedPayroll.getId(), basePay, nightPay, overtimePay, ...);

            // 8. PDF 생성 및 S3 업로드
            try {
                String s3Key = generateAndUploadPdf(
                        savedPayroll, basePay, nightPay, overtimePay,
                        holidayPay, weeklyHolidayPay,
                        nationalPension, healthInsurance, longTermCare, employmentInsurance,
                        netPay
                );
                savedPayroll.updateS3Key(s3Key);

                log.info("[급여 생성] PDF 생성 및 S3 업로드 완료 - payrollId: {}, s3Key: {}",
                        savedPayroll.getId(), s3Key);
            } catch (Exception e) {
                log.error("[급여 생성] PDF 생성 실패 - payrollId: {}", savedPayroll.getId(), e);
                // PDF 생성 실패해도 급여 데이터는 저장되었으므로 예외를 던지지 않음
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
     * @param longTermCare 장기요양보험
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
                                        BigDecimal longTermCare,
                                        BigDecimal employmentInsurance,
                                        BigDecimal netPay) {
        // 1. PDF 생성
        byte[] pdfBytes = pdfGenerator.generatePayrollPdf(
                payroll, basePay, nightPay, overtimePay, holidayPay, weeklyHolidayPay,
                nationalPension, healthInsurance, longTermCare, employmentInsurance, netPay
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
        s3Service.uploadPdf(s3Key, pdfBytes);

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
}