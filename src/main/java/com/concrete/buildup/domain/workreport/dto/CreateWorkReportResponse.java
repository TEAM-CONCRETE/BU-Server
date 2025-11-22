package com.concrete.buildup.domain.workreport.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 작업일보 생성 응답 DTO
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "작업일보 생성 응답")
public class CreateWorkReportResponse {

    @Schema(description = "생성된 작업일보 ID", example = "123")
    private Long workReportId;

    @Schema(description = "생성된 PDF URL", example = "https://build-up-contracts.s3.ap-northeast-2.amazonaws.com/work-reports/1/2025-11-19/WR-2025-11-19-1.pdf")
    private String pdfUrl;

    @Schema(description = "응답 메시지", example = "작업일보가 생성되었습니다.")
    private String message;
}
