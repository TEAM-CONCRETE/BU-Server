package com.concrete.buildup.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Face Similarity API 설정 프로퍼티
 * application.yml의 ai.face-similarity 설정을 바인딩합니다.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ai.face-similarity")
public class FaceSimilarityProperties {

    /**
     * Face Similarity API Base URL
     * 기본값: Railway 배포 서버
     */
    private String baseUrl;

    /**
     * 연결 타임아웃 (밀리초)
     * 기본값: 10000 (10초)
     */
    private Integer connectTimeoutMs;

    /**
     * 읽기 타임아웃 (초)
     * 기본값: 30초
     */
    private Integer readTimeoutS;

    /**
     * 쓰기 타임아웃 (초)
     * 기본값: 10초
     */
    private Integer writeTimeoutS;

    /**
     * API 키 (선택사항)
     * 환경변수 AI_FACE_SIMILARITY_API_KEY로 주입
     */
    private String apiKey;
}
