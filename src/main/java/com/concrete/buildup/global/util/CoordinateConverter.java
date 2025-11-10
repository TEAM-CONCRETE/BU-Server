package com.concrete.buildup.global.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 좌표 변환 유틸리티
 *
 * <p>프론트엔드 뷰포트 좌표를 PDF 좌표(pt)로 변환하는 기능을 제공합니다.</p>
 *
 * <p>좌표계 차이:</p>
 * <ul>
 *   <li>프론트엔드 뷰포트: 왼쪽 상단이 (0, 0), Y축은 아래로 증가</li>
 *   <li>PDF 좌표계: 왼쪽 하단이 (0, 0), Y축은 위로 증가</li>
 * </ul>
 *
 * <p>사용 예시:</p>
 * <pre>
 * // 프론트엔드에서 서명 이미지 위치: (100, 200) 뷰포트 크기: 800x1131
 * PdfCoordinates coords = CoordinateConverter.convertToPdfCoordinates(
 *     100, 200,           // 뷰포트 X, Y
 *     800, 1131,          // 뷰포트 너비, 높이
 *     595.0, 842.0        // PDF 페이지 너비, 높이 (A4)
 * );
 * // PDF 좌표: (74.38, 653.50)
 * </pre>
 *
 * @author Build-Up Team
 * @since 1.0
 */
public class CoordinateConverter {

    /**
     * A4 페이지 너비 (pt)
     * 1 inch = 72 pt, A4 width = 8.27 inch
     */
    private static final double PDF_PAGE_WIDTH = 595.0;

    /**
     * A4 페이지 높이 (pt)
     * 1 inch = 72 pt, A4 height = 11.69 inch
     */
    private static final double PDF_PAGE_HEIGHT = 842.0;

    /**
     * 프론트엔드 뷰포트 좌표를 PDF 좌표(pt)로 변환
     *
     * <p>변환 과정:</p>
     * <ol>
     *   <li>뷰포트 크기와 PDF 페이지 크기의 스케일 비율 계산</li>
     *   <li>X 좌표를 스케일에 맞게 변환</li>
     *   <li>Y 좌표를 스케일에 맞게 변환하고 Y축 반전 (상단 기준 → 하단 기준)</li>
     * </ol>
     *
     * @param viewX 뷰포트 X 좌표 (왼쪽에서부터의 거리)
     * @param viewY 뷰포트 Y 좌표 (위에서부터의 거리)
     * @param viewWidth 뷰포트 너비 (픽셀)
     * @param viewHeight 뷰포트 높이 (픽셀)
     * @param pdfPageWidth PDF 페이지 너비 (pt, A4 기준 595.0)
     * @param pdfPageHeight PDF 페이지 높이 (pt, A4 기준 842.0)
     * @return PDF 좌표 (소수점 둘째 자리 반올림)
     */
    public static PdfCoordinates convertToPdfCoordinates(
            double viewX, double viewY,
            double viewWidth, double viewHeight,
            double pdfPageWidth, double pdfPageHeight
    ) {
        // 스케일 계산 (뷰포트 → PDF)
        double scaleX = pdfPageWidth / viewWidth;
        double scaleY = pdfPageHeight / viewHeight;

        // PDF 좌표 변환
        // X: 뷰포트와 PDF 모두 왼쪽이 기준이므로 스케일만 적용
        double pdfX = viewX * scaleX;

        // Y: 뷰포트는 상단 기준, PDF는 하단 기준이므로 Y축 반전 필요
        // PDF Y = (페이지 높이) - (뷰포트 Y * 스케일)
        double pdfY = pdfPageHeight - (viewY * scaleY);

        return new PdfCoordinates(
                BigDecimal.valueOf(pdfX).setScale(2, RoundingMode.HALF_UP),
                BigDecimal.valueOf(pdfY).setScale(2, RoundingMode.HALF_UP)
        );
    }

    /**
     * PDF 좌표를 담는 불변 클래스
     */
    public static class PdfCoordinates {
        private final BigDecimal x;
        private final BigDecimal y;

        /**
         * PDF 좌표 생성자
         *
         * @param x PDF X 좌표 (pt)
         * @param y PDF Y 좌표 (pt)
         */
        public PdfCoordinates(BigDecimal x, BigDecimal y) {
            this.x = x;
            this.y = y;
        }

        /**
         * PDF X 좌표 반환
         *
         * @return X 좌표 (pt)
         */
        public BigDecimal getX() {
            return x;
        }

        /**
         * PDF Y 좌표 반환
         *
         * @return Y 좌표 (pt)
         */
        public BigDecimal getY() {
            return y;
        }

        @Override
        public String toString() {
            return String.format("PdfCoordinates(x=%.2f, y=%.2f)", x, y);
        }
    }
}
