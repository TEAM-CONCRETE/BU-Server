package com.concrete.buildup.global.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

/**
 * 공통 PDF 생성 서비스
 *
 * <p>다양한 도메인(계약서, 작업일보, 안전교육일지 등)에서 사용하는 공통 PDF 생성 기능을 제공합니다.</p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>HTML 템플릿을 PDF로 변환: Thymeleaf 템플릿을 렌더링하여 PDF 생성</li>
 *   <li>PDF에 이미지 스탬핑: 기존 PDF에 이미지를 지정된 위치에 삽입</li>
 * </ul>
 *
 * <p>사용 예시:</p>
 * <pre>
 * // 계약서 PDF 생성
 * Map&lt;String, Object&gt; variables = Map.of("contract", contract, "detail", detail);
 * byte[] pdf = pdfService.generatePdfFromTemplate("contract/contract-pdf", variables);
 *
 * // 작업일보 PDF 생성
 * Map&lt;String, Object&gt; variables = Map.of("workReport", workReport);
 * byte[] pdf = pdfService.generatePdfFromTemplate("workreport/workreport-pdf", variables);
 * </pre>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfService {

    private final TemplateEngine templateEngine;

    /**
     * HTML 템플릿을 PDF로 변환
     *
     * <p>Thymeleaf 템플릿을 렌더링하여 HTML을 생성하고, 이를 PDF로 변환합니다.</p>
     *
     * @param templatePath 템플릿 경로 (예: "contract/contract-pdf", "workreport/workreport-pdf")
     * @param variables 템플릿에 전달할 변수 맵
     * @return PDF 바이트 배열
     * @throws RuntimeException PDF 생성 실패 시
     */
    public byte[] generatePdfFromTemplate(String templatePath, Map<String, Object> variables) {
        log.info("PDF 생성 시작: templatePath={}", templatePath);

        try {
            // 1. Thymeleaf Context에 변수 설정
            Context context = new Context();
            if (variables != null) {
                variables.forEach(context::setVariable);
            }

            // 2. HTML 템플릿 렌더링
            String html = templateEngine.process(templatePath, context);
            log.debug("HTML 템플릿 렌더링 완료: templatePath={}, htmlLength={}", templatePath, html.length());

            // 3. HTML → PDF 변환
            byte[] pdfBytes = convertHtmlToPdf(html);

            log.info("PDF 생성 완료: templatePath={}, size={} bytes", templatePath, pdfBytes.length);
            return pdfBytes;

        } catch (Exception e) {
            log.error("PDF 생성 실패: templatePath={}", templatePath, e);
            throw new RuntimeException("PDF 생성 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * PDF에 이미지 스탬핑
     *
     * <p>기존 PDF에 이미지를 지정된 위치에 삽입합니다.</p>
     * <p>계약서 서명, 작업일보 서명, 안전교육일지 서명 등 다양한 용도로 사용 가능합니다.</p>
     *
     * @param originalPdfBytes 원본 PDF 바이트 배열
     * @param imageBytes 이미지 바이트 배열 (PNG, JPG 등)
     * @param x 이미지 X 좌표 (PDF pt 단위)
     * @param y 이미지 Y 좌표 (PDF pt 단위)
     * @param width 이미지 너비 (PDF pt 단위)
     * @param height 이미지 높이 (PDF pt 단위)
     * @return 이미지가 스탬핑된 PDF 바이트 배열
     * @throws RuntimeException 이미지 스탬핑 실패 시
     */
    public byte[] stampImageOnPdf(
            byte[] originalPdfBytes,
            byte[] imageBytes,
            BigDecimal x,
            BigDecimal y,
            BigDecimal width,
            BigDecimal height
    ) {
        log.info("PDF에 이미지 스탬핑 시작: x={}, y={}, width={}, height={}", x, y, width, height);

        try {
            // OpenPDF를 사용하여 PDF에 이미지 삽입
            // TODO: OpenPDF를 사용한 PDF 이미지 스탬핑 구현 필요
            // com.lowagie.text.pdf.PdfReader, com.lowagie.text.pdf.PdfStamper 등을 사용

            // 임시로 원본 PDF 반환 (실제 구현 필요)
            log.warn("PDF 이미지 스탬핑 기능이 아직 구현되지 않았습니다. 원본 PDF를 반환합니다.");
            return originalPdfBytes;

        } catch (Exception e) {
            log.error("PDF 이미지 스탬핑 실패", e);
            throw new RuntimeException("PDF 이미지 스탬핑 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * HTML을 PDF로 변환
     *
     * <p>Flying Saucer (xhtmlrenderer)를 사용하여 HTML을 PDF로 변환합니다.</p>
     * <p>OpenPDF와 호환되는 버전을 사용하여 HTML/CSS를 렌더링합니다.</p>
     *
     * @param html HTML 문자열
     * @return PDF 바이트 배열
     * @throws RuntimeException PDF 변환 실패 시
     */
    private byte[] convertHtmlToPdf(String html) {
        log.debug("HTML을 PDF로 변환 시작: htmlLength={}", html.length());

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // Flying Saucer를 사용하여 HTML을 PDF로 변환
            ITextRenderer renderer = new ITextRenderer();

            // HTML 문자열을 문서로 설정
            // baseUrl은 상대 경로 리소스를 위한 기본 URL (필요시 설정)
            renderer.setDocumentFromString(html, null);

            // PDF 레이아웃 계산 및 렌더링
            renderer.layout();

            // PDF 생성
            renderer.createPDF(baos);

            byte[] pdfBytes = baos.toByteArray();
            log.debug("HTML to PDF 변환 완료: pdfSize={} bytes", pdfBytes.length);

            return pdfBytes;

        } catch (IOException e) {
            log.error("HTML to PDF 변환 실패: IO 오류", e);
            throw new RuntimeException("HTML to PDF 변환 중 IO 오류가 발생했습니다: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("HTML to PDF 변환 실패", e);
            throw new RuntimeException("HTML to PDF 변환 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
}

