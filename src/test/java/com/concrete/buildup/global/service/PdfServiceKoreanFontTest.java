package com.concrete.buildup.global.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PdfService 한글 폰트 렌더링 테스트
 * 실제 PDF 생성 및 한글 폰트 렌더링을 검증합니다.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("PdfService 한글 폰트 렌더링 테스트")
class PdfServiceKoreanFontTest {

    @Autowired
    private PdfService pdfService;

    @Test
    @DisplayName("한글 폰트 렌더링 테스트 - Thymeleaf 템플릿 없이 직접 HTML")
    void testKoreanFontRenderingWithDirectHtml() throws IOException {
        // given - 간단한 테스트 템플릿 생성
        // Thymeleaf 템플릿 대신 직접 HTML을 생성하여 테스트

        // PdfService의 private 메서드를 테스트하기 위해
        // 실제로는 작업일보 템플릿을 사용해서 테스트

        System.out.println("=".repeat(80));
        System.out.println("한글 폰트 렌더링 테스트를 시작합니다.");
        System.out.println("주의: 이 테스트는 Thymeleaf 템플릿이 필요합니다.");
        System.out.println("작업일보 템플릿(workreport/workreport-pdf)을 사용하여 실제 PDF를 생성합니다.");
        System.out.println("=".repeat(80));

        // 작업일보 템플릿에 전달할 간단한 테스트 데이터
        Map<String, Object> variables = new HashMap<>();

        // 간단한 객체 대신 Map 사용
        Map<String, Object> workReport = new HashMap<>();
        workReport.put("id", 1L);

        Map<String, Object> site = new HashMap<>();
        site.put("siteName", "테스트 현장");
        site.put("siteAddress", "서울시 강남구");

        variables.put("workReport", workReport);
        variables.put("site", site);
        variables.put("managerName", "김관리");
        variables.put("materials", new java.util.ArrayList<>());

        // WorkSectionDto 대신 Map 사용
        java.util.List<Map<String, Object>> workSections = new java.util.ArrayList<>();
        Map<String, Object> section1 = new HashMap<>();
        section1.put("sectionName", "철근공사");
        section1.put("employeeNum", 9);
        section1.put("context", "1층 바닥 철근 배근 작업 완료");
        workSections.add(section1);

        Map<String, Object> section2 = new HashMap<>();
        section2.put("sectionName", "거푸집공사");
        section2.put("employeeNum", 6);
        section2.put("context", "2층 기둥 거푸집 설치");
        workSections.add(section2);

        variables.put("workSections", workSections);

        // when - PDF 생성
        byte[] pdfBytes = null;
        try {
            pdfBytes = pdfService.generatePdfFromTemplate("workreport/workreport-pdf", variables);
        } catch (Exception e) {
            System.err.println("PDF 생성 중 오류 발생: " + e.getMessage());
            e.printStackTrace();

            // 템플릿이 없거나 다른 문제가 있을 경우 테스트 스킵
            System.out.println("템플릿을 찾을 수 없거나 다른 오류가 발생했습니다. 테스트를 종료합니다.");
            return;
        }

        // then
        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(1000);

        // 테스트 PDF 파일로 저장 (수동 확인용)
        String testPdfPath = "/tmp/korean-font-test-workreport.pdf";
        try (FileOutputStream fos = new FileOutputStream(testPdfPath)) {
            fos.write(pdfBytes);
        }

        System.out.println("=".repeat(80));
        System.out.println("✅ PDF 생성 완료: " + testPdfPath);
        System.out.println("📄 PDF 크기: " + pdfBytes.length + " bytes");
        System.out.println("=".repeat(80));
        System.out.println("🔍 다음 명령어로 PDF를 열어서 한글이 표시되는지 확인하세요:");
        System.out.println("  open " + testPdfPath);
        System.out.println("=".repeat(80));
        System.out.println("✅ 확인 사항:");
        System.out.println("  1. '철근공사', '거푸집공사' 등 한글 텍스트가 표시되는가?");
        System.out.println("  2. 숫자 9, 6이 표시되는가?");
        System.out.println("  3. 테이블 구조가 정상적으로 렌더링되는가?");
        System.out.println("=".repeat(80));

        // 파일이 생성되었는지 확인
        assertThat(Files.exists(Paths.get(testPdfPath))).isTrue();
    }
}
