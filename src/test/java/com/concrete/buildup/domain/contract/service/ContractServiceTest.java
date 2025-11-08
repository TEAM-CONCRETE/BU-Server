package com.concrete.buildup.domain.contract.service;

import com.concrete.buildup.domain.auth.entity.Corporation;
import com.concrete.buildup.domain.auth.entity.Employee;
import com.concrete.buildup.domain.auth.entity.Manager;
import com.concrete.buildup.domain.auth.entity.User;
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
import com.concrete.buildup.domain.contract.enums.PayPeriod;
import com.concrete.buildup.domain.contract.enums.PayType;
import com.concrete.buildup.domain.contract.repository.ContractDetailRepository;
import com.concrete.buildup.domain.contract.repository.ContractRepository;
import com.concrete.buildup.domain.site.entity.Site;
import com.concrete.buildup.domain.site.repository.SiteRepository;
import com.concrete.buildup.global.exception.BusinessException;
import com.concrete.buildup.global.exception.errorcode.ContractErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

/**
 * ContractService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ContractService 테스트")
class ContractServiceTest {

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private ContractDetailRepository contractDetailRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private CorporationRepository corporationRepository;

    @Mock
    private ManagerRepository managerRepository;

    @Mock
    private SiteRepository siteRepository;

    @InjectMocks
    private ContractService contractService;

    @Test
    @DisplayName("계약 생성 성공 - 상용직, 첫 계약")
    void createContract_Success_PermanentFirstContract() {
        // given
        Long siteId = 1L;
        Long employeeId = 1L;
        Long corporationId = 1L;
        Long managerId = 1L;

        Manager manager = createManager(managerId, "김관리");
        Site site = createSite(siteId, "테스트현장", manager);
        Employee employee = createEmployee(employeeId, "홍길동", null);
        Corporation corporation = createCorporation(corporationId, "테스트회사", "서울시 강남구");

        CreateContractRequest request = createContractRequest(employeeId, corporationId, managerId);

        Contract savedContract = Contract.builder()
                .employeeId(employeeId)
                .corporationId(corporationId)
                .managerId(managerId)
                .contractState(ContractState.DRAFT)
                .build();
        // ID 설정 (저장 후 자동 생성되는 ID 시뮬레이션)
        try {
            java.lang.reflect.Field idField = Contract.class.getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(savedContract, 100L);
        } catch (Exception e) {
            // ID 설정 실패 시 무시
        }

        given(siteRepository.findById(siteId)).willReturn(Optional.of(site));
        given(employeeRepository.findById(employeeId)).willReturn(Optional.of(employee));
        given(corporationRepository.findById(corporationId)).willReturn(Optional.of(corporation));
        given(managerRepository.findById(managerId)).willReturn(Optional.of(manager));
        given(contractRepository.findByEmployeeIdAndContractState(employeeId, ContractState.FULLY_SIGNED))
                .willReturn(Collections.emptyList());
        given(contractRepository.save(any(Contract.class))).willReturn(savedContract);
        given(contractDetailRepository.save(any(ContractDetail.class))).willReturn(any());

        // when
        CreateContractResponse response = contractService.createContract(siteId, request, EmpType.PERMANENT);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContractId()).isEqualTo(100L);
        assertThat(response.getContractState()).isEqualTo(ContractState.DRAFT);

        // Employee의 empType이 설정되었는지 검증
        verify(siteRepository).findById(siteId);
        verify(employeeRepository).findById(employeeId);
        verify(corporationRepository).findById(corporationId);
        verify(managerRepository).findById(managerId);
        verify(contractRepository).save(any(Contract.class));
        verify(contractDetailRepository).save(any(ContractDetail.class));
    }

    @Test
    @DisplayName("계약 생성 성공 - 일용직, 첫 계약")
    void createContract_Success_DailyFirstContract() {
        // given
        Long siteId = 1L;
        Long employeeId = 1L;
        Long corporationId = 1L;

        Site site = createSite(siteId, "테스트현장", null);
        Employee employee = createEmployee(employeeId, "홍길동", null);
        Corporation corporation = createCorporation(corporationId, "테스트회사", "서울시 강남구");

        CreateContractRequest request = createContractRequest(employeeId, corporationId, null);

        Contract savedContract = Contract.builder()
                .employeeId(employeeId)
                .corporationId(corporationId)
                .contractState(ContractState.DRAFT)
                .build();
        try {
            java.lang.reflect.Field idField = Contract.class.getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(savedContract, 200L);
        } catch (Exception e) {
            // ID 설정 실패 시 무시
        }

        given(siteRepository.findById(siteId)).willReturn(Optional.of(site));
        given(employeeRepository.findById(employeeId)).willReturn(Optional.of(employee));
        given(corporationRepository.findById(corporationId)).willReturn(Optional.of(corporation));
        given(contractRepository.findByEmployeeIdAndContractState(employeeId, ContractState.FULLY_SIGNED))
                .willReturn(Collections.emptyList());
        given(contractRepository.save(any(Contract.class))).willReturn(savedContract);
        given(contractDetailRepository.save(any(ContractDetail.class))).willReturn(any());

        // when
        CreateContractResponse response = contractService.createContract(siteId, request, EmpType.DAILY);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContractId()).isEqualTo(200L);
        assertThat(response.getContractState()).isEqualTo(ContractState.DRAFT);

        verify(siteRepository).findById(siteId);
        verify(employeeRepository).findById(employeeId);
        verify(corporationRepository).findById(corporationId);
        verify(managerRepository, never()).findById(any()); // managerId가 null이므로 호출되지 않음
    }

    @Test
    @DisplayName("계약 생성 실패 - 근로자를 찾을 수 없음")
    void createContract_Fail_EmployeeNotFound() {
        // given
        Long siteId = 1L;
        Long employeeId = 999L;
        Site site = createSite(siteId, "테스트현장", null);
        CreateContractRequest request = createContractRequest(employeeId, 1L, 1L);

        given(siteRepository.findById(siteId)).willReturn(Optional.of(site));
        given(employeeRepository.findById(employeeId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> contractService.createContract(siteId, request, EmpType.PERMANENT))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ContractErrorCode.EMPLOYEE_NOT_FOUND);

        verify(employeeRepository).findById(employeeId);
        verify(corporationRepository, never()).findById(any());
    }

    @Test
    @DisplayName("계약 생성 실패 - 기업을 찾을 수 없음")
    void createContract_Fail_CorporationNotFound() {
        // given
        Long siteId = 1L;
        Long employeeId = 1L;
        Long corporationId = 999L;

        Site site = createSite(siteId, "테스트현장", null);
        Employee employee = createEmployee(employeeId, "홍길동", null);
        CreateContractRequest request = createContractRequest(employeeId, corporationId, 1L);

        given(siteRepository.findById(siteId)).willReturn(Optional.of(site));
        given(employeeRepository.findById(employeeId)).willReturn(Optional.of(employee));
        given(corporationRepository.findById(corporationId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> contractService.createContract(siteId, request, EmpType.PERMANENT))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ContractErrorCode.CORPORATION_NOT_FOUND);

        verify(corporationRepository).findById(corporationId);
        verify(managerRepository, never()).findById(any());
    }

    @Test
    @DisplayName("계약 생성 실패 - 관리자를 찾을 수 없음")
    void createContract_Fail_ManagerNotFound() {
        // given
        Long siteId = 1L;
        Long employeeId = 1L;
        Long corporationId = 1L;
        Long managerId = 999L;

        Site site = createSite(siteId, "테스트현장", null);
        Employee employee = createEmployee(employeeId, "홍길동", null);
        Corporation corporation = createCorporation(corporationId, "테스트회사", "서울시 강남구");
        CreateContractRequest request = createContractRequest(employeeId, corporationId, managerId);

        given(siteRepository.findById(siteId)).willReturn(Optional.of(site));
        given(employeeRepository.findById(employeeId)).willReturn(Optional.of(employee));
        given(corporationRepository.findById(corporationId)).willReturn(Optional.of(corporation));
        given(managerRepository.findById(managerId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> contractService.createContract(siteId, request, EmpType.PERMANENT))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ContractErrorCode.MANAGER_NOT_FOUND);

        verify(managerRepository).findById(managerId);
        verify(contractRepository, never()).save(any());
    }

    @Test
    @DisplayName("계약 생성 실패 - 다른 타입의 FULLY_SIGNED 계약이 이미 존재")
    void createContract_Fail_ConflictingEmpType() {
        // given
        Long siteId = 1L;
        Long employeeId = 1L;
        Long corporationId = 1L;

        Site site = createSite(siteId, "테스트현장", null);
        // 이미 일용직(DAILY) 타입으로 설정된 근로자
        Employee employee = createEmployee(employeeId, "홍길동", "DAILY");
        Corporation corporation = createCorporation(corporationId, "테스트회사", "서울시 강남구");

        // 일용직 FULLY_SIGNED 계약이 존재
        Contract existingContract = Contract.builder()
                .employeeId(employeeId)
                .contractState(ContractState.FULLY_SIGNED)
                .build();

        CreateContractRequest request = createContractRequest(employeeId, corporationId, null);

        given(siteRepository.findById(siteId)).willReturn(Optional.of(site));
        given(employeeRepository.findById(employeeId)).willReturn(Optional.of(employee));
        given(corporationRepository.findById(corporationId)).willReturn(Optional.of(corporation));
        given(contractRepository.findByEmployeeIdAndContractState(employeeId, ContractState.FULLY_SIGNED))
                .willReturn(List.of(existingContract));

        // when & then - 상용직(PERMANENT) 계약 생성 시도
        assertThatThrownBy(() -> contractService.createContract(siteId, request, EmpType.PERMANENT))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ContractErrorCode.CONFLICTING_EMP_TYPE);

        verify(contractRepository, never()).save(any());
    }

    @Test
    @DisplayName("계약 생성 성공 - 같은 타입의 FULLY_SIGNED 계약이 이미 존재")
    void createContract_Success_SameEmpType() {
        // given
        Long siteId = 1L;
        Long employeeId = 1L;
        Long corporationId = 1L;

        Site site = createSite(siteId, "테스트현장", null);
        // 이미 상용직(PERMANENT) 타입으로 설정된 근로자
        Employee employee = createEmployee(employeeId, "홍길동", "PERMANENT");
        Corporation corporation = createCorporation(corporationId, "테스트회사", "서울시 강남구");

        // 상용직 FULLY_SIGNED 계약이 존재
        Contract existingContract = Contract.builder()
                .employeeId(employeeId)
                .contractState(ContractState.FULLY_SIGNED)
                .build();

        CreateContractRequest request = createContractRequest(employeeId, corporationId, null);

        Contract savedContract = Contract.builder()
                .employeeId(employeeId)
                .corporationId(corporationId)
                .contractState(ContractState.DRAFT)
                .build();
        try {
            java.lang.reflect.Field idField = Contract.class.getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(savedContract, 300L);
        } catch (Exception e) {
            // ID 설정 실패 시 무시
        }

        given(siteRepository.findById(siteId)).willReturn(Optional.of(site));
        given(employeeRepository.findById(employeeId)).willReturn(Optional.of(employee));
        given(corporationRepository.findById(corporationId)).willReturn(Optional.of(corporation));
        given(contractRepository.findByEmployeeIdAndContractState(employeeId, ContractState.FULLY_SIGNED))
                .willReturn(List.of(existingContract));
        given(contractRepository.save(any(Contract.class))).willReturn(savedContract);
        given(contractDetailRepository.save(any(ContractDetail.class))).willReturn(any());

        // when - 같은 타입(PERMANENT) 계약 생성 시도
        CreateContractResponse response = contractService.createContract(siteId, request, EmpType.PERMANENT);

        // then - 성공해야 함
        assertThat(response).isNotNull();
        assertThat(response.getContractId()).isEqualTo(300L);

        verify(contractRepository).save(any(Contract.class));
        verify(contractDetailRepository).save(any(ContractDetail.class));
    }

    @Test
    @DisplayName("계약 생성 시 스냅샷 데이터 정확성 검증")
    void createContract_VerifySnapshotData() {
        // given
        Long siteId = 1L;
        Long employeeId = 1L;
        Long corporationId = 1L;

        Site site = createSite(siteId, "테스트현장", null);
        Employee employee = createEmployee(employeeId, "홍길동", null);
        employee.updateProfile(null, "010-1234-5678", "서울시 강남구 테헤란로 123");

        Corporation corporation = createCorporation(corporationId, "테스트주식회사", "서울시 강남구 역삼동");

        CreateContractRequest request = createContractRequest(employeeId, corporationId, null);

        Contract savedContract = Contract.builder()
                .employeeId(employeeId)
                .corporationId(corporationId)
                .contractState(ContractState.DRAFT)
                .build();
        try {
            java.lang.reflect.Field idField = Contract.class.getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(savedContract, 400L);
        } catch (Exception e) {
            // ID 설정 실패 시 무시
        }

        given(siteRepository.findById(siteId)).willReturn(Optional.of(site));
        given(employeeRepository.findById(employeeId)).willReturn(Optional.of(employee));
        given(corporationRepository.findById(corporationId)).willReturn(Optional.of(corporation));
        given(contractRepository.findByEmployeeIdAndContractState(employeeId, ContractState.FULLY_SIGNED))
                .willReturn(Collections.emptyList());
        given(contractRepository.save(any(Contract.class))).willReturn(savedContract);
        given(contractDetailRepository.save(any(ContractDetail.class))).willReturn(any());

        // when
        contractService.createContract(siteId, request, EmpType.PERMANENT);

        // then
        ArgumentCaptor<ContractDetail> contractDetailCaptor = ArgumentCaptor.forClass(ContractDetail.class);
        verify(contractDetailRepository).save(contractDetailCaptor.capture());

        ContractDetail savedDetail = contractDetailCaptor.getValue();
        assertThat(savedDetail.getCorpName()).isEqualTo("테스트주식회사");
        assertThat(savedDetail.getEmpName()).isEqualTo("홍길동");
        assertThat(savedDetail.getCorpAddress()).isEqualTo("서울시 강남구 역삼동");
        assertThat(savedDetail.getEmpAddress()).isEqualTo("서울시 강남구 테헤란로 123");
        assertThat(savedDetail.getWorkPay()).isEqualTo(new BigDecimal("3000000.00"));
        assertThat(savedDetail.getPayPeriod()).isEqualTo(PayPeriod.MONTHLY);
        assertThat(savedDetail.getPayType()).isEqualTo(PayType.TRANSFER);
    }

    @Test
    @DisplayName("계약 생성 실패 - 현장을 찾을 수 없음")
    void createContract_Fail_SiteNotFound() {
        // given
        Long siteId = 999L;
        CreateContractRequest request = createContractRequest(1L, 1L, 1L);

        given(siteRepository.findById(siteId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> contractService.createContract(siteId, request, EmpType.PERMANENT))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ContractErrorCode.SITE_NOT_FOUND);

        verify(siteRepository).findById(siteId);
        verify(employeeRepository, never()).findById(any());
    }

    @Test
    @DisplayName("계약 생성 성공 - Manager 권한 검증 성공")
    void createContract_Success_ManagerAuthorized() {
        // given
        Long siteId = 1L;
        Long employeeId = 1L;
        Long corporationId = 1L;
        Long managerId = 1L;

        Manager manager = createManager(managerId, "김관리");
        Site site = createSite(siteId, "테스트현장", manager);
        Employee employee = createEmployee(employeeId, "홍길동", null);
        Corporation corporation = createCorporation(corporationId, "테스트회사", "서울시 강남구");

        CreateContractRequest request = createContractRequest(employeeId, corporationId, managerId);

        Contract savedContract = Contract.builder()
                .employeeId(employeeId)
                .corporationId(corporationId)
                .managerId(managerId)
                .contractState(ContractState.DRAFT)
                .build();
        try {
            java.lang.reflect.Field idField = Contract.class.getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(savedContract, 500L);
        } catch (Exception e) {
            // ID 설정 실패 시 무시
        }

        given(siteRepository.findById(siteId)).willReturn(Optional.of(site));
        given(employeeRepository.findById(employeeId)).willReturn(Optional.of(employee));
        given(corporationRepository.findById(corporationId)).willReturn(Optional.of(corporation));
        given(managerRepository.findById(managerId)).willReturn(Optional.of(manager));
        given(contractRepository.findByEmployeeIdAndContractState(employeeId, ContractState.FULLY_SIGNED))
                .willReturn(Collections.emptyList());
        given(contractRepository.save(any(Contract.class))).willReturn(savedContract);
        given(contractDetailRepository.save(any(ContractDetail.class))).willReturn(any());

        // when
        CreateContractResponse response = contractService.createContract(siteId, request, EmpType.PERMANENT);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContractId()).isEqualTo(500L);
        assertThat(response.getContractState()).isEqualTo(ContractState.DRAFT);

        verify(siteRepository).findById(siteId);
        verify(managerRepository).findById(managerId);
        verify(contractRepository).save(any(Contract.class));
    }

    @Test
    @DisplayName("계약 생성 실패 - Manager가 해당 현장의 관리자가 아님")
    void createContract_Fail_ManagerNotAuthorized() {
        // given
        Long siteId = 1L;
        Long employeeId = 1L;
        Long corporationId = 1L;
        Long managerId = 1L;
        Long otherManagerId = 2L;

        Manager siteManager = createManager(otherManagerId, "다른관리자");
        Manager requestManager = createManager(managerId, "요청관리자");
        Site site = createSite(siteId, "테스트현장", siteManager);
        Employee employee = createEmployee(employeeId, "홍길동", null);
        Corporation corporation = createCorporation(corporationId, "테스트회사", "서울시 강남구");

        CreateContractRequest request = createContractRequest(employeeId, corporationId, managerId);

        given(siteRepository.findById(siteId)).willReturn(Optional.of(site));
        given(employeeRepository.findById(employeeId)).willReturn(Optional.of(employee));
        given(corporationRepository.findById(corporationId)).willReturn(Optional.of(corporation));
        given(managerRepository.findById(managerId)).willReturn(Optional.of(requestManager));

        // when & then
        assertThatThrownBy(() -> contractService.createContract(siteId, request, EmpType.PERMANENT))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ContractErrorCode.MANAGER_NOT_AUTHORIZED);

        verify(siteRepository).findById(siteId);
        verify(managerRepository).findById(managerId);
        verify(contractRepository, never()).save(any());
    }

    @Test
    @DisplayName("계약 생성 실패 - Site에 Manager가 할당되지 않았는데 Manager ID 제공됨")
    void createContract_Fail_SiteHasNoManager() {
        // given
        Long siteId = 1L;
        Long employeeId = 1L;
        Long corporationId = 1L;
        Long managerId = 1L;

        Manager manager = createManager(managerId, "김관리");
        Site site = createSite(siteId, "테스트현장", null); // Site에 Manager가 없음
        Employee employee = createEmployee(employeeId, "홍길동", null);
        Corporation corporation = createCorporation(corporationId, "테스트회사", "서울시 강남구");

        CreateContractRequest request = createContractRequest(employeeId, corporationId, managerId);

        given(siteRepository.findById(siteId)).willReturn(Optional.of(site));
        given(employeeRepository.findById(employeeId)).willReturn(Optional.of(employee));
        given(corporationRepository.findById(corporationId)).willReturn(Optional.of(corporation));
        given(managerRepository.findById(managerId)).willReturn(Optional.of(manager));

        // when & then
        assertThatThrownBy(() -> contractService.createContract(siteId, request, EmpType.PERMANENT))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ContractErrorCode.MANAGER_NOT_AUTHORIZED);

        verify(contractRepository, never()).save(any());
    }

    // ========== Helper Methods ==========

    private Employee createEmployee(Long id, String name, String empType) {
        User user = User.builder()
                .userId("test" + id)
                .phone("010-0000-0000")
                .build();

        Employee employee = Employee.builder()
                .user(user)
                .empName(name)
                .empType(empType)
                .build();

        try {
            java.lang.reflect.Field idField = Employee.class.getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(employee, id);
        } catch (Exception e) {
            // ID 설정 실패 시 무시
        }

        return employee;
    }

    private Corporation createCorporation(Long id, String name, String address) {
        User user = User.builder()
                .userId("corp" + id)
                .phone("02-0000-0000")
                .build();

        Corporation corporation = Corporation.builder()
                .user(user)
                .corpName(name)
                .corpAddress(address)
                .corpCeoName("대표이사")
                .build();

        try {
            java.lang.reflect.Field idField = Corporation.class.getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(corporation, id);
        } catch (Exception e) {
            // ID 설정 실패 시 무시
        }

        return corporation;
    }

    private Manager createManager(Long id, String name) {
        User user = User.builder()
                .userId("manager" + id)
                .phone("010-1111-1111")
                .build();

        Manager manager = Manager.builder()
                .user(user)
                .managerName(name)
                .build();

        try {
            java.lang.reflect.Field idField = Manager.class.getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(manager, id);
        } catch (Exception e) {
            // ID 설정 실패 시 무시
        }

        return manager;
    }

    private Site createSite(Long id, String name, Manager manager) {
        Site site = Site.builder()
                .siteName(name)
                .siteAddress("서울시 강남구")
                .manager(manager)
                .build();

        try {
            java.lang.reflect.Field idField = Site.class.getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(site, id);
        } catch (Exception e) {
            // ID 설정 실패 시 무시
        }

        return site;
    }

    private CreateContractRequest createContractRequest(Long employeeId, Long corporationId, Long managerId) {
        ContractDetailRequest details = ContractDetailRequest.builder()
                .workPlace("서울시 강남구 테헤란로 123")
                .workType("일반건설현장근로자")
                .workStartTime(LocalTime.of(9, 0))
                .workEndTime(LocalTime.of(18, 0))
                .breakStartTime(LocalTime.of(12, 0))
                .breakEndTime(LocalTime.of(13, 0))
                .workOnDay("주 5일 (월~금)")
                .workOffDay("토, 일")
                .workPay(new BigDecimal("3000000.00"))
                .workBonus(new BigDecimal("500000.00"))
                .additionalHourPay(new BigDecimal("150000.00"))
                .additionalNightPay(new BigDecimal("100000.00"))
                .additionalHolidayPay(new BigDecimal("200000.00"))
                .payday("매월 25일")
                .payPeriod(PayPeriod.MONTHLY)
                .payType(PayType.TRANSFER)
                .isEoiApplicable(true)
                .isWciApplicable(true)
                .isNpsApplicable(true)
                .isNhiApplicable(true)
                .build();

        return CreateContractRequest.builder()
                .employeeId(employeeId)
                .corporationId(corporationId)
                .managerId(managerId)
                .role("현장 관리자")
                .employeeStartDate(LocalDate.of(2024, 1, 1))
                .employeeEndDate(LocalDate.of(2024, 12, 31))
                .details(details)
                .build();
    }

    // ========== getContracts 테스트 ==========

    @Test
    @DisplayName("계약 목록 조회 성공 - 필터링 없음")
    void getContracts_Success_NoFilter() {
        // given
        Long siteId = 1L;
        Long managerId = 1L;
        Manager manager = createManager(managerId, "김관리");
        Site site = createSite(siteId, "테스트현장", manager);

        ContractSearchCondition condition = ContractSearchCondition.builder()
                .page(1)
                .size(20)
                .build();

        Employee employee1 = createEmployeeWithResidentNum(1L, "홍길동", "950101-1234567", "PERMANENT");
        Employee employee2 = createEmployeeWithResidentNum(2L, "김철수", "880215-2345678", "DAILY");

        Contract contract1 = createContract(1L, 1L, 1L, managerId, EmpType.PERMANENT, ContractState.FULLY_SIGNED, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));
        Contract contract2 = createContract(2L, 2L, 1L, managerId, EmpType.DAILY, ContractState.DRAFT, LocalDate.of(2024, 3, 1), LocalDate.of(2024, 6, 30));

        Page<Contract> contractPage = new PageImpl<>(List.of(contract1, contract2));

        given(siteRepository.findById(siteId)).willReturn(Optional.of(site));
        given(contractRepository.findByDynamicConditions(
                eq(managerId),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                any(Pageable.class)
        )).willReturn(contractPage);
        given(employeeRepository.findAllById(List.of(1L, 2L))).willReturn(List.of(employee1, employee2));

        // when
        ContractListResponse response = contractService.getContracts(siteId, condition);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getPageInfo().getCurrentPage()).isEqualTo(1);
        assertThat(response.getPageInfo().getTotalElements()).isEqualTo(2);

        // 주민번호 마스킹 검증
        ContractSummaryDto firstItem = response.getItems().get(0);
        assertThat(firstItem.getEmployeeResidentNumber()).isEqualTo("950101-1******");
        assertThat(firstItem.getEmployeeName()).isEqualTo("홍길동");

        verify(siteRepository).findById(siteId);
        verify(contractRepository).findByDynamicConditions(
                eq(managerId),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                any(Pageable.class)
        );
        verify(employeeRepository).findAllById(List.of(1L, 2L));
    }

    @Test
    @DisplayName("계약 목록 조회 성공 - employeeId 필터")
    void getContracts_Success_FilterByEmployeeId() {
        // given
        Long siteId = 1L;
        Long managerId = 1L;
        Manager manager = createManager(managerId, "김관리");
        Site site = createSite(siteId, "테스트현장", manager);

        ContractSearchCondition condition = ContractSearchCondition.builder()
                .employeeId(1L)
                .page(1)
                .size(20)
                .build();

        Employee employee1 = createEmployeeWithResidentNum(1L, "홍길동", "950101-1234567", "PERMANENT");

        Contract contract1 = createContract(1L, 1L, 1L, managerId, EmpType.PERMANENT, ContractState.FULLY_SIGNED, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));

        Page<Contract> contractPage = new PageImpl<>(List.of(contract1));

        given(siteRepository.findById(siteId)).willReturn(Optional.of(site));
        given(contractRepository.findByDynamicConditions(
                eq(managerId),
                eq(1L),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                any(Pageable.class)
        )).willReturn(contractPage);
        given(employeeRepository.findAllById(List.of(1L))).willReturn(List.of(employee1));

        // when
        ContractListResponse response = contractService.getContracts(siteId, condition);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getEmployeeId()).isEqualTo(1L);
        assertThat(response.getItems().get(0).getEmployeeName()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("계약 목록 조회 성공 - empType 필터")
    void getContracts_Success_FilterByEmpType() {
        // given
        Long siteId = 1L;
        Long managerId = 1L;
        Manager manager = createManager(managerId, "김관리");
        Site site = createSite(siteId, "테스트현장", manager);

        ContractSearchCondition condition = ContractSearchCondition.builder()
                .empType(EmpType.PERMANENT)
                .page(1)
                .size(20)
                .build();

        Employee employee1 = createEmployeeWithResidentNum(1L, "홍길동", "950101-1234567", "PERMANENT");

        Contract contract1 = createContract(1L, 1L, 1L, managerId, EmpType.PERMANENT, ContractState.FULLY_SIGNED, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));

        Page<Contract> contractPage = new PageImpl<>(List.of(contract1));

        given(siteRepository.findById(siteId)).willReturn(Optional.of(site));
        // empType 필터를 위해 Employee 조회
        given(employeeRepository.findByEmpType("PERMANENT")).willReturn(List.of(employee1));
        given(contractRepository.findByDynamicConditions(
                eq(managerId),
                eq(null),
                eq(List.of(1L)),
                eq(null),
                eq(null),
                eq(null),
                any(Pageable.class)
        )).willReturn(contractPage);
        given(employeeRepository.findAllById(List.of(1L))).willReturn(List.of(employee1));

        // when
        ContractListResponse response = contractService.getContracts(siteId, condition);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getEmpType()).isEqualTo(EmpType.PERMANENT);

        verify(employeeRepository).findByEmpType("PERMANENT");
    }

    @Test
    @DisplayName("계약 목록 조회 성공 - status 필터")
    void getContracts_Success_FilterByStatus() {
        // given
        Long siteId = 1L;
        Long managerId = 1L;
        Manager manager = createManager(managerId, "김관리");
        Site site = createSite(siteId, "테스트현장", manager);

        ContractSearchCondition condition = ContractSearchCondition.builder()
                .status(ContractState.FULLY_SIGNED)
                .page(1)
                .size(20)
                .build();

        Employee employee1 = createEmployeeWithResidentNum(1L, "홍길동", "950101-1234567", "PERMANENT");

        Contract contract1 = createContract(1L, 1L, 1L, managerId, EmpType.PERMANENT, ContractState.FULLY_SIGNED, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));

        Page<Contract> contractPage = new PageImpl<>(List.of(contract1));

        given(siteRepository.findById(siteId)).willReturn(Optional.of(site));
        given(contractRepository.findByDynamicConditions(
                eq(managerId),
                eq(null),
                eq(null),
                eq(ContractState.FULLY_SIGNED),
                eq(null),
                eq(null),
                any(Pageable.class)
        )).willReturn(contractPage);
        given(employeeRepository.findAllById(List.of(1L))).willReturn(List.of(employee1));

        // when
        ContractListResponse response = contractService.getContracts(siteId, condition);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getContractState()).isEqualTo(ContractState.FULLY_SIGNED);
    }

    @Test
    @DisplayName("계약 목록 조회 성공 - 날짜 범위 필터")
    void getContracts_Success_FilterByDateRange() {
        // given
        Long siteId = 1L;
        Long managerId = 1L;
        Manager manager = createManager(managerId, "김관리");
        Site site = createSite(siteId, "테스트현장", manager);

        LocalDate from = LocalDate.of(2024, 2, 1);
        LocalDate to = LocalDate.of(2024, 4, 1);

        ContractSearchCondition condition = ContractSearchCondition.builder()
                .from(from)
                .to(to)
                .page(1)
                .size(20)
                .build();

        Employee employee2 = createEmployeeWithResidentNum(2L, "김철수", "880215-2345678", "DAILY");

        Contract contract2 = createContract(2L, 2L, 1L, managerId, EmpType.DAILY, ContractState.DRAFT, LocalDate.of(2024, 3, 1), LocalDate.of(2024, 6, 30));

        Page<Contract> contractPage = new PageImpl<>(List.of(contract2));

        given(siteRepository.findById(siteId)).willReturn(Optional.of(site));
        given(contractRepository.findByDynamicConditions(
                eq(managerId),
                eq(null),
                eq(null),
                eq(null),
                eq(from),
                eq(to),
                any(Pageable.class)
        )).willReturn(contractPage);
        given(employeeRepository.findAllById(List.of(2L))).willReturn(List.of(employee2));

        // when
        ContractListResponse response = contractService.getContracts(siteId, condition);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getEmployeeStartDate()).isEqualTo(LocalDate.of(2024, 3, 1));
    }

    @Test
    @DisplayName("계약 목록 조회 실패 - Site를 찾을 수 없음")
    void getContracts_Fail_SiteNotFound() {
        // given
        Long siteId = 999L;
        ContractSearchCondition condition = ContractSearchCondition.builder()
                .page(1)
                .size(20)
                .build();

        given(siteRepository.findById(siteId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> contractService.getContracts(siteId, condition))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ContractErrorCode.SITE_NOT_FOUND);

        verify(siteRepository).findById(siteId);
        verify(contractRepository, never()).findByManagerId(any(), any());
    }

    @Test
    @DisplayName("계약 목록 조회 성공 - 빈 목록 반환 (현장에 관리자가 없음)")
    void getContracts_Success_EmptyResult_NoManager() {
        // given
        Long siteId = 1L;
        Site site = createSite(siteId, "테스트현장", null); // Manager 없음

        ContractSearchCondition condition = ContractSearchCondition.builder()
                .page(1)
                .size(20)
                .build();

        given(siteRepository.findById(siteId)).willReturn(Optional.of(site));

        // when
        ContractListResponse response = contractService.getContracts(siteId, condition);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getItems()).isEmpty();
        assertThat(response.getPageInfo().getTotalElements()).isEqualTo(0);

        verify(siteRepository).findById(siteId);
        verify(contractRepository, never()).findByManagerId(any(), any());
    }

    // Helper 메서드 추가
    private Employee createEmployeeWithResidentNum(Long id, String name, String residentNum, String empType) {
        User user = User.builder()
                .userId("test" + id)
                .phone("010-0000-0000")
                .build();

        Employee employee = Employee.builder()
                .user(user)
                .empName(name)
                .residentNum(residentNum)
                .empType(empType)
                .build();

        try {
            java.lang.reflect.Field idField = Employee.class.getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(employee, id);
        } catch (Exception e) {
            // ID 설정 실패 시 무시
        }

        return employee;
    }

    private Contract createContract(Long id, Long employeeId, Long corporationId, Long managerId, EmpType empType, ContractState contractState, LocalDate startDate, LocalDate endDate) {
        // empType은 Contract가 아닌 Employee에 저장되므로, Contract 생성 시에는 사용하지 않음
        Contract contract = Contract.builder()
                .employeeId(employeeId)
                .corporationId(corporationId)
                .managerId(managerId)
                .contractState(contractState)
                .employeeStartDate(startDate)
                .employeeEndDate(endDate)
                .build();

        try {
            java.lang.reflect.Field idField = Contract.class.getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(contract, id);
        } catch (Exception e) {
            // ID 설정 실패 시 무시
        }

        return contract;
    }
}