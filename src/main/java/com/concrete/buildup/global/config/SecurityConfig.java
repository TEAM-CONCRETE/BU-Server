package com.concrete.buildup.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 설정
 * - CORS 설정
 * - CSRF 설정 (REST API용 비활성화)
 * - 인증/인가 설정
 * - 세션 관리
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Security Filter Chain 설정
     * Spring Security 6.x+ 방식 사용
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF 비활성화 (REST API 사용 시)
            .csrf(csrf -> csrf.disable())

            // CORS 설정 활성화
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // 세션 관리 - Stateless (JWT 사용 시)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // URL별 접근 권한 설정
            .authorizeHttpRequests(auth -> auth
                // 공개 엔드포인트
                .requestMatchers(
                    "/api/auth/**",           // 인증 관련 API
                    "/api/public/**",         // 공개 API
                    "/api/swagger-ui/**",     // Swagger UI
                    "/api/v3/api-docs/**",    // Swagger API Docs
                    "/api/actuator/health"    // Health Check
                ).permitAll()

                // 인증 필요 엔드포인트
                .requestMatchers(
                    "/api/v1/uploads/**"      // 파일 업로드 API (Presigned URL 발급)
                ).authenticated()

                // 그 외 모든 요청은 인증 필요
                .anyRequest().authenticated()
            )

            // HTTP Basic 인증 활성화 (개발 편의용, 운영에서는 제거 고려)
            .httpBasic(basic -> {});

        return http.build();
    }

    /**
     * CORS 설정
     * 프론트엔드 애플리케이션과의 통신을 위한 설정
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 허용할 Origin (개발/운영 환경에 따라 수정 필요)
        configuration.setAllowedOrigins(List.of(
            "http://localhost:3000",      // React 기본 포트
            "http://localhost:5173",      // Vite 기본 포트
            "http://localhost:4200"       // Angular 기본 포트
        ));

        // 허용할 HTTP 메서드
        configuration.setAllowedMethods(List.of(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        // 허용할 헤더
        configuration.setAllowedHeaders(List.of("*"));

        // 인증 정보 포함 허용
        configuration.setAllowCredentials(true);

        // preflight 요청 캐시 시간 (초)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    /**
     * 비밀번호 암호화를 위한 BCrypt Encoder
     * strength: 10 (기본값, 높을수록 보안 강화되나 성능 저하)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
