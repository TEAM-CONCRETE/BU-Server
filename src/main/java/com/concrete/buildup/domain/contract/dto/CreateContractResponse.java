package com.concrete.buildup.domain.contract.dto;

import com.concrete.buildup.domain.contract.enums.ContractState;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 계약 생성 응답 DTO
 *
 * <p>계약 생성 성공 시 반환되는 응답 DTO입니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "계약 생성 응답")
public class CreateContractResponse {

    @Schema(description = "생성된 계약 ID", example = "1")
    private Long contractId;

    @Schema(description = "계약 상태 (DRAFT, SENT, ADMIN_SIGNED, FULLY_SIGNED, TERMINATED)", example = "MANAGER_SIGNING_PENDING")
    private ContractState contractState;

    @Schema(description = "초안 PDF URL (S3)", example = "https://s3.amazonaws.com/bucket/contracts/1/v1.pdf")
    private String pdfUrl;
}