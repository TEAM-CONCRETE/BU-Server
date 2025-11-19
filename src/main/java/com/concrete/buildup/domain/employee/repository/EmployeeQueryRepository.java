package com.concrete.buildup.domain.employee.repository;

import com.concrete.buildup.domain.contract.enums.EmpType;
import com.concrete.buildup.domain.employee.dto.EmployeeListResponseDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 사원 조회용 Repository
 *
 * <p>현장 기반 사원 목록 조회를 위한 동적 쿼리를 제공합니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Repository
@RequiredArgsConstructor
public class EmployeeQueryRepository {

    private final EntityManager em;

    /**
     * 현장 ID 기반 사원 목록 조회 (페이징, 필터링)
     *
     * <p>Site → Manager → Contract → Employee 경로로 조회합니다.</p>
     *
     * @param siteId 현장 ID
     * @param empType 근로자 유형 필터 (nullable)
     * @param name 이름 검색어 (nullable)
     * @param pageable 페이징 정보
     * @return 사원 목록 (페이징)
     */
    public Page<EmployeeListResponseDto> findBySiteId(
            Long siteId,
            EmpType empType,
            String name,
            Pageable pageable
    ) {
        // 동적 JPQL 생성
        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT new com.concrete.buildup.domain.employee.dto.EmployeeListResponseDto(")
            .append("e.id, e.empName, e.residentNum, c.empType) ")
            .append("FROM Employee e ")
            .append("JOIN Contract c ON c.employeeId = e.id ")
            .append("JOIN Site s ON s.manager.id = c.managerId ")
            .append("WHERE s.id = :siteId ")
            .append("AND c.contractState = 'FULLY_SIGNED' ");

        // 동적 조건 추가
        if (empType != null) {
            jpql.append("AND c.empType = :empType ");
        }
        if (name != null && !name.isBlank()) {
            jpql.append("AND e.empName LIKE :name ");
        }

        jpql.append("GROUP BY e.id, e.empName, e.residentNum, c.empType ")
            .append("ORDER BY e.empName ASC");

        // 데이터 조회 쿼리
        TypedQuery<EmployeeListResponseDto> query = em.createQuery(jpql.toString(), EmployeeListResponseDto.class);
        query.setParameter("siteId", siteId);

        if (empType != null) {
            query.setParameter("empType", empType);
        }
        if (name != null && !name.isBlank()) {
            query.setParameter("name", "%" + name + "%");
        }

        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<EmployeeListResponseDto> content = query.getResultList();

        // Count 쿼리
        long total = countBySiteId(siteId, empType, name);

        return new PageImpl<>(content, pageable, total);
    }

    /**
     * 전체 개수 조회
     */
    private long countBySiteId(Long siteId, EmpType empType, String name) {
        StringBuilder countJpql = new StringBuilder();
        countJpql.append("SELECT COUNT(DISTINCT e.id) ")
                .append("FROM Employee e ")
                .append("JOIN Contract c ON c.employeeId = e.id ")
                .append("JOIN Site s ON s.manager.id = c.managerId ")
                .append("WHERE s.id = :siteId ")
                .append("AND c.contractState = 'FULLY_SIGNED' ");

        if (empType != null) {
            countJpql.append("AND c.empType = :empType ");
        }
        if (name != null && !name.isBlank()) {
            countJpql.append("AND e.empName LIKE :name ");
        }

        TypedQuery<Long> countQuery = em.createQuery(countJpql.toString(), Long.class);
        countQuery.setParameter("siteId", siteId);

        if (empType != null) {
            countQuery.setParameter("empType", empType);
        }
        if (name != null && !name.isBlank()) {
            countQuery.setParameter("name", "%" + name + "%");
        }

        return countQuery.getSingleResult();
    }
}