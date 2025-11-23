package com.concrete.buildup.domain.safetydoc.repository;

import com.concrete.buildup.domain.safetydoc.entity.SafetyEducationLog;
import com.concrete.buildup.domain.safetydoc.enums.SafetyEducationStatus;
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
     * 현장별, 날짜별 안전교육일지 조회 (가장 최근 것 1개)
     *
     * @param siteId 현장 ID
     * @param date 조회 날짜
     * @return 해당 날짜의 가장 최근 안전교육일지 (없으면 Optional.empty())
     */
    @Query("SELECT s FROM SafetyEducationLog s " +
            "WHERE s.site.id = :siteId " +
            "AND CAST(s.createdAt AS LocalDate) = :date " +
            "AND s.isDeleted = false " +
            "ORDER BY s.createdAt DESC " +
            "LIMIT 1")
    Optional<SafetyEducationLog> findLatestBySiteIdAndDate(@Param("siteId") Long siteId, @Param("date") LocalDate date);
}
