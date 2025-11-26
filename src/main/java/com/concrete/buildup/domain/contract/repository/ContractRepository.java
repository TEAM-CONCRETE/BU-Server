package com.concrete.buildup.domain.contract.repository;

import com.concrete.buildup.domain.contract.entity.Contract;
import com.concrete.buildup.domain.contract.enums.ContractState;
import com.concrete.buildup.domain.contract.enums.EmpType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Contract Repository
 *
 * <p>근로계약 엔티티에 대한 데이터 접근 계층입니다.</p>
 *
 * <p>QueryDSL을 사용한 동적 쿼리는 {@link ContractRepositoryCustom}을 통해 제공됩니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Repository
public interface ContractRepository extends JpaRepository<Contract, Long>, ContractRepositoryCustom {

    /**
     * 근로자 ID로 계약 목록 조회 (페이징)
     *
     * @param employeeId 근로자 ID
     * @param pageable 페이징 정보
     * @return 계약 목록 (페이징)
     */
    Page<Contract> findByEmployeeId(Long employeeId, Pageable pageable);

    /**
     * 기업 ID로 계약 목록 조회 (페이징)
     *
     * @param corporationId 기업 ID
     * @param pageable 페이징 정보
     * @return 계약 목록 (페이징)
     */
    Page<Contract> findByCorporationId(Long corporationId, Pageable pageable);


    /**
     * 계약 상태로 계약 목록 조회 (페이징)
     *
     * @param contractState 계약 상태
     * @param pageable 페이징 정보
     * @return 계약 목록 (페이징)
     */
    Page<Contract> findByContractState(ContractState contractState, Pageable pageable);

    /**
     * 근로자 ID와 계약 상태로 계약 목록 조회
     *
     * @param employeeId 근로자 ID
     * @param contractState 계약 상태
     * @return 계약 목록
     */
    List<Contract> findByEmployeeIdAndContractState(Long employeeId, ContractState contractState);

    /**
     * 근로자 ID로 계약 목록 조회 (최신순 정렬)
     *
     * @param employeeId 근로자 ID
     * @return 계약 목록 (작성일 기준 최신순)
     */
    List<Contract> findByEmployeeIdOrderByWrittenAtDesc(Long employeeId);

    /**
     * 근로 시작일 범위로 계약 목록 조회
     *
     * @param startDate 시작일 (이상)
     * @param endDate 종료일 (이하)
     * @return 계약 목록
     */
    List<Contract> findByEmployeeStartDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * 특정 날짜에 활성화된 계약 조회
     * (근로 시작일 <= 특정날짜 AND (근로 종료일 >= 특정날짜 OR 근로 종료일 IS NULL))
     *
     * <p>메서드 네이밍으로는 OR 조건의 괄호 그룹핑이 불가능하므로 @Query 사용</p>
     *
     * @param employeeId 근로자 ID
     * @param date 확인할 날짜
     * @return 활성 계약 목록
     */
    @Query("SELECT c FROM Contract c WHERE c.employeeId = :employeeId " +
           "AND c.employeeStartDate <= :date " +
           "AND (c.employeeEndDate >= :date OR c.employeeEndDate IS NULL)")
    List<Contract> findActiveContractsByEmployeeIdAndDate(@Param("employeeId") Long employeeId,
                                                            @Param("date") LocalDate date);

    /**
     * 관리자 ID, 근로자 유형, 특정 날짜로 활성 계약 조회
     * (근로 시작일 <= 특정날짜 AND (근로 종료일 >= 특정날짜 OR 근로 종료일 IS NULL))
     *
     * @param managerId 관리자 ID
     * @param empType 근로자 유형
     * @param date 확인할 날짜
     * @return 활성 계약 목록
     */
    @Query("SELECT c FROM Contract c WHERE c.managerId = :managerId " +
           "AND c.empType = :empType " +
           "AND c.employeeStartDate <= :date " +
           "AND (c.employeeEndDate >= :date OR c.employeeEndDate IS NULL)")
    List<Contract> findActiveContractsByManagerIdAndEmpTypeAndDate(
            @Param("managerId") Long managerId,
            @Param("empType") EmpType empType,
            @Param("date") LocalDate date);

    // ========== Fetch Join 쿼리 (N+1 문제 방지) ==========

    /**
     * 계약 ID로 계약 + 상세 정보 조회 (Fetch Join)
     * N+1 문제 방지를 위해 ContractDetail을 함께 조회
     *
     * @param contractId 계약 ID
     * @return 계약 + 상세 정보 (Optional)
     */
    @Query("SELECT c FROM Contract c LEFT JOIN FETCH c.contractDetail WHERE c.id = :contractId")
    Optional<Contract> findByIdWithDetails(@Param("contractId") Long contractId);

