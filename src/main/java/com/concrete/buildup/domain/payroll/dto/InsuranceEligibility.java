package com.concrete.buildup.domain.payroll.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 4대보험 가입 여부 DTO
 *
 * <p>근로자의 4대보험 가입 여부 정보를 담는 DTO입니다.</p>
 * <p>산재보험은 근로자 부담이 없으므로 포함하지 않습니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "4대보험 가입 여부")
public class InsuranceEligibility {

    @Schema(description = "고용보험 가입 여부", example = "true", required = true)
    private Boolean employmentInsurance;

    @Schema(description = "건강보험 가입 여부", example = "true", required = true)
    private Boolean healthInsurance;

    @Schema(description = "국민연금 가입 여부", example = "false", required = true)
    private Boolean nationalPension;

    // 산재보험은 근로자 부담이 없으므로 제외
}