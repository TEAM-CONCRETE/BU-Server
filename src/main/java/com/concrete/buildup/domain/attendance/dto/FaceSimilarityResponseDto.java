package com.concrete.buildup.domain.attendance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Face Similarity API 응답 DTO
 * Railway에 배포된 얼굴 유사도 검증 API의 응답을 매핑합니다.
 *
 * 해석 가이드:
 * - verified: cosine_distance < threshold면 true → 같은 사람으로 판정
 * - threshold: 기본 0.35 (0.3~0.5 범위에서 튜닝 권장)
 * - faces_detected: 각 이미지에서 검출된 얼굴 개수 {"image1": 1, "image2": 1}
 * - bboxes: [x1,y1,x2,y2] 픽셀 좌표(좌상단 원점)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FaceSimilarityResponseDto {

    /**
     * API 모델 이름
     * 예: "insightface-buffalo_s"
     */
    @JsonProperty("model")
    private String model;

    /**
     * 거리 측정 방법
     * 예: "cosine"
     */
    @JsonProperty("metric")
    private String metric;

    /**
     * 코사인 유사도 (0.0 ~ 1.0)
     * 높을수록 유사 (1에 가까울수록 동일인물)
     */
    @JsonProperty("cosine_similarity")
    private Double cosineSimilarity;

    /**
     * 코사인 거리 (0.0 ~ 2.0)
     * 낮을수록 유사 (0에 가까울수록 동일인물)
     */
    @JsonProperty("cosine_distance")
    private Double cosineDistance;

    /**
     * 유사도 점수 (0.0 ~ 1.0)
     * 높을수록 유사
     */
    @JsonProperty("similarity_0_1")
    private Double similarity;

    /**
     * 검증 성공 여부
     * true: cosine_distance < threshold (같은 사람으로 판정)
     * false: cosine_distance >= threshold (다른 사람으로 판정)
     */
    @JsonProperty("verified")
    private Boolean verified;

    /**
     * 유사도 임계값
     * 기본값: 0.35 (0.3~0.5 범위에서 튜닝 권장)
     */
    @JsonProperty("threshold")
    private Double threshold;

    /**
     * 각 이미지에서 검출된 얼굴 수
     * 정상: {"image1": 1, "image2": 1}
     * 미검출: {"image1": 0, "image2": 1} 또는 {"image1": 1, "image2": 0}
     */
    @JsonProperty("faces_detected")
    private Map<String, Integer> facesDetected;

    /**
     * 얼굴 경계 상자 좌표
     * 형식: {"image1": [[x1,y1,x2,y2]], "image2": [[x1,y1,x2,y2]]}
     * [x1,y1]: 좌상단 좌표, [x2,y2]: 우하단 좌표 (픽셀 단위, 좌상단 원점)
     */
    @JsonProperty("bboxes")
    private Map<String, List<List<Integer>>> bboxes;

    /**
     * 첫 번째 이미지 URL (응답에 포함됨)
     */
    @JsonProperty("image1_url")
    private String image1Url;

    /**
     * 두 번째 이미지 URL (응답에 포함됨)
     */
    @JsonProperty("image2_url")
    private String image2Url;

    /**
     * 에러 상세 정보 (nullable)
     * FastAPI 표준 오류 응답의 detail 필드
     * 예: "No face detected in image2"
     */
    @JsonProperty("detail")
    private String detail;
}
