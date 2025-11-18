package com.concrete.buildup.domain.workreport.service;

import com.concrete.buildup.domain.auth.entity.Manager;
import com.concrete.buildup.domain.auth.repository.ManagerRepository;
import com.concrete.buildup.domain.site.entity.Site;
import com.concrete.buildup.domain.site.repository.SiteRepository;
import com.concrete.buildup.domain.upload.service.S3Service;
import com.concrete.buildup.domain.workreport.dto.CreateWorkReportRequest;
import com.concrete.buildup.domain.workreport.dto.CreateWorkReportResponse;
import com.concrete.buildup.domain.workreport.dto.MaterialInputDto;
import com.concrete.buildup.domain.workreport.entity.WorkReport;
import com.concrete.buildup.domain.workreport.entity.WorkReportMaterial;
import com.concrete.buildup.domain.workreport.repository.WorkReportMaterialRepository;
import com.concrete.buildup.domain.workreport.repository.WorkReportRepository;
import com.concrete.buildup.global.exception.BusinessException;
import com.concrete.buildup.global.exception.errorcode.AuthErrorCode;
import com.concrete.buildup.global.exception.errorcode.WorkReportErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 작업일보 서비스
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkReportService {

    private final WorkReportRepository workReportRepository;
    private final WorkReportMaterialRepository workReportMaterialRepository;
    private final SiteRepository siteRepository;
    private final ManagerRepository managerRepository;
    private final WorkReportPdfService workReportPdfService;
    private final Optional<S3Service> s3Service;

    /**
     * 작업일보 생성 (PDF 자동 생성)
     *
     * <p>워크플로우:</p>
     * <ol>
     *   <li>Site 존재 여부 검증</li>
     *   <li>Manager 권한 검증</li>
     *   <li>중복 작성 검증 (동일 현장, 동일 날짜)</li>
     *   <li>WorkReport 엔티티 생성</li>
     *   <li>자재 목록 추가</li>
     *   <li>DB 저장 (트랜잭션)</li>
     *   <li>PDF 생성</li>
     *   <li>S3 업로드</li>
     *   <li>WorkReport 업데이트 (pdfUrl, pdfGeneratedAt)</li>
     *   <li>응답 반환</li>
     * </ol>
     *
     * @param siteId 현장 ID
     * @param request 작업일보 생성 요청
     * @param managerId 관리자 ID (JWT에서 추출)
     * @return 작업일보 생성 응답 (workReportId, pdfUrl)
     * @throws BusinessException 현장 미존재, 권한 없음, 중복 작성, PDF 생성 실패 등
     */
    @Transactional
    public CreateWorkReportResponse createWorkReport(
            Long siteId,
            CreateWorkReportRequest request,
            Long managerId
    ) {
        log.info("작업일보 생성 시작: siteId={}, managerId={}, workDate={}",
                siteId, managerId, request.getWorkDate());

        // 1. Site 존재 여부 검증
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new BusinessException(
                        WorkReportErrorCode.SITE_NOT_FOUND,
                        "현장을 찾을 수 없습니다. siteId=" + siteId
                ));

        // 2. Manager 조회
        Manager manager = managerRepository.findById(managerId)
                .orElseThrow(() -> new BusinessException(
                        AuthErrorCode.USER_NOT_FOUND,
                        "관리자를 찾을 수 없습니다. managerId=" + managerId
                ));

        // 3. 중복 작성 검증 (동일 현장, 동일 날짜)
        List<WorkReport> existingReports = workReportRepository
                .findBySiteIdAndWorkDateAndIsDeletedFalse(siteId, request.getWorkDate());
        if (!existingReports.isEmpty()) {
            throw new BusinessException(
                    WorkReportErrorCode.DUPLICATE_WORK_REPORT,
                    String.format("해당 날짜에 이미 작업일보가 작성되었습니다. workDate=%s", request.getWorkDate())
            );
        }

        // 4. WorkReport 엔티티 생성
        WorkReport workReport = WorkReport.builder()
                .site(site)
                .manager(manager)
                .workDate(request.getWorkDate())
                .workSection(request.getWorkSection())
                .workSectionEmployeeNum(request.getWorkSectionEmployeeNum())
                .workReportContext(request.getWorkReportContext())
                .build();

        // 5. 자재 목록 추가
        if (request.getMaterials() != null && !request.getMaterials().isEmpty()) {
            for (MaterialInputDto materialDto : request.getMaterials()) {
                WorkReportMaterial material = WorkReportMaterial.builder()
                        .materialName(materialDto.getMaterialName())
                        .materialStandard(materialDto.getMaterialStandard())
                        .materialUnit(materialDto.getMaterialUnit())
                        .build();

                workReport.addMaterial(material);
            }
        }

        // 6. DB 저장
        WorkReport savedWorkReport = workReportRepository.save(workReport);
        log.info("작업일보 저장 완료: workReportId={}", savedWorkReport.getId());

        try {
            // 7. PDF 생성
            byte[] pdfBytes = workReportPdfService.generateWorkReportPdf(
                    savedWorkReport,
                    site,
                    manager.getUser().getName(),
                    savedWorkReport.getMaterials()
            );

            // 8. S3 업로드
            S3Service service = s3Service.orElseThrow(() ->
                    new BusinessException(WorkReportErrorCode.S3_UPLOAD_FAILED, "S3 서비스가 비활성화되어 있습니다."));

            String s3Key = buildS3Key(savedWorkReport.getId(), siteId, request.getWorkDate().toString());
            service.uploadPdf(s3Key, pdfBytes);
            String pdfUrl = service.getPdfUrl(s3Key);

            log.info("작업일보 PDF S3 업로드 완료: s3Key={}, pdfUrl={}", s3Key, pdfUrl);

            // 9. WorkReport 업데이트 (pdfUrl, pdfGeneratedAt)
            savedWorkReport.updatePdf(pdfUrl, LocalDateTime.now());
            workReportRepository.save(savedWorkReport);

            log.info("작업일보 생성 완료: workReportId={}, pdfUrl={}", savedWorkReport.getId(), pdfUrl);

            // 10. 응답 반환
            return CreateWorkReportResponse.builder()
                    .workReportId(savedWorkReport.getId())
                    .pdfUrl(pdfUrl)
                    .message("작업일보가 생성되었습니다.")
                    .build();

        } catch (BusinessException e) {
            // BusinessException은 그대로 throw
            throw e;
        } catch (Exception e) {
            log.error("작업일보 PDF 생성 실패: workReportId={}", savedWorkReport.getId(), e);
            throw new BusinessException(
                    WorkReportErrorCode.PDF_GENERATION_FAILED,
                    "PDF 생성 중 오류가 발생했습니다: " + e.getMessage()
            );
        }
    }

    /**
     * S3 Key 생성
     *
     * <p>형식: work-reports/{siteId}/{workReportId}/WR-{date}.pdf</p>
     *
     * @param workReportId 작업일보 ID
     * @param siteId 현장 ID
     * @param workDate 작업일자
     * @return S3 Key
     */
    private String buildS3Key(Long workReportId, Long siteId, String workDate) {
        return String.format("work-reports/%d/%d/WR-%s.pdf", siteId, workReportId, workDate);
    }
}
