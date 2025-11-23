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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
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
 *
 * 조건부 활성화:
 * spring.cloud.aws.s3.enabled=true 일 때만 빈 생성
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@ConditionalOnProperty(name = "spring.cloud.aws.s3.enabled", havingValue = "true")
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
            // S3 키 생성
            String s3Key = buildS3Key(
                    request.getResourceType(),
                    request.getResourceId(),
                    request.getSignerRole(),
                    request.getFileExtension(),
                    request.getEmployeeId()
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
     *
     * 파일명 규칙:
     * - CONTRACT: uploads/{resourceType}/{resourceId}/{signerRole}.{ext} (기존 패턴 유지)
     * - SAFETY_DOC 관리자: uploads/{resourceType}/{resourceId}/{signerRole}/{timestamp}.{ext}
     * - SAFETY_DOC 참석자: uploads/{resourceType}/{resourceId}/{signerRole}/{employeeId}/{timestamp}.{ext}
     *
     * @param resourceType 리소스 타입 (CONTRACT, WORK_REPORT, SAFETY_DOC)
     * @param resourceId 리소스 ID
     * @param signerRole 서명자 역할
     * @param fileExtension 파일 확장자
     * @param employeeId 근로자 ID (안전교육일지 참석자 서명 시 필수)
     * @return S3 객체 키
     */
    private String buildS3Key(ResourceType resourceType, String resourceId, SignerRole signerRole, String fileExtension, Long employeeId) {
        // CONTRACT는 기존 패턴 유지 (다른 팀원 작업과의 호환성)
        if (resourceType == ResourceType.CONTRACT) {
            return String.format("uploads/%s/%s/%s.%s",
                    resourceType.getFolderName(),
                    resourceId,
                    signerRole.name(),
                    fileExtension);
        }

        long timestamp = System.currentTimeMillis();

        // 안전교육일지 참석자(EMPLOYEE) 서명인 경우 employeeId로 구분
        if (resourceType == ResourceType.SAFETY_DOC && signerRole == SignerRole.EMPLOYEE && employeeId != null) {
            return String.format("uploads/%s/%s/%s/%d/%d.%s",
                    resourceType.getFolderName(),
                    resourceId,
                    signerRole.name(),
                    employeeId,
                    timestamp,
                    fileExtension);
        }

        // SAFETY_DOC 관리자 및 기타 경로 (타임스탬프로 덮어쓰기 방지)
        return String.format("uploads/%s/%s/%s/%d.%s",
                resourceType.getFolderName(),
                resourceId,
                signerRole.name(),
                timestamp,
                fileExtension);
    }

    /**
     * PDF 다운로드
     * S3에서 PDF 파일을 다운로드합니다.
     *
     * @param s3Key S3 객체 키 (파일 경로)
     * @return PDF 바이트 배열
     * @throws BusinessException 파일 다운로드 실패 시
     */
    public byte[] downloadPdf(String s3Key) {
        try {
            log.info("Downloading PDF from S3: {}", s3Key);

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(getObjectRequest);
            byte[] data = objectBytes.asByteArray();

            log.info("PDF downloaded successfully: {} bytes", data.length);
            return data;

        } catch (Exception e) {
            log.error("Failed to download PDF from S3: {}", s3Key, e);
            throw new BusinessException(S3ErrorCode.FILE_DOWNLOAD_FAILED, e);
        }
    }

    /**
     * PDF 업로드
     * 바이트 배열을 S3에 PDF로 업로드합니다.
     *
     * @param s3Key S3 객체 키 (파일 경로)
     * @param pdfBytes PDF 바이트 배열
     * @throws BusinessException 파일 업로드 실패 시
     */
    public void uploadPdf(String s3Key, byte[] pdfBytes) {
        try {
            log.info("Uploading PDF to S3: {}, size: {} bytes", s3Key, pdfBytes.length);

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType("application/pdf")
                    .build();

            s3Client.putObject(putObjectRequest,
                    software.amazon.awssdk.core.sync.RequestBody.fromBytes(pdfBytes));

            log.info("PDF uploaded successfully: {}", s3Key);

        } catch (Exception e) {
            log.error("Failed to upload PDF to S3: {}", s3Key, e);
            throw new BusinessException(S3ErrorCode.FILE_UPLOAD_FAILED, e);
        }
    }

    /**
     * S3 URL 생성
     * S3 키로부터 접근 가능한 URL을 생성합니다.
     *
     * @param s3Key S3 객체 키
     * @return S3 URL
     */
    public String getPdfUrl(String s3Key) {
        return String.format("https://%s.s3.amazonaws.com/%s", bucketName, s3Key);
    }

    /**
     * 범용 Presigned URL 생성 (얼굴 이미지, 프로필 사진 등)
     * 클라이언트가 직접 S3에 파일을 업로드할 수 있는 임시 URL을 발급합니다.
     *
     * @param userId 사용자 ID (JWT에서 추출)
     * @param resourceType 리소스 타입 (EMPLOYEE_PROFILE, ATTENDANCE_PROBE 등)
     * @param fileExtension 파일 확장자
     * @param siteId 현장 ID (ATTENDANCE_PROBE일 때 사용)
     * @param employeeId 근로자 ID (ATTENDANCE_PROBE일 때 사용)
     * @return Presigned URL 응답 (uploadUrl, expiresAt, s3Key, bucket)
     * @throws BusinessException Presigned URL 생성 실패 시
     */
    public PresignedUrlResponse generateSimplePresignedUrl(String userId, ResourceType resourceType, String fileExtension, Long siteId, Long employeeId) {
        try {
            // S3 키 생성
            String s3Key = buildSimpleS3Key(userId, resourceType, fileExtension, siteId, employeeId);

            log.info("Generating simple presigned URL for userId: {}, s3Key: {}", userId, s3Key);

            // PutObjectRequest 생성
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(getContentType(fileExtension))
                    .build();

            // Presigned URL 생성 요청
            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(PRESIGNED_URL_EXPIRATION)
                    .putObjectRequest(putObjectRequest)
                    .build();

            // Presigned URL 발급
            PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
            String uploadUrl = presignedRequest.url().toString();

            // AWS SDK가 생성한 실제 만료 시각 사용
            Instant expiration = presignedRequest.expiration();
            LocalDateTime expiresAt = LocalDateTime.ofInstant(expiration, ZoneId.systemDefault());

            log.info("Simple presigned URL generated successfully. Expires at: {}", expiresAt);

            return PresignedUrlResponse.builder()
                    .uploadUrl(uploadUrl)
                    .expiresAt(expiresAt)
                    .s3Key(s3Key)
                    .bucket(bucketName)
                    .build();

        } catch (Exception e) {
            log.error("Failed to generate simple presigned URL for userId: {}", userId, e);
            throw new BusinessException(S3ErrorCode.PRESIGNED_URL_GENERATION_FAILED, e);
        }
    }

    /**
     * 범용 S3 키 생성 (얼굴 이미지, 프로필 사진 등)
     * 파일명 규칙:
     * - ATTENDANCE_PROBE: attendance/{siteId}/{employeeId}/{timestamp}.{ext}
     * - EMPLOYEE_PROFILE: profile/{employeeId}/face.{ext} (덮어쓰기)
     * - 기타: uploads/{folderName}/{userId}/{timestamp}.{ext}
     *
     * @param userId 사용자 ID
     * @param resourceType 리소스 타입
     * @param fileExtension 파일 확장자
     * @param siteId 현장 ID (ATTENDANCE_PROBE 전용)
     * @param employeeId 근로자 ID (ATTENDANCE_PROBE, EMPLOYEE_PROFILE 전용)
     * @return S3 객체 키
     */
    private String buildSimpleS3Key(String userId, ResourceType resourceType, String fileExtension, Long siteId, Long employeeId) {
        long timestamp = System.currentTimeMillis();

        // ATTENDANCE_PROBE는 현장/근로자 기반 폴더 구조 사용 (타임스탬프 포함)
        if (resourceType == ResourceType.ATTENDANCE_PROBE) {
            if (siteId == null || employeeId == null) {
                throw new BusinessException(S3ErrorCode.PRESIGNED_URL_GENERATION_FAILED,
                    "ATTENDANCE_PROBE 타입은 siteId와 employeeId가 필수입니다.");
            }
            return String.format("attendance/%d/%d/%d.%s",
                    siteId,
                    employeeId,
                    timestamp,
                    fileExtension);
        }

        // EMPLOYEE_PROFILE는 근로자 ID 기반 고정 파일명 사용 (덮어쓰기)
        if (resourceType == ResourceType.EMPLOYEE_PROFILE) {
            if (employeeId == null) {
                throw new BusinessException(S3ErrorCode.PRESIGNED_URL_GENERATION_FAILED,
                    "EMPLOYEE_PROFILE 타입은 employeeId가 필수입니다.");
            }
            return String.format("profile/%d/face.%s",
                    employeeId,
                    fileExtension);
        }

        // 기타 타입은 기존 폴더 구조 사용
        return String.format("uploads/%s/%s/%d.%s",
                resourceType.getFolderName(),
                userId,
                timestamp,
                fileExtension);
    }

    /**
     * S3 객체에 대한 Presigned GET URL 생성
     * Face API 등 외부 서비스가 임시로 S3 이미지를 다운로드할 수 있도록 합니다.
     *
     * @param s3Key S3 객체 키
     * @return Presigned GET URL (15분 유효)
     * @throws BusinessException Presigned URL 생성 실패 시
     */
    public String generatePresignedGetUrl(String s3Key) {
        try {
            log.info("Generating presigned GET URL for s3Key: {}", s3Key);

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(PRESIGNED_URL_EXPIRATION)
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            String getUrl = presignedRequest.url().toString();

            log.info("Presigned GET URL generated successfully for s3Key: {}", s3Key);

            return getUrl;

        } catch (Exception e) {
            log.error("Failed to generate presigned GET URL for s3Key: {}", s3Key, e);
            throw new BusinessException(S3ErrorCode.PRESIGNED_URL_GENERATION_FAILED, e);
        }
    }

    /**
     * 출퇴근 이미지 파일 직접 업로드 (백엔드 처리 방식)
     *
     * <p>클라이언트가 전송한 MultipartFile을 백엔드에서 직접 S3에 업로드합니다.
     * Presigned URL을 사용하지 않고 백엔드가 직접 업로드하므로 보안성이 향상됩니다.</p>
     *
     * @param file 업로드할 이미지 파일 (MultipartFile)
     * @param siteId 현장 ID
     * @param employeeId 근로자 ID
     * @return S3 객체 키 (attendance/{siteId}/{employeeId}/{timestamp}.jpg)
     * @throws BusinessException 파일 업로드 실패 시
     */
    public String uploadAttendanceImage(org.springframework.web.multipart.MultipartFile file, Long siteId, Long employeeId) {
        try {
            // 파일 확장자 추출
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);

            // S3 키 생성: attendance/{siteId}/{employeeId}/{timestamp}.{ext}
            long timestamp = System.currentTimeMillis();
            String s3Key = String.format("attendance/%d/%d/%d.%s",
                    siteId,
                    employeeId,
                    timestamp,
                    fileExtension);

            log.info("Uploading attendance image to S3: s3Key={}, size={} bytes, contentType={}",
                    s3Key, file.getSize(), file.getContentType());

            // PutObjectRequest 생성
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(file.getContentType())
                    .build();

            // S3에 업로드
            s3Client.putObject(putObjectRequest,
                    software.amazon.awssdk.core.sync.RequestBody.fromInputStream(
                            file.getInputStream(),
                            file.getSize()
                    ));

            log.info("Attendance image uploaded successfully: {}", s3Key);

            return s3Key;

        } catch (Exception e) {
            log.error("Failed to upload attendance image to S3 for siteId={}, employeeId={}",
                    siteId, employeeId, e);
            throw new BusinessException(S3ErrorCode.FILE_UPLOAD_FAILED, e);
        }
    }

    /**
     * 파일명에서 확장자 추출
     *
     * @param filename 파일명
     * @return 확장자 (예: "jpg", "png")
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "jpg";  // 기본값
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
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

    /**
     * S3 URL에서 S3 키 추출
     *
     * <p>S3 URL에서 버킷 이후의 경로(키)를 추출합니다.</p>
     * <p>지원하는 URL 형식:</p>
     * <ul>
     *   <li>https://bucket.s3.amazonaws.com/path/to/file.pdf</li>
     *   <li>https://bucket.s3.ap-northeast-2.amazonaws.com/path/to/file.pdf</li>
     * </ul>
     *
     * @param url S3 URL
     * @return S3 키 (null이면 추출 실패)
     */
    public String extractS3KeyFromUrl(String url) {
        if (url == null || url.isBlank()) {
            log.warn("S3 URL이 null 또는 빈 문자열입니다.");
            return null;
        }

        // safety-docs/ 로 시작하는 키 추출
        int safetyDocsIndex = url.indexOf("safety-docs/");
        if (safetyDocsIndex != -1) {
            return url.substring(safetyDocsIndex);
        }

        // contracts/ 로 시작하는 키 추출
        int contractsIndex = url.indexOf("contracts/");
        if (contractsIndex != -1) {
            return url.substring(contractsIndex);
        }

        // work-reports/ 로 시작하는 키 추출
        int workReportsIndex = url.indexOf("work-reports/");
        if (workReportsIndex != -1) {
            return url.substring(workReportsIndex);
        }

        // uploads/ 로 시작하는 키 추출
        int uploadsIndex = url.indexOf("uploads/");
        if (uploadsIndex != -1) {
            return url.substring(uploadsIndex);
        }

        // 그 외: .com/ 이후의 경로 추출 시도
        int comSlashIndex = url.indexOf(".com/");
        if (comSlashIndex != -1) {
            String key = url.substring(comSlashIndex + 5);
            if (!key.isBlank()) {
                return key;
            }
        }

        log.warn("S3 URL에서 키를 추출할 수 없습니다: {}", url);
        return null;
    }

    /**
     * S3에 파일이 존재하는지 확인
     *
     * @param s3Key S3 객체 키
     * @return 파일이 존재하면 true, 아니면 false
     */
    public boolean doesObjectExist(String s3Key) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            s3Client.headObject(headObjectRequest);
            log.debug("S3 파일 존재 확인 성공: {}", s3Key);
            return true;

        } catch (NoSuchKeyException e) {
            log.debug("S3 파일이 존재하지 않음: {}", s3Key);
            return false;

        } catch (Exception e) {
            log.warn("S3 파일 존재 확인 중 예외 발생: {}", s3Key, e);
            return false;
        }
    }
}