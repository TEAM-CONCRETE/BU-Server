package com.concrete.buildup.domain.attendance.service;

import com.concrete.buildup.domain.attendance.dto.FaceSimilarityResponseDto;
import com.concrete.buildup.domain.attendance.exception.FaceApiClientException;
import com.concrete.buildup.domain.attendance.exception.FaceApiException;
import com.concrete.buildup.domain.attendance.exception.FaceNotDetectedException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * FaceSimilarityClient 단위 테스트
 * MockWebServer를 사용하여 Face API 응답을 모킹합니다.
 */
class FaceSimilarityClientTest {

    private MockWebServer mockWebServer;
    private FaceSimilarityClient faceSimilarityClient;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        // MockWebServer를 사용하는 WebClient 생성
        WebClient webClient = WebClient.builder()
            .baseUrl(mockWebServer.url("/").toString())
            .build();

        faceSimilarityClient = new FaceSimilarityClient(webClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("얼굴 인식 성공 - 검증 통과")
    void compareFaces_Success_Verified() {
        // Given
        String registeredUrl = "https://s3.amazonaws.com/bucket/registered.jpg";
        String capturedUrl = "https://s3.amazonaws.com/bucket/captured.jpg";

        String mockResponseBody = """
            {
              "model": "insightface-buffalo_s",
              "metric": "cosine",
              "cosine_similarity": 0.949683,
              "cosine_distance": 0.050317,
              "similarity_0_1": 0.974841,
              "verified": true,
              "threshold": 0.35,
              "faces_detected": {
                "image1": 1,
                "image2": 1
              },
              "image1_url": "https://s3.amazonaws.com/bucket/registered.jpg",
              "image2_url": "https://s3.amazonaws.com/bucket/captured.jpg"
            }
            """;

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(mockResponseBody)
            .addHeader("Content-Type", "application/json"));

        // When
        FaceSimilarityResponseDto result = faceSimilarityClient.compareFaces(registeredUrl, capturedUrl);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getVerified()).isTrue();
        assertThat(result.getCosineSimilarity()).isEqualTo(0.949683);
        assertThat(result.getSimilarity()).isEqualTo(0.974841);
        assertThat(result.getFacesDetected()).isNotNull();
        assertThat(result.getFacesDetected().get("image1")).isEqualTo(1);
        assertThat(result.getFacesDetected().get("image2")).isEqualTo(1);
    }

    @Test
    @DisplayName("얼굴 인식 성공 - 검증 실패 (유사도 미달)")
    void compareFaces_Success_NotVerified() {
        // Given
        String registeredUrl = "https://s3.amazonaws.com/bucket/registered.jpg";
        String capturedUrl = "https://s3.amazonaws.com/bucket/captured.jpg";

        String mockResponseBody = """
            {
              "model": "insightface-buffalo_s",
              "metric": "cosine",
              "cosine_similarity": 0.5,
              "cosine_distance": 0.5,
              "similarity_0_1": 0.75,
              "verified": false,
              "threshold": 0.35,
              "faces_detected": {
                "image1": 1,
                "image2": 1
              }
            }
            """;

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(mockResponseBody)
            .addHeader("Content-Type", "application/json"));

        // When
        FaceSimilarityResponseDto result = faceSimilarityClient.compareFaces(registeredUrl, capturedUrl);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getVerified()).isFalse();
        assertThat(result.getCosineSimilarity()).isEqualTo(0.5);
    }

    @Test
    @DisplayName("얼굴 미검출 - 422 Unprocessable Entity")
    void compareFaces_FaceNotDetected() {
        // Given
        String registeredUrl = "https://s3.amazonaws.com/bucket/registered.jpg";
        String capturedUrl = "https://s3.amazonaws.com/bucket/captured.jpg";

        String mockErrorResponse = """
            {
              "detail": "No face detected in image2"
            }
            """;

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(422)
            .setBody(mockErrorResponse)
            .addHeader("Content-Type", "application/json"));

        // When & Then
        assertThatThrownBy(() -> faceSimilarityClient.compareFaces(registeredUrl, capturedUrl))
            .isInstanceOf(FaceNotDetectedException.class)
            .hasMessageContaining("얼굴이 감지되지 않았습니다");
    }

    @Test
    @DisplayName("이미지 크기 초과 - 413 Payload Too Large (재시도 안함)")
    void compareFaces_PayloadTooLarge() {
        // Given
        String registeredUrl = "https://s3.amazonaws.com/bucket/registered.jpg";
        String capturedUrl = "https://s3.amazonaws.com/bucket/captured.jpg";

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(413)
            .setBody("{\"detail\": \"Image size exceeds 10MB\"}")
            .addHeader("Content-Type", "application/json"));

        // When & Then
        assertThatThrownBy(() -> faceSimilarityClient.compareFaces(registeredUrl, capturedUrl))
            .isInstanceOf(FaceApiClientException.class)
            .hasMessageContaining("이미지 크기가 너무 큽니다");
    }

    @Test
    @DisplayName("서버 내부 오류 - 500 Internal Server Error")
    void compareFaces_InternalServerError() {
        // Given
        String registeredUrl = "https://s3.amazonaws.com/bucket/registered.jpg";
        String capturedUrl = "https://s3.amazonaws.com/bucket/captured.jpg";

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(500)
            .setBody("{\"detail\": \"Internal server error\"}")
            .addHeader("Content-Type", "application/json"));

        // When & Then
        assertThatThrownBy(() -> faceSimilarityClient.compareFaces(registeredUrl, capturedUrl))
            .isInstanceOf(FaceApiException.class)
            .hasMessageContaining("서비스 내부 오류");
    }

    @Test
    @DisplayName("응답이 null인 경우")
    void compareFaces_NullResponse() {
        // Given
        String registeredUrl = "https://s3.amazonaws.com/bucket/registered.jpg";
        String capturedUrl = "https://s3.amazonaws.com/bucket/captured.jpg";

        // Empty response body (null 응답 시뮬레이션)
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader("Content-Type", "application/json"));

        // When & Then
        assertThatThrownBy(() -> faceSimilarityClient.compareFaces(registeredUrl, capturedUrl))
            .isInstanceOf(FaceApiException.class)
            .hasMessageContaining("Face API 응답이 null입니다");
    }
}
