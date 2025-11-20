package com.concrete.buildup.domain.workreport.service;

import com.concrete.buildup.domain.site.entity.Site;
import com.concrete.buildup.domain.workreport.dto.WorkSectionDto;
import com.concrete.buildup.domain.workreport.entity.WorkReport;
import com.concrete.buildup.domain.workreport.entity.WorkReportMaterial;
import com.concrete.buildup.global.exception.BusinessException;
import com.concrete.buildup.global.exception.errorcode.WorkReportErrorCode;
import com.concrete.buildup.global.service.PdfService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 작업일보 PDF 생성 서비스
 *
 * <p>작업일보 전용 PDF 생성 기능을 제공합니다.</p>
 * <p>공통 PDF 생성 로직은 {@link PdfService}를 사용합니다.</p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>작업일보 PDF 생성: WorkReport 데이터를 HTML 템플릿에 반영하여 PDF 생성</li>
 * </ul>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkReportPdfService {

    private final PdfService pdfService;
    private final ObjectMapper objectMapper;

    /**
     * 작업일보 PDF 생성
     *
     * <p>WorkReport, Site, Manager 정보를 HTML 템플릿에 반영하여 PDF를 생성합니다.</p>
     *
     * @param workReport 작업일보 엔티티
     * @param site 현장 엔티티
     * @param managerName 관리자 이름
     * @param materials 자재 목록
     * @return PDF 바이트 배열
     */
    public byte[] generateWorkReportPdf(
            WorkReport workReport,
            Site site,
            String managerName,
            List<WorkReportMaterial> materials
    ) {
        log.info("작업일보 PDF 생성 시작: workReportId={}", workReport.getId());

        // JSON 문자열을 List<WorkSectionDto>로 파싱
        List<WorkSectionDto> workSections;
        try {
            workSections = objectMapper.readValue(
                    workReport.getWorkSections(),
                    new TypeReference<List<WorkSectionDto>>() {}
            );
        } catch (Exception e) {
            log.error("공정 정보 JSON 파싱 실패: workReportId={}", workReport.getId(), e);
            throw new BusinessException(
                    WorkReportErrorCode.PDF_GENERATION_FAILED,
                    "공정 정보 처리 중 오류가 발생했습니다."
            );
        }

        // 공통 PDF 서비스를 사용하여 템플릿 렌더링 및 PDF 변환
        Map<String, Object> variables = Map.of(
                "workReport", workReport,
                "site", site,
                "managerName", managerName,
                "materials", materials != null ? materials : List.of(),
                "workSections", workSections
        );

        return pdfService.generatePdfFromTemplate("workreport/workreport-pdf", variables);
    }
}
