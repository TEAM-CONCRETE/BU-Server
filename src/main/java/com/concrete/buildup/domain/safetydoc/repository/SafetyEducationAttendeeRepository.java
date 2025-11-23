package com.concrete.buildup.domain.safetydoc.repository;

import com.concrete.buildup.domain.safetydoc.entity.SafetyEducationAttendee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SafetyEducationAttendeeRepository extends JpaRepository<SafetyEducationAttendee, Long> {

    List<SafetyEducationAttendee> findBySafetyEducationLogIdAndIsDeletedFalse(Long logId);

    @Query("SELECT a FROM SafetyEducationAttendee a " +
            "JOIN FETCH a.employee " +
            "WHERE a.safetyEducationLog.id = :logId AND a.isDeleted = false")
    List<SafetyEducationAttendee> findBySafetyEducationLogIdWithEmployee(@Param("logId") Long logId);

    Optional<SafetyEducationAttendee> findBySafetyEducationLogIdAndEmployeeIdAndIsDeletedFalse(
            Long logId, Long employeeId
    );

    @Query("SELECT COUNT(a) FROM SafetyEducationAttendee a " +
            "WHERE a.safetyEducationLog.id = :logId " +
            "AND a.isSigned = true AND a.isDeleted = false")
    long countSignedAttendees(@Param("logId") Long logId);

    @Query("SELECT COUNT(a) FROM SafetyEducationAttendee a " +
            "WHERE a.safetyEducationLog.id = :logId AND a.isDeleted = false")
    long countTotalAttendees(@Param("logId") Long logId);

    /**
     * 오늘 완료된 안전교육에서 서명한 참석자의 employeeId 목록 조회
     * N+1 문제 해결을 위한 배치 조회
     */
    @Query("SELECT DISTINCT a.employee.id FROM SafetyEducationAttendee a " +
            "WHERE a.safetyEducationLog.site.id = :siteId " +
            "AND a.safetyEducationLog.status = 'COMPLETED' " +
            "AND a.safetyEducationLog.createdAt BETWEEN :startDate AND :endDate " +
            "AND a.isSigned = true " +
            "AND a.isDeleted = false " +
            "AND a.safetyEducationLog.isDeleted = false")
    List<Long> findSignedEmployeeIdsBySiteIdAndDateRange(
            @Param("siteId") Long siteId,
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate
    );
}
