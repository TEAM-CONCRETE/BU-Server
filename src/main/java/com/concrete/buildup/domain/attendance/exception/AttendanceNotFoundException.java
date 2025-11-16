package com.concrete.buildup.domain.attendance.exception;

/**
 * 출퇴근 기록을 찾을 수 없는 예외
 *
 * <p>요청한 출퇴근 기록이 데이터베이스에 존재하지 않을 때 발생합니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
public class AttendanceNotFoundException extends RuntimeException {

    /**
     * 출퇴근 기록을 찾을 수 없을 때 발생하는 예외
     *
     * @param attendanceId 찾으려던 출퇴근 기록 ID
     */
    public AttendanceNotFoundException(Long attendanceId) {
        super(String.format("출퇴근 기록을 찾을 수 없습니다. (ID: %d)", attendanceId));
    }

    /**
     * 출퇴근 기록을 찾을 수 없을 때 발생하는 예외 (상세 메시지)
     *
     * @param message 상세 메시지
     */
    public AttendanceNotFoundException(String message) {
        super(message);
    }
}
