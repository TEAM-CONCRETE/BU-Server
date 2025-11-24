package com.concrete.buildup.domain.employee.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 급여명세서 PDF 미리보기 응답 DTO
 */
@Schema(description = "급여명세서 PDF 미리보기 응답")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyPayrollPdfResponse {

    @Schema(description = "급여 ID", example = "1")
    private Long payrollId;

    @Schema(description = "PDF 미리보기 URL (15분간 유효)", example = "https://bucket.s3.amazonaws.com/payroll/...")
    private String pdfUrl;

    @Schema(description = "URL 만료 시각", example = "2025-01-25T12:30:00")
    private LocalDateTime expiresAt;
}
