package com.concrete.buildup.domain.contract.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 계약 목록 조회 응답 DTO
 *
 * <p>계약 목록과 페이징 정보를 담는 응답 DTO입니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractListResponse {

    /**
     * 계약 목록
     */
    private List<ContractSummaryDto> items;

    /**
     * 페이징 정보
     */
    private PageInfo pageInfo;

    /**
     * 페이징 정보 DTO
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PageInfo {
        /**
         * 현재 페이지 번호 (1-based)
         */
        private int currentPage;

        /**
         * 페이지 크기
         */
        private int pageSize;

        /**
         * 전체 데이터 개수
         */
        private long totalElements;

        /**
         * 전체 페이지 개수
         */
        private int totalPages;

        /**
         * 다음 페이지 존재 여부
         */
        private boolean hasNext;

        /**
         * 이전 페이지 존재 여부
         */
        private boolean hasPrevious;
    }
}
