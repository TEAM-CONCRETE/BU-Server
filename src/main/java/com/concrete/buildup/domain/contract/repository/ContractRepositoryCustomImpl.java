package com.concrete.buildup.domain.contract.repository;

import com.concrete.buildup.domain.contract.entity.Contract;
import com.concrete.buildup.domain.contract.enums.ContractState;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

import static com.concrete.buildup.domain.contract.entity.QContract.contract;

/**
 * Contract Custom Repository 구현체
 *
 * <p>QueryDSL을 사용한 동적 쿼리 및 복잡한 조회 기능을 구현합니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Repository
@RequiredArgsConstructor
public class ContractRepositoryCustomImpl implements ContractRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * 동적 검색 조건으로 계약 목록 조회
     *
     * @param employeeId 근로자 ID (nullable)
     * @param corporationId 기업 ID (nullable)
     * @param contractState 계약 상태 (nullable)
     * @param startDate 근로 시작일 (이후) (nullable)
     * @param endDate 근로 종료일 (이전) (nullable)
     * @param pageable 페이징 정보
     * @return 계약 목록 (페이징)
     */
    @Override
    public Page<Contract> searchContracts(
            Long employeeId,
            Long corporationId,
            ContractState contractState,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    ) {
        // 동적 쿼리 생성
        List<Contract> content = queryFactory
                .selectFrom(contract)
                .where(
                        employeeIdEq(employeeId),
                        corporationIdEq(corporationId),
                        contractStateEq(contractState),
                        employeeStartDateGoe(startDate),
                        employeeEndDateLoe(endDate)
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // count 쿼리 (최적화)
        JPAQuery<Long> countQuery = queryFactory
                .select(contract.count())
                .from(contract)
                .where(
                        employeeIdEq(employeeId),
                        corporationIdEq(corporationId),
                        contractStateEq(contractState),
                        employeeStartDateGoe(startDate),
                        employeeEndDateLoe(endDate)
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    /**
     * 특정 기간 동안 활성화된 계약 목록 조회
     *
     * @param startDate 조회 시작일
     * @param endDate 조회 종료일
     * @return 활성 계약 목록
     */
    @Override
    public List<Contract> findActiveContractsBetween(LocalDate startDate, LocalDate endDate) {
        return queryFactory
                .selectFrom(contract)
                .where(
                        contract.employeeStartDate.loe(endDate),
                        contract.employeeEndDate.goe(startDate)
                                .or(contract.employeeEndDate.isNull())
                )
                .fetch();
    }

    // ========== 동적 조건 헬퍼 메서드 ==========

    /**
     * 근로자 ID 동적 조건
     */
    private BooleanExpression employeeIdEq(Long employeeId) {
        return employeeId != null ? contract.employeeId.eq(employeeId) : null;
    }

    /**
     * 기업 ID 동적 조건
     */
    private BooleanExpression corporationIdEq(Long corporationId) {
        return corporationId != null ? contract.corporationId.eq(corporationId) : null;
    }

    /**
     * 계약 상태 동적 조건
     */
    private BooleanExpression contractStateEq(ContractState contractState) {
        return contractState != null ? contract.contractState.eq(contractState) : null;
    }

    /**
     * 근로 시작일 (이후) 동적 조건
     */
    private BooleanExpression employeeStartDateGoe(LocalDate startDate) {
        return startDate != null ? contract.employeeStartDate.goe(startDate) : null;
    }

    /**
     * 근로 종료일 (이전) 동적 조건
     */
    private BooleanExpression employeeEndDateLoe(LocalDate endDate) {
        return endDate != null ? contract.employeeEndDate.loe(endDate) : null;
    }
}
