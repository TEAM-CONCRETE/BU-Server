package com.concrete.buildup.domain.contract.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 서명 처리 요청 DTO
 *
 * <p>관리자 또는 근로자의 서명 처리 시 필요한 정보를 전달받습니다.</p>
 * <p>서명 좌표는 서버에서 HTML 템플릿 기반으로 자동 계산됩니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "서명 처리 요청")
public class SignatureRequest {

    @NotBlank(message = "서명 이미지 S3 키는 필수입니다")
    @Schema(
            description = "서명 이미지 S3 키 (서버에서 자동으로 S3 전체 URL로 변환되어 저장됨)",
            example = "uploads/contracts/123/MANAGER.png",
            required = true
    )
    private String signatureS3Key;

    @NotBlank(message = "클라이언트 해시는 필수입니다")
    @Pattern(regexp = "^[0-9a-fA-F]{64}$", message = "SHA-256 해시 형식이 올바르지 않습니다 (64자리 16진수)")
    @Schema(
            description = "클라이언트에서 계산한 서명 이미지의 SHA-256 해시값",
            example = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            required = true
    )
    private String clientHash;
}