    /**
     * 계약 ID 목록으로 계약 + 상세 정보 일괄 조회 (Fetch Join)
     * N+1 문제 방지를 위해 ContractDetail을 함께 조회
     *
     * @param contractIds 계약 ID 목록
     * @return 계약 + 상세 정보 목록
     */
    @Query("SELECT c FROM Contract c LEFT JOIN FETCH c.contractDetail WHERE c.id IN :contractIds")
    List<Contract> findByIdInWithDetails(@Param("contractIds") List<Long> contractIds);

    /**
     * 근로자 ID로 계약 목록 + 상세 정보 조회 (Fetch Join, 페이징)
     * N+1 문제 방지를 위해 ContractDetail을 함께 조회
     *
     * @param employeeId 근로자 ID
     * @param pageable 페이징 정보
     * @return 계약 + 상세 정보 목록 (페이징)
     */
    @Query(value = "SELECT DISTINCT c FROM Contract c LEFT JOIN FETCH c.contractDetail WHERE c.employeeId = :employeeId",
           countQuery = "SELECT COUNT(c) FROM Contract c WHERE c.employeeId = :employeeId")
    Page<Contract> findByEmployeeIdWithDetails(@Param("employeeId") Long employeeId, Pageable pageable);

    /**
     * 기업 ID로 계약 목록 + 상세 정보 조회 (Fetch Join, 페이징)
     * N+1 문제 방지를 위해 ContractDetail을 함께 조회
     *
     * @param corporationId 기업 ID
     * @param pageable 페이징 정보
     * @return 계약 + 상세 정보 목록 (페이징)
     */
    @Query(value = "SELECT DISTINCT c FROM Contract c LEFT JOIN FETCH c.contractDetail WHERE c.corporationId = :corporationId",
           countQuery = "SELECT COUNT(c) FROM Contract c WHERE c.corporationId = :corporationId")
    Page<Contract> findByCorporationIdWithDetails(@Param("corporationId") Long corporationId, Pageable pageable);

    /**
     * 계약 상태로 계약 목록 + 상세 정보 조회 (Fetch Join, 페이징)
     * N+1 문제 방지를 위해 ContractDetail을 함께 조회
     *
     * @param contractState 계약 상태
     * @param pageable 페이징 정보
     * @return 계약 + 상세 정보 목록 (페이징)
     */
    @Query(value = "SELECT DISTINCT c FROM Contract c LEFT JOIN FETCH c.contractDetail WHERE c.contractState = :contractState",
           countQuery = "SELECT COUNT(c) FROM Contract c WHERE c.contractState = :contractState")
    Page<Contract> findByContractStateWithDetails(@Param("contractState") ContractState contractState, Pageable pageable);

    /**
     * 현장 ID로 계약 목록 조회 (페이징)
     *
     * <p>계약 목록 조회 API에서 사용됩니다.</p>
     * <p>Employee는 연관관계가 없으므로 Service 레이어에서 별도 조회합니다.</p>
     *
     * @param siteId 현장 ID (Contract 테이블에는 없으므로 Manager를 통해 간접 조회 필요)
     * @param pageable 페이징 정보
     * @return 계약 목록 (페이징)
     */
    Page<Contract> findByManagerId(Long managerId, Pageable pageable);

    /**
     * 관리자 ID와 근로자 ID로 계약 목록 조회 (페이징)
     *
     * @param managerId 관리자 ID
     * @param employeeId 근로자 ID
     * @param pageable 페이징 정보
     * @return 계약 목록 (페이징)
     */
    Page<Contract> findByManagerIdAndEmployeeId(Long managerId, Long employeeId, Pageable pageable);

    /**
     * 관리자 ID와 계약 상태로 계약 목록 조회 (페이징)
     *
     * @param managerId 관리자 ID
     * @param contractState 계약 상태
     * @param pageable 페이징 정보
     * @return 계약 목록 (페이징)
     */
    Page<Contract> findByManagerIdAndContractState(Long managerId, ContractState contractState, Pageable pageable);

    /**
     * 관리자 ID와 날짜 범위로 계약 목록 조회 (페이징)
     *
     * @param managerId 관리자 ID
     * @param from 시작일 (이상)
     * @param to 종료일 (이하)
     * @param pageable 페이징 정보
     * @return 계약 목록 (페이징)
     */
    Page<Contract> findByManagerIdAndEmployeeStartDateBetween(Long managerId, LocalDate from, LocalDate to, Pageable pageable);

