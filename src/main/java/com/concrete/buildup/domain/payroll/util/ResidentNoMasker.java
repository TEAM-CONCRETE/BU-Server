package com.concrete.buildup.domain.payroll.util;

/**
 * 주민등록번호 마스킹 유틸리티
 *
 * <p>주민등록번호를 개인정보 보호를 위해 마스킹 처리합니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
public class ResidentNoMasker {

    private ResidentNoMasker() {
        // 유틸리티 클래스 인스턴스화 방지
    }

    /**
     * 주민등록번호 마스킹 처리
     *
     * <p>뒷자리 7자리를 '*'로 마스킹합니다.</p>
     *
     * <pre>
     * 예시:
     * - 입력: "850101-1234567"
     * - 출력: "850101-1******"
     * </pre>
     *
     * @param residentNo 원본 주민등록번호 (형식: YYMMDD-NNNNNNN)
     * @return 마스킹된 주민등록번호 (형식: YYMMDD-N******, null이면 null 반환)
     */
    public static String mask(String residentNo) {
        if (residentNo == null || residentNo.isEmpty()) {
            return null;
        }

        // 하이픈 포함 14자리 형식 체크
        if (residentNo.length() != 14 || residentNo.charAt(6) != '-') {
            return residentNo; // 형식이 맞지 않으면 원본 반환
        }

        // 앞 8자리(생년월일 6자리 + 하이픈 + 성별 1자리) + 6개의 '*'
        return residentNo.substring(0, 8) + "******";
    }
}