package com.concrete.buildup.domain.attendance.entity;

import com.concrete.buildup.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 근태 기록 엔티티
 *
 * 근로자의 일일 근태 정보를 저장합니다.
 * 출퇴근 시간, 근로시간, 근태 상태 등을 관리합니다.
 *
 * 테이블: attendances
 */
@Entity
@Table(
    name = "attendances",
    indexes = {
        @Index(name = "idx_contract_id", columnList = "contract_id"),
        @Index(name = "idx_employee_id", columnList = "employee_id"),
        @Index(name = "idx_site_id", columnList = "site_id"),
        @Index(name = "idx_search_date", columnList = "search_date"),
        @Index(name = "idx_employee_search_date", columnList = "employee_id, search_date")
    }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Attendance extends BaseEntity {

    @Column(name = "contract_id", nullable = false)
    private Long contractId;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "site_id", nullable = false)
    private Long siteId;

    @Column(name = "search_date", nullable = false)
    private LocalDate searchDate;

    @Column(name = "emp_type", length = 30)
    private String empType;

    @Column(name = "emp_name", length = 50)
    private String empName;

    @Column(name = "resident_num", length = 500)
    private String residentNum;

    @Column(name = "attendance_status", length = 20)
    private String attendanceStatus;

    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;

    @Column(name = "check_out_time")
    private LocalDateTime checkOutTime;

    @Column(name = "total_work_hour", precision = 6, scale = 2)
    private BigDecimal totalWorkHour;

    @Column(name = "night_work_hour", precision = 6, scale = 2)
    private BigDecimal nightWorkHour;

    @Column(name = "additional_work_hour", precision = 6, scale = 2)
    private BigDecimal additionalWorkHour;

    @Column(name = "holiday_work_hour", precision = 6, scale = 2)
    private BigDecimal holidayWorkHour;
}
