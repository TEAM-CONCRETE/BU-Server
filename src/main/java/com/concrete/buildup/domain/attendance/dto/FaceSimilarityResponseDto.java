package com.concrete.buildup.domain.attendance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Face Similarity API 응답 DTO
 * Railway에 배포된 얼굴 유사도 검증 API의 응답을 매핑합니다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FaceSimilarityResponseDto {

    /**
     * 검증 성공 여부
     * true: 유사도가 threshold 이상
     * false: 유사도가 threshold 미만
     */
    @JsonProperty("verified")
    private Boolean verified;

    /**
     * 코사인 거리 (0.0 ~ 2.0)
     * 낮을수록 유사
     */
    @JsonProperty("cosine_distance")
    private Double cosineDistance;

    /**
     * 코사인 유사도 (0.0 ~ 1.0)
     * 높을수록 유사
     */
    @JsonProperty("cosine_similarity")
    private Double cosineSimilarity;

    /**
     * 유사도 점수 (0.0 ~ 1.0)
     * 높을수록 유사
     */
    @JsonProperty("similarity_0_1")
    private Double similarity;

    /**
     * 감지된 얼굴 수
     * 정상적인 경우 1
     */
    @JsonProperty("faces_detected")
    private Integer facesDetected;

    /**
     * 유사도 임계값
     * 기본값: 0.35
     */
    @JsonProperty("threshold")
    private Double threshold;

    /**
     * API 모델 이름
     */
    @JsonProperty("model")
    private String model;

    /**
     * 거리 측정 방법 (cosine, euclidean 등)
     */
    @JsonProperty("metric")
    private String metric;

    /**
     * 에러 메시지 (nullable)
     * API 호출 실패 시에만 존재
     */
    @JsonProperty("error")
    private String error;
}
