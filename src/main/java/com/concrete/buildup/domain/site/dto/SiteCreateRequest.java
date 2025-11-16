package com.concrete.buildup.domain.site.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 현장 생성 요청 DTO
 *
 * <p>건설 현장 등록 시 사용되는 요청 DTO입니다.</p>
 * <p>기업 관리자가 새로운 현장을 등록할 때 사용합니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "현장 등록 요청")
public class SiteCreateRequest {

    @NotBlank(message = "현장명은 필수입니다")
    @Schema(description = "현장명", example = "강남 재개발 현장", required = true)
    private String siteName;

    @Schema(description = "현장 주소", example = "서울시 강남구 테헤란로 123")
    private String siteAddress;

    @Schema(description = "발주처 (클라이언트)", example = "서울시청")
    private String clientName;

    @Schema(description = "공사 시작일", example = "2025-01-01")
    private LocalDate startDate;

    @Schema(description = "공사 종료일", example = "2025-12-31")
    private LocalDate endDate;
}
