package com.concrete.buildup.domain.attendance.exception;

/**
 * Face API 클라이언트 오류 예외
 *
 * <p>클라이언트 측 요청 오류로 재시도해도 성공할 가능성이 없는 경우 사용합니다.</p>
 * <ul>
 *   <li>400: 잘못된 요청 (이미지 다운로드/디코딩 실패)</li>
 *   <li>403: 권한 없음 (S3 접근 실패)</li>
 *   <li>413: 이미지 크기 초과</li>
 * </ul>
 *
 * <p>이 예외는 재시도하지 않습니다 (@Retryable의 noRetryFor에 포함).</p>
 */
public class FaceApiClientException extends RuntimeException {

    public FaceApiClientException(String message) {
        super(message);
    }

    public FaceApiClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
