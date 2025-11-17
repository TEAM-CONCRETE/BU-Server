package com.concrete.buildup.global.util;

import java.security.SecureRandom;
import java.util.Random;

/**
 * 현장 시크릿키 생성 유틸리티
 *
 * 현장 등록 시 현장 관리자/근로자용 시크릿키를 생성합니다.
 * 형식: {기업명앞4글자대문자}-{siteId}-{randomString}-{randomNumber}
 * 예시: CONC-2039-XYZ-9205
 */
public class SecretKeyGenerator {

    private static final String DEFAULT_PREFIX = "SITE";
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int PREFIX_LENGTH = 4;
    private static final int RANDOM_STRING_LENGTH = 3;
    private static final int RANDOM_NUMBER_LENGTH = 4;
    private static final Random SECURE_RANDOM = new SecureRandom();

    private SecretKeyGenerator() {
        // 유틸리티 클래스는 인스턴스화 방지
    }

    /**
     * 현장 관리자용 시크릿키 생성
     *
     * @param corpName 기업명
     * @param siteId 현장 ID
     * @return 생성된 시크릿키 (형식: {기업명앞4글자}-{siteId}-{randomString}-{randomNumber})
     */
    public static String generateManagerSecretKey(String corpName, Long siteId) {
        return generateSecretKey(corpName, siteId);
    }

    /**
     * 근로자용 시크릿키 생성
     *
     * @param corpName 기업명
     * @param siteId 현장 ID
     * @return 생성된 시크릿키 (형식: {기업명앞4글자}-{siteId}-{randomString}-{randomNumber})
     */
    public static String generateEmployeeSecretKey(String corpName, Long siteId) {
        return generateSecretKey(corpName, siteId);
    }

    /**
     * 시크릿키 생성 (내부 메서드)
     *
     * @param corpName 기업명
     * @param siteId 현장 ID
     * @return 생성된 시크릿키
     * @throws IllegalArgumentException siteId가 null인 경우
     */
    private static String generateSecretKey(String corpName, Long siteId) {
        if (siteId == null) {
            throw new IllegalArgumentException("siteId must not be null");
        }

        String prefix = extractPrefix(corpName);
        String randomString = generateRandomString();
        String randomNumber = generateRandomNumber();
        return String.format("%s-%d-%s-%s", prefix, siteId, randomString, randomNumber);
    }

    /**
     * 기업명에서 Prefix 추출 (영문자 앞 4글자 대문자)
     *
     * @param corpName 기업명
     * @return 추출된 Prefix (최대 4글자, 기본값: "SITE")
     */
    private static String extractPrefix(String corpName) {
        if (corpName == null || corpName.isEmpty()) {
            return DEFAULT_PREFIX;
        }

        // 영문자만 추출
        StringBuilder alphaOnly = new StringBuilder();
        for (char c : corpName.toCharArray()) {
            if (Character.isAlphabetic(c) && c < 128) { // ASCII 영문자만
                alphaOnly.append(c);
            }
        }

        String extracted = alphaOnly.toString().toUpperCase();

        // 추출된 영문자가 없으면 기본값 사용
        if (extracted.isEmpty()) {
            return DEFAULT_PREFIX;
        }

        // 4글자 이상이면 앞 4글자만, 미만이면 있는 만큼 사용
        return extracted.length() >= PREFIX_LENGTH
            ? extracted.substring(0, PREFIX_LENGTH)
            : extracted;
    }

    /**
     * 랜덤 문자열 생성 (대문자 알파벳 3자리)
     *
     * @return 랜덤 문자열 (예: "XYZ")
     */
    private static String generateRandomString() {
        StringBuilder sb = new StringBuilder(RANDOM_STRING_LENGTH);
        for (int i = 0; i < RANDOM_STRING_LENGTH; i++) {
            int index = SECURE_RANDOM.nextInt(ALPHABET.length());
            sb.append(ALPHABET.charAt(index));
        }
        return sb.toString();
    }

    /**
     * 랜덤 숫자 생성 (4자리)
     *
     * @return 랜덤 숫자 문자열 (예: "9205")
     */
    private static String generateRandomNumber() {
        int number = SECURE_RANDOM.nextInt(10000); // 0 ~ 9999
        return String.format("%04d", number); // 4자리로 패딩 (예: 0001, 0123, 9999)
    }
}
