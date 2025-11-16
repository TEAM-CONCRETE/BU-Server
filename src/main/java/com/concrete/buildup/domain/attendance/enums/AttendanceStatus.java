package com.concrete.buildup.domain.attendance.enums;

/**
 * 출근 상태
 */
public enum AttendanceStatus {
    /**
     * 정상 출근
     */
    NORMAL("정상"),

    /**
     * 지각
     */
    LATE("지각"),

    /**
     * 조퇴
     */
    EARLY_LEAVE("조퇴"),

    /**
     * 결근
     */
    ABSENT("결근"),

    /**
     * 휴무
     */
    DAY_OFF("휴무");

    private final String description;

    AttendanceStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}