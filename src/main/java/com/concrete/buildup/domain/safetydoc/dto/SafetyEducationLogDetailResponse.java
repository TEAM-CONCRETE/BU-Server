package com.concrete.buildup.domain.safetydoc.dto;

import com.concrete.buildup.domain.safetydoc.entity.SafetyEducationLog;
import com.concrete.buildup.domain.safetydoc.enums.EducationType;
import com.concrete.buildup.domain.safetydoc.enums.SafetyEducationStatus;
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
public class SafetyEducationLogDetailResponse {

    private Long id;
    private Long siteId;
    private String siteName;
    private String corporationName;
    private String managerName;

    private EducationType educationType;
    private String educationSubject;
    private String educationContent;
    private String instructorName;
    private String educationLocation;

    private SafetyEducationStatus status;
    private LocalDateTime managerSignedAt;

    private String pdfUrl;
    private String finalPdfUrl;
    private LocalDateTime pdfGeneratedAt;

    private List<AttendeeDto> attendees;
    private int totalAttendeeCount;
    private int signedAttendeeCount;

    private LocalDateTime createdAt;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttendeeDto {
        private Long employeeId;
        private String empName;
        private String empType;
        private Boolean isSigned;
        private LocalDateTime signedAt;
    }

    public static SafetyEducationLogDetailResponse from(
            SafetyEducationLog log,
            List<AttendeeDto> attendees,
            int signedCount
    ) {
        return SafetyEducationLogDetailResponse.builder()
                .id(log.getId())
                .siteId(log.getSite().getId())
                .siteName(log.getSite().getSiteName())
                .corporationName(log.getCorporation().getCorpName())
                .managerName(log.getManager().getManagerName())
                .educationType(log.getEducationType())
                .educationSubject(log.getEducationSubject())
                .educationContent(log.getEducationContent())
                .instructorName(log.getInstructorName())
                .educationLocation(log.getEducationLocation())
                .status(log.getStatus())
                .managerSignedAt(log.getManagerSignedAt())
                .pdfUrl(log.getPdfUrl())
                .finalPdfUrl(log.getFinalPdfUrl())
                .pdfGeneratedAt(log.getPdfGeneratedAt())
                .attendees(attendees)
                .totalAttendeeCount(attendees.size())
                .signedAttendeeCount(signedCount)
                .createdAt(log.getCreatedAt())
                .build();
    }
}
