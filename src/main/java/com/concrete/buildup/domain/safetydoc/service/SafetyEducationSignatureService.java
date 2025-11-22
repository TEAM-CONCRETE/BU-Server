package com.concrete.buildup.domain.safetydoc.service;

import com.concrete.buildup.domain.auth.entity.Manager;
import com.concrete.buildup.domain.auth.entity.User;
import com.concrete.buildup.domain.auth.repository.ManagerRepository;
import com.concrete.buildup.domain.auth.repository.UserRepository;
import com.concrete.buildup.domain.contract.enums.SignerRole;
import com.concrete.buildup.domain.contract.enums.VerificationStatus;
import com.concrete.buildup.domain.safetydoc.dto.SafetyEducationSignatureRequest;
import com.concrete.buildup.domain.safetydoc.dto.SafetyEducationSignatureResponse;
import com.concrete.buildup.domain.safetydoc.entity.SafetyEducationAttendee;
import com.concrete.buildup.domain.safetydoc.entity.SafetyEducationLog;
import com.concrete.buildup.domain.safetydoc.entity.SafetyEducationSignLog;
import com.concrete.buildup.domain.safetydoc.enums.SafetyEducationStatus;
import com.concrete.buildup.domain.safetydoc.repository.SafetyEducationAttendeeRepository;
import com.concrete.buildup.domain.safetydoc.repository.SafetyEducationLogRepository;
import com.concrete.buildup.domain.safetydoc.repository.SafetyEducationSignLogRepository;
import com.concrete.buildup.domain.site.entity.Site;
import com.concrete.buildup.domain.site.repository.SiteRepository;
import com.concrete.buildup.domain.upload.service.S3Service;
import com.concrete.buildup.global.exception.BusinessException;
import com.concrete.buildup.global.exception.errorcode.AuthErrorCode;
import com.concrete.buildup.global.exception.errorcode.SafetyDocErrorCode;
import com.concrete.buildup.domain.contract.service.PdfGenerationService;
import com.concrete.buildup.global.util.CoordinateConverter;
import com.concrete.buildup.global.util.SignatureVerificationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class SafetyEducationSignatureService {

    private static final double PDF_PAGE_WIDTH = 595.0;  // A4 width in pt
    private static final double PDF_PAGE_HEIGHT = 842.0; // A4 height in pt

    private final SafetyEducationLogRepository safetyEducationLogRepository;
    private final SafetyEducationAttendeeRepository attendeeRepository;
    private final SafetyEducationSignLogRepository signLogRepository;
    private final SiteRepository siteRepository;
    private final UserRepository userRepository;
    private final ManagerRepository managerRepository;
    private final Optional<S3Service> s3Service;
    private final PdfGenerationService pdfGenerationService;

    public SafetyEducationSignatureResponse processManagerSignature(
            Long siteId,
            Long logId,
            SafetyEducationSignatureRequest request,
            String signedIp,
            String signedDevice,
            String currentUserId
    ) {
        // 1. Site 존재 여부 검증
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new BusinessException(SafetyDocErrorCode.SITE_NOT_FOUND));

        // 2. 안전교육일지 조회 및 상태 검증
        SafetyEducationLog log = safetyEducationLogRepository.findById(logId)
                .orElseThrow(() -> new BusinessException(SafetyDocErrorCode.SAFETY_EDUCATION_LOG_NOT_FOUND));

        if (log.getStatus() != SafetyEducationStatus.MANAGER_SIGNING_PENDING) {
            throw new BusinessException(SafetyDocErrorCode.INVALID_SAFETY_EDUCATION_STATUS);
        }

        // 3. 권한 검증
        User user = userRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));

        Manager manager = managerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));

        if (site.getManager() == null || !site.getManager().getId().equals(manager.getId())) {
            throw new BusinessException(SafetyDocErrorCode.MANAGER_NOT_AUTHORIZED);
        }

        // 4. 이미 서명했는지 확인
        if (signLogRepository.existsBySafetyEducationLogIdAndSignerRoleAndIsDeletedFalse(logId, SignerRole.MANAGER)) {
            throw new BusinessException(SafetyDocErrorCode.ALREADY_SIGNED);
        }

        S3Service service = s3Service.orElseThrow(() ->
                new BusinessException(SafetyDocErrorCode.S3_UPLOAD_FAILED, "S3 서비스가 비활성화되어 있습니다."));

        // 5. 서명 이미지 해시 검증
        byte[] signatureImageBytes = service.downloadImage(request.getSignatureS3Key());
        String serverHash = SignatureVerificationUtil.calculateSHA256(
                new ByteArrayInputStream(signatureImageBytes)
        );

        if (!SignatureVerificationUtil.verifySignatureHash(request.getClientHash(), serverHash)) {
            throw new BusinessException(SafetyDocErrorCode.SIGNATURE_HASH_MISMATCH);
        }

        // 6. 기존 PDF 다운로드
        String existingPdfUrl = log.getPdfUrl();
        String existingS3Key = extractS3KeyFromUrl(existingPdfUrl);
        byte[] existingPdfBytes = service.downloadPdf(existingS3Key);

        // 7. 좌표 변환
        SafetyEducationSignatureRequest.SignatureCoordinatesDto coords = request.getCoordinates();
        CoordinateConverter.PdfCoordinates pdfCoords = CoordinateConverter.convertToPdfCoordinates(
                coords.getX(), coords.getY(),
                coords.getViewWidth(), coords.getViewHeight(),
                PDF_PAGE_WIDTH, PDF_PAGE_HEIGHT
        );

        // 8. 서명 크기 스케일링
        BigDecimal scaledWidth = BigDecimal.valueOf(coords.getWidth() * (PDF_PAGE_WIDTH / coords.getViewWidth()));
        BigDecimal scaledHeight = BigDecimal.valueOf(coords.getHeight() * (PDF_PAGE_HEIGHT / coords.getViewHeight()));

        // 9. 서명 스탬핑
        byte[] signedPdfBytes = pdfGenerationService.stampSignatureOnPdf(
                existingPdfBytes,
                signatureImageBytes,
                pdfCoords.getX(),
                pdfCoords.getY().subtract(scaledHeight),
                scaledWidth,
                scaledHeight
        );

        // 10. 새 PDF S3 업로드
        LocalDate today = LocalDate.now();
        String dateStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String newS3Key = String.format("safety-docs/%d/%s/SE-%s-%d-manager-signed.pdf",
                siteId, dateStr, dateStr, logId);

        service.uploadPdf(newS3Key, signedPdfBytes);
        String newPdfUrl = service.getPdfUrl(newS3Key);

        // 11. 서명 로그 저장
        SafetyEducationSignLog signLog = SafetyEducationSignLog.builder()
                .safetyEducationLog(log)
                .signerRole(SignerRole.MANAGER)
                .signerId(manager.getId())
                .signerName(manager.getManagerName())
                .signatureImageUrl(request.getSignatureS3Key())
                .signatureHash(serverHash)
                .signatureX(pdfCoords.getX())
                .signatureY(pdfCoords.getY())
                .signatureWidth(BigDecimal.valueOf(coords.getWidth()))
                .signatureHeight(BigDecimal.valueOf(coords.getHeight()))
                .signedIp(signedIp)
                .signedDevice(signedDevice)
                .signedAt(LocalDateTime.now())
                .verificationStatus(VerificationStatus.VERIFIED)
                .verifiedAt(LocalDateTime.now())
                .build();
        signLogRepository.save(signLog);

        // 12. 상태 전환 및 PDF URL 업데이트
        log.signByManager();
        log.updatePdf(newPdfUrl);
        safetyEducationLogRepository.save(log);

        return SafetyEducationSignatureResponse.builder()
                .safetyEducationLogId(logId)
                .status(log.getStatus())
                .pdfUrl(newPdfUrl)
                .signedAt(signLog.getSignedAt())
                .build();
    }

    public SafetyEducationSignatureResponse processAttendeeSignature(
            Long siteId,
            Long logId,
            Long employeeId,
            SafetyEducationSignatureRequest request,
            String signedIp,
            String signedDevice
    ) {
        // 1. 안전교육일지 조회 및 상태 검증
        SafetyEducationLog log = safetyEducationLogRepository.findByIdWithAttendees(logId)
                .orElseThrow(() -> new BusinessException(SafetyDocErrorCode.SAFETY_EDUCATION_LOG_NOT_FOUND));

        if (log.getStatus() != SafetyEducationStatus.MANAGER_SIGNED) {
            throw new BusinessException(SafetyDocErrorCode.INVALID_SAFETY_EDUCATION_STATUS);
        }

        // 2. 참석자 조회 및 검증
        SafetyEducationAttendee attendee = attendeeRepository
                .findBySafetyEducationLogIdAndEmployeeIdAndIsDeletedFalse(logId, employeeId)
                .orElseThrow(() -> new BusinessException(SafetyDocErrorCode.EMPLOYEE_NOT_AUTHORIZED));

        // 3. 이미 서명했는지 확인
        if (attendee.getIsSigned()) {
            throw new BusinessException(SafetyDocErrorCode.ALREADY_SIGNED);
        }

        S3Service service = s3Service.orElseThrow(() ->
                new BusinessException(SafetyDocErrorCode.S3_UPLOAD_FAILED, "S3 서비스가 비활성화되어 있습니다."));

        // 4. 서명 이미지 해시 검증
        byte[] signatureImageBytes = service.downloadImage(request.getSignatureS3Key());
        String serverHash = SignatureVerificationUtil.calculateSHA256(
                new ByteArrayInputStream(signatureImageBytes)
        );

        if (!SignatureVerificationUtil.verifySignatureHash(request.getClientHash(), serverHash)) {
            throw new BusinessException(SafetyDocErrorCode.SIGNATURE_HASH_MISMATCH);
        }

        // 5. 참석자 서명 완료 처리 (DB에 서명 이미지 URL 저장)
        attendee.sign(request.getSignatureS3Key());
        attendeeRepository.save(attendee);

        // 6. 서명 로그 저장
        SafetyEducationSignLog signLog = SafetyEducationSignLog.builder()
                .safetyEducationLog(log)
                .signerRole(SignerRole.EMPLOYEE)
                .signerId(employeeId)
                .signerName(attendee.getEmployee().getEmpName())
                .signatureImageUrl(request.getSignatureS3Key())
                .signatureHash(serverHash)
                .signedIp(signedIp)
                .signedDevice(signedDevice)
                .signedAt(LocalDateTime.now())
                .verificationStatus(VerificationStatus.VERIFIED)
                .verifiedAt(LocalDateTime.now())
                .build();
        signLogRepository.save(signLog);

        // 7. 모든 참석자 서명 완료 시 최종 PDF 생성
        String pdfUrl = log.getPdfUrl();
        String pdfHash = null;

        // 다시 조회하여 모든 참석자 서명 상태 확인
        List<SafetyEducationAttendee> allAttendees = attendeeRepository
                .findBySafetyEducationLogIdAndIsDeletedFalse(logId);
        boolean allSigned = allAttendees.stream().allMatch(SafetyEducationAttendee::getIsSigned);

        if (allSigned) {
            // 최종 PDF 생성: 관리자 서명된 PDF에 모든 참석자 서명 스탬핑
            pdfUrl = generateFinalPdfWithAllSignatures(log, allAttendees, siteId, service);

            // 최종 PDF 해시 계산
            String finalS3Key = extractS3KeyFromUrl(pdfUrl);
            byte[] finalPdfBytes = service.downloadPdf(finalS3Key);
            pdfHash = SignatureVerificationUtil.calculateSHA256(new ByteArrayInputStream(finalPdfBytes));

            log.complete();
            log.updateFinalPdf(pdfUrl, pdfHash);
        }

        safetyEducationLogRepository.save(log);

        return SafetyEducationSignatureResponse.builder()
                .safetyEducationLogId(logId)
                .status(log.getStatus())
                .pdfUrl(pdfUrl)
                .pdfHash(pdfHash)
                .signedAt(signLog.getSignedAt())
                .build();
    }

    /**
     * 모든 참석자 서명이 완료되었을 때 최종 PDF 생성
     * 관리자 서명된 PDF에 모든 참석자의 서명을 스탬핑
     */
    private String generateFinalPdfWithAllSignatures(
            SafetyEducationLog log,
            List<SafetyEducationAttendee> attendees,
            Long siteId,
            S3Service service
    ) {
        // 1. 관리자 서명된 PDF 다운로드
        String existingPdfUrl = log.getPdfUrl();
        String existingS3Key = extractS3KeyFromUrl(existingPdfUrl);
        byte[] currentPdfBytes = service.downloadPdf(existingS3Key);

        // 2. 각 참석자의 서명을 PDF에 스탬핑
        // 참석자 테이블의 서명란 위치 계산 (PDF 좌표 기준)
        // PDF 템플릿 기준: 참석자 테이블은 약 Y=400pt 부근에서 시작
        // 각 행 높이 약 25pt, 서명란은 우측에 위치
        final double TABLE_START_Y = 380.0;  // 테이블 시작 Y 위치 (상단 기준)
        final double ROW_HEIGHT = 25.0;       // 각 행의 높이
        final double SIGNATURE_X = 480.0;     // 서명란 X 위치 (좌측 기준)
        final double SIGNATURE_WIDTH = 60.0;  // 서명 이미지 너비
        final double SIGNATURE_HEIGHT = 20.0; // 서명 이미지 높이

        for (int i = 0; i < attendees.size(); i++) {
            SafetyEducationAttendee attendee = attendees.get(i);
            if (attendee.getIsSigned() && attendee.getSignatureImageUrl() != null) {
                // 서명 이미지 다운로드
                byte[] signatureImageBytes = service.downloadImage(attendee.getSignatureImageUrl());

                // 해당 참석자 행의 서명란 Y 좌표 계산
                // PDF는 좌하단이 원점이므로, 페이지 높이(842)에서 빼야 함
                double rowY = PDF_PAGE_HEIGHT - (TABLE_START_Y + (i + 1) * ROW_HEIGHT);

                // PDF에 서명 스탬핑
                currentPdfBytes = pdfGenerationService.stampSignatureOnPdf(
                        currentPdfBytes,
                        signatureImageBytes,
                        BigDecimal.valueOf(SIGNATURE_X),
                        BigDecimal.valueOf(rowY),
                        BigDecimal.valueOf(SIGNATURE_WIDTH),
                        BigDecimal.valueOf(SIGNATURE_HEIGHT)
                );
            }
        }

        // 3. 최종 PDF S3 업로드
        LocalDate today = LocalDate.now();
        String dateStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String finalS3Key = String.format("safety-docs/%d/%s/SE-%s-%d-final.pdf",
                siteId, dateStr, dateStr, log.getId());

        service.uploadPdf(finalS3Key, currentPdfBytes);
        return service.getPdfUrl(finalS3Key);
    }

    public String generateInitialPdf(Long siteId, Long logId, String currentUserId) {
        // Site 존재 여부 검증
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new BusinessException(SafetyDocErrorCode.SITE_NOT_FOUND));

        // 안전교육일지 조회
        SafetyEducationLog log = safetyEducationLogRepository.findByIdWithAttendees(logId)
                .orElseThrow(() -> new BusinessException(SafetyDocErrorCode.SAFETY_EDUCATION_LOG_NOT_FOUND));

        // 권한 검증
        User user = userRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));

        Manager manager = managerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));

        if (site.getManager() == null || !site.getManager().getId().equals(manager.getId())) {
            throw new BusinessException(SafetyDocErrorCode.MANAGER_NOT_AUTHORIZED);
        }

        // 상태 변경 및 저장
        log.transitionToManagerSigningPending();
        safetyEducationLogRepository.save(log);

        return log.getPdfUrl();
    }

    private String extractS3KeyFromUrl(String url) {
        // S3 URL에서 키 추출
        // 예: https://bucket.s3.region.amazonaws.com/safety-docs/1/2024-01-15/SE-2024-01-15-1.pdf
        // -> safety-docs/1/2024-01-15/SE-2024-01-15-1.pdf
        if (url == null) {
            throw new BusinessException(SafetyDocErrorCode.PDF_GENERATION_FAILED, "PDF URL이 없습니다.");
        }
        int index = url.indexOf("safety-docs/");
        if (index == -1) {
            throw new BusinessException(SafetyDocErrorCode.PDF_GENERATION_FAILED, "잘못된 PDF URL 형식입니다.");
        }
        return url.substring(index);
    }
}
