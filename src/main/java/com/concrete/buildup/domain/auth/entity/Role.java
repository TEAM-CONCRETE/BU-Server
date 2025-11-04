package com.concrete.buildup.domain.auth.entity;

import com.concrete.buildup.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 역할 엔티티
 *
 * 사용자 역할 관리 (EMPLOYEE, MANAGER, CORPORATION, ADMIN)
 *
 * 테이블: roles
 */
@Entity
@Table(name = "roles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Role extends BaseEntity {

    /**
     * 역할명 (EMPLOYEE, MANAGER, CORPORATION, ADMIN)
     * 중복 불가
     */
    @Column(name = "role_name", nullable = false, unique = true, length = 50)
    private String roleName;

    /**
     * 역할 설명
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}