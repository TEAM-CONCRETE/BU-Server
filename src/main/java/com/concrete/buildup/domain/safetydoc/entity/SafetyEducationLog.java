package com.concrete.buildup.domain.safetydoc.entity;

import com.concrete.buildup.domain.auth.entity.Manager;
import com.concrete.buildup.domain.safetydoc.enums.EducationType;
import com.concrete.buildup.domain.safetydoc.enums.SafetyEducationStatus;
import com.concrete.buildup.domain.auth.entity.Corporation;
import com.concrete.buildup.domain.site.entity.Site;
import com.concrete.buildup.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 안전교육일지 엔티티
 */
@Entity
@Table(name = "safety_education_logs", indexes = {
        @Index(name = "idx_safety_edu_site_id", columnList = "site_id"),
        @Index(name = "idx_safety_edu_manager_id", columnList = "manager_id"),
        @Index(name = "idx_safety_edu_corporation_id", columnList = "corporation_id"),
        @Index(name = "idx_safety_edu_status", columnList = "status")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SafetyEducationLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "manager_id", nullable = false)
    private Manager manager;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "corporation_id", nullable = false)
    private Corporation corporation;

    @Enumerated(EnumType.STRING)
    @Column(name = "education_type", nullable = false, length = 30)
    private EducationType educationType;

    @Column(name = "education_subject", nullable = false, length = 200)
    private String educationSubject;

    @Column(name = "education_content", nullable = false, columnDefinition = "TEXT")
    private String educationContent;

    @Column(name = "instructor_name", nullable = false, length = 50)
    private String instructorName;

    @Column(name = "education_location", nullable = false, length = 200)
    private String educationLocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private SafetyEducationStatus status = SafetyEducationStatus.DRAFT;

    @Column(name = "manager_signed_at")
    private LocalDateTime managerSignedAt;

    @Column(name = "pdf_url", length = 500)
    private String pdfUrl;

    @Column(name = "pdf_generated_at")
    private LocalDateTime pdfGeneratedAt;

    @Column(name = "final_pdf_url", length = 500)
    private String finalPdfUrl;

    @Column(name = "final_pdf_hash", length = 255)
    private String finalPdfHash;

    @OneToMany(mappedBy = "safetyEducationLog", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SafetyEducationAttendee> attendees = new ArrayList<>();

    // 도메인 로직

    public void addAttendee(SafetyEducationAttendee attendee) {
        this.attendees.add(attendee);
        attendee.setSafetyEducationLog(this);
    }

    public void transitionToManagerSigningPending() {
        this.status = SafetyEducationStatus.MANAGER_SIGNING_PENDING;
    }

    public void signByManager() {
        this.status = SafetyEducationStatus.MANAGER_SIGNED;
        this.managerSignedAt = LocalDateTime.now();
    }

    public void complete() {
        this.status = SafetyEducationStatus.COMPLETED;
    }

    public void updatePdf(String pdfUrl) {
        this.pdfUrl = pdfUrl;
        this.pdfGeneratedAt = LocalDateTime.now();
    }

    public void updateFinalPdf(String finalPdfUrl, String finalPdfHash) {
        this.finalPdfUrl = finalPdfUrl;
        this.finalPdfHash = finalPdfHash;
    }

    public boolean areAllAttendeesSigned() {
        if (attendees.isEmpty()) {
            return false;
        }
        return attendees.stream().allMatch(SafetyEducationAttendee::getIsSigned);
    }
}
