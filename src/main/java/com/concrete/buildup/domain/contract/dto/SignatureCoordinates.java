package com.concrete.buildup.domain.contract.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 서명 이미지 좌표 정보 DTO
 *
 * <p>프론트엔드에서 서명한 위치의 뷰포트 좌표와 크기 정보를 전달받습니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "서명 이미지 좌표 정보")
public class SignatureCoordinates {

    @NotNull(message = "X 좌표는 필수입니다")
    @Positive(message = "X 좌표는 양수여야 합니다")
    @Schema(description = "서명 이미지 X 좌표 (뷰포트 기준)", example = "100.0", required = true)
    private Double x;

    @NotNull(message = "Y 좌표는 필수입니다")
    @Positive(message = "Y 좌표는 양수여야 합니다")
    @Schema(description = "서명 이미지 Y 좌표 (뷰포트 기준)", example = "200.0", required = true)
    private Double y;

    @NotNull(message = "너비는 필수입니다")
    @Positive(message = "너비는 양수여야 합니다")
    @Schema(description = "서명 이미지 너비 (픽셀)", example = "150.0", required = true)
    private Double width;

    @NotNull(message = "높이는 필수입니다")
    @Positive(message = "높이는 양수여야 합니다")
    @Schema(description = "서명 이미지 높이 (픽셀)", example = "50.0", required = true)
    private Double height;

    @NotNull(message = "뷰포트 너비는 필수입니다")
    @Positive(message = "뷰포트 너비는 양수여야 합니다")
    @Schema(description = "뷰포트 너비 (픽셀)", example = "800.0", required = true)
    private Double viewWidth;

    @NotNull(message = "뷰포트 높이는 필수입니다")
    @Positive(message = "뷰포트 높이는 양수여야 합니다")
    @Schema(description = "뷰포트 높이 (픽셀)", example = "1131.0", required = true)
    private Double viewHeight;
}
