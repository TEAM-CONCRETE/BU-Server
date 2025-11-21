package com.concrete.buildup.domain.document.service;

import com.concrete.buildup.domain.contract.entity.Contract;
import com.concrete.buildup.domain.contract.entity.ContractDetail;
import com.concrete.buildup.domain.contract.enums.ContractState;
import com.concrete.buildup.domain.contract.enums.EmpType;
import com.concrete.buildup.domain.contract.repository.ContractDetailRepository;
import com.concrete.buildup.domain.contract.repository.ContractRepository;
import com.concrete.buildup.domain.document.dto.DocumentUrlResponseDto;
import com.concrete.buildup.domain.payroll.entity.Payroll;
import com.concrete.buildup.domain.payroll.enums.PayStatus;
import com.concrete.buildup.domain.payroll.repository.PayrollRepository;
import com.concrete.buildup.domain.upload.service.S3Service;
import com.concrete.buildup.global.exception.BusinessException;
import com.concrete.buildup.global.exception.errorcode.DocumentErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentService 테스트")
class DocumentServiceTest {

    @Mock
    private S3Service s3Service;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private ContractDetailRepository contractDetailRepository;

    @Mock
    private PayrollRepository payrollRepository;

    @InjectMocks
    private DocumentService documentService;

    @Nested
    @DisplayName("근로계약서 PDF URL 발급")
    class GetContractPdfUrl {

        @Test
        @DisplayName("성공 - FULLY_SIGNED 상태")
        void success_fullySigned() {
            // given
            Long contractId = 1L;
            LocalDateTime writtenAt = LocalDateTime.of(2025, 1, 12, 10, 0);
            String expectedS3Key = "contracts/1/홍길동_PERMANENT_20250112_v3_final.pdf";
            String expectedSignedUrl = "https://bucket.s3.amazonaws.com/signed-url";

            Contract contract = Contract.builder()
                    .employeeId(100L)
                    .corporationId(1L)
                    .empType(EmpType.PERMANENT)
                    .contractState(ContractState.FULLY_SIGNED)
                    .employeeStartDate(LocalDate.of(2025, 1, 1))
                    .writtenAt(writtenAt)
                    .build();

            ContractDetail contractDetail = mock(ContractDetail.class);
            given(contractDetail.getEmpName()).willReturn("홍길동");

            given(contractRepository.findById(contractId)).willReturn(Optional.of(contract));
            given(contractDetailRepository.findByContractId(contractId)).willReturn(Optional.of(contractDetail));
            given(s3Service.doesObjectExist(expectedS3Key)).willReturn(true);
            given(s3Service.generatePresignedGetUrl(expectedS3Key)).willReturn(expectedSignedUrl);

            // when
            DocumentUrlResponseDto response = documentService.getContractPdfUrl(contractId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getUrl()).isEqualTo(expectedSignedUrl);
            assertThat(response.getExpiresAt()).isNotNull();

            verify(contractRepository).findById(contractId);
            verify(contractDetailRepository).findByContractId(contractId);
            verify(s3Service).doesObjectExist(expectedS3Key);
            verify(s3Service).generatePresignedGetUrl(expectedS3Key);
        }

        @Test
        @DisplayName("성공 - MANAGER_SIGNING_PENDING 상태 (v1_draft)")
        void success_managerSigningPending() {
            // given
            Long contractId = 1L;
            LocalDateTime writtenAt = LocalDateTime.of(2025, 1, 12, 10, 0);
            String expectedS3Key = "contracts/1/홍길동_PERMANENT_20250112_v1_draft.pdf";
            String expectedSignedUrl = "https://bucket.s3.amazonaws.com/signed-url";

            Contract contract = Contract.builder()
                    .employeeId(100L)
                    .corporationId(1L)
                    .empType(EmpType.PERMANENT)
                    .contractState(ContractState.MANAGER_SIGNING_PENDING)
                    .employeeStartDate(LocalDate.of(2025, 1, 1))
                    .writtenAt(writtenAt)
                    .build();

            ContractDetail contractDetail = mock(ContractDetail.class);
            given(contractDetail.getEmpName()).willReturn("홍길동");

            given(contractRepository.findById(contractId)).willReturn(Optional.of(contract));
            given(contractDetailRepository.findByContractId(contractId)).willReturn(Optional.of(contractDetail));
            given(s3Service.doesObjectExist(expectedS3Key)).willReturn(true);
            given(s3Service.generatePresignedGetUrl(expectedS3Key)).willReturn(expectedSignedUrl);

            // when
            DocumentUrlResponseDto response = documentService.getContractPdfUrl(contractId);

            // then
            assertThat(response.getUrl()).isEqualTo(expectedSignedUrl);
            verify(s3Service).doesObjectExist(expectedS3Key);
        }

