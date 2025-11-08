package com.concrete.buildup.domain.contract.service;

import com.concrete.buildup.domain.auth.entity.Corporation;
import com.concrete.buildup.domain.auth.entity.Employee;
import com.concrete.buildup.domain.auth.entity.Manager;
import com.concrete.buildup.domain.auth.repository.CorporationRepository;
import com.concrete.buildup.domain.auth.repository.EmployeeRepository;
import com.concrete.buildup.domain.auth.repository.ManagerRepository;
import com.concrete.buildup.domain.contract.dto.CreateContractRequest;
import com.concrete.buildup.domain.contract.dto.CreateContractResponse;
import com.concrete.buildup.domain.contract.entity.Contract;
import com.concrete.buildup.domain.contract.enums.ContractState;
import com.concrete.buildup.domain.contract.enums.EmpType;
import com.concrete.buildup.domain.contract.repository.ContractDetailRepository;
import com.concrete.buildup.domain.contract.repository.ContractRepository;
import com.concrete.buildup.global.exception.BusinessException;
import com.concrete.buildup.global.exception.errorcode.ContractErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    /**
     * 계약 생성
     *
     * <p>상용직 또는 일용직 근로계약을 생성합니다.</p>
     * <p>비즈니스 로직:</p>
     * <ul>
     *   <li>1. Employee, Corporation, Manager 존재 여부 검증</li>
     *   <li>2. 근로자의 기존 FULLY_SIGNED 계약 조회</li>
     *   <li>3. 다른 타입의 FULLY_SIGNED 계약이 있으면 예외 발생 (422)</li>
     *   <li>4. 같은 타입이거나 없으면 emp_type 설정</li>
     *   <li>5. Contract + ContractDetail 생성 및 저장 (트랜잭션)</li>
     * </ul>
     *
     * @param request 계약 생성 요청 DTO
     * @param empType 근로자 유형 (DAILY/PERMANENT)
     * @return CreateContractResponse - 생성된 계약 ID와 상태
     * @throws BusinessException EMPLOYEE_NOT_FOUND - 근로자를 찾을 수 없음
     * @throws BusinessException CORPORATION_NOT_FOUND - 기업을 찾을 수 없음
     * @throws BusinessException MANAGER_NOT_FOUND - 관리자를 찾을 수 없음 (managerId가 있는 경우)
     * @throws BusinessException CONFLICTING_EMP_TYPE - 다른 타입의 계약이 이미 존재함
     */
    @Transactional
    public CreateContractResponse createContract(CreateContractRequest request, EmpType empType) {
        log.info("계약 생성 시작: employeeId={}, corporationId={}, empType={}",
                request.getEmployeeId(), request.getCorporationId(), empType);

        // ========== 1. 존재 여부 검증 ==========

        // 1-1. Employee 존재 여부 검증
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> {
                    log.warn("근로자를 찾을 수 없음: employeeId={}", request.getEmployeeId());
                    return new BusinessException(ContractErrorCode.EMPLOYEE_NOT_FOUND);
                });
        log.debug("근로자 조회 성공: employeeId={}, empName={}", employee.getId(), employee.getEmpName());

        // 1-2. Corporation 존재 여부 검증
        Corporation corporation = corporationRepository.findById(request.getCorporationId())
                .orElseThrow(() -> {
                    log.warn("기업을 찾을 수 없음: corporationId={}", request.getCorporationId());
                    return new BusinessException(ContractErrorCode.CORPORATION_NOT_FOUND);
                });
        log.debug("기업 조회 성공: corporationId={}, corpName={}", corporation.getId(), corporation.getCorpName());

        // 1-3. Manager 존재 여부 검증 (Optional)
        Manager manager = null;
        if (request.getManagerId() != null) {
            manager = managerRepository.findById(request.getManagerId())
                    .orElseThrow(() -> {
                        log.warn("관리자를 찾을 수 없음: managerId={}", request.getManagerId());
                        return new BusinessException(ContractErrorCode.MANAGER_NOT_FOUND);
                    });
            log.debug("관리자 조회 성공: managerId={}, managerName={}", manager.getId(), manager.getManagerName());
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

        // TODO: 4번 커밋에서 Contract 및 ContractDetail 생성 로직 추가 예정

        return null; // 임시 반환
    }
}