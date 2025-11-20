package com.concrete.buildup.domain.employee.repository;

import com.concrete.buildup.domain.contract.enums.EmpType;
import com.concrete.buildup.domain.employee.dto.EmployeeListResponseDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
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
     * <p>Native Query를 사용하여 @Convert 우회</p>
     *
     * @param siteId 현장 ID
     * @param empType 근로자 유형 필터 (nullable)
     * @param name 이름 검색어 (nullable)
     * @param pageable 페이징 정보
     * @return 사원 목록 (페이징)
     */
    @SuppressWarnings("unchecked")
    public Page<EmployeeListResponseDto> findBySiteId(
            Long siteId,
            EmpType empType,
            String name,
            Pageable pageable
    ) {
        // Native Query 생성
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT e.id, e.emp_name, e.resident_num, c.emp_type ")
           .append("FROM employees e ")
           .append("JOIN contracts c ON c.employee_id = e.id ")
           .append("JOIN sites s ON s.manager_id = c.manager_id ")
           .append("WHERE s.id = :siteId ")
           .append("AND c.contract_state = 'FULLY_SIGNED' ");

        // 동적 조건 추가
        if (empType != null) {
            sql.append("AND c.emp_type = :empType ");
        }
        if (name != null && !name.isBlank()) {
            sql.append("AND e.emp_name LIKE :name ");
        }

        sql.append("GROUP BY e.id, e.emp_name, e.resident_num, c.emp_type ")
           .append("ORDER BY e.emp_name ASC");

        // 데이터 조회 쿼리
        Query query = em.createNativeQuery(sql.toString());
        query.setParameter("siteId", siteId);

        if (empType != null) {
            query.setParameter("empType", empType.name());
        }
        if (name != null && !name.isBlank()) {
            query.setParameter("name", "%" + name + "%");
        }

        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        // Object[] → DTO 변환
        List<Object[]> results = query.getResultList();
        List<EmployeeListResponseDto> content = results.stream()
                .map(row -> new EmployeeListResponseDto(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        (String) row[2],
                        row[3] != null ? EmpType.valueOf((String) row[3]) : null
                ))
                .toList();

        // Count 쿼리
        long total = countBySiteId(siteId, empType, name);

        return new PageImpl<>(content, pageable, total);
    }

    /**
     * 전체 개수 조회
     */
    private long countBySiteId(Long siteId, EmpType empType, String name) {
        StringBuilder countSql = new StringBuilder();
        countSql.append("SELECT COUNT(DISTINCT e.id) ")
                .append("FROM employees e ")
                .append("JOIN contracts c ON c.employee_id = e.id ")
                .append("JOIN sites s ON s.manager_id = c.manager_id ")
                .append("WHERE s.id = :siteId ")
                .append("AND c.contract_state = 'FULLY_SIGNED' ");

        if (empType != null) {
            countSql.append("AND c.emp_type = :empType ");
        }
        if (name != null && !name.isBlank()) {
            countSql.append("AND e.emp_name LIKE :name ");
        }

        Query countQuery = em.createNativeQuery(countSql.toString());
        countQuery.setParameter("siteId", siteId);

        if (empType != null) {
            countQuery.setParameter("empType", empType.name());
        }
        if (name != null && !name.isBlank()) {
            countQuery.setParameter("name", "%" + name + "%");
        }

        return ((Number) countQuery.getSingleResult()).longValue();
    }
}