package com.concrete.buildup.domain.upload.service;

import com.concrete.buildup.domain.contract.enums.SignerRole;
import com.concrete.buildup.domain.upload.dto.PresignedUrlRequest;
import com.concrete.buildup.domain.upload.dto.PresignedUrlResponse;
import com.concrete.buildup.domain.upload.enums.ResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * S3Service 실제 AWS 통합 테스트
 *
 * 이 테스트는 실제 AWS S3에 연결하여 동작을 검증합니다.
 *
 * 실행 조건:
 * - AWS_INTEGRATION_TEST=true 환경변수 설정 (필수)
 * - src/test/resources/application-test.yml에 AWS 자격증명 설정 필요
 *
 * 실행 예시:
 * AWS_INTEGRATION_TEST=true ./gradlew test --tests S3ServiceIntegrationTest
 *
 * 주의사항:
 * - 실제 AWS 비용이 발생할 수 있습니다 (미미한 수준)
 * - 테스트용 버킷을 별도로 생성하여 사용하는 것을 권장합니다
 * - KEEP_TEST_FILES=true 환경변수 설정 시 테스트 파일이 S3에 남아있음
 */
@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "AWS_INTEGRATION_TEST", matches = "true")
@DisplayName("S3Service 실제 AWS 통합 테스트")
class S3ServiceIntegrationTest {

    private final S3Service s3Service;
    private final S3Client s3Client;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;

    private String testS3Key;

    @Autowired
    public S3ServiceIntegrationTest(S3Service s3Service, S3Client s3Client) {
        this.s3Service = s3Service;
        this.s3Client = s3Client;
    }

    @BeforeEach
    void setUp() {
        // 테스트용 고유 키 생성 (시간 기반)
        testS3Key = "test-uploads/integration-test-" + System.currentTimeMillis() + "/EMPLOYEE.png";
    }

    @Test
    @DisplayName("실제 Presigned URL 발급 및 업로드 테스트")
    void generatePresignedUrl_RealAWS_Success() throws IOException {
        // Given
        PresignedUrlRequest request = PresignedUrlRequest.builder()
                .resourceType(ResourceType.CONTRACT)
                .resourceId("integration-test-" + System.currentTimeMillis())
                .signerRole(SignerRole.EMPLOYEE)
                .fileExtension("png")
                .build();

        // When - Presigned URL 발급
        PresignedUrlResponse response = s3Service.generatePresignedUrl(request);

        // Then - 응답 검증
        assertThat(response).isNotNull();
        assertThat(response.getUploadUrl()).isNotNull();
        assertThat(response.getUploadUrl()).startsWith("https://");
        assertThat(response.getUploadUrl()).contains(bucketName);
        assertThat(response.getS3Key()).isNotNull();
        assertThat(response.getBucket()).isEqualTo(bucketName);
        assertThat(response.getExpiresAt()).isAfter(LocalDateTime.now());
        assertThat(response.getExpiresAt()).isBefore(LocalDateTime.now().plusMinutes(20));

        // 실제 업로드 테스트
        try {
            // 테스트 이미지 데이터 (1x1 투명 PNG)
            byte[] testImageData = createTestPngImage();

            // Presigned URL로 PUT 요청
            HttpURLConnection connection = (HttpURLConnection) new URL(response.getUploadUrl()).openConnection();
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Content-Type", "image/png");
            connection.setDoOutput(true);
            connection.getOutputStream().write(testImageData);

            int responseCode = connection.getResponseCode();
            assertThat(responseCode).isEqualTo(200);

            // S3에 파일이 실제로 업로드되었는지 확인
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(response.getS3Key())
                    .build();

            s3Client.headObject(headObjectRequest);
            // 예외가 발생하지 않으면 파일이 존재함

            // AWS 콘솔 확인을 위한 정보 출력
            printS3FileInfo(response.getS3Key(), "PNG Image");

        } finally {
            // 테스트 후 정리
            cleanupTestFile(response.getS3Key());
        }
    }

    @Test
    @DisplayName("실제 이미지 다운로드 테스트")
    void downloadImage_RealAWS_Success() {
        // Given - 먼저 테스트 파일 업로드
        byte[] testImageData = createTestPngImage();

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(testS3Key)
                .contentType("image/png")
                .build();

        s3Client.putObject(putRequest, RequestBody.fromBytes(testImageData));

        try {
            // When - 이미지 다운로드
            byte[] downloadedData = s3Service.downloadImage(testS3Key);

            // Then
            assertThat(downloadedData).isNotNull();
            assertThat(downloadedData).isEqualTo(testImageData);

            // AWS 콘솔 확인을 위한 정보 출력
            printS3FileInfo(testS3Key, "PNG Image (Download Test)");

        } finally {
            // 테스트 후 정리
            cleanupTestFile(testS3Key);
        }
    }

