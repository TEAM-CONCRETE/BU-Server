package com.concrete.buildup.domain.safetydoc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = """
        서명 요청 정보

        ## 서명 처리 전 필수 단계
        1. `/v1/uploads/signatures` API로 Presigned URL 발급
        2. 발급받은 URL로 서명 이미지(PNG) PUT 요청
        3. 업로드 완료 후 이 API 호출

        ## 해시 검증
        클라이언트에서 서명 이미지의 SHA256 해시를 계산하여 전송하면,
        서버에서 S3의 이미지를 다운로드 후 해시를 재계산하여 무결성을 검증합니다.
        """)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SafetyEducationSignatureRequest {

    @Schema(
            description = "S3에 업로드된 서명 이미지의 키. Presigned URL 발급 시 응답받은 s3Key 값을 사용",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "uploads/safety-education/1/signatures/MANAGER/1699000000000.png"
    )
    @NotBlank(message = "서명 이미지 S3 키는 필수입니다.")
    private String signatureS3Key;

    @Schema(
            description = """
                    서명 이미지의 SHA256 해시값 (무결성 검증용).
                    클라이언트에서 서명 이미지 파일의 SHA256 해시를 계산하여 전송.
                    서버에서 동일한 방식으로 해시를 계산하여 일치 여부를 검증함.
                    """,
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
    )
    @NotBlank(message = "클라이언트 해시값은 필수입니다.")
    private String clientHash;

    @Schema(
            description = "PDF 내 서명 위치 좌표 정보. 클라이언트 화면에서의 좌표를 전송하면 서버에서 PDF 좌표로 변환",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "서명 좌표 정보는 필수입니다.")
    private SignatureCoordinatesDto coordinates;

    @Schema(description = "서명 좌표 정보 (클라이언트 뷰포트 기준)")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SignatureCoordinatesDto {

        @Schema(
                description = "서명 위치 X 좌표 (클라이언트 뷰포트 기준, 좌측 상단이 원점)",
                example = "400.0"
        )
        private Double x;

        @Schema(
                description = "서명 위치 Y 좌표 (클라이언트 뷰포트 기준, 좌측 상단이 원점)",
                example = "750.0"
        )
        private Double y;

        @Schema(
                description = "서명 이미지 너비 (px)",
                example = "150.0"
        )
        private Double width;

        @Schema(
                description = "서명 이미지 높이 (px)",
                example = "50.0"
        )
        private Double height;

        @Schema(
                description = "클라이언트 뷰포트 너비 (PDF 좌표 변환에 사용)",
                example = "595.0"
        )
        private Double viewWidth;

        @Schema(
                description = "클라이언트 뷰포트 높이 (PDF 좌표 변환에 사용)",
                example = "842.0"
        )
        private Double viewHeight;
    }
}
