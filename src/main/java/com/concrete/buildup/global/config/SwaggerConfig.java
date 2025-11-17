package com.concrete.buildup.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger/OpenAPI 설정
 *
 * - Swagger UI: http://localhost:8080/api/swagger-ui/index.html
 * - OpenAPI JSON: http://localhost:8080/api/v3/api-docs
 */
@Configuration
public class SwaggerConfig {

    @Value("${spring.application.name:Build-Up Platform}")
    private String applicationName;

    /**
     * OpenAPI 설정
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(servers())
                .components(securitySchemes())
                .addSecurityItem(securityRequirement());
    }

    /**
     * API 기본 정보
     */
    private Info apiInfo() {
        return new Info()
                .title(applicationName + " API")
                .description("""
                        Build-Up Platform REST API 문서입니다.

                        ## 주요 기능
                        - 인증/인가: 회원가입, 로그인, JWT 토큰 관리
                        - 사원 관리: 사원 등록, 조회, 수정, 삭제
                        - 현장 관리: 현장 등록, 조회, 관리
                        - 계약 관리: 근로계약서 생성, 전자서명
                        - 급여 관리: 급여 계산, 명세서 PDF 생성
                        - 근태 관리: 출퇴근 기록, 근태 조회
                        - 파일 업로드: S3 Presigned URL 발급

                        ## 인증 방법
                        Bearer Authentication을 사용합니다. 로그인 후 발급받은 JWT 토큰을 헤더에 포함하세요.
                        ```
                        Authorization: Bearer {your-jwt-token}
                        ```
                        """)
                .version("v1.0.0")
                .contact(new Contact()
                        .name("TEAM CONCRETE")
                        .email("support@build-up.kr")
                        .url("https://build-up.kr"))
                .license(new License()
                        .name("Apache 2.0")
                        .url("https://www.apache.org/licenses/LICENSE-2.0.html"));
    }

    /**
     * 서버 정보
     */
    private List<Server> servers() {
        Server localServer = new Server()
                .url("http://localhost:8080/api")
                .description("로컬 개발 서버");

        Server awsDevServer = new Server()
                .url("http://3.37.234.173:8080/api")
                .description("AWS 개발 서버");

        Server prodServer = new Server()
                .url("https://api.build-up.kr")
                .description("운영 서버");

        return List.of(localServer, awsDevServer, prodServer);
    }

    /**
     * 보안 스키마 설정
     */
    private Components securitySchemes() {
        return new Components()
                .addSecuritySchemes("Bearer Authentication",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT 토큰을 입력하세요 (Bearer 제외)"))
                .addSecuritySchemes("Cookie Authentication",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("accessToken")
                                .description("로그인 시 자동으로 설정되는 쿠키 인증"));
    }

    /**
     * 보안 요구사항
     */
    private SecurityRequirement securityRequirement() {
        return new SecurityRequirement()
                .addList("Bearer Authentication")
                .addList("Cookie Authentication");
    }
}