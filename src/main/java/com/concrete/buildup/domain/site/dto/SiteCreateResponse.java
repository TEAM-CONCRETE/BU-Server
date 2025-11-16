package com.concrete.buildup.domain.site.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/**
 * 현장 생성 응답 DTO
 *
 * <p>현장 등록 성공 시 반환되는 응답 DTO입니다.</p>
 * <p>생성된 현장 정보와 시크릿키 정보를 포함합니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@AllArgsConstructor
@Builder
@Schema(description = "현장 등록 응답")
public class SiteCreateResponse {

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

    @Schema(description = "현장 관리자용 시크릿키", example = "CONC-1-XYZ-9205")
    private String managerSecretKey;

    @Schema(description = "근로자용 시크릿키", example = "CONC-1-ABC-1234")
    private String employeeSecretKey;

    /**
     * Entity로부터 응답 DTO 생성
     */
    public static SiteCreateResponse of(Long siteId, String siteName, String siteAddress,
                                         String clientName, LocalDate startDate, LocalDate endDate,
                                         String managerSecretKey, String employeeSecretKey) {
        return SiteCreateResponse.builder()
            .siteId(siteId)
            .siteName(siteName)
            .siteAddress(siteAddress)
            .clientName(clientName)
            .startDate(startDate)
            .endDate(endDate)
            .managerSecretKey(managerSecretKey)
            .employeeSecretKey(employeeSecretKey)
            .build();
    }
}
