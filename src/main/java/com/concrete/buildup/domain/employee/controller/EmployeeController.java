package com.concrete.buildup.domain.employee.controller;

import com.concrete.buildup.domain.employee.dto.EmployeeDetailResponseDto;
import com.concrete.buildup.domain.employee.dto.EmployeePageResponseDto;
import com.concrete.buildup.domain.employee.service.EmployeeService;
import com.concrete.buildup.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 사원 관리 Controller
 *
 * <p>사원 목록 조회 등의 REST API를 제공합니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping("/sites/{siteId}/employees")
@RequiredArgsConstructor
@Validated
@Tag(name = "Employee", description = "사원 관리 API")
public class EmployeeController {

    private final EmployeeService employeeService;

    /**
     * 현장 기반 사원 목록 조회
     *
     * @param siteId 현장 ID
     * @param empType 근로자 유형 필터 (DAILY/PERMANENT)
     * @param name 이름 검색어
     * @param page 페이지 번호 (1부터 시작, 기본값 1)
     * @param size 페이지 크기 (기본값 10)
     * @return 사원 목록 (페이징)
     */
    @GetMapping
    @Operation(summary = "사원 목록 조회", description = "현장에 소속된 사원 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<EmployeePageResponseDto>> getEmployees(
            @Parameter(description = "현장 ID", required = true)
            @PathVariable Long siteId,

            @Parameter(description = "근로자 유형 (DAILY/PERMANENT)")
            @RequestParam(required = false) String empType,

            @Parameter(description = "이름 검색어")
            @RequestParam(required = false) String name,

            @Parameter(description = "페이지 번호 (1부터 시작)")
            @RequestParam(defaultValue = "1") @Min(1) int page,

            @Parameter(description = "페이지 크기")
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size
    ) {
        log.info("사원 목록 조회 API 호출: siteId={}, empType={}, name={}, page={}, size={}",
                siteId, empType, name, page, size);

        EmployeePageResponseDto response = employeeService.getEmployeesBySite(
                siteId, empType, name, page, size
        );

        return ResponseEntity.ok(ApiResponse.success(response, "사원 목록 조회에 성공했습니다"));
    }

    /**
     * 사원 상세 조회
     *
     * @param siteId 현장 ID
     * @param employeeId 사원 ID
     * @return 사원 상세 정보
     */
    @GetMapping("/{employeeId}")
    @Operation(
        summary = "사원 상세 조회",
        description = """
            현장에 소속된 사원의 상세 정보를 조회합니다.

            **조회 정보:**
            - 기본 정보: 이름, 주민번호(마스킹), 연락처, 이메일, 주소
            - 근무 정보: 입사일, 퇴사일, 사원 구분
            - 비상 연락망

            **권한:**
            - 해당 현장의 SiteManager만 조회 가능
            - 현장에 소속되지 않은 사원은 조회 불가
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "사원 상세 조회 성공",
            content = @Content(schema = @Schema(implementation = EmployeeDetailResponseDto.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "현장 또는 사원을 찾을 수 없음"
        )
    })
    public ResponseEntity<ApiResponse<EmployeeDetailResponseDto>> getEmployeeDetail(
            @Parameter(description = "현장 ID", required = true)
            @PathVariable Long siteId,

            @Parameter(description = "사원 ID", required = true)
            @PathVariable Long employeeId
    ) {
        log.info("사원 상세 조회 API 호출: siteId={}, employeeId={}", siteId, employeeId);

        EmployeeDetailResponseDto response = employeeService.getEmployeeDetail(siteId, employeeId);

        return ResponseEntity.ok(ApiResponse.success(response, "사원 상세 조회에 성공했습니다"));
    }
}