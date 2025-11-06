package com.concrete.buildup.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 로그인 결과 내부 DTO
 *
 * <p>Service와 Controller 간의 데이터 전달용 내부 DTO입니다.</p>
 * <p>API 응답에는 loginResponse만 포함되고, refreshToken은 HttpOnly 쿠키로 전달됩니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResult {

    /**
     * 로그인 응답 (API Body로 전달)
     */
    private LoginResponse loginResponse;

    /**
     * Refresh Token (HttpOnly 쿠키로 전달, 평문)
     */
    private String refreshToken;

    /**
     * Refresh Token 만료 시간 (초)
     */
    private Long refreshTokenMaxAge;
}
