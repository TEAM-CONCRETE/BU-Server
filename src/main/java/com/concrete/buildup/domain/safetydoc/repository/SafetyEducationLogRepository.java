package com.concrete.buildup.domain.safetydoc.repository;

import com.concrete.buildup.domain.safetydoc.entity.SafetyEducationLog;
import com.concrete.buildup.domain.safetydoc.enums.SafetyEducationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
