package com.concrete.buildup.global.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
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

        PdfReader reader = null;
        PdfStamper stamper = null;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // 1. 원본 PDF 읽기
            reader = new PdfReader(originalPdfBytes);

            // 2. PdfStamper 생성 (수정 가능한 PDF 생성)
            stamper = new PdfStamper(reader, baos);

            // 3. 이미지 객체 생성
            Image image = Image.getInstance(imageBytes);

            // 4. 이미지 크기 설정
            image.scaleAbsolute(width.floatValue(), height.floatValue());

            // 5. 첫 번째 페이지에 이미지 삽입
            // PDF 좌표계: 왼쪽 하단이 (0, 0), Y축은 아래에서 위로 증가
            // 일반적으로 Y 좌표는 페이지 높이에서 빼서 계산해야 할 수 있음
            PdfContentByte contentByte = stamper.getOverContent(1);
            contentByte.addImage(image, width.floatValue(), 0, 0, height.floatValue(),
                    x.floatValue(), y.floatValue());

            // 6. PdfStamper를 먼저 닫아야 baos에 데이터가 flush됨
            stamper.close();
            stamper = null; // finally에서 이중 종료 방지

            // 7. 완성된 PDF 바이트 배열 반환
            byte[] resultPdfBytes = baos.toByteArray();
            log.info("PDF 이미지 스탬핑 완료: originalSize={} bytes, resultSize={} bytes",
                    originalPdfBytes.length, resultPdfBytes.length);

            return resultPdfBytes;

        } catch (IOException e) {
            log.error("PDF 이미지 스탬핑 실패: IO 오류", e);
            throw new RuntimeException("PDF 이미지 스탬핑 중 IO 오류가 발생했습니다: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("PDF 이미지 스탬핑 실패", e);
            throw new RuntimeException("PDF 이미지 스탬핑 중 오류가 발생했습니다: " + e.getMessage(), e);
        } finally {
            // 리소스 정리 (이중 종료 방지)
            if (stamper != null) {
                try {
                    stamper.close();
                } catch (Exception e) {
                    log.warn("PdfStamper 닫기 실패", e);
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    log.warn("PdfReader 닫기 실패", e);
                }
            }
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

            // 한글 폰트 등록 (macOS/Windows/Linux 대응) - layout() 전에 호출해야 함
            registerKoreanFonts(renderer);

            // HTML 문자열을 문서로 설정
            // baseUrl은 상대 경로 리소스를 위한 기본 URL (필요시 설정)
            renderer.setDocumentFromString(html, null);

            // PDF 레이아웃 계산 및 렌더링 (폰트 등록 후 호출)
            renderer.layout();

            // PDF 생성
            renderer.createPDF(baos);

            // 리소스 정리
            renderer.finishPDF();

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

    /**
     * 한글 폰트 등록
     *
     * <p>Flying Saucer에 한글 폰트를 등록하여 PDF에서 한글이 표시되도록 합니다.</p>
     * <p>프로젝트 리소스에 포함된 폰트를 사용하여 환경 독립적으로 작동합니다.</p>
     * <p>중요: setDocumentFromString() 호출 전에 폰트를 등록해야 합니다.</p>
     *
     * @param renderer ITextRenderer 인스턴스
     */
    private void registerKoreanFonts(ITextRenderer renderer) {
        try {
            // 프로젝트 리소스에 포함된 한글 폰트 사용 (환경 독립적)
            renderer.getFontResolver().addFont(
                    "/fonts/NanumGothic.ttf",
                    "KoreanFont",  // CSS에서 사용할 font-family 이름
                    com.lowagie.text.pdf.BaseFont.IDENTITY_H,
                    com.lowagie.text.pdf.BaseFont.EMBEDDED,
                    null
            );
            log.info("✅ 한글 폰트 등록 성공: /fonts/NanumGothic.ttf (family name: KoreanFont)");
        } catch (Exception e) {
            log.error("❌ 한글 폰트 등록 실패: {}", e.getMessage(), e);
            log.error("프로젝트 리소스에 /fonts/NanumGothic.ttf 파일이 있는지 확인하세요.");
        }
    }
}

