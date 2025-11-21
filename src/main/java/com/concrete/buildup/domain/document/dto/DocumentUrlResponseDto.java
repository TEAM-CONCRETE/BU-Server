package com.concrete.buildup.domain.document.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 문서 PDF Signed URL 응답 DTO
 */
@Getter
@Builder
@Schema(description = "문서 PDF 조회 응답")
public class DocumentUrlResponseDto {

    @Schema(description = "PDF 다운로드용 Signed URL",
            example = "https://s3.ap-northeast-2.amazonaws.com/bucket/contracts/123/signed_final.pdf?X-Amz-Algorithm=...")
    private String url;

    @Schema(description = "URL 만료 시간", example = "2025-12-01T10:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expiresAt;

    public static DocumentUrlResponseDto of(String url, LocalDateTime expiresAt) {
        return DocumentUrlResponseDto.builder()
                .url(url)
                .expiresAt(expiresAt)
                .build();
    }
}