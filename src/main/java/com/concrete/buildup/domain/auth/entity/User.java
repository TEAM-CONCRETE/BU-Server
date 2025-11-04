package com.concrete.buildup.domain.auth.entity;

import com.concrete.buildup.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 사용자 엔티티
 *
 * 플랫폼 사용자 기본 정보 및 인증 정보
 *
 * 테이블: users
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    /**
     * 로그인용 ID
     * 중복 불가
     */
    @Column(name = "user_id", nullable = false, unique = true, length = 50)
    private String userId;

    /**
     * 비밀번호 (해시)
     */
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    /**
     * 전화번호
     */
    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    /**
     * 이메일
     */
    @Column(name = "email", length = 100)
    private String email;

    /**
     * 인증용 시크릿키
     */
    @Column(name = "secret_key", length = 100)
    private String secretKey;

    /**
     * 역할 (계약 시 할당)
     * N:1 관계
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role role;

    /**
     * 비밀번호 변경
     */
    public void updatePassword(String newPassword) {
        this.password = newPassword;
    }

    /**
     * 역할 할당
     */
    public void assignRole(Role role) {
        this.role = role;
    }

    /**
     * 사용자 정보 수정
     */
    public void updateInfo(String phone, String email) {
        this.phone = phone;
        if (email != null) {
            this.email = email;
        }
    }
}