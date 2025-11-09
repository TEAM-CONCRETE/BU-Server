package com.concrete.buildup.global.config;

import com.concrete.buildup.global.security.JwtAccessDeniedHandler;
import com.concrete.buildup.global.security.JwtAuthenticationEntryPoint;
import com.concrete.buildup.global.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
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
 * - 예외 처리 (401/403)
 * - 프로파일별 보안 설정 (dev/test/local: 모든 API 허용, prod: JWT 인증)
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final Environment environment;

    /**
     * Security Filter Chain 설정
     * Spring Security 6.x+ 방식 사용
     *
     * 프로파일별 설정:
     * - dev/test/local: 모든 API 인증 없이 접근 가능 (다중 프로파일 지원)
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
        // Environment.acceptsProfiles()를 사용하여 다중 프로파일 설정 지원
        // 예: spring.profiles.active=dev,local 에서도 정상 동작
        if (isDevelopmentMode()) {
            // dev/test/local 프로파일: 모든 API 허용 (인증/인가 구현 완료까지)
            http.authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            );
        } else {
            // prod 프로파일: JWT 인증 필터 및 예외 처리 적용
            http
                // 예외 처리 설정
                .exceptionHandling(exception -> exception
                    // 인증 실패 시 401 응답 (토큰 없음, 토큰 만료, 토큰 유효하지 않음)
                    .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                    // 권한 부족 시 403 응답 (인증은 되었으나 권한 없음)
                    .accessDeniedHandler(jwtAccessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                    // 공개 엔드포인트
                    .requestMatchers(
                        "/v1/auth/**",           // 인증 관련 API
                        "/v1/public/**",         // 공개 API
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
     * 개발 모드 여부 확인
     *
     * 다중 프로파일 설정 지원:
     * - spring.profiles.active=dev → true
     * - spring.profiles.active=dev,local → true
     * - spring.profiles.active=local,dev → true
     * - spring.profiles.active=prod → false
     *
     * @return dev, test, local 프로파일 중 하나라도 활성화되어 있으면 true
     */
    private boolean isDevelopmentMode() {
        return environment.acceptsProfiles(
            org.springframework.core.env.Profiles.of("dev", "test", "local")
        );
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
