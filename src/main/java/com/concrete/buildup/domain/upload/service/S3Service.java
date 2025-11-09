package com.concrete.buildup.domain.upload.service;

import com.concrete.buildup.domain.contract.enums.SignerRole;
import com.concrete.buildup.domain.upload.dto.PresignedUrlRequest;
import com.concrete.buildup.domain.upload.dto.PresignedUrlResponse;
import com.concrete.buildup.domain.upload.enums.ResourceType;
import com.concrete.buildup.global.exception.BusinessException;
import com.concrete.buildup.global.exception.errorcode.S3ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * S3 파일 업로드 서비스
 *
 * 주요 기능:
 * - Presigned URL 발급 (클라이언트 직접 업로드용)
 * - 이미지 다운로드 (서명 검증용)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;

    /**
     * Presigned URL 만료 시간 (15분)
     */
    private static final Duration PRESIGNED_URL_EXPIRATION = Duration.ofMinutes(15);

    /**
     * Presigned URL 생성
     * 클라이언트가 직접 S3에 파일을 업로드할 수 있는 임시 URL을 발급합니다.
     *
     * @param request Presigned URL 발급 요청 정보
     * @return Presigned URL 응답 (uploadUrl, expiresAt, s3Key, bucket)
     * @throws BusinessException Presigned URL 생성 실패 시
     */
    public PresignedUrlResponse generatePresignedUrl(PresignedUrlRequest request) {
        try {
            // S3 키 생성: uploads/{resourceType}/{resourceId}/{signerRole}.{ext}
            String s3Key = buildS3Key(
                    request.getResourceType(),
                    request.getResourceId(),
                    request.getSignerRole(),
                    request.getFileExtension()
            );

            log.info("Generating presigned URL for s3Key: {}", s3Key);

            // PutObjectRequest 생성
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(getContentType(request.getFileExtension()))
                    .build();

            // Presigned URL 생성 요청
            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(PRESIGNED_URL_EXPIRATION)
                    .putObjectRequest(putObjectRequest)
                    .build();

            // Presigned URL 발급
            PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
            String uploadUrl = presignedRequest.url().toString();

            // AWS SDK가 생성한 실제 만료 시각 사용 (서버 시간과 무관하게 정확함)
            Instant expiration = presignedRequest.expiration();
            LocalDateTime expiresAt = LocalDateTime.ofInstant(expiration, ZoneId.systemDefault());

            log.info("Presigned URL generated successfully. Expires at: {}", expiresAt);

            return PresignedUrlResponse.builder()
                    .uploadUrl(uploadUrl)
                    .expiresAt(expiresAt)
                    .s3Key(s3Key)
                    .bucket(bucketName)
                    .build();

        } catch (Exception e) {
            log.error("Failed to generate presigned URL", e);
            throw new BusinessException(S3ErrorCode.PRESIGNED_URL_GENERATION_FAILED, e);
        }
    }

    /**
     * S3에서 이미지 다운로드
     * 서명 검증 등을 위해 S3에 저장된 이미지를 다운로드합니다.
     *
     * @param s3Key S3 객체 키 (파일 경로)
     * @return 이미지 바이트 배열
     * @throws BusinessException 파일 다운로드 실패 시
     */
    public byte[] downloadImage(String s3Key) {
        try {
            log.info("Downloading image from S3: {}", s3Key);

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(getObjectRequest);

            log.info("Image downloaded successfully. Size: {} bytes", objectBytes.asByteArray().length);

            return objectBytes.asByteArray();

        } catch (Exception e) {
            log.error("Failed to download image from S3: {}", s3Key, e);
            throw new BusinessException(S3ErrorCode.FILE_DOWNLOAD_FAILED, e);
        }
    }

    /**
     * S3 키 생성
     * 파일명 규칙: uploads/{resourceType}/{resourceId}/{signerRole}.{ext}
     *
     * @param resourceType 리소스 타입 (CONTRACT, WORK_REPORT, SAFETY_DOC)
     * @param resourceId 리소스 ID
     * @param signerRole 서명자 역할
     * @param fileExtension 파일 확장자
     * @return S3 객체 키
     */
    private String buildS3Key(ResourceType resourceType, String resourceId, SignerRole signerRole, String fileExtension) {
        return String.format("uploads/%s/%s/%s.%s",
                resourceType.getFolderName(),
                resourceId,
                signerRole.name(),
                fileExtension);
    }

    /**
     * 파일 확장자에 따른 Content-Type 반환
     *
     * @param fileExtension 파일 확장자
     * @return Content-Type
     */
    private String getContentType(String fileExtension) {
        return switch (fileExtension.toLowerCase()) {
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "pdf" -> "application/pdf";
            default -> "application/octet-stream";
        };
    }
}