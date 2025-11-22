package com.concrete.buildup.domain.safetydoc.dto;

import com.concrete.buildup.domain.safetydoc.entity.SafetyEducationLog;
import com.concrete.buildup.domain.safetydoc.enums.EducationType;
import com.concrete.buildup.domain.safetydoc.enums.SafetyEducationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SafetyEducationLogListResponse {

    private Long id;
    private EducationType educationType;
    private String educationSubject;
    private String instructorName;
    private SafetyEducationStatus status;
    private int totalAttendeeCount;
    private int signedAttendeeCount;
    private LocalDateTime createdAt;
    private String pdfUrl;

    public static SafetyEducationLogListResponse from(
            SafetyEducationLog log,
            int totalCount,
            int signedCount
    ) {
        return SafetyEducationLogListResponse.builder()
                .id(log.getId())
                .educationType(log.getEducationType())
                .educationSubject(log.getEducationSubject())
                .instructorName(log.getInstructorName())
                .status(log.getStatus())
                .totalAttendeeCount(totalCount)
                .signedAttendeeCount(signedCount)
                .createdAt(log.getCreatedAt())
                .pdfUrl(log.getPdfUrl())
                .build();
    }
}
