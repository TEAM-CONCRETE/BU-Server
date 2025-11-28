package com.concrete.buildup.domain.site.service;

import com.concrete.buildup.domain.attendance.entity.AttendanceRecord;
import com.concrete.buildup.domain.attendance.enums.AttendanceState;
import com.concrete.buildup.domain.attendance.enums.AttendanceType;
import com.concrete.buildup.domain.attendance.repository.AttendanceRecordRepository;
import com.concrete.buildup.domain.auth.entity.Employee;
import com.concrete.buildup.domain.auth.entity.Manager;
import com.concrete.buildup.domain.auth.repository.EmployeeRepository;
import com.concrete.buildup.domain.contract.entity.Contract;
import com.concrete.buildup.domain.contract.enums.ContractState;
import com.concrete.buildup.domain.contract.repository.ContractRepository;
import com.concrete.buildup.domain.safetydoc.entity.SafetyEducationLog;
import com.concrete.buildup.domain.safetydoc.enums.SafetyEducationStatus;
import com.concrete.buildup.domain.safetydoc.repository.SafetyEducationLogRepository;
import com.concrete.buildup.domain.site.dto.DashboardResponse;
import com.concrete.buildup.domain.site.entity.Site;
import com.concrete.buildup.domain.site.repository.SiteRepository;
import com.concrete.buildup.global.exception.BusinessException;
import com.concrete.buildup.global.exception.errorcode.SiteErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 대시보드 서비스
 *
 * <p>현장 대시보드 정보를 제공합니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class DashboardService {

    private final SiteRepository siteRepository;
    private final ContractRepository contractRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final SafetyEducationLogRepository safetyEducationLogRepository;

    /**
     * 현장 대시보드 조회
     *
     * <p>기업 관리자와 현장 관리자 공통 API</p>
     *
     * @param siteId 현장 ID
     * @return 대시보드 정보
     */
    public DashboardResponse getDashboard(Long siteId) {
        log.info("대시보드 조회 시작 - siteId: {}", siteId);

        // 현장 정보 조회 (Manager 정보 포함)
        Site site = siteRepository.findByIdWithManager(siteId)
            .orElseThrow(() -> new BusinessException(SiteErrorCode.SITE_NOT_FOUND));

        Manager manager = site.getManager();
        Long managerId = manager != null ? manager.getId() : null;

        // 1. 현장 기본 정보
        DashboardResponse.SiteBasicInfo siteInfo = buildSiteBasicInfo(site);

        // 2. 인원 현황
        DashboardResponse.WorkforceStatus workforceStatus = buildWorkforceStatus(siteId, managerId);

        // 3. 안전 현황 (임의 값)
        DashboardResponse.SafetyStatus safetyStatus = buildSafetyStatus();

        // 4. 노무 현황
        DashboardResponse.LaborStatus laborStatus = buildLaborStatus(siteId, managerId);

        log.info("대시보드 조회 완료 - siteId: {}", siteId);

        return DashboardResponse.builder()
            .siteInfo(siteInfo)
            .workforceStatus(workforceStatus)
            .safetyStatus(safetyStatus)
            .laborStatus(laborStatus)
            .build();
    }

    /**
     * 현장 기본 정보 구성
     */
    private DashboardResponse.SiteBasicInfo buildSiteBasicInfo(Site site) {
        // 진행률 계산 (시작일 ~ 종료일 기준)
        BigDecimal progressRate = calculateProgressRate(site.getStartDate(), site.getEndDate());

        String managerName = null;
        if (site.getManager() != null) {
            managerName = site.getManager().getManagerName();
        }

        return DashboardResponse.SiteBasicInfo.builder()
            .siteId(site.getId())
            .siteName(site.getSiteName())
            .siteAddress(site.getSiteAddress())
            .clientName(site.getClientName())
            .startDate(site.getStartDate())
            .endDate(site.getEndDate())
            .progressRate(progressRate)
            .managerName(managerName)
            .build();
    }

    /**
     * 진행률 계산
     * (오늘 - 시작일) / (종료일 - 시작일) * 100
     */
    private BigDecimal calculateProgressRate(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return BigDecimal.ZERO;
        }

        LocalDate today = LocalDate.now();

        // 시작 전
        if (today.isBefore(startDate)) {
            return BigDecimal.ZERO;
        }

        // 종료 후
        if (today.isAfter(endDate)) {
            return BigDecimal.valueOf(100);
        }

        // 진행 중
        long totalDays = ChronoUnit.DAYS.between(startDate, endDate);
        long elapsedDays = ChronoUnit.DAYS.between(startDate, today);

        if (totalDays == 0) {
            return BigDecimal.valueOf(100);
        }

        return BigDecimal.valueOf(elapsedDays)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(totalDays), 1, RoundingMode.HALF_UP);
    }

    /**
     * 인원 현황 구성
     *
     * <p>siteId 기준으로 해당 현장에 소속된 근로자를 조회합니다.</p>
     * <p>User.siteId를 통해 현장 소속 근로자를 직접 조회하여, managerId가 null인 경우에도 정상 동작합니다.</p>
     */
    private DashboardResponse.WorkforceStatus buildWorkforceStatus(Long siteId, Long managerId) {
        // siteId 기준으로 해당 현장에 소속된 모든 근로자 조회
        List<Employee> employees = employeeRepository.findBySiteId(siteId);

        // 총 근로자 수
        int totalWorkers = employees.size();

        // 상용직/일용직 구분
        Map<String, Long> empTypeCount = employees.stream()
            .collect(Collectors.groupingBy(
                emp -> emp.getEmpType() != null ? emp.getEmpType() : "UNKNOWN",
                Collectors.counting()
            ));

        int permanentWorkers = empTypeCount.getOrDefault("PERMANENT", 0L).intValue();
        int dailyWorkers = empTypeCount.getOrDefault("DAILY", 0L).intValue();

        LocalDate today = LocalDate.now();

        // 금일 출근 인원 (AttendanceRecord에서 CHECK_IN, CONFIRMED 상태)
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        List<AttendanceRecord> todayAttendanceRecords = attendanceRecordRepository
            .findBySiteIdAndTimestampBetween(siteId, startOfDay, endOfDay, Pageable.unpaged())
            .getContent()
            .stream()
            .filter(record -> record.getAttendanceType() == AttendanceType.CHECK_IN)
            .filter(record -> record.getState() == AttendanceState.CONFIRMED)
            .collect(Collectors.toList());

        int todayAttendance = (int) todayAttendanceRecords.stream()
            .map(AttendanceRecord::getEmployeeId)
            .distinct()
            .count();

        return DashboardResponse.WorkforceStatus.builder()
            .totalWorkers(totalWorkers)
            .permanentWorkers(permanentWorkers)
            .dailyWorkers(dailyWorkers)
            .todayAttendance(todayAttendance)
            .todayLateCount(0)
            .build();
    }

    /**
     * 계약이 특정 날짜에 활성 상태인지 확인
     */
    private boolean isContractActiveOnDate(Contract contract, LocalDate date) {
        LocalDate startDate = contract.getEmployeeStartDate();
        LocalDate endDate = contract.getEmployeeEndDate();

        if (startDate == null) {
            return false;
        }

        // 시작일 <= 날짜
        if (date.isBefore(startDate)) {
            return false;
        }

        // 종료일이 없거나 종료일 >= 날짜
        return endDate == null || !date.isAfter(endDate);
    }

    /**
     * 금일 지각 인원 계산
     * workStartTime + 5분 이후 출근한 인원
     */
    private int calculateTodayLateCount(List<AttendanceRecord> attendanceRecords, List<Contract> contracts) {
        // ContractDetail을 포함한 Contract를 일괄 조회 (N+1 문제 해결)
        List<Long> contractIds = contracts.stream()
            .map(Contract::getId)
            .distinct()
            .collect(Collectors.toList());

        if (contractIds.isEmpty()) {
            return 0;
        }

        // 일괄 조회로 N+1 문제 해결
        Map<Long, Contract> contractWithDetailsMap = contractRepository.findByIdInWithDetails(contractIds)
            .stream()
            .collect(Collectors.toMap(
                Contract::getEmployeeId,
                contract -> contract,
                (existing, replacement) -> existing // 중복 시 기존 값 유지
            ));

        int lateCount = 0;
        for (AttendanceRecord record : attendanceRecords) {
            Contract contract = contractWithDetailsMap.get(record.getEmployeeId());
            if (contract == null || contract.getContractDetail() == null) {
                continue;
            }

            LocalTime workStartTime = contract.getContractDetail().getWorkStartTime();
            if (workStartTime == null) {
                continue; // 출근 시간 정보 없음
            }

            // 지각 기준: workStartTime + 5분
            LocalTime lateThreshold = workStartTime.plusMinutes(5);
            LocalTime checkInTime = record.getTimestamp().toLocalTime();

            if (checkInTime.isAfter(lateThreshold)) {
                lateCount++;
            }
        }

        return lateCount;
    }

    /**
     * 안전 현황 구성 (임의 값)
     */
    private DashboardResponse.SafetyStatus buildSafetyStatus() {
        // 안전 관련 지표는 계산이 어려운 경우 임의의 숫자로 대체 (요구사항)
        return DashboardResponse.SafetyStatus.builder()
            .safetyRate(BigDecimal.valueOf(92.5))
            .todayWarnings(2)
            .incompletedEducation(5)
            .completedInspections(10)
            .build();
    }

    /**
     * 노무 현황 구성
     */
    private DashboardResponse.LaborStatus buildLaborStatus(Long siteId, Long managerId) {
        List<DashboardResponse.PendingContractItem> pendingContracts = new ArrayList<>();

        if (managerId != null) {
            // 1. 미결 근로계약서 조회 (FULLY_SIGNED가 아닌 것)
            List<Contract> pendingEmploymentContracts = contractRepository.findByManagerId(managerId, Pageable.unpaged())
                .getContent()
                .stream()
                .filter(contract -> contract.getContractState() != ContractState.FULLY_SIGNED)
                .filter(contract -> contract.getContractState() != ContractState.TERMINATED)
                .collect(Collectors.toList());

            // Employee 정보 조회
            Map<Long, Employee> employeeMap = getEmployeeMap(
                pendingEmploymentContracts.stream()
                    .map(Contract::getEmployeeId)
                    .collect(Collectors.toList())
            );

            for (Contract contract : pendingEmploymentContracts) {
                Employee employee = employeeMap.get(contract.getEmployeeId());
                String targetName = employee != null ? employee.getEmpName() : "알 수 없음";

                pendingContracts.add(DashboardResponse.PendingContractItem.builder()
                    .contractId(contract.getId())
                    .contractType("근로계약서")
                    .targetName(targetName)
                    .contractState(contract.getContractState().name())
                    .build());
            }

            // 2. 미결 안전교육일지 조회 (COMPLETED가 아닌 것)
            List<SafetyEducationLog> pendingSafetyLogs = safetyEducationLogRepository
                .findBySiteIdAndIsDeletedFalseOrderByCreatedAtDesc(siteId)
                .stream()
                .filter(log -> log.getStatus() != SafetyEducationStatus.COMPLETED)
                .collect(Collectors.toList());

            for (SafetyEducationLog log : pendingSafetyLogs) {
                pendingContracts.add(DashboardResponse.PendingContractItem.builder()
                    .contractId(log.getId())
                    .contractType("안전교육일지")
                    .targetName(log.getEducationSubject()) // 교육 과목을 대상자 이름으로 사용
                    .contractState(log.getStatus().name())
                    .build());
            }
        }

        return DashboardResponse.LaborStatus.builder()
            .totalPendingContracts(pendingContracts.size())
            .pendingContracts(pendingContracts)
            .build();
    }

    /**
     * Employee ID 목록으로 Employee Map 생성
     */
    private Map<Long, Employee> getEmployeeMap(List<Long> employeeIds) {
        if (employeeIds.isEmpty()) {
            return Map.of();
        }

        return employeeRepository.findAllById(employeeIds).stream()
            .collect(Collectors.toMap(
                employee -> employee.getId(),
                employee -> employee
            ));
    }
}
