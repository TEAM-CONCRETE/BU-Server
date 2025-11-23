package com.concrete.buildup.domain.workreport.repository;

import com.concrete.buildup.domain.workreport.entity.WorkReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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

    /**
     * 현장별 작업일보가 있는 날짜 목록 조회 (중복 제거)
     */
    @Query("SELECT DISTINCT CAST(w.createdAt AS LocalDate) FROM WorkReport w " +
            "WHERE w.site.id = :siteId AND w.isDeleted = false " +
            "ORDER BY CAST(w.createdAt AS LocalDate) DESC")
    List<LocalDate> findDistinctDatesBySiteId(@Param("siteId") Long siteId);

    /**
     * 현장별, 날짜별 작업일보 조회 (가장 최근 것 1개)
     *
     * @param siteId 현장 ID
     * @param date 조회 날짜
     * @return 해당 날짜의 가장 최근 작업일보 (없으면 Optional.empty())
     */
    @Query("SELECT w FROM WorkReport w " +
            "WHERE w.site.id = :siteId " +
            "AND CAST(w.createdAt AS LocalDate) = :date " +
            "AND w.isDeleted = false " +
            "ORDER BY w.createdAt DESC " +
            "LIMIT 1")
    Optional<WorkReport> findLatestBySiteIdAndDate(@Param("siteId") Long siteId, @Param("date") LocalDate date);
}
