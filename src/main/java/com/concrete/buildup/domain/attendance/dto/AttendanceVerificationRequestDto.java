package com.concrete.buildup.domain.attendance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 출퇴근 검증 요청 DTO (Presigned URL 방식)
 *
 * <p>클라이언트가 Presigned URL을 통해 S3에 업로드한 이미지를 기반으로
 * 얼굴 인식을 통한 출퇴근 검증을 요청할 때 사용합니다.</p>
 *
 * <p>출퇴근 유형(CHECK_IN/CHECK_OUT)은 요청에 포함하지 않으며,
 * 서버가 당일 마지막 기록을 조회하여 자동으로 판단합니다.</p>
 *
 * <p>워크플로우:</p>
 * <ol>
 *   <li>근로자가 공용 태블릿에서 전화번호 입력</li>
 *   <li>얼굴 이미지 촬영 및 S3 업로드</li>
 *   <li>이 API 호출 (전화번호 + uploadId 전달)</li>
 * </ol>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "출퇴근 검증 요청 (Presigned URL 방식)")
public class AttendanceVerificationRequestDto {

    /**
     * 근로자 전화번호
     */
    @NotBlank(message = "전화번호는 필수입니다")
    @Schema(description = "근로자 전화번호 (하이픈 포함 또는 제외)", example = "010-1234-5678", required = true)
    private String phoneNumber;

    /**
     * S3 객체 키 (업로드 ID)
     *
     * <p>클라이언트가 Presigned URL을 통해 업로드한 이미지의 S3 객체 키입니다.
     * 형식: attendance/{현장ID}/{근로자ID}/{timestamp}.jpg</p>
     */
    @NotBlank(message = "업로드 ID는 필수입니다")
    @Schema(description = "S3 객체 키 (Presigned URL로 업로드한 파일의 키)",
            example = "attendance/174/1/1735708800000.jpg",
            required = true)
    private String uploadId;
}
