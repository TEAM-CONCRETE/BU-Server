package com.concrete.buildup.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.List;

/**
 * Web MVC 설정
 * - HTTP Message Converter 설정
 * - Interceptor 설정
 * - 기타 웹 관련 설정
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Interceptor 등록
     * 예: 로깅, 인증, 권한 체크 등
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // TODO: 필요한 Interceptor 등록
        // registry.addInterceptor(new LoggingInterceptor())
        //     .addPathPatterns("/api/**")
        //     .excludePathPatterns("/api/auth/**");
    }

    /**
     * HTTP Message Converter 설정
     * JSON 직렬화/역직렬화 설정
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // ObjectMapper 설정
        ObjectMapper objectMapper = new ObjectMapper();

        // Java 8 날짜/시간 API 지원
        objectMapper.registerModule(new JavaTimeModule());

        // 날짜를 타임스탬프가 아닌 ISO-8601 형식으로 직렬화
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // null 값을 가진 필드 제외 (선택사항)
        // objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // 알 수 없는 프로퍼티 무시 (선택사항)
        // objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // Jackson HTTP Message Converter 등록
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);
        converters.add(0, converter);
    }
}
