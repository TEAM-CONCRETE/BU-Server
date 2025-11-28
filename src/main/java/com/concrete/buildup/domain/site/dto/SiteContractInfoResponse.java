package com.concrete.buildup.domain.site.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 근로계약서 작성을 위한 기업/현장 정보 응답 DTO
 *
 * <p>현장 관리자가 근로계약서 작성 시 필요한 기업 및 현장 정보를 제공합니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "근로계약서 작성용 기업/현장 정보 응답")
public class SiteContractInfoResponse {

    @Schema(description = "현장 ID", example = "1")
    private Long siteId;

    @Schema(description = "현장명", example = "세종대학교 AI 센터 재개발 현장")
    private String siteName;

    @Schema(description = "현장 주소", example = "서울시 광진구 능동로 98")
    private String siteAddress;

    @Schema(description = "기업 정보")
    private CorporationInfo corporation;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "기업 정보")
    public static class CorporationInfo {

        @Schema(description = "기업 ID", example = "1")
        private Long corporationId;

        @Schema(description = "사업주 (회사명)", example = "빌드업건설(주)")
        private String corpName;

        @Schema(description = "대표자명", example = "김대표")
        private String corpCeoName;

        @Schema(description = "기업 주소", example = "서울시 강남구 테헤란로 123")
        private String corpAddress;
    }
}
