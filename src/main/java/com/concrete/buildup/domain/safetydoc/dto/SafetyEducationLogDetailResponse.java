package com.concrete.buildup.domain.safetydoc.dto;

import com.concrete.buildup.domain.safetydoc.entity.SafetyEducationLog;
import com.concrete.buildup.domain.safetydoc.enums.EducationType;
import com.concrete.buildup.domain.safetydoc.enums.SafetyEducationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Schema(description = "안전교육일지 상세 응답")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SafetyEducationLogDetailResponse {

    @Schema(description = "안전교육일지 ID", example = "1")
    private Long id;

    @Schema(description = "현장 ID", example = "1")
    private Long siteId;

    @Schema(description = "현장명", example = "강남 오피스빌딩 신축공사")
    private String siteName;

    @Schema(description = "소속 기업명", example = "(주)콘크리트")
    private String corporationName;

    @Schema(description = "관리자 성명", example = "김관리")
    private String managerName;

    @Schema(description = "교육 구분", example = "REGULAR")
    private EducationType educationType;

    @Schema(description = "교육 과목명", example = "추락 재해 예방 교육")
    private String educationSubject;

    @Schema(description = "교육 내용 상세", example = "1. 추락 재해 현황 및 사례\\n2. 추락 방지 시설 점검 요령...")
    private String educationContent;

    @Schema(description = "교육 실시자 성명", example = "김안전")
    private String instructorName;

    @Schema(description = "교육 실시 장소", example = "현장 사무실 회의실")
    private String educationLocation;

    @Schema(description = "안전교육일지 상태", example = "MANAGER_SIGNED")
    private SafetyEducationStatus status;

    @Schema(
            description = "관리자 서명 일시. 관리자 서명 전에는 null",
            example = "2024-01-15T10:30:00",
            nullable = true
    )
    private LocalDateTime managerSignedAt;

    @Schema(
            description = "현재 PDF URL (초안 또는 관리자 서명 PDF)",
            example = "https://bucket.s3.ap-northeast-2.amazonaws.com/safety-docs/1/2024-01-15/SE-2024-01-15-1-manager-signed.pdf"
    )
    private String pdfUrl;

    @Schema(
            description = "최종 PDF URL. 모든 참석자 서명 완료 후에만 값이 존재",
            example = "https://bucket.s3.ap-northeast-2.amazonaws.com/safety-docs/1/2024-01-15/SE-2024-01-15-1-final.pdf",
            nullable = true
    )
    private String finalPdfUrl;

    @Schema(
            description = "PDF 생성 일시",
            example = "2024-01-15T09:00:00"
    )
    private LocalDateTime pdfGeneratedAt;

    @Schema(description = "교육 대상자(참석자) 목록")
    private List<AttendeeDto> attendees;

    @Schema(description = "전체 참석자 수", example = "10")
    private int totalAttendeeCount;

    @Schema(description = "서명 완료한 참석자 수", example = "7")
    private int signedAttendeeCount;

    @Schema(description = "안전교육일지 생성일시", example = "2024-01-15T09:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "교육 대상자(참석자) 정보")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttendeeDto {

        @Schema(description = "근로자(Employee) ID", example = "10")
        private Long employeeId;

        @Schema(description = "근로자 성명", example = "홍길동")
        private String empName;

        @Schema(
                description = "근로자 유형 (PERMANENT: 상용직, DAILY: 일용직)",
                example = "PERMANENT"
        )
        private String empType;

        @Schema(description = "서명 완료 여부", example = "true")
        private Boolean isSigned;

        @Schema(
                description = "서명 일시. 서명 전에는 null",
                example = "2024-01-15T11:00:00",
                nullable = true
        )
        private LocalDateTime signedAt;
    }

    public static SafetyEducationLogDetailResponse from(
            SafetyEducationLog log,
            List<AttendeeDto> attendees,
            int signedCount
    ) {
        // null 방어: attendees가 null인 경우 빈 리스트로 처리
        List<AttendeeDto> safeAttendees = attendees != null ? attendees : Collections.emptyList();

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
                .attendees(safeAttendees)
                .totalAttendeeCount(safeAttendees.size())
                .signedAttendeeCount(signedCount)
                .createdAt(log.getCreatedAt())
                .build();
    }
}
