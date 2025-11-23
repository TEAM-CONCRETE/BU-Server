package com.concrete.buildup.domain.safetydoc.entity;

import com.concrete.buildup.domain.auth.entity.Employee;
import com.concrete.buildup.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 안전교육 참석자 엔티티
 */
@Entity
@Table(name = "safety_education_attendees", indexes = {
        @Index(name = "idx_safety_attendee_log_id", columnList = "safety_education_log_id"),
        @Index(name = "idx_safety_attendee_employee_id", columnList = "employee_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SafetyEducationAttendee extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "safety_education_log_id", nullable = false)
    @Setter(AccessLevel.PACKAGE)
    private SafetyEducationLog safetyEducationLog;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "is_signed", nullable = false)
    @Builder.Default
    private Boolean isSigned = false;

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @Column(name = "signature_image_url", length = 500)
    private String signatureImageUrl;

    // 도메인 로직

    public void sign(String signatureImageUrl) {
        this.isSigned = true;
        this.signedAt = LocalDateTime.now();
        this.signatureImageUrl = signatureImageUrl;
    }
}
