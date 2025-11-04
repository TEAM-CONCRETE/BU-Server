package com.concrete.buildup.domain.contract.repository;

import com.concrete.buildup.domain.contract.entity.Contract;
import com.concrete.buildup.domain.contract.enums.ContractState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

/**
 * Contract Custom Repository
 *
 * <p>QueryDSL을 사용한 동적 쿼리 및 복잡한 조회 기능을 위한 Custom Repository 인터페이스입니다.</p>
 *
 * <p>향후 동적 검색 조건, 복잡한 집계 쿼리 등에 사용될 수 있습니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
public interface ContractRepositoryCustom {

    /**
     * 동적 검색 조건으로 계약 목록 조회 (QueryDSL)
     *
     * <p>향후 구현 예정: 여러 검색 조건을 동적으로 조합하여 조회</p>
     *
     * @param employeeId 근로자 ID (nullable)
     * @param corporationId 기업 ID (nullable)
     * @param contractState 계약 상태 (nullable)
     * @param startDate 근로 시작일 (이후) (nullable)
     * @param endDate 근로 종료일 (이전) (nullable)
     * @param pageable 페이징 정보
     * @return 계약 목록 (페이징)
     */
    Page<Contract> searchContracts(
            Long employeeId,
            Long corporationId,
            ContractState contractState,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    );

    /**
     * 특정 기간 동안 활성화된 계약 목록 조회 (QueryDSL)
     *
     * <p>근로 시작일 <= 종료날짜 AND (근로 종료일 >= 시작날짜 OR 근로 종료일 IS NULL)</p>
     *
     * @param startDate 조회 시작일
     * @param endDate 조회 종료일
     * @return 활성 계약 목록
     */
    List<Contract> findActiveContractsBetween(LocalDate startDate, LocalDate endDate);
}
