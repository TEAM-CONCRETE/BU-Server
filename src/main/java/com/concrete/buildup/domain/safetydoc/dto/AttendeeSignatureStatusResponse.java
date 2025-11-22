package com.concrete.buildup.domain.safetydoc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendeeSignatureStatusResponse {

    private Long safetyEducationLogId;
    private int totalCount;
    private int signedCount;
    private int unsignedCount;
    private List<AttendeeSignatureDto> attendees;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttendeeSignatureDto {
        private Long employeeId;
        private String empName;
        private String empType;
        private Boolean isSigned;
        private LocalDateTime signedAt;
    }
}
