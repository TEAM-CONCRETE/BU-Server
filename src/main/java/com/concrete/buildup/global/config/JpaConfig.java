package com.concrete.buildup.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/**
 * JPA 설정
 * - JPA Auditing 활성화
 * - AuditorAware 설정 (생성자/수정자 자동 저장)
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {

    /**
     * AuditorAware Bean 설정
     * 엔티티의 생성자/수정자를 자동으로 저장하기 위한 설정
     *
     * 현재는 "system"으로 고정되어 있으며,
     * 추후 Spring Security의 인증 정보를 활용하여 실제 사용자 정보를 반환하도록 수정 필요
     *
     * 예시 (Spring Security 연동):
     * return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
     *     .filter(Authentication::isAuthenticated)
     *     .map(Authentication::getName);
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        // TODO: Spring Security 인증 정보 연동
        return () -> Optional.of("system");
    }
}
