package com.concrete.buildup.domain.attendance.repository;

import com.concrete.buildup.domain.attendance.entity.Attendance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * 근태 Repository
 *
 * 근로자의 일일 근태 정보 조회 및 관리
 */
@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    /**
     * 현장의 특정 기간 근태 기록 조회 (상용직/일용직 구분, 페이징)
     *
     * @param siteId 현장 ID
     * @param empType 근로자 유형 (PERMANENT/DAILY)
     * @param startDate 시작일
     * @param endDate 종료일
     * @param pageable 페이징 정보
     * @return 근태 기록 페이지
     */
    Page<Attendance> findBySiteIdAndEmpTypeAndSearchDateBetween(
        Long siteId,
        String empType,
        LocalDate startDate,
        LocalDate endDate,
        Pageable pageable
    );

    /**
     * 현장의 특정 날짜 근태 기록 조회 (상용직/일용직 구분, 페이징)
     *
     * @param siteId 현장 ID
     * @param empType 근로자 유형
     * @param searchDate 조회 날짜
     * @param pageable 페이징 정보
     * @return 근태 기록 페이지
     */
    Page<Attendance> findBySiteIdAndEmpTypeAndSearchDate(
        Long siteId,
        String empType,
        LocalDate searchDate,
        Pageable pageable
    );

    /**
     * 현장의 특정 기간 근태 상태별 개수 조회
     *
     * @param siteId 현장 ID
     * @param empType 근로자 유형
     * @param startDate 시작일
     * @param endDate 종료일
     * @param attendanceStatus 근태 상태
     * @return 해당 상태의 근태 기록 수
     */
    Long countBySiteIdAndEmpTypeAndSearchDateBetweenAndAttendanceStatus(
        Long siteId,
        String empType,
        LocalDate startDate,
        LocalDate endDate,
        String attendanceStatus
    );

    /**
     * 특정 사원의 기간별 근태 기록 조회
     *
     * @param employeeId 사원 ID
     * @param startDate 시작일
     * @param endDate 종료일
     * @return 근태 기록 리스트
     */
    List<Attendance> findByEmployeeIdAndSearchDateBetween(
        Long employeeId,
        LocalDate startDate,
        LocalDate endDate
    );

    /**
     * 특정 사원의 특정 날짜 근태 기록 조회
     *
     * @param employeeId 사원 ID
     * @param searchDate 조회 날짜
     * @return 근태 기록 리스트
     */
    List<Attendance> findByEmployeeIdAndSearchDate(Long employeeId, LocalDate searchDate);

    /**
     * 급여 계산용 정상 출근 기록 조회
     *
     * @param employeeId 사원 ID
     * @param startDate 시작일
     * @param endDate 종료일
     * @return 정상 출근 기록 리스트
     */
    default List<Attendance> findNormalAttendancesForPayroll(Long employeeId, LocalDate startDate, LocalDate endDate) {
        return findByEmployeeIdAndSearchDateBetween(employeeId, startDate, endDate);
    }
}
