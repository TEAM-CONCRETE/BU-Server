package com.concrete.buildup.domain.safetydoc.controller;

import com.concrete.buildup.domain.safetydoc.dto.*;
import com.concrete.buildup.domain.safetydoc.service.SafetyEducationService;
import com.concrete.buildup.domain.safetydoc.service.SafetyEducationSignatureService;
import com.concrete.buildup.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

    @Operation(
            summary = "안전교육일지 생성",
            description = """
                    새로운 안전교육일지를 생성하고 초안 PDF를 자동으로 생성합니다.

                    ## 생성 조건
                    - 현장에 소속된 관리자만 생성 가능
                    - 최소 1명 이상의 교육 대상자(근로자) 필요

                    ## 생성 후 상태
                    - 상태: `MANAGER_SIGNING_PENDING` (관리자 서명 대기)
                    - 초안 PDF URL이 응답에 포함됨

                    ## PDF 포함 내용
                    - 현장 정보 (현장명, 주소, 소속기업)
                    - 교육 정보 (교육일시, 교육구분, 교육과목, 교육실시자, 교육장소)
                    - 교육 내용 상세
                    - 교육 대상자 명단 (서명란 포함)
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "안전교육일지 생성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CreateSafetyEducationLogResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "message": "안전교육일지가 생성되었습니다.",
                                      "data": {
                                        "safetyEducationLogId": 1,
                                        "status": "MANAGER_SIGNING_PENDING",
                                        "pdfUrl": "https://bucket.s3.amazonaws.com/safety-docs/1/2024-01-15/SE-2024-01-15-1.pdf",
                                        "attendeeCount": 5
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (필수 필드 누락, 유효성 검사 실패)",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "success": false,
                              "message": "교육 대상자는 최소 1명 이상이어야 합니다.",
                              "data": null
                            }
                            """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "권한 없음 (현장 관리자가 아님)",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "success": false,
                              "message": "해당 현장의 관리자만 접근 가능합니다.",
                              "data": null
                            }
                            """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "현장 또는 근로자를 찾을 수 없음"
            )
    })
    @PostMapping
    public ResponseEntity<ApiResponse<CreateSafetyEducationLogResponse>> createSafetyEducationLog(
            @Parameter(description = "현장 ID", required = true, example = "1")
            @PathVariable Long siteId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "안전교육일지 생성 요청",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "educationType": "REGULAR",
                                      "educationSubject": "추락 재해 예방 교육",
                                      "educationContent": "1. 추락 재해 현황 및 사례\\n2. 추락 방지 시설 점검 요령\\n3. 개인 보호구 착용 방법\\n4. 비상 시 대응 절차",
                                      "instructorName": "김안전",
                                      "educationLocation": "현장 사무실 회의실",
                                      "attendeeEmployeeIds": [1, 2, 3, 5, 8]
                                    }
                                    """)
                    )
            )
            @Valid @RequestBody CreateSafetyEducationLogRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        CreateSafetyEducationLogResponse response = safetyEducationService.createSafetyEducationLog(
                siteId, request, userDetails.getUsername()
        );
        return ResponseEntity.ok(ApiResponse.success(response, "안전교육일지가 생성되었습니다."));
    }

    @Operation(
            summary = "안전교육일지 목록 조회",
            description = """
                    현장의 안전교육일지 목록을 조회합니다.

                    ## 조회 정보
                    - 교육 구분, 교육과목, 교육 실시자
                    - 상태 (초안/관리자 서명 대기/관리자 서명 완료/완료)
                    - 전체 참석자 수 및 서명 완료 인원 수
                    - 생성일시, PDF URL

                    ## 정렬
                    - 최신순 정렬 (생성일시 기준 내림차순)

                    ## 필터링
                    - year와 month를 함께 제공하면 해당 연도/월의 데이터만 조회됩니다.
                    - 예: year=2025&month=11 → 2025년 11월 데이터만 조회
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "message": "안전교육일지 목록을 조회했습니다.",
                                      "data": [
                                        {
                                          "id": 1,
                                          "educationType": "REGULAR",
                                          "educationSubject": "추락 재해 예방 교육",
                                          "instructorName": "김안전",
                                          "status": "COMPLETED",
                                          "totalAttendeeCount": 5,
                                          "signedAttendeeCount": 5,
                                          "createdAt": "2024-01-15T09:00:00",
                                          "pdfUrl": "https://bucket.s3.amazonaws.com/safety-docs/1/2024-01-15/SE-2024-01-15-1-final.pdf"
                                        }
                                      ]
                                    }
                                    """)
                    )
            )
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<SafetyEducationLogListResponse>>> getSafetyEducationLogs(
            @Parameter(description = "현장 ID", required = true, example = "1")
            @PathVariable Long siteId,
            @Parameter(description = "연도 (선택)", example = "2025")
            @RequestParam(required = false) Integer year,
            @Parameter(description = "월 (1-12, 선택)", example = "11")
            @RequestParam(required = false) Integer month,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        List<SafetyEducationLogListResponse> response = safetyEducationService.getSafetyEducationLogs(
                siteId, year, month, userDetails.getUsername()
        );
        return ResponseEntity.ok(ApiResponse.success(response, "안전교육일지 목록을 조회했습니다."));
    }

    @Operation(
            summary = "교육 대상자용 근로자 목록 조회",
            description = """
                    안전교육 대상자로 선택할 수 있는 현장 근로자 목록을 조회합니다.

                    ## 필터링
                    - ALL: 전체 근로자
                    - PERMANENT: 상용직 근로자만
                    - DAILY: 일용직 근로자만

                    ## 응답 정보
                    - 근로자 ID, 성명, 근로자 유형
                    - 주민등록번호 (마스킹 처리됨)
                    - 금일 안전교육 이수 여부
                    - 유형별 인원 통계
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "근로자 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "message": "근로자 목록을 조회했습니다.",
                                      "data": {
                                        "items": [
                                          {
                                            "employeeId": 1,
                                            "empName": "홍길동",
                                            "empType": "PERMANENT",
                                            "residentNum": "900101-1******",
                                            "hasSafetyEducation": false
                                          }
                                        ],
                                        "summary": {
                                          "totalCount": 10,
                                          "permanentCount": 6,
                                          "dailyCount": 4
                                        }
                                      }
                                    }
                                    """)
                    )
            )
    })
    @GetMapping("/employees")
    public ResponseEntity<ApiResponse<EmployeeListForSafetyEducationResponse>> getEmployeesForSafetyEducation(
            @Parameter(description = "현장 ID", required = true, example = "1")
            @PathVariable Long siteId,
            @Parameter(
                    description = "근로자 유형 필터",
                    schema = @Schema(allowableValues = {"ALL", "PERMANENT", "DAILY"}, defaultValue = "ALL"),
                    example = "ALL"
            )
            @RequestParam(defaultValue = "ALL") String empType,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        EmployeeListForSafetyEducationResponse response = safetyEducationService.getEmployeesForSafetyEducation(
                siteId, empType, userDetails.getUsername()
        );
        return ResponseEntity.ok(ApiResponse.success(response, "근로자 목록을 조회했습니다."));
    }

    @Operation(
            summary = "관리자 서명 처리",
            description = """
                    관리자의 서명을 처리하고 PDF에 서명을 스탬핑합니다.

                    ## 사전 조건
                    - 안전교육일지 상태: `MANAGER_SIGNING_PENDING`
                    - 현장 관리자만 서명 가능
                    - 서명 이미지가 S3에 업로드되어 있어야 함

                    ## 서명 이미지 업로드 절차
                    1. `POST /v1/uploads/signatures` 호출하여 Presigned URL 발급
                    2. 발급받은 URL로 서명 이미지 PUT 요청
                    3. 업로드된 S3 키를 이 API에 전달

                    ## 처리 결과
                    - 서명 이미지가 PDF에 스탬핑됨
                    - 새 PDF 생성 및 S3 업로드
                    - 상태 변경: `MANAGER_SIGNED`

                    ## 해시 검증
                    - 클라이언트에서 계산한 서명 이미지 SHA256 해시
                    - 서버에서 재계산하여 무결성 검증
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "관리자 서명 완료",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "message": "관리자 서명이 완료되었습니다.",
                                      "data": {
                                        "safetyEducationLogId": 1,
                                        "status": "MANAGER_SIGNED",
                                        "pdfUrl": "https://bucket.s3.amazonaws.com/safety-docs/1/2024-01-15/SE-2024-01-15-1-manager-signed.pdf",
                                        "pdfHash": null,
                                        "signedAt": "2024-01-15T10:30:00"
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 상태 또는 이미 서명됨",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "success": false,
                              "message": "현재 상태에서는 서명할 수 없습니다.",
                              "data": null
                            }
                            """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "서명 해시 불일치",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "success": false,
                              "message": "서명 이미지 해시가 일치하지 않습니다.",
                              "data": null
                            }
                            """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "권한 없음 (현장 관리자가 아님)"
            )
    })
    @PostMapping("/{logId}/signatures/manager")
    public ResponseEntity<ApiResponse<SafetyEducationSignatureResponse>> processManagerSignature(
            @Parameter(description = "현장 ID", required = true, example = "1")
            @PathVariable Long siteId,
            @Parameter(description = "안전교육일지 ID", required = true, example = "1")
            @PathVariable Long logId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "관리자 서명 요청",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "signatureS3Key": "uploads/safetydocs/1/MANAGER/1699000000000.png",
                                      "clientHash": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                                      "coordinates": {
                                        "x": 400.0,
                                        "y": 750.0,
                                        "width": 150.0,
                                        "height": 50.0,
                                        "viewWidth": 595.0,
                                        "viewHeight": 842.0
                                      }
                                    }
                                    """)
                    )
            )
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

    @Operation(
            summary = "참석자 서명 처리",
            description = """
                    교육 참석자(근로자)의 서명을 처리합니다.

                    ## 사전 조건
                    - 안전교육일지 상태: `MANAGER_SIGNED` (관리자 서명 완료 후)
                    - 해당 근로자가 교육 대상자로 등록되어 있어야 함
                    - 아직 서명하지 않은 참석자만 서명 가능

                    ## 서명 이미지 업로드 절차
                    1. `POST /v1/uploads/signatures` 호출하여 Presigned URL 발급
                    2. 발급받은 URL로 서명 이미지 PUT 요청
                    3. 업로드된 S3 키를 이 API에 전달

                    ## 처리 결과
                    - 참석자 서명 완료 처리
                    - 서명 로그 기록 (IP, 디바이스 정보 포함)

                    ## 모든 참석자 서명 완료 시
                    - 최종 PDF 생성 (관리자 서명 PDF에 모든 참석자 서명 스탬핑)
                    - 상태 변경: `COMPLETED`
                    - PDF 해시값 생성 (무결성 검증용)
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "참석자 서명 완료",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "일부 서명 완료",
                                            value = """
                                                    {
                                                      "success": true,
                                                      "message": "참석자 서명이 완료되었습니다.",
                                                      "data": {
                                                        "safetyEducationLogId": 1,
                                                        "status": "MANAGER_SIGNED",
                                                        "pdfUrl": "https://bucket.s3.amazonaws.com/safety-docs/1/2024-01-15/SE-2024-01-15-1-manager-signed.pdf",
                                                        "pdfHash": null,
                                                        "signedAt": "2024-01-15T11:00:00"
                                                      }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "모든 참석자 서명 완료 (최종)",
                                            value = """
                                                    {
                                                      "success": true,
                                                      "message": "참석자 서명이 완료되었습니다.",
                                                      "data": {
                                                        "safetyEducationLogId": 1,
                                                        "status": "COMPLETED",
                                                        "pdfUrl": "https://bucket.s3.amazonaws.com/safety-docs/1/2024-01-15/SE-2024-01-15-1-final.pdf",
                                                        "pdfHash": "a1b2c3d4e5f6...",
                                                        "signedAt": "2024-01-15T11:30:00"
                                                      }
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "이미 서명함",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "success": false,
                              "message": "이미 서명을 완료했습니다.",
                              "data": null
                            }
                            """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "권한 없음 (교육 대상자가 아님)"
            )
    })
    @PostMapping("/{logId}/signatures/attendee/{employeeId}")
    public ResponseEntity<ApiResponse<SafetyEducationSignatureResponse>> processAttendeeSignature(
            @Parameter(description = "현장 ID", required = true, example = "1")
            @PathVariable Long siteId,
            @Parameter(description = "안전교육일지 ID", required = true, example = "1")
            @PathVariable Long logId,
            @Parameter(description = "근로자(Employee) ID", required = true, example = "10")
            @PathVariable Long employeeId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "참석자(근로자) 서명 요청",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "signatureS3Key": "uploads/safetydocs/1/EMPLOYEE/10/1699000000000.png",
                                      "clientHash": "a1b2c3d4e5f6789abcdef0123456789abcdef0123456789abcdef0123456789a",
                                      "coordinates": {
                                        "x": 480.0,
                                        "y": 400.0,
                                        "width": 60.0,
                                        "height": 20.0,
                                        "viewWidth": 595.0,
                                        "viewHeight": 842.0
                                      }
                                    }
                                    """)
                    )
            )
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

    @Operation(
            summary = "참석자 서명 현황 조회",
            description = """
                    안전교육일지의 참석자별 서명 현황을 조회합니다.

                    ## 조회 정보
                    - 전체/서명완료/미서명 인원 수
                    - 참석자별 상세 정보
                      - 근로자 ID, 성명, 근로자 유형
                      - 서명 여부 및 서명 일시

                    ## 활용
                    - 서명 진행 상황 모니터링
                    - 미서명자 확인 및 서명 요청
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "서명 현황 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "message": "서명 현황을 조회했습니다.",
                                      "data": {
                                        "safetyEducationLogId": 1,
                                        "totalCount": 5,
                                        "signedCount": 3,
                                        "unsignedCount": 2,
                                        "attendees": [
                                          {
                                            "employeeId": 10,
                                            "empName": "홍길동",
                                            "empType": "PERMANENT",
                                            "isSigned": true,
                                            "signedAt": "2024-01-15T11:00:00"
                                          },
                                          {
                                            "employeeId": 11,
                                            "empName": "김철수",
                                            "empType": "DAILY",
                                            "isSigned": false,
                                            "signedAt": null
                                          }
                                        ]
                                      }
                                    }
                                    """)
                    )
            )
    })
    @GetMapping("/{logId}/attendees/signature-status")
    public ResponseEntity<ApiResponse<AttendeeSignatureStatusResponse>> getAttendeeSignatureStatus(
            @Parameter(description = "현장 ID", required = true, example = "1")
            @PathVariable Long siteId,
            @Parameter(description = "안전교육일지 ID", required = true, example = "1")
            @PathVariable Long logId,
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
