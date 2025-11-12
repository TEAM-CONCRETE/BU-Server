package com.concrete.buildup.domain.contract.service;

import com.concrete.buildup.domain.contract.dto.SignatureCompleteResponse;
import com.concrete.buildup.domain.contract.dto.SignatureCoordinates;
import com.concrete.buildup.domain.contract.entity.Contract;
import com.concrete.buildup.domain.contract.entity.ContractDetail;
import com.concrete.buildup.domain.contract.entity.ContractSignLog;
import com.concrete.buildup.domain.contract.enums.ContractState;
import com.concrete.buildup.domain.contract.enums.EmpType;
import com.concrete.buildup.domain.contract.enums.SignerRole;
import com.concrete.buildup.domain.contract.enums.VerificationStatus;
import com.concrete.buildup.domain.contract.repository.ContractDetailRepository;
import com.concrete.buildup.domain.contract.repository.ContractRepository;
import com.concrete.buildup.domain.contract.repository.ContractSignLogRepository;
import com.concrete.buildup.domain.upload.service.S3Service;
import com.concrete.buildup.global.exception.BusinessException;
import com.concrete.buildup.global.exception.errorcode.ContractErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * ContractSignatureService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ContractSignatureService 테스트")
class ContractSignatureServiceTest {

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private ContractDetailRepository contractDetailRepository;

    @Mock
    private ContractSignLogRepository signLogRepository;

    @Mock
    private S3Service s3Service;

    @Mock
    private PdfGenerationService pdfGenerationService;

    @InjectMocks
    private ContractSignatureService contractSignatureService;

    private Contract contract;
    private ContractDetail contractDetail;

    @BeforeEach
    void setUp() {
        // 테스트용 Contract 생성
        contract = Contract.builder()
                .employeeId(1L)
                .corporationId(1L)
                .managerId(1L)
                .empType(EmpType.PERMANENT)
                .contractState(ContractState.DRAFT)
                .employeeStartDate(LocalDate.of(2024, 1, 1))
                .build();

        // ID 설정 (저장된 상태 시뮬레이션)
        setId(contract, 100L);

        // 테스트용 ContractDetail 생성 (empName 추가)
        contractDetail = ContractDetail.builder()
                .contract(contract)
                .empName("홍길동") // buildS3Key()에서 사용되므로 필수
                .workPlace("서울시 강남구")
                .workType("일반건설현장근로자")
                .workPay(new BigDecimal("3000000"))
                .build();
    }

    /**
     * 초안 PDF 생성 테스트
     */
    @Test
    @DisplayName("초안 PDF 생성 성공")
    void generateInitialPdf_Success() {
        // given
        Long contractId = 100L;
        byte[] mockPdfBytes = "mock pdf content".getBytes();
        String expectedPdfUrl = "https://bucket.s3.amazonaws.com/contracts/100/홍길동_PERMANENT_20250112_v1_draft.pdf";

        given(contractRepository.findById(contractId)).willReturn(Optional.of(contract));
        given(contractDetailRepository.findByContractId(contractId)).willReturn(Optional.of(contractDetail));
        given(pdfGenerationService.generateContractPdf(contract, contractDetail)).willReturn(mockPdfBytes);
        given(s3Service.getPdfUrl(anyString())).willReturn(expectedPdfUrl);

        // when
        String pdfUrl = contractSignatureService.generateInitialPdf(contractId);

        // then
        assertThat(pdfUrl).isNotNull();
        assertThat(contract.getContractState()).isEqualTo(ContractState.MANAGER_SIGNING_PENDING);

        verify(pdfGenerationService).generateContractPdf(contract, contractDetail);
        verify(s3Service).uploadPdf(anyString(), eq(mockPdfBytes));
        verify(s3Service).getPdfUrl(anyString());
        verify(contractRepository).save(contract);
    }

