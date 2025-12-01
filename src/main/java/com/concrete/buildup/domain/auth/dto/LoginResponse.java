package com.concrete.buildup.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 로그인 응답 DTO
 *
 * <p>로그인 성공 시 사용자 정보를 반환합니다.</p>
 * <p>Access Token과 Refresh Token은 HttpOnly 쿠키로 전달됩니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "로그인 응답")
public class LoginResponse {

    @Schema(description = "사용자 ID", example = "testuser001")
    private String userId;

    @Schema(description = "사용자 이름 (근로자명, 관리자명, 기업명)", example = "홍길동")
    private String userName;

    @Schema(description = "사용자 역할", example = "ROLE_EMPLOYEE")
    private String role;

    @Schema(description = "프로필 완성 여부 (Phase1 회원가입 구분용)", example = "true")
    private Boolean profileCompleted;

    @Schema(description = "Access Token 만료 시간 (초)", example = "3600")
    private Long expiresIn;

    @Schema(description = "근로자 ID (ROLE_EMPLOYEE인 경우만)", example = "1")
    private Long employeeId;

    @Schema(description = "현장 관리자 ID (ROLE_MANAGER인 경우만)", example = "1")
    private Long managerId;

    @Schema(description = "현장 ID (ROLE_MANAGER인 경우만)", example = "1")
    private Long siteId;

    @Schema(description = "필수 개인정보 완성 여부 (ROLE_EMPLOYEE인 경우만) - 주민등록번호, 전화번호, 이메일, 주소, 비상연락망이 모두 입력된 경우 true", example = "true")
    private Boolean hasRequiredInfo;

    @Schema(description = "프로필 이미지 등록 여부 (ROLE_EMPLOYEE인 경우만) - 얼굴 인식 출근을 위한 프로필 이미지가 등록된 경우 true", example = "true")
    private Boolean hasProfileImage;
}
