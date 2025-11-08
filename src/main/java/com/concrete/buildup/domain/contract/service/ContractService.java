package com.concrete.buildup.domain.contract.service;

import com.concrete.buildup.domain.auth.entity.Corporation;
import com.concrete.buildup.domain.auth.entity.Employee;
import com.concrete.buildup.domain.auth.entity.Manager;
import com.concrete.buildup.domain.auth.repository.CorporationRepository;
import com.concrete.buildup.domain.auth.repository.EmployeeRepository;
import com.concrete.buildup.domain.auth.repository.ManagerRepository;
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
    private final EmployeeRepository employeeRepository;
    private final CorporationRepository corporationRepository;
    private final ManagerRepository managerRepository;
    private final SiteRepository siteRepository;

    /**
     * 계약 생성
     *
     * <p>상용직 또는 일용직 근로계약을 생성합니다.</p>
     * <p>비즈니스 로직:</p>
     * <ul>
     *   <li>1. Site 존재 여부 검증</li>
     *   <li>2. Employee, Corporation, Manager 존재 여부 검증</li>
     *   <li>3. Manager가 해당 Site의 관리자인지 권한 검증 (managerId가 있는 경우)</li>
     *   <li>4. 근로자의 기존 FULLY_SIGNED 계약 조회</li>
     *   <li>5. 다른 타입의 FULLY_SIGNED 계약이 있으면 예외 발생 (422)</li>
     *   <li>6. 같은 타입이거나 없으면 emp_type 설정</li>
     *   <li>7. Contract + ContractDetail 생성 및 저장 (트랜잭션)</li>
     * </ul>
     *
     * @param siteId 현장 ID
     * @param request 계약 생성 요청 DTO
     * @param empType 근로자 유형 (DAILY/PERMANENT)
     * @return CreateContractResponse - 생성된 계약 ID와 상태
     * @throws BusinessException SITE_NOT_FOUND - 현장을 찾을 수 없음
     * @throws BusinessException EMPLOYEE_NOT_FOUND - 근로자를 찾을 수 없음
     * @throws BusinessException CORPORATION_NOT_FOUND - 기업을 찾을 수 없음
     * @throws BusinessException MANAGER_NOT_FOUND - 관리자를 찾을 수 없음 (managerId가 있는 경우)
     * @throws BusinessException MANAGER_NOT_AUTHORIZED - 관리자가 해당 현장의 관리자가 아님
     * @throws BusinessException CONFLICTING_EMP_TYPE - 다른 타입의 계약이 이미 존재함
     */
    @Transactional
    public CreateContractResponse createContract(Long siteId, CreateContractRequest request, EmpType empType) {
        log.info("계약 생성 시작: siteId={}, employeeId={}, corporationId={}, empType={}",
                siteId, request.getEmployeeId(), request.getCorporationId(), empType);

        // ========== 1. 존재 여부 검증 ==========

        // 1-0. Site 존재 여부 검증
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> {
                    log.warn("현장을 찾을 수 없음: siteId={}", siteId);
                    return new BusinessException(ContractErrorCode.SITE_NOT_FOUND);
                });
        log.debug("현장 조회 성공: siteId={}", site.getId());

        // 1-1. Employee 존재 여부 검증
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> {
                    log.warn("근로자를 찾을 수 없음: employeeId={}", request.getEmployeeId());
                    return new BusinessException(ContractErrorCode.EMPLOYEE_NOT_FOUND);
                });
        log.debug("근로자 조회 성공: employeeId={}", employee.getId());

        // 1-2. Corporation 존재 여부 검증
        Corporation corporation = corporationRepository.findById(request.getCorporationId())
                .orElseThrow(() -> {
                    log.warn("기업을 찾을 수 없음: corporationId={}", request.getCorporationId());
                    return new BusinessException(ContractErrorCode.CORPORATION_NOT_FOUND);
                });
        log.debug("기업 조회 성공: corporationId={}", corporation.getId());

        // 1-3. Manager 존재 여부 검증 및 권한 검증 (Optional)
        Manager manager = null;
        if (request.getManagerId() != null) {
            manager = managerRepository.findById(request.getManagerId())
                    .orElseThrow(() -> {
                        log.warn("관리자를 찾을 수 없음: managerId={}", request.getManagerId());
                        return new BusinessException(ContractErrorCode.MANAGER_NOT_FOUND);
                    });
            log.debug("관리자 조회 성공: managerId={}", manager.getId());

            // 1-4. Manager가 해당 Site의 관리자인지 권한 검증
            if (site.getManager() == null || !site.getManager().getId().equals(manager.getId())) {
                log.warn("관리자가 해당 현장의 관리자가 아님: managerId={}, siteId={}, siteManagerId={}",
                        manager.getId(), siteId, site.getManager() != null ? site.getManager().getId() : null);
                throw new BusinessException(ContractErrorCode.MANAGER_NOT_AUTHORIZED);
            }
            log.debug("관리자 권한 검증 성공: managerId={}, siteId={}", manager.getId(), siteId);
        }

        // ========== 2. 근로자 emp_type 검증 및 설정 ==========

        // 2-1. 기존 FULLY_SIGNED 계약 조회
        List<Contract> fullySignedContracts = contractRepository.findByEmployeeIdAndContractState(
                employee.getId(),
                ContractState.FULLY_SIGNED
        );
        log.debug("근로자의 FULLY_SIGNED 계약 개수: {}", fullySignedContracts.size());

        // 2-2. 기존 계약이 있는 경우 타입 검증
        if (!fullySignedContracts.isEmpty()) {
            // 근로자의 현재 emp_type 확인
            String currentEmpType = employee.getEmpType();
            String requestedEmpType = empType.name();

            log.debug("근로자 emp_type 확인: currentEmpType={}, requestedEmpType={}", currentEmpType, requestedEmpType);

            // 다른 타입의 계약이 이미 존재하면 예외 발생
            if (currentEmpType != null && !currentEmpType.equals(requestedEmpType)) {
                log.warn("다른 타입의 계약이 이미 존재: employeeId={}, currentEmpType={}, requestedEmpType={}",
                        employee.getId(), currentEmpType, requestedEmpType);
                throw new BusinessException(ContractErrorCode.CONFLICTING_EMP_TYPE);
            }
        }

        // 2-3. emp_type 설정 (기존 타입이 없거나 같은 경우)
        if (employee.getEmpType() == null || !employee.getEmpType().equals(empType.name())) {
            employee.changeEmpType(empType.name());
            log.info("근로자 emp_type 설정: employeeId={}, empType={}", employee.getId(), empType.name());
        }

        // ========== 3. Contract 엔티티 생성 ==========

        Contract contract = Contract.builder()
                .employeeId(employee.getId())
                .corporationId(corporation.getId())
                .managerId(manager != null ? manager.getId() : null)
                .role(request.getRole())
                .contractState(ContractState.DRAFT) // 초기 상태: DRAFT
                .employeeStartDate(request.getEmployeeStartDate())
                .employeeEndDate(request.getEmployeeEndDate())
                .writtenAt(LocalDateTime.now())
                .build();

        // Contract 저장
        Contract savedContract = contractRepository.save(contract);
        log.info("Contract 생성 완료: contractId={}", savedContract.getId());

        // ========== 4. ContractDetail 엔티티 생성 (스냅샷) ==========

        ContractDetailRequest details = request.getDetails();

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
                .workOnDay(details.getWorkOnDay())
                .workOffDay(details.getWorkOffDay())
                // 급여 정보
                .workPay(details.getWorkPay())
                .workBonus(details.getWorkBonus())
                .additionalHourPay(details.getAdditionalHourPay())
                .additionalNightPay(details.getAdditionalNightPay())
                .additionalHolidayPay(details.getAdditionalHolidayPay())
                // 지급 정보
                .payday(details.getPayday())
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

        // ========== 5. 응답 생성 ==========

        log.info("계약 생성 완료 - contractId: {}, employeeId: {}", savedContract.getId(), employee.getId());

        return CreateContractResponse.builder()
                .contractId(savedContract.getId())
                .contractState(savedContract.getContractState())
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

        // ========== 2. managerId로 계약 목록 조회 (페이징) ==========
        Pageable pageable = PageRequest.of(
                condition.getPageIndex(),
                condition.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        Page<Contract> contractPage = contractRepository.findByManagerId(managerId, pageable);
        List<Contract> contracts = contractPage.getContent();
        log.debug("계약 목록 조회 완료: count={}", contracts.size());

        if (contracts.isEmpty()) {
            return buildEmptyResponse(condition);
        }

        // ========== 3. Employee 일괄 조회 (N+1 방지) ==========
        List<Long> employeeIds = contracts.stream()
                .map(Contract::getEmployeeId)
                .distinct()
                .toList();

        Map<Long, Employee> employeeMap = employeeRepository.findAllById(employeeIds).stream()
                .collect(Collectors.toMap(Employee::getId, e -> e));
        log.debug("근로자 정보 일괄 조회 완료: count={}", employeeMap.size());

        // ========== 4. Stream 필터링 ==========
        List<ContractSummaryDto> filteredItems = contracts.stream()
                .filter(contract -> matchesCondition(contract, employeeMap.get(contract.getEmployeeId()), condition))
                .map(contract -> toSummaryDto(contract, employeeMap.get(contract.getEmployeeId())))
                .toList();

        log.debug("필터링 후 계약 개수: {}", filteredItems.size());

        // ========== 5. PageInfo 생성 ==========
        ContractListResponse.PageInfo pageInfo = ContractListResponse.PageInfo.builder()
                .currentPage(condition.getPage())
                .pageSize(condition.getSize())
                .totalElements(contractPage.getTotalElements())
                .totalPages(contractPage.getTotalPages())
                .hasNext(contractPage.hasNext())
                .hasPrevious(contractPage.hasPrevious())
                .build();

        log.info("계약 목록 조회 완료: totalElements={}", contractPage.getTotalElements());

        return ContractListResponse.builder()
                .items(filteredItems)
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
     * 검색 조건 매칭 확인
     */
    private boolean matchesCondition(Contract contract, Employee employee, ContractSearchCondition condition) {
        // employeeId 필터
        if (condition.getEmployeeId() != null && !condition.getEmployeeId().equals(contract.getEmployeeId())) {
            return false;
        }

        // empType 필터 (Employee 테이블에서 조회)
        if (condition.getEmpType() != null && employee != null) {
            String empTypeStr = employee.getEmpType();
            if (empTypeStr == null || !empTypeStr.equals(condition.getEmpType().name())) {
                return false;
            }
        }

        // status 필터
        if (condition.getStatus() != null && !condition.getStatus().equals(contract.getContractState())) {
            return false;
        }

        // from 날짜 필터 (계약 시작일 >= from)
        if (condition.getFrom() != null && contract.getEmployeeStartDate().isBefore(condition.getFrom())) {
            return false;
        }

        // to 날짜 필터 (계약 시작일 <= to)
        if (condition.getTo() != null && contract.getEmployeeStartDate().isAfter(condition.getTo())) {
            return false;
        }

        return true;
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