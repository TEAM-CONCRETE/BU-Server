package com.concrete.buildup.domain.document.service;

import com.concrete.buildup.domain.contract.entity.Contract;
import com.concrete.buildup.domain.contract.entity.ContractDetail;
import com.concrete.buildup.domain.contract.enums.ContractState;
import com.concrete.buildup.domain.contract.repository.ContractDetailRepository;
import com.concrete.buildup.domain.contract.repository.ContractRepository;
import com.concrete.buildup.domain.document.dto.DocumentUrlResponseDto;
import com.concrete.buildup.domain.payroll.entity.Payroll;
import com.concrete.buildup.domain.payroll.repository.PayrollRepository;
import com.concrete.buildup.domain.upload.service.S3Service;
import com.concrete.buildup.global.exception.BusinessException;
import com.concrete.buildup.global.exception.errorcode.CommonErrorCode;
import com.concrete.buildup.global.exception.errorcode.DocumentErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * 문서 조회 Service
 *
 * <p>근로계약서, 급여명세서 등의 PDF 문서에 대한 Signed URL을 발급합니다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentService {

    private final Optional<S3Service> s3Service;
    private final ContractRepository contractRepository;
    private final ContractDetailRepository contractDetailRepository;
    private final PayrollRepository payrollRepository;

    private static final int SIGNED_URL_EXPIRATION_MINUTES = 15;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 근로계약서 PDF Signed URL 발급
     *
     * <p>계약 상태에 따라 적절한 버전의 PDF를 조회합니다.</p>
     * <ul>
     *   <li>MANAGER_SIGNING_PENDING: v1_draft (초안)</li>
     *   <li>EMPLOYEE_SIGNING_PENDING: v2_manager_signed (담당자 서명 완료)</li>
     *   <li>FULLY_SIGNED: v3_final (양측 서명 완료)</li>
     * </ul>
     *
     * @param contractId 계약 ID
     * @return Signed URL 응답
     * @throws BusinessException 계약이 존재하지 않거나 PDF 파일이 없는 경우
     */
    public DocumentUrlResponseDto getContractPdfUrl(Long contractId) {
        log.info("근로계약서 PDF URL 발급 요청: contractId={}", contractId);

        // 계약 조회
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new BusinessException(DocumentErrorCode.CONTRACT_NOT_FOUND));

        // 계약 상세 조회
        ContractDetail contractDetail = contractDetailRepository.findByContractId(contractId)
                .orElseThrow(() -> new BusinessException(DocumentErrorCode.DOCUMENT_NOT_FOUND));

        // 상태에 따른 PDF 버전 결정
        String pdfVersion = getPdfVersionByState(contract.getContractState());
        if (pdfVersion == null) {
            log.warn("PDF가 아직 생성되지 않은 계약: contractId={}, state={}", contractId, contract.getContractState());
            throw new BusinessException(DocumentErrorCode.DOCUMENT_NOT_FOUND);
        }

        // S3 키 동적 생성
        String s3Key = buildContractS3Key(contractId, contract, contractDetail, pdfVersion);

        // S3 서비스 확인
        S3Service service = s3Service.orElseThrow(() ->
                new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, "S3 서비스가 비활성화되어 있습니다."));

        // S3에 파일 존재 여부 확인
        if (!service.doesObjectExist(s3Key)) {
            log.warn("근로계약서 PDF 파일이 존재하지 않음: s3Key={}", s3Key);
            throw new BusinessException(DocumentErrorCode.DOCUMENT_NOT_FOUND);
        }

        // Signed URL 발급
        String signedUrl = service.generatePresignedGetUrl(s3Key);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(SIGNED_URL_EXPIRATION_MINUTES);

        log.info("근로계약서 PDF URL 발급 완료: contractId={}, version={}, expiresAt={}", contractId, pdfVersion, expiresAt);

        return DocumentUrlResponseDto.of(signedUrl, expiresAt);
    }

    /**
     * 급여명세서 PDF Signed URL 발급
     *
     * <p>급여명세서의 s3Key를 사용하여 Signed URL을 발급합니다.</p>
     *
     * @param payrollId 급여 ID
     * @return Signed URL 응답
     * @throws BusinessException 급여명세서가 존재하지 않거나 PDF 파일이 없는 경우
     */
    public DocumentUrlResponseDto getPayslipPdfUrl(Long payrollId) {
        log.info("급여명세서 PDF URL 발급 요청: payrollId={}", payrollId);

        // 급여 조회
        Payroll payroll = payrollRepository.findById(payrollId)
                .orElseThrow(() -> new BusinessException(DocumentErrorCode.PAYROLL_NOT_FOUND));

        // S3 키 확인
        String s3Key = payroll.getS3Key();
        if (s3Key == null || s3Key.isBlank()) {
            log.warn("급여명세서 S3 키가 저장되지 않음: payrollId={}", payrollId);
            throw new BusinessException(DocumentErrorCode.DOCUMENT_NOT_FOUND);
        }

        // S3 서비스 확인
        S3Service service = s3Service.orElseThrow(() ->
                new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, "S3 서비스가 비활성화되어 있습니다."));

        // S3에 파일 존재 여부 확인
        if (!service.doesObjectExist(s3Key)) {
            log.warn("급여명세서 PDF 파일이 존재하지 않음: s3Key={}", s3Key);
            throw new BusinessException(DocumentErrorCode.DOCUMENT_NOT_FOUND);
        }

        // Signed URL 발급
        String signedUrl = service.generatePresignedGetUrl(s3Key);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(SIGNED_URL_EXPIRATION_MINUTES);

        log.info("급여명세서 PDF URL 발급 완료: payrollId={}, expiresAt={}", payrollId, expiresAt);

        return DocumentUrlResponseDto.of(signedUrl, expiresAt);
    }

    /**
     * 계약 상태에 따른 PDF 버전 반환
     *
     * @param state 계약 상태
     * @return PDF 버전 (null이면 PDF 없음)
     */
    private String getPdfVersionByState(ContractState state) {
        return switch (state) {
            case MANAGER_SIGNING_PENDING -> "v1_draft";
            case EMPLOYEE_SIGNING_PENDING -> "v2_manager_signed";
            case FULLY_SIGNED -> "v3_final";
            default -> null;
        };
    }

    /**
     * 계약서 S3 키 생성
     *
     * @param contractId 계약 ID
     * @param contract 계약 엔티티
     * @param contractDetail 계약 상세 엔티티
     * @param version PDF 버전
     * @return S3 키
     */
    private String buildContractS3Key(Long contractId, Contract contract, ContractDetail contractDetail, String version) {
        String employeeName = contractDetail.getEmpName();
        String empType = contract.getEmpType().name();
        String dateStr = contract.getWrittenAt().format(DATE_FORMATTER);

        String fileName = String.format("%s_%s_%s_%s.pdf", employeeName, empType, dateStr, version);
        return String.format("contracts/%d/%s", contractId, fileName);
    }
}