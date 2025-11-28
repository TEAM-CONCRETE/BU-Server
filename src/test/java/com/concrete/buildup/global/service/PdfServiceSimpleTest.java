package com.concrete.buildup.global.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.thymeleaf.TemplateEngine;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PdfService 간단한 한글 폰트 테스트
 * private 메서드인 convertHtmlToPdf를 Reflection으로 호출하여 테스트
 */
@SpringBootTest
@DisplayName("PdfService 간단한 폰트 테스트")
class PdfServiceSimpleTest {

    @Autowired
    private TemplateEngine templateEngine;

    @Test
    @DisplayName("한글 폰트 렌더링 - 간단한 HTML 직접 변환")
    void testKoreanFontWithSimpleHtml() throws Exception {
        // given
        PdfService pdfService = new PdfService(templateEngine);

        String simpleHtml = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8" />
                    <style>
                        body {
                            font-family: 'KoreanFont', sans-serif;
                            font-size: 14pt;
                            margin: 40px;
                        }
                        h1 {
                            font-size: 24pt;
                            font-weight: bold;
                            color: #333;
                        }
                        table {
                            width: 100%;
                            border-collapse: collapse;
                            margin: 20px 0;
                        }
                        th, td {
                            border: 1px solid #000;
                            padding: 10px;
                            text-align: left;
                        }
                        th {
                            background-color: #f0f0f0;
                            font-weight: bold;
                        }
                    </style>
                </head>
                <body>
                    <h1>작업일보 한글 폰트 테스트</h1>

                    <h2>1. 한글 텍스트 렌더링</h2>
                    <p>철근공사, 거푸집공사, 콘크리트타설, 미장공사, 도장공사</p>

                    <h2>2. 숫자 렌더링</h2>
                    <p>인원: 123명, 작업시간: 8시간, 자재: 456개</p>

                    <h2>3. 혼합 텍스트</h2>
                    <p>1층 바닥 철근 배근 작업 완료 (인원: 9명)</p>
                    <p>2층 기둥 거푸집 설치 (인원: 6명)</p>

                    <h2>4. 테이블 렌더링</h2>
                    <table>
                        <thead>
                            <tr>
                                <th>공정명</th>
                                <th>인원수</th>
                                <th>작업내용</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td>철근공사</td>
                                <td>9</td>
                                <td>1층 바닥 철근 배근 작업 완료</td>
                            </tr>
                            <tr>
                                <td>거푸집공사</td>
                                <td>6</td>
                                <td>2층 기둥 거푸집 설치</td>
                            </tr>
                            <tr>
                                <td>콘크리트타설</td>
                                <td>5</td>
                                <td>1층 슬라브 콘크리트 타설</td>
                            </tr>
                        </tbody>
                    </table>

                    <p style="margin-top: 40px; font-size: 12pt; color: #666;">
                        ✅ 이 PDF에서 한글 텍스트가 정상적으로 보인다면 폰트 설정이 올바르게 되어 있는 것입니다.
                    </p>
                </body>
                </html>
                """;

        // when - Reflection을 사용하여 private 메서드 호출
        Method convertMethod = PdfService.class.getDeclaredMethod("convertHtmlToPdf", String.class);
        convertMethod.setAccessible(true);

        byte[] pdfBytes = (byte[]) convertMethod.invoke(pdfService, simpleHtml);

        // then
        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(1000);

        // 테스트 PDF 파일로 저장
        String testPdfPath = "/tmp/korean-font-simple-test.pdf";
        try (FileOutputStream fos = new FileOutputStream(testPdfPath)) {
            fos.write(pdfBytes);
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.println("✅ PDF 생성 완료!");
        System.out.println("=".repeat(80));
        System.out.println("📄 파일 경로: " + testPdfPath);
        System.out.println("📊 파일 크기: " + pdfBytes.length + " bytes (" + (pdfBytes.length / 1024) + " KB)");
        System.out.println("=".repeat(80));
        System.out.println("🔍 PDF 열기 명령어:");
        System.out.println("  open " + testPdfPath);
        System.out.println("=".repeat(80));
        System.out.println("✅ 확인 사항:");
        System.out.println("  1. 한글 텍스트: '철근공사', '거푸집공사', '콘크리트타설' 등이 보이는가?");
        System.out.println("  2. 숫자: 123, 456, 9, 6, 5 등이 보이는가?");
        System.out.println("  3. 테이블: 테이블 구조와 내용이 정상적으로 렌더링되는가?");
        System.out.println("  4. 특수문자: ✅ 체크마크가 보이는가?");
        System.out.println("=".repeat(80));
        System.out.println("❌ 만약 한글이 보이지 않는다면:");
        System.out.println("  - Flying Saucer가 폰트를 제대로 등록하지 못한 것입니다.");
        System.out.println("  - 로그에서 '한글 폰트 등록 성공' 메시지를 확인하세요.");
        System.out.println("=".repeat(80) + "\n");

        // 파일이 생성되었는지 확인
        assertThat(Files.exists(Paths.get(testPdfPath))).isTrue();

        // PDF가 일정 크기 이상인지 확인 (한글 폰트가 임베딩되면 파일 크기가 큼)
        // AppleGothic 폰트 임베딩 시 최소 10KB 이상
        assertThat(pdfBytes.length).isGreaterThan(10 * 1024);
    }
}
