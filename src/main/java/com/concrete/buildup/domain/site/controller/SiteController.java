package com.concrete.buildup.domain.site.controller;

import com.concrete.buildup.domain.site.dto.SafetyWorkDocumentListResponse;
import com.concrete.buildup.domain.site.dto.SiteCreateRequest;
import com.concrete.buildup.domain.site.dto.SiteCreateResponse;
import com.concrete.buildup.domain.site.dto.SiteDetailResponse;
import com.concrete.buildup.domain.site.dto.SiteListResponse;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@Tag(name = "Site", description = "현장 관리 API")
public class SiteController {

    private final SiteService siteService;

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
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @PageableDefault(size = 20) Pageable pageable
    ) {
        log.info("안전/작업 문서 목록 조회 API 호출: siteId={}, page={}, size={}",
                siteId, pageable.getPageNumber(), pageable.getPageSize());

        SafetyWorkDocumentListResponse response = siteService.getSafetyWorkDocuments(siteId, pageable);

        log.info("안전/작업 문서 목록 조회 완료: siteId={}, totalElements={}", siteId, response.getTotalElements());

        return ResponseEntity.ok(
                ApiResponse.success(response, "안전/작업 문서 목록을 조회했습니다.")
        );
    }
}
