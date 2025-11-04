package com.concrete.buildup.domain.contract.entity;

import com.concrete.buildup.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 전자서명 세션 엔티티
 *
 * signing_sessions 테이블과 매핑되며, 계약서별 역할별 1회성 서명 세션을 관리합니다.
 *
 * 주요 기능:
 * - 1회성 서명 세션 토큰 관리 (해시값 저장)
 * - 세션 상태 관리 (PENDING/SIGNED/CANCELED/EXPIRED)
 * - 세션 만료 시간 관리
 * - 서명 완료 시 Webhook 콜백 지원
 *
 * 보안:
 * - 세션 토큰은 해시값으로만 저장 (평문 저장 금지)
 * - 만료 시간 설정 (예: 48시간)
 * - 1회성 토큰으로 재사용 방지
 *
 * 연관 관계:
 * - Contract (N:1): 한 계약에 여러 서명 세션 (역할별)
 * - User (N:1): 한 사용자가 여러 계약의 서명 세션 가질 수 있음
 */
@Entity
@Table(name = "signing_sessions", indexes = {
    @Index(name = "idx_session_token_hash", columnList = "session_token_hash", unique = true),
    @Index(name = "idx_contract_id", columnList = "contract_id"),
    @Index(name = "idx_signer_user_id", columnList = "signer_user_id"),
    @Index(name = "idx_contract_signer_role", columnList = "contract_id, signer_role"),
    @Index(name = "idx_state", columnList = "state"),
    @Index(name = "idx_expires_at", columnList = "expires_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SigningSession extends BaseEntity {

    /**
     * 계약 ID
     * TODO: Contract 엔티티 구현 후 @ManyToOne 연관관계로 변경
     */
    @Column(name = "contract_id", nullable = false)
    private Long contractId;

    /**
     * 서명자 역할
     * MANAGER: 관리자, EMPLOYEE: 근로자
     * CORPORATION은 ContractSignLog에서만 사용 (세션은 MANAGER/EMPLOYEE만)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "signer_role", length = 30, nullable = false)
    private SignerRole signerRole;

    /**
     * 서명자 사용자 ID
     * TODO: User 엔티티 구현 후 @ManyToOne 연관관계로 변경
     * 외부 서명자(비회원)의 경우 NULL
     */
    @Column(name = "signer_user_id")
    private Long signerUserId;

    /**
     * 세션 상태
     * PENDING: 서명 대기, SIGNED: 서명 완료, CANCELED: 취소됨, EXPIRED: 만료됨
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "state", length = 20, nullable = false)
    private SessionState state;

    /**
     * 세션 토큰 해시값
     * SHA-256 등으로 해시된 토큰 (평문 저장 금지)
     * UNIQUE 제약조건
     */
    @Column(name = "session_token_hash", length = 128, nullable = false, unique = true)
    private String sessionTokenHash;

    /**
     * 콜백 URL
     * 서명 완료 시 호출할 Webhook URL (선택사항)
     */
    @Column(name = "callback_url", length = 255)
    private String callbackUrl;

    /**
     * 세션 만료 시각
     * 예: 생성 후 48시간
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /**
     * 서명자 역할 Enum
     */
    public enum SignerRole {
        /** 관리자 */
        MANAGER,
        /** 근로자 */
        EMPLOYEE
    }

    /**
     * 세션 상태 Enum
     */
    public enum SessionState {
        /** 서명 대기 */
        PENDING,
        /** 서명 완료 */
        SIGNED,
        /** 취소됨 */
        CANCELED,
        /** 만료됨 */
        EXPIRED
    }

    /**
     * SigningSession 생성자
     */
    @Builder
    public SigningSession(
        Long contractId,
        SignerRole signerRole,
        Long signerUserId,
        SessionState state,
        String sessionTokenHash,
        String callbackUrl,
        LocalDateTime expiresAt
    ) {
        this.contractId = contractId;
        this.signerRole = signerRole;
        this.signerUserId = signerUserId;
        this.state = state != null ? state : SessionState.PENDING;
        this.sessionTokenHash = sessionTokenHash;
        this.callbackUrl = callbackUrl;
        this.expiresAt = expiresAt;
    }

    /**
     * 서명 완료 처리
     */
    public void markAsSigned() {
        if (this.state != SessionState.PENDING) {
            throw new IllegalStateException("서명 대기 상태가 아닙니다. 현재 상태: " + this.state);
        }
        if (isExpired()) {
            throw new IllegalStateException("세션이 만료되었습니다.");
        }
        this.state = SessionState.SIGNED;
    }

    /**
     * 세션 취소 처리
     */
    public void cancel() {
        if (this.state == SessionState.SIGNED) {
            throw new IllegalStateException("이미 서명 완료된 세션은 취소할 수 없습니다.");
        }
        this.state = SessionState.CANCELED;
    }

    /**
     * 세션 만료 처리
     */
    public void expire() {
        if (this.state == SessionState.SIGNED) {
            throw new IllegalStateException("이미 서명 완료된 세션은 만료 처리할 수 없습니다.");
        }
        this.state = SessionState.EXPIRED;
    }

    /**
     * 세션 만료 여부 확인
     *
     * @return 현재 시각이 만료 시각을 지났으면 true
     */
    public boolean isExpired() {
        if (this.expiresAt == null) {
            return false; // 만료 시간이 설정되지 않은 경우 만료되지 않음
        }
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    /**
     * 세션 유효성 검증
     * 서명 가능한 상태인지 확인
     *
     * @return 서명 가능하면 true
     */
    public boolean isValid() {
        return this.state == SessionState.PENDING && !isExpired();
    }

    /**
     * 대기 상태 확인
     *
     * @return 서명 대기 상태면 true
     */
    public boolean isPending() {
        return this.state == SessionState.PENDING;
    }

    /**
     * 서명 완료 상태 확인
     *
     * @return 서명 완료 상태면 true
     */
    public boolean isSigned() {
        return this.state == SessionState.SIGNED;
    }

    /**
     * 취소 상태 확인
     *
     * @return 취소 상태면 true
     */
    public boolean isCanceled() {
        return this.state == SessionState.CANCELED;
    }

    /**
     * 외부 서명자 여부 확인
     *
     * @return 외부 서명자(비회원)면 true
     */
    public boolean isExternalSigner() {
        return this.signerUserId == null;
    }

    /**
     * 콜백 URL 존재 여부 확인
     *
     * @return 콜백 URL이 설정되어 있으면 true
     */
    public boolean hasCallbackUrl() {
        return this.callbackUrl != null && !this.callbackUrl.isEmpty();
    }

    /**
     * 세션 만료 시간 연장
     *
     * @param newExpiresAt 새로운 만료 시각
     */
    public void extendExpiration(LocalDateTime newExpiresAt) {
        if (this.state != SessionState.PENDING) {
            throw new IllegalStateException("대기 상태의 세션만 만료 시간을 연장할 수 있습니다.");
        }
        if (newExpiresAt.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("만료 시각은 현재 시각 이후여야 합니다.");
        }
        this.expiresAt = newExpiresAt;
    }
}
