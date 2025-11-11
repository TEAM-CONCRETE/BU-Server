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
     */
    public void updateState(AttendanceState newState, String reason) {
        this.state = newState;
        if (reason != null) {
            this.failureReason = reason;
        }
    }

    /**
     * 관리자 승인
     */
    public void approve() {
        this.state = AttendanceState.CONFIRMED;
        this.failureReason = null;
    }

    /**
     * 관리자 거부
     */
    public void reject(String reason) {
        this.state = AttendanceState.REJECTED;
        this.failureReason = reason;
    }
}
