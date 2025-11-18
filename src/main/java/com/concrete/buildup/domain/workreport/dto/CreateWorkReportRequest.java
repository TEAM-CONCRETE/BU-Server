package com.concrete.buildup.domain.workreport.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
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

    @Schema(description = "작업일자", example = "2025-11-19")
    @NotNull(message = "작업일자는 필수입니다.")
    private LocalDate workDate;

    @Schema(description = "공정명 (예: 철근공사, 거푸집공사)", example = "철근공사")
    @NotBlank(message = "공정명은 필수입니다.")
    @Size(max = 100, message = "공정명은 100자 이하여야 합니다.")
    private String workSection;

    @Schema(description = "투입 인력 수 (명)", example = "15")
    @NotNull(message = "투입 인력 수는 필수입니다.")
    @Min(value = 0, message = "투입 인력 수는 0 이상이어야 합니다.")
    private Integer workSectionEmployeeNum;

    @Schema(description = "공정별 작업 내용", example = "1층 기둥 철근 배근 작업 완료")
    @NotBlank(message = "작업 내용은 필수입니다.")
    private String workReportContext;

    @Schema(description = "투입 자재 목록")
    @Valid
    @Builder.Default
    private List<MaterialInputDto> materials = new ArrayList<>();
}
