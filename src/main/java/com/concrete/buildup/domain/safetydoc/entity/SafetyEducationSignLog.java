package com.concrete.buildup.domain.safetydoc.entity;

import com.concrete.buildup.domain.contract.enums.SignerRole;
import com.concrete.buildup.domain.contract.enums.VerificationStatus;
import com.concrete.buildup.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 안전교육일지 서명 로그 엔티티
 */
@Entity
@Table(name = "safety_education_sign_logs", indexes = {
        @Index(name = "idx_safety_sign_log_id", columnList = "safety_education_log_id"),
        @Index(name = "idx_safety_sign_signer_id", columnList = "signer_id"),
        @Index(name = "idx_safety_sign_verification", columnList = "verification_status")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SafetyEducationSignLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "safety_education_log_id", nullable = false)
    private SafetyEducationLog safetyEducationLog;

    @Enumerated(EnumType.STRING)
    @Column(name = "signer_role", nullable = false, length = 30)
    private SignerRole signerRole;

    @Column(name = "signer_id", nullable = false)
    private Long signerId;

    @Column(name = "signer_name", nullable = false, length = 50)
    private String signerName;

    @Column(name = "signature_image_url", length = 500)
    private String signatureImageUrl;

    @Column(name = "signature_hash", length = 255)
    private String signatureHash;

    @Column(name = "signature_x", precision = 10, scale = 2)
    private BigDecimal signatureX;

    @Column(name = "signature_y", precision = 10, scale = 2)
    private BigDecimal signatureY;

    @Column(name = "signature_width", precision = 10, scale = 2)
    private BigDecimal signatureWidth;

    @Column(name = "signature_height", precision = 10, scale = 2)
    private BigDecimal signatureHeight;

    @Column(name = "signed_ip", length = 45)
    private String signedIp;

    @Column(name = "signed_device", length = 500)
    private String signedDevice;

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", length = 20)
    @Builder.Default
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    // 도메인 로직

    public void verify() {
        this.verificationStatus = VerificationStatus.VERIFIED;
        this.verifiedAt = LocalDateTime.now();
    }

    public boolean isVerified() {
        return this.verificationStatus == VerificationStatus.VERIFIED;
    }
}
