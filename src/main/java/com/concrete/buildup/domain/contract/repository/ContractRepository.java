package com.concrete.buildup.domain.contract.repository;

import com.concrete.buildup.domain.contract.entity.Contract;
import com.concrete.buildup.domain.contract.enums.ContractState;
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
 * @author Build-Up Team
 * @since 1.0
 */
@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {

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
     * 관리자 ID로 계약 목록 조회
     *
     * @param managerId 관리자 ID
     * @return 계약 목록
     */
    List<Contract> findByManagerId(Long managerId);

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
     * @param employeeId 근로자 ID
     * @param date 확인할 날짜
     * @return 활성 계약 목록
     */
    List<Contract> findByEmployeeIdAndEmployeeStartDateLessThanEqualAndEmployeeEndDateGreaterThanEqualOrEmployeeEndDateIsNull(
            Long employeeId, LocalDate date, LocalDate date2);

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
}