        @Test
        @DisplayName("성공 - EMPLOYEE_SIGNING_PENDING 상태 (v2_manager_signed)")
        void success_employeeSigningPending() {
            // given
            Long contractId = 1L;
            LocalDateTime writtenAt = LocalDateTime.of(2025, 1, 12, 10, 0);
            String expectedS3Key = "contracts/1/홍길동_PERMANENT_20250112_v2_manager_signed.pdf";
            String expectedSignedUrl = "https://bucket.s3.amazonaws.com/signed-url";

            Contract contract = Contract.builder()
                    .employeeId(100L)
                    .corporationId(1L)
                    .empType(EmpType.PERMANENT)
                    .contractState(ContractState.EMPLOYEE_SIGNING_PENDING)
                    .employeeStartDate(LocalDate.of(2025, 1, 1))
                    .writtenAt(writtenAt)
                    .build();

            ContractDetail contractDetail = mock(ContractDetail.class);
            given(contractDetail.getEmpName()).willReturn("홍길동");

            given(contractRepository.findById(contractId)).willReturn(Optional.of(contract));
            given(contractDetailRepository.findByContractId(contractId)).willReturn(Optional.of(contractDetail));
            given(s3Service.doesObjectExist(expectedS3Key)).willReturn(true);
            given(s3Service.generatePresignedGetUrl(expectedS3Key)).willReturn(expectedSignedUrl);

            // when
            DocumentUrlResponseDto response = documentService.getContractPdfUrl(contractId);

            // then
            assertThat(response.getUrl()).isEqualTo(expectedSignedUrl);
            verify(s3Service).doesObjectExist(expectedS3Key);
        }

