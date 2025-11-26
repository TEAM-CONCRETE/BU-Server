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

    @Mock
    private UserRepository userRepository;

    @Mock
    private ContractSignatureService contractSignatureService;

    @InjectMocks
    private ContractService contractService;

    @Test
    @DisplayName("계약 생성 성공 - 상용직, 첫 계약")
    void createContract_Success_PermanentFirstContract() {
        // given
        Long siteId = 1L;
        Long userId = 100L;
        Long employeeId = 1L;
        Long corporationId = 1L;
        Long managerId = 1L;

        User user = createUser(userId, "test_employee");
        Manager manager = createManager(managerId, "김관리");
        Corporation corporation = createCorporation(corporationId, "테스트회사", "서울시 강남구");
        Site site = createSiteWithCorporation(siteId, "테스트현장", manager, corporation);
        Employee employee = createEmployee(employeeId, "홍길동", null);

        CreateContractRequest request = createContractRequest("test_employee");

        Contract savedContract = Contract.builder()
                .employeeId(employeeId)
                .corporationId(corporationId)
                .managerId(managerId)
                .empType(EmpType.PERMANENT)
                .contractState(ContractState.DRAFT)
                .build();
        try {
            java.lang.reflect.Field idField = Contract.class.getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(savedContract, 100L);
        } catch (Exception e) {
            // ID 설정 실패 시 무시
        }

        given(siteRepository.findById(siteId)).willReturn(Optional.of(site));
        given(userRepository.findByUserId("test_employee")).willReturn(Optional.of(user));
        given(employeeRepository.findByUserId(userId)).willReturn(Optional.of(employee));
        given(contractRepository.findByEmployeeIdAndContractState(employeeId, ContractState.FULLY_SIGNED))
                .willReturn(Collections.emptyList());
        given(contractRepository.save(any(Contract.class))).willAnswer(invocation -> {
            Contract arg = invocation.getArgument(0);
            if (arg.getContractState() != null) {
                // 실제로는 엔티티의 상태가 변경되므로, 반환 객체도 동일한 상태를 가져야 함
            }
            return savedContract;
        });
        given(contractDetailRepository.save(any(ContractDetail.class))).willReturn(any());

        given(contractSignatureService.generateInitialPdf(100L)).willAnswer(invocation -> {
            savedContract.transitionToManagerSigningPending();
            return "https://bucket.s3.amazonaws.com/contracts/100/v1.pdf";
        });

        // when
        CreateContractResponse response = contractService.createContract(siteId, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContractId()).isEqualTo(100L);
        assertThat(response.getContractState()).isEqualTo(ContractState.MANAGER_SIGNING_PENDING);
        assertThat(response.getPdfUrl()).isEqualTo("https://bucket.s3.amazonaws.com/contracts/100/v1.pdf");

        verify(siteRepository).findById(siteId);
        verify(userRepository).findByUserId("test_employee");
        verify(employeeRepository).findByUserId(userId);
        verify(contractRepository).save(any(Contract.class));
        verify(contractDetailRepository).save(any(ContractDetail.class));
    }

    @Test
    @DisplayName("계약 생성 성공 - 일용직, 첫 계약")
    void createContract_Success_DailyFirstContract() {
        // given
        Long siteId = 1L;
        Long userId = 200L;
        Long employeeId = 1L;
        Long corporationId = 1L;
        Long managerId = 1L;

        User user = createUser(userId, "daily_employee");
        Manager manager = createManager(managerId, "김관리");
        Corporation corporation = createCorporation(corporationId, "테스트회사", "서울시 강남구");
        Site site = createSiteWithCorporation(siteId, "테스트현장", manager, corporation);
        Employee employee = createEmployee(employeeId, "홍길동", null);

        CreateContractRequest request = createContractRequest("daily_employee");

        Contract savedContract = Contract.builder()
                .employeeId(employeeId)
                .corporationId(corporationId)
                .empType(EmpType.PERMANENT)
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
        given(userRepository.findByUserId("daily_employee")).willReturn(Optional.of(user));
        given(employeeRepository.findByUserId(userId)).willReturn(Optional.of(employee));
        given(contractRepository.findByEmployeeIdAndContractState(employeeId, ContractState.FULLY_SIGNED))
                .willReturn(Collections.emptyList());
        given(contractRepository.save(any(Contract.class))).willReturn(savedContract);
        given(contractDetailRepository.save(any(ContractDetail.class))).willReturn(any());

        given(contractSignatureService.generateInitialPdf(200L)).willAnswer(invocation -> {
            savedContract.transitionToManagerSigningPending();
            return "https://bucket.s3.amazonaws.com/contracts/200/v1.pdf";
        });

        // when
        CreateContractResponse response = contractService.createContract(siteId, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContractId()).isEqualTo(200L);
        assertThat(response.getContractState()).isEqualTo(ContractState.MANAGER_SIGNING_PENDING);
        assertThat(response.getPdfUrl()).isNotNull();

        verify(siteRepository).findById(siteId);
        verify(userRepository).findByUserId("daily_employee");
        verify(employeeRepository).findByUserId(userId);
    }

    @Test
    @DisplayName("계약 생성 실패 - 근로자를 찾을 수 없음")
    void createContract_Fail_EmployeeNotFound() {
        // given
        Long siteId = 1L;
        Long userId = 999L;
        Long managerId = 1L;
        Manager manager = createManager(managerId, "김관리");
        Corporation corporation = createCorporation(1L, "테스트회사", "서울시 강남구");
        Site site = createSiteWithCorporation(siteId, "테스트현장", manager, corporation);
        User user = createUser(userId, "unknown_employee");
        CreateContractRequest request = createContractRequest("unknown_employee");

        given(siteRepository.findById(siteId)).willReturn(Optional.of(site));
        given(userRepository.findByUserId("unknown_employee")).willReturn(Optional.of(user));
        given(employeeRepository.findByUserId(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> contractService.createContract(siteId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ContractErrorCode.EMPLOYEE_NOT_FOUND);

        verify(userRepository).findByUserId("unknown_employee");
        verify(employeeRepository).findByUserId(userId);
    }

    @Test
    @DisplayName("계약 생성 실패 - 기업을 찾을 수 없음")
    void createContract_Fail_CorporationNotFound() {
        // given
        Long siteId = 1L;
        Site site = createSite(siteId, "테스트현장", null); // Corporation 없음
        CreateContractRequest request = createContractRequest("test_employee");

        given(siteRepository.findById(siteId)).willReturn(Optional.of(site));

        // when & then
        assertThatThrownBy(() -> contractService.createContract(siteId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ContractErrorCode.CORPORATION_NOT_FOUND);

        verify(siteRepository).findById(siteId);
    }

    @Test
    @DisplayName("계약 생성 실패 - User를 찾을 수 없음 (근로자를 찾을 수 없음으로 처리)")
    void createContract_Fail_UserNotFound() {
        // given
        Long siteId = 1L;
        Long managerId = 1L;
        Manager manager = createManager(managerId, "김관리");
        Corporation corporation = createCorporation(1L, "테스트회사", "서울시 강남구");
        Site site = createSiteWithCorporation(siteId, "테스트현장", manager, corporation);
        CreateContractRequest request = createContractRequest("unknown_user");

        given(siteRepository.findById(siteId)).willReturn(Optional.of(site));
        given(userRepository.findByUserId("unknown_user")).willReturn(Optional.empty());

        // when & then
        // User가 없으면 결국 Employee를 찾을 수 없으므로 EMPLOYEE_NOT_FOUND 반환
        assertThatThrownBy(() -> contractService.createContract(siteId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ContractErrorCode.EMPLOYEE_NOT_FOUND);

        verify(userRepository).findByUserId("unknown_user");
        verify(employeeRepository, never()).findByUserId(any());
        verify(contractRepository, never()).save(any());
    }

    @Test
    @DisplayName("계약 생성 실패 - 다른 타입의 FULLY_SIGNED 계약이 이미 존재")
    void createContract_Fail_ConflictingEmpType() {
        // given
        Long siteId = 1L;
        Long userId = 300L;
        Long employeeId = 1L;
        Long corporationId = 1L;
        Long managerId = 1L;

        User user = createUser(userId, "conflict_employee");
        Manager manager = createManager(managerId, "김관리");
        Corporation corporation = createCorporation(corporationId, "테스트회사", "서울시 강남구");
        Site site = createSiteWithCorporation(siteId, "테스트현장", manager, corporation);
        // 이미 일용직(DAILY) 타입으로 설정된 근로자
        Employee employee = createEmployee(employeeId, "홍길동", "DAILY");

        // 일용직 FULLY_SIGNED 계약이 존재
        Contract existingContract = Contract.builder()
                .employeeId(employeeId)
                .empType(EmpType.DAILY)
                .contractState(ContractState.FULLY_SIGNED)
                .build();

        CreateContractRequest request = createContractRequest("conflict_employee");

        given(siteRepository.findById(siteId)).willReturn(Optional.of(site));
        given(userRepository.findByUserId("conflict_employee")).willReturn(Optional.of(user));
        given(employeeRepository.findByUserId(userId)).willReturn(Optional.of(employee));
        given(contractRepository.findByEmployeeIdAndContractState(employeeId, ContractState.FULLY_SIGNED))
                .willReturn(List.of(existingContract));

        // when & then - 상용직(PERMANENT) 계약 생성 시도
        assertThatThrownBy(() -> contractService.createContract(siteId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ContractErrorCode.CONFLICTING_EMP_TYPE);

        verify(contractRepository, never()).save(any());
    }

    @Test
    @DisplayName("계약 생성 성공 - 같은 타입의 FULLY_SIGNED 계약이 이미 존재")
    void createContract_Success_SameEmpType() {
        // given
        Long siteId = 1L;
        Long userId = 400L;
        Long employeeId = 1L;
        Long corporationId = 1L;
        Long managerId = 1L;

        User user = createUser(userId, "same_type_employee");
        Manager manager = createManager(managerId, "김관리");
        Corporation corporation = createCorporation(corporationId, "테스트회사", "서울시 강남구");
        Site site = createSiteWithCorporation(siteId, "테스트현장", manager, corporation);
        // 이미 상용직(PERMANENT) 타입으로 설정된 근로자
        Employee employee = createEmployee(employeeId, "홍길동", "PERMANENT");

        // 상용직 FULLY_SIGNED 계약이 존재
        Contract existingContract = Contract.builder()
                .employeeId(employeeId)
                .empType(EmpType.PERMANENT)
                .contractState(ContractState.FULLY_SIGNED)
                .build();

        CreateContractRequest request = createContractRequest("same_type_employee");

        Contract savedContract = Contract.builder()
                .employeeId(employeeId)
                .corporationId(corporationId)
                .empType(EmpType.PERMANENT)
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
        given(userRepository.findByUserId("same_type_employee")).willReturn(Optional.of(user));
        given(employeeRepository.findByUserId(userId)).willReturn(Optional.of(employee));
        given(contractRepository.findByEmployeeIdAndContractState(employeeId, ContractState.FULLY_SIGNED))
                .willReturn(List.of(existingContract));
        given(contractRepository.save(any(Contract.class))).willReturn(savedContract);
        given(contractDetailRepository.save(any(ContractDetail.class))).willReturn(any());
        given(contractSignatureService.generateInitialPdf(300L))
                .willReturn("https://bucket.s3.amazonaws.com/contracts/300/v1.pdf");

        // when - 같은 타입(PERMANENT) 계약 생성 시도
        CreateContractResponse response = contractService.createContract(siteId, request);

        // then - 성공해야 함
        assertThat(response).isNotNull();
        assertThat(response.getContractId()).isEqualTo(300L);
        assertThat(response.getPdfUrl()).isNotNull();

        verify(contractRepository).save(any(Contract.class));
        verify(contractDetailRepository).save(any(ContractDetail.class));
    }

    @Test
    @DisplayName("계약 생성 시 스냅샷 데이터 정확성 검증")
    void createContract_VerifySnapshotData() {
        // given
        Long siteId = 1L;
        Long userId = 500L;
        Long employeeId = 1L;
        Long corporationId = 1L;
        Long managerId = 1L;

        User user = createUser(userId, "snapshot_employee");
        Manager manager = createManager(managerId, "김관리");
        Corporation corporation = createCorporation(corporationId, "테스트주식회사", "서울시 강남구 역삼동");
        Site site = createSiteWithCorporation(siteId, "테스트현장", manager, corporation);
        Employee employee = createEmployee(employeeId, "홍길동", null);
        employee.updateProfile(null, "010-1234-5678", "서울시 강남구 테헤란로 123");

        CreateContractRequest request = createContractRequest("snapshot_employee");

        Contract savedContract = Contract.builder()
                .employeeId(employeeId)
                .corporationId(corporationId)
                .empType(EmpType.PERMANENT)
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
        given(userRepository.findByUserId("snapshot_employee")).willReturn(Optional.of(user));
        given(employeeRepository.findByUserId(userId)).willReturn(Optional.of(employee));
        given(contractRepository.findByEmployeeIdAndContractState(employeeId, ContractState.FULLY_SIGNED))
                .willReturn(Collections.emptyList());
        given(contractRepository.save(any(Contract.class))).willReturn(savedContract);
        given(contractDetailRepository.save(any(ContractDetail.class))).willReturn(any());
        given(contractSignatureService.generateInitialPdf(400L))
                .willReturn("https://bucket.s3.amazonaws.com/contracts/400/v1.pdf");

        // when
        contractService.createContract(siteId, request);

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
        CreateContractRequest request = createContractRequest("test_employee");

        given(siteRepository.findById(siteId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> contractService.createContract(siteId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ContractErrorCode.SITE_NOT_FOUND);

        verify(siteRepository).findById(siteId);
        verify(userRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("계약 생성 성공 - Site에 Manager 설정되어 있는 경우")
    void createContract_Success_WithManager() {
        // given
        Long siteId = 1L;
        Long userId = 600L;
        Long employeeId = 1L;
        Long corporationId = 1L;
        Long managerId = 1L;

        User user = createUser(userId, "manager_test_employee");
        Manager manager = createManager(managerId, "김관리");
        Corporation corporation = createCorporation(corporationId, "테스트회사", "서울시 강남구");
        Site site = createSiteWithCorporation(siteId, "테스트현장", manager, corporation);
        Employee employee = createEmployee(employeeId, "홍길동", null);

        CreateContractRequest request = createContractRequest("manager_test_employee");

        Contract savedContract = Contract.builder()
                .employeeId(employeeId)
                .corporationId(corporationId)
                .managerId(managerId)
                .empType(EmpType.PERMANENT)
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
        given(userRepository.findByUserId("manager_test_employee")).willReturn(Optional.of(user));
        given(employeeRepository.findByUserId(userId)).willReturn(Optional.of(employee));
        given(contractRepository.findByEmployeeIdAndContractState(employeeId, ContractState.FULLY_SIGNED))
                .willReturn(Collections.emptyList());
        given(contractRepository.save(any(Contract.class))).willReturn(savedContract);
        given(contractDetailRepository.save(any(ContractDetail.class))).willReturn(any());

        given(contractSignatureService.generateInitialPdf(500L)).willAnswer(invocation -> {
            savedContract.transitionToManagerSigningPending();
            return "https://bucket.s3.amazonaws.com/contracts/500/v1.pdf";
        });

        // when
        CreateContractResponse response = contractService.createContract(siteId, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContractId()).isEqualTo(500L);
        assertThat(response.getContractState()).isEqualTo(ContractState.MANAGER_SIGNING_PENDING);
        assertThat(response.getPdfUrl()).isNotNull();

        verify(siteRepository).findById(siteId);
        verify(contractRepository).save(any(Contract.class));
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
        return createSiteWithCorporation(id, name, manager, null);
    }

    private Site createSiteWithCorporation(Long id, String name, Manager manager, Corporation corporation) {
        Site site = Site.builder()
                .siteName(name)
                .siteAddress("서울시 강남구")
                .manager(manager)
                .corporation(corporation)
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

    private CreateContractRequest createContractRequest(String employeeUserId) {
        ContractDetailRequest details = ContractDetailRequest.builder()
                .workPlace("서울시 강남구 테헤란로 123")
                .workType("일반건설현장근로자")
                .workStartTime(LocalTime.of(9, 0))
                .workEndTime(LocalTime.of(18, 0))
                .breakStartTime(LocalTime.of(12, 0))
                .breakEndTime(LocalTime.of(13, 0))
                .workOnDays("주 5일 (월~금)")
                .workOffDays("토, 일")
                .workPay(new BigDecimal("3000000.00"))
                .additionalHourPay(new BigDecimal("150000.00"))
                .additionalNightPay(new BigDecimal("100000.00"))
                .additionalHolidayPay(new BigDecimal("200000.00"))
                .payDay(25)
                .payPeriod(PayPeriod.MONTHLY)
                .payType(PayType.TRANSFER)
                .isEoiApplicable(true)
                .isWciApplicable(true)
                .isNpsApplicable(true)
                .isNhiApplicable(true)
                .build();

        return CreateContractRequest.builder()
                .employeeUserId(employeeUserId)
                .empType(EmpType.PERMANENT)
                .role("현장 관리자")
                .employeeStartDate(LocalDate.of(2024, 1, 1))
                .employeeEndDate(LocalDate.of(2024, 12, 31))
                .details(details)
                .build();
    }

    private User createUser(Long id, String userId) {
        User user = User.builder()
                .userId(userId)
                .phone("010-0000-0000")
                .build();

        try {
            java.lang.reflect.Field idField = User.class.getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, id);
        } catch (Exception e) {
            // ID 설정 실패 시 무시
        }

        return user;
    }

    /**
     * @deprecated API 변경으로 인해 비활성화된 테스트를 위한 임시 helper 메서드
     */
    @Deprecated
    private CreateContractRequest createContractRequest(Long employeeId, Long corporationId, Long managerId) {
        // 임시로 빈 request 반환
        return createContractRequest("temp_user_" + employeeId);
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
        // Contract 엔티티에 empType 추가
        Contract contract = Contract.builder()
                .employeeId(employeeId)
                .corporationId(corporationId)
                .managerId(managerId)
                .empType(empType)
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

    // ========== PDF 자동 생성 기능 테스트 ==========

    @Test
    @DisplayName("계약 생성 시 PDF 자동 생성 성공")
    void createContract_AutoGeneratePdf_Success() {
        // given
        Long siteId = 1L;
        Long userId = 700L;
        Long employeeId = 1L;
        Long corporationId = 1L;
        Long managerId = 1L;

        User user = createUser(userId, "pdf_success_employee");
        Manager manager = createManager(managerId, "김관리");
        Corporation corporation = createCorporation(corporationId, "테스트회사", "서울시 강남구");
        Site site = createSiteWithCorporation(siteId, "테스트현장", manager, corporation);
        Employee employee = createEmployee(employeeId, "홍길동", null);

        CreateContractRequest request = createContractRequest("pdf_success_employee");

        Contract savedContract = Contract.builder()
                .employeeId(employeeId)
                .corporationId(corporationId)
                .empType(EmpType.PERMANENT)
                .contractState(ContractState.DRAFT)
                .build();
        try {
            java.lang.reflect.Field idField = Contract.class.getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(savedContract, 999L);
        } catch (Exception e) {
            // ID 설정 실패 시 무시
        }

        String expectedPdfUrl = "https://bucket.s3.amazonaws.com/contracts/999/v1.pdf";

        given(siteRepository.findById(siteId)).willReturn(Optional.of(site));
        given(userRepository.findByUserId("pdf_success_employee")).willReturn(Optional.of(user));
        given(employeeRepository.findByUserId(userId)).willReturn(Optional.of(employee));
        given(contractRepository.findByEmployeeIdAndContractState(employeeId, ContractState.FULLY_SIGNED))
                .willReturn(Collections.emptyList());
        given(contractRepository.save(any(Contract.class))).willReturn(savedContract);
        given(contractDetailRepository.save(any(ContractDetail.class))).willReturn(any());

        given(contractSignatureService.generateInitialPdf(999L)).willAnswer(invocation -> {
            savedContract.transitionToManagerSigningPending();
            return expectedPdfUrl;
        });

        // when
        CreateContractResponse response = contractService.createContract(siteId, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContractId()).isEqualTo(999L);
        assertThat(response.getPdfUrl()).isEqualTo(expectedPdfUrl);
        assertThat(response.getContractState()).isEqualTo(ContractState.MANAGER_SIGNING_PENDING);

        // PDF 생성 메서드가 호출되었는지 검증
        verify(contractSignatureService).generateInitialPdf(999L);

        // Repository 저장 검증
        verify(contractRepository).save(any(Contract.class));
        verify(contractDetailRepository).save(any(ContractDetail.class));
    }

    @Test
    @DisplayName("계약 생성 시 PDF 생성 실패 시 트랜잭션 롤백")
    void createContract_AutoGeneratePdf_Failure_Rollback() {
        // given
        Long siteId = 1L;
        Long userId = 800L;
        Long employeeId = 1L;
        Long corporationId = 1L;
        Long managerId = 1L;

        User user = createUser(userId, "pdf_failure_employee");
        Manager manager = createManager(managerId, "김관리");
        Corporation corporation = createCorporation(corporationId, "테스트회사", "서울시 강남구");
        Site site = createSiteWithCorporation(siteId, "테스트현장", manager, corporation);
        Employee employee = createEmployee(employeeId, "홍길동", null);

        CreateContractRequest request = createContractRequest("pdf_failure_employee");

        Contract savedContract = Contract.builder()
                .employeeId(employeeId)
                .corporationId(corporationId)
                .empType(EmpType.PERMANENT)
                .contractState(ContractState.DRAFT)
                .build();
        try {
            java.lang.reflect.Field idField = Contract.class.getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(savedContract, 888L);
        } catch (Exception e) {
            // ID 설정 실패 시 무시
        }

        given(siteRepository.findById(siteId)).willReturn(Optional.of(site));
        given(userRepository.findByUserId("pdf_failure_employee")).willReturn(Optional.of(user));
        given(employeeRepository.findByUserId(userId)).willReturn(Optional.of(employee));
        given(contractRepository.findByEmployeeIdAndContractState(employeeId, ContractState.FULLY_SIGNED))
                .willReturn(Collections.emptyList());
        given(contractRepository.save(any(Contract.class))).willReturn(savedContract);
        given(contractDetailRepository.save(any(ContractDetail.class))).willReturn(any());

        // PDF 생성 실패 시뮬레이션
        given(contractSignatureService.generateInitialPdf(888L))
                .willThrow(new RuntimeException("PDF generation failed"));

        // when & then
        assertThatThrownBy(() -> contractService.createContract(siteId, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("PDF generation failed");

        // PDF 생성이 시도되었는지 확인
        verify(contractSignatureService).generateInitialPdf(888L);
    }
}
