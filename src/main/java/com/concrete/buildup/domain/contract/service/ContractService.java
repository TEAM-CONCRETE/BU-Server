package com.concrete.buildup.domain.contract.service;

import com.concrete.buildup.domain.auth.entity.Corporation;
import com.concrete.buildup.domain.auth.entity.Employee;
import com.concrete.buildup.domain.auth.entity.Manager;
import com.concrete.buildup.domain.auth.entity.User;
import com.concrete.buildup.domain.auth.repository.CorporationRepository;
import com.concrete.buildup.domain.auth.repository.EmployeeRepository;
import com.concrete.buildup.domain.auth.repository.ManagerRepository;
import com.concrete.buildup.domain.auth.repository.UserRepository;
import com.concrete.buildup.domain.contract.dto.ContractDetailRequest;
import com.concrete.buildup.domain.contract.dto.ContractListResponse;
import com.concrete.buildup.domain.contract.dto.ContractSearchCondition;
import com.concrete.buildup.domain.contract.dto.ContractSummaryDto;
import com.concrete.buildup.domain.contract.dto.CreateContractRequest;
import com.concrete.buildup.domain.contract.dto.CreateContractResponse;
import com.concrete.buildup.domain.contract.entity.Contract;
import com.concrete.buildup.domain.contract.entity.ContractDetail;
import com.concrete.buildup.domain.contract.enums.ContractState;
import com.concrete.buildup.domain.contract.enums.EmpType;
import com.concrete.buildup.domain.contract.repository.ContractDetailRepository;
import com.concrete.buildup.domain.contract.repository.ContractRepository;
import com.concrete.buildup.domain.site.entity.Site;
import com.concrete.buildup.domain.site.repository.SiteRepository;
import com.concrete.buildup.global.exception.BusinessException;
import com.concrete.buildup.global.exception.errorcode.ContractErrorCode;
import com.concrete.buildup.global.util.MaskingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 계약 관리 서비스
 *
 * <p>근로계약 생성, 조회, 서명 등 계약 관련 비즈니스 로직을 처리합니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContractService {

    private final ContractRepository contractRepository;
    private final ContractDetailRepository contractDetailRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final CorporationRepository corporationRepository;
    private final ManagerRepository managerRepository;
    private final SiteRepository siteRepository;
    private final ContractSignatureService contractSignatureService;

    /**
     * 계약 생성 및 초안 PDF 자동 생성
     *
     * <p>상용직 또는 일용직 근로계약을 생성하고, 초안 PDF를 자동으로 생성합니다.</p>
     * <p>비즈니스 로직:</p>
     * <ul>
     *   <li>1. Site 존재 여부 검증</li>
     *   <li>2. Employee, Corporation, Manager 존재 여부 검증</li>
     *   <li>3. Manager가 해당 Site의 관리자인지 권한 검증 (managerId가 있는 경우)</li>
     *   <li>4. 급여 지급일 검증 (1~31 범위)</li>
     *   <li>5. 근로자의 기존 FULLY_SIGNED 계약 조회</li>
     *   <li>6. 다른 타입의 FULLY_SIGNED 계약이 있으면 예외 발생 (422)</li>
     *   <li>7. 같은 타입이거나 없으면 emp_type 설정</li>
     *   <li>8. Contract + ContractDetail 생성 및 저장 (트랜잭션)</li>
     *   <li>9. 초안 PDF(v1) 생성 및 S3 업로드</li>
     *   <li>10. 계약 상태를 MANAGER_SIGNING_PENDING으로 변경</li>
     * </ul>
     *
     * @param siteId 현장 ID
     * @param request 계약 생성 요청 DTO
     * @return CreateContractResponse - 생성된 계약 ID, 상태, PDF URL
     * @throws BusinessException SITE_NOT_FOUND - 현장을 찾을 수 없음
     * @throws BusinessException EMPLOYEE_NOT_FOUND - 근로자를 찾을 수 없음
     * @throws BusinessException CORPORATION_NOT_FOUND - 기업을 찾을 수 없음
     * @throws BusinessException MANAGER_NOT_FOUND - 관리자를 찾을 수 없음 (managerId가 있는 경우)
     * @throws BusinessException MANAGER_NOT_AUTHORIZED - 관리자가 해당 현장의 관리자가 아님
     * @throws BusinessException CONFLICTING_EMP_TYPE - 다른 타입의 계약이 이미 존재함
     * @throws BusinessException INVALID_PAY_DAY - 급여 지급일이 유효하지 않음 (1~31 범위 외)
     */
    @Transactional
    public CreateContractResponse createContract(Long siteId, CreateContractRequest request) {
        log.info("계약 생성 시작: siteId={}, userId={}, empType={}",
                siteId, request.getUserId(), request.getEmpType());

        // ========== 1. Site 조회 및 Corporation, Manager 정보 획득 ==========

        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> {
                    log.warn("현장을 찾을 수 없음: siteId={}", siteId);
                    return new BusinessException(ContractErrorCode.SITE_NOT_FOUND);
                });
        log.debug("현장 조회 성공: siteId={}", site.getId());

        // 1-1. Site에서 Corporation 정보 획득
        Corporation corporation = site.getCorporation();
        if (corporation == null) {
            log.warn("현장에 기업 정보가 없음: siteId={}", siteId);
            throw new BusinessException(ContractErrorCode.CORPORATION_NOT_FOUND);
        }
        log.debug("기업 조회 성공: corporationId={}", corporation.getId());

        // 1-2. Site에서 Manager 정보 획득
        Manager manager = site.getManager();
        if (manager == null) {
            log.warn("현장에 관리자 정보가 없음: siteId={}", siteId);
            throw new BusinessException(ContractErrorCode.MANAGER_NOT_FOUND);
        }
        log.debug("관리자 조회 성공: managerId={}", manager.getId());

        // ========== 2. userId로 Employee 조회 ==========

        // 2-1. User 조회 (userId = 로그인 ID)
        User employeeUser = userRepository.findByUserId(request.getUserId())
                .orElseThrow(() -> {
                    log.warn("근로자 User를 찾을 수 없음: userId={}", request.getUserId());
                    return new BusinessException(ContractErrorCode.EMPLOYEE_NOT_FOUND);
                });
        log.debug("근로자 User 조회 성공: userId={}", employeeUser.getId());

        // 2-2. Employee 조회 (User -> Employee 1:1 관계)
        Employee employee = employeeRepository.findByUserId(employeeUser.getId())
                .orElseThrow(() -> {
                    log.warn("근로자 정보를 찾을 수 없음: userId={}", employeeUser.getId());
                    return new BusinessException(ContractErrorCode.EMPLOYEE_NOT_FOUND);
                });
        log.debug("근로자 조회 성공: employeeId={}", employee.getId());

        // ========== 2. 근로자 emp_type 검증 및 설정 ==========

        // 2-1. 기존 FULLY_SIGNED 계약 조회
        List<Contract> fullySignedContracts = contractRepository.findByEmployeeIdAndContractState(
                employee.getId(),
                ContractState.FULLY_SIGNED
        );
        log.debug("근로자의 FULLY_SIGNED 계약 개수: {}", fullySignedContracts.size());

        // 2-2. 급여 지급일 검증 (1~31 범위)
        ContractDetailRequest details = request.getDetails();
        if (details.getPayDay() != null && (details.getPayDay() < 1 || details.getPayDay() > 31)) {
            log.warn("급여 지급일이 유효하지 않음: payDay={}", details.getPayDay());
            throw new BusinessException(ContractErrorCode.INVALID_PAY_DAY);
        }

        // 2-3. 기존 계약이 있는 경우 타입 검증
        if (!fullySignedContracts.isEmpty()) {
            // 근로자의 현재 emp_type 확인
            String currentEmpType = employee.getEmpType();
            String requestedEmpType = request.getEmpType().name();

            log.debug("근로자 emp_type 확인: currentEmpType={}, requestedEmpType={}", currentEmpType, requestedEmpType);

            // 다른 타입의 계약이 이미 존재하면 예외 발생
            if (currentEmpType != null && !currentEmpType.equals(requestedEmpType)) {
                log.warn("다른 타입의 계약이 이미 존재: employeeId={}, currentEmpType={}, requestedEmpType={}",
                        employee.getId(), currentEmpType, requestedEmpType);
                throw new BusinessException(ContractErrorCode.CONFLICTING_EMP_TYPE);
            }
        }

        // 2-4. emp_type 설정 (기존 타입이 없거나 같은 경우)
        if (employee.getEmpType() == null || !employee.getEmpType().equals(request.getEmpType().name())) {
            employee.changeEmpType(request.getEmpType().name());
            log.info("근로자 emp_type 설정: employeeId={}, empType={}", employee.getId(), request.getEmpType().name());
        }

        // ========== 3. Contract 엔티티 생성 ==========

        Contract contract = Contract.builder()
                .employeeId(employee.getId())
                .corporationId(corporation.getId())
                .managerId(manager != null ? manager.getId() : null)
                .role(request.getRole())
                .empType(request.getEmpType())
                .contractState(ContractState.DRAFT) // 초기 상태: DRAFT
                .employeeStartDate(request.getEmployeeStartDate())
                .employeeEndDate(request.getEmployeeEndDate())
                .writtenAt(LocalDateTime.now())
                .build();

        // Contract 저장
        Contract savedContract = contractRepository.save(contract);
        log.info("Contract 생성 완료: contractId={}", savedContract.getId());

        // ========== 4. ContractDetail 엔티티 생성 (스냅샷) ==========

        ContractDetail contractDetail = ContractDetail.builder()
                .contract(savedContract)
                // 스냅샷 필드 (계약 당시 정보)
                .corpName(corporation.getCorpName())
                .empName(employee.getEmpName())
                .corpAddress(corporation.getCorpAddress())
                .corpCeoName(corporation.getCorpCeoName())
                .empAddress(employee.getEmpAddress())
                // 근무 정보
                .workPlace(details.getWorkPlace())
                .workType(details.getWorkType())
                .workStartTime(details.getWorkStartTime())
                .workEndTime(details.getWorkEndTime())
                .breakStartTime(details.getBreakStartTime())
                .breakEndTime(details.getBreakEndTime())
                .workOnDays(details.getWorkOnDays())
                .workOffDays(details.getWorkOffDays())
                // 급여 정보
                .workPay(details.getWorkPay())
                .additionalHourPay(details.getAdditionalHourPay())
                .additionalNightPay(details.getAdditionalNightPay())
                .additionalHolidayPay(details.getAdditionalHolidayPay())
                // 지급 정보
                .payDay(details.getPayDay())
                .payPeriod(details.getPayPeriod())
                .payType(details.getPayType())
                // 4대보험
                .isEoiApplicable(details.getIsEoiApplicable())
                .isWciApplicable(details.getIsWciApplicable())
                .isNpsApplicable(details.getIsNpsApplicable())
                .isNhiApplicable(details.getIsNhiApplicable())
                .build();

        // ContractDetail 저장
        contractDetailRepository.save(contractDetail);
        log.info("ContractDetail 생성 완료: contractId={}", savedContract.getId());

        // ========== 5. 초안 PDF 생성 및 S3 업로드 ==========

        String pdfUrl = contractSignatureService.generateInitialPdf(savedContract.getId());
        log.info("초안 PDF 생성 완료: contractId={}, pdfUrl={}", savedContract.getId(), pdfUrl);

        // ========== 6. 응답 생성 ==========

        log.info("계약 생성 완료 - contractId: {}, employeeId: {}, state: {}",
                savedContract.getId(), employee.getId(), savedContract.getContractState());

        return CreateContractResponse.builder()
                .contractId(savedContract.getId())
                .contractState(savedContract.getContractState()) // MANAGER_SIGNING_PENDING
                .pdfUrl(pdfUrl)
                .build();
    }

    /**
     * 계약 목록 조회
     *
     * <p>현장별 계약 목록을 필터링 및 페이징하여 조회합니다.</p>
     * <p>비즈니스 로직:</p>
     * <ul>
     *   <li>1. Site 존재 여부 검증 및 managerId 획득</li>
     *   <li>2. managerId로 계약 목록 조회 (페이징)</li>
     *   <li>3. 계약 목록에서 employeeId 추출 → Employee 일괄 조회 (N+1 방지)</li>
     *   <li>4. 검색 조건(employeeId, empType, status, from, to)으로 Stream 필터링</li>
     *   <li>5. Employee + Contract 정보를 ContractSummaryDto로 변환 (주민번호 마스킹)</li>
     *   <li>6. 수동 페이징 처리 후 ContractListResponse 생성</li>
     * </ul>
     *
     * @param siteId 현장 ID
     * @param condition 검색 조건
     * @return ContractListResponse - 계약 목록 + 페이징 정보
     * @throws BusinessException SITE_NOT_FOUND - 현장을 찾을 수 없음
     */
    public ContractListResponse getContracts(Long siteId, ContractSearchCondition condition) {
        log.info("계약 목록 조회 시작: siteId={}, condition={}", siteId, condition);

        // ========== 1. Site 존재 여부 검증 및 managerId 획득 ==========
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> {
                    log.warn("현장을 찾을 수 없음: siteId={}", siteId);
                    return new BusinessException(ContractErrorCode.SITE_NOT_FOUND);
                });
        log.debug("현장 조회 성공: siteId={}", site.getId());

        Long managerId = site.getManager() != null ? site.getManager().getId() : null;
        if (managerId == null) {
            log.warn("현장에 관리자가 할당되지 않음: siteId={}", siteId);
            // 관리자가 없는 현장은 빈 목록 반환
            return ContractListResponse.builder()
                    .items(List.of())
                    .pageInfo(ContractListResponse.PageInfo.builder()
                            .currentPage(condition.getPage())
                            .pageSize(condition.getSize())
                            .totalElements(0)
                            .totalPages(0)
                            .hasNext(false)
                            .hasPrevious(false)
                            .build())
                    .build();
        }

        // ========== 2. empType 필터링 - Employee 테이블에서 해당 타입의 employeeId 목록 조회 ==========
        List<Long> employeeIdsByType = null;
        if (condition.getEmpType() != null) {
            List<Employee> employeesByType;
            
            // UNCONTRACTED인 경우 emp_type이 NULL인 근로자 조회
            if (condition.getEmpType() == EmpType.UNCONTRACTED) {
                employeesByType = employeeRepository.findByEmpTypeIsNull();
                log.debug("미계약 근로자 필터링: count={}", employeesByType.size());
            } else {
                employeesByType = employeeRepository.findByEmpType(condition.getEmpType().name());
                log.debug("empType 필터링 완료: empType={}, count={}", condition.getEmpType(), employeesByType.size());
            }
            
            employeeIdsByType = employeesByType.stream()
                    .map(Employee::getId)
                    .toList();

            // empType에 해당하는 근로자가 없으면 빈 목록 반환
            if (employeeIdsByType.isEmpty()) {
                log.info("해당 empType의 근로자가 없음: empType={}", condition.getEmpType());
                return buildEmptyResponse(condition);
            }
        }

        // ========== 3. 동적 조건으로 Contract 조회 (페이징) ==========
        Pageable pageable = PageRequest.of(
                condition.getPageIndex(),
                condition.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Contract> contractPage = contractRepository.findByDynamicConditions(
                managerId,
                condition.getEmployeeId(),
                employeeIdsByType,
                condition.getStatus(),
                condition.getFrom(),
                condition.getTo(),
                pageable
        );

        List<Contract> contracts = contractPage.getContent();
        log.info("DB 조회 완료: totalElements={}, totalPages={}, currentPage={}",
                contractPage.getTotalElements(), contractPage.getTotalPages(), condition.getPage());

        if (contracts.isEmpty()) {
            return buildEmptyResponse(condition);
        }

        // ========== 4. Employee 일괄 조회 (N+1 방지) ==========
        List<Long> employeeIds = contracts.stream()
                .map(Contract::getEmployeeId)
                .distinct()
                .toList();

        Map<Long, Employee> employeeMap = employeeRepository.findAllById(employeeIds).stream()
                .collect(Collectors.toMap(Employee::getId, e -> e));
        log.debug("근로자 정보 일괄 조회 완료: count={}", employeeMap.size());

        // ========== 5. DTO 변환 ==========
        List<ContractSummaryDto> items = contracts.stream()
                .map(contract -> toSummaryDto(contract, employeeMap.get(contract.getEmployeeId())))
                .toList();

        // ========== 6. PageInfo 생성 (필터링된 결과 기준) ==========
        ContractListResponse.PageInfo pageInfo = ContractListResponse.PageInfo.builder()
                .currentPage(condition.getPage())
                .pageSize(condition.getSize())
                .totalElements(contractPage.getTotalElements())
                .totalPages(contractPage.getTotalPages())
                .hasNext(contractPage.hasNext())
                .hasPrevious(contractPage.hasPrevious())
                .build();

        log.info("계약 목록 조회 완료: itemCount={}", items.size());

        return ContractListResponse.builder()
                .items(items)
                .pageInfo(pageInfo)
                .build();
    }

    /**
     * 빈 응답 생성 헬퍼 메서드
     */
    private ContractListResponse buildEmptyResponse(ContractSearchCondition condition) {
        return ContractListResponse.builder()
                .items(List.of())
                .pageInfo(ContractListResponse.PageInfo.builder()
                        .currentPage(condition.getPage())
                        .pageSize(condition.getSize())
                        .totalElements(0)
                        .totalPages(0)
                        .hasNext(false)
                        .hasPrevious(false)
                        .build())
                .build();
    }


    /**
     * Contract + Employee를 ContractSummaryDto로 변환
     */
    private ContractSummaryDto toSummaryDto(Contract contract, Employee employee) {
        if (employee == null) {
            log.warn("근로자 정보를 찾을 수 없음: employeeId={}", contract.getEmployeeId());
            // Employee가 없는 경우에도 Contract 정보는 반환 (데이터 일관성 문제 방지)
            return ContractSummaryDto.builder()
                    .contractId(contract.getId())
                    .employeeId(contract.getEmployeeId())
                    .employeeName("알 수 없음")
                    .employeeResidentNumber(null)
                    .empType(null)
                    .role(contract.getRole())
                    .contractState(contract.getContractState())
                    .employeeStartDate(contract.getEmployeeStartDate())
                    .employeeEndDate(contract.getEmployeeEndDate())
                    .writtenAt(contract.getWrittenAt())
                    .corporationSignedAt(contract.getCorpSignedAt())
                    .employeeSignedAt(contract.getEmpSignedAt())
                    .build();
        }

        // empType 변환
        EmpType empType = null;
        if (employee.getEmpType() != null) {
            try {
                empType = EmpType.valueOf(employee.getEmpType());
            } catch (IllegalArgumentException e) {
                log.warn("유효하지 않은 empType: {}", employee.getEmpType());
            }
        }

        return ContractSummaryDto.builder()
                .contractId(contract.getId())
                .employeeId(employee.getId())
                .employeeName(employee.getEmpName())
                .employeeResidentNumber(MaskingUtil.maskResidentNumber(employee.getResidentNum()))
                .empType(empType)
                .role(contract.getRole())
                .contractState(contract.getContractState())
                .employeeStartDate(contract.getEmployeeStartDate())
                .employeeEndDate(contract.getEmployeeEndDate())
                .writtenAt(contract.getWrittenAt())
                .corporationSignedAt(contract.getCorpSignedAt())
                .employeeSignedAt(contract.getEmpSignedAt())
                .build();
    }
}