    /**
     * 관리자 ID, 근로자 ID, 계약 상태로 계약 목록 조회 (페이징)
     *
     * @param managerId 관리자 ID
     * @param employeeId 근로자 ID
     * @param contractState 계약 상태
     * @param pageable 페이징 정보
     * @return 계약 목록 (페이징)
     */
    Page<Contract> findByManagerIdAndEmployeeIdAndContractState(Long managerId, Long employeeId, ContractState contractState, Pageable pageable);

    /**
     * 관리자 ID, 근로자 ID 목록으로 계약 목록 조회 (페이징)
     * empType 필터링을 위해 사용
     *
     * @param managerId 관리자 ID
     * @param employeeIds 근로자 ID 목록
     * @param pageable 페이징 정보
     * @return 계약 목록 (페이징)
     */
    Page<Contract> findByManagerIdAndEmployeeIdIn(Long managerId, List<Long> employeeIds, Pageable pageable);

    /**
     * 관리자 ID, 근로자 ID 목록, 계약 상태로 계약 목록 조회 (페이징)
     *
     * @param managerId 관리자 ID
     * @param employeeIds 근로자 ID 목록
     * @param contractState 계약 상태
     * @param pageable 페이징 정보
     * @return 계약 목록 (페이징)
     */
    Page<Contract> findByManagerIdAndEmployeeIdInAndContractState(Long managerId, List<Long> employeeIds, ContractState contractState, Pageable pageable);

    /**
     * 복합 조건 쿼리 - 모든 조건 조합 지원
     * JPQL을 사용하여 동적으로 조건 추가
     *
     * @param managerId 관리자 ID (필수)
     * @param employeeId 근로자 ID (nullable)
     * @param employeeIds 근로자 ID 목록 (nullable)
     * @param contractState 계약 상태 (nullable)
     * @param from 시작일 (nullable)
     * @param to 종료일 (nullable)
     * @param pageable 페이징 정보
     * @return 계약 목록 (페이징)
     */
    @Query("SELECT c FROM Contract c WHERE c.managerId = :managerId " +
           "AND (:employeeId IS NULL OR c.employeeId = :employeeId) " +
           "AND (:employeeIds IS NULL OR c.employeeId IN :employeeIds) " +
           "AND (:contractState IS NULL OR c.contractState = :contractState) " +
           "AND (:from IS NULL OR c.employeeStartDate >= :from) " +
           "AND (:to IS NULL OR c.employeeStartDate <= :to)")
    Page<Contract> findByDynamicConditions(
            @Param("managerId") Long managerId,
            @Param("employeeId") Long employeeId,
            @Param("employeeIds") List<Long> employeeIds,
            @Param("contractState") ContractState contractState,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable
    );

    // ========== 급여 생성용 쿼리 ==========

    /**
     * 상용직 급여 대상자 조회
     * 조건: empType = PERMANENT, contractState = FULLY_SIGNED,
     *      근로 기간이 대상 기간과 겹침
     *
     * @param startDate 급여 대상 기간 시작일
     * @param endDate 급여 대상 기간 종료일
     * @return 급여 대상 계약 목록
     */
    @Query("SELECT c FROM Contract c LEFT JOIN FETCH c.contractDetail " +
           "WHERE c.empType = 'PERMANENT' " +
           "AND c.contractState = 'FULLY_SIGNED' " +
           "AND c.employeeStartDate <= :endDate " +
           "AND (c.employeeEndDate >= :startDate OR c.employeeEndDate IS NULL)")
    List<Contract> findPermanentContractsForPayroll(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 일용직 급여 대상자 조회 (PayPeriod별)
     * 조건: empType = DAILY, contractState = FULLY_SIGNED,
     *      근로 기간이 대상 기간과 겹침
     *
     * @param startDate 급여 대상 기간 시작일
     * @param endDate 급여 대상 기간 종료일
     * @return 급여 대상 계약 목록
     */
    @Query("SELECT c FROM Contract c LEFT JOIN FETCH c.contractDetail cd " +
           "WHERE c.empType = 'DAILY' " +
           "AND c.contractState = 'FULLY_SIGNED' " +
           "AND c.employeeStartDate <= :endDate " +
           "AND (c.employeeEndDate >= :startDate OR c.employeeEndDate IS NULL) " +
           "AND cd.payPeriod = :payPeriod")
    List<Contract> findDailyContractsForPayrollByPeriod(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("payPeriod") com.concrete.buildup.domain.contract.enums.PayPeriod payPeriod
    );
}
