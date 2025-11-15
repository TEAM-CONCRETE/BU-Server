package com.concrete.buildup.domain.attendance.service;

import com.concrete.buildup.domain.attendance.dto.*;
import com.concrete.buildup.domain.attendance.entity.Attendance;
import com.concrete.buildup.domain.attendance.repository.AttendanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * 근태 현황 조회
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
        // 날짜 범위 계산
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

        // 근로자 유형 매핑 (REGULAR -> PERMANENT, DAILY -> DAILY)
        String empType = "REGULAR".equals(employmentType) ? "PERMANENT" : "DAILY";

        // 페이징 설정
        Pageable pageable = PageRequest.of(page - 1, size);

        // 근태 기록 조회
        Page<Attendance> attendancePage;
        if (day != null) {
            attendancePage = attendanceRepository.findBySiteIdAndEmpTypeAndSearchDate(
                siteId, empType, startDate, pageable
            );
        } else {
            attendancePage = attendanceRepository.findBySiteIdAndEmpTypeAndSearchDateBetween(
                siteId, empType, startDate, endDate, pageable
            );
        }

        // Summary 계산
        AttendanceSummaryDto summary = calculateSummary(siteId, empType, startDate, endDate);

        // 상세 레코드 변환
        List<AttendanceDetailDto> records = attendancePage.getContent().stream()
            .map(this::convertToDetailDto)
            .collect(Collectors.toList());

        // 페이징 정보 구성
        PaginationDto pagination = PaginationDto.builder()
            .currentPage(page)
            .totalPages(attendancePage.getTotalPages())
            .totalRecords(attendancePage.getTotalElements())
            .pageSize(size)
            .build();

        return AttendanceListResponseDto.builder()
            .summary(summary)
            .records(records)
            .pagination(pagination)
            .build();
    }

    /**
     * 근태 요약 정보 계산
     */
    private AttendanceSummaryDto calculateSummary(
        Long siteId,
        String empType,
        LocalDate startDate,
        LocalDate endDate
    ) {
        Long normalCount = attendanceRepository.countBySiteIdAndEmpTypeAndSearchDateBetweenAndAttendanceStatus(
            siteId, empType, startDate, endDate, "NORMAL"
        );

        Long lateCount = attendanceRepository.countBySiteIdAndEmpTypeAndSearchDateBetweenAndAttendanceStatus(
            siteId, empType, startDate, endDate, "LATE"
        );

        Long earlyLeaveCount = attendanceRepository.countBySiteIdAndEmpTypeAndSearchDateBetweenAndAttendanceStatus(
            siteId, empType, startDate, endDate, "EARLY_LEAVE"
        );

        Long absentCount = attendanceRepository.countBySiteIdAndEmpTypeAndSearchDateBetweenAndAttendanceStatus(
            siteId, empType, startDate, endDate, "ABSENT"
        );

        return AttendanceSummaryDto.builder()
            .normalAttendance(normalCount != null ? normalCount : 0L)
            .late(lateCount != null ? lateCount : 0L)
            .earlyLeave(earlyLeaveCount != null ? earlyLeaveCount : 0L)
            .absent(absentCount != null ? absentCount : 0L)
            .build();
    }

    /**
     * Attendance Entity를 AttendanceDetailDto로 변환
     */
    private AttendanceDetailDto convertToDetailDto(Attendance attendance) {
        return AttendanceDetailDto.builder()
            .workerId(attendance.getEmployeeId())
            .workerName(attendance.getEmpName())
            .residentNumber(maskResidentNumber(attendance.getResidentNum()))
            .attendanceStatus(attendance.getAttendanceStatus())
            .checkInTime(formatTime(attendance.getCheckInTime()))
            .checkOutTime(formatTime(attendance.getCheckOutTime()))
            .totalWorkHours(formatHours(attendance.getTotalWorkHour()))
            .nightWorkHours(formatHours(attendance.getNightWorkHour()))
            .overtimeHours(formatHours(attendance.getAdditionalWorkHour()))
            .holidayWorkHours(formatHours(attendance.getHolidayWorkHour()))
            .build();
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
