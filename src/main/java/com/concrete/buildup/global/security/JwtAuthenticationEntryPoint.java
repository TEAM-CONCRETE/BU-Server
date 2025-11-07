package com.concrete.buildup.global.security;

import com.concrete.buildup.global.common.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * JWT 인증 실패 핸들러
 *
 * 인증되지 않은 사용자가 보호된 리소스에 접근할 때 호출됩니다.
 * 401 Unauthorized 응답을 반환합니다.
 *
 * 발생 상황:
 * - JWT 토큰이 없는 경우
 * - JWT 토큰이 유효하지 않은 경우
 * - JWT 토큰이 만료된 경우
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {

        log.warn("인증 실패 - URI: {}, 메시지: {}",
            request.getRequestURI(),
            authException.getMessage());

        // 401 Unauthorized 응답 설정
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        // ApiResponse 형식으로 에러 응답 생성
        ApiResponse<Object> errorResponse = ApiResponse.error(
            "인증이 필요합니다. 로그인 후 다시 시도해주세요."
        );

        // JSON 응답 작성
        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
    }
}