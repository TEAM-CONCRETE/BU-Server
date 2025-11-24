package com.concrete.buildup.domain.site.dto;

import com.concrete.buildup.domain.site.entity.Site;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 현장 목록 조회 응답 DTO
 *
 * <p>기업 관리자가 소속 현장 목록을 조회할 때 사용됩니다.</p>
 */
@Schema(description = "현장 목록 응답")
@Getter
@Builder
public class SiteListResponse {

    @Schema(description = "현장 목록")
    private List<SiteSummary> sites;

    @Schema(description = "전체 현장 수", example = "3")
    private int totalCount;

    /**
     * 현장 요약 정보
     */
    @Schema(description = "현장 요약 정보")
    @Getter
    @Builder
    public static class SiteSummary {

        @Schema(description = "현장 ID", example = "1")
        private Long siteId;

        @Schema(description = "현장명", example = "강남 오피스텔 신축현장")
        private String siteName;

        @Schema(description = "현장 주소", example = "서울특별시 강남구 역삼동 123-45")
        private String siteAddress;

        @Schema(description = "현장 관리자 이름", example = "이강남")
        private String managerName;

        /**
         * Site 엔티티를 SiteSummary DTO로 변환
         */
        public static SiteSummary from(Site site) {
            return SiteSummary.builder()
                    .siteId(site.getId())
                    .siteName(site.getSiteName())
                    .siteAddress(site.getSiteAddress())
                    .managerName(site.getManager() != null ? site.getManager().getManagerName() : null)
                    .build();
        }
    }

    /**
     * Site 엔티티 리스트를 SiteListResponse로 변환
     */
    public static SiteListResponse from(List<Site> sites) {
        List<SiteSummary> summaries = sites.stream()
                .map(SiteSummary::from)
                .collect(Collectors.toList());

        return SiteListResponse.builder()
                .sites(summaries)
                .totalCount(summaries.size())
                .build();
    }
}
