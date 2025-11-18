package com.concrete.buildup.domain.attendance.service;

import com.concrete.buildup.global.exception.BusinessException;
import com.concrete.buildup.global.exception.errorcode.AttendanceErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * 얼굴 인식 AI API 서비스
 *
 * <p>InsightFace Buffalo-S 모델을 사용하는 AI API와 통신하여
 * 두 얼굴 이미지의 유사도를 계산합니다.</p>
 *
 * <p>AI API Base URL: https://bubbly-reprieve-production-52a0.up.railway.app</p>
 * <p>엔드포인트: GET /face/similarity_url</p>
 * <p>파라미터: image1_url, image2_url, threshold (Query Parameters)</p>
 * <p>모델: InsightFace Buffalo-S</p>
 * <p>임계값: 0.35 (Cosine Distance)</p>
 * <p>검증 방식: cosine_distance < threshold → verified = true</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FaceRecognitionService {

    @Qualifier("faceSimilarityWebClient")
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    /**
     * 얼굴 유사도 임계값 (0.35 이상이면 동일인으로 판단)
     */
    private static final double SIMILARITY_THRESHOLD = 0.35;

    /**
     * 두 얼굴 이미지 비교
     *
     * <p>S3 Presigned URL을 사용하여 AI API에 전달하고,
     * Cosine Similarity를 계산하여 동일인 여부를 판단합니다.</p>
     *
     * @param profileImageUrl 프로필 이미지 Presigned GET URL
     * @param attendanceImageUrl 출퇴근 촬영 이미지 Presigned GET URL
     * @return 동일인이면 true, 아니면 false
     * @throws BusinessException AI API 호출 실패 또는 얼굴 비교 실패 시
     */
    public boolean compareFaces(String profileImageUrl, String attendanceImageUrl) {
        try {
            log.info("Face recognition API 호출 시작: profileImageUrl={}, attendanceImageUrl={}",
                    maskUrl(profileImageUrl), maskUrl(attendanceImageUrl));

            // WebClient로 API 호출 (GET 방식, Query Parameters 사용)
            String responseBody = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/face/similarity_url")
                            .queryParam("image1_url", profileImageUrl)
                            .queryParam("image2_url", attendanceImageUrl)
                            .queryParam("threshold", SIMILARITY_THRESHOLD)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            // 응답 파싱
            if (responseBody != null) {
                JsonNode jsonResponse = objectMapper.readTree(responseBody);

                // API 응답 필드: verified, cosine_distance, similarity_0_1
                boolean verified = jsonResponse.get("verified").asBoolean();
                double cosineDistance = jsonResponse.has("cosine_distance")
                        ? jsonResponse.get("cosine_distance").asDouble()
                        : 1.0;
                double similarity01 = jsonResponse.has("similarity_0_1")
                        ? jsonResponse.get("similarity_0_1").asDouble()
                        : (1.0 - cosineDistance);

                log.info("Face recognition 결과: verified={}, cosine_distance={}, similarity_0_1={}, threshold={}",
                        verified, cosineDistance, similarity01, SIMILARITY_THRESHOLD);

                if (!verified) {
                    log.warn("얼굴 비교 실패: verified=false, cosine_distance={}, threshold={}",
                            cosineDistance, SIMILARITY_THRESHOLD);
                    throw new BusinessException(
                            AttendanceErrorCode.FACE_VERIFICATION_FAILED,
                            String.format("얼굴 인식에 실패했습니다. (유사도: %.2f%%, 기준: %.2f%%)",
                                    similarity01 * 100, SIMILARITY_THRESHOLD * 100)
                    );
                }

                return true;

            } else {
                log.error("Face recognition API 응답이 비어있습니다.");
                throw new BusinessException(
                        AttendanceErrorCode.FACE_API_ERROR,
                        "얼굴 인식 API 응답이 비어있습니다."
                );
            }

        } catch (WebClientResponseException e) {
            // 4xx, 5xx 에러
            log.error("Face recognition API 오류: status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new BusinessException(
                    AttendanceErrorCode.FACE_API_ERROR,
                    String.format("얼굴 인식 API 오류: %s", e.getMessage())
            );

        } catch (BusinessException e) {
            // 이미 처리된 BusinessException은 그대로 throw
            throw e;

        } catch (Exception e) {
            // 기타 예외 (타임아웃, 네트워크 오류 등)
            log.error("Face recognition 중 예상치 못한 오류 발생", e);
            throw new BusinessException(
                    AttendanceErrorCode.FACE_API_ERROR,
                    "얼굴 인식 중 오류가 발생했습니다: " + e.getMessage()
            );
        }
    }

    /**
     * URL 마스킹 (로그 보안을 위해 쿼리 파라미터 숨김)
     *
     * @param url 원본 URL
     * @return 마스킹된 URL (쿼리 파라미터 제거)
     */
    private String maskUrl(String url) {
        if (url == null) {
            return "null";
        }

        int queryIndex = url.indexOf('?');
        if (queryIndex > 0) {
            return url.substring(0, queryIndex) + "?...";
        }

        return url;
    }

    /**
     * AI API 연결 상태 확인 (헬스 체크)
     *
     * @return API가 정상 작동하면 true
     */
    public boolean isApiHealthy() {
        try {
            webClient.get()
                    .uri("/health")
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(Duration.ofSeconds(5))
                    .block();

            log.info("Face recognition API 헬스 체크: 정상");
            return true;

        } catch (Exception e) {
            log.warn("Face recognition API 헬스 체크 실패: {}", e.getMessage());
            return false;
        }
    }
}
