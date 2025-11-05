package com.concrete.buildup.domain.upload.controller;

import com.concrete.buildup.domain.contract.enums.SignerRole;
import com.concrete.buildup.domain.upload.dto.PresignedUrlRequest;
import com.concrete.buildup.domain.upload.dto.PresignedUrlResponse;
import com.concrete.buildup.domain.upload.enums.ResourceType;
import com.concrete.buildup.domain.upload.service.S3Service;
import com.concrete.buildup.global.exception.BusinessException;
import com.concrete.buildup.global.exception.errorcode.S3ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * UploadController 테스트
 */
@WebMvcTest(UploadController.class)
class UploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private S3Service s3Service;

    @Nested
    @DisplayName("POST /v1/uploads/signatures - Presigned URL 발급")
    class GeneratePresignedUrlTest {

        @Test
        @DisplayName("정상적인 Presigned URL 발급")
        @WithMockUser
        void generatePresignedUrl_Success() throws Exception {
            // Given
            PresignedUrlRequest request = PresignedUrlRequest.builder()
                    .resourceType(ResourceType.CONTRACT)
                    .resourceId("123")
                    .signerRole(SignerRole.EMPLOYEE)
                    .fileExtension("png")
                    .build();

            PresignedUrlResponse mockResponse = PresignedUrlResponse.builder()
                    .uploadUrl("https://build-up-contracts.s3.ap-northeast-2.amazonaws.com/uploads/contracts/123/EMPLOYEE.png?signature=xxx")
                    .expiresAt(LocalDateTime.now().plusMinutes(15))
                    .s3Key("uploads/contracts/123/EMPLOYEE.png")
                    .bucket("build-up-contracts")
                    .build();

            when(s3Service.generatePresignedUrl(any(PresignedUrlRequest.class)))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(post("/v1/uploads/signatures")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Presigned URL이 발급되었습니다."))
                    .andExpect(jsonPath("$.data.uploadUrl").value(mockResponse.getUploadUrl()))
                    .andExpect(jsonPath("$.data.s3Key").value("uploads/contracts/123/EMPLOYEE.png"))
                    .andExpect(jsonPath("$.data.bucket").value("build-up-contracts"))
                    .andExpect(jsonPath("$.data.expiresAt").exists());
        }

        @Test
        @DisplayName("PDF 파일 확장자로 Presigned URL 발급")
        @WithMockUser
        void generatePresignedUrl_PdfExtension_Success() throws Exception {
            // Given
            PresignedUrlRequest request = PresignedUrlRequest.builder()
                    .resourceType(ResourceType.CONTRACT)
                    .resourceId("456")
                    .signerRole(SignerRole.CORPORATION)
                    .fileExtension("pdf")
                    .build();

            PresignedUrlResponse mockResponse = PresignedUrlResponse.builder()
                    .uploadUrl("https://build-up-contracts.s3.ap-northeast-2.amazonaws.com/uploads/contracts/456/CORPORATION.pdf?signature=xxx")
                    .expiresAt(LocalDateTime.now().plusMinutes(15))
                    .s3Key("uploads/contracts/456/CORPORATION.pdf")
                    .bucket("build-up-contracts")
                    .build();

            when(s3Service.generatePresignedUrl(any(PresignedUrlRequest.class)))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(post("/v1/uploads/signatures")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.s3Key").value("uploads/contracts/456/CORPORATION.pdf"));
        }

        @Test
        @DisplayName("필수 필드 누락 시 400 에러")
        @WithMockUser
        void generatePresignedUrl_MissingRequiredFields_Returns400() throws Exception {
            // Given - resourceType 누락
            String invalidRequest = """
                {
                    "resourceId": "123",
                    "signerRole": "EMPLOYEE",
                    "fileExtension": "png"
                }
                """;

            // When & Then
            mockMvc.perform(post("/v1/uploads/signatures")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("잘못된 파일 확장자로 요청 시 400 에러")
        @WithMockUser
        void generatePresignedUrl_InvalidFileExtension_Returns400() throws Exception {
            // Given - 지원하지 않는 확장자
            String invalidRequest = """
                {
                    "resourceType": "CONTRACT",
                    "resourceId": "123",
                    "signerRole": "EMPLOYEE",
                    "fileExtension": "exe"
                }
                """;

            // When & Then
            mockMvc.perform(post("/v1/uploads/signatures")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("빈 resourceId로 요청 시 400 에러")
        @WithMockUser
        void generatePresignedUrl_EmptyResourceId_Returns400() throws Exception {
            // Given
            String invalidRequest = """
                {
                    "resourceType": "CONTRACT",
                    "resourceId": "",
                    "signerRole": "EMPLOYEE",
                    "fileExtension": "png"
                }
                """;

            // When & Then
            mockMvc.perform(post("/v1/uploads/signatures")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("인증 없이 요청 시 401 에러")
        void generatePresignedUrl_Unauthorized_Returns401() throws Exception {
            // Given
            PresignedUrlRequest request = PresignedUrlRequest.builder()
                    .resourceType(ResourceType.CONTRACT)
                    .resourceId("123")
                    .signerRole(SignerRole.EMPLOYEE)
                    .fileExtension("png")
                    .build();

            // When & Then
            mockMvc.perform(post("/v1/uploads/signatures")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("S3 서비스 오류 발생 시 500 에러")
        @WithMockUser
        void generatePresignedUrl_S3ServiceError_Returns500() throws Exception {
            // Given
            PresignedUrlRequest request = PresignedUrlRequest.builder()
                    .resourceType(ResourceType.CONTRACT)
                    .resourceId("123")
                    .signerRole(SignerRole.EMPLOYEE)
                    .fileExtension("png")
                    .build();

            when(s3Service.generatePresignedUrl(any(PresignedUrlRequest.class)))
                    .thenThrow(new BusinessException(S3ErrorCode.PRESIGNED_URL_GENERATION_FAILED));

            // When & Then
            mockMvc.perform(post("/v1/uploads/signatures")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("WORK_REPORT 리소스 타입으로 요청")
        @WithMockUser
        void generatePresignedUrl_WorkReportType_Success() throws Exception {
            // Given
            PresignedUrlRequest request = PresignedUrlRequest.builder()
                    .resourceType(ResourceType.WORK_REPORT)
                    .resourceId("789")
                    .signerRole(SignerRole.MANAGER)
                    .fileExtension("png")
                    .build();

            PresignedUrlResponse mockResponse = PresignedUrlResponse.builder()
                    .uploadUrl("https://build-up-contracts.s3.ap-northeast-2.amazonaws.com/uploads/workreports/789/MANAGER.png?signature=xxx")
                    .expiresAt(LocalDateTime.now().plusMinutes(15))
                    .s3Key("uploads/workreports/789/MANAGER.png")
                    .bucket("build-up-contracts")
                    .build();

            when(s3Service.generatePresignedUrl(any(PresignedUrlRequest.class)))
                    .thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(post("/v1/uploads/signatures")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.s3Key").value("uploads/workreports/789/MANAGER.png"));
        }
    }
}
