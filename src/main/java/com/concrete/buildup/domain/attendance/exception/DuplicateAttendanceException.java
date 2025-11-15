package com.concrete.buildup.domain.attendance.exception;

import com.concrete.buildup.domain.attendance.enums.AttendanceType;

/**
 * 중복 출퇴근 기록 예외
 *
 * <p>같은 날짜에 동일한 유형(출근/퇴근)의 출퇴근 기록이 이미 존재하는 경우 발생합니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
public class DuplicateAttendanceException extends RuntimeException {

    /**
     * 중복 출퇴근 예외 생성
     *
     * @param attendanceType 중복된 출퇴근 유형
     */
    public DuplicateAttendanceException(AttendanceType attendanceType) {
        super(String.format("이미 %s 기록이 존재합니다.", attendanceType.getDescription()));
    }

    /**
     * 중복 출퇴근 예외 생성 (상세 메시지)
     *
     * @param message 상세 메시지
     */
    public DuplicateAttendanceException(String message) {
        super(message);
    }
}
