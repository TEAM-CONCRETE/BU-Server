package com.concrete.buildup.domain.workreport.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;

/**
 * 작업일보 목록 조회 응답 DTO (현장 관리자용)
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@Builder
@Schema(description = "작업일보 목록 응답")
public class WorkReportListResponse {

    @Schema(description = "작업일보 목록")
    private List<WorkReportSummary> content;

    @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
    private int pageNumber;

    @Schema(description = "페이지 크기", example = "20")
    private int pageSize;

    @Schema(description = "전체 요소 수", example = "100")
    private long totalElements;

    @Schema(description = "전체 페이지 수", example = "5")
    private int totalPages;

    @Schema(description = "첫 페이지 여부", example = "true")
    private boolean first;

    @Schema(description = "마지막 페이지 여부", example = "false")
    private boolean last;

    /**
     * 작업일보 요약 정보
     */
    @Getter
    @Builder
    @Schema(description = "작업일보 요약 정보")
    public static class WorkReportSummary {

        @Schema(description = "작업일보 ID", example = "1")
        private Long workReportId;

        @Schema(description = "작업일자", example = "2025-11-24")
        private LocalDate workDate;

        @Schema(description = "작성자 이름", example = "김현장")
        private String writerName;
    }

    /**
     * Page 객체로부터 응답 DTO 생성
     */
    public static WorkReportListResponse from(Page<WorkReportSummary> page) {
        return WorkReportListResponse.builder()
                .content(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
