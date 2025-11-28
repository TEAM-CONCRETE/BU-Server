package com.concrete.buildup.domain.site.controller;

import com.concrete.buildup.domain.site.dto.DashboardResponse;
import com.concrete.buildup.domain.site.dto.SafetyWorkDocumentListResponse;
import com.concrete.buildup.domain.site.dto.SiteContractInfoResponse;
import com.concrete.buildup.domain.site.dto.SiteCreateRequest;
import com.concrete.buildup.domain.site.dto.SiteCreateResponse;
import com.concrete.buildup.domain.site.dto.SiteDetailResponse;
import com.concrete.buildup.domain.site.dto.SiteListResponse;
import com.concrete.buildup.domain.site.service.DashboardService;
import com.concrete.buildup.domain.site.service.SiteService;
import com.concrete.buildup.global.common.ApiResponse;
import com.concrete.buildup.global.util.MaskingUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 현장 관리 컨트롤러
 *
 * <p>현장 등록, 조회 등 현장 관리 API를 제공합니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping("/v1/sites")
@RequiredArgsConstructor
@Validated
@Tag(name = "Site", description = "현장 관리 API")
public class SiteController {

    private final SiteService siteService;
    private final DashboardService dashboardService;

    /**
     * 현장 등록 API
     *
     * <p>CORPORATION 권한 사용자가 새로운 건설 현장을 등록합니다.</p>
     * <p>현장 등록 시 자동으로 현장 관리자용/근로자용 시크릿키가 생성됩니다.</p>
     *
     * @param request 현장 등록 요청 정보 (현장명, 주소, 발주처, 공사 기간)
     * @return SiteCreateResponse - 생성된 현장 정보 및 시크릿키
     */
    @Operation(
            summary = "현장 등록",
            description = "CORPORATION 권한 사용자가 새로운 건설 현장을 등록합니다. " +
                    "현장 등록 시 자동으로 현장 관리자용 시크릿키와 근로자용 시크릿키가 생성됩니다. " +
                    "생성된 시크릿키는 해당 현장의 관리자/근로자 회원가입 시 사용됩니다."
    )
    @PostMapping
    @PreAuthorize("hasRole('CORPORATION')")
    public ResponseEntity<ApiResponse<SiteCreateResponse>> createSite(
            @Valid @RequestBody SiteCreateRequest request
    ) {
        log.info("현장 등록 API 호출: siteName={}", request.getSiteName());

        SiteCreateResponse response = siteService.createSite(request);

        log.info("현장 등록 완료: siteId={}, managerSecretKey={}, employeeSecretKey={}",
                response.getSiteId(),
                MaskingUtil.maskSecretKey(response.getManagerSecretKey()),
                MaskingUtil.maskSecretKey(response.getEmployeeSecretKey()));

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "현장이 성공적으로 등록되었습니다"));
    }

    /**
     * 기업 관리자의 현장 목록 조회 API
     *
     * <p>현재 로그인한 기업 관리자가 담당하는 모든 현장 목록을 조회합니다.</p>
     * <p>왼쪽 사이드바에 현장 목록을 표시하는 데 사용됩니다.</p>
     *
     * @return SiteListResponse - 현장 목록
     */
    @Operation(
            summary = "기업 관리자의 현장 목록 조회",
            description = "현재 로그인한 기업 관리자가 담당하는 모든 현장 목록을 조회합니다. " +
                    "왼쪽 사이드바에 현장 목록을 표시하는 데 사용됩니다. " +
                    "각 현장의 ID, 이름, 주소, 관리자 이름을 반환합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "message": "현장 목록을 성공적으로 조회했습니다",
                                      "data": {
                                        "sites": [
                                          {
                                            "siteId": 1,
                                            "siteName": "강남 오피스텔 신축현장",
                                            "siteAddress": "서울특별시 강남구 역삼동 123-45",
                                            "managerName": "이강남"
                                          },
                                          {
                                            "siteId": 2,
                                            "siteName": "송파 아파트 리모델링",
                                            "siteAddress": "서울특별시 송파구 잠실동 456-78",
                                            "managerName": "박송파"
                                          }
                                        ],
                                        "totalCount": 2
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "기업 정보를 찾을 수 없음"
            )
    })
    @GetMapping
    @PreAuthorize("hasRole('CORPORATION')")
    public ResponseEntity<ApiResponse<SiteListResponse>> getMySites() {
        log.info("기업 현장 목록 조회 API 호출");

        SiteListResponse response = siteService.getMySites();

        log.info("기업 현장 목록 조회 완료: totalCount={}", response.getTotalCount());

        return ResponseEntity.ok(
                ApiResponse.success(response, "현장 목록을 성공적으로 조회했습니다")
        );
    }

    /**
     * 현장 상세 조회 API
     *
     * <p>현장 ID로 현장 상세 정보를 조회합니다.</p>
     * <p>작업일보 작성 페이지 등에서 현장 기본 정보를 표시하는 데 사용됩니다.</p>
     *
     * @param siteId 현장 ID
     * @return SiteDetailResponse - 현장 상세 정보
     */
    @Operation(
            summary = "현장 상세 조회",
            description = "현장 ID로 현장 상세 정보를 조회합니다. " +
                    "현장명, 주소, 공사 기간, 관리자 이름 등의 정보를 반환합니다. " +
                    "작업일보 작성 페이지에서 현장 기본 정보를 자동으로 표시하는 데 사용됩니다."
    )
    @GetMapping("/{siteId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'CORPORATION', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<SiteDetailResponse>> getSiteDetail(
            @Parameter(description = "현장 ID", required = true, example = "1")
            @PathVariable Long siteId
    ) {
        log.info("현장 상세 조회 API 호출: siteId={}", siteId);

        SiteDetailResponse response = siteService.getSiteById(siteId);

        log.info("현장 상세 조회 완료: siteId={}, siteName={}", siteId, response.getSiteName());

        return ResponseEntity.ok(
                ApiResponse.success(response, "현장 정보를 성공적으로 조회했습니다")
        );
    }

    /**
     * 안전/작업 문서 목록 조회 API (기업 관리자용)
     *
     * <p>현장의 안전교육일지와 작업일보를 날짜별로 통합 조회합니다.</p>
     *
     * @param siteId 현장 ID
     * @param pageable 페이지네이션 정보 (기본값: page=0, size=20)
     * @return SafetyWorkDocumentListResponse - 날짜별 문서 목록
     */
    @Operation(
            summary = "안전/작업 문서 목록 조회 (기업 관리자용)",
            description = """
                    현장의 안전교육일지와 작업일보를 날짜별로 통합 조회합니다.

                    **조회 정보:**
                    - 날짜별 안전교육일지 (ID, 상태, 교육과목)
                    - 날짜별 작업일보 (ID, 순번)

                    **정렬:** 최신 날짜순 (내림차순)

                    **필터링:**
                    - year와 month를 함께 제공하면 해당 연도/월의 데이터만 조회됩니다.
                    - 예: year=2025&month=11 → 2025년 11월 데이터만 조회
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "message": "안전/작업 문서 목록을 조회했습니다.",
                                      "data": {
                                        "content": [
                                          {
                                            "date": "2025-11-24",
                                            "safetyEducationLog": {
                                              "logId": 15,
                                              "status": "COMPLETED",
                                              "educationSubject": "추락 재해 예방 교육"
                                            },
                                            "workReport": {
                                              "workReportId": 23,
                                              "sequence": 1
                                            }
                                          },
                                          {
                                            "date": "2025-11-23",
                                            "safetyEducationLog": null,
                                            "workReport": {
                                              "workReportId": 22,
                                              "sequence": 2
                                            }
                                          }
                                        ],
                                        "pageNumber": 0,
                                        "pageSize": 20,
                                        "totalElements": 50,
                                        "totalPages": 3,
                                        "first": true,
                                        "last": false
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "현장을 찾을 수 없음"
            )
    })
    @GetMapping("/{siteId}/safety-work-documents")
    @PreAuthorize("hasRole('CORPORATION')")
    public ResponseEntity<ApiResponse<SafetyWorkDocumentListResponse>> getSafetyWorkDocuments(
            @Parameter(description = "현장 ID", required = true, example = "1")
            @PathVariable Long siteId,
            @Parameter(description = "연도 (선택)", example = "2025")
            @RequestParam(required = false) Integer year,
            @Parameter(description = "월 (1-12, 선택)", example = "11")
            @RequestParam(required = false) @Min(value = 1, message = "월은 1 이상이어야 합니다") @Max(value = 12, message = "월은 12 이하여야 합니다") Integer month,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @PageableDefault(size = 20) Pageable pageable
    ) {
        log.info("안전/작업 문서 목록 조회 API 호출: siteId={}, year={}, month={}, page={}, size={}",
                siteId, year, month, pageable.getPageNumber(), pageable.getPageSize());

        SafetyWorkDocumentListResponse response = siteService.getSafetyWorkDocuments(siteId, year, month, pageable);

        log.info("안전/작업 문서 목록 조회 완료: siteId={}, totalElements={}", siteId, response.getTotalElements());

        return ResponseEntity.ok(
                ApiResponse.success(response, "안전/작업 문서 목록을 조회했습니다.")
        );
    }

    /**
     * 현장 대시보드 조회 API
     *
     * <p>기업 관리자와 현장 관리자가 현장의 대시보드 정보를 조회합니다.</p>
     * <p>현장 기본 정보, 인원 현황, 안전 현황, 노무 현황을 포함합니다.</p>
     *
     * @param siteId 현장 ID
     * @return DashboardResponse - 대시보드 정보
     */
    @Operation(
            summary = "현장 대시보드 조회",
            description = """
                    현장의 대시보드 정보를 조회합니다.

                    **포함 정보:**
                    - 현장 기본 정보: 현장명, 주소, 발주처, 공사 기간, 진행률, 관리자 이름
                    - 인원 현황: 총 근로자 수, 상용직/일용직 수, 금일 출근 인원, 금일 지각 인원
                    - 안전 현황: 안전율, 금일 안전 경고, 교육 미이수 인원, 안전점검 완료 건수
                    - 노무 현황: 미결 전자계약 수, 미결 계약 목록 (근로계약서, 안전교육일지)

                    **사용 시나리오:**
                    - 기업 관리자: 왼쪽 사이드바에서 현장 선택 후 대시보드 확인
                    - 현장 관리자: 자신이 담당하는 현장의 대시보드 확인
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "message": "대시보드 정보를 성공적으로 조회했습니다.",
                                      "data": {
                                        "siteInfo": {
                                          "siteId": 1,
                                          "siteName": "강남 오피스텔 신축현장",
                                          "siteAddress": "서울특별시 강남구 역삼동 123-45",
                                          "clientName": "서울시설공단",
                                          "startDate": "2025-01-01",
                                          "endDate": "2025-12-31",
                                          "progressRate": 45.5,
                                          "managerName": "김현장"
                                        },
                                        "workforceStatus": {
                                          "totalWorkers": 50,
                                          "permanentWorkers": 30,
                                          "dailyWorkers": 20,
                                          "todayAttendance": 45,
                                          "todayLateCount": 3
                                        },
                                        "safetyStatus": {
                                          "safetyRate": 92.5,
                                          "todayWarnings": 2,
                                          "incompletedEducation": 5,
                                          "completedInspections": 10
                                        },
                                        "laborStatus": {
                                          "totalPendingContracts": 3,
                                          "pendingContracts": [
                                            {
                                              "contractId": 101,
                                              "contractType": "근로계약서",
                                              "targetName": "김철수",
                                              "contractState": "MANAGER_SIGNING_PENDING"
                                            },
                                            {
                                              "contractId": 15,
                                              "contractType": "안전교육일지",
                                              "targetName": "추락 재해 예방 교육",
                                              "contractState": "MANAGER_SIGNING_PENDING"
                                            }
                                          ]
                                        }
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "현장을 찾을 수 없음"
            )
    })
    @GetMapping("/{siteId}/dashboard")
    @PreAuthorize("hasAnyRole('MANAGER', 'CORPORATION')")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard(
            @Parameter(description = "현장 ID", required = true, example = "1")
            @PathVariable Long siteId
    ) {
        log.info("대시보드 조회 API 호출: siteId={}", siteId);

        DashboardResponse response = dashboardService.getDashboard(siteId);

        log.info("대시보드 조회 완료: siteId={}", siteId);

        return ResponseEntity.ok(
                ApiResponse.success(response, "대시보드 정보를 성공적으로 조회했습니다.")
        );
    }

    /**
     * 근로계약서 작성용 기업/현장 정보 조회 API
     *
     * <p>현장 관리자가 근로계약서 작성 시 필요한 기업 및 현장 정보를 조회합니다.</p>
     * <p>현재 로그인한 사용자의 현장 정보를 기반으로 기업 정보를 조회합니다.</p>
     *
     * @return SiteContractInfoResponse - 기업/현장 정보
     */
    @Operation(
            summary = "근로계약서 작성용 기업/현장 정보 조회",
            description = "현장 관리자가 근로계약서 작성 시 필요한 기업 및 현장 정보를 조회합니다. " +
                    "현재 로그인한 사용자의 siteId를 기반으로 현장과 기업 정보를 반환합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "message": "기업/현장 정보 조회가 완료되었습니다",
                                      "data": {
                                        "siteId": 1,
                                        "siteName": "세종대학교 AI 센터 재개발 현장",
                                        "siteAddress": "서울시 광진구 능동로 98",
                                        "corporation": {
                                          "corporationId": 1,
                                          "corpName": "빌드업건설(주)",
                                          "corpCeoName": "김대표",
                                          "corpAddress": "서울시 강남구 테헤란로 123"
                                        }
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "현장 또는 사용자를 찾을 수 없음"
            )
    })
    @GetMapping("/contract-info")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<SiteContractInfoResponse>> getContractFormInfo() {
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("근로계약서 작성용 정보 조회 API 호출: userId={}", currentUserId);

        SiteContractInfoResponse response = siteService.getContractFormInfo(currentUserId);

        log.info("근로계약서 작성용 정보 조회 완료: siteId={}", response.getSiteId());

        return ResponseEntity.ok(ApiResponse.success(response, "기업/현장 정보 조회가 완료되었습니다"));
    }
}
