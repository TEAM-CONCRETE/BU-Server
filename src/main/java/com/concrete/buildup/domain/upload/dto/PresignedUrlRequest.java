package com.concrete.buildup.domain.upload.dto;

import com.concrete.buildup.domain.contract.enums.SignerRole;
import com.concrete.buildup.domain.upload.enums.ResourceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Presigned URL 발급 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Presigned URL 발급 요청")
public class PresignedUrlRequest {

    @NotNull(message = "리소스 타입은 필수입니다.")
    @Schema(description = "리소스 타입 (CONTRACT, WORK_REPORT, SAFETY_DOC)", example = "CONTRACT")
    private ResourceType resourceType;

    @NotBlank(message = "리소스 ID는 필수입니다.")
    @Schema(description = "리소스 ID (계약서 ID, 작업일보 ID 등)", example = "123")
    private String resourceId;

    @NotNull(message = "서명자 역할은 필수입니다.")
    @Schema(description = "서명자 역할 (EMPLOYEE, MANAGER, CORPORATION)", example = "EMPLOYEE")
    private SignerRole signerRole;

    @NotBlank(message = "파일 확장자는 필수입니다.")
    @Pattern(regexp = "^(png|jpg|jpeg|pdf)$", message = "지원하지 않는 파일 형식입니다. (png, jpg, jpeg, pdf만 가능)")
    @Schema(description = "파일 확장자", example = "png", allowableValues = {"png", "jpg", "jpeg", "pdf"})
    private String fileExtension;

    @Schema(
            description = "근로자 ID (안전교육일지 참석자 서명 시 필수). 참석자별 서명 이미지를 구분하기 위해 사용",
            example = "10",
            nullable = true
    )
    private Long employeeId;
}