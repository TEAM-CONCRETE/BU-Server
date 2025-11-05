package com.concrete.buildup.global.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * AES-256 암호화/복호화 유틸리티 클래스
 *
 * 주민등록번호와 같은 개인정보를 안전하게 암호화하여 DB에 저장하고,
 * 필요 시 복호화하는 기능을 제공합니다.
 */
@Slf4j
@Component
public class AesEncryptionUtil {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";

    @Value("${security.encryption.key}")
    private String encryptionKey;

    /**
     * 문자열을 AES-256으로 암호화
     *
     * @param plainText 평문
     * @return Base64로 인코딩된 암호문
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }

        try {
            SecretKeySpec keySpec = new SecretKeySpec(
                encryptionKey.getBytes(StandardCharsets.UTF_8),
                ALGORITHM
            );

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);

            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("암호화 실패: {}", e.getMessage());
            throw new RuntimeException("암호화 처리 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * AES-256으로 암호화된 문자열을 복호화
     *
     * @param encryptedText Base64로 인코딩된 암호문
     * @return 평문
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }

        try {
            SecretKeySpec keySpec = new SecretKeySpec(
                encryptionKey.getBytes(StandardCharsets.UTF_8),
                ALGORITHM
            );

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);

            byte[] decoded = Base64.getDecoder().decode(encryptedText);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("복호화 실패: {}", e.getMessage());
            throw new RuntimeException("복호화 처리 중 오류가 발생했습니다.", e);
        }
    }
}
