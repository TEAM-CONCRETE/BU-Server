package com.concrete.buildup.domain.employee.controller;

import com.concrete.buildup.domain.employee.dto.EmployeePageResponseDto;
import com.concrete.buildup.domain.employee.service.EmployeeService;
import com.concrete.buildup.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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
            @RequestParam(defaultValue = "1") int page,

            @Parameter(description = "페이지 크기")
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("사원 목록 조회 API 호출: siteId={}, empType={}, name={}, page={}, size={}",
                siteId, empType, name, page, size);

        EmployeePageResponseDto response = employeeService.getEmployeesBySite(
                siteId, empType, name, page, size
        );

        return ResponseEntity.ok(ApiResponse.success(response, "사원 목록 조회에 성공했습니다"));
    }
}