package com.concrete.buildup.domain.workreport.service;

import com.concrete.buildup.domain.site.entity.Site;
import com.concrete.buildup.domain.workreport.entity.WorkReport;
import com.concrete.buildup.domain.workreport.entity.WorkReportMaterial;
import com.concrete.buildup.global.service.PdfService;
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

        // 공통 PDF 서비스를 사용하여 템플릿 렌더링 및 PDF 변환
        Map<String, Object> variables = Map.of(
                "workReport", workReport,
                "site", site,
                "managerName", managerName,
                "materials", materials != null ? materials : List.of()
        );

        return pdfService.generatePdfFromTemplate("workreport/workreport-pdf", variables);
    }
}
