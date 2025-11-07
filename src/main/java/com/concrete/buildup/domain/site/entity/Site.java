package com.concrete.buildup.domain.site.entity;

import com.concrete.buildup.domain.auth.entity.Corporation;
import com.concrete.buildup.domain.auth.entity.Manager;
import com.concrete.buildup.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 현장 엔티티
 *
 * 건설 현장 정보 및 시크릿키 관리
 *
 * 테이블: sites
 */
@Entity
@Table(name = "sites")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Site extends BaseEntity {

    /**
     * 현장명
     */
    @Column(name = "site_name", nullable = false, length = 100)
    private String siteName;

    /**
     * 현장 주소
     */
    @Column(name = "site_address", length = 255)
    private String siteAddress;

    /**
     * 소속 기업
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "corporation_id")
    private Corporation corporation;

    /**
     * 현장 관리자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private Manager manager;

    /**
     * 현장 관리자용 시크릿키
     * 현장 관리자 회원가입 시 사용
     */
    @Column(name = "manager_secret_key", unique = true, length = 100)
    private String managerSecretKey;

    /**
     * 근로자용 시크릿키
     * 근로자 회원가입 시 사용
     */
    @Column(name = "employee_secret_key", unique = true, length = 100)
    private String employeeSecretKey;

    /**
     * 시크릿키 만료 시간
     */
    @Column(name = "secret_key_expires_at")
    private LocalDateTime secretKeyExpiresAt;

    /**
     * 시크릿키 유효성 확인
     */
    public boolean isSecretKeyValid() {
        if (this.secretKeyExpiresAt == null) {
            return true; // 만료 시간이 없으면 영구 유효
        }
        return LocalDateTime.now().isBefore(this.secretKeyExpiresAt);
    }

    /**
     * 현장 정보 수정
     */
    public void updateSiteInfo(String siteName, String siteAddress) {
        if (siteName != null) {
            this.siteName = siteName;
        }
        if (siteAddress != null) {
            this.siteAddress = siteAddress;
        }
    }

    /**
     * 현장 관리자 할당
     */
    public void assignManager(Manager manager) {
        this.manager = manager;
    }
}
