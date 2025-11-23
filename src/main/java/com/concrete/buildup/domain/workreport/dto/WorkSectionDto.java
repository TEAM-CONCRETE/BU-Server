package com.concrete.buildup.domain.workreport.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 공정 정보 DTO
 *
 * <p>작업일보의 각 공정별 정보를 담는 DTO입니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "공정 정보")
public class WorkSectionDto {

    @Schema(
            description = "공정명",
            example = "철근공사",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "공정명은 필수입니다.")
    private String sectionName;

    @Schema(
            description = "투입 인력 수 (명)",
            example = "9",
            minimum = "0",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "투입 인력 수는 필수입니다.")
    @Min(value = 0, message = "투입 인력 수는 0 이상이어야 합니다.")
    private Integer employeeNum;

    @Schema(
            description = "작업 내용",
            example = "1층 바닥 철근 배근 작업 완료",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "작업 내용은 필수입니다.")
    private String context;
}
