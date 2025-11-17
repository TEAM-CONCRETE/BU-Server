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

    @JsonIgnore  // Access Token은 HttpOnly 쿠키로 전달되므로 JSON 응답에서 제외
    @Schema(description = "Access Token (JWT)", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @Schema(description = "사용자 ID", example = "testuser001")
    private String userId;

    @Schema(description = "사용자 역할 (EMPLOYEE, MANAGER, ADMIN)", example = "EMPLOYEE")
    private String role;

    @Schema(description = "Access Token 만료 시간 (초)", example = "3600")
    private Long expiresIn;
}
