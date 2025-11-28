package com.concrete.buildup.domain.contract.controller;

import com.concrete.buildup.domain.contract.dto.ContractListResponse;
import com.concrete.buildup.domain.contract.dto.ContractSearchCondition;
import com.concrete.buildup.domain.contract.dto.CreateContractRequest;
import com.concrete.buildup.domain.contract.dto.CreateContractResponse;
import com.concrete.buildup.domain.contract.dto.SignatureCompleteResponse;
import com.concrete.buildup.domain.contract.dto.SignatureRequest;
import com.concrete.buildup.domain.contract.enums.EmpType;
import com.concrete.buildup.domain.contract.service.ContractService;
import com.concrete.buildup.domain.contract.service.ContractSignatureService;
import com.concrete.buildup.global.common.ApiResponse;
import com.concrete.buildup.global.exception.BusinessException;
import com.concrete.buildup.global.exception.errorcode.ContractErrorCode;
import jakarta.servlet.http.HttpServletRequest;
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
    private final ContractSignatureService contractSignatureService;

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
        log.info("상용직 계약 생성 API 호출: siteId={}, userId={}",
                siteId, request.getUserId());

        // empType 검증: /regular 엔드포인트는 PERMANENT만 허용
        if (request.getEmpType() != EmpType.PERMANENT) {
            log.warn("상용직 엔드포인트에 잘못된 empType 전달: empType={}", request.getEmpType());
            throw new BusinessException(ContractErrorCode.INVALID_EMP_TYPE_FOR_ENDPOINT);
        }

        CreateContractResponse response = contractService.createContract(siteId, request);

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
        log.info("일용직 계약 생성 API 호출: siteId={}, userId={}",
                siteId, request.getUserId());

        // empType 검증: /daily 엔드포인트는 DAILY만 허용
        if (request.getEmpType() != EmpType.DAILY) {
            log.warn("일용직 엔드포인트에 잘못된 empType 전달: empType={}", request.getEmpType());
            throw new BusinessException(ContractErrorCode.INVALID_EMP_TYPE_FOR_ENDPOINT);
        }

        CreateContractResponse response = contractService.createContract(siteId, request);

        log.info("일용직 계약 생성 완료: siteId={}, contractId={}", siteId, response.getContractId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "일용직 계약이 생성되었습니다"));
    }

    /**
     * 계약서 초안 PDF 생성 API
     *
     * <p>계약 생성 후 서명이 없는 초안 PDF(v1)를 생성합니다.</p>
     * <p>비즈니스 로직:</p>
     * <ul>
     *   <li>Contract + ContractDetail 조회</li>
     *   <li>Thymeleaf 템플릿 기반 PDF 생성</li>
     *   <li>S3에 contracts/{contractId}/v1.pdf 업로드</li>
     *   <li>계약 상태를 MANAGER_SIGNING_PENDING으로 변경</li>
     * </ul>
     *
     * @param siteId 현장 ID
     * @param contractId 계약 ID
     * @return PDF URL (v1.pdf)
     */
    @Operation(
            summary = "계약서 초안 PDF 생성",
            description = "계약 생성 후 서명이 없는 초안 PDF를 생성하고 S3에 업로드합니다. " +
                    "생성된 PDF는 관리자 서명 대기 상태가 됩니다."
    )
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @PostMapping("/{contractId}/pdf/initial")
    public ResponseEntity<ApiResponse<String>> generateInitialPdf(
            @Parameter(description = "현장 ID", required = true, example = "1")
            @PathVariable Long siteId,
            @Parameter(description = "계약 ID", required = true, example = "1")
            @PathVariable Long contractId
    ) {
        log.info("초안 PDF 생성 API 호출: siteId={}, contractId={}", siteId, contractId);

        String pdfUrl = contractSignatureService.generateInitialPdf(contractId);

        log.info("초안 PDF 생성 완료: contractId={}, pdfUrl={}", contractId, pdfUrl);

        return ResponseEntity.ok(ApiResponse.success(pdfUrl, "초안 PDF가 생성되었습니다"));
    }

    /**
     * 관리자 서명 처리 API
     *
     * <p>관리자의 서명을 처리하고 v2 PDF를 생성합니다.</p>
     * <p>비즈니스 로직:</p>
     * <ul>
     *   <li>계약 상태 검증 (MANAGER_SIGNING_PENDING인지 확인)</li>
     *   <li>S3에서 서명 이미지 다운로드</li>
     *   <li>서명 이미지 해시 검증 (클라이언트 해시 vs 서버 해시)</li>
     *   <li>v1 PDF에 서명 이미지 스탬핑</li>
     *   <li>S3에 v2 PDF 업로드</li>
     *   <li>ContractSignLog 생성</li>
     *   <li>계약 상태를 EMPLOYEE_SIGNING_PENDING으로 변경</li>
     * </ul>
     *
     * @param siteId 현장 ID
     * @param contractId 계약 ID
     * @param request 서명 요청 정보
     * @param httpRequest HTTP 요청 (IP 추출용)
     * @return SignatureCompleteResponse - 서명 완료 정보 (v2 PDF URL 포함)
     */
    @Operation(
            summary = "관리자 서명 처리",
            description = "관리자의 서명을 처리하고 v2 PDF를 생성합니다. " +
                    "서명 이미지의 무결성을 검증하고, v1 PDF에 서명을 추가합니다. " +
                    "처리 완료 후 근로자 서명 대기 상태로 전환됩니다."
    )
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @PostMapping("/{contractId}/signatures/manager")
    public ResponseEntity<ApiResponse<SignatureCompleteResponse>> processManagerSignature(
            @Parameter(description = "현장 ID", required = true, example = "1")
            @PathVariable Long siteId,
            @Parameter(description = "계약 ID", required = true, example = "1")
            @PathVariable Long contractId,
            @Valid @RequestBody SignatureRequest request,
            HttpServletRequest httpRequest
    ) {
        log.info("관리자 서명 처리 API 호출: siteId={}, contractId={}", siteId, contractId);

        // IP 주소 및 User-Agent 추출 (HTTP 헤더에서만 신뢰)
        String clientIp = extractClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        SignatureCompleteResponse response = contractSignatureService.processManagerSignature(
                contractId,
                request.getSignatureS3Key(),
                request.getClientHash(),
                request.getCoordinates(),
                clientIp,
                userAgent
        );

        log.info("관리자 서명 처리 완료: contractId={}, newState={}", contractId, response.getContractState());

        return ResponseEntity.ok(ApiResponse.success(response, "관리자 서명이 완료되었습니다"));
    }

    /**
     * 근로자 서명 처리 API
     *
     * <p>근로자의 서명을 처리하고 최종 PDF(v3)를 생성합니다.</p>
     * <p>비즈니스 로직:</p>
     * <ul>
     *   <li>계약 상태 검증 (EMPLOYEE_SIGNING_PENDING인지 확인)</li>
     *   <li>S3에서 서명 이미지 다운로드</li>
     *   <li>서명 이미지 해시 검증</li>
     *   <li>v2 PDF에 서명 이미지 스탬핑</li>
     *   <li>v3 PDF의 SHA-256 해시 계산</li>
     *   <li>S3에 v3 PDF 업로드</li>
     *   <li>Contract에 최종 PDF URL 및 해시 저장</li>
     *   <li>ContractSignLog 생성</li>
     *   <li>계약 상태를 FULLY_SIGNED로 변경</li>
     * </ul>
     *
     * @param siteId 현장 ID
     * @param contractId 계약 ID
     * @param request 서명 요청 정보
     * @param httpRequest HTTP 요청 (IP 추출용)
     * @return SignatureCompleteResponse - 서명 완료 정보 (최종 PDF URL 및 해시 포함)
     */
    @Operation(
            summary = "근로자 서명 처리",
            description = "근로자의 서명을 처리하고 최종 PDF(v3)를 생성합니다. " +
                    "서명 이미지의 무결성을 검증하고, v2 PDF에 서명을 추가합니다. " +
                    "최종 PDF의 해시값이 계산되어 저장되며, 계약이 완전 서명 완료 상태가 됩니다."
    )
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
    @PostMapping("/{contractId}/signatures/employee")
    public ResponseEntity<ApiResponse<SignatureCompleteResponse>> processEmployeeSignature(
            @Parameter(description = "현장 ID", required = true, example = "1")
            @PathVariable Long siteId,
            @Parameter(description = "계약 ID", required = true, example = "1")
            @PathVariable Long contractId,
            @Valid @RequestBody SignatureRequest request,
            HttpServletRequest httpRequest
    ) {
        log.info("근로자 서명 처리 API 호출: siteId={}, contractId={}", siteId, contractId);

        // IP 주소 및 User-Agent 추출 (HTTP 헤더에서만 신뢰)
        String clientIp = extractClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        SignatureCompleteResponse response = contractSignatureService.processEmployeeSignature(
                contractId,
                request.getSignatureS3Key(),
                request.getClientHash(),
                request.getCoordinates(),
                clientIp,
                userAgent
        );

        log.info("근로자 서명 처리 완료: contractId={}, newState={}, pdfHash={}",
                contractId, response.getContractState(), response.getPdfHash());

        return ResponseEntity.ok(ApiResponse.success(response, "근로자 서명이 완료되었습니다"));
    }

    /**
     * 클라이언트 IP 주소 추출
     *
     * <p>프록시 환경을 고려하여 X-Forwarded-For 헤더를 우선적으로 확인합니다.</p>
     *
     * @param request HTTP 요청
     * @return 클라이언트 IP 주소
     */
    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For는 "client, proxy1, proxy2" 형식일 수 있으므로 첫 번째 IP 반환
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
