package com.concrete.buildup.domain.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceDetailDto {
    private Long workerId;
    private String workerName;
    private String residentNumber;
    private String attendanceStatus;
    private String checkInTime;
    private String checkOutTime;
    private String totalWorkHours;
    private String nightWorkHours;
    private String overtimeHours;
    private String holidayWorkHours;
}
