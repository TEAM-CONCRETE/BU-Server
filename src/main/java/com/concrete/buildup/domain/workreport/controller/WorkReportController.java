package com.concrete.buildup.domain.workreport.controller;

import com.concrete.buildup.domain.workreport.dto.CreateWorkReportRequest;
import com.concrete.buildup.domain.workreport.dto.CreateWorkReportResponse;
import com.concrete.buildup.domain.workreport.service.WorkReportService;
import com.concrete.buildup.global.common.ApiResponse;
import com.concrete.buildup.global.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
     * <p>비즈니스 로직:</p>
     * <ul>
     *   <li>현장 존재 여부 검증</li>
     *   <li>관리자 권한 검증</li>
     *   <li>중복 작성 검증 (동일 현장, 동일 날짜)</li>
     *   <li>작업일보 DB 저장 (WorkReport + WorkReportMaterial)</li>
     *   <li>PDF 생성 (Thymeleaf 템플릿 + Flying Saucer)</li>
     *   <li>S3 업로드 (work-reports/{siteId}/{workReportId}/WR-{date}.pdf)</li>
     *   <li>PDF URL 및 생성 시각 저장</li>
     * </ul>
     *
     * @param siteId 현장 ID
     * @param request 작업일보 생성 요청 DTO
     * @return CreateWorkReportResponse - 생성된 작업일보 ID와 PDF URL
     */
    @Operation(
            summary = "작업일보 생성",
            description = "관리자가 작업일보를 생성합니다. " +
                    "PDF가 즉시 생성되어 S3에 업로드되며, 생성 완료 후 PDF URL이 반환됩니다. " +
                    "동일 현장의 동일 날짜에 이미 작업일보가 존재하면 중복 오류가 발생합니다."
    )
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
}
