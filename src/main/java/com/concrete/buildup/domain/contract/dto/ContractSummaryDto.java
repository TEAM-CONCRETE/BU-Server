package com.concrete.buildup.domain.contract.dto;

import com.concrete.buildup.domain.contract.enums.ContractState;
import com.concrete.buildup.domain.contract.enums.EmpType;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "계약 목록 요약 정보")
public class ContractSummaryDto {

    @Schema(description = "계약 ID", example = "1")
    private Long contractId;

    @Schema(description = "근로자 ID (Employee PK)", example = "10")
    private Long employeeId;

    @Schema(description = "사용자 ID (로그인 ID)", example = "hong123")
    private String userId;

    @Schema(description = "근로자 이름", example = "홍길동")
    private String employeeName;

    @Schema(description = "근로자 주민등록번호 (마스킹 처리)", example = "950101-1******")
    private String employeeResidentNumber;

    @Schema(description = "근로자 전화번호", example = "010-1234-5678")
    private String employeePhone;

    @Schema(description = "근로자 유형 (DAILY: 일용직, PERMANENT: 상용직, UNCONTRACTED: 미계약)", example = "PERMANENT")
    private EmpType empType;

    @Schema(description = "직책/역할", example = "현장 관리자")
    private String role;

    @Schema(description = "계약 상태 (DRAFT: 초안, MANAGER_SIGNING_PENDING: 관리자 서명 대기, EMPLOYEE_SIGNING_PENDING: 근로자 서명 대기, FULLY_SIGNED: 완전 서명)", example = "FULLY_SIGNED")
    private ContractState contractState;

    @Schema(description = "근로 시작일", example = "2024-01-01")
    private LocalDate employeeStartDate;

    @Schema(description = "근로 종료일", example = "2024-12-31")
    private LocalDate employeeEndDate;

    @Schema(description = "계약서 작성일시", example = "2024-01-01T09:00:00")
    private LocalDateTime writtenAt;

    @Schema(description = "기업 서명일시", example = "2024-01-02T10:00:00")
    private LocalDateTime corporationSignedAt;

    @Schema(description = "근로자 서명일시", example = "2024-01-03T14:00:00")
    private LocalDateTime employeeSignedAt;
}
