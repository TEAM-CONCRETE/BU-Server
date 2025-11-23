package com.concrete.buildup.domain.upload.service;

import com.concrete.buildup.domain.contract.enums.SignerRole;
import com.concrete.buildup.domain.upload.dto.PresignedUrlRequest;
import com.concrete.buildup.domain.upload.dto.PresignedUrlResponse;
import com.concrete.buildup.domain.upload.enums.ResourceType;
import com.concrete.buildup.global.exception.BusinessException;
import com.concrete.buildup.global.exception.errorcode.S3ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * S3Service 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @InjectMocks
    private S3Service s3Service;

    @Nested
    @DisplayName("Presigned URL 생성 테스트")
    class GeneratePresignedUrlTest {

        @Test
        @DisplayName("정상적인 Presigned URL 생성")
        void generatePresignedUrl_Success() throws Exception {
            // Given
            PresignedUrlRequest request = PresignedUrlRequest.builder()
                    .resourceType(ResourceType.CONTRACT)
                    .resourceId("123")
                    .signerRole(SignerRole.EMPLOYEE)
                    .fileExtension("png")
                    .build();

            URL mockUrl = new URL("https://build-up-contracts.s3.ap-northeast-2.amazonaws.com/uploads/contracts/123/EMPLOYEE.png");
            PresignedPutObjectRequest mockPresignedRequest = mock(PresignedPutObjectRequest.class);
            Instant mockExpiration = Instant.now().plusSeconds(900); // 15분 후

            when(mockPresignedRequest.url()).thenReturn(mockUrl);
            when(mockPresignedRequest.expiration()).thenReturn(mockExpiration);
            when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
                    .thenReturn(mockPresignedRequest);

            // When
            PresignedUrlResponse response = s3Service.generatePresignedUrl(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getUploadUrl()).contains("build-up-contracts");
            // CONTRACT는 기존 패턴 유지: uploads/contracts/{id}/{role}.{ext}
            assertThat(response.getS3Key()).isEqualTo("uploads/contracts/123/EMPLOYEE.png");
            assertThat(response.getExpiresAt()).isAfter(LocalDateTime.now());

            verify(s3Presigner).presignPutObject(any(PutObjectPresignRequest.class));
        }

        @Test
        @DisplayName("CONTRACT 리소스 타입으로 Presigned URL 생성")
        void generatePresignedUrl_ContractType_Success() throws Exception {
            // Given
            PresignedUrlRequest request = PresignedUrlRequest.builder()
                    .resourceType(ResourceType.CONTRACT)
                    .resourceId("456")
                    .signerRole(SignerRole.MANAGER)
                    .fileExtension("png")
                    .build();

            URL mockUrl = new URL("https://build-up-contracts.s3.ap-northeast-2.amazonaws.com/uploads/contracts/456/MANAGER.png");
            PresignedPutObjectRequest mockPresignedRequest = mock(PresignedPutObjectRequest.class);
            Instant mockExpiration = Instant.now().plusSeconds(900); // 15분 후

            when(mockPresignedRequest.url()).thenReturn(mockUrl);
            when(mockPresignedRequest.expiration()).thenReturn(mockExpiration);
            when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
                    .thenReturn(mockPresignedRequest);

            // When
            PresignedUrlResponse response = s3Service.generatePresignedUrl(request);

            // Then
            // CONTRACT는 기존 패턴 유지: uploads/contracts/{id}/{role}.{ext}
            assertThat(response.getS3Key()).isEqualTo("uploads/contracts/456/MANAGER.png");
        }

        @Test
        @DisplayName("PDF 파일 확장자로 Presigned URL 생성")
        void generatePresignedUrl_PdfExtension_Success() throws Exception {
            // Given
            PresignedUrlRequest request = PresignedUrlRequest.builder()
                    .resourceType(ResourceType.CONTRACT)
                    .resourceId("789")
                    .signerRole(SignerRole.CORPORATION)
                    .fileExtension("pdf")
                    .build();

            URL mockUrl = new URL("https://build-up-contracts.s3.ap-northeast-2.amazonaws.com/uploads/contracts/789/CORPORATION.pdf");
            PresignedPutObjectRequest mockPresignedRequest = mock(PresignedPutObjectRequest.class);
            Instant mockExpiration = Instant.now().plusSeconds(900); // 15분 후

            when(mockPresignedRequest.url()).thenReturn(mockUrl);
            when(mockPresignedRequest.expiration()).thenReturn(mockExpiration);
            when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
                    .thenReturn(mockPresignedRequest);

            // When
            PresignedUrlResponse response = s3Service.generatePresignedUrl(request);

            // Then
            // CONTRACT는 기존 패턴 유지: uploads/contracts/{id}/{role}.{ext}
            assertThat(response.getS3Key()).isEqualTo("uploads/contracts/789/CORPORATION.pdf");
        }

        @Test
        @DisplayName("S3Presigner 예외 발생 시 BusinessException 발생")
        void generatePresignedUrl_S3PresignerError_ThrowsBusinessException() {
            // Given
            PresignedUrlRequest request = PresignedUrlRequest.builder()
                    .resourceType(ResourceType.CONTRACT)
                    .resourceId("123")
                    .signerRole(SignerRole.EMPLOYEE)
                    .fileExtension("png")
                    .build();

            when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
                    .thenThrow(new RuntimeException("S3 connection failed"));

            // When & Then
            assertThatThrownBy(() -> s3Service.generatePresignedUrl(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Presigned URL 생성에 실패했습니다");

            verify(s3Presigner).presignPutObject(any(PutObjectPresignRequest.class));
        }
    }

    @Nested
    @DisplayName("이미지 다운로드 테스트")
    class DownloadImageTest {

        @Test
        @DisplayName("정상적인 이미지 다운로드")
        void downloadImage_Success() {
            // Given
            String s3Key = "uploads/contracts/123/EMPLOYEE.png";
            byte[] expectedBytes = new byte[]{1, 2, 3, 4, 5};

            ResponseBytes<GetObjectResponse> mockResponse = mock(ResponseBytes.class);
            when(mockResponse.asByteArray()).thenReturn(expectedBytes);
            when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                    .thenReturn(mockResponse);

            // When
            byte[] result = s3Service.downloadImage(s3Key);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(expectedBytes);
            assertThat(result).hasSize(5);

            verify(s3Client).getObjectAsBytes(any(GetObjectRequest.class));
        }

        @Test
        @DisplayName("S3Client 예외 발생 시 BusinessException 발생")
        void downloadImage_S3ClientError_ThrowsBusinessException() {
            // Given
            String s3Key = "uploads/contracts/123/EMPLOYEE.png";

            when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                    .thenThrow(new RuntimeException("File not found"));

            // When & Then
            assertThatThrownBy(() -> s3Service.downloadImage(s3Key))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("파일 다운로드에 실패했습니다");

            verify(s3Client).getObjectAsBytes(any(GetObjectRequest.class));
        }

        @Test
        @DisplayName("빈 파일 다운로드")
        void downloadImage_EmptyFile_ReturnsEmptyBytes() {
            // Given
            String s3Key = "uploads/contracts/123/EMPLOYEE.png";
            byte[] emptyBytes = new byte[0];

            ResponseBytes<GetObjectResponse> mockResponse = mock(ResponseBytes.class);
            when(mockResponse.asByteArray()).thenReturn(emptyBytes);
            when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                    .thenReturn(mockResponse);

            // When
            byte[] result = s3Service.downloadImage(s3Key);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).isEmpty();

            verify(s3Client).getObjectAsBytes(any(GetObjectRequest.class));
        }
    }

    @Nested
    @DisplayName("PDF 다운로드 테스트")
    class DownloadPdfTest {

        @Test
        @DisplayName("정상적인 PDF 다운로드")
        void downloadPdf_Success() {
            // Given
            String s3Key = "contracts/1/v1.pdf";
            byte[] expectedPdfData = "PDF content".getBytes();

            ResponseBytes<GetObjectResponse> mockResponse = mock(ResponseBytes.class);
            when(mockResponse.asByteArray()).thenReturn(expectedPdfData);
            when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                    .thenReturn(mockResponse);

            // When
            byte[] result = s3Service.downloadPdf(s3Key);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(expectedPdfData);
            assertThat(result.length).isEqualTo(expectedPdfData.length);

            verify(s3Client).getObjectAsBytes(any(GetObjectRequest.class));
        }

        @Test
        @DisplayName("S3Client 예외 발생 시 BusinessException 발생")
        void downloadPdf_S3ClientError_ThrowsBusinessException() {
            // Given
            String s3Key = "contracts/1/v1.pdf";

            when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                    .thenThrow(new RuntimeException("File not found"));

            // When & Then
            assertThatThrownBy(() -> s3Service.downloadPdf(s3Key))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("파일 다운로드에 실패했습니다");

            verify(s3Client).getObjectAsBytes(any(GetObjectRequest.class));
        }
    }

    @Nested
    @DisplayName("PDF 업로드 테스트")
    class UploadPdfTest {

        @Test
        @DisplayName("정상적인 PDF 업로드")
        void uploadPdf_Success() {
            // Given
            String s3Key = "contracts/1/v2.pdf";
            byte[] pdfBytes = "PDF content v2".getBytes();

            // When
            s3Service.uploadPdf(s3Key, pdfBytes);

            // Then
            verify(s3Client).putObject(
                    argThat((PutObjectRequest request) ->
                            request.key().equals(s3Key) &&
                            request.contentType().equals("application/pdf")
                    ),
                    any(RequestBody.class)
            );
        }

        @Test
        @DisplayName("S3Client 예외 발생 시 BusinessException 발생")
        void uploadPdf_S3ClientError_ThrowsBusinessException() {
            // Given
            String s3Key = "contracts/1/v2.pdf";
            byte[] pdfBytes = "PDF content".getBytes();

            doThrow(new RuntimeException("Upload failed")).when(s3Client).putObject(
                    any(PutObjectRequest.class),
                    any(RequestBody.class)
            );

            // When & Then
            assertThatThrownBy(() -> s3Service.uploadPdf(s3Key, pdfBytes))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("파일 업로드에 실패했습니다");

            verify(s3Client).putObject(
                    any(PutObjectRequest.class),
                    any(RequestBody.class)
            );
        }
    }

    @Nested
    @DisplayName("PDF URL 생성 테스트")
    class GetPdfUrlTest {

        @Test
        @DisplayName("정상적인 PDF URL 생성")
        void getPdfUrl_Success() {
            // Given
            String s3Key = "contracts/1/v3.pdf";
            String bucketName = "test-bucket";
            ReflectionTestUtils.setField(s3Service, "bucketName", bucketName);

            String expectedUrl = String.format("https://%s.s3.amazonaws.com/%s", bucketName, s3Key);

            // When
            String actualUrl = s3Service.getPdfUrl(s3Key);

            // Then
            assertThat(actualUrl).isEqualTo(expectedUrl);
            assertThat(actualUrl).contains(bucketName);
            assertThat(actualUrl).contains(s3Key);
            assertThat(actualUrl).startsWith("https://");
            assertThat(actualUrl).contains(".s3.amazonaws.com/");
        }

        @Test
        @DisplayName("여러 경로에 대한 PDF URL 생성")
        void getPdfUrl_MultiplePaths() {
            // Given
            String bucketName = "my-bucket";
            ReflectionTestUtils.setField(s3Service, "bucketName", bucketName);

            // When & Then
            String url1 = s3Service.getPdfUrl("contracts/1/v1.pdf");
            assertThat(url1).isEqualTo("https://my-bucket.s3.amazonaws.com/contracts/1/v1.pdf");

            String url2 = s3Service.getPdfUrl("contracts/100/v2.pdf");
            assertThat(url2).isEqualTo("https://my-bucket.s3.amazonaws.com/contracts/100/v2.pdf");

            String url3 = s3Service.getPdfUrl("contracts/999/v3.pdf");
            assertThat(url3).isEqualTo("https://my-bucket.s3.amazonaws.com/contracts/999/v3.pdf");
        }
    }
}
