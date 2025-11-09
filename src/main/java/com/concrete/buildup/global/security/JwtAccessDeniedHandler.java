package com.concrete.buildup.global.security;

import com.concrete.buildup.global.common.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * JWT 권한 부족 핸들러
 *
 * 인증은 되었지만 요청한 리소스에 대한 권한이 없을 때 호출됩니다.
 * 403 Forbidden 응답을 반환합니다.
 *
 * 발생 상황:
 * - 로그인은 되어 있지만 해당 API에 접근할 권한이 없는 경우
 * - 예: EMPLOYEE 역할인데 MANAGER 전용 API 호출 시
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {

        log.warn("권한 부족 - URI: {}, 메시지: {}",
            request.getRequestURI(),
            accessDeniedException.getMessage());

        // 403 Forbidden 응답 설정
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        // ApiResponse 형식으로 에러 응답 생성
        ApiResponse<Object> errorResponse = ApiResponse.error(
            "접근 권한이 없습니다. 관리자에게 문의하세요."
        );

        // JSON 응답 작성
        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
    }
}