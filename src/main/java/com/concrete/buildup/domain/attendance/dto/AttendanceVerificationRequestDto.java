package com.concrete.buildup.domain.attendance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

/**
 * 출퇴근 검증 요청 DTO (백엔드 직접 처리 방식)
 *
 * <p>클라이언트가 얼굴 이미지를 직접 전송하여
 * 얼굴 인식을 통한 출퇴근 검증을 요청할 때 사용합니다.</p>
 *
 * <p>출퇴근 유형(CHECK_IN/CHECK_OUT)은 요청에 포함하지 않으며,
 * 서버가 당일 마지막 기록을 조회하여 자동으로 판단합니다.</p>
 *
 * <p>워크플로우:</p>
 * <ol>
 *   <li>근로자가 공용 태블릿에서 전화번호 입력</li>
 *   <li>얼굴 이미지 촬영</li>
 *   <li>이 API 호출 (전화번호 + 이미지 파일 직접 전송)</li>
 *   <li>백엔드에서 전화번호 → employeeId 조회</li>
 *   <li>백엔드에서 파일 검증 후 S3 업로드</li>
 *   <li>백엔드에서 AI API 호출 및 얼굴 비교</li>
 *   <li>검증 성공 시 출퇴근 기록 저장</li>
 * </ol>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "출퇴근 검증 요청 (백엔드 직접 처리 방식)")
public class AttendanceVerificationRequestDto {

    /**
     * 근로자 전화번호
     *
     * <p>백엔드에서 전화번호를 정규화하고 Employee를 조회합니다.</p>
     * <p>지원 형식: "010-1234-5678", "01012345678"</p>
     */
    @NotBlank(message = "전화번호는 필수입니다")
    @Schema(
        description = "근로자 전화번호 (하이픈 포함 또는 제외)",
        example = "010-1234-5678",
        required = true
    )
    private String phoneNumber;

    /**
     * 얼굴 이미지 파일
     *
     * <p>클라이언트가 촬영한 얼굴 이미지 파일입니다.</p>
     * <p>백엔드에서 다음을 검증합니다:</p>
     * <ul>
     *   <li>파일 크기: 최대 5MB</li>
     *   <li>파일 타입: image/jpeg, image/png만 허용</li>
     *   <li>이미지 포맷: JPEG/PNG 매직 넘버 검증</li>
     * </ul>
     */
    @NotNull(message = "얼굴 이미지는 필수입니다")
    @Schema(
        description = "얼굴 이미지 파일 (JPG, PNG)",
        required = true,
        type = "string",
        format = "binary"
    )
    private MultipartFile faceImage;
}
