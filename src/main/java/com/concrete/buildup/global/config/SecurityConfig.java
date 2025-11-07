package com.concrete.buildup.global.config;

import com.concrete.buildup.global.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
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
 * - JWT 인증 필터
 * - 프로파일별 보안 설정 (dev: 모든 API 허용, prod: JWT 인증)
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${spring.profiles.active:prod}")
    private String activeProfile;

    /**
     * Security Filter Chain 설정
     * Spring Security 6.x+ 방식 사용
     *
     * 프로파일별 설정:
     * - dev: 모든 API 인증 없이 접근 가능
     * - prod: JWT 인증 필터 적용
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
            );

        // 프로파일별 인증 설정
        if ("dev".equals(activeProfile) || "test".equals(activeProfile)) {
            // dev/test 프로파일: 모든 API 허용 (인증/인가 구현 완료까지)
            http.authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            );
        } else {
            // prod 프로파일: JWT 인증 필터 적용
            http
                .authorizeHttpRequests(auth -> auth
                    // 공개 엔드포인트
                    .requestMatchers(
                        "/api/v1/auth/**",       // 인증 관련 API
                        "/api/v1/public/**",     // 공개 API
                        "/swagger-ui/**",        // Swagger UI
                        "/v3/api-docs/**",       // Swagger API Docs
                        "/actuator/health"       // Health Check
                    ).permitAll()

                    // 그 외 모든 요청은 인증 필요
                    .anyRequest().authenticated()
                )
                // JWT 인증 필터 추가
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        }

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
