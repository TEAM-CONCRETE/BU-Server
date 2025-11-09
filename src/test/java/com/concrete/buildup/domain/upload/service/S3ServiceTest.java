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
import software.amazon.awssdk.core.ResponseBytes;
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
            assertThat(response.getUploadUrl()).contains("uploads/contracts/123/EMPLOYEE.png");
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
            assertThat(response.getS3Key()).isEqualTo("uploads/contracts/456/MANAGER.png");
            assertThat(response.getUploadUrl()).contains("MANAGER.png");
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
            assertThat(response.getS3Key()).isEqualTo("uploads/contracts/789/CORPORATION.pdf");
            assertThat(response.getUploadUrl()).contains("CORPORATION.pdf");
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
}
