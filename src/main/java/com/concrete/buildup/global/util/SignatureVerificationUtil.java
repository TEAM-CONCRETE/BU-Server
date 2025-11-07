package com.concrete.buildup.global.util;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * SHA-256 해시 기반 전자서명 검증 유틸리티 클래스
 *
 * 전자서명 이미지의 무결성을 검증하기 위해 SHA-256 해시를 계산하고 비교합니다.
 * 클라이언트가 업로드한 서명 이미지의 해시값과 서버에 저장된 해시값을 비교하여
 * 서명의 위변조 여부를 확인합니다.
 *
 * SHA-256 해시 형식:
 * - 256비트 (32바이트) 해시 값
 * - 64자리 16진수 문자열 (0-9, a-f, A-F)
 * - 예: "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
 */
@Slf4j
public class SignatureVerificationUtil {

    /**
     * SHA-256 해시의 표준 길이 (16진수 문자열 기준)
     * SHA-256: 256비트 = 32바이트 = 64자리 16진수
     */
    private static final int SHA256_HEX_LENGTH = 64;

    /**
     * 16진수 문자열 검증을 위한 정규식
     * 소문자 a-f, 대문자 A-F, 숫자 0-9만 허용
     */
    private static final String HEX_PATTERN = "^[0-9a-fA-F]+$";

    /**
     * SHA-256 알고리즘 이름
     */
    private static final String SHA256_ALGORITHM = "SHA-256";

    /**
     * InputStream 읽기 버퍼 크기 (8KB)
     */
    private static final int BUFFER_SIZE = 8192;

    private SignatureVerificationUtil() {
        // Utility 클래스이므로 인스턴스화 방지
        throw new UnsupportedOperationException("이 클래스는 인스턴스화할 수 없습니다.");
    }

    /**
     * SHA-256 해시 형식이 올바른지 검증합니다.
     *
     * 검증 조건:
     * 1. null이 아니어야 함
     * 2. 정확히 64자여야 함 (SHA-256의 표준 길이)
     * 3. 16진수 문자만 포함해야 함 (0-9, a-f, A-F)
     *
     * @param hash 검증할 해시 문자열
     * @return 올바른 형식이면 true, 그렇지 않으면 false
     */
    public static boolean isValidHashFormat(String hash) {
        // null 체크
        if (hash == null) {
            log.debug("해시 검증 실패: null");
            return false;
        }

        // 길이 검증 (SHA-256은 64자)
        if (hash.length() != SHA256_HEX_LENGTH) {
            log.debug("해시 검증 실패: 잘못된 길이 ({}자, 예상: {}자)", hash.length(), SHA256_HEX_LENGTH);
            return false;
        }

        // 16진수 형식 검증
        if (!hash.matches(HEX_PATTERN)) {
            log.debug("해시 검증 실패: 16진수가 아닌 문자 포함");
            return false;
        }

        log.debug("해시 형식 검증 성공");
        return true;
    }

    /**
     * InputStream으로부터 SHA-256 해시를 계산합니다.
     *
     * 이미지 파일을 읽어서 SHA-256 해시값을 생성합니다.
     * 결과는 64자리 소문자 16진수 문자열로 반환됩니다.
     *
     * @param imageStream 해시를 계산할 이미지의 InputStream (null 불가)
     * @return 64자리 소문자 16진수 SHA-256 해시 문자열
     * @throws IllegalArgumentException InputStream이 null인 경우
     * @throws RuntimeException 해시 계산 중 오류 발생 시
     */
    public static String calculateSHA256(InputStream imageStream) {
        // null 체크
        if (imageStream == null) {
            log.error("SHA-256 해시 계산 실패: InputStream이 null입니다.");
            throw new IllegalArgumentException("InputStream은 null일 수 없습니다.");
        }

        try {
            // SHA-256 MessageDigest 생성
            MessageDigest digest = MessageDigest.getInstance(SHA256_ALGORITHM);

            // 버퍼를 사용하여 InputStream 읽기
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            while ((bytesRead = imageStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }

            // 해시 계산 완료
            byte[] hashBytes = digest.digest();

            // 바이트 배열을 16진수 문자열로 변환
            String hexHash = bytesToHex(hashBytes);

            log.debug("SHA-256 해시 계산 완료: {}", hexHash);
            return hexHash;

        } catch (NoSuchAlgorithmException e) {
            // SHA-256은 표준 알고리즘이므로 발생하지 않아야 함
            log.error("SHA-256 알고리즘을 찾을 수 없습니다: {}", e.getMessage());
            throw new RuntimeException("SHA-256 알고리즘을 지원하지 않는 환경입니다.", e);

        } catch (IOException e) {
            log.error("InputStream 읽기 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("이미지 데이터를 읽는 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 두 SHA-256 해시값을 비교하여 서명이 유효한지 검증합니다.
     *
     * 대소문자를 구분하지 않고 비교합니다. (16진수는 대소문자 무관)
     * 두 해시 모두 null이 아니고 유효한 형식이어야 하며, 값이 일치해야 합니다.
     *
     * @param expectedHash 예상되는 해시값 (DB에 저장된 해시)
     * @param actualHash 실제 계산된 해시값 (이미지로부터 계산)
     * @return 두 해시가 일치하면 true, 그렇지 않으면 false
     */
    public static boolean verifySignatureHash(String expectedHash, String actualHash) {
        // null 체크
        if (expectedHash == null || actualHash == null) {
            log.warn("해시 검증 실패: null 값이 입력되었습니다. (expected: {}, actual: {})",
                    expectedHash == null ? "null" : "ok",
                    actualHash == null ? "null" : "ok");
            return false;
        }

        // 해시 형식 검증
        if (!isValidHashFormat(expectedHash)) {
            log.warn("해시 검증 실패: 예상 해시가 올바른 형식이 아닙니다. (expected: {})", expectedHash);
            return false;
        }

        if (!isValidHashFormat(actualHash)) {
            log.warn("해시 검증 실패: 실제 해시가 올바른 형식이 아닙니다. (actual: {})", actualHash);
            return false;
        }

        // 대소문자 무시하고 비교
        boolean isMatch = expectedHash.equalsIgnoreCase(actualHash);

        if (isMatch) {
            log.info("서명 해시 검증 성공");
        } else {
            log.warn("서명 해시 검증 실패: 해시가 일치하지 않습니다.");
            log.debug("Expected: {}", expectedHash);
            log.debug("Actual:   {}", actualHash);
        }

        return isMatch;
    }

    /**
     * 바이트 배열을 16진수 문자열로 변환합니다.
     *
     * @param bytes 변환할 바이트 배열
     * @return 소문자 16진수 문자열
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();

        for (byte b : bytes) {
            // 각 바이트를 2자리 16진수로 변환
            String hex = Integer.toHexString(0xff & b);

            // 1자리 숫자인 경우 앞에 0 추가
            if (hex.length() == 1) {
                hexString.append('0');
            }

            hexString.append(hex);
        }

        return hexString.toString();
    }
}