package com.concrete.buildup.domain.attendance.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 출퇴근 기록의 유형을 나타내는 Enum
 *
 * <p>근로자의 출근 및 퇴근 상태를 표현합니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@RequiredArgsConstructor
public enum AttendanceType {

    /**
     * 출근 - 근로자가 현장에 도착하여 출근을 기록한 상태
     */
    CHECK_IN("출근"),

    /**
     * 퇴근 - 근로자가 현장에서 퇴근을 기록한 상태
     */
    CHECK_OUT("퇴근");

    /**
     * 출퇴근 유형에 대한 한글 설명
     */
    private final String description;
}
