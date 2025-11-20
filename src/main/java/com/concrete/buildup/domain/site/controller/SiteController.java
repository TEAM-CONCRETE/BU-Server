package com.concrete.buildup.domain.site.controller;

import com.concrete.buildup.domain.site.dto.SiteCreateRequest;
import com.concrete.buildup.domain.site.dto.SiteCreateResponse;
import com.concrete.buildup.domain.site.dto.SiteDetailResponse;
import com.concrete.buildup.domain.site.service.SiteService;
import com.concrete.buildup.global.common.ApiResponse;
import com.concrete.buildup.global.util.MaskingUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
}
