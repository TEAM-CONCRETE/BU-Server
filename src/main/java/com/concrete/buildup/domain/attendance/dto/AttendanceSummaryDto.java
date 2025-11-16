package com.concrete.buildup.domain.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceSummaryDto {
    private Long normalAttendance;
    private Long late;
    private Long earlyLeave;
    private Long absent;
}
