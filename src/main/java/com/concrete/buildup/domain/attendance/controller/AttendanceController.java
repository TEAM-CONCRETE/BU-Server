package com.concrete.buildup.domain.attendance.controller;

import com.concrete.buildup.domain.attendance.dto.AttendanceListResponseDto;
import com.concrete.buildup.domain.attendance.service.AttendanceService;
import com.concrete.buildup.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 근태 관리 Controller
 *
 * 기업 관리자 및 현장 관리자가 해당 현장의 근로자 근태 현황을 조회합니다.
 */
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    /**
     * 근태 현황 조회 (상용직/일용직)
     *
     * 기업 관리자 또는 현장 관리자가 해당 현장의 근로자 근태를 조회합니다.
     *
     * @param siteId 현장 ID
     * @param year 년도
     * @param month 월
     * @param day 일 (optional)
     * @param employmentType 근로자 유형 (REGULAR/DAILY)
     * @param page 페이지 번호 (default: 1)
     * @param size 페이지 크기 (default: 20)
     * @return 근태 현황 응답
     */
    @GetMapping("/{siteId}/attendance/records")
    public ResponseEntity<ApiResponse<AttendanceListResponseDto>> getAttendanceRecords(
        @PathVariable Long siteId,
        @RequestParam Integer year,
        @RequestParam Integer month,
        @RequestParam(required = false) Integer day,
        @RequestParam String employmentType,
        @RequestParam(defaultValue = "1") Integer page,
        @RequestParam(defaultValue = "20") Integer size
    ) {
        AttendanceListResponseDto response = attendanceService.getAttendanceRecords(
            siteId, year, month, day, employmentType, page, size
        );

        return ResponseEntity.ok(
            ApiResponse.success(response, "근태 현황을 조회했습니다.")
        );
    }
}
