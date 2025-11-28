package com.concrete.buildup.domain.contract.entity;

import com.concrete.buildup.domain.contract.enums.SignerRole;
import com.concrete.buildup.domain.contract.enums.VerificationStatus;
import com.concrete.buildup.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 계약 서명 로그 엔티티
 *
 * contract_sign_logs 테이블과 매핑되며, 계약서별 서명 이력 및 증거 데이터를 관리합니다.
 *
 * 주요 기능:
 * - 서명자별 서명 이력 기록
 * - 전자서명 이미지 및 해시값 저장 (무결성 검증)
 * - 서명 환경 정보 기록 (IP, 디바이스)
 * - 서명 검증 상태 관리
 *
 * 연관 관계:
 * - Contract (N:1): 한 계약에 여러 서명 로그
 * - User (N:1): 한 사용자가 여러 계약에 서명 가능
 */
@Entity
@Table(name = "contract_sign_logs", indexes = {
    @Index(name = "idx_contract_id", columnList = "contract_id"),
    @Index(name = "idx_signer_id", columnList = "signer_id"),
    @Index(name = "idx_contract_signer_role", columnList = "contract_id, signer_role"),
    @Index(name = "idx_verification_status", columnList = "verification_status")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContractSignLog extends BaseEntity {

    /**
     * 계약 (N:1 단방향)
     * 서명 로그는 계약을 참조하지만, 계약에서 서명 로그 목록을 탐색할 필요는 적음
     * 필요시 Repository로 조회
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    /**
     * 서명자 역할
     * EMPLOYEE: 근로자, CORPORATION: 기업, MANAGER: 관리자
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "signer_role", length = 30)
    private SignerRole signerRole;

    /**
     * 서명자 사용자 ID
     * TODO: User 엔티티 구현 후 @ManyToOne 연관관계로 변경
     * 외부 서명자(비회원)의 경우 NULL
     */
    @Column(name = "signer_id")
    private Long signerId;

    /**
     * 서명자 이름
     * 서명 당시 이름 스냅샷
     */
    @Column(name = "signer_name", length = 50)
    private String signerName;

    /**
     * 서명 이미지 URL
     * S3 등 스토리지에 저장된 서명 이미지 경로
     */
    @Column(name = "signature_image_url", length = 255)
    private String signatureImageUrl;

    /**
     * 서명 해시값
     * SHA-256 해시값, 서명 무결성 검증용
     */
    @Column(name = "signature_hash", length = 255)
    private String signatureHash;

    /**
     * 서명 시점 IP 주소
     * IPv4: 최대 15자, IPv6: 최대 45자
     */
    @Column(name = "signed_ip", length = 45)
    private String signedIp;

    /**
     * 서명 디바이스 정보
     * User-Agent 등 디바이스 식별 정보
     */
    @Column(name = "signed_device", length = 500)
    private String signedDevice;

    /**
     * 서명 시각
     */
    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    /**
     * 서명 검증 완료 시각
     */
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    /**
     * 검증 상태
     * PENDING: 검증 대기, VERIFIED: 검증 완료, FAILED: 검증 실패
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", length = 20)
    private VerificationStatus verificationStatus;

    /**
     * 서명 X 좌표 (PDF pt)
     * PDF 내 서명 이미지의 X 좌표 위치
     */
    @Column(name = "signature_x", precision = 10, scale = 2)
    private BigDecimal signatureX;

    /**
     * 서명 Y 좌표 (PDF pt)
     * PDF 내 서명 이미지의 Y 좌표 위치
     */
    @Column(name = "signature_y", precision = 10, scale = 2)
    private BigDecimal signatureY;

    /**
     * 서명 이미지 너비 (PDF pt)
     * PDF 내 서명 이미지의 너비
     */
    @Column(name = "signature_width", precision = 10, scale = 2)
    private BigDecimal signatureWidth;

    /**
     * 서명 이미지 높이 (PDF pt)
     * PDF 내 서명 이미지의 높이
     */
    @Column(name = "signature_height", precision = 10, scale = 2)
    private BigDecimal signatureHeight;

    /**
     * ContractSignLog 생성자
     */
    @Builder
    public ContractSignLog(
        Contract contract,
        SignerRole signerRole,
        Long signerId,
        String signerName,
        String signatureImageUrl,
        String signatureHash,
        String signedIp,
        String signedDevice,
        LocalDateTime signedAt,
        LocalDateTime verifiedAt,
        VerificationStatus verificationStatus,
        BigDecimal signatureX,
        BigDecimal signatureY,
        BigDecimal signatureWidth,
        BigDecimal signatureHeight
    ) {
        this.contract = contract;
        this.signerRole = signerRole;
        this.signerId = signerId;
        this.signerName = signerName;
        this.signatureImageUrl = signatureImageUrl;
        this.signatureHash = signatureHash;
        this.signedIp = signedIp;
        this.signedDevice = signedDevice;
        this.signedAt = signedAt != null ? signedAt : LocalDateTime.now();
        this.verifiedAt = verifiedAt;
        this.verificationStatus = verificationStatus != null ? verificationStatus : VerificationStatus.PENDING;
        this.signatureX = signatureX;
        this.signatureY = signatureY;
        this.signatureWidth = signatureWidth;
        this.signatureHeight = signatureHeight;
    }

    /**
     * 서명 검증 완료 처리
     */
    public void verify() {
        this.verificationStatus = VerificationStatus.VERIFIED;
        this.verifiedAt = LocalDateTime.now();
    }

    /**
     * 서명 검증 실패 처리
     */
    public void markAsFailed() {
        this.verificationStatus = VerificationStatus.FAILED;
        this.verifiedAt = LocalDateTime.now();
    }

    /**
     * 검증 대기 상태 확인
     *
     * @return 검증 대기 중이면 true
     */
    public boolean isPending() {
        return this.verificationStatus == VerificationStatus.PENDING;
    }

    /**
     * 검증 완료 상태 확인
     *
     * @return 검증 완료되었으면 true
     */
    public boolean isVerified() {
        return this.verificationStatus == VerificationStatus.VERIFIED;
    }

    /**
     * 검증 실패 상태 확인
     *
     * @return 검증 실패했으면 true
     */
    public boolean isFailed() {
        return this.verificationStatus == VerificationStatus.FAILED;
    }

    /**
     * 외부 서명자 여부 확인
     *
     * @return 외부 서명자(비회원)면 true
     */
    public boolean isExternalSigner() {
        return this.signerId == null;
    }

    /**
     * 서명 이미지 존재 여부 확인
     *
     * @return 서명 이미지가 있으면 true
     */
    public boolean hasSignatureImage() {
        return this.signatureImageUrl != null && !this.signatureImageUrl.isEmpty();
    }

    /**
     * 서명 해시 존재 여부 확인
     *
     * @return 서명 해시가 있으면 true
     */
    public boolean hasSignatureHash() {
        return this.signatureHash != null && !this.signatureHash.isEmpty();
    }
}
