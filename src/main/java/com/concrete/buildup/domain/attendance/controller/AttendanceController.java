package com.concrete.buildup.domain.attendance.controller;

import com.concrete.buildup.domain.attendance.dto.AttendanceListResponseDto;
import com.concrete.buildup.domain.attendance.dto.AttendanceVerificationRequestDto;
import com.concrete.buildup.domain.attendance.dto.AttendanceVerificationResponseDto;
import com.concrete.buildup.domain.attendance.service.AttendanceService;
import com.concrete.buildup.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 근태 관리 Controller
 *
 * <p>기업 관리자 및 현장 관리자가 해당 현장의 근로자 근태 현황을 조회합니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Tag(name = "Attendance", description = "근태 관리 API")
@PreAuthorize("hasAnyRole('MANAGER', 'CORPORATION')")
@Validated
public class AttendanceController {

    private final AttendanceService attendanceService;

    /**
     * 근태 현황 조회 (상용직/일용직)
     *
     * <p>기업 관리자 또는 현장 관리자가 해당 현장의 근로자 근태를 조회합니다.</p>
     * <p>조회 조건:</p>
     * <ul>
     *   <li>year: 조회 년도 (예: 2025)</li>
     *   <li>month: 조회 월 (1~12)</li>
     *   <li>day: 특정 일 조회 (선택, 미입력시 해당 월 전체 조회)</li>
     *   <li>employmentType: 근로자 유형 (REGULAR: 상용직, DAILY: 일용직)</li>
     *   <li>page: 페이지 번호 (기본값: 1)</li>
     *   <li>size: 페이지 크기 (기본값: 20)</li>
     * </ul>
     *
     * @param siteId 현장 ID
     * @param year 년도
     * @param month 월
     * @param day 일 (optional)
     * @param employmentType 근로자 유형 (REGULAR/DAILY)
     * @param page 페이지 번호 (default: 1)
     * @param size 페이지 크기 (default: 20)
     * @return AttendanceListResponseDto - 근태 현황 + 통계 + 페이징 정보
     */
    @Operation(
            summary = "근태 현황 조회",
            description = "현장별 근로자 근태 현황을 조회합니다. " +
                    "상용직(REGULAR)과 일용직(DAILY)을 구분하여 조회할 수 있으며, " +
                    "특정 일자 또는 월 전체 조회가 가능합니다. " +
                    "주민등록번호는 마스킹 처리되어 반환됩니다. " +
                    "예: GET /v1/1/attendance/records?year=2025&month=11&day=15&employmentType=REGULAR&page=1&size=20"
    )
    @GetMapping("/{siteId}/attendance/records")
    public ResponseEntity<ApiResponse<AttendanceListResponseDto>> getAttendanceRecords(
        @Parameter(description = "현장 ID", required = true, example = "1")
        @PathVariable Long siteId,
        @Parameter(description = "조회 년도 (2000~2100)", required = true, example = "2025")
        @RequestParam @Min(2000) @Max(2100) Integer year,
        @Parameter(description = "조회 월 (1~12)", required = true, example = "11")
        @RequestParam @Min(1) @Max(12) Integer month,
        @Parameter(description = "조회 일 (1~31, 선택, 미입력시 월 전체 조회)", required = false, example = "15")
        @RequestParam(required = false) @Min(1) @Max(31) Integer day,
        @Parameter(description = "근로자 유형 (REGULAR: 상용직, DAILY: 일용직)", required = true, example = "REGULAR")
        @RequestParam String employmentType,
        @Parameter(description = "페이지 번호 (1부터 시작)", required = false, example = "1")
        @RequestParam(defaultValue = "1") @Min(1) Integer page,
        @Parameter(description = "페이지 크기 (1~100)", required = false, example = "20")
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size
    ) {
        AttendanceListResponseDto response = attendanceService.getAttendanceRecords(
            siteId, year, month, day, employmentType, page, size
        );

        return ResponseEntity.ok(
            ApiResponse.success(response, "근태 현황을 조회했습니다.")
        );
    }

    /**
     * 출퇴근 검증 및 기록 (얼굴 인식)
     *
     * <p>현장 관리자가 로그인한 공용 태블릿에서 근로자가 사용합니다.
     * 근로자가 주민번호를 입력하고 얼굴 이미지를 촬영하면, Presigned URL을 통해
     * S3에 업로드한 후 이 API를 호출하여 얼굴 인식 검증 및 출퇴근 기록을 수행합니다.</p>
     *
     * <p>처리 흐름:</p>
     * <ol>
     *   <li>로그인한 현장 관리자의 현장 ID를 SecurityContext에서 자동 조회</li>
     *   <li>근로자의 얼굴 이미지 등록 여부 확인</li>
     *   <li>출퇴근 유형 자동 판단 (당일 마지막 기록 기준)</li>
     *   <li>중복 기록 검증</li>
     *   <li>Face API를 통한 얼굴 유사도 검증</li>
     *   <li>검증 성공 시 출퇴근 기록 저장</li>
     * </ol>
     *
     * @param request 출퇴근 검증 요청 (employeeId, uploadId)
     * @return AttendanceVerificationResponseDto - 검증 결과 및 출퇴근 기록 정보
     */
    @Operation(
            summary = "출퇴근 검증 및 기록 (얼굴 인식)",
            description = "현장 공용 태블릿에서 얼굴 인식을 통한 출퇴근 검증 및 기록을 수행합니다. " +
                    "Presigned URL을 통해 업로드한 얼굴 이미지를 기반으로 검증하며, " +
                    "출퇴근 유형(CHECK_IN/CHECK_OUT)은 당일 마지막 기록을 기반으로 자동 판단됩니다. " +
                    "현장 ID는 로그인한 관리자 정보에서 자동으로 조회됩니다. " +
                    "예: POST /v1/attendance/verify"
    )
    @PostMapping("/attendance/verify")
    public ResponseEntity<ApiResponse<AttendanceVerificationResponseDto>> verifyAttendance(
        @Valid @RequestBody AttendanceVerificationRequestDto request
    ) {
        AttendanceVerificationResponseDto response = attendanceService.verifyAndRecordAttendance(request);

        return ResponseEntity.ok(
            ApiResponse.success(response, response.getMessage())
        );
    }
}
