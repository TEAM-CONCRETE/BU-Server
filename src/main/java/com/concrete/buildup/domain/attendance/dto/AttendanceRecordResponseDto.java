package com.concrete.buildup.domain.attendance.dto;

import com.concrete.buildup.domain.attendance.enums.AttendanceState;
import com.concrete.buildup.domain.attendance.enums.AttendanceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceRecordResponseDto {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private AttendanceType attendanceType;
    private LocalDateTime timestamp;
    private String capturedFaceImageUrl;
    private Double similarityScore;
    private AttendanceState state;
    private String failureReason;
}
