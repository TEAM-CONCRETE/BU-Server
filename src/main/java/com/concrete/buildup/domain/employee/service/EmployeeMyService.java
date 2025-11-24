package com.concrete.buildup.domain.employee.service;

import com.concrete.buildup.domain.attendance.entity.AttendanceRecord;
import com.concrete.buildup.domain.attendance.enums.AttendanceState;
import com.concrete.buildup.domain.attendance.enums.AttendanceType;
import com.concrete.buildup.domain.attendance.repository.AttendanceRecordRepository;
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
    private final AttendanceRecordRepository attendanceRecordRepository;
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
     * <p>날짜별로 그룹핑하여 출근/퇴근 시간을 하나의 레코드로 반환합니다.</p>
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

        // 3. 전체 출퇴근 기록 조회 (최근 6개월)
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusMonths(6);

        List<AttendanceRecord> allRecords = attendanceRecordRepository
                .findByEmployeeIdAndTimestampBetween(employeeId, startTime, endTime);

        // 4. CONFIRMED 상태만 필터링
        List<AttendanceRecord> confirmedRecords = allRecords.stream()
                .filter(record -> record.getState() == AttendanceState.CONFIRMED)
                .sorted(Comparator.comparing(AttendanceRecord::getTimestamp).reversed())
                .toList();

        // 5. 날짜별로 그룹핑하여 출근/퇴근 페어 생성
        Map<LocalDate, Map<Long, List<AttendanceRecord>>> recordsByDateAndSite = confirmedRecords.stream()
                .collect(Collectors.groupingBy(
                        record -> record.getTimestamp().toLocalDate(),
                        LinkedHashMap::new,
                        Collectors.groupingBy(AttendanceRecord::getSiteId)
                ));

        // 6. Site 정보 일괄 조회 (N+1 방지)
        Set<Long> siteIds = confirmedRecords.stream()
                .map(AttendanceRecord::getSiteId)
                .collect(Collectors.toSet());
        Map<Long, Site> siteMap = siteRepository.findAllById(siteIds).stream()
                .collect(Collectors.toMap(Site::getId, site -> site));

        // 7. 날짜+현장별 요약 레코드 생성
        List<MyAttendanceSummary> summaries = new ArrayList<>();
        for (Map.Entry<LocalDate, Map<Long, List<AttendanceRecord>>> dateEntry : recordsByDateAndSite.entrySet()) {
            LocalDate date = dateEntry.getKey();
            for (Map.Entry<Long, List<AttendanceRecord>> siteEntry : dateEntry.getValue().entrySet()) {
                Long siteId = siteEntry.getKey();
                List<AttendanceRecord> records = siteEntry.getValue();

                Site site = siteMap.get(siteId);
                String siteName = site != null ? site.getSiteName() : "알 수 없음";

                // 출근/퇴근 기록 찾기
                AttendanceRecord checkIn = records.stream()
                        .filter(r -> r.getAttendanceType() == AttendanceType.CHECK_IN)
                        .findFirst()
                        .orElse(null);

                AttendanceRecord checkOut = records.stream()
                        .filter(r -> r.getAttendanceType() == AttendanceType.CHECK_OUT)
                        .findFirst()
                        .orElse(null);

                // 출근 기록이 있어야 유효한 데이터
                if (checkIn != null) {
                    String status = determineStatus(checkIn, checkOut);
                    Boolean isLate = determineIsLate(checkIn);

                    summaries.add(MyAttendanceSummary.builder()
                            .attendanceId(checkIn.getId())
                            .date(date)
                            .siteId(siteId)
                            .siteName(siteName)
                            .checkInTime(checkIn.getTimestamp().format(TIME_FORMATTER))
                            .checkOutTime(checkOut != null ? checkOut.getTimestamp().format(TIME_FORMATTER) : null)
                            .status(status)
                            .isLate(isLate)
                            .build());
                }
            }
        }

        // 8. 날짜 내림차순 정렬
        summaries.sort(Comparator.comparing(MyAttendanceSummary::getDate).reversed());

        // 9. 페이지네이션 적용
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
    private String determineStatus(AttendanceRecord checkIn, AttendanceRecord checkOut) {
        if (checkOut != null) {
            return "COMPLETED"; // 퇴근 완료
        }

        // 출근만 있는 경우 - 당일이면 근무중, 과거면 미퇴근
        LocalDate today = LocalDate.now();
        LocalDate checkInDate = checkIn.getTimestamp().toLocalDate();

        if (checkInDate.equals(today)) {
            return "WORKING"; // 근무중
        } else {
            return "INCOMPLETE"; // 미퇴근 (과거 데이터)
        }
    }

    /**
     * 지각 여부 결정
     * <p>현재는 단순히 9시 이후 출근을 지각으로 판단합니다.</p>
     * <p>TODO: 계약서의 출근 시간 기준으로 변경 필요</p>
     */
    private Boolean determineIsLate(AttendanceRecord checkIn) {
        LocalTime checkInTime = checkIn.getTimestamp().toLocalTime();
        LocalTime standardTime = LocalTime.of(9, 5); // 9시 5분 기준 (5분 유예)
        return checkInTime.isAfter(standardTime);
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
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);

        List<AttendanceRecord> todayRecords = attendanceRecordRepository
                .findByEmployeeIdAndTimestampBetween(employeeId, startOfDay, endOfDay);

        // CONFIRMED 상태만 필터링
        List<AttendanceRecord> confirmedRecords = todayRecords.stream()
                .filter(r -> r.getState() == AttendanceState.CONFIRMED)
                .toList();

        if (confirmedRecords.isEmpty()) {
            return null;
        }

        // 출근/퇴근 기록 찾기
        AttendanceRecord checkIn = confirmedRecords.stream()
                .filter(r -> r.getAttendanceType() == AttendanceType.CHECK_IN)
                .findFirst()
                .orElse(null);

        AttendanceRecord checkOut = confirmedRecords.stream()
                .filter(r -> r.getAttendanceType() == AttendanceType.CHECK_OUT)
                .findFirst()
                .orElse(null);

        if (checkIn == null) {
            return null;
        }

        Site site = siteRepository.findById(checkIn.getSiteId()).orElse(null);
        String status = checkOut != null ? "COMPLETED" : "WORKING";

        return TodayAttendanceInfo.builder()
                .siteId(checkIn.getSiteId())
                .siteName(site != null ? site.getSiteName() : "알 수 없음")
                .checkInTime(checkIn.getTimestamp().format(TIME_FORMATTER))
                .checkOutTime(checkOut != null ? checkOut.getTimestamp().format(TIME_FORMATTER) : null)
                .status(status)
                .isLate(determineIsLate(checkIn))
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
