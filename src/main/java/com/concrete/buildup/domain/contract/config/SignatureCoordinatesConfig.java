package com.concrete.buildup.domain.contract.config;

import java.math.BigDecimal;

/**
 * 서명 좌표 설정
 *
 * <p>PDF 문서 내 서명 위치를 정의하는 상수 클래스입니다.</p>
 * <p>HTML 템플릿(contract-pdf.html)의 서명란 위치를 기반으로 계산된 고정값입니다.</p>
 *
 * <p>PDF 좌표계:</p>
 * <ul>
 *   <li>원점: 좌측 하단 (0, 0)</li>
 *   <li>A4 크기: 595 x 842 포인트</li>
 *   <li>Y 좌표: 아래에서 위로 증가</li>
 * </ul>
 *
 * <p>서명란 레이아웃 (HTML 템플릿 기준):</p>
 * <ul>
 *   <li>테이블 너비: 100% (body margin 40px 제외)</li>
 *   <li>왼쪽 칸 (50%): 관리자(사업주) 서명</li>
 *   <li>오른쪽 칸 (50%): 근로자 서명</li>
 *   <li>서명 칸 높이: 100px</li>
 * </ul>
 *
 * @author Build-Up Team
 * @since 1.0
 */
public final class SignatureCoordinatesConfig {

    private SignatureCoordinatesConfig() {
        // 인스턴스화 방지
    }

    // ========== PDF 문서 크기 (A4) ==========

    /** A4 PDF 너비 (포인트) */
    public static final double PDF_WIDTH = 595.0;

    /** A4 PDF 높이 (포인트) */
    public static final double PDF_HEIGHT = 842.0;

    // ========== 서명 이미지 크기 ==========

    /** 서명 이미지 너비 (포인트) - 서명란 너비에 맞춤 */
    public static final BigDecimal SIGNATURE_WIDTH = BigDecimal.valueOf(100.0);

    /** 서명 이미지 높이 (포인트) - 서명란 높이(60px)에 맞춤 */
    public static final BigDecimal SIGNATURE_HEIGHT = BigDecimal.valueOf(35.0);

    // ========== 관리자(사업주) 서명 좌표 ==========

    /**
     * 관리자 서명 X 좌표 (포인트)
     * <p>왼쪽 칸 중앙 정렬: 좌측 여백(40pt) + 서명 중앙 위치</p>
     */
    public static final BigDecimal MANAGER_SIGNATURE_X = BigDecimal.valueOf(90.0);

    /**
     * 관리자 서명 Y 좌표 (포인트)
     * <p>PDF 마지막 페이지 하단 기준, 서명란 위치</p>
     * <p>footer(20pt) + 서명란(60pt) 위쪽 = 약 90pt</p>
     */
    public static final BigDecimal MANAGER_SIGNATURE_Y = BigDecimal.valueOf(90.0);

    // ========== 근로자 서명 좌표 ==========

    /**
     * 근로자 서명 X 좌표 (포인트)
     * <p>오른쪽 칸 중앙 정렬: 문서 중앙(297.5pt) + 서명 중앙 위치</p>
     */
    public static final BigDecimal EMPLOYEE_SIGNATURE_X = BigDecimal.valueOf(380.0);

    /**
     * 근로자 서명 Y 좌표 (포인트)
     * <p>관리자 서명과 동일한 Y 좌표 (같은 행)</p>
     */
    public static final BigDecimal EMPLOYEE_SIGNATURE_Y = BigDecimal.valueOf(90.0);
}

