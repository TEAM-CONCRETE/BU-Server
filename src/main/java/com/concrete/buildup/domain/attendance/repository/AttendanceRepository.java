package com.concrete.buildup.domain.attendance.repository;

import com.concrete.buildup.domain.attendance.entity.Attendance;
import com.concrete.buildup.domain.attendance.enums.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 근태 기록 Repository
 *
 * TODO: 담당자가 추가 쿼리 메서드 구현 필요
 * - 현장별 근태 조회
 * - 통계성 쿼리
 * - 복잡한 조건 검색
 */
@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    /**
     * 근로자 ID와 날짜로 근태 조회
     *
     * @param employeeId 근로자 ID
     * @param searchDate 근무일자
     * @return 근태 기록
     */
    Optional<Attendance> findByEmployeeIdAndSearchDate(Long employeeId, LocalDate searchDate);

    /**
     * 근로자 ID와 기간으로 근태 목록 조회
     *
     * @param employeeId 근로자 ID
     * @param startDate 시작일
     * @param endDate 종료일
     * @return 근태 목록
     */
    List<Attendance> findByEmployeeIdAndSearchDateBetween(
            Long employeeId,
            LocalDate startDate,
            LocalDate endDate
    );

    /**
     * 계약 ID와 기간으로 근태 목록 조회
     *
     * @param contractId 계약 ID
     * @param startDate 시작일
     * @param endDate 종료일
     * @return 근태 목록
     */
    List<Attendance> findByContractIdAndSearchDateBetween(
            Long contractId,
            LocalDate startDate,
            LocalDate endDate
    );

    /**
     * 현장 ID와 날짜로 근태 목록 조회
     *
     * @param siteId 현장 ID
     * @param searchDate 근무일자
     * @return 근태 목록
     */
    List<Attendance> findBySiteIdAndSearchDate(Long siteId, LocalDate searchDate);

    /**
     * 현장 ID와 기간으로 근태 목록 조회
     *
     * @param siteId 현장 ID
     * @param startDate 시작일
     * @param endDate 종료일
     * @return 근태 목록
     */
    List<Attendance> findBySiteIdAndSearchDateBetween(
            Long siteId,
            LocalDate startDate,
            LocalDate endDate
    );

    /**
     * 근로자 ID, 기간, 출근 상태로 근태 목록 조회
     *
     * @param employeeId 근로자 ID
     * @param startDate 시작일
     * @param endDate 종료일
     * @param status 출근 상태
     * @return 근태 목록
     */
    List<Attendance> findByEmployeeIdAndSearchDateBetweenAndAttendanceStatus(
            Long employeeId,
            LocalDate startDate,
            LocalDate endDate,
            AttendanceStatus status
    );

    /**
     * 급여 계산용: 근로자의 기간별 총 근무시간 합계
     * 정상 출근(NORMAL) 기록만 집계
     *
     * @param employeeId 근로자 ID
     * @param startDate 시작일
     * @param endDate 종료일
     * @return 근태 목록 (정상 출근만)
     */
    @Query("SELECT a FROM Attendance a WHERE a.employeeId = :employeeId " +
           "AND a.searchDate BETWEEN :startDate AND :endDate " +
           "AND a.attendanceStatus = 'NORMAL' " +
           "AND a.checkOutTime IS NOT NULL " +
           "ORDER BY a.searchDate ASC")
    List<Attendance> findNormalAttendancesForPayroll(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 계약별 기간 내 정상 출근 기록 조회
     *
     * @param contractId 계약 ID
     * @param startDate 시작일
     * @param endDate 종료일
     * @return 근태 목록
     */
    @Query("SELECT a FROM Attendance a WHERE a.contractId = :contractId " +
           "AND a.searchDate BETWEEN :startDate AND :endDate " +
           "AND a.attendanceStatus = 'NORMAL' " +
           "AND a.checkOutTime IS NOT NULL " +
           "ORDER BY a.searchDate ASC")
    List<Attendance> findNormalAttendancesByContract(
            @Param("contractId") Long contractId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}