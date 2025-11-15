package com.concrete.buildup.domain.attendance.entity;

import com.concrete.buildup.domain.attendance.enums.AttendanceStatus;
import com.concrete.buildup.domain.contract.enums.EmpType;
import com.concrete.buildup.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 근태 기록 엔티티
 *
 * attendances 테이블과 매핑되며, 근로자의 출퇴근 및 근무시간을 관리합니다.
 *
 * 주요 기능:
 * - 출퇴근 시간 기록
 * - 근무시간 자동 계산 (총 근로시간, 야간, 연장, 휴일)
 * - 출근 상태 관리 (정상, 지각, 조퇴, 결근, 휴무)
 *
 * 연관 관계:
 * - Contract (N:1): 계약
 * - Employee (N:1): 근로자
 * - Site (N:1): 현장
 */
@Entity
@Table(name = "attendances", indexes = {
        @Index(name = "idx_contract_id", columnList = "contract_id"),
        @Index(name = "idx_employee_id", columnList = "employee_id"),
        @Index(name = "idx_site_id", columnList = "site_id"),
        @Index(name = "idx_search_date", columnList = "search_date"),
        @Index(name = "idx_attendances_employee_date", columnList = "employee_id, search_date")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Attendance extends BaseEntity {

    /**
     * 계약 ID
     */
    @Column(name = "contract_id", nullable = false)
    private Long contractId;

    /**
     * 근로자 ID
     */
    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    /**
     * 현장 ID
     */
    @Column(name = "site_id", nullable = false)
    private Long siteId;

    /**
     * 근무일자
     */
    @Column(name = "search_date", nullable = false)
    private LocalDate searchDate;

    // ========== 근로자 정보 (스냅샷) ==========

    /**
     * 근로자 유형
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "emp_type", length = 30)
    private EmpType empType;

    /**
     * 근로자 이름
     */
    @Column(name = "emp_name", length = 50)
    private String empName;

    /**
     * 주민등록번호 (AES-256-GCM 암호화)
     */
    @Column(name = "resident_num", length = 500)
    private String residentNum;

    // ========== 출퇴근 정보 ==========

    /**
     * 출근 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_status", length = 20)
    private AttendanceStatus attendanceStatus;

    /**
     * 출근 시간
     */
    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;

    /**
     * 퇴근 시간
     */
    @Column(name = "check_out_time")
    private LocalDateTime checkOutTime;

    // ========== 근무시간 ==========

    /**
     * 총 근로시간
     */
    @Column(name = "total_work_hour", precision = 6, scale = 2)
    private BigDecimal totalWorkHour;

    /**
     * 야간 근로시간
     */
    @Column(name = "night_work_hour", precision = 6, scale = 2)
    private BigDecimal nightWorkHour;

    /**
     * 연장 근로시간
     */
    @Column(name = "additional_work_hour", precision = 6, scale = 2)
    private BigDecimal additionalWorkHour;

    /**
     * 휴일 근로시간
     */
    @Column(name = "holiday_work_hour", precision = 6, scale = 2)
    private BigDecimal holidayWorkHour;

    // ========== 빌더 ==========

    @Builder
    public Attendance(Long contractId, Long employeeId, Long siteId, LocalDate searchDate,
                      EmpType empType, String empName, String residentNum,
                      AttendanceStatus attendanceStatus,
                      LocalDateTime checkInTime, LocalDateTime checkOutTime,
                      BigDecimal totalWorkHour, BigDecimal nightWorkHour,
                      BigDecimal additionalWorkHour, BigDecimal holidayWorkHour) {
        this.contractId = contractId;
        this.employeeId = employeeId;
        this.siteId = siteId;
        this.searchDate = searchDate;
        this.empType = empType;
        this.empName = empName;
        this.residentNum = residentNum;
        this.attendanceStatus = attendanceStatus;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
        this.totalWorkHour = totalWorkHour;
        this.nightWorkHour = nightWorkHour;
        this.additionalWorkHour = additionalWorkHour;
        this.holidayWorkHour = holidayWorkHour;
    }

    // ========== 비즈니스 메서드 ==========

    /**
     * 퇴근 처리
     */
    public void checkOut(LocalDateTime checkOutTime) {
        this.checkOutTime = checkOutTime;
        // TODO: 담당자가 근무시간 계산 로직 구현 필요
        // calculateWorkHours();
    }

    /**
     * 출근 상태 업데이트
     */
    public void updateStatus(AttendanceStatus status) {
        this.attendanceStatus = status;
    }
}