package com.concrete.buildup.domain.contract.dto;

import com.concrete.buildup.domain.contract.enums.ContractState;
import com.concrete.buildup.domain.contract.enums.EmpType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 계약 목록 조회 응답 DTO (요약 정보)
 *
 * <p>계약 목록에서 각 계약의 요약 정보를 담는 DTO입니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractSummaryDto {

    /**
     * 계약 ID
     */
    private Long contractId;

    /**
     * 근로자 ID
     */
    private Long employeeId;

    /**
     * 근로자 이름
     */
    private String employeeName;

    /**
     * 근로자 주민등록번호 (마스킹 처리)
     * 예: "010324-1******"
     */
    private String employeeResidentNumber;

    /**
     * 근로자 유형
     * DAILY: 일용직, PERMANENT: 상용직
     */
    private EmpType empType;

    /**
     * 직책/역할
     */
    private String role;

    /**
     * 계약 상태
     * DRAFT: 초안, PENDING: 대기중, FULLY_SIGNED: 완전서명
     */
    private ContractState contractState;

    /**
     * 근로 시작일
     */
    private LocalDate employeeStartDate;

    /**
     * 근로 종료일
     */
    private LocalDate employeeEndDate;

    /**
     * 계약서 작성일시
     */
    private LocalDateTime writtenAt;

    /**
     * 기업 서명일시
     */
    private LocalDateTime corporationSignedAt;

    /**
     * 근로자 서명일시
     */
    private LocalDateTime employeeSignedAt;
}
