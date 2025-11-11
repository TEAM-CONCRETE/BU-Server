package com.concrete.buildup.domain.attendance.controller;

import com.concrete.buildup.domain.attendance.dto.FaceRegistrationRequestDto;
import com.concrete.buildup.domain.attendance.service.EmployeeFaceService;
import com.concrete.buildup.global.common.ApiResponse;
import com.concrete.buildup.global.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 근로자용 출퇴근 API 컨트롤러
 * 근로자가 본인의 얼굴 등록, 출퇴근 기록 조회 등을 수행합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
@Tag(name = "Employee Attendance", description = "근로자용 출퇴근 API")
public class EmployeeAttendanceController {

    private final EmployeeFaceService employeeFaceService;

    /**
     * POST /api/attendance/my-face
     * 근로자 본인의 얼굴 이미지 등록
     *
     * 플로우:
     * 1. 클라이언트: face-api.js로 얼굴 1개 검출 확인
     * 2. 클라이언트: POST /v1/uploads/presign (resourceType=EMPLOYEE_PROFILE) → Presigned URL 발급
     * 3. 클라이언트: PUT {uploadUrl} → S3 직접 업로드
     * 4. 클라이언트: POST /api/attendance/my-face (uploadId 전달)
     * 5. 백엔드: uploadId로 S3 URL 구성 후 DB 저장
     *
     * @param request uploadId (S3 객체 키)
     * @return ApiResponse<String> (등록된 이미지 S3 URL)
     */
    @PostMapping("/my-face")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(
        summary = "본인 얼굴 이미지 등록",
        description = """
            근로자가 본인의 얼굴 이미지를 등록합니다.

            **사전 작업:**
            1. 프론트엔드에서 face-api.js로 얼굴 1개 검출 확인
            2. POST /v1/uploads/presign 호출하여 Presigned URL 발급
            3. 발급받은 uploadUrl로 PUT 요청하여 이미지 S3 업로드
            4. 이 API에 uploadId(s3Key) 전달

            **보안:**
            - JWT 토큰의 사용자 정보로 본인만 등록 가능
            - 다른 사원의 얼굴을 등록할 수 없음

            **주의:**
            - 기존 얼굴 이미지가 있으면 덮어씌워집니다
            """
    )
    public ResponseEntity<ApiResponse<String>> registerMyFaceImage(
        @Valid @RequestBody FaceRegistrationRequestDto request
    ) {
        // JWT에서 현재 사용자 ID 추출
        String userId = SecurityUtil.getCurrentUserId();
        log.info("근로자 얼굴 등록 요청 - userId: {}, uploadId: {}", userId, request.getUploadId());

        String imageUrl = employeeFaceService.registerEmployeeFaceImage(userId, request.getUploadId());

        return ResponseEntity.ok(ApiResponse.success(
            "얼굴 이미지가 등록되었습니다.",
            imageUrl
        ));
    }
}
