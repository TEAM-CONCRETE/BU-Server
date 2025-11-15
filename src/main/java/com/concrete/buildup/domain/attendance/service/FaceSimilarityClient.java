package com.concrete.buildup.domain.attendance.service;

import com.concrete.buildup.domain.attendance.dto.FaceSimilarityRequestDto;
import com.concrete.buildup.domain.attendance.dto.FaceSimilarityResponseDto;
import com.concrete.buildup.domain.attendance.exception.FaceApiClientException;
import com.concrete.buildup.domain.attendance.exception.FaceApiException;
import com.concrete.buildup.domain.attendance.exception.FaceNotDetectedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Face Similarity API 클라이언트
 * Railway에 배포된 얼굴 유사도 검증 서비스와 통신합니다.
 *
 * <p><b>처리 흐름:</b></p>
 * <ol>
 *   <li>GET 요청으로 이미지 URL을 쿼리 파라미터로 전달</li>
 *   <li>API가 S3에서 이미지를 다운로드하여 얼굴 검출 및 유사도 비교 수행</li>
 *   <li>응답 분석 및 예외 처리</li>
 * </ol>
 *
 * <p><b>재시도 전략 (Spring Retry):</b></p>
 * <ul>
 *   <li><b>재시도 O:</b> 서버 오류 (500, 503), 네트워크 오류</li>
 *   <li><b>재시도 X:</b> 클라이언트 오류 (400, 403, 413, 422)</li>
 *   <li><b>최대 재시도:</b> 3회 (초기 1회 + 재시도 2회)</li>
 *   <li><b>백오프 전략:</b> 1초 초기 지연, 배수 2.0 (1초 → 2초)</li>
 * </ul>
 *
 * <p><b>오류 상태 코드 및 예외 매핑:</b></p>
 * <table>
 *   <tr><th>코드</th><th>설명</th><th>예외</th><th>재시도</th></tr>
 *   <tr><td>422</td><td>얼굴 미검출</td><td>FaceNotDetectedException</td><td>X</td></tr>
 *   <tr><td>400</td><td>이미지 처리 실패</td><td>FaceApiClientException</td><td>X</td></tr>
 *   <tr><td>403</td><td>S3 접근 권한 없음</td><td>FaceApiClientException</td><td>X</td></tr>
 *   <tr><td>413</td><td>이미지 크기 초과 (10MB)</td><td>FaceApiClientException</td><td>X</td></tr>
 *   <tr><td>500</td><td>서버 내부 오류</td><td>FaceApiException</td><td>O</td></tr>
 *   <tr><td>503</td><td>서비스 일시 중단</td><td>FaceApiException</td><td>O</td></tr>
 * </table>
 *
 * @see FaceApiException 재시도 가능한 서버 오류
 * @see FaceApiClientException 재시도 불가능한 클라이언트 오류
 * @see FaceNotDetectedException 얼굴 미검출 오류
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FaceSimilarityClient {

    private static final String FACE_SIMILARITY_ENDPOINT = "/face/similarity_url";
    private static final double DEFAULT_THRESHOLD = 0.35;

    private final WebClient faceSimilarityWebClient;

    /**
     * 두 얼굴 이미지의 유사도를 비교합니다.
     *
     * <p>재시도 전략:</p>
     * <ul>
     *   <li>재시도 O: 네트워크 오류, 서버 오류 (500, 503)</li>
     *   <li>재시도 X: 클라이언트 오류 (400, 403, 413, 422)</li>
     * </ul>
     *
     * @param registeredImageUrl 등록된 얼굴 이미지 S3 URL
     * @param capturedImageUrl 촬영된 얼굴 이미지 S3 URL
     * @return Face Similarity API 응답
     * @throws FaceNotDetectedException 얼굴이 검출되지 않은 경우 (재시도 안함)
     * @throws FaceApiClientException 클라이언트 오류 (재시도 안함)
     * @throws FaceApiException 서버 오류 또는 네트워크 오류 (재시도 가능)
     */
    @Retryable(
        retryFor = {WebClientRequestException.class, FaceApiException.class},
        noRetryFor = {FaceNotDetectedException.class, FaceApiClientException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public FaceSimilarityResponseDto compareFaces(String registeredImageUrl, String capturedImageUrl) {
        log.info("Face Similarity API 호출 시작 - registered: {}, captured: {}",
                 maskUrl(registeredImageUrl), maskUrl(capturedImageUrl));

        log.info("Face API 요청 생성 - image1_url: {}, image2_url: {}, threshold: {}",
                 maskUrl(registeredImageUrl), maskUrl(capturedImageUrl), DEFAULT_THRESHOLD);

        try {
            FaceSimilarityResponseDto response = faceSimilarityWebClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path(FACE_SIMILARITY_ENDPOINT)
                    .queryParam("image1_url", registeredImageUrl)
                    .queryParam("image2_url", capturedImageUrl)
                    .queryParam("threshold", DEFAULT_THRESHOLD)
                    .build())
                .retrieve()
                .bodyToMono(FaceSimilarityResponseDto.class)
                .doOnError(error -> log.error("Face API 호출 중 오류 발생", error))
                .block();

            if (response == null) {
                throw new FaceApiException("Face API 응답이 null입니다.");
            }

            log.info("Face Similarity API 응답 - verified: {}, similarity: {}, facesDetected: {}",
                     response.getVerified(), response.getSimilarity(), response.getFacesDetected());

            return response;

        } catch (WebClientResponseException e) {
            handleHttpError(e);
            throw new FaceApiException("Face API 호출 실패", e); // handleHttpError에서 예외 발생 시 도달하지 않음

        } catch (WebClientRequestException e) {
            log.error("Face API 연결 실패 - message: {}", e.getMessage());
            throw new FaceApiException("Face API 연결 실패: " + e.getMessage(), e);

        } catch (Exception e) {
            log.error("Face API 알 수 없는 오류", e);
            throw new FaceApiException("Face API 알 수 없는 오류: " + e.getMessage(), e);
        }
    }

    /**
     * HTTP 오류 상태 코드별 예외 처리
     *
     * @param e WebClientResponseException
     * @throws FaceNotDetectedException 422 Unprocessable Entity (재시도 안함)
     * @throws FaceApiClientException 클라이언트 오류 (400, 403, 413 - 재시도 안함)
     * @throws FaceApiException 서버 오류 (500, 503 - 재시도 가능)
     */
    private void handleHttpError(WebClientResponseException e) {
        HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
        String responseBody = e.getResponseBodyAsString();

        log.error("Face API HTTP 오류 - status: {}, body: {}", status, responseBody);

        if (status == HttpStatus.UNPROCESSABLE_ENTITY) {
            // 422: 얼굴 미검출 (No face detected in image1/image2) - 재시도 불필요
            String detail = extractDetailFromErrorResponse(responseBody);
            throw new FaceNotDetectedException(
                detail != null ? detail : "얼굴이 감지되지 않았습니다. 다시 촬영해주세요."
            );

        } else if (status == HttpStatus.BAD_REQUEST) {
            // 400: 이미지 다운로드/디코딩 실패 - 재시도 불필요
            String detail = extractDetailFromErrorResponse(responseBody);
            throw new FaceApiClientException("이미지 처리 실패: " + (detail != null ? detail : "잘못된 요청입니다."), e);

        } else if (status == HttpStatus.FORBIDDEN) {
            // 403: S3 이미지 접근 권한 없음 - 재시도 불필요
            String detail = extractDetailFromErrorResponse(responseBody);
            throw new FaceApiClientException("이미지 접근 권한이 없습니다: " + (detail != null ? detail : "S3 이미지 다운로드 실패"), e);

        } else if (status == HttpStatus.PAYLOAD_TOO_LARGE) {
            // 413: 이미지 크기 초과 (10MB) - 재시도 불필요
            throw new FaceApiClientException("이미지 크기가 너무 큽니다. 10MB 이하의 이미지를 사용해주세요.", e);

        } else if (status == HttpStatus.SERVICE_UNAVAILABLE) {
            // 503: 모델 준비 중 - 재시도 시 성공 가능
            throw new FaceApiException("얼굴 인식 서비스가 준비 중입니다. 잠시 후 다시 시도해주세요.", e);

        } else if (status == HttpStatus.INTERNAL_SERVER_ERROR) {
            // 500: 서버 내부 오류 - 재시도 시 성공 가능
            String detail = extractDetailFromErrorResponse(responseBody);
            throw new FaceApiException("얼굴 인식 서비스 내부 오류: " + (detail != null ? detail : "서버 오류"), e);

        } else {
            // 기타 오류 - 안전하게 재시도 불필요로 처리
            throw new FaceApiClientException("Face API 호출 실패: HTTP " + status, e);
        }
    }

    /**
     * FastAPI 오류 응답에서 detail 필드 추출
     * 형식: {"detail": "No face detected in image2"}
     *
     * @param responseBody 응답 본문
     * @return detail 메시지 (추출 실패 시 null)
     */
    private String extractDetailFromErrorResponse(String responseBody) {
        try {
            if (responseBody != null && responseBody.contains("\"detail\"")) {
                int start = responseBody.indexOf("\"detail\"") + 10;
                int end = responseBody.indexOf("\"", start);
                if (end > start) {
                    return responseBody.substring(start, end);
                }
            }
        } catch (Exception e) {
            log.warn("detail 필드 추출 실패", e);
        }
        return null;
    }

    /**
     * URL 마스킹 (개인정보 보호)
     * 로그에 전체 URL 노출을 방지합니다.
     *
     * @param url S3 URL
     * @return 마스킹된 URL (앞 20자만 표시)
     */
    private String maskUrl(String url) {
        if (url == null || url.length() < 20) {
            return url;
        }
        return url.substring(0, 20) + "***";
    }
}
