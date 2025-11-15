package com.concrete.buildup.domain.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceListResponseDto {
    private AttendanceSummaryDto summary;
    private List<AttendanceDetailDto> records;
    private PaginationDto pagination;
}
