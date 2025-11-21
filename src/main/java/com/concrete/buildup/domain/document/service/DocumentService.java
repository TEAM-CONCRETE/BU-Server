package com.concrete.buildup.domain.document.service;

import com.concrete.buildup.domain.contract.repository.ContractRepository;
import com.concrete.buildup.domain.document.dto.DocumentUrlResponseDto;
import com.concrete.buildup.domain.payroll.repository.PayrollRepository;
import com.concrete.buildup.domain.upload.service.S3Service;
import com.concrete.buildup.global.exception.BusinessException;
import com.concrete.buildup.global.exception.errorcode.DocumentErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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

    private final S3Service s3Service;
    private final ContractRepository contractRepository;
    private final PayrollRepository payrollRepository;

    /**
     * Signed URL 만료 시간 (15분)
     */
    private static final int SIGNED_URL_EXPIRATION_MINUTES = 15;

    /**
     * 근로계약서 PDF Signed URL 발급
     *
     * @param contractId 계약 ID
     * @return Signed URL 응답
     * @throws BusinessException 계약이 존재하지 않거나 PDF 파일이 없는 경우
     */
    public DocumentUrlResponseDto getContractPdfUrl(Long contractId) {
        log.info("근로계약서 PDF URL 발급 요청: contractId={}", contractId);

        // 계약 존재 여부 확인
        if (!contractRepository.existsById(contractId)) {
            throw new BusinessException(DocumentErrorCode.CONTRACT_NOT_FOUND);
        }

        // S3 경로 생성
        String s3Key = String.format("contracts/%d/signed_final.pdf", contractId);

        // S3에 파일 존재 여부 확인
        if (!s3Service.doesObjectExist(s3Key)) {
            log.warn("근로계약서 PDF 파일이 존재하지 않음: s3Key={}", s3Key);
            throw new BusinessException(DocumentErrorCode.DOCUMENT_NOT_FOUND);
        }

        // Signed URL 발급
        String signedUrl = s3Service.generatePresignedGetUrl(s3Key);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(SIGNED_URL_EXPIRATION_MINUTES);

        log.info("근로계약서 PDF URL 발급 완료: contractId={}, expiresAt={}", contractId, expiresAt);

        return DocumentUrlResponseDto.of(signedUrl, expiresAt);
    }

    /**
     * 급여명세서 PDF Signed URL 발급
     *
     * @param payrollId 급여 ID
     * @return Signed URL 응답
     * @throws BusinessException 급여명세서가 존재하지 않거나 PDF 파일이 없는 경우
     */
    public DocumentUrlResponseDto getPayslipPdfUrl(Long payrollId) {
        log.info("급여명세서 PDF URL 발급 요청: payrollId={}", payrollId);

        // 급여 존재 여부 확인
        if (!payrollRepository.existsById(payrollId)) {
            throw new BusinessException(DocumentErrorCode.PAYROLL_NOT_FOUND);
        }

        // S3 경로 생성
        String s3Key = String.format("payslips/%d/payslip.pdf", payrollId);

        // S3에 파일 존재 여부 확인
        if (!s3Service.doesObjectExist(s3Key)) {
            log.warn("급여명세서 PDF 파일이 존재하지 않음: s3Key={}", s3Key);
            throw new BusinessException(DocumentErrorCode.DOCUMENT_NOT_FOUND);
        }

        // Signed URL 발급
        String signedUrl = s3Service.generatePresignedGetUrl(s3Key);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(SIGNED_URL_EXPIRATION_MINUTES);

        log.info("급여명세서 PDF URL 발급 완료: payrollId={}, expiresAt={}", payrollId, expiresAt);

        return DocumentUrlResponseDto.of(signedUrl, expiresAt);
    }
}