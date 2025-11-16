package com.concrete.buildup.domain.payroll.entity;

import com.concrete.buildup.domain.payroll.enums.ItemType;
import com.concrete.buildup.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 급여 명세 항목 엔티티
 *
 * payslip_items 테이블과 매핑되며, 급여명세의 세부 항목을 관리합니다.
 *
 * 주요 기능:
 * - 급여 항목별 금액 및 타입 저장 (지급/공제)
 * - 기본급, 각종 수당, 세금, 4대보험 등 상세 내역 관리
 *
 * 연관 관계:
 * - Payroll (N:1): 급여 정보
 */
@Entity
@Table(name = "payslip_items", indexes = {
        @Index(name = "idx_payroll_id", columnList = "payroll_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PayslipItem extends BaseEntity {

    /**
     * 급여 ID
     * TODO: Payroll 엔티티 구현 후 @ManyToOne 연관관계로 변경
     */
    @Column(name = "payroll_id", nullable = false)
    private Long payrollId;

    /**
     * 항목명
     * 예: 기본급, 식대, 야간근로수당, 소득세, 국민연금 등
     */
    @Column(name = "item_name", length = 100, nullable = false)
    private String itemName;

    /**
     * 항목 구분
     * EARNING: 지급 항목 (기본급, 수당 등)
     * DEDUCTION: 공제 항목 (세금, 4대보험 등)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", length = 30, nullable = false)
    private ItemType itemType;

    /**
     * 금액
     */
    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    /**
     * 적용 날짜
     */
    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    // ========== 빌더 ==========

    @Builder
    public PayslipItem(Long payrollId, String itemName, ItemType itemType,
                       BigDecimal amount, LocalDate effectiveDate) {
        this.payrollId = payrollId;
        this.itemName = itemName;
        this.itemType = itemType;
        this.amount = amount;
        this.effectiveDate = effectiveDate;
    }
}