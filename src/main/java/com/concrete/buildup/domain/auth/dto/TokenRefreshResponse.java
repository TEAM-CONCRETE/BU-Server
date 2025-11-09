package com.concrete.buildup.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 토큰 재발급 응답 DTO
 *
 * <p>Refresh Token을 사용하여 새로운 Access Token과 Refresh Token을 재발급한 결과를 반환합니다.</p>
 * <p>새로운 Refresh Token은 HttpOnly 쿠키로 전달됩니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "토큰 재발급 응답")
public class TokenRefreshResponse {

    @Schema(description = "새로운 Access Token (JWT)", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @Schema(description = "Access Token 만료 시간 (초)", example = "3600")
    private Long expiresIn;
}