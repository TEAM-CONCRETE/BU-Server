package com.concrete.buildup.domain.auth.entity;

import com.concrete.buildup.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 기업 엔티티
 *
 * 기업 정보
 *
 * 테이블: corporations
 */
@Entity
@Table(name = "corporations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Corporation extends BaseEntity {

    /**
     * 사용자 ID (1:1 관계)
     * User 엔티티와 연결
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /**
     * 회사명
     */
    @Column(name = "corp_name", nullable = false, length = 100)
    private String corpName;

    /**
     * 본사 주소
     */
    @Column(name = "corp_address", length = 255)
    private String corpAddress;

    /**
     * 대표자 이름
     */
    @Column(name = "corp_ceo_name", length = 50)
    private String corpCeoName;

    /**
     * 기업 정보 수정
     */
    public void updateInfo(String corpName, String corpAddress, String corpCeoName) {
        if (corpName != null) {
            this.corpName = corpName;
        }
        if (corpAddress != null) {
            this.corpAddress = corpAddress;
        }
        if (corpCeoName != null) {
            this.corpCeoName = corpCeoName;
        }
    }
}