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
}
