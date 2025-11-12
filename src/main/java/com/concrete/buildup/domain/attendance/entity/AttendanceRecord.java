package com.concrete.buildup.domain.attendance.entity;

import com.concrete.buildup.domain.attendance.enums.AttendanceState;
import com.concrete.buildup.domain.attendance.enums.AttendanceType;
import com.concrete.buildup.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 출퇴근 기록 엔티티
 *
 * 얼굴 인식 기반 출퇴근 기록을 저장합니다.
 * Face Similarity API 검증 결과와 메타데이터를 포함합니다.
 *
 * 테이블: attendance_records
 */
@Entity
@Table(
    name = "attendance_records",
    indexes = {
        @Index(name = "idx_employee_timestamp", columnList = "employee_id, timestamp"),
        @Index(name = "idx_site_timestamp", columnList = "site_id, timestamp"),
        @Index(name = "idx_state", columnList = "state")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AttendanceRecord extends BaseEntity {

    /**
     * 근로자 ID
     */
    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    /**
     * 출퇴근 유형 (CHECK_IN/CHECK_OUT)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_type", nullable = false, length = 30)
    private AttendanceType attendanceType;

    /**
     * 출퇴근 기록 시각
     */
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    /**
     * 촬영된 얼굴 이미지 S3 URL
     */
    @Column(name = "captured_face_image_url", nullable = false, length = 500)
    private String capturedFaceImageUrl;

    /**
     * 얼굴 유사도 점수 (0.0~1.0)
     * Face Similarity API의 cosine_similarity 값
     */
    @Column(name = "similarity_score")
    private Double similarityScore;

    /**
     * 출퇴근 기록 상태 (CONFIRMED/PENDING_REVIEW/REJECTED)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 30)
    @Builder.Default
    private AttendanceState state = AttendanceState.CONFIRMED;

    /**
     * 현장 ID
     */
    @Column(name = "site_id", nullable = false)
    private Long siteId;

    /**
     * 실패 사유 (검증 실패 시)
     * 예: "얼굴 유사도 미달 (0.72)"
     */
    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    /**
     * 출퇴근 상태 변경 (관리자 검토 후)
     *
     * @param newState 새로운 상태
     * @param reason 상태 변경 사유
     * @throws IllegalStateException 허용되지 않는 상태 전이인 경우
     */
    public void updateState(AttendanceState newState, String reason) {
        validateStateTransition(this.state, newState);
        this.state = newState;

        // CONFIRMED로 전이 시 failureReason 제거
        if (newState == AttendanceState.CONFIRMED) {
            this.failureReason = null;
        } else if (reason != null) {
            this.failureReason = reason;
        }
    }

    /**
     * 관리자 승인 (PENDING_REVIEW → CONFIRMED)
     *
     * @throws IllegalStateException PENDING_REVIEW 상태가 아닌 경우
     */
    public void approve() {
        if (this.state != AttendanceState.PENDING_REVIEW) {
            throw new IllegalStateException(
                String.format("승인할 수 없는 상태입니다. 현재 상태: %s (PENDING_REVIEW만 승인 가능)", this.state)
            );
        }
        this.state = AttendanceState.CONFIRMED;
        this.failureReason = null;
    }

    /**
     * 관리자 거부 (PENDING_REVIEW → REJECTED)
     *
     * @param reason 거부 사유
     * @throws IllegalStateException PENDING_REVIEW 상태가 아닌 경우
     * @throws IllegalArgumentException reason이 null이거나 빈 문자열인 경우
     */
    public void reject(String reason) {
        if (this.state != AttendanceState.PENDING_REVIEW) {
            throw new IllegalStateException(
                String.format("거부할 수 없는 상태입니다. 현재 상태: %s (PENDING_REVIEW만 거부 가능)", this.state)
            );
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("거부 사유는 필수입니다.");
        }
        this.state = AttendanceState.REJECTED;
        this.failureReason = reason;
    }

    /**
     * 상태 전이 검증
     *
     * @param currentState 현재 상태
     * @param newState 새로운 상태
     * @throws IllegalStateException 허용되지 않는 상태 전이인 경우
     */
    private void validateStateTransition(AttendanceState currentState, AttendanceState newState) {
        // 동일 상태로의 전이는 허용하지 않음
        if (currentState == newState) {
            throw new IllegalStateException(
                String.format("동일한 상태로 전이할 수 없습니다: %s", currentState)
            );
        }

        // 허용되는 상태 전이 규칙
        switch (currentState) {
            case CONFIRMED:
                // CONFIRMED 상태에서는 다른 상태로 전이 불가 (최종 상태)
                throw new IllegalStateException(
                    String.format("CONFIRMED 상태에서는 다른 상태로 전이할 수 없습니다. 시도한 전이: %s → %s",
                        currentState, newState)
                );

            case PENDING_REVIEW:
                // PENDING_REVIEW에서는 CONFIRMED 또는 REJECTED로만 전이 가능
                if (newState != AttendanceState.CONFIRMED && newState != AttendanceState.REJECTED) {
                    throw new IllegalStateException(
                        String.format("PENDING_REVIEW 상태에서는 CONFIRMED 또는 REJECTED로만 전이할 수 있습니다. 시도한 전이: %s → %s",
                            currentState, newState)
                    );
                }
                break;

            case REJECTED:
                // REJECTED 상태에서는 PENDING_REVIEW로만 재검토 가능
                if (newState != AttendanceState.PENDING_REVIEW) {
                    throw new IllegalStateException(
                        String.format("REJECTED 상태에서는 PENDING_REVIEW로만 전이할 수 있습니다. 시도한 전이: %s → %s",
                            currentState, newState)
                    );
                }
                break;

            default:
                throw new IllegalStateException("알 수 없는 상태입니다: " + currentState);
        }
    }
}
