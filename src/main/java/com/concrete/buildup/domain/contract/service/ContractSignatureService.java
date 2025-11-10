package com.concrete.buildup.domain.contract.service;

import com.concrete.buildup.domain.contract.dto.SignatureCompleteResponse;
import com.concrete.buildup.domain.contract.dto.SignatureCoordinates;
import com.concrete.buildup.domain.contract.entity.Contract;
import com.concrete.buildup.domain.contract.entity.ContractDetail;
import com.concrete.buildup.domain.contract.entity.ContractSignLog;
import com.concrete.buildup.domain.contract.enums.ContractState;
import com.concrete.buildup.domain.contract.enums.SignerRole;
import com.concrete.buildup.domain.contract.enums.VerificationStatus;
import com.concrete.buildup.domain.contract.repository.ContractDetailRepository;
import com.concrete.buildup.domain.contract.repository.ContractRepository;
import com.concrete.buildup.domain.contract.repository.ContractSignLogRepository;
import com.concrete.buildup.domain.upload.service.S3Service;
import com.concrete.buildup.global.exception.BusinessException;
import com.concrete.buildup.global.exception.errorcode.ContractErrorCode;
import com.concrete.buildup.global.util.CoordinateConverter;
import com.concrete.buildup.global.util.SignatureVerificationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 계약 서명 처리 서비스
 *
 * <p>초안 PDF 생성, 서명 이미지 검증, PDF에 서명 스탬핑을 처리합니다.</p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>초안 PDF 생성 (v1)</li>
 *   <li>관리자 서명 처리 (v2)</li>
 *   <li>근로자 서명 처리 (v3, 최종)</li>
 *   <li>서명 이미지 해시 검증</li>
 *   <li>서명 이력 기록</li>
 * </ul>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ContractSignatureService {

    private final ContractRepository contractRepository;
    private final ContractDetailRepository contractDetailRepository;
    private final ContractSignLogRepository signLogRepository;
    private final S3Service s3Service;
    private final PdfGenerationService pdfGenerationService;

    /**
     * 초안 PDF 생성 및 업로드 (v1)
     *
     * <p>계약 생성 시 자동으로 호출되며, 저장된 Contract와 ContractDetail 데이터를 기반으로 PDF를 생성합니다.</p>
     *
     * @param contractId 계약 ID
     * @return S3 PDF URL
     * @throws BusinessException CONTRACT_NOT_FOUND, CONTRACT_DETAIL_NOT_FOUND
     */
    public String generateInitialPdf(Long contractId) {
        log.info("초안 PDF 생성 시작: contractId={}", contractId);

        // 1. Contract + ContractDetail 조회
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new BusinessException(ContractErrorCode.CONTRACT_NOT_FOUND));

        ContractDetail contractDetail = contractDetailRepository.findByContractId(contractId)
                .orElseThrow(() -> new BusinessException(ContractErrorCode.CONTRACT_DETAIL_NOT_FOUND));

        // 2. PDF 생성 (Contract와 ContractDetail의 모든 데이터 반영)
        byte[] pdfBytes = pdfGenerationService.generateContractPdf(contract, contractDetail);

        // 3. S3에 v1.pdf 업로드
        String s3Key = String.format("contracts/%d/v1.pdf", contractId);
        s3Service.uploadPdf(s3Key, pdfBytes);
        String pdfUrl = s3Service.getPdfUrl(s3Key);

        // 4. Contract 상태 → MANAGER_SIGNING_PENDING
        contract.transitionToManagerSigningPending();
        contractRepository.save(contract);

        log.info("초안 PDF 생성 완료: contractId={}, pdfUrl={}", contractId, pdfUrl);

        return pdfUrl;
    }

    /**
     * 관리자 서명 완료 처리 (v2 생성)
     *
     * <p>관리자 서명 이미지를 검증하고, v1 PDF에 서명을 스탬핑하여 v2 PDF를 생성합니다.</p>
     *
     * @param contractId 계약 ID
     * @param signatureS3Key 서명 이미지 S3 키
     * @param clientHash 클라이언트에서 계산한 SHA-256 해시
     * @param coordinates 서명 좌표 정보
     * @param signedIp 서명 IP 주소
     * @param signedDevice 서명 디바이스 정보
     * @return 서명 완료 응답
     * @throws BusinessException CONTRACT_NOT_FOUND, INVALID_CONTRACT_STATE, SIGNATURE_HASH_MISMATCH
     */
    public SignatureCompleteResponse processManagerSignature(
            Long contractId,
            String signatureS3Key,
            String clientHash,
            SignatureCoordinates coordinates,
            String signedIp,
            String signedDevice
    ) {
        log.info("관리자 서명 처리 시작: contractId={}", contractId);

        // 1. Contract 조회 및 상태 검증
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new BusinessException(ContractErrorCode.CONTRACT_NOT_FOUND));

        if (contract.getContractState() != ContractState.MANAGER_SIGNING_PENDING) {
            throw new BusinessException(ContractErrorCode.INVALID_CONTRACT_STATE);
        }

        // 2. S3에서 서명 이미지 다운로드
        byte[] signatureImageBytes = s3Service.downloadImage(signatureS3Key);

        // 3. 서버에서 해시 재계산 및 검증
        String serverHash = SignatureVerificationUtil.calculateSHA256(
                new ByteArrayInputStream(signatureImageBytes)
        );

        if (!SignatureVerificationUtil.verifySignatureHash(clientHash, serverHash)) {
            throw new BusinessException(ContractErrorCode.SIGNATURE_HASH_MISMATCH);
        }

        // 4. S3에서 v1 PDF 다운로드
        String v1S3Key = String.format("contracts/%d/v1.pdf", contractId);
        byte[] v1PdfBytes = s3Service.downloadPdf(v1S3Key);

        // 5. 좌표 변환 (뷰포트 → PDF)
        CoordinateConverter.PdfCoordinates pdfCoords = CoordinateConverter.convertToPdfCoordinates(
                coordinates.getX(), coordinates.getY(),
                coordinates.getViewWidth(), coordinates.getViewHeight(),
                595.0, 842.0 // A4 size
        );

        // 6. PDF에 서명 이미지 스탬핑 → v2 생성
        byte[] v2PdfBytes = pdfGenerationService.stampSignatureOnPdf(
                v1PdfBytes,
                signatureImageBytes,
                pdfCoords.getX(), pdfCoords.getY(),
                BigDecimal.valueOf(coordinates.getWidth() * (595.0 / coordinates.getViewWidth())),
                BigDecimal.valueOf(coordinates.getHeight() * (842.0 / coordinates.getViewHeight()))
        );

        // 7. v2 PDF를 S3에 업로드
        String v2S3Key = String.format("contracts/%d/v2.pdf", contractId);
        s3Service.uploadPdf(v2S3Key, v2PdfBytes);
        String v2PdfUrl = s3Service.getPdfUrl(v2S3Key);

        // 8. ContractSignLog 저장
        ContractSignLog signLog = ContractSignLog.builder()
                .contract(contract)
                .signerRole(SignerRole.MANAGER)
                .signerId(contract.getManagerId())
                .signerName("관리자") // TODO: Manager 엔티티에서 조회
                .signatureImageUrl(signatureS3Key)
                .signatureHash(serverHash)
                .signatureX(pdfCoords.getX())
                .signatureY(pdfCoords.getY())
                .signatureWidth(BigDecimal.valueOf(coordinates.getWidth()))
                .signatureHeight(BigDecimal.valueOf(coordinates.getHeight()))
                .signedIp(signedIp)
                .signedDevice(signedDevice)
                .signedAt(LocalDateTime.now())
                .verificationStatus(VerificationStatus.VERIFIED)
                .verifiedAt(LocalDateTime.now())
                .build();
        signLogRepository.save(signLog);

        // 9. Contract 상태 → EMPLOYEE_SIGNING_PENDING
        contract.transitionToEmployeeSigningPending();
        contract.signByAdmin();
        contractRepository.save(contract);

        log.info("관리자 서명 처리 완료: contractId={}, v2PdfUrl={}", contractId, v2PdfUrl);

        return SignatureCompleteResponse.builder()
                .contractId(contractId)
                .contractState(contract.getContractState())
                .pdfUrl(v2PdfUrl)
                .pdfHash(null) // v2는 최종이 아니므로 해시 저장 안 함
                .signedAt(signLog.getSignedAt())
                .build();
    }

    /**
     * 근로자 서명 완료 처리 (v3 생성 및 최종 저장)
     *
     * <p>근로자 서명 이미지를 검증하고, v2 PDF에 서명을 스탬핑하여 최종 v3 PDF를 생성합니다.</p>
     * <p>v3 PDF의 해시값을 계산하여 Contract에 저장하고, 계약 상태를 FULLY_SIGNED로 변경합니다.</p>
     *
     * @param contractId 계약 ID
     * @param signatureS3Key 서명 이미지 S3 키
     * @param clientHash 클라이언트에서 계산한 SHA-256 해시
     * @param coordinates 서명 좌표 정보
     * @param signedIp 서명 IP 주소
     * @param signedDevice 서명 디바이스 정보
     * @return 서명 완료 응답 (최종 PDF URL 및 해시 포함)
     * @throws BusinessException CONTRACT_NOT_FOUND, INVALID_CONTRACT_STATE, SIGNATURE_HASH_MISMATCH
     */
    public SignatureCompleteResponse processEmployeeSignature(
            Long contractId,
            String signatureS3Key,
            String clientHash,
            SignatureCoordinates coordinates,
            String signedIp,
            String signedDevice
    ) {
        log.info("근로자 서명 처리 시작: contractId={}", contractId);

        // 1. Contract 조회 및 상태 검증
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new BusinessException(ContractErrorCode.CONTRACT_NOT_FOUND));

        if (contract.getContractState() != ContractState.EMPLOYEE_SIGNING_PENDING) {
            throw new BusinessException(ContractErrorCode.INVALID_CONTRACT_STATE);
        }

        // 2. S3에서 서명 이미지 다운로드
        byte[] signatureImageBytes = s3Service.downloadImage(signatureS3Key);

        // 3. 서버에서 해시 재계산 및 검증
        String serverHash = SignatureVerificationUtil.calculateSHA256(
                new ByteArrayInputStream(signatureImageBytes)
        );

        if (!SignatureVerificationUtil.verifySignatureHash(clientHash, serverHash)) {
            throw new BusinessException(ContractErrorCode.SIGNATURE_HASH_MISMATCH);
        }

        // 4. S3에서 v2 PDF 다운로드
        String v2S3Key = String.format("contracts/%d/v2.pdf", contractId);
        byte[] v2PdfBytes = s3Service.downloadPdf(v2S3Key);

        // 5. 좌표 변환 (뷰포트 → PDF)
        CoordinateConverter.PdfCoordinates pdfCoords = CoordinateConverter.convertToPdfCoordinates(
                coordinates.getX(), coordinates.getY(),
                coordinates.getViewWidth(), coordinates.getViewHeight(),
                595.0, 842.0
        );

        // 6. PDF에 서명 이미지 스탬핑 → v3 생성
        byte[] v3PdfBytes = pdfGenerationService.stampSignatureOnPdf(
                v2PdfBytes,
                signatureImageBytes,
                pdfCoords.getX(), pdfCoords.getY(),
                BigDecimal.valueOf(coordinates.getWidth() * (595.0 / coordinates.getViewWidth())),
                BigDecimal.valueOf(coordinates.getHeight() * (842.0 / coordinates.getViewHeight()))
        );

        // 7. v3 PDF의 SHA-256 해시 계산
        String v3PdfHash = SignatureVerificationUtil.calculateSHA256(
                new ByteArrayInputStream(v3PdfBytes)
        );

        // 8. v3 PDF를 S3에 업로드
        String v3S3Key = String.format("contracts/%d/v3.pdf", contractId);
        s3Service.uploadPdf(v3S3Key, v3PdfBytes);
        String v3PdfUrl = s3Service.getPdfUrl(v3S3Key);

        // 9. Contract.finalPdfUrl, finalPdfHash 업데이트 (최종 저장)
        contract.updateFinalPdf(v3PdfUrl, v3PdfHash);

        // 10. ContractSignLog 저장
        ContractSignLog signLog = ContractSignLog.builder()
                .contract(contract)
                .signerRole(SignerRole.EMPLOYEE)
                .signerId(contract.getEmployeeId())
                .signerName("근로자") // TODO: Employee 엔티티에서 조회
                .signatureImageUrl(signatureS3Key)
                .signatureHash(serverHash)
                .signatureX(pdfCoords.getX())
                .signatureY(pdfCoords.getY())
                .signatureWidth(BigDecimal.valueOf(coordinates.getWidth()))
                .signatureHeight(BigDecimal.valueOf(coordinates.getHeight()))
                .signedIp(signedIp)
                .signedDevice(signedDevice)
                .signedAt(LocalDateTime.now())
                .verificationStatus(VerificationStatus.VERIFIED)
                .verifiedAt(LocalDateTime.now())
                .build();
        signLogRepository.save(signLog);

        // 11. Contract 상태 → FULLY_SIGNED
        contract.transitionToFullySigned();
        contractRepository.save(contract);

        log.info("근로자 서명 처리 완료: contractId={}, finalPdfUrl={}", contractId, v3PdfUrl);

        return SignatureCompleteResponse.builder()
                .contractId(contractId)
                .contractState(contract.getContractState())
                .pdfUrl(v3PdfUrl)
                .pdfHash(v3PdfHash)
                .signedAt(signLog.getSignedAt())
                .build();
    }
}
