package com.concrete.buildup.domain.workreport.service;

import com.concrete.buildup.domain.auth.entity.Manager;
import com.concrete.buildup.domain.auth.entity.User;
import com.concrete.buildup.domain.auth.repository.ManagerRepository;
import com.concrete.buildup.domain.auth.repository.UserRepository;
import com.concrete.buildup.domain.site.entity.Site;
import com.concrete.buildup.domain.site.repository.SiteRepository;
import com.concrete.buildup.domain.upload.service.S3Service;
import com.concrete.buildup.domain.workreport.dto.CreateWorkReportRequest;
import com.concrete.buildup.domain.workreport.dto.CreateWorkReportResponse;
import com.concrete.buildup.domain.workreport.dto.MaterialInputDto;
import com.concrete.buildup.domain.workreport.dto.WorkReportListResponse;
import com.concrete.buildup.domain.workreport.entity.WorkReport;
import com.concrete.buildup.domain.workreport.entity.WorkReportMaterial;
import com.concrete.buildup.domain.workreport.repository.WorkReportMaterialRepository;
import com.concrete.buildup.domain.workreport.repository.WorkReportRepository;
import com.concrete.buildup.global.exception.BusinessException;
import com.concrete.buildup.global.exception.errorcode.AuthErrorCode;
import com.concrete.buildup.global.exception.errorcode.WorkReportErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private final UserRepository userRepository;
    private final ManagerRepository managerRepository;
    private final WorkReportPdfService workReportPdfService;
    private final Optional<S3Service> s3Service;
    private final ObjectMapper objectMapper;

    /**
     * 작업일보 생성 (PDF 자동 생성)
     *
     * <p>워크플로우:</p>
     * <ol>
     *   <li>Site 존재 여부 검증</li>
     *   <li>Manager 존재 여부 검증</li>
     *   <li>Manager가 해당 Site에 권한이 있는지 검증</li>
     *   <li>오늘 생성된 작업일보 개수를 세어 순번(sequence) 결정</li>
     *   <li>WorkReport 엔티티 생성</li>
     *   <li>자재 목록 추가</li>
     *   <li>DB 저장 (트랜잭션)</li>
     *   <li>PDF 생성</li>
     *   <li>S3 업로드 (work-reports/{siteId}/{date}/WR-{date}-{sequence}.pdf)</li>
     *   <li>WorkReport 업데이트 (pdfUrl, pdfGeneratedAt)</li>
     *   <li>응답 반환</li>
     * </ol>
     *
     * <p>참고: 동일 현장의 동일 날짜에 여러 개의 작업일보 생성이 허용됩니다.
     * 각 작업일보에는 순번(1, 2, 3...)이 부여됩니다.</p>
     *
     * @param siteId 현장 ID
     * @param request 작업일보 생성 요청
     * @param currentUserId 관리자 userId (JWT에서 추출)
     * @return 작업일보 생성 응답 (workReportId, pdfUrl)
     * @throws BusinessException 현장 미존재, 관리자 미존재, 권한 없음, PDF 생성 실패 등
     */
    @Transactional
    public CreateWorkReportResponse createWorkReport(
            Long siteId,
            CreateWorkReportRequest request,
            String currentUserId
    ) {
        log.info("작업일보 생성 시작: siteId={}, currentUserId={}",
                siteId, currentUserId);

        // 1. Site 존재 여부 검증
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new BusinessException(
                        WorkReportErrorCode.SITE_NOT_FOUND,
                        "현장을 찾을 수 없습니다. siteId=" + siteId
                ));

        // 2. User 조회 후 Manager 조회
        User user = userRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new BusinessException(
                        AuthErrorCode.USER_NOT_FOUND,
                        "사용자를 찾을 수 없습니다. userId=" + currentUserId
                ));

        Manager manager = managerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessException(
                        AuthErrorCode.USER_NOT_FOUND,
                        "관리자를 찾을 수 없습니다. userId=" + currentUserId
                ));

        // 3. Manager가 해당 Site에 권한이 있는지 검증
        // Site에 할당된 manager와 현재 요청한 manager가 일치하는지 확인
        if (site.getManager() == null || !site.getManager().getId().equals(manager.getId())) {
            throw new BusinessException(
                    WorkReportErrorCode.MANAGER_NOT_AUTHORIZED,
                    "해당 현장에 대한 권한이 없습니다. siteId=" + siteId + ", managerId=" + manager.getId()
            );
        }

        // 4. 오늘 생성된 작업일보 개수 조회 (순번 계산용)
        LocalDate today = LocalDate.now();
        String todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE);

        // 오늘 작성된 작업일보 개수를 세어 순번 결정
        long todayCount = workReportRepository
                .findBySiteIdAndIsDeletedFalse(siteId).stream()
                .filter(wr -> wr.getCreatedAt() != null && wr.getCreatedAt().toLocalDate().equals(today))
                .count();

        int sequenceNumber = (int) todayCount + 1;
        log.info("오늘({}) 생성된 작업일보 개수: {}, 다음 순번: {}", todayStr, todayCount, sequenceNumber);

        // 5. 공정 정보를 JSON으로 변환
        String workSectionsJson;
        try {
            workSectionsJson = objectMapper.writeValueAsString(request.getWorkSections());
        } catch (JsonProcessingException e) {
            log.error("공정 정보 JSON 변환 실패", e);
            throw new BusinessException(
                    WorkReportErrorCode.INVALID_WORK_SECTION_DATA,
                    "공정 정보 JSON 변환에 실패했습니다: " + e.getMessage()
            );
        }

        // 6. WorkReport 엔티티 생성
        WorkReport workReport = WorkReport.builder()
                .site(site)
                .manager(manager)
                .corporation(site.getCorporation())
                .workSections(workSectionsJson)
                .build();

        // 7. 자재 목록 추가
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

        // 8. DB 저장
        WorkReport savedWorkReport = workReportRepository.save(workReport);
        log.info("작업일보 저장 완료: workReportId={}", savedWorkReport.getId());

        try {
            // 9. PDF 생성
            byte[] pdfBytes = workReportPdfService.generateWorkReportPdf(
                    savedWorkReport,
                    site,
                    manager.getManagerName(),
                    savedWorkReport.getMaterials()
            );

            // 10. S3 업로드
            S3Service service = s3Service.orElseThrow(() ->
                    new BusinessException(WorkReportErrorCode.S3_UPLOAD_FAILED, "S3 서비스가 비활성화되어 있습니다."));

            String s3Key = buildS3Key(savedWorkReport.getId(), siteId, todayStr, sequenceNumber);
            service.uploadPdf(s3Key, pdfBytes);
            String pdfUrl = service.getPdfUrl(s3Key);

            log.info("작업일보 PDF S3 업로드 완료: s3Key={}, pdfUrl={}", s3Key, pdfUrl);

            // 11. WorkReport 업데이트 (pdfUrl, pdfGeneratedAt)
            savedWorkReport.updatePdf(pdfUrl, LocalDateTime.now());
            workReportRepository.save(savedWorkReport);

            log.info("작업일보 생성 완료: workReportId={}, pdfUrl={}", savedWorkReport.getId(), pdfUrl);

            // 12. 응답 반환
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
     * <p>형식: work-reports/{siteId}/{date}/WR-{date}-{sequence}.pdf</p>
     * <p>날짜별 폴더로 구성하여 동일 현장의 작업일보를 날짜별로 정리합니다.</p>
     * <p>같은 날짜에 여러 개 생성 시 순번(-1, -2, -3)을 사용합니다.</p>
     *
     * @param workReportId 작업일보 ID (로그용)
     * @param siteId 현장 ID
     * @param createdDate 작성일 (yyyy-MM-dd 형식)
     * @param sequenceNumber 순번 (1부터 시작)
     * @return S3 Key
     */
    private String buildS3Key(Long workReportId, Long siteId, String createdDate, int sequenceNumber) {
        return String.format("work-reports/%d/%s/WR-%s-%d.pdf", siteId, createdDate, createdDate, sequenceNumber);
    }

    /**
     * 현장 관리자용 작업일보 목록 조회
     *
     * <p>현장에 속한 작업일보 목록을 페이지네이션하여 조회합니다.</p>
     * <p>작업일자, 작성자 이름을 포함하여 반환합니다.</p>
     * <p>연도/월 필터링 옵션을 지원합니다.</p>
     *
     * @param siteId 현장 ID
     * @param year 연도 (선택)
     * @param month 월 (선택, 1-12)
     * @param pageable 페이지네이션 정보
     * @return 작업일보 목록 응답
     */
    public WorkReportListResponse getWorkReportList(Long siteId, Integer year, Integer month, Pageable pageable) {
        log.info("작업일보 목록 조회: siteId={}, year={}, month={}, page={}, size={}",
                siteId, year, month, pageable.getPageNumber(), pageable.getPageSize());

        // 현장 존재 여부 확인
        if (!siteRepository.existsById(siteId)) {
            throw new BusinessException(WorkReportErrorCode.SITE_NOT_FOUND);
        }

        // 작업일보 목록 조회 (Manager fetch join)
        Page<WorkReport> workReportPage;
        if (year != null && month != null) {
            // 연도/월 필터링
            workReportPage = workReportRepository.findBySiteIdWithManagerByYearMonth(siteId, year, month, pageable);
        } else {
            // 전체 조회
            workReportPage = workReportRepository.findBySiteIdWithManager(siteId, pageable);
        }

        // DTO 변환
        Page<WorkReportListResponse.WorkReportSummary> summaryPage = workReportPage.map(workReport ->
                WorkReportListResponse.WorkReportSummary.builder()
                        .workReportId(workReport.getId())
                        .workDate(workReport.getCreatedAt().toLocalDate())
                        .writerName(workReport.getManager().getManagerName())
                        .build()
        );

        log.info("작업일보 목록 조회 완료: siteId={}, totalElements={}", siteId, summaryPage.getTotalElements());

        return WorkReportListResponse.from(summaryPage);
    }
}
