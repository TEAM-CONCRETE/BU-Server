package com.concrete.buildup.global.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * WebClient 설정
 * 외부 API 호출을 위한 WebClient Bean을 구성합니다.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    private final FaceSimilarityProperties faceSimilarityProperties;

    /**
     * Face Similarity API 호출을 위한 WebClient
     * Railway에 배포된 얼굴 유사도 검증 서비스와 통신합니다.
     *
     * 설정:
     * - 연결 타임아웃: 10초
     * - 응답 타임아웃: 30초
     * - 읽기 타임아웃: 30초
     * - 쓰기 타임아웃: 10초
     * - API 키 헤더 (선택사항)
     */
    @Bean
    public WebClient faceSimilarityWebClient() {
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, faceSimilarityProperties.getConnectTimeoutMs())
            .responseTimeout(Duration.ofSeconds(faceSimilarityProperties.getReadTimeoutS()))
            .doOnConnected(conn -> conn
                .addHandlerLast(new ReadTimeoutHandler(faceSimilarityProperties.getReadTimeoutS(), TimeUnit.SECONDS))
                .addHandlerLast(new WriteTimeoutHandler(faceSimilarityProperties.getWriteTimeoutS(), TimeUnit.SECONDS))
            );

        WebClient.Builder builder = WebClient.builder()
            .baseUrl(faceSimilarityProperties.getBaseUrl())
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .filter(logRequest())
            .filter(logResponse());

        // API 키가 설정되어 있으면 헤더에 추가
        if (faceSimilarityProperties.getApiKey() != null && !faceSimilarityProperties.getApiKey().isEmpty()) {
            builder.defaultHeader("X-API-Key", faceSimilarityProperties.getApiKey());
            log.info("Face Similarity API Key가 설정되었습니다.");
        }

        log.info("Face Similarity WebClient 초기화 완료 - baseUrl: {}", faceSimilarityProperties.getBaseUrl());
        return builder.build();
    }

    /**
     * 요청 로깅 필터
     */
    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.debug("Face API 요청 - {} {}", clientRequest.method(), clientRequest.url());
            return Mono.just(clientRequest);
        });
    }

    /**
     * 응답 로깅 필터
     */
    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            log.debug("Face API 응답 - Status: {}", clientResponse.statusCode());
            return Mono.just(clientResponse);
        });
    }
}
