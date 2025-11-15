package com.concrete.buildup.domain.attendance.service;

import com.concrete.buildup.domain.attendance.dto.*;
import com.concrete.buildup.domain.attendance.entity.Attendance;
import com.concrete.buildup.domain.attendance.repository.AttendanceRepository;
import com.concrete.buildup.domain.auth.entity.Employee;
import com.concrete.buildup.domain.auth.repository.EmployeeRepository;
import com.concrete.buildup.domain.contract.entity.Contract;
import com.concrete.buildup.domain.contract.enums.EmpType;
import com.concrete.buildup.domain.contract.repository.ContractRepository;
import com.concrete.buildup.domain.site.entity.Site;
import com.concrete.buildup.domain.site.repository.SiteRepository;
import com.concrete.buildup.global.exception.BusinessException;
import com.concrete.buildup.global.exception.errorcode.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final ContractRepository contractRepository;
    private final SiteRepository siteRepository;
    private final EmployeeRepository employeeRepository;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * 근태 현황 조회 (계약 기반 - 출퇴근 기록 없어도 표시)
     *
     * @param siteId 현장 ID
     * @param year 년도
     * @param month 월
     * @param day 일 (optional)
     * @param employmentType 근로자 유형 (REGULAR/DAILY)
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 근태 현황 응답
     */
    public AttendanceListResponseDto getAttendanceRecords(
        Long siteId,
        Integer year,
        Integer month,
        Integer day,
        String employmentType,
        Integer page,
        Integer size
    ) {
        // 1. Site 조회 → managerId 획득
        Site site = siteRepository.findById(siteId)
            .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "현장을 찾을 수 없습니다."));
        Long managerId = site.getManager().getId();

        // 2. 날짜 범위 계산
        LocalDate startDate;
        LocalDate endDate;

        if (day != null) {
            // 특정 일자 조회
            startDate = LocalDate.of(year, month, day);
            endDate = startDate;
        } else {
            // 월 전체 조회
            startDate = LocalDate.of(year, month, 1);
            endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        }

        // 3. 근로자 유형 매핑 (REGULAR → PERMANENT, DAILY → DAILY)
        EmpType empType = "REGULAR".equals(employmentType) ? EmpType.PERMANENT : EmpType.DAILY;

        // 4. 해당 현장(managerId), 날짜, 근로자 유형에 맞는 활성 계약 조회
        List<Contract> activeContracts;
        if (day != null) {
            activeContracts = contractRepository.findActiveContractsByManagerIdAndEmpTypeAndDate(
                managerId, empType, startDate
            );
        } else {
            // 월 조회의 경우 startDate부터 endDate 사이에 활성화된 계약 조회
            activeContracts = contractRepository.findActiveContractsByManagerIdAndEmpTypeAndDate(
                managerId, empType, startDate
            ).stream()
            .filter(c -> c.getEmployeeStartDate().isBefore(endDate.plusDays(1)))
            .collect(Collectors.toList());
        }

        log.info("Active contracts found: {} for site: {}, date: {}-{}-{}, empType: {}",
            activeContracts.size(), siteId, year, month, day, empType);

        // 5. 각 계약에 대해 근태 데이터 생성
        Map<Long, Employee> employeeCache = new HashMap<>();
        List<AttendanceDetailDto> allRecords = activeContracts.stream()
            .map(contract -> {
                // Employee 정보 조회 (캐싱)
                Employee employee = employeeCache.computeIfAbsent(
                    contract.getEmployeeId(),
                    id -> employeeRepository.findById(id).orElse(null)
                );

                if (employee == null) {
                    log.warn("Employee not found for contract: {}, employeeId: {}",
                        contract.getId(), contract.getEmployeeId());
                    return null;
                }

                // Attendance 조회
                Optional<Attendance> attendanceOpt;
                if (day != null) {
                    attendanceOpt = attendanceRepository
                        .findByEmployeeIdAndSearchDate(contract.getEmployeeId(), startDate)
                        .stream()
                        .findFirst();
                } else {
                    attendanceOpt = Optional.empty(); // 월 조회시에는 복잡하므로 일단 ABSENT 처리
                }

                return convertToDetailDto(employee, attendanceOpt.orElse(null));
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        // 6. 페이징 처리
        int totalRecords = allRecords.size();
        int start = (page - 1) * size;
        int end = Math.min(start + size, totalRecords);

        List<AttendanceDetailDto> pagedRecords = start < totalRecords
            ? allRecords.subList(start, end)
            : Collections.emptyList();

        // 7. Summary 계산
        AttendanceSummaryDto summary = calculateSummary(allRecords);

        // 8. 페이징 정보 구성
        PaginationDto pagination = PaginationDto.builder()
            .currentPage(page)
            .totalPages((int) Math.ceil((double) totalRecords / size))
            .totalRecords((long) totalRecords)
            .pageSize(size)
            .build();

        return AttendanceListResponseDto.builder()
            .summary(summary)
            .records(pagedRecords)
            .pagination(pagination)
            .build();
    }

    /**
     * 근태 요약 정보 계산
     */
    private AttendanceSummaryDto calculateSummary(List<AttendanceDetailDto> records) {
        long normalCount = records.stream()
            .filter(r -> "NORMAL".equals(r.getAttendanceStatus()))
            .count();

        long lateCount = records.stream()
            .filter(r -> "LATE".equals(r.getAttendanceStatus()))
            .count();

        long earlyLeaveCount = records.stream()
            .filter(r -> "EARLY_LEAVE".equals(r.getAttendanceStatus()))
            .count();

        long absentCount = records.stream()
            .filter(r -> "ABSENT".equals(r.getAttendanceStatus()))
            .count();

        return AttendanceSummaryDto.builder()
            .normalAttendance(normalCount)
            .late(lateCount)
            .earlyLeave(earlyLeaveCount)
            .absent(absentCount)
            .build();
    }

    /**
     * Employee와 Attendance를 AttendanceDetailDto로 변환
     */
    private AttendanceDetailDto convertToDetailDto(Employee employee, Attendance attendance) {
        if (attendance != null) {
            // Attendance 기록이 있는 경우
            return AttendanceDetailDto.builder()
                .workerId(employee.getId())
                .workerName(employee.getEmpName())
                .residentNumber(maskResidentNumber(employee.getResidentNum()))
                .attendanceStatus(attendance.getAttendanceStatus())
                .checkInTime(formatTime(attendance.getCheckInTime()))
                .checkOutTime(formatTime(attendance.getCheckOutTime()))
                .totalWorkHours(formatHours(attendance.getTotalWorkHour()))
                .nightWorkHours(formatHours(attendance.getNightWorkHour()))
                .overtimeHours(formatHours(attendance.getAdditionalWorkHour()))
                .holidayWorkHours(formatHours(attendance.getHolidayWorkHour()))
                .build();
        } else {
            // Attendance 기록이 없는 경우 → ABSENT로 표시
            return AttendanceDetailDto.builder()
                .workerId(employee.getId())
                .workerName(employee.getEmpName())
                .residentNumber(maskResidentNumber(employee.getResidentNum()))
                .attendanceStatus("ABSENT")
                .checkInTime("-")
                .checkOutTime("-")
                .totalWorkHours("-")
                .nightWorkHours("-")
                .overtimeHours("-")
                .holidayWorkHours("-")
                .build();
        }
    }

    /**
     * 주민번호 마스킹 처리
     */
    private String maskResidentNumber(String residentNum) {
        if (residentNum == null || residentNum.isEmpty()) {
            return "";
        }
        // "850101-1******" 형식으로 마스킹
        if (residentNum.length() >= 8) {
            return residentNum.substring(0, 8) + "******";
        }
        return residentNum;
    }

    /**
     * LocalDateTime을 HH:mm 형식으로 변환
     */
    private String formatTime(java.time.LocalDateTime dateTime) {
        if (dateTime == null) {
            return "-";
        }
        return dateTime.format(TIME_FORMATTER);
    }

    /**
     * BigDecimal 시간을 "N시간" 형식으로 변환
     */
    private String formatHours(BigDecimal hours) {
        if (hours == null || hours.compareTo(BigDecimal.ZERO) == 0) {
            return "-";
        }
        return hours.stripTrailingZeros().toPlainString() + "시간";
    }
}
