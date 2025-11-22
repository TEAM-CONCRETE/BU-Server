package com.concrete.buildup.domain.workreport.repository;

import com.concrete.buildup.domain.workreport.entity.WorkReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 작업일보 Repository
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Repository
public interface WorkReportRepository extends JpaRepository<WorkReport, Long> {

    /**
     * 현장별 작업일보 목록 조회 (작성일 기준 내림차순)
     *
     * @param siteId 현장 ID
     * @return 작업일보 목록
     */
    List<WorkReport> findBySiteIdAndIsDeletedFalseOrderByCreatedAtDesc(Long siteId);

    /**
     * 현장의 모든 작업일보 조회 (삭제되지 않은 것만)
     *
     * @param siteId 현장 ID
     * @return 작업일보 목록
     */
    List<WorkReport> findBySiteIdAndIsDeletedFalse(Long siteId);

    /**
     * 관리자가 작성한 작업일보 목록 조회 (작성일 기준 내림차순)
     *
     * @param managerId 관리자 ID
     * @return 작업일보 목록
     */
    List<WorkReport> findByManagerIdAndIsDeletedFalseOrderByCreatedAtDesc(Long managerId);
}