    @Test
    @DisplayName("PDF 파일 Presigned URL 발급 테스트")
    void generatePresignedUrl_PdfFile_Success() throws IOException {
        // Given
        PresignedUrlRequest request = PresignedUrlRequest.builder()
                .resourceType(ResourceType.CONTRACT)
                .resourceId("integration-test-pdf-" + System.currentTimeMillis())
                .signerRole(SignerRole.CORPORATION)
                .fileExtension("pdf")
                .build();

        // When
        PresignedUrlResponse response = s3Service.generatePresignedUrl(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getS3Key()).endsWith(".pdf");
        assertThat(response.getUploadUrl()).contains(".pdf");

        // 실제 업로드 테스트
        try {
            byte[] testPdfData = createTestPdfData();

            HttpURLConnection connection = (HttpURLConnection) new URL(response.getUploadUrl()).openConnection();
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Content-Type", "application/pdf");
            connection.setDoOutput(true);
            connection.getOutputStream().write(testPdfData);

            int responseCode = connection.getResponseCode();
            assertThat(responseCode).isEqualTo(200);

            // AWS 콘솔 확인을 위한 정보 출력
            printS3FileInfo(response.getS3Key(), "PDF Document");

        } finally {
            cleanupTestFile(response.getS3Key());
        }
    }

    @Test
    @DisplayName("WORK_REPORT 리소스 타입으로 Presigned URL 발급")
    void generatePresignedUrl_WorkReportType_Success() {
        // Given
        PresignedUrlRequest request = PresignedUrlRequest.builder()
                .resourceType(ResourceType.WORK_REPORT)
                .resourceId("integration-test-work-" + System.currentTimeMillis())
                .signerRole(SignerRole.MANAGER)
                .fileExtension("png")
                .build();

        // When
        PresignedUrlResponse response = s3Service.generatePresignedUrl(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getS3Key()).contains("workreports");
        assertThat(response.getS3Key()).contains("MANAGER");
    }

    /**
     * 테스트용 1x1 투명 PNG 이미지 생성
     */
    private byte[] createTestPngImage() {
        // 최소한의 PNG 파일 (1x1 투명 픽셀)
        return new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, // PNG 시그니처
                0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52, // IHDR 청크
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, // 1x1 픽셀
                0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4, (byte) 0x89,
                0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41, 0x54, // IDAT 청크
                0x78, (byte) 0x9C, 0x63, 0x00, 0x01, 0x00, 0x00, 0x05, 0x00, 0x01,
                0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, // IEND 청크
                (byte) 0xAE, 0x42, 0x60, (byte) 0x82
        };
    }

    /**
     * 테스트용 간단한 PDF 데이터 생성
     */
    private byte[] createTestPdfData() {
        // 최소한의 PDF 파일
        String pdf = "%PDF-1.4\n" +
                "1 0 obj\n" +
                "<< /Type /Catalog /Pages 2 0 R >>\n" +
                "endobj\n" +
                "2 0 obj\n" +
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>\n" +
                "endobj\n" +
                "3 0 obj\n" +
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] >>\n" +
                "endobj\n" +
                "xref\n" +
                "0 4\n" +
                "0000000000 65535 f\n" +
                "0000000009 00000 n\n" +
                "0000000058 00000 n\n" +
                "0000000115 00000 n\n" +
                "trailer\n" +
                "<< /Size 4 /Root 1 0 R >>\n" +
                "startxref\n" +
                "190\n" +
                "%%EOF";
        return pdf.getBytes();
    }

    /**
     * AWS 콘솔 확인을 위한 S3 파일 정보 출력
     */
    private void printS3FileInfo(String s3Key, String fileType) {
        boolean keepFiles = "true".equalsIgnoreCase(System.getenv("KEEP_TEST_FILES"));

        System.out.println("\n========================================");
        System.out.println("AWS S3 파일 업로드 성공!");
        System.out.println("========================================");
        System.out.println("파일 타입: " + fileType);
        System.out.println("버킷 이름: " + bucketName);
        System.out.println("S3 키: " + s3Key);
        System.out.println("AWS 콘솔 링크: https://s3.console.aws.amazon.com/s3/object/" + bucketName + "?prefix=" + s3Key);
        System.out.println("파일 보존: " + (keepFiles ? "예 (수동 삭제 필요)" : "아니오 (자동 삭제됨)"));
        System.out.println("========================================\n");
    }

    /**
     * 테스트 후 S3에서 파일 삭제
     * KEEP_TEST_FILES=true 환경변수가 설정되어 있으면 삭제하지 않음
     */
    private void cleanupTestFile(String s3Key) {
        // KEEP_TEST_FILES 환경변수 확인
        boolean keepFiles = "true".equalsIgnoreCase(System.getenv("KEEP_TEST_FILES"));

        if (keepFiles) {
            System.out.println("⚠️  파일 보존됨 (KEEP_TEST_FILES=true): " + s3Key);
            System.out.println("   수동으로 삭제하려면: aws s3 rm s3://" + bucketName + "/" + s3Key);
            return;
        }

        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();
            s3Client.deleteObject(deleteRequest);
            System.out.println("✓ 테스트 파일 삭제됨: " + s3Key);
        } catch (Exception e) {
            System.err.println("Failed to cleanup test file: " + s3Key + " - " + e.getMessage());
        }
    }
}