        @Test
        @DisplayName("실패 - 계약 없음")
        void fail_contractNotFound() {
            // given
            Long contractId = 999L;
            given(contractRepository.findById(contractId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> documentService.getContractPdfUrl(contractId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", DocumentErrorCode.CONTRACT_NOT_FOUND);

            verify(contractRepository).findById(contractId);
            verify(s3Service, never()).doesObjectExist(anyString());
        }

        @Test
        @DisplayName("실패 - DRAFT 상태 (PDF 미생성)")
        void fail_draftState() {
            // given
            Long contractId = 1L;
            Contract contract = Contract.builder()
                    .employeeId(100L)
                    .corporationId(1L)
                    .empType(EmpType.PERMANENT)
                    .contractState(ContractState.DRAFT)
                    .employeeStartDate(LocalDate.of(2025, 1, 1))
                    .build();

            ContractDetail contractDetail = mock(ContractDetail.class);

            given(contractRepository.findById(contractId)).willReturn(Optional.of(contract));
            given(contractDetailRepository.findByContractId(contractId)).willReturn(Optional.of(contractDetail));

            // when & then
            assertThatThrownBy(() -> documentService.getContractPdfUrl(contractId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", DocumentErrorCode.DOCUMENT_NOT_FOUND);

            verify(s3Service, never()).doesObjectExist(anyString());
        }

        @Test
        @DisplayName("실패 - S3 파일 없음")
        void fail_s3FileNotFound() {
            // given
            Long contractId = 1L;
            LocalDateTime writtenAt = LocalDateTime.of(2025, 1, 12, 10, 0);
            String expectedS3Key = "contracts/1/홍길동_PERMANENT_20250112_v3_final.pdf";

            Contract contract = Contract.builder()
                    .employeeId(100L)
                    .corporationId(1L)
                    .empType(EmpType.PERMANENT)
                    .contractState(ContractState.FULLY_SIGNED)
                    .employeeStartDate(LocalDate.of(2025, 1, 1))
                    .writtenAt(writtenAt)
                    .build();

            ContractDetail contractDetail = mock(ContractDetail.class);
            given(contractDetail.getEmpName()).willReturn("홍길동");

            given(contractRepository.findById(contractId)).willReturn(Optional.of(contract));
            given(contractDetailRepository.findByContractId(contractId)).willReturn(Optional.of(contractDetail));
            given(s3Service.doesObjectExist(expectedS3Key)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> documentService.getContractPdfUrl(contractId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", DocumentErrorCode.DOCUMENT_NOT_FOUND);

            verify(s3Service).doesObjectExist(expectedS3Key);
            verify(s3Service, never()).generatePresignedGetUrl(anyString());
        }
    }

    @Nested
    @DisplayName("급여명세서 PDF URL 발급")
    class GetPayslipPdfUrl {

        @Test
        @DisplayName("성공")
        void success() {
            // given
            Long payrollId = 1L;
            String expectedS3Key = "payroll/EMP100/2025/09/payslip-1.pdf";
            String expectedSignedUrl = "https://bucket.s3.amazonaws.com/signed-url";

            Payroll payroll = Payroll.builder()
                    .employeeId(100L)
                    .contractId(1L)
                    .corporationId(1L)
                    .siteId(1L)
                    .salaryYear(2025)
                    .salaryMonth(9)
                    .salaryWeek(0)
                    .salaryDay(LocalDate.of(2025, 9, 1))
                    .searchDate(LocalDate.of(2025, 9, 1))
                    .payStatus(PayStatus.PAID)
                    .s3Key(expectedS3Key)
                    .build();

            given(payrollRepository.findById(payrollId)).willReturn(Optional.of(payroll));
            given(s3Service.doesObjectExist(expectedS3Key)).willReturn(true);
            given(s3Service.generatePresignedGetUrl(expectedS3Key)).willReturn(expectedSignedUrl);

            // when
            DocumentUrlResponseDto response = documentService.getPayslipPdfUrl(payrollId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getUrl()).isEqualTo(expectedSignedUrl);
            assertThat(response.getExpiresAt()).isNotNull();

            verify(payrollRepository).findById(payrollId);
            verify(s3Service).doesObjectExist(expectedS3Key);
            verify(s3Service).generatePresignedGetUrl(expectedS3Key);
        }

        @Test
        @DisplayName("실패 - 급여명세서 없음")
        void fail_payrollNotFound() {
            // given
            Long payrollId = 999L;
            given(payrollRepository.findById(payrollId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> documentService.getPayslipPdfUrl(payrollId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", DocumentErrorCode.PAYROLL_NOT_FOUND);

            verify(payrollRepository).findById(payrollId);
            verify(s3Service, never()).doesObjectExist(anyString());
        }

        @Test
        @DisplayName("실패 - s3Key 없음")
        void fail_noS3Key() {
            // given
            Long payrollId = 1L;
            Payroll payroll = Payroll.builder()
                    .employeeId(100L)
                    .contractId(1L)
                    .corporationId(1L)
                    .siteId(1L)
                    .salaryYear(2025)
                    .salaryMonth(9)
                    .salaryWeek(0)
                    .salaryDay(LocalDate.of(2025, 9, 1))
                    .searchDate(LocalDate.of(2025, 9, 1))
                    .payStatus(PayStatus.PENDING)
                    .build();

            given(payrollRepository.findById(payrollId)).willReturn(Optional.of(payroll));

            // when & then
            assertThatThrownBy(() -> documentService.getPayslipPdfUrl(payrollId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", DocumentErrorCode.DOCUMENT_NOT_FOUND);

            verify(payrollRepository).findById(payrollId);
            verify(s3Service, never()).doesObjectExist(anyString());
        }

        @Test
        @DisplayName("실패 - S3 파일 없음")
        void fail_s3FileNotFound() {
            // given
            Long payrollId = 1L;
            String expectedS3Key = "payroll/EMP100/2025/09/payslip-1.pdf";

            Payroll payroll = Payroll.builder()
                    .employeeId(100L)
                    .contractId(1L)
                    .corporationId(1L)
                    .siteId(1L)
                    .salaryYear(2025)
                    .salaryMonth(9)
                    .salaryWeek(0)
                    .salaryDay(LocalDate.of(2025, 9, 1))
                    .searchDate(LocalDate.of(2025, 9, 1))
                    .payStatus(PayStatus.PAID)
                    .s3Key(expectedS3Key)
                    .build();

            given(payrollRepository.findById(payrollId)).willReturn(Optional.of(payroll));
            given(s3Service.doesObjectExist(expectedS3Key)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> documentService.getPayslipPdfUrl(payrollId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", DocumentErrorCode.DOCUMENT_NOT_FOUND);

            verify(payrollRepository).findById(payrollId);
            verify(s3Service).doesObjectExist(expectedS3Key);
            verify(s3Service, never()).generatePresignedGetUrl(anyString());
        }
    }
}