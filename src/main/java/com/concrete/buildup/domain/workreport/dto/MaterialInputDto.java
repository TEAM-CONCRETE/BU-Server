package com.concrete.buildup.domain.workreport.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 자재 투입 정보 DTO
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "자재 투입 정보")
public class MaterialInputDto {

    @Schema(
            description = "자재 품명",
            example = "레미콘",
            maxLength = 100,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "자재 품명은 필수입니다.")
    @Size(max = 100, message = "자재 품명은 100자 이하여야 합니다.")
    private String materialName;

    @Schema(
            description = "자재 규격",
            example = "25-210-12",
            maxLength = 100
    )
    @Size(max = 100, message = "자재 규격은 100자 이하여야 합니다.")
    private String materialStandard;

    @Schema(
            description = "자재 단위",
            example = "m³",
            maxLength = 20
    )
    @Size(max = 20, message = "자재 단위는 20자 이하여야 합니다.")
    private String materialUnit;
}
