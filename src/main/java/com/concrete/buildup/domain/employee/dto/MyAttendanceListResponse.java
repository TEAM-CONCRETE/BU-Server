package com.concrete.buildup.domain.employee.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;

/**
 * 근로자 본인 출퇴근 내역 응답 DTO
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Schema(description = "본인 출퇴근 내역 응답")
@Getter
@Builder
public class MyAttendanceListResponse {

    @Schema(description = "출퇴근 내역 목록")
    private List<MyAttendanceSummary> content;

    @Schema(description = "현재 페이지 번호", example = "0")
    private int pageNumber;

    @Schema(description = "페이지 크기", example = "20")
    private int pageSize;

    @Schema(description = "전체 요소 수", example = "45")
    private long totalElements;

    @Schema(description = "전체 페이지 수", example = "3")
    private int totalPages;

    @Schema(description = "첫 페이지 여부", example = "true")
    private boolean first;

    @Schema(description = "마지막 페이지 여부", example = "false")
    private boolean last;

    /**
     * Page 객체로부터 응답 생성
     */
    public static MyAttendanceListResponse from(Page<MyAttendanceSummary> page) {
        return MyAttendanceListResponse.builder()
                .content(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    /**
     * 개별 출퇴근 기록 요약
     */
    @Schema(description = "출퇴근 기록 요약")
    @Getter
    @Builder
    public static class MyAttendanceSummary {

        @Schema(description = "출퇴근 기록 ID", example = "123")
        private Long attendanceId;

        @Schema(description = "날짜", example = "2025-11-24")
        private LocalDate date;

        @Schema(description = "현장 ID", example = "1")
        private Long siteId;

        @Schema(description = "현장명", example = "강남 현장")
        private String siteName;

        @Schema(description = "출근 시간", example = "08:55")
        private String checkInTime;

        @Schema(description = "퇴근 시간 (퇴근 전이면 null)", example = "18:10")
        private String checkOutTime;

        @Schema(description = "근태 상태 (WORKING: 근무중, COMPLETED: 퇴근완료, ABSENT: 결근)", example = "COMPLETED")
        private String status;

        @Schema(description = "지각 여부", example = "false")
        private Boolean isLate;
    }
}