    @Test
    @DisplayName("초안 PDF 생성 실패 - 계약 없음")
    void generateInitialPdf_ContractNotFound() {
        // given
        Long contractId = 999L;
        given(contractRepository.findById(contractId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> contractSignatureService.generateInitialPdf(contractId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ContractErrorCode.CONTRACT_NOT_FOUND);

        verify(contractDetailRepository, never()).findByContractId(any());
        verify(pdfGenerationService, never()).generateContractPdf(any(), any());
    }

    @Test
    @DisplayName("초안 PDF 생성 실패 - 계약 상세 없음")
    void generateInitialPdf_ContractDetailNotFound() {
        // given
        Long contractId = 100L;
        given(contractRepository.findById(contractId)).willReturn(Optional.of(contract));
        given(contractDetailRepository.findByContractId(contractId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> contractSignatureService.generateInitialPdf(contractId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ContractErrorCode.CONTRACT_DETAIL_NOT_FOUND);

        verify(pdfGenerationService, never()).generateContractPdf(any(), any());
    }

    /**
     * 관리자 서명 처리 테스트
     */
    @Test
    @DisplayName("관리자 서명 처리 성공")
    void processManagerSignature_Success() {
        // given
        Long contractId = 100L;
        String signatureS3Key = "uploads/CONTRACT/100/MANAGER.png";
        SignatureCoordinates coordinates = SignatureCoordinates.builder()
                .x(100.0)
                .y(200.0)
                .width(150.0)
                .height(50.0)
                .viewWidth(800.0)
                .viewHeight(1131.0)
                .build();

        // Contract 상태를 MANAGER_SIGNING_PENDING으로 설정
        contract = Contract.builder()
                .employeeId(1L)
                .corporationId(1L)
                .managerId(1L)
                .empType(EmpType.PERMANENT)
                .contractState(ContractState.MANAGER_SIGNING_PENDING)
                .employeeStartDate(LocalDate.of(2024, 1, 1))
                .build();
        setId(contract, contractId);

        byte[] signatureImageBytes = "signature image".getBytes();
        byte[] v1PdfBytes = "v1 pdf content".getBytes();
        byte[] v2PdfBytes = "v2 pdf content".getBytes();

        // 실제 해시 계산 (SignatureVerificationUtil이 계산할 값)
        String clientHash = com.concrete.buildup.global.util.SignatureVerificationUtil.calculateSHA256(
                new java.io.ByteArrayInputStream(signatureImageBytes)
        );

        given(contractRepository.findById(contractId)).willReturn(Optional.of(contract));
        given(contractDetailRepository.findByContractId(contractId)).willReturn(Optional.of(contractDetail));
        given(s3Service.downloadImage(signatureS3Key)).willReturn(signatureImageBytes);
        given(s3Service.downloadPdf(anyString())).willReturn(v1PdfBytes);
        given(pdfGenerationService.stampSignatureOnPdf(
                eq(v1PdfBytes),
                eq(signatureImageBytes),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(BigDecimal.class)
        )).willReturn(v2PdfBytes);
        given(s3Service.getPdfUrl(anyString()))
                .willReturn("https://bucket.s3.amazonaws.com/contracts/100/홍길동_PERMANENT_20250112_v2_manager_signed.pdf");

        // when
        SignatureCompleteResponse response = contractSignatureService.processManagerSignature(
                contractId,
                signatureS3Key,
                clientHash,
                coordinates,
                "192.168.1.1",
                "Chrome/Win10"
        );

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContractId()).isEqualTo(contractId);
        assertThat(response.getContractState()).isEqualTo(ContractState.EMPLOYEE_SIGNING_PENDING);
        assertThat(response.getPdfUrl()).isNotNull();
        assertThat(response.getPdfHash()).isNull(); // v2는 중간 단계이므로 null

        // Contract 상태 변경 확인
        assertThat(contract.getContractState()).isEqualTo(ContractState.EMPLOYEE_SIGNING_PENDING);
        assertThat(contract.getCorpSignedAt()).isNotNull();

        // ContractSignLog 저장 확인
        ArgumentCaptor<ContractSignLog> signLogCaptor = ArgumentCaptor.forClass(ContractSignLog.class);
        verify(signLogRepository).save(signLogCaptor.capture());

        ContractSignLog savedLog = signLogCaptor.getValue();
        assertThat(savedLog.getSignerRole()).isEqualTo(SignerRole.MANAGER);
        assertThat(savedLog.getSignerId()).isEqualTo(1L);
        assertThat(savedLog.getSignatureImageUrl()).isEqualTo(signatureS3Key);
        assertThat(savedLog.getSignedIp()).isEqualTo("192.168.1.1");
        assertThat(savedLog.getSignedDevice()).isEqualTo("Chrome/Win10");
        assertThat(savedLog.getVerificationStatus()).isEqualTo(VerificationStatus.VERIFIED);

        // S3 업로드 확인 (S3 키는 anyString()으로 검증)
        verify(s3Service).uploadPdf(anyString(), eq(v2PdfBytes));
    }

    @Test
    @DisplayName("관리자 서명 처리 실패 - 잘못된 계약 상태")
    void processManagerSignature_InvalidState() {
        // given
        Long contractId = 100L;
        contract = Contract.builder()
                .employeeId(1L)
                .corporationId(1L)
                .empType(EmpType.PERMANENT)
                .contractState(ContractState.DRAFT) // 잘못된 상태
                .employeeStartDate(LocalDate.of(2024, 1, 1))
                .build();
        setId(contract, contractId);

        given(contractRepository.findById(contractId)).willReturn(Optional.of(contract));
        given(contractDetailRepository.findByContractId(contractId)).willReturn(Optional.of(contractDetail));

        // when & then
        assertThatThrownBy(() -> contractSignatureService.processManagerSignature(
                contractId,
                "uploads/CONTRACT/100/MANAGER.png",
                "hash",
                SignatureCoordinates.builder()
                        .x(100.0).y(200.0).width(150.0).height(50.0)
                        .viewWidth(800.0).viewHeight(1131.0)
                        .build(),
                "192.168.1.1",
                "Chrome/Win10"
        ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ContractErrorCode.INVALID_CONTRACT_STATE);

        verify(s3Service, never()).downloadImage(any());
    }

    /**
     * 근로자 서명 처리 테스트
     */
    @Test
    @DisplayName("근로자 서명 처리 성공")
    void processEmployeeSignature_Success() {
        // given
        Long contractId = 100L;
        String signatureS3Key = "uploads/CONTRACT/100/EMPLOYEE.png";
        SignatureCoordinates coordinates = SignatureCoordinates.builder()
                .x(450.0)
                .y(200.0)
                .width(150.0)
                .height(50.0)
                .viewWidth(800.0)
                .viewHeight(1131.0)
                .build();

        // Contract 상태를 EMPLOYEE_SIGNING_PENDING으로 설정
        contract = Contract.builder()
                .employeeId(1L)
                .corporationId(1L)
                .managerId(1L)
                .empType(EmpType.PERMANENT)
                .contractState(ContractState.EMPLOYEE_SIGNING_PENDING)
                .employeeStartDate(LocalDate.of(2024, 1, 1))
                .build();
        setId(contract, contractId);

        byte[] signatureImageBytes = "employee signature".getBytes();
        byte[] v2PdfBytes = "v2 pdf content".getBytes();
        byte[] v3PdfBytes = "v3 pdf content".getBytes();

        // 실제 해시 계산
        String clientHash = com.concrete.buildup.global.util.SignatureVerificationUtil.calculateSHA256(
                new java.io.ByteArrayInputStream(signatureImageBytes)
        );

        given(contractRepository.findById(contractId)).willReturn(Optional.of(contract));
        given(contractDetailRepository.findByContractId(contractId)).willReturn(Optional.of(contractDetail));
        given(s3Service.downloadImage(signatureS3Key)).willReturn(signatureImageBytes);
        given(s3Service.downloadPdf(anyString())).willReturn(v2PdfBytes);
        given(pdfGenerationService.stampSignatureOnPdf(
                eq(v2PdfBytes),
                eq(signatureImageBytes),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(BigDecimal.class)
        )).willReturn(v3PdfBytes);
        given(s3Service.getPdfUrl(anyString()))
                .willReturn("https://bucket.s3.amazonaws.com/contracts/100/홍길동_PERMANENT_20250112_v3_final.pdf");

        // when
        SignatureCompleteResponse response = contractSignatureService.processEmployeeSignature(
                contractId,
                signatureS3Key,
                clientHash,
                coordinates,
                "192.168.1.2",
                "Safari/iOS"
        );

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContractId()).isEqualTo(contractId);
        assertThat(response.getContractState()).isEqualTo(ContractState.FULLY_SIGNED);
        assertThat(response.getPdfUrl()).isNotNull();
        assertThat(response.getPdfHash()).isNotNull(); // v3는 최종이므로 해시 포함

        // Contract 상태 및 최종 PDF 정보 확인
        assertThat(contract.getContractState()).isEqualTo(ContractState.FULLY_SIGNED);
        assertThat(contract.getFinalPdfUrl()).isNotNull();
        assertThat(contract.getFinalPdfHash()).isNotNull();
        assertThat(contract.getPdfGeneratedAt()).isNotNull();

        // ContractSignLog 저장 확인
        ArgumentCaptor<ContractSignLog> signLogCaptor = ArgumentCaptor.forClass(ContractSignLog.class);
        verify(signLogRepository).save(signLogCaptor.capture());

        ContractSignLog savedLog = signLogCaptor.getValue();
        assertThat(savedLog.getSignerRole()).isEqualTo(SignerRole.EMPLOYEE);
        assertThat(savedLog.getSignerId()).isEqualTo(1L);
        assertThat(savedLog.getSignedIp()).isEqualTo("192.168.1.2");
        assertThat(savedLog.getSignedDevice()).isEqualTo("Safari/iOS");

        // S3 업로드 확인 (S3 키는 anyString()으로 검증)
        verify(s3Service).uploadPdf(anyString(), eq(v3PdfBytes));
    }

    @Test
    @DisplayName("근로자 서명 처리 실패 - 잘못된 계약 상태")
    void processEmployeeSignature_InvalidState() {
        // given
        Long contractId = 100L;
        contract = Contract.builder()
                .employeeId(1L)
                .corporationId(1L)
                .empType(EmpType.PERMANENT)
                .contractState(ContractState.MANAGER_SIGNING_PENDING) // 잘못된 상태
                .employeeStartDate(LocalDate.of(2024, 1, 1))
                .build();
        setId(contract, contractId);

        given(contractRepository.findById(contractId)).willReturn(Optional.of(contract));
        given(contractDetailRepository.findByContractId(contractId)).willReturn(Optional.of(contractDetail));

        // when & then
        assertThatThrownBy(() -> contractSignatureService.processEmployeeSignature(
                contractId,
                "uploads/CONTRACT/100/EMPLOYEE.png",
                "hash",
                SignatureCoordinates.builder()
                        .x(450.0).y(200.0).width(150.0).height(50.0)
                        .viewWidth(800.0).viewHeight(1131.0)
                        .build(),
                "192.168.1.2",
                "Safari/iOS"
        ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ContractErrorCode.INVALID_CONTRACT_STATE);

        verify(s3Service, never()).downloadImage(any());
    }

    @Test
    @DisplayName("관리자 서명 처리 실패 - 해시 불일치")
    void processManagerSignature_HashMismatch() {
        // given
        Long contractId = 100L;
        String signatureS3Key = "uploads/CONTRACT/100/MANAGER.png";
        SignatureCoordinates coordinates = SignatureCoordinates.builder()
                .x(100.0)
                .y(200.0)
                .width(150.0)
                .height(50.0)
                .viewWidth(800.0)
                .viewHeight(1131.0)
                .build();

        contract = Contract.builder()
                .employeeId(1L)
                .corporationId(1L)
                .managerId(1L)
                .empType(EmpType.PERMANENT)
                .contractState(ContractState.MANAGER_SIGNING_PENDING)
                .employeeStartDate(LocalDate.of(2024, 1, 1))
                .build();
        setId(contract, contractId);

        byte[] signatureImageBytes = "signature image".getBytes();

        // 잘못된 해시 제공 (의도적으로 다른 해시)
        String wrongHash = "0000000000000000000000000000000000000000000000000000000000000000";

        given(contractRepository.findById(contractId)).willReturn(Optional.of(contract));
        given(contractDetailRepository.findByContractId(contractId)).willReturn(Optional.of(contractDetail));
        given(s3Service.downloadImage(signatureS3Key)).willReturn(signatureImageBytes);

        // when & then
        assertThatThrownBy(() -> contractSignatureService.processManagerSignature(
                contractId,
                signatureS3Key,
                wrongHash,
                coordinates,
                "192.168.1.1",
                "Chrome/Win10"
        ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ContractErrorCode.SIGNATURE_HASH_MISMATCH);

        // v1 PDF 다운로드가 이루어지지 않았는지 확인 (해시 검증 실패로 조기 종료)
        verify(s3Service, never()).downloadPdf(any());
        verify(pdfGenerationService, never()).stampSignatureOnPdf(any(), any(), any(), any(), any(), any());
        verify(signLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("근로자 서명 처리 실패 - 해시 불일치")
    void processEmployeeSignature_HashMismatch() {
        // given
        Long contractId = 100L;
        String signatureS3Key = "uploads/CONTRACT/100/EMPLOYEE.png";
        SignatureCoordinates coordinates = SignatureCoordinates.builder()
                .x(450.0)
                .y(200.0)
                .width(150.0)
                .height(50.0)
                .viewWidth(800.0)
                .viewHeight(1131.0)
                .build();

        contract = Contract.builder()
                .employeeId(1L)
                .corporationId(1L)
                .managerId(1L)
                .empType(EmpType.PERMANENT)
                .contractState(ContractState.EMPLOYEE_SIGNING_PENDING)
                .employeeStartDate(LocalDate.of(2024, 1, 1))
                .build();
        setId(contract, contractId);

        byte[] signatureImageBytes = "employee signature".getBytes();

        // 잘못된 해시 제공
        String wrongHash = "1111111111111111111111111111111111111111111111111111111111111111";

        given(contractRepository.findById(contractId)).willReturn(Optional.of(contract));
        given(contractDetailRepository.findByContractId(contractId)).willReturn(Optional.of(contractDetail));
        given(s3Service.downloadImage(signatureS3Key)).willReturn(signatureImageBytes);

        // when & then
        assertThatThrownBy(() -> contractSignatureService.processEmployeeSignature(
                contractId,
                signatureS3Key,
                wrongHash,
                coordinates,
                "192.168.1.2",
                "Safari/iOS"
        ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ContractErrorCode.SIGNATURE_HASH_MISMATCH);

        // v2 PDF 다운로드가 이루어지지 않았는지 확인
        verify(s3Service, never()).downloadPdf(any());
        verify(pdfGenerationService, never()).stampSignatureOnPdf(any(), any(), any(), any(), any(), any());
        verify(signLogRepository, never()).save(any());
    }

    /**
     * Reflection을 사용하여 ID 설정
     */
    private void setId(Contract contract, Long id) {
        try {
            java.lang.reflect.Field idField = Contract.class.getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(contract, id);
        } catch (Exception e) {
            throw new RuntimeException("ID 설정 실패", e);
        }
    }
}
