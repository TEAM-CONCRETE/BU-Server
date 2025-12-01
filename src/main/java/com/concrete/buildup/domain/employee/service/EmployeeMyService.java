package com.concrete.buildup.domain.employee.service;

import com.concrete.buildup.domain.attendance.entity.Attendance;
import com.concrete.buildup.domain.attendance.repository.AttendanceRepository;
import com.concrete.buildup.domain.auth.entity.Employee;
import com.concrete.buildup.domain.auth.entity.User;
import com.concrete.buildup.domain.auth.repository.EmployeeRepository;
import com.concrete.buildup.domain.auth.repository.UserRepository;
import com.concrete.buildup.domain.contract.entity.Contract;
import com.concrete.buildup.domain.contract.enums.ContractState;
import com.concrete.buildup.domain.contract.repository.ContractRepository;
import com.concrete.buildup.domain.employee.dto.MyAttendanceListResponse;
import com.concrete.buildup.domain.employee.dto.MyAttendanceListResponse.MyAttendanceSummary;
import com.concrete.buildup.domain.employee.dto.MyHomeResponse;
import com.concrete.buildup.domain.employee.dto.MyHomeResponse.*;
import com.concrete.buildup.domain.employee.dto.MyPayrollListResponse;
import com.concrete.buildup.domain.employee.dto.MyPayrollListResponse.MyPayrollSummary;
import com.concrete.buildup.domain.payroll.entity.Payroll;
import com.concrete.buildup.domain.payroll.entity.PayslipItem;
import com.concrete.buildup.domain.payroll.repository.PayrollRepository;
import com.concrete.buildup.domain.payroll.repository.PayslipItemRepository;
import com.concrete.buildup.domain.safetydoc.entity.SafetyEducationAttendee;
import com.concrete.buildup.domain.safetydoc.repository.SafetyEducationAttendeeRepository;
import com.concrete.buildup.domain.site.entity.Site;
import com.concrete.buildup.domain.site.repository.SiteRepository;
import com.concrete.buildup.global.exception.BusinessException;
import com.concrete.buildup.global.exception.errorcode.EmployeeErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 근로자 본인 정보 조회 서비스
 *
 * <p>근로자가 본인의 출퇴근 내역, 급여 내역 등을 조회하는 기능을 제공합니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeMyService {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final SiteRepository siteRepository;
    private final ContractRepository contractRepository;
    private final PayrollRepository payrollRepository;
    private final PayslipItemRepository payslipItemRepository;
    private final SafetyEducationAttendeeRepository safetyEducationAttendeeRepository;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * 본인 출퇴근 내역 조회
     *
     * <p>JWT에서 추출한 userId로 본인의 출퇴근 기록을 조회합니다.</p>
     * <p>attendances 테이블에서 일별 근태 기록을 조회합니다.</p>
     *
     * @param currentUserId JWT에서 추출한 로그인 ID
     * @param pageable 페이지네이션 정보
     * @return 본인 출퇴근 내역
     */
    public MyAttendanceListResponse getMyAttendanceList(String currentUserId, Pageable pageable) {
        log.info("본인 출퇴근 내역 조회: userId={}", currentUserId);

        // 1. userId(로그인 ID)로 User 조회
        User user = userRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new BusinessException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND));

        // 2. User로 Employee 조회
        Employee employee = employeeRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND));

        Long employeeId = employee.getId();

        // 3. 전체 출퇴근 기록 조회 (최근 6개월) - attendances 테이블
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(6);

        List<Attendance> attendances = attendanceRepository
                .findByEmployeeIdAndSearchDateBetween(employeeId, startDate, endDate);

        // 4. Site 정보 일괄 조회 (N+1 방지)
        Set<Long> siteIds = attendances.stream()
                .map(Attendance::getSiteId)
                .collect(Collectors.toSet());
        Map<Long, Site> siteMap = siteRepository.findAllById(siteIds).stream()
                .collect(Collectors.toMap(Site::getId, site -> site));

        // 5. DTO 변환 (날짜 내림차순 정렬)
        List<MyAttendanceSummary> summaries = attendances.stream()
                .sorted(Comparator.comparing(Attendance::getSearchDate).reversed())
                .map(attendance -> {
                    Site site = siteMap.get(attendance.getSiteId());
                    String siteName = site != null ? site.getSiteName() : "알 수 없음";
                    String status = determineStatus(attendance);

                    return MyAttendanceSummary.builder()
                            .attendanceId(attendance.getId())
                            .date(attendance.getSearchDate())
                            .siteId(attendance.getSiteId())
                            .siteName(siteName)
                            .checkInTime(attendance.getCheckInTime() != null
                                    ? attendance.getCheckInTime().format(TIME_FORMATTER) : null)
                            .checkOutTime(attendance.getCheckOutTime() != null
                                    ? attendance.getCheckOutTime().format(TIME_FORMATTER) : null)
                            .status(status)
                            .isLate(attendance.getIsLate())
                            .build();
                })
                .toList();

        // 6. 페이지네이션 적용
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), summaries.size());

        List<MyAttendanceSummary> pagedContent = start < summaries.size()
                ? summaries.subList(start, end)
                : Collections.emptyList();

        Page<MyAttendanceSummary> page = new PageImpl<>(pagedContent, pageable, summaries.size());

        log.info("본인 출퇴근 내역 조회 완료: employeeId={}, totalRecords={}", employeeId, summaries.size());

        return MyAttendanceListResponse.from(page);
    }

    /**
     * 근태 상태 결정
     */
    private String determineStatus(Attendance attendance) {
        if (attendance.getCheckOutTime() != null) {
            return "COMPLETED"; // 퇴근 완료
        }

        // 출근만 있는 경우 - 당일이면 근무중, 과거면 미퇴근
        LocalDate today = LocalDate.now();

        if (attendance.getSearchDate().equals(today)) {
            return "WORKING"; // 근무중
        } else {
            return "INCOMPLETE"; // 미퇴근 (과거 데이터)
        }
    }

    /**
     * 홈 화면 정보 조회
     *
     * <p>근로자 홈 화면에 표시할 정보를 조회합니다.</p>
     * <ul>
     *   <li>미결 전자계약 (근로계약서 미서명, 안전교육일지 미서명)</li>
     *   <li>최근 급여 내역</li>
     *   <li>금일 근태 정보</li>
     * </ul>
     *
     * @param currentUserId JWT에서 추출한 로그인 ID
     * @return 홈 화면 정보
     */
    public MyHomeResponse getMyHome(String currentUserId) {
        log.info("홈 화면 정보 조회: userId={}", currentUserId);

        // 1. userId(로그인 ID)로 User 조회
        User user = userRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new BusinessException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND));

        // 2. User로 Employee 조회
        Employee employee = employeeRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND));

        Long employeeId = employee.getId();

        // 3. 미결 전자계약 조회
        PendingContractsInfo pendingContracts = getPendingContracts(employeeId);

        // 4. 최근 급여 조회
        RecentSalaryInfo recentSalary = getRecentSalary(employeeId);

        // 5. 금일 근태 조회
        TodayAttendanceInfo todayAttendance = getTodayAttendance(employeeId);

        log.info("홈 화면 정보 조회 완료: employeeId={}, pendingCount={}",
                employeeId, pendingContracts.getCount());

        return MyHomeResponse.builder()
                .pendingContracts(pendingContracts)
                .recentSalary(recentSalary)
                .todayAttendance(todayAttendance)
                .build();
    }

    /**
     * 미결 전자계약 조회
     */
    private PendingContractsInfo getPendingContracts(Long employeeId) {
        List<PendingContractItem> items = new ArrayList<>();

        // 1. 미서명 근로계약서 조회 (EMPLOYEE_SIGNING_PENDING 상태)
        List<Contract> pendingContracts = contractRepository
                .findByEmployeeIdAndContractState(employeeId, ContractState.EMPLOYEE_SIGNING_PENDING);

        // Site 정보 일괄 조회
        Set<Long> managerIds = pendingContracts.stream()
                .map(Contract::getManagerId)
                .collect(Collectors.toSet());

        Map<Long, Site> siteByManagerId = new HashMap<>();
        if (!managerIds.isEmpty()) {
            // Manager ID로 Site 조회
            for (Long managerId : managerIds) {
                siteRepository.findByManagerId(managerId)
                        .ifPresent(site -> siteByManagerId.put(managerId, site));
            }
        }

        for (Contract contract : pendingContracts) {
            Site site = siteByManagerId.get(contract.getManagerId());
            items.add(PendingContractItem.builder()
                    .type("CONTRACT")
                    .contractId(contract.getId())
                    .siteId(site != null ? site.getId() : null)
                    .siteName(site != null ? site.getSiteName() : "알 수 없음")
                    .build());
        }

        // 2. 미서명 안전교육일지 조회
        List<SafetyEducationAttendee> unsignedAttendees = safetyEducationAttendeeRepository
                .findByEmployeeIdAndIsSignedFalse(employeeId);

        for (SafetyEducationAttendee attendee : unsignedAttendees) {
            Site site = attendee.getSafetyEducationLog().getSite();
            items.add(PendingContractItem.builder()
                    .type("SAFETY_EDUCATION")
                    .safetyLogId(attendee.getSafetyEducationLog().getId())
                    .siteId(site.getId())
                    .siteName(site.getSiteName())
                    .build());
        }

        return PendingContractsInfo.builder()
                .count(items.size())
                .items(items)
                .build();
    }

    /**
     * 최근 급여 조회
     */
    private RecentSalaryInfo getRecentSalary(Long employeeId) {
        List<Payroll> payrolls = payrollRepository.findByEmployeeIdOrderBySearchDateDesc(employeeId);

        if (payrolls.isEmpty()) {
            return null;
        }

        Payroll latestPayroll = payrolls.get(0);
        Site site = siteRepository.findById(latestPayroll.getSiteId()).orElse(null);

        return RecentSalaryInfo.builder()
                .payrollId(latestPayroll.getId())
                .siteId(latestPayroll.getSiteId())
                .siteName(site != null ? site.getSiteName() : "알 수 없음")
                .payDate(latestPayroll.getSearchDate())
                .netPay(latestPayroll.getTotalPay())
                .build();
    }

    /**
     * 금일 근태 조회
     */
    private TodayAttendanceInfo getTodayAttendance(Long employeeId) {
        LocalDate today = LocalDate.now();

        // attendances 테이블에서 금일 근태 조회
        List<Attendance> todayAttendances = attendanceRepository
                .findByEmployeeIdAndSearchDate(employeeId, today);

        if (todayAttendances.isEmpty()) {
            return null;
        }

        // 첫 번째 근태 기록 사용 (하루에 여러 현장 근무 가능성 고려)
        Attendance attendance = todayAttendances.get(0);

        Site site = siteRepository.findById(attendance.getSiteId()).orElse(null);
        String status = attendance.getCheckOutTime() != null ? "COMPLETED" : "WORKING";

        return TodayAttendanceInfo.builder()
                .siteId(attendance.getSiteId())
                .siteName(site != null ? site.getSiteName() : "알 수 없음")
                .checkInTime(attendance.getCheckInTime() != null
                        ? attendance.getCheckInTime().format(TIME_FORMATTER) : null)
                .checkOutTime(attendance.getCheckOutTime() != null
                        ? attendance.getCheckOutTime().format(TIME_FORMATTER) : null)
                .status(status)
                .isLate(attendance.getIsLate())
                .build();
    }

    /**
     * 본인 급여 내역 조회
     *
     * <p>JWT에서 추출한 userId로 본인의 급여 내역을 조회합니다.</p>
     * <p>최근 급여의 상세 정보(기본급, 연장수당, 야간수당, 세금, 4대보험)와 급여 목록을 반환합니다.</p>
     *
     * @param currentUserId JWT에서 추출한 로그인 ID
     * @param pageable 페이지네이션 정보
     * @return 본인 급여 내역
     */
    public MyPayrollListResponse getMyPayrollList(String currentUserId, Pageable pageable) {
        log.info("본인 급여 내역 조회: userId={}", currentUserId);

        // 1. userId(로그인 ID)로 User 조회
        User user = userRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new BusinessException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND));

        // 2. User로 Employee 조회
        Employee employee = employeeRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND));

        Long employeeId = employee.getId();

        // 3. 급여 내역 조회 (최신순)
        List<Payroll> allPayrolls = payrollRepository.findByEmployeeIdOrderBySearchDateDesc(employeeId);

        // 4. Site 정보 일괄 조회 (N+1 방지)
        Set<Long> siteIds = allPayrolls.stream()
                .map(Payroll::getSiteId)
                .collect(Collectors.toSet());
        Map<Long, Site> siteMap = siteRepository.findAllById(siteIds).stream()
                .collect(Collectors.toMap(Site::getId, site -> site));

        // 5. 최근 급여 상세 정보 조회
        MyPayrollListResponse.LatestPayrollDetail latestPayroll = null;
        if (!allPayrolls.isEmpty()) {
            latestPayroll = buildLatestPayrollDetail(allPayrolls.get(0), siteMap);
        }

        // 6. DTO 변환
        List<MyPayrollSummary> summaries = allPayrolls.stream()
                .map(payroll -> {
                    Site site = siteMap.get(payroll.getSiteId());
                    String siteName = site != null ? site.getSiteName() : "알 수 없음";
                    return MyPayrollSummary.from(payroll, siteName);
                })
                .toList();

        // 7. 페이지네이션 적용
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), summaries.size());

        List<MyPayrollSummary> pagedContent = start < summaries.size()
                ? summaries.subList(start, end)
                : Collections.emptyList();

        Page<MyPayrollSummary> page = new PageImpl<>(pagedContent, pageable, summaries.size());

        log.info("본인 급여 내역 조회 완료: employeeId={}, totalRecords={}", employeeId, summaries.size());

        return MyPayrollListResponse.from(page, latestPayroll);
    }

    /**
     * 최근 급여 상세 정보 생성
     *
     * <p>PayslipItem에서 기본급, 연장수당, 야간수당, 4대보험료 등을 조회합니다.</p>
     */
    private MyPayrollListResponse.LatestPayrollDetail buildLatestPayrollDetail(
            Payroll payroll, Map<Long, Site> siteMap) {

        Site site = siteMap.get(payroll.getSiteId());
        String siteName = site != null ? site.getSiteName() : "알 수 없음";

        // PayslipItem 조회
        List<PayslipItem> items = payslipItemRepository.findByPayrollId(payroll.getId());

        // 항목별 금액 추출
        BigDecimal basePay = findItemAmount(items, "기본급");
        BigDecimal overtimePay = findItemAmount(items, "연장근로수당", "연장수당");
        BigDecimal nightPay = findItemAmount(items, "야간근로수당", "야간수당");

        // 4대보험 합계 계산
        BigDecimal nationalPension = findItemAmount(items, "국민연금");
        BigDecimal healthInsurance = findItemAmount(items, "건강보험");
        BigDecimal employmentInsurance = findItemAmount(items, "고용보험");
        BigDecimal industrialAccident = findItemAmount(items, "산재보험");
        BigDecimal longTermCare = findItemAmount(items, "장기요양보험");

        BigDecimal insuranceTotal = nationalPension
                .add(healthInsurance)
                .add(employmentInsurance)
                .add(industrialAccident)
                .add(longTermCare);

        return MyPayrollListResponse.LatestPayrollDetail.builder()
                .payrollId(payroll.getId())
                .siteName(siteName)
                .salaryYear(payroll.getSalaryYear())
                .salaryMonth(payroll.getSalaryMonth())
                .payDate(payroll.getSearchDate())
                .payStatus(payroll.getPayStatus())
                .netPay(payroll.getTotalPay())
                .basePay(basePay)
                .overtimePay(overtimePay)
                .nightPay(nightPay)
                .incomeTax(payroll.getIncomeTax() != null ? payroll.getIncomeTax() : BigDecimal.ZERO)
                .residentTax(payroll.getResidentTax() != null ? payroll.getResidentTax() : BigDecimal.ZERO)
                .insuranceTotal(insuranceTotal)
                .hasPdf(payroll.getS3Key() != null && !payroll.getS3Key().isEmpty())
                .build();
    }

    /**
     * PayslipItem 목록에서 특정 항목명의 금액 조회
     *
     * @param items PayslipItem 목록
     * @param itemNames 찾을 항목명들 (여러 개 가능, OR 조건)
     * @return 해당 항목의 금액 (없으면 0)
     */
    private BigDecimal findItemAmount(List<PayslipItem> items, String... itemNames) {
        return items.stream()
                .filter(item -> {
                    for (String name : itemNames) {
                        if (item.getItemName().contains(name)) {
                            return true;
                        }
                    }
                    return false;
                })
                .map(PayslipItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}
