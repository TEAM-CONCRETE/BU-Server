package com.concrete.buildup.domain.payroll.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 급여 내역 조회 - 요약 정보 응답 DTO
 *
 * <p>급여 내역 목록과 함께 전체 통계 정보를 포함합니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@Builder
public class SalaryHistorySummaryResponse {

    /**
     * 전체 급여 내역 건수
     */
    private Integer totalCount;

    /**
     * 미지급 건수
     */
    private Integer unpaidCount;

    /**
     * 총 지급액 (지급 완료된 금액의 합계)
     */
    private BigDecimal totalPaidAmount;

    /**
     * 급여 내역 목록
     */
    private List<SalaryHistoryItemResponse> data;
}