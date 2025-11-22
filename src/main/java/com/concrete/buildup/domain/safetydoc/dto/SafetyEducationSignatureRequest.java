package com.concrete.buildup.domain.safetydoc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SafetyEducationSignatureRequest {

    @NotBlank(message = "서명 이미지 S3 키는 필수입니다.")
    private String signatureS3Key;

    @NotBlank(message = "클라이언트 해시값은 필수입니다.")
    private String clientHash;

    @NotNull(message = "서명 좌표 정보는 필수입니다.")
    private SignatureCoordinatesDto coordinates;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SignatureCoordinatesDto {
        private Double x;
        private Double y;
        private Double width;
        private Double height;
        private Double viewWidth;
        private Double viewHeight;
    }
}
