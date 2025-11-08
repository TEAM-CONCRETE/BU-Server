package com.concrete.buildup.domain.contract.dto;

import com.concrete.buildup.domain.contract.enums.PayPeriod;
import com.concrete.buildup.domain.contract.enums.PayType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * 계약 상세 정보 요청 DTO (내장 DTO)
 *
 * <p>계약 생성 시 상세 정보를 담는 DTO입니다.</p>
 * <p>ContractDetail 엔티티의 스냅샷 데이터로 저장됩니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "계약 상세 정보 요청")
public class ContractDetailRequest {

    // ========== 근무 정보 ==========

    @Schema(description = "근무 장소", example = "서울시 강남구 테헤란로 123")
    private String workPlace;

    @Schema(description = "직종", example = "일반건설현장근로자")
    private String workType;

    @Schema(description = "근무 시작 시간", example = "09:00:00")
    private LocalTime workStartTime;

    @Schema(description = "근무 종료 시간", example = "18:00:00")
    private LocalTime workEndTime;

    @Schema(description = "휴게 시작 시간", example = "12:00:00")
    private LocalTime breakStartTime;

    @Schema(description = "휴게 종료 시간", example = "13:00:00")
    private LocalTime breakEndTime;

    @Schema(description = "근무일", example = "주 5일 (월~금)")
    private String workOnDay;

    @Schema(description = "휴일", example = "토, 일")
    private String workOffDay;

    // ========== 급여 정보 ==========

    @NotNull(message = "기본 임금은 필수입니다")
    @Schema(description = "기본 임금", example = "3000000.00", required = true)
    private BigDecimal workPay;

    @Schema(description = "상여금", example = "500000.00")
    private BigDecimal workBonus;

    @Schema(description = "시간외 근로 수당", example = "150000.00")
    private BigDecimal additionalHourPay;

    @Schema(description = "야간 근로 수당", example = "100000.00")
    private BigDecimal additionalNightPay;

    @Schema(description = "휴일 근로 수당", example = "200000.00")
    private BigDecimal additionalHolidayPay;

    // ========== 지급 정보 ==========

    @NotNull(message = "임금 지급일은 필수입니다")
    @Schema(description = "임금 지급일", example = "매월 25일", required = true)
    private String payday;

    @NotNull(message = "지급 주기는 필수입니다")
    @Schema(description = "지급 주기 (DAILY, WEEKLY, MONTHLY)", example = "MONTHLY", required = true)
    private PayPeriod payPeriod;

    @NotNull(message = "지급 방법은 필수입니다")
    @Schema(description = "지급 방법 (CASH, TRANSFER)", example = "TRANSFER", required = true)
    private PayType payType;

    // ========== 4대보험 적용 여부 ==========

    @Schema(description = "고용보험 적용 여부", example = "true")
    private Boolean isEoiApplicable;

    @Schema(description = "산재보험 적용 여부", example = "true")
    private Boolean isWciApplicable;

    @Schema(description = "국민연금 적용 여부", example = "true")
    private Boolean isNpsApplicable;

    @Schema(description = "건강보험 적용 여부", example = "true")
    private Boolean isNhiApplicable;
}