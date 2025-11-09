package com.concrete.buildup.domain.contract.entity;

import com.concrete.buildup.domain.contract.enums.ContractState;
import com.concrete.buildup.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 근로 계약 엔티티
 *
 * contracts 테이블과 매핑되며, 근로 계약의 기본 정보와 상태를 관리합니다.
 *
 * 주요 기능:
 * - 근로자, 기업, 관리자 간 계약 관계 관리
 * - 계약 상태 추적 (작성 중, 전송됨, 서명 완료 등)
 * - 계약 기간 및 서명 일시 기록
 *
 * 연관 관계:
 * - Employee (N:1): 한 근로자가 여러 계약을 가질 수 있음
 * - Corporation (N:1): 한 기업이 여러 계약을 체결할 수 있음
 * - Manager (N:1): 한 관리자가 여러 계약을 관리할 수 있음
 * - ContractDetail (1:1): 계약 상세 정보 (스냅샷)
 * - ContractSignLog (1:N): 계약 서명 이력
 * - SigningSession (1:N): 전자서명 세션
 */
@Entity
@Table(name = "contracts", indexes = {
    @Index(name = "idx_employee_id", columnList = "employee_id"),
    @Index(name = "idx_corporation_id", columnList = "corporation_id"),
    @Index(name = "idx_manager_id", columnList = "manager_id"),
    @Index(name = "idx_contract_state", columnList = "contract_state"),
    @Index(name = "idx_employee_start_date", columnList = "employee_start_date")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Contract extends BaseEntity {

    /**
     * 근로자 ID
     * TODO: Employee 엔티티 구현 후 @ManyToOne 연관관계로 변경
     */
    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    /**
     * 기업 ID
     * TODO: Corporation 엔티티 구현 후 @ManyToOne 연관관계로 변경
     */
    @Column(name = "corporation_id", nullable = false)
    private Long corporationId;

    /**
     * 관리자 ID
     * TODO: Manager 엔티티 구현 후 @ManyToOne 연관관계로 변경
     */
    @Column(name = "manager_id")
    private Long managerId;

    /**
     * 계약 시 역할
     * 예: "현장 관리자", "안전 관리자" 등
     */
    @Column(name = "role", length = 30)
    private String role;

    /**
     * 계약 상태
     * - DRAFT: 작성 중
     * - SENT: 전송됨
     * - ADMIN_SIGNED: 관리자 서명 완료
     * - FULLY_SIGNED: 양측 서명 완료
     * - TERMINATED: 계약 종료
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "contract_state", length = 30)
    private ContractState contractState;

    /**
     * 근로 시작일
     */
    @Column(name = "employee_start_date", nullable = false)
    private LocalDate employeeStartDate;

    /**
     * 근로 종료일
     * NULL인 경우 계속 근로
     */
    @Column(name = "employee_end_date")
    private LocalDate employeeEndDate;

    /**
     * 계약서 작성일시
     */
    @Column(name = "written_at", nullable = false)
    private LocalDateTime writtenAt;

    /**
     * 기업 서명일시
     */
    @Column(name = "corp_signed_at")
    private LocalDateTime corpSignedAt;

    /**
     * 근로자 서명일시
     */
    @Column(name = "emp_signed_at")
    private LocalDateTime empSignedAt;

    /**
     * 계약 상세 정보 (1:1 양방향)
     * 계약 조회 시 상세 정보도 함께 필요한 경우가 많아 양방향 관계 설정
     */
    @OneToOne(mappedBy = "contract", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private ContractDetail contractDetail;

    /**
     * 최종 PDF S3 URL
     * 양측 서명이 완료된 최종 계약서 PDF의 S3 경로
     */
    @Column(name = "final_pdf_url", length = 500)
    private String finalPdfUrl;

    /**
     * 최종 PDF SHA-256 해시값
     * PDF 무결성 검증을 위한 해시값
     */
    @Column(name = "final_pdf_hash", length = 255)
    private String finalPdfHash;

    /**
     * PDF 생성 시각
     * 최종 PDF가 생성된 시각
     */
    @Column(name = "pdf_generated_at")
    private LocalDateTime pdfGeneratedAt;

    /**
     * Contract 생성자
     */
    @Builder
    public Contract(
        Long employeeId,
        Long corporationId,
        Long managerId,
        String role,
        ContractState contractState,
        LocalDate employeeStartDate,
        LocalDate employeeEndDate,
        LocalDateTime writtenAt,
        LocalDateTime corpSignedAt,
        LocalDateTime empSignedAt
    ) {
        this.employeeId = employeeId;
        this.corporationId = corporationId;
        this.managerId = managerId;
        this.role = role;
        this.contractState = contractState != null ? contractState : ContractState.DRAFT;
        this.employeeStartDate = employeeStartDate;
        this.employeeEndDate = employeeEndDate;
        this.writtenAt = writtenAt != null ? writtenAt : LocalDateTime.now();
        this.corpSignedAt = corpSignedAt;
        this.empSignedAt = empSignedAt;
    }

    /**
     * 관리자 서명 처리
     */
    public void signByAdmin() {
        this.corpSignedAt = LocalDateTime.now();
        this.contractState = ContractState.ADMIN_SIGNED;
    }

    /**
     * 근로자 서명 처리
     */
    public void signByEmployee() {
        this.empSignedAt = LocalDateTime.now();

        // 기업 서명이 이미 되어 있다면 완전히 서명된 상태로 변경
        if (this.corpSignedAt != null) {
            this.contractState = ContractState.FULLY_SIGNED;
        }
    }

    /**
     * 계약 종료 처리
     */
    public void terminate() {
        this.contractState = ContractState.TERMINATED;
    }

    /**
     * 계약이 활성 상태인지 확인
     */
    public boolean isActive() {
        return this.contractState == ContractState.FULLY_SIGNED
            && !this.getIsDeleted();
    }

    /**
     * 최종 PDF 정보 업데이트
     *
     * @param pdfUrl 최종 PDF S3 URL
     * @param pdfHash 최종 PDF SHA-256 해시값
     */
    public void updateFinalPdf(String pdfUrl, String pdfHash) {
        this.finalPdfUrl = pdfUrl;
        this.finalPdfHash = pdfHash;
        this.pdfGeneratedAt = LocalDateTime.now();
    }

    /**
     * 관리자 서명 대기 상태로 전환
     * DRAFT 상태에서만 전환 가능
     *
     * @throws IllegalStateException DRAFT 상태가 아닐 경우
     */
    public void transitionToManagerSigningPending() {
        if (this.contractState != ContractState.DRAFT) {
            throw new IllegalStateException("DRAFT 상태에서만 전환 가능");
        }
        this.contractState = ContractState.MANAGER_SIGNING_PENDING;
    }

    /**
     * 근로자 서명 대기 상태로 전환
     * MANAGER_SIGNING_PENDING 상태에서만 전환 가능
     *
     * @throws IllegalStateException MANAGER_SIGNING_PENDING 상태가 아닐 경우
     */
    public void transitionToEmployeeSigningPending() {
        if (this.contractState != ContractState.MANAGER_SIGNING_PENDING) {
            throw new IllegalStateException("MANAGER_SIGNING_PENDING 상태에서만 전환 가능");
        }
        this.contractState = ContractState.EMPLOYEE_SIGNING_PENDING;
    }

    /**
     * 완전 서명 완료 상태로 전환
     * EMPLOYEE_SIGNING_PENDING 상태에서만 전환 가능
     *
     * @throws IllegalStateException EMPLOYEE_SIGNING_PENDING 상태가 아닐 경우
     */
    public void transitionToFullySigned() {
        if (this.contractState != ContractState.EMPLOYEE_SIGNING_PENDING) {
            throw new IllegalStateException("EMPLOYEE_SIGNING_PENDING 상태에서만 전환 가능");
        }
        this.contractState = ContractState.FULLY_SIGNED;
        this.empSignedAt = LocalDateTime.now();
    }
}
