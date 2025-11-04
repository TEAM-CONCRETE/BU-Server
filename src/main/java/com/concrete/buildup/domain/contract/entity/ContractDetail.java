package com.concrete.buildup.domain.contract.entity;

import com.concrete.buildup.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * 계약 상세 정보 엔티티 (스냅샷)
 *
 * contract_details 테이블과 매핑되며, 계약 당시의 상세 정보를 스냅샷으로 보존합니다.
 * 계약 체결 후 회사명, 근로자 정보 등이 변경되어도 계약서 원본은 유지됩니다.
 *
 * 주요 기능:
 * - 계약 당시 회사/근로자 정보 스냅샷 보존
 * - 근무 조건 (장소, 시간, 휴게시간, 근무일/휴일)
 * - 급여 정보 (기본급, 상여금, 각종 수당)
 * - 급여 지급 정보 (지급일, 주기, 방법)
 * - 4대보험 적용 여부
 *
 * 연관 관계:
 * - Contract (1:1): 계약 기본 정보와 1:1 매핑
 */
@Entity
@Table(name = "contract_details", indexes = {
    @Index(name = "idx_contract_id", columnList = "contract_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContractDetail extends BaseEntity {

    /**
     * 계약 (1:1 양방향)
     * 계약 상세 정보는 계약의 생명주기에 종속됨
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false, unique = true)
    private Contract contract;

    // ========== 스냅샷 필드 (계약 당시 정보) ==========

    /**
     * 회사명 (스냅샷)
     * 계약 체결 당시의 회사명을 보존
     */
    @Column(name = "corp_name", length = 100, nullable = false)
    private String corpName;

    /**
     * 근로자 이름 (스냅샷)
     * 계약 체결 당시의 근로자 이름을 보존
     */
    @Column(name = "emp_name", length = 50, nullable = false)
    private String empName;

    /**
     * 회사 주소 (스냅샷)
     */
    @Column(name = "corp_address", length = 255)
    private String corpAddress;

    /**
     * 대표자 이름 (스냅샷)
     */
    @Column(name = "corp_ceo_name", length = 50)
    private String corpCeoName;

    /**
     * 근로자 주소 (스냅샷)
     */
    @Column(name = "emp_address", length = 255)
    private String empAddress;

    // ========== 근무 정보 ==========

    /**
     * 근무 장소
     */
    @Column(name = "work_place", length = 255)
    private String workPlace;

    /**
     * 직종
     * 예: "일용직", "상용직" 등
     */
    @Column(name = "work_type", length = 100)
    private String workType;

    /**
     * 근무 시작 시간
     * 예: 09:00
     */
    @Column(name = "work_start_time")
    private LocalTime workStartTime;

    /**
     * 근무 종료 시간
     * 예: 18:00
     */
    @Column(name = "work_end_time")
    private LocalTime workEndTime;

    /**
     * 휴게 시작 시간
     * 예: 12:00
     */
    @Column(name = "break_start_time")
    private LocalTime breakStartTime;

    /**
     * 휴게 종료 시간
     * 예: 13:00
     */
    @Column(name = "break_end_time")
    private LocalTime breakEndTime;

    /**
     * 근무일
     * 예: "주 5일"
     */
    @Column(name = "work_on_day", length = 100)
    private String workOnDay;

    /**
     * 휴일
     * 예: "토, 일"
     */
    @Column(name = "work_off_day", length = 100)
    private String workOffDay;

    // ========== 급여 정보 ==========

    /**
     * 기본 임금
     */
    @Column(name = "work_pay", precision = 15, scale = 2)
    private BigDecimal workPay;

    /**
     * 상여금
     */
    @Column(name = "work_bonus", precision = 15, scale = 2)
    private BigDecimal workBonus;

    /**
     * 시간 외 근로 수당
     */
    @Column(name = "additional_hour_pay", precision = 15, scale = 2)
    private BigDecimal additionalHourPay;

    /**
     * 야간 근로 수당
     */
    @Column(name = "additional_night_pay", precision = 15, scale = 2)
    private BigDecimal additionalNightPay;

    /**
     * 휴일 근로 수당
     */
    @Column(name = "additional_holiday_pay", precision = 15, scale = 2)
    private BigDecimal additionalHolidayPay;

    // ========== 지급 정보 ==========

    /**
     * 임금 지급일
     * 예: "매월 25일", "매주 금요일"
     */
    @Column(name = "payday", length = 30)
    private String payday;

    /**
     * 지급 주기
     * DAILY: 일급, WEEKLY: 주급, MONTHLY: 월급
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "pay_period", length = 30)
    private PayPeriod payPeriod;

    /**
     * 지급 방법
     * CASH: 현금, TRANSFER: 계좌이체
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "pay_type", length = 30)
    private PayType payType;

    // ========== 4대보험 적용 여부 ==========

    /**
     * 고용보험 적용 여부 (EOI: Employment Insurance)
     */
    @Column(name = "is_eoi_applicable")
    private Boolean isEoiApplicable;

    /**
     * 산재보험 적용 여부 (WCI: Workers' Compensation Insurance)
     */
    @Column(name = "is_wci_applicable")
    private Boolean isWciApplicable;

    /**
     * 국민연금 적용 여부 (NPS: National Pension Service)
     */
    @Column(name = "is_nps_applicable")
    private Boolean isNpsApplicable;

    /**
     * 건강보험 적용 여부 (NHI: National Health Insurance)
     */
    @Column(name = "is_nhi_applicable")
    private Boolean isNhiApplicable;

    /**
     * 지급 주기 Enum
     */
    public enum PayPeriod {
        /** 일급 */
        DAILY,
        /** 주급 */
        WEEKLY,
        /** 월급 */
        MONTHLY
    }

    /**
     * 지급 방법 Enum
     */
    public enum PayType {
        /** 현금 */
        CASH,
        /** 계좌이체 */
        TRANSFER
    }

    /**
     * ContractDetail 생성자
     */
    @Builder
    public ContractDetail(
        Contract contract,
        String corpName,
        String empName,
        String corpAddress,
        String corpCeoName,
        String empAddress,
        String workPlace,
        String workType,
        LocalTime workStartTime,
        LocalTime workEndTime,
        LocalTime breakStartTime,
        LocalTime breakEndTime,
        String workOnDay,
        String workOffDay,
        BigDecimal workPay,
        BigDecimal workBonus,
        BigDecimal additionalHourPay,
        BigDecimal additionalNightPay,
        BigDecimal additionalHolidayPay,
        String payday,
        PayPeriod payPeriod,
        PayType payType,
        Boolean isEoiApplicable,
        Boolean isWciApplicable,
        Boolean isNpsApplicable,
        Boolean isNhiApplicable
    ) {
        this.contract = contract;
        this.corpName = corpName;
        this.empName = empName;
        this.corpAddress = corpAddress;
        this.corpCeoName = corpCeoName;
        this.empAddress = empAddress;
        this.workPlace = workPlace;
        this.workType = workType;
        this.workStartTime = workStartTime;
        this.workEndTime = workEndTime;
        this.breakStartTime = breakStartTime;
        this.breakEndTime = breakEndTime;
        this.workOnDay = workOnDay;
        this.workOffDay = workOffDay;
        this.workPay = workPay;
        this.workBonus = workBonus;
        this.additionalHourPay = additionalHourPay;
        this.additionalNightPay = additionalNightPay;
        this.additionalHolidayPay = additionalHolidayPay;
        this.payday = payday;
        this.payPeriod = payPeriod;
        this.payType = payType;
        this.isEoiApplicable = isEoiApplicable;
        this.isWciApplicable = isWciApplicable;
        this.isNpsApplicable = isNpsApplicable;
        this.isNhiApplicable = isNhiApplicable;
    }

    /**
     * 총 급여 계산 (기본급 + 상여금)
     *
     * @return 총 급여 (null인 경우 0 반환)
     */
    public BigDecimal calculateTotalPay() {
        BigDecimal total = BigDecimal.ZERO;

        if (workPay != null) {
            total = total.add(workPay);
        }
        if (workBonus != null) {
            total = total.add(workBonus);
        }

        return total;
    }

    /**
     * 4대보험 적용 여부 확인
     *
     * @return 하나라도 적용되어 있으면 true
     */
    public boolean hasAnyInsurance() {
        return Boolean.TRUE.equals(isEoiApplicable)
            || Boolean.TRUE.equals(isWciApplicable)
            || Boolean.TRUE.equals(isNpsApplicable)
            || Boolean.TRUE.equals(isNhiApplicable);
    }

    /**
     * 근무 시간 계산 (분 단위)
     * 휴게시간을 제외한 실제 근무 시간
     *
     * @return 근무 시간 (분), 시간 정보가 없으면 0 반환
     */
    public long calculateWorkMinutes() {
        if (workStartTime == null || workEndTime == null) {
            return 0;
        }

        long totalMinutes = java.time.Duration.between(workStartTime, workEndTime).toMinutes();

        // 휴게시간 차감
        if (breakStartTime != null && breakEndTime != null) {
            long breakMinutes = java.time.Duration.between(breakStartTime, breakEndTime).toMinutes();
            totalMinutes -= breakMinutes;
        }

        return Math.max(0, totalMinutes);
    }

    /**
     * 근무 시간 계산 (시간 단위, 소수점 2자리)
     *
     * @return 근무 시간 (시간)
     */
    public BigDecimal calculateWorkHours() {
        long minutes = calculateWorkMinutes();
        return BigDecimal.valueOf(minutes)
            .divide(BigDecimal.valueOf(60), 2, BigDecimal.ROUND_HALF_UP);
    }
}