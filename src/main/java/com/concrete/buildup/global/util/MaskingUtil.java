package com.concrete.buildup.global.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 개인정보 마스킹 유틸리티
 *
 * <p>개인식별정보(PII)를 마스킹 처리하는 유틸리티 클래스입니다.</p>
 * <p>GDPR, 개인정보보호법 등 컴플라이언스 준수를 위해 사용됩니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MaskingUtil {

    private static final String MASK_CHARACTER = "*";

    /**
     * 주민등록번호 마스킹
     *
     * <p>주민등록번호의 뒷자리를 마스킹 처리합니다.</p>
     *
     * <p>예시:</p>
     * <ul>
     *   <li>"010324-1234567" → "010324-1******"</li>
     *   <li>"0103241234567" → "010324-1******"</li>
     *   <li>null → null</li>
     *   <li>"" → ""</li>
     * </ul>
     *
     * @param residentNumber 주민등록번호 (하이픈 포함 가능)
     * @return 마스킹된 주민등록번호
     */
    public static String maskResidentNumber(String residentNumber) {
        if (residentNumber == null || residentNumber.isEmpty()) {
            return residentNumber;
        }

        // 하이픈 제거
        String cleaned = residentNumber.replace("-", "");

        // 유효성 검증 (13자리)
        if (cleaned.length() != 13) {
            return residentNumber; // 유효하지 않은 형식은 그대로 반환
        }

        // 앞 6자리 + 하이픈 + 뒷자리 첫 번째 + 6개 마스킹
        return cleaned.substring(0, 6) + "-" + cleaned.charAt(6) + MASK_CHARACTER.repeat(6);
    }

    /**
     * 이름 마스킹
     *
     * <p>이름의 중간 글자를 마스킹 처리합니다.</p>
     *
     * <p>예시:</p>
     * <ul>
     *   <li>"홍길동" → "홍*동"</li>
     *   <li>"김철수" → "김*수"</li>
     *   <li>"이도" → "이*"</li>
     *   <li>"박" → "박"</li>
     * </ul>
     *
     * @param name 이름
     * @return 마스킹된 이름
     */
    public static String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }

        int length = name.length();

        if (length == 1) {
            return name; // 한 글자는 마스킹하지 않음
        } else if (length == 2) {
            return name.charAt(0) + MASK_CHARACTER;
        } else {
            // 3글자 이상: 첫 글자 + 중간 마스킹 + 마지막 글자
            StringBuilder masked = new StringBuilder();
            masked.append(name.charAt(0));
            masked.append(MASK_CHARACTER.repeat(length - 2));
            masked.append(name.charAt(length - 1));
            return masked.toString();
        }
    }

    /**
     * 전화번호 마스킹
     *
     * <p>전화번호의 중간 자리를 마스킹 처리합니다.</p>
     *
     * <p>예시:</p>
     * <ul>
     *   <li>"010-1234-5678" → "010-****-5678"</li>
     *   <li>"02-123-4567" → "02-***-4567"</li>
     * </ul>
     *
     * @param phoneNumber 전화번호
     * @return 마스킹된 전화번호
     */
    public static String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return phoneNumber;
        }

        // 하이픈 기준으로 분리
        String[] parts = phoneNumber.split("-");

        if (parts.length != 3) {
            return phoneNumber; // 유효하지 않은 형식은 그대로 반환
        }

        // 중간 부분 마스킹
        return parts[0] + "-" + MASK_CHARACTER.repeat(parts[1].length()) + "-" + parts[2];
    }

    /**
     * 시크릿키 마스킹
     *
     * <p>시크릿키의 앞부분을 마스킹 처리하고 마지막 4자리만 표시합니다.</p>
     * <p>보안상 중요한 정보를 로그에 남길 때 사용됩니다.</p>
     *
     * <p>예시:</p>
     * <ul>
     *   <li>"abc123def456ghi789" → "**************i789"</li>
     *   <li>"short" → "*hort"</li>
     *   <li>null → null</li>
     *   <li>"" → ""</li>
     * </ul>
     *
     * @param secretKey 시크릿키
     * @return 마스킹된 시크릿키 (마지막 4자리만 표시)
     */
    public static String maskSecretKey(String secretKey) {
        if (secretKey == null || secretKey.isEmpty()) {
            return secretKey;
        }

        int length = secretKey.length();

        // 4자 이하인 경우: 첫 글자만 마스킹
        if (length <= 4) {
            return MASK_CHARACTER + secretKey.substring(1);
        }

        // 5자 이상인 경우: 마지막 4자리만 표시
        int visibleLength = 4;
        int maskLength = length - visibleLength;
        return MASK_CHARACTER.repeat(maskLength) + secretKey.substring(maskLength);
    }
}
