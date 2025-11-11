package com.concrete.buildup.domain.attendance.repository;

import com.concrete.buildup.domain.attendance.entity.AttendanceRecord;
import com.concrete.buildup.domain.attendance.enums.AttendanceState;
import com.concrete.buildup.domain.attendance.enums.AttendanceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 출퇴근 기록 Repository
 *
 * 출퇴근 기록의 조회, 저장, 업데이트를 담당합니다.
 */
@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    /**
     * 사원의 특정 기간 출퇴근 기록 조회
     *
     * @param employeeId 사원 ID
     * @param start 시작 일시
     * @param end 종료 일시
     * @return 출퇴근 기록 리스트
     */
    List<AttendanceRecord> findByEmployeeIdAndTimestampBetween(
        Long employeeId,
        LocalDateTime start,
        LocalDateTime end
    );

    /**
     * 현장의 특정 기간 출퇴근 기록 조회 (페이징)
     *
     * @param siteId 현장 ID
     * @param start 시작 일시
     * @param end 종료 일시
     * @param pageable 페이징 정보
     * @return 출퇴근 기록 페이지
     */
    Page<AttendanceRecord> findBySiteIdAndTimestampBetween(
        Long siteId,
        LocalDateTime start,
        LocalDateTime end,
        Pageable pageable
    );

    /**
     * 특정 사원의 특정 타입 당일 기록 조회 (중복 체크용)
     *
     * @param employeeId 사원 ID
     * @param attendanceType 출퇴근 타입
     * @param startOfDay 당일 시작 시각
     * @param endOfDay 당일 종료 시각
     * @param state 출퇴근 상태
     * @return 출퇴근 기록 (Optional)
     */
    Optional<AttendanceRecord> findFirstByEmployeeIdAndAttendanceTypeAndTimestampBetweenAndState(
        Long employeeId,
        AttendanceType attendanceType,
        LocalDateTime startOfDay,
        LocalDateTime endOfDay,
        AttendanceState state
    );

    /**
     * 사원의 최근 출퇴근 기록 조회
     *
     * @param employeeId 사원 ID
     * @return 최근 출퇴근 기록 (Optional)
     */
    Optional<AttendanceRecord> findTopByEmployeeIdOrderByTimestampDesc(Long employeeId);

    /**
     * 현장의 특정 상태 출퇴근 기록 조회 (페이징)
     *
     * @param siteId 현장 ID
     * @param state 출퇴근 상태
     * @param pageable 페이징 정보
     * @return 출퇴근 기록 페이지
     */
    Page<AttendanceRecord> findBySiteIdAndState(
        Long siteId,
        AttendanceState state,
        Pageable pageable
    );

    /**
     * 현장의 PENDING_REVIEW 상태 출퇴근 기록 개수 조회
     *
     * @param siteId 현장 ID
     * @param state 출퇴근 상태
     * @return 대기 중인 출퇴근 기록 개수
     */
    Long countBySiteIdAndState(Long siteId, AttendanceState state);
}