package com.concrete.buildup.domain.attendance.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 출퇴근 기록의 상태를 나타내는 Enum
 *
 * <p>얼굴 인식 검증 결과에 따른 출퇴근 기록의 승인 상태를 표현합니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@RequiredArgsConstructor
public enum AttendanceState {

    /**
     * 확인됨 - 얼굴 인식 검증 통과, 출퇴근 자동 승인
     */
    CONFIRMED("확인됨"),

    /**
     * 검토 대기 - 얼굴 인식 검증 실패, 관리자 검토 필요
     */
    PENDING_REVIEW("검토 대기"),

    /**
     * 거부됨 - 관리자가 출퇴근 기록을 거부한 상태
     */
    REJECTED("거부됨");

    /**
     * 상태에 대한 한글 설명
     */
    private final String description;
}
