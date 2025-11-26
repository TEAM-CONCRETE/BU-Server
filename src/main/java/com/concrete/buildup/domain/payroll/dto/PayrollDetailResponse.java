package com.concrete.buildup.domain.payroll.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 급여명세서 상세 조회 응답 DTO
 *
 * <p>급여명세서의 전체 정보를 담는 최상위 응답 객체입니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@Builder
@Schema(description = "급여명세서 상세 조회 응답")
public class PayrollDetailResponse {

    @Schema(description = "급여명세서 헤더 정보")
    private PayrollHeaderDto header;

    @Schema(description = "급여 집계 정보")
    private PayrollSummaryDto summary;

    @Schema(description = "근무일지 리스트 (페이징)")
    private PagedAttendances attendances;

    /**
     * 페이징된 근무일지 정보
     */
    @Getter
    @Builder
    @Schema(description = "페이징된 근무일지 정보")
    public static class PagedAttendances {

        @Schema(description = "근무일지 리스트")
        private List<AttendanceItemDto> content;

        @Schema(description = "전체 항목 수", example = "20")
        private Long totalElements;

        @Schema(description = "전체 페이지 수", example = "2")
        private Integer totalPages;

        @Schema(description = "현재 페이지 (0부터 시작)", example = "0")
        private Integer currentPage;

        @Schema(description = "페이지 크기", example = "10")
        private Integer size;
    }
}