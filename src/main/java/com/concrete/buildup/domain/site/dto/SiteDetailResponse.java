package com.concrete.buildup.domain.site.dto;

import com.concrete.buildup.domain.site.entity.Site;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/**
 * 현장 상세 조회 응답 DTO
 *
 * <p>현장 상세 정보 조회 시 반환되는 응답 DTO입니다.</p>
 * <p>작업일보 작성 페이지 등에서 현장 기본 정보를 표시하는 데 사용됩니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@AllArgsConstructor
@Builder
@Schema(description = "현장 상세 조회 응답")
public class SiteDetailResponse {

    @Schema(description = "현장 ID", example = "1")
    private Long siteId;

    @Schema(description = "현장명", example = "강남 재개발 현장")
    private String siteName;

    @Schema(description = "현장 주소", example = "서울시 강남구 테헤란로 123")
    private String siteAddress;

    @Schema(description = "발주처 (클라이언트)", example = "서울시청")
    private String clientName;

    @Schema(description = "공사 시작일", example = "2025-01-01")
    private LocalDate startDate;

    @Schema(description = "공사 종료일", example = "2025-12-31")
    private LocalDate endDate;

    @Schema(description = "현장 관리자 이름", example = "김관리")
    private String managerName;

    /**
     * Entity로부터 응답 DTO 생성
     *
     * @param site Site 엔티티
     * @return SiteDetailResponse
     */
    public static SiteDetailResponse from(Site site) {
        return SiteDetailResponse.builder()
            .siteId(site.getId())
            .siteName(site.getSiteName())
            .siteAddress(site.getSiteAddress())
            .clientName(site.getClientName())
            .startDate(site.getStartDate())
            .endDate(site.getEndDate())
            .managerName(site.getManager() != null ? site.getManager().getManagerName() : null)
            .build();
    }
}
