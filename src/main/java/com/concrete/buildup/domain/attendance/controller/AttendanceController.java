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
import org.springframework.web.multipart.MultipartFile;

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
     * 출퇴근 검증 및 기록 (얼굴 인식 - 백엔드 직접 처리)
     *
     * <p>현장 관리자가 로그인한 공용 태블릿에서 근로자가 사용합니다.
     * 근로자가 전화번호를 입력하고 얼굴 이미지를 촬영하면, 이미지를 직접 전송하여
     * 백엔드에서 얼굴 인식 검증 및 출퇴근 기록을 수행합니다.</p>
     *
     * <p>처리 흐름:</p>
     * <ol>
     *   <li>전화번호로 근로자 조회 (Employee 엔티티)</li>
     *   <li>파일 검증 (크기, 타입, 이미지 포맷)</li>
     *   <li>S3에 이미지 업로드</li>
     *   <li>로그인한 현장 관리자의 현장 ID를 SecurityContext에서 자동 조회</li>
     *   <li>근로자의 얼굴 이미지 등록 여부 확인</li>
     *   <li>출퇴근 유형 자동 판단 (당일 마지막 기록 기준)</li>
     *   <li>중복 기록 검증</li>
     *   <li>Face API를 통한 얼굴 유사도 검증</li>
     *   <li>검증 성공 시 출퇴근 기록 저장</li>
     * </ol>
     *
     * @param phoneNumber 근로자 전화번호
     * @param faceImage 얼굴 이미지 파일 (MultipartFile)
     * @return AttendanceVerificationResponseDto - 검증 결과 및 출퇴근 기록 정보
     */
    @Operation(
            summary = "출퇴근 검증 및 기록 (얼굴 인식 - 백엔드 직접 처리)",
            description = """
                    현장 공용 태블릿에서 얼굴 인식을 통한 출퇴근 검증 및 기록을 수행합니다.

                    **처리 흐름:**
                    1. 클라이언트가 전화번호와 얼굴 이미지를 multipart/form-data로 전송
                    2. 백엔드에서 파일 검증 (크기, MIME 타입, Magic Number)
                    3. S3에 이미지 직접 업로드
                    4. Face Recognition API로 얼굴 유사도 비교 (임계값 0.35)
                    5. 출퇴근 유형 자동 판단 (당일 마지막 기록 기준)
                    6. 출퇴근 기록 저장 및 응답 반환

                    **응답 필드:**
                    - success: API 호출 성공 여부
                    - verified: 얼굴 인식 검증 통과 여부
                    - recordId: 출퇴근 기록 ID
                    - employeeId: 근로자 ID
                    - employeeName: 근로자 이름
                    - attendanceType: 출퇴근 유형 (CHECK_IN/CHECK_OUT)
                    - timestamp: 출퇴근 기록 시각
                    - message: 결과 메시지
                    - isLate: 지각 여부 (출근 시에만)

                    **요청 형식:** Content-Type: multipart/form-data

                    **예시:** POST /v1/attendance/verify
                    - phoneNumber: "010-1234-5678"
                    - faceImage: [이미지 파일]
                    """
    )
    @PostMapping(value = "/attendance/verify", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<AttendanceVerificationResponseDto>> verifyAttendance(
        @Parameter(description = "근로자 전화번호 (하이픈 포함 또는 제외)", required = true, example = "010-1234-5678")
        @RequestPart("phoneNumber") String phoneNumber,

        @Parameter(description = "얼굴 이미지 파일 (JPG, PNG)", required = true)
        @RequestPart("faceImage") MultipartFile faceImage
    ) {
        // DTO 생성
        AttendanceVerificationRequestDto request = AttendanceVerificationRequestDto.builder()
                .phoneNumber(phoneNumber)
                .faceImage(faceImage)
                .build();

        // 서비스 호출
        AttendanceVerificationResponseDto response = attendanceService.verifyAndRecordAttendance(request);

        return ResponseEntity.ok(
            ApiResponse.success(response, response.getMessage())
        );
    }
}
