package com.concrete.buildup.domain.attendance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 얼굴 이미지 등록 요청 DTO
 * Presigned URL 방식으로 S3에 업로드된 이미지의 uploadId를 전달받습니다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "얼굴 이미지 등록 요청")
public class FaceRegistrationRequestDto {

    @NotBlank(message = "uploadId는 필수입니다.")
    @Schema(
        description = "S3 객체 키 (Presigned URL로 업로드한 이미지의 S3 경로)",
        example = "uploads/employee-profiles/123/profile.jpg",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String uploadId;
}
