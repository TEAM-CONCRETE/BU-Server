package com.concrete.buildup.global.common;

import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 모든 Entity의 기본 클래스
 *
 * 공통 필드:
 * - id: 기본 키 (자동 증가)
 * - createdAt: 생성 일시
 * - updatedAt: 수정 일시
 *
 * JPA Auditing을 사용하여 자동으로 생성/수정 일시를 관리합니다.
 *
 * 사용 예시:
 * <pre>
 * {@code
 * @Entity
 * @Table(name = "employees")
 * public class Employee extends BaseEntity {
 *     // 추가 필드만 정의
 *     private String name;
 * }
 * }
 * </pre>
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    /**
     * 기본 키 (자동 증가)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 생성 일시
     * JPA Auditing이 자동으로 설정
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 수정 일시
     * JPA Auditing이 자동으로 업데이트
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 삭제 여부 (소프트 삭제)
     * true: 삭제됨, false: 삭제되지 않음
     */
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    /**
     * 소프트 삭제 처리
     */
    public void delete() {
        this.isDeleted = true;
    }

    /**
     * 소프트 삭제 복구
     */
    public void restore() {
        this.isDeleted = false;
    }
}