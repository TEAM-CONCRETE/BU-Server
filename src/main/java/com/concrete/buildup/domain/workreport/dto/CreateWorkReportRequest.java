package com.concrete.buildup.domain.workreport.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 작업일보 생성 요청 DTO
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "작업일보 생성 요청")
public class CreateWorkReportRequest {

    @Schema(
            description = "공정 목록 (최소 1개 필수)",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @Valid
    @NotNull(message = "공정 목록은 필수입니다.")
    @Size(min = 1, message = "최소 1개 이상의 공정이 필요합니다.")
    @Builder.Default
    private List<WorkSectionDto> workSections = new ArrayList<>();

    @Schema(description = "투입 자재 목록 (선택)")
    @Valid
    @Builder.Default
    private List<MaterialInputDto> materials = new ArrayList<>();
}
