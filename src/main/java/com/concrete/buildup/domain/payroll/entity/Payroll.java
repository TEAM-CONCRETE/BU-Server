package com.concrete.buildup.domain.payroll.entity;

import com.concrete.buildup.domain.contract.enums.EmpType;
import com.concrete.buildup.domain.contract.enums.PayPeriod;
import com.concrete.buildup.domain.payroll.enums.PayStatus;
import com.concrete.buildup.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 급여 정보 엔티티
 *
 * payrolls 테이블과 매핑되며, 근로자별 급여 정보를 관리합니다.
 *
 * 주요 기능:
 * - 근로자별 급여 정보 저장
 * - 급여 대상 기간 관리 (연/월/주/일)
 * - 급여 계산 결과 저장 (총 지급액, 세금, 공제 등)
 * - PDF 급여명세서 S3 경로 관리
 *
 * 연관 관계:
 * - Employee (N:1): 근로자
 * - Contract (N:1): 계약
 * - Corporation (N:1): 기업
 * - PayslipItem (1:N): 급여 명세 항목
 */
@Entity
@Table(name = "payrolls", indexes = {
        @Index(name = "idx_employee_id", columnList = "employee_id"),
        @Index(name = "idx_contract_id", columnList = "contract_id"),
        @Index(name = "idx_corporation_id", columnList = "corporation_id"),
        @Index(name = "idx_site_id", columnList = "site_id"),
        @Index(name = "idx_search_date", columnList = "search_date"),
        @Index(name = "idx_pay_status", columnList = "pay_status"),
        @Index(name = "idx_s3_key", columnList = "s3_key"),
        @Index(name = "idx_period_search", columnList = "site_id, salary_year, salary_month, emp_type, pay_cycle")
}, uniqueConstraints = {
        @UniqueConstraint(
                name = "idx_payroll_unique",
                columnNames = {"employee_id", "salary_year", "salary_month", "pay_cycle", "salary_week", "salary_day"}
        )
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payroll extends BaseEntity {

    /**
     * 근로자 ID
     * TODO: Employee 엔티티 구현 후 @ManyToOne 연관관계로 변경
     */
    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    /**
     * 계약 ID
     * TODO: Contract 엔티티 구현 후 @ManyToOne 연관관계로 변경
     */
    @Column(name = "contract_id", nullable = false)
    private Long contractId;

    /**
     * 기업 ID
     * TODO: Corporation 엔티티 구현 후 @ManyToOne 연관관계로 변경
     */
    @Column(name = "corporation_id", nullable = false)
    private Long corporationId;

    /**
     * 현장 ID
     * TODO: Site 엔티티 구현 후 @ManyToOne 연관관계로 변경
     * nullable: Employee/User에서 siteId를 조회하여 설정 필요 (현재는 null 허용)
     */
    @Column(name = "site_id")
    private Long siteId;

    // ========== 급여 대상 기간 ==========

    /**
     * 급여 대상 연도
     * 예: 2025
     */
    @Column(name = "salary_year", nullable = false)
    private Integer salaryYear;

    /**
     * 급여 대상 월
     * 1~12
     */
    @Column(name = "salary_month", nullable = false)
    private Integer salaryMonth;

    /**
     * 급여 대상 주차
     * - 주급: 1~5
     * - 월급/일급: 0 (센티널 값)
     */
    @Column(name = "salary_week", nullable = false)
    private Integer salaryWeek;

    /**
     * 급여 대상 일자
     * - 일급: 실제 날짜
     * - 월급/주급: 해당 월의 1일 (센티널 값)
     */
    @Column(name = "salary_day", nullable = false)
    private LocalDate salaryDay;

    // ========== 지급 정보 ==========

    /**
     * 지급 기준월 (yyyy-mm)
     */
    @Column(name = "search_date", nullable = false)
    private LocalDate searchDate;

    /**
     * 지급 예정일
     */
    @Column(name = "pay_due_date")
    private LocalDate payDueDate;

    // ========== 근로자 정보 (스냅샷) ==========

    /**
     * 근로자 유형
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "emp_type", length = 30)
    private EmpType empType;

    /**
     * 근로자 이름
     */
    @Column(name = "emp_name", length = 50)
    private String empName;

    /**
     * 주민등록번호 (AES-256-GCM 암호화)
     */
    @Column(name = "resident_num", length = 500)
    private String residentNum;

    // ========== 급여 계산 결과 ==========

    /**
     * 총 근로시간
     */
    @Column(name = "total_work_hour", precision = 8, scale = 2)
    private BigDecimal totalWorkHour;

    /**
     * 총 지급액
     */
    @Column(name = "total_pay", precision = 15, scale = 2)
    private BigDecimal totalPay;

    /**
     * 일급 기준 지급액
     */
    @Column(name = "total_pay_by_day", precision = 15, scale = 2)
    private BigDecimal totalPayByDay;

    /**
     * 비과세 소득
     */
    @Column(name = "none_tax_income", precision = 15, scale = 2)
    private BigDecimal noneTaxIncome;

    /**
     * 소득세
     */
    @Column(name = "income_tax", precision = 15, scale = 2)
    private BigDecimal incomeTax;

    /**
     * 주민세
     */
    @Column(name = "resident_tax", precision = 15, scale = 2)
    private BigDecimal residentTax;

    // ========== 급여 주기 및 상태 ==========

    /**
     * 급여 주기
     * DAILY/WEEKLY/MONTHLY
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "pay_cycle", length = 20)
    private PayPeriod payCycle;

    /**
     * 지급 상태
     * PENDING/PAID/CANCELLED
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "pay_status", length = 20)
    private PayStatus payStatus;

    // ========== S3 저장 정보 ==========

    /**
     * 급여명세서 PDF 저장 S3 경로
     * 예: payroll/EMP123/2025/01/salary-456.pdf
     */
    @Column(name = "s3_key", length = 500)
    private String s3Key;

    /**
     * 급여명세서 자동 생성 시각
     */
    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    // ========== 빌더 ==========

    @Builder
    public Payroll(Long employeeId, Long contractId, Long corporationId, Long siteId,
                   Integer salaryYear, Integer salaryMonth, Integer salaryWeek, LocalDate salaryDay,
                   LocalDate searchDate, LocalDate payDueDate,
                   EmpType empType, String empName, String residentNum,
                   BigDecimal totalWorkHour, BigDecimal totalPay, BigDecimal totalPayByDay,
                   BigDecimal noneTaxIncome, BigDecimal incomeTax, BigDecimal residentTax,
                   PayPeriod payCycle, PayStatus payStatus,
                   String s3Key, LocalDateTime generatedAt) {
        this.employeeId = employeeId;
        this.contractId = contractId;
        this.corporationId = corporationId;
        this.siteId = siteId;
        this.salaryYear = salaryYear;
        this.salaryMonth = salaryMonth;
        this.salaryWeek = salaryWeek;
        this.salaryDay = salaryDay;
        this.searchDate = searchDate;
        this.payDueDate = payDueDate;
        this.empType = empType;
        this.empName = empName;
        this.residentNum = residentNum;
        this.totalWorkHour = totalWorkHour;
        this.totalPay = totalPay;
        this.totalPayByDay = totalPayByDay;
        this.noneTaxIncome = noneTaxIncome;
        this.incomeTax = incomeTax;
        this.residentTax = residentTax;
        this.payCycle = payCycle;
        this.payStatus = payStatus;
        this.s3Key = s3Key;
        this.generatedAt = generatedAt;
    }

    // ========== 비즈니스 메서드 ==========

    /**
     * S3 키 업데이트
     */
    public void updateS3Key(String s3Key) {
        this.s3Key = s3Key;
    }

    /**
     * 급여 지급 완료 처리
     */
    public void markAsPaid() {
        this.payStatus = PayStatus.PAID;
    }

    /**
     * 급여 지급 취소 처리
     */
    public void cancel() {
        this.payStatus = PayStatus.CANCELLED;
    }
}