package com.concrete.buildup.domain.payroll.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 가산수당 발생/지급 조건 DTO
 *
 * <p>연장/야간/휴일 가산수당의 지급 조건을 판단하기 위한 정보를 담는 DTO입니다.</p>
 * <p>5인 이상 사업장은 가산수당 지급 의무, 5인 미만은 선택적 지급 가능합니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "가산수당 발생/지급 조건")
public class OvertimeConditions {

    // ========== 사업장 규모 ==========

    @Schema(description = "상시근로자 수 (5인 이상이면 가산수당 의무 지급)", example = "10", required = true)
    private Integer totalEmployees;

    // ========== 5인 미만 사업장의 자율 지급 정책 ==========

    @Schema(description = "연장수당 자율 지급 여부 (5인 미만일 때)", example = "true")
    private Boolean autoPayOvertime;

    @Schema(description = "야간수당 자율 지급 여부 (5인 미만일 때)", example = "true")
    private Boolean autoPayNight;

    @Schema(description = "휴일수당 자율 지급 여부 (5인 미만일 때)", example = "true")
    private Boolean autoPayHoliday;

    // ========== 연장근로 발생 조건 ==========

    @Schema(description = "일 8시간 초과 여부", example = "true")
    private Boolean exceedsDailyLimit;

    @Schema(description = "주 40시간 초과 여부", example = "false")
    private Boolean exceedsWeeklyLimit;

    // ========== 야간/휴일 근로 존재 여부 ==========

    @Schema(description = "야간근로 존재 여부 (22:00~06:00 구간 근로)", example = "true")
    private Boolean hasNightWork;

    @Schema(description = "휴일근로 존재 여부 (주휴일/법정공휴일 근로)", example = "false")
    private Boolean hasHolidayWork;
}
