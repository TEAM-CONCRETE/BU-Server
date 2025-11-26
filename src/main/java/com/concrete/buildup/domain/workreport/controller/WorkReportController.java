package com.concrete.buildup.domain.workreport.controller;

import com.concrete.buildup.domain.workreport.dto.CreateWorkReportRequest;
import com.concrete.buildup.domain.workreport.dto.CreateWorkReportResponse;
import com.concrete.buildup.domain.workreport.dto.WorkReportListResponse;
import com.concrete.buildup.domain.workreport.service.WorkReportService;
import com.concrete.buildup.global.common.ApiResponse;
import com.concrete.buildup.global.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 작업일보 관리 컨트롤러
 *
 * <p>작업일보 생성, 조회 등 작업일보 관련 API를 제공합니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping("/v1/{siteId}/work-reports")
@RequiredArgsConstructor
@Validated
@Tag(name = "WorkReport", description = "작업일보 관리 API")
public class WorkReportController {

    private final WorkReportService workReportService;

    /**
     * 작업일보 생성 API
     *
     * <p>관리자가 작업일보를 생성하면 즉시 PDF가 생성되어 S3에 업로드됩니다.</p>
     * <p>동일 현장의 동일 날짜에 여러 개의 작업일보를 생성할 수 있으며,
     * 생성 순서대로 순번(sequence)이 부여됩니다.</p>
     * <p>비즈니스 로직:</p>
     * <ul>
     *   <li>현장 존재 여부 검증</li>
     *   <li>관리자 권한 검증</li>
     *   <li>작업일보 DB 저장 (WorkReport + WorkReportMaterial)</li>
     *   <li>PDF 생성 (Thymeleaf 템플릿 + Flying Saucer)</li>
     *   <li>S3 업로드 (work-reports/{siteId}/{date}/WR-{date}-{sequence}.pdf)</li>
     *   <li>PDF URL 및 생성 시각 저장</li>
     * </ul>
     *
     * @param siteId 현장 ID
     * @param request 작업일보 생성 요청 DTO
     * @return CreateWorkReportResponse - 생성된 작업일보 ID와 PDF URL
     */
    @Operation(
            summary = "작업일보 생성",
            description = """
                관리자가 작업일보를 생성합니다.

                **처리 과정:**
                1. 작업일보 DB 저장
                2. PDF 즉시 생성 및 S3 업로드
                3. PDF URL 반환

                **파일명 규칙:** `WR-{날짜}-{순번}.pdf` (예: WR-2025-11-24-1.pdf)
                """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "작업일보 생성 성공",
                    content = @Content(schema = @Schema(implementation = CreateWorkReportResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (필수 필드 누락, 유효성 검증 실패)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "현장을 찾을 수 없음"
            )
    })
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<CreateWorkReportResponse>> createWorkReport(
            @Parameter(description = "현장 ID", required = true, example = "1")
            @PathVariable Long siteId,
            @Valid @RequestBody CreateWorkReportRequest request
    ) {
        log.info("작업일보 생성 API 호출: siteId={}", siteId);

        // JWT에서 관리자 userId 추출
        String currentUserId = SecurityUtil.getCurrentUserId();

        CreateWorkReportResponse response = workReportService.createWorkReport(siteId, request, currentUserId);

        log.info("작업일보 생성 완료: siteId={}, workReportId={}, pdfUrl={}",
                siteId, response.getWorkReportId(), response.getPdfUrl());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "작업일보가 생성되었습니다"));
    }

    /**
     * 작업일보 목록 조회 API (현장 관리자용)
     *
     * <p>현장에 속한 작업일보 목록을 페이지네이션하여 조회합니다.</p>
     *
     * @param siteId 현장 ID
     * @param pageable 페이지네이션 정보 (기본값: page=0, size=20)
     * @return WorkReportListResponse - 작업일보 목록
     */
    @Operation(
            summary = "작업일보 목록 조회 (현장 관리자용)",
            description = """
                현장에 속한 작업일보 목록을 조회합니다.

                **조회 정보:**
                - 작업일보 ID
                - 작업일자
                - 작성자 이름

                **정렬:** 최신 작성일순 (내림차순)

                **필터링:**
                - year와 month를 함께 제공하면 해당 연도/월의 데이터만 조회됩니다.
                - 예: year=2025&month=11 → 2025년 11월 데이터만 조회
                """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = WorkReportListResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "현장을 찾을 수 없음"
            )
    })
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<WorkReportListResponse>> getWorkReportList(
            @Parameter(description = "현장 ID", required = true, example = "1")
            @PathVariable Long siteId,
            @Parameter(description = "연도 (선택)", example = "2025")
            @RequestParam(required = false) Integer year,
            @Parameter(description = "월 (1-12, 선택)", example = "11")
            @RequestParam(required = false) @Min(value = 1, message = "월은 1 이상이어야 합니다") @Max(value = 12, message = "월은 12 이하여야 합니다") Integer month,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @PageableDefault(size = 20) Pageable pageable
    ) {
        log.info("작업일보 목록 조회 API 호출: siteId={}, year={}, month={}, page={}, size={}",
                siteId, year, month, pageable.getPageNumber(), pageable.getPageSize());

        WorkReportListResponse response = workReportService.getWorkReportList(siteId, year, month, pageable);

        log.info("작업일보 목록 조회 완료: siteId={}, totalElements={}", siteId, response.getTotalElements());

        return ResponseEntity.ok(
                ApiResponse.success(response, "작업일보 목록을 조회했습니다")
        );
    }
}
