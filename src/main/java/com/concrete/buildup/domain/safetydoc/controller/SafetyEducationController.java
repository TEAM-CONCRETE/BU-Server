package com.concrete.buildup.domain.safetydoc.controller;

import com.concrete.buildup.domain.safetydoc.dto.*;
import com.concrete.buildup.domain.safetydoc.service.SafetyEducationService;
import com.concrete.buildup.domain.safetydoc.service.SafetyEducationSignatureService;
import com.concrete.buildup.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Safety Education", description = "안전교육일지 관리 API")
@RestController
@RequestMapping("/v1/{siteId}/safety-education-logs")
@RequiredArgsConstructor
public class SafetyEducationController {

    private final SafetyEducationService safetyEducationService;
    private final SafetyEducationSignatureService signatureService;

    @Operation(summary = "안전교육일지 생성", description = "새로운 안전교육일지를 생성하고 초안 PDF를 생성합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<CreateSafetyEducationLogResponse>> createSafetyEducationLog(
            @Parameter(description = "현장 ID") @PathVariable Long siteId,
            @Valid @RequestBody CreateSafetyEducationLogRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        CreateSafetyEducationLogResponse response = safetyEducationService.createSafetyEducationLog(
                siteId, request, userDetails.getUsername()
        );
        return ResponseEntity.ok(ApiResponse.success(response, "안전교육일지가 생성되었습니다."));
    }

    @Operation(summary = "안전교육일지 목록 조회", description = "현장의 안전교육일지 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<SafetyEducationLogListResponse>>> getSafetyEducationLogs(
            @Parameter(description = "현장 ID") @PathVariable Long siteId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        List<SafetyEducationLogListResponse> response = safetyEducationService.getSafetyEducationLogs(
                siteId, userDetails.getUsername()
        );
        return ResponseEntity.ok(ApiResponse.success(response, "안전교육일지 목록을 조회했습니다."));
    }

    @Operation(summary = "안전교육일지 상세 조회", description = "안전교육일지의 상세 정보를 조회합니다.")
    @GetMapping("/{logId}")
    public ResponseEntity<ApiResponse<SafetyEducationLogDetailResponse>> getSafetyEducationLogDetail(
            @Parameter(description = "현장 ID") @PathVariable Long siteId,
            @Parameter(description = "안전교육일지 ID") @PathVariable Long logId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        SafetyEducationLogDetailResponse response = safetyEducationService.getSafetyEducationLogDetail(
                siteId, logId, userDetails.getUsername()
        );
        return ResponseEntity.ok(ApiResponse.success(response, "안전교육일지 상세 정보를 조회했습니다."));
    }

    @Operation(summary = "교육 대상자용 근로자 목록 조회", description = "안전교육 대상자로 선택할 수 있는 현장 근로자 목록을 조회합니다.")
    @GetMapping("/employees")
    public ResponseEntity<ApiResponse<EmployeeListForSafetyEducationResponse>> getEmployeesForSafetyEducation(
            @Parameter(description = "현장 ID") @PathVariable Long siteId,
            @Parameter(description = "근로자 유형 필터 (ALL, PERMANENT, DAILY)") @RequestParam(defaultValue = "ALL") String empType,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        EmployeeListForSafetyEducationResponse response = safetyEducationService.getEmployeesForSafetyEducation(
                siteId, empType, userDetails.getUsername()
        );
        return ResponseEntity.ok(ApiResponse.success(response, "근로자 목록을 조회했습니다."));
    }

    @Operation(summary = "관리자 서명 처리", description = "관리자의 서명을 처리하고 PDF에 서명을 추가합니다.")
    @PostMapping("/{logId}/signatures/manager")
    public ResponseEntity<ApiResponse<SafetyEducationSignatureResponse>> processManagerSignature(
            @Parameter(description = "현장 ID") @PathVariable Long siteId,
            @Parameter(description = "안전교육일지 ID") @PathVariable Long logId,
            @Valid @RequestBody SafetyEducationSignatureRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest
    ) {
        String signedIp = getClientIp(httpRequest);
        String signedDevice = httpRequest.getHeader("User-Agent");

        SafetyEducationSignatureResponse response = signatureService.processManagerSignature(
                siteId, logId, request, signedIp, signedDevice, userDetails.getUsername()
        );
        return ResponseEntity.ok(ApiResponse.success(response, "관리자 서명이 완료되었습니다."));
    }

    @Operation(summary = "참석자 서명 처리", description = "교육 참석자의 서명을 처리합니다.")
    @PostMapping("/{logId}/signatures/attendee/{employeeId}")
    public ResponseEntity<ApiResponse<SafetyEducationSignatureResponse>> processAttendeeSignature(
            @Parameter(description = "현장 ID") @PathVariable Long siteId,
            @Parameter(description = "안전교육일지 ID") @PathVariable Long logId,
            @Parameter(description = "근로자 ID") @PathVariable Long employeeId,
            @Valid @RequestBody SafetyEducationSignatureRequest request,
            HttpServletRequest httpRequest
    ) {
        String signedIp = getClientIp(httpRequest);
        String signedDevice = httpRequest.getHeader("User-Agent");

        SafetyEducationSignatureResponse response = signatureService.processAttendeeSignature(
                siteId, logId, employeeId, request, signedIp, signedDevice
        );
        return ResponseEntity.ok(ApiResponse.success(response, "참석자 서명이 완료되었습니다."));
    }

    @Operation(summary = "참석자 서명 현황 조회", description = "안전교육일지의 참석자별 서명 현황을 조회합니다.")
    @GetMapping("/{logId}/attendees/signature-status")
    public ResponseEntity<ApiResponse<AttendeeSignatureStatusResponse>> getAttendeeSignatureStatus(
            @Parameter(description = "현장 ID") @PathVariable Long siteId,
            @Parameter(description = "안전교육일지 ID") @PathVariable Long logId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        AttendeeSignatureStatusResponse response = safetyEducationService.getAttendeeSignatureStatus(
                siteId, logId, userDetails.getUsername()
        );
        return ResponseEntity.ok(ApiResponse.success(response, "서명 현황을 조회했습니다."));
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
