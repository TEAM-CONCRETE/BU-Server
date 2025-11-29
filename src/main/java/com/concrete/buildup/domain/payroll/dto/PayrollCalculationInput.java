package com.concrete.buildup.domain.payroll.dto;

import com.concrete.buildup.domain.contract.enums.EmpType;
import com.concrete.buildup.domain.contract.enums.PayPeriod;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 급여 계산 입력 DTO
 *
 * <p>급여 계산에 필요한 모든 입력 정보를 담는 DTO입니다.</p>
 * <p>v1.1 급여 계산 로직의 입력 파라미터로 사용됩니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "급여 계산 입력 정보")
public class PayrollCalculationInput {

    // ========== 기본 정보 ==========

    @Schema(description = "고용 형태 (DAILY: 일용직, PERMANENT: 상용직)", example = "DAILY", required = true)
    private EmpType empType;

    @Schema(description = "지급 주기 (DAILY: 일, WEEKLY: 주, MONTHLY: 월)", example = "DAILY", required = true)
    private PayPeriod payPeriod;

    @Schema(description = "시급", example = "10000", required = true)
    private BigDecimal hourlyRate;

    @Schema(description = "1일 소정근로시간", example = "8", required = true)
    private BigDecimal dailyWorkHours;

    @Schema(description = "근로일수", example = "20", required = true)
    private Integer workDays;

    // ========== 가산 시간 (발생 시간) ==========

    @Schema(description = "연장근로시간", example = "10")
    private BigDecimal overtimeHours;

    @Schema(description = "야간근로시간", example = "5")
    private BigDecimal nightHours;

    @Schema(description = "휴일근로시간", example = "8")
    private BigDecimal holidayHours;

    // ========== 주휴수당 ==========

    @Schema(description = "주휴수당 대상 여부", example = "true")
    private Boolean weeklyHolidayEligible;

    // ========== 4대보험 가입 여부 ==========

    @Schema(description = "보험 가입 여부", required = true)
    private InsuranceEligibility insurance;

    // ========== 세금 관련 (상용직) ==========

    @Schema(description = "부양가족 수 (상용직 세금 계산용)", example = "2")
    private Integer dependents;

    // ========== 요율 ==========

    @Schema(description = "보험 요율 정보", required = true)
    private InsuranceRates rates;

    // ========== 가산수당 조건 ==========

    @Schema(description = "가산수당 발생/지급 조건", required = true)
    private OvertimeConditions overtimeConditions;

    // ========== 일용직 세금 계산용 (추가) ==========

    @Schema(description = "당월 근로일수 누적 (일용직 4대보험 적용 판단용)", example = "8")
    private Integer monthlyWorkDaysAccumulated;

    @Schema(description = "월 환산 소득 (일용직 4대보험 적용 판단용)", example = "2200000")
    private BigDecimal monthlyEstimatedIncome;
}