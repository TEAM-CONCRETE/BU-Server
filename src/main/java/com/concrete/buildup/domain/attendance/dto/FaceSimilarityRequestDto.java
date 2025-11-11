package com.concrete.buildup.domain.attendance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Face Similarity API 요청 DTO
 * Railway에 배포된 얼굴 유사도 검증 API에 전달할 요청을 매핑합니다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaceSimilarityRequestDto {

    /**
     * 등록된 얼굴 이미지 URL (S3)
     */
    @JsonProperty("image1_url")
    private String image1Url;

    /**
     * 촬영된 얼굴 이미지 URL (S3)
     */
    @JsonProperty("image2_url")
    private String image2Url;

    /**
     * 유사도 임계값 (기본: 0.35)
     * 코사인 거리가 이 값보다 작으면 검증 성공
     */
    @JsonProperty("threshold")
    private Double threshold;
}
