package com.concrete.buildup.domain.contract.controller;

import com.concrete.buildup.domain.contract.dto.ContractListResponse;
import com.concrete.buildup.domain.contract.dto.ContractSearchCondition;
import com.concrete.buildup.domain.contract.dto.CreateContractRequest;
import com.concrete.buildup.domain.contract.dto.CreateContractResponse;
import com.concrete.buildup.domain.contract.enums.EmpType;
import com.concrete.buildup.domain.contract.service.ContractService;
import com.concrete.buildup.global.common.ApiResponse;
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
 * 계약 관리 컨트롤러
 *
 * <p>근로계약 생성, 조회, 서명 등 계약 관련 API를 제공합니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping("/v1/{siteId}/contracts")
@RequiredArgsConstructor
@Validated
@Tag(name = "Contract", description = "계약 관리 API")
public class ContractController {

    private final ContractService contractService;

    /**
     * 계약 목록 조회 API
     *
     * <p>현장별 계약 목록을 필터링 및 페이징하여 조회합니다.</p>
     * <p>검색 조건:</p>
     * <ul>
     *   <li>employeeId: 근로자 ID로 필터링</li>
     *   <li>empType: 근로자 유형(DAILY/PERMANENT)으로 필터링</li>
     *   <li>status: 계약 상태(DRAFT/PENDING/FULLY_SIGNED)로 필터링</li>
     *   <li>from: 계약 시작일 범위 시작</li>
     *   <li>to: 계약 시작일 범위 종료</li>
     *   <li>page: 페이지 번호 (기본값: 1)</li>
     *   <li>size: 페이지 크기 (기본값: 20, 최대: 100)</li>
     * </ul>
     *
     * @param siteId 현장 ID
     * @param condition 검색 조건
     * @return ContractListResponse - 계약 목록 + 페이징 정보
     */
    @Operation(
            summary = "계약 목록 조회",
            description = "현장별 계약 목록을 필터링 및 페이징하여 조회합니다. " +
                    "모든 검색 조건은 선택적이며, 주민등록번호는 마스킹 처리되어 반환됩니다. " +
                    "예: GET /v1/1/contracts?employeeId=1&status=FULLY_SIGNED&page=1&size=20"
    )
    @PreAuthorize("hasAnyRole('MANAGER', 'CORPORATION', 'ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<ContractListResponse>> getContracts(
            @Parameter(description = "현장 ID", required = true, example = "1")
            @PathVariable Long siteId,
            @Valid @ModelAttribute ContractSearchCondition condition
    ) {
        log.info("계약 목록 조회 API 호출: siteId={}, condition={}", siteId, condition);

        ContractListResponse response = contractService.getContracts(siteId, condition);

        log.info("계약 목록 조회 완료: siteId={}, totalElements={}",
                siteId, response.getPageInfo().getTotalElements());

        return ResponseEntity.ok(ApiResponse.success(response, "계약 목록 조회가 완료되었습니다"));
    }

    /**
     * 상용직 계약 생성 API
     *
     * <p>상용직(PERMANENT) 근로계약을 생성합니다.</p>
     * <p>비즈니스 로직:</p>
     * <ul>
     *   <li>Employee, Corporation, Manager 존재 여부 검증</li>
     *   <li>근로자의 기존 FULLY_SIGNED 계약 타입 검증</li>
     *   <li>다른 타입의 계약이 있으면 422 에러 반환</li>
     *   <li>Contract + ContractDetail 생성 (초기 상태: DRAFT)</li>
     *   <li>스냅샷 데이터 저장 (계약 당시 기업/근로자 정보)</li>
     * </ul>
     *
     * @param siteId 현장 ID
     * @param request 계약 생성 요청 DTO
     * @return CreateContractResponse - 생성된 계약 ID와 상태
     */
    @Operation(
            summary = "상용직 계약 생성",
            description = "상용직(PERMANENT) 근로계약을 생성합니다. " +
                    "초기 상태는 DRAFT이며, 계약 당시 기업과 근로자의 정보가 스냅샷으로 저장됩니다. " +
                    "근로자에게 이미 다른 타입(일용직)의 FULLY_SIGNED 계약이 있으면 422 에러가 발생합니다."
    )
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @PostMapping("/regular")
    public ResponseEntity<ApiResponse<CreateContractResponse>> createRegularContract(
            @Parameter(description = "현장 ID", required = true, example = "1")
            @PathVariable Long siteId,
            @Valid @RequestBody CreateContractRequest request
    ) {
        log.info("상용직 계약 생성 API 호출: siteId={}, employeeId={}, corporationId={}",
                siteId, request.getEmployeeId(), request.getCorporationId());

        CreateContractResponse response = contractService.createContract(siteId, request, EmpType.PERMANENT);

        log.info("상용직 계약 생성 완료: siteId={}, contractId={}", siteId, response.getContractId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "상용직 계약이 생성되었습니다"));
    }

    /**
     * 일용직 계약 생성 API
     *
     * <p>일용직(DAILY) 근로계약을 생성합니다.</p>
     * <p>비즈니스 로직:</p>
     * <ul>
     *   <li>Employee, Corporation, Manager 존재 여부 검증</li>
     *   <li>근로자의 기존 FULLY_SIGNED 계약 타입 검증</li>
     *   <li>다른 타입의 계약이 있으면 422 에러 반환</li>
     *   <li>Contract + ContractDetail 생성 (초기 상태: DRAFT)</li>
     *   <li>스냅샷 데이터 저장 (계약 당시 기업/근로자 정보)</li>
     * </ul>
     *
     * @param siteId 현장 ID
     * @param request 계약 생성 요청 DTO
     * @return CreateContractResponse - 생성된 계약 ID와 상태
     */
    @Operation(
            summary = "일용직 계약 생성",
            description = "일용직(DAILY) 근로계약을 생성합니다. " +
                    "초기 상태는 DRAFT이며, 계약 당시 기업과 근로자의 정보가 스냅샷으로 저장됩니다. " +
                    "근로자에게 이미 다른 타입(상용직)의 FULLY_SIGNED 계약이 있으면 422 에러가 발생합니다."
    )
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @PostMapping("/daily")
    public ResponseEntity<ApiResponse<CreateContractResponse>> createDailyContract(
            @Parameter(description = "현장 ID", required = true, example = "1")
            @PathVariable Long siteId,
            @Valid @RequestBody CreateContractRequest request
    ) {
        log.info("일용직 계약 생성 API 호출: siteId={}, employeeId={}, corporationId={}",
                siteId, request.getEmployeeId(), request.getCorporationId());

        CreateContractResponse response = contractService.createContract(siteId, request, EmpType.DAILY);

        log.info("일용직 계약 생성 완료: siteId={}, contractId={}", siteId, response.getContractId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "일용직 계약이 생성되었습니다"));
    }
}