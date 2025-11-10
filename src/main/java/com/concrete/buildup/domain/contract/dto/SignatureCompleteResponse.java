package com.concrete.buildup.domain.contract.dto;

import com.concrete.buildup.domain.contract.enums.ContractState;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 서명 완료 응답 DTO
 *
 * <p>서명 처리 완료 후 반환되는 정보입니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "서명 완료 응답")
public class SignatureCompleteResponse {

    @Schema(description = "계약 ID", example = "1")
    private Long contractId;

    @Schema(description = "계약 상태", example = "FULLY_SIGNED")
    private ContractState contractState;

    @Schema(description = "PDF URL (S3)", example = "https://s3.amazonaws.com/bucket/contracts/1/v3.pdf")
    private String pdfUrl;

    @Schema(description = "PDF SHA-256 해시 (최종 PDF인 경우만)", example = "a3b5c7d9...")
    private String pdfHash;

    @Schema(description = "서명 완료 일시", example = "2024-01-15T14:30:00")
    private LocalDateTime signedAt;
}
