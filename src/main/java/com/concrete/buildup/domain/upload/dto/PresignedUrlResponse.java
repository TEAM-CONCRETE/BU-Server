package com.concrete.buildup.domain.upload.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Presigned URL 발급 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Presigned URL 발급 응답")
public class PresignedUrlResponse {

    @Schema(description = "Presigned Upload URL (클라이언트가 이 URL로 PUT 요청하여 파일 업로드)",
            example = "https://build-up-contracts.s3.ap-northeast-2.amazonaws.com/uploads/contracts/123/EMPLOYEE.png?X-Amz-Algorithm=...")
    private String uploadUrl;

    @Schema(description = "URL 만료 시간", example = "2025-11-05T14:25:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expiresAt;

    @Schema(description = "S3 객체 키 (파일 경로)", example = "uploads/contracts/123/EMPLOYEE.png")
    private String s3Key;

    @Schema(description = "S3 버킷명", example = "build-up-contracts")
    private String bucket;
}