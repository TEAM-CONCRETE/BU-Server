package com.concrete.buildup.domain.upload.dto;

import com.concrete.buildup.domain.upload.enums.ResourceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 범용 Presigned URL 발급 요청 DTO
 * 얼굴 이미지, 프로필 사진 등 단순 파일 업로드에 사용됩니다.
 *
 * 기존 PresignedUrlRequest와의 차이점:
 * - resourceId 불필요 (JWT에서 userId 자동 추출)
 * - signerRole 불필요 (서명이 아닌 일반 파일 업로드)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "범용 Presigned URL 발급 요청 (얼굴 이미지, 프로필 사진 등)")
public class SimplePresignedUrlRequest {

    @NotNull(message = "리소스 타입은 필수입니다.")
    @Schema(
        description = "리소스 타입",
        example = "EMPLOYEE_PROFILE",
        allowableValues = {"EMPLOYEE_PROFILE", "ATTENDANCE_PROBE", "WORK_REPORT", "SAFETY_DOC"}
    )
    private ResourceType resourceType;

    @Pattern(regexp = "^(png|jpg|jpeg|pdf)$", message = "지원하지 않는 파일 형식입니다. (png, jpg, jpeg, pdf만 가능)")
    @Schema(
        description = "파일 확장자 (기본값: jpg)",
        example = "jpg",
        allowableValues = {"png", "jpg", "jpeg", "pdf"}
    )
    private String fileExtension;

    @Schema(
        description = "현장 ID (ATTENDANCE_PROBE인 경우 필수)",
        example = "174"
    )
    private Long siteId;

    @Schema(
        description = "근로자 ID (ATTENDANCE_PROBE, EMPLOYEE_PROFILE인 경우 필수)",
        example = "1"
    )
    private Long employeeId;

    /**
     * fileExtension이 null인 경우 기본값 설정
     */
    public String getFileExtension() {
        return fileExtension != null ? fileExtension : "jpg";
    }

    /**
     * ATTENDANCE_PROBE 타입인 경우 siteId와 employeeId가 모두 필수
     */
    @AssertTrue(message = "ATTENDANCE_PROBE 타입인 경우 siteId와 employeeId가 모두 필요합니다.")
    private boolean isAttendanceProbeFieldsValid() {
        if (resourceType == ResourceType.ATTENDANCE_PROBE) {
            return siteId != null && employeeId != null;
        }
        return true;
    }

    /**
     * EMPLOYEE_PROFILE 타입인 경우 employeeId가 필수
     */
    @AssertTrue(message = "EMPLOYEE_PROFILE 타입인 경우 employeeId가 필요합니다.")
    private boolean isEmployeeProfileFieldsValid() {
        if (resourceType == ResourceType.EMPLOYEE_PROFILE) {
            return employeeId != null;
        }
        return true;
    }
}