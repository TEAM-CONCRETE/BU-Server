package com.concrete.buildup.domain.safetydoc.repository;

import com.concrete.buildup.domain.safetydoc.entity.SafetyEducationLog;
import com.concrete.buildup.domain.safetydoc.enums.SafetyEducationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SafetyEducationLogRepository extends JpaRepository<SafetyEducationLog, Long> {

    List<SafetyEducationLog> findBySiteIdAndIsDeletedFalseOrderByCreatedAtDesc(Long siteId);

    List<SafetyEducationLog> findBySiteIdAndStatusAndIsDeletedFalse(Long siteId, SafetyEducationStatus status);

    @Query("SELECT s FROM SafetyEducationLog s " +
            "LEFT JOIN FETCH s.attendees " +
            "WHERE s.id = :logId AND s.isDeleted = false")
    Optional<SafetyEducationLog> findByIdWithAttendees(@Param("logId") Long logId);

    @Query("SELECT s FROM SafetyEducationLog s " +
            "WHERE s.site.id = :siteId " +
            "AND s.createdAt BETWEEN :startDate AND :endDate " +
            "AND s.isDeleted = false " +
            "ORDER BY s.createdAt DESC")
    List<SafetyEducationLog> findBySiteIdAndDateRange(
            @Param("siteId") Long siteId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    long countBySiteIdAndCreatedAtBetweenAndIsDeletedFalse(
            Long siteId,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );

    /**
     * 현장별 안전교육일지가 있는 날짜 목록 조회 (중복 제거)
     */
    @Query("SELECT DISTINCT CAST(s.createdAt AS LocalDate) FROM SafetyEducationLog s " +
            "WHERE s.site.id = :siteId AND s.isDeleted = false " +
            "ORDER BY CAST(s.createdAt AS LocalDate) DESC")
    List<LocalDate> findDistinctDatesBySiteId(@Param("siteId") Long siteId);

    /**
     * 현장별 안전교육일지가 있는 날짜 목록 조회 (연도/월 필터링, 중복 제거)
     */
    @Query("SELECT DISTINCT CAST(s.createdAt AS LocalDate) FROM SafetyEducationLog s " +
            "WHERE s.site.id = :siteId " +
            "AND s.createdAt >= :startDate " +
            "AND s.createdAt < :endDate " +
            "AND s.isDeleted = false " +
            "ORDER BY CAST(s.createdAt AS LocalDate) DESC")
    List<LocalDate> findDistinctDatesBySiteIdAndYearMonth(
            @Param("siteId") Long siteId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * 현장별, 날짜별 안전교육일지 조회 (페이지네이션 지원)
     *
     * @param siteId 현장 ID
     * @param date 조회 날짜
     * @param pageable 페이지네이션 정보
     * @return 해당 날짜의 안전교육일지 목록 (createdAt 내림차순)
     */
    @Query("SELECT s FROM SafetyEducationLog s " +
            "WHERE s.site.id = :siteId " +
            "AND CAST(s.createdAt AS LocalDate) = :date " +
            "AND s.isDeleted = false " +
            "ORDER BY s.createdAt DESC")
    List<SafetyEducationLog> findBySiteIdAndDate(
            @Param("siteId") Long siteId,
            @Param("date") LocalDate date,
            Pageable pageable);

    /**
     * 현장별 안전교육일지 목록 조회 (연도/월 필터링)
     *
     * @param siteId 현장 ID
     * @param startDate 시작 날짜 (해당 월의 1일 00:00:00)
     * @param endDate 종료 날짜 (다음 월의 1일 00:00:00)
     * @return 안전교육일지 목록 (createdAt 내림차순)
     */
    @Query("SELECT s FROM SafetyEducationLog s " +
            "WHERE s.site.id = :siteId " +
            "AND s.createdAt >= :startDate " +
            "AND s.createdAt < :endDate " +
            "AND s.isDeleted = false " +
            "ORDER BY s.createdAt DESC")
    List<SafetyEducationLog> findBySiteIdAndYearMonth(
            @Param("siteId") Long siteId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
