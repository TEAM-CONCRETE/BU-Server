package com.concrete.buildup.domain.safetydoc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "참석자 서명 현황 응답")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendeeSignatureStatusResponse {

    @Schema(description = "안전교육일지 ID", example = "1")
    private Long safetyEducationLogId;

    @Schema(description = "전체 참석자 수", example = "10")
    private int totalCount;

    @Schema(description = "서명 완료한 참석자 수", example = "7")
    private int signedCount;

    @Schema(description = "미서명 참석자 수", example = "3")
    private int unsignedCount;

    @Schema(description = "참석자별 서명 현황 목록")
    private List<AttendeeSignatureDto> attendees;

    @Schema(description = "참석자 서명 상세 정보")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttendeeSignatureDto {

        @Schema(description = "근로자(Employee) ID. 서명 API 호출 시 이 ID를 사용", example = "10")
        private Long employeeId;

        @Schema(description = "근로자 성명", example = "홍길동")
        private String empName;

        @Schema(
                description = "근로자 유형 (PERMANENT: 상용직, DAILY: 일용직)",
                example = "PERMANENT",
                allowableValues = {"PERMANENT", "DAILY"}
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
}
