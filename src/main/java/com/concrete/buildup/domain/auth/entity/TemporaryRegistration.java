package com.concrete.buildup.domain.auth.entity;

import com.concrete.buildup.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

/**
 * 2단계 회원가입 임시 저장 엔티티
 *
 * <p>1단계 회원가입 정보를 임시 저장하고, 2단계 완료 후 삭제됩니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Entity
@Table(name = "temporary_registrations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Comment("2단계 회원가입 임시 저장")
public class TemporaryRegistration extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("임시 등록 ID")
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    @Comment("사용자 ID (1단계)")
    private String userId;

    @Column(nullable = false, length = 255)
    @Comment("암호화된 비밀번호 (BCrypt)")
    private String passwordHash;

    @Column(nullable = false, length = 50)
    @Comment("근로자 이름 (1단계)")
    private String empName;

    @Column(length = 100)
    @Comment("시크릿키 (1단계, nullable)")
    private String secretKey;

    @Column(nullable = false, unique = true, length = 36)
    @Comment("등록 토큰 (UUID, 2단계 인증용)")
    private String registrationToken;

    @Column(nullable = false)
    @Comment("만료 시간 (기본 30분)")
    private LocalDateTime expiresAt;

    /**
     * 등록 토큰이 만료되었는지 확인
     *
     * @return true if expired, false otherwise
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * 만료 시간 연장 (30분)
     */
    public void extendExpiration() {
        this.expiresAt = LocalDateTime.now().plusMinutes(30);
    }
}
