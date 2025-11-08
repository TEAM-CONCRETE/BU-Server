package com.concrete.buildup.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI(Swagger) 설정
 *
 * <p>Swagger UI를 통한 API 문서화를 설정합니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI buildUpOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Build-Up Platform API")
                        .description("건설 현장 관리 플랫폼 Build-Up의 백엔드 API 문서입니다.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Build-Up Team")
                                .email("team-concrete@example.com")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080/api")
                                .description("로컬 개발 서버"),
                        new Server()
                                .url("https://api.buildup.com")
                                .description("운영 서버 (예시)")
                ));
    }

    /**
     * 전체 API 그룹 설정
     *
     * <p>모든 API 엔드포인트를 하나의 그룹으로 문서화합니다.</p>
     */
    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("all")
                .pathsToMatch("/v1/**")
                .build();
    }
}