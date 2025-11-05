package com.concrete.buildup.domain.auth.entity;

import com.concrete.buildup.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 현장 관리자 엔티티
 *
 * 현장 관리자 정보
 *
 * 테이블: managers
 */
@Entity
@Table(name = "managers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Manager extends BaseEntity {

    /**
     * 사용자 ID (1:1 관계)
     * User 엔티티와 연결
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /**
     * 관리자 이름
     */
    @Column(name = "manager_name", nullable = false, length = 50)
    private String managerName;

    /**
     * 관리자 이름 수정
     */
    public void updateName(String managerName) {
        if (managerName != null) {
            this.managerName = managerName;
        }
    }
}