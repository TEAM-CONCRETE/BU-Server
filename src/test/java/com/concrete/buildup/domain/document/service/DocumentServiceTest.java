package com.concrete.buildup.domain.document.service;

import com.concrete.buildup.domain.contract.repository.ContractRepository;
import com.concrete.buildup.domain.document.dto.DocumentUrlResponseDto;
import com.concrete.buildup.domain.payroll.repository.PayrollRepository;
import com.concrete.buildup.domain.upload.service.S3Service;
import com.concrete.buildup.global.exception.BusinessException;
import com.concrete.buildup.global.exception.errorcode.DocumentErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * DocumentService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentService 테스트")
class DocumentServiceTest {

    @Mock
    private S3Service s3Service;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private PayrollRepository payrollRepository;

    @InjectMocks
    private DocumentService documentService;

    // ========== 근로계약서 PDF URL 발급 테스트 ==========

    @Test
    @DisplayName("근로계약서 PDF URL 발급 성공")
    void getContractPdfUrl_Success() {
        // given
        Long contractId = 1L;
        String expectedS3Key = "contracts/1/signed_final.pdf";
        String expectedUrl = "https://s3.amazonaws.com/bucket/contracts/1/signed_final.pdf?signed";

        given(contractRepository.existsById(contractId)).willReturn(true);
        given(s3Service.doesObjectExist(expectedS3Key)).willReturn(true);
        given(s3Service.generatePresignedGetUrl(expectedS3Key)).willReturn(expectedUrl);

        // when
        DocumentUrlResponseDto response = documentService.getContractPdfUrl(contractId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getUrl()).isEqualTo(expectedUrl);
        assertThat(response.getExpiresAt()).isNotNull();

        verify(contractRepository, times(1)).existsById(contractId);
        verify(s3Service, times(1)).doesObjectExist(expectedS3Key);
        verify(s3Service, times(1)).generatePresignedGetUrl(expectedS3Key);
    }

    @Test
    @DisplayName("근로계약서 PDF URL 발급 실패 - 계약 없음")
    void getContractPdfUrl_Fail_ContractNotFound() {
        // given
        Long contractId = 999L;

        given(contractRepository.existsById(contractId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> documentService.getContractPdfUrl(contractId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", DocumentErrorCode.CONTRACT_NOT_FOUND);

        verify(contractRepository, times(1)).existsById(contractId);
        verify(s3Service, never()).doesObjectExist(anyString());
        verify(s3Service, never()).generatePresignedGetUrl(anyString());
    }

    @Test
    @DisplayName("근로계약서 PDF URL 발급 실패 - 파일 없음")
    void getContractPdfUrl_Fail_DocumentNotFound() {
        // given
        Long contractId = 1L;
        String expectedS3Key = "contracts/1/signed_final.pdf";

        given(contractRepository.existsById(contractId)).willReturn(true);
        given(s3Service.doesObjectExist(expectedS3Key)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> documentService.getContractPdfUrl(contractId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", DocumentErrorCode.DOCUMENT_NOT_FOUND);

        verify(contractRepository, times(1)).existsById(contractId);
        verify(s3Service, times(1)).doesObjectExist(expectedS3Key);
        verify(s3Service, never()).generatePresignedGetUrl(anyString());
    }

    // ========== 급여명세서 PDF URL 발급 테스트 ==========

    @Test
    @DisplayName("급여명세서 PDF URL 발급 성공")
    void getPayslipPdfUrl_Success() {
        // given
        Long payrollId = 1L;
        String expectedS3Key = "payslips/1/payslip.pdf";
        String expectedUrl = "https://s3.amazonaws.com/bucket/payslips/1/payslip.pdf?signed";

        given(payrollRepository.existsById(payrollId)).willReturn(true);
        given(s3Service.doesObjectExist(expectedS3Key)).willReturn(true);
        given(s3Service.generatePresignedGetUrl(expectedS3Key)).willReturn(expectedUrl);

        // when
        DocumentUrlResponseDto response = documentService.getPayslipPdfUrl(payrollId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getUrl()).isEqualTo(expectedUrl);
        assertThat(response.getExpiresAt()).isNotNull();

        verify(payrollRepository, times(1)).existsById(payrollId);
        verify(s3Service, times(1)).doesObjectExist(expectedS3Key);
        verify(s3Service, times(1)).generatePresignedGetUrl(expectedS3Key);
    }

    @Test
    @DisplayName("급여명세서 PDF URL 발급 실패 - 급여명세서 없음")
    void getPayslipPdfUrl_Fail_PayrollNotFound() {
        // given
        Long payrollId = 999L;

        given(payrollRepository.existsById(payrollId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> documentService.getPayslipPdfUrl(payrollId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", DocumentErrorCode.PAYROLL_NOT_FOUND);

        verify(payrollRepository, times(1)).existsById(payrollId);
        verify(s3Service, never()).doesObjectExist(anyString());
        verify(s3Service, never()).generatePresignedGetUrl(anyString());
    }

    @Test
    @DisplayName("급여명세서 PDF URL 발급 실패 - 파일 없음")
    void getPayslipPdfUrl_Fail_DocumentNotFound() {
        // given
        Long payrollId = 1L;
        String expectedS3Key = "payslips/1/payslip.pdf";

        given(payrollRepository.existsById(payrollId)).willReturn(true);
        given(s3Service.doesObjectExist(expectedS3Key)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> documentService.getPayslipPdfUrl(payrollId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", DocumentErrorCode.DOCUMENT_NOT_FOUND);

        verify(payrollRepository, times(1)).existsById(payrollId);
        verify(s3Service, times(1)).doesObjectExist(expectedS3Key);
        verify(s3Service, never()).generatePresignedGetUrl(anyString());
    }
}