package com.concrete.buildup.domain.workreport.repository;

import com.concrete.buildup.domain.workreport.entity.WorkReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
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
     * 현장별 작업일보 목록 조회
     *
     * @param siteId 현장 ID
     * @return 작업일보 목록
     */
    List<WorkReport> findBySiteIdAndIsDeletedFalseOrderByWorkDateDesc(Long siteId);

    /**
     * 현장의 특정 날짜 작업일보 조회
     *
     * @param siteId 현장 ID
     * @param workDate 작업일자
     * @return 작업일보 목록
     */
    List<WorkReport> findBySiteIdAndWorkDateAndIsDeletedFalse(Long siteId, LocalDate workDate);

    /**
     * 관리자가 작성한 작업일보 목록 조회
     *
     * @param managerId 관리자 ID
     * @return 작업일보 목록
     */
    List<WorkReport> findByManagerIdAndIsDeletedFalseOrderByWorkDateDesc(Long managerId);
}
