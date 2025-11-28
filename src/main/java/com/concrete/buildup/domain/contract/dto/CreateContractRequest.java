package com.concrete.buildup.domain.contract.dto;

import com.concrete.buildup.domain.contract.enums.EmpType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 계약 생성 요청 DTO
 *
 * <p>근로계약 생성 시 사용되는 요청 DTO입니다.</p>
 * <p>기본 정보와 상세 정보(details)를 포함합니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "계약 생성 요청")
public class CreateContractRequest {

    // ========== 기본 정보 ==========

    @NotNull(message = "근로자 User ID는 필수입니다")
    @Schema(description = "근로자의 로그인 ID (User 테이블의 user_id)", example = "emp_daily1", required = true)
    private String userId;

    @Schema(description = "계약 시 역할", example = "현장 관리자")
    private String role;

    @NotNull(message = "근로자 유형은 필수입니다")
    @Schema(description = "근로자 유형 (DAILY: 일용직, PERMANENT: 상용직)", example = "PERMANENT", required = true)
    private EmpType empType;

    @NotNull(message = "근로 시작일은 필수입니다")
    @Schema(description = "근로 시작일", example = "2024-01-01", required = true)
    private LocalDate employeeStartDate;

    @Schema(description = "근로 종료일 (NULL인 경우 계속 근로)", example = "2024-12-31")
    private LocalDate employeeEndDate;

    // ========== 상세 정보 ==========

    @NotNull(message = "계약 상세 정보는 필수입니다")
    @Valid
    @Schema(description = "계약 상세 정보", required = true)
    private ContractDetailRequest details;
}