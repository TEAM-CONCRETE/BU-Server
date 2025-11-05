package com.concrete.buildup.global.util;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM 암호화/복호화 유틸리티 클래스
 *
 * 주민등록번호와 같은 개인정보를 안전하게 암호화하여 DB에 저장하고,
 * 필요 시 복호화하는 기능을 제공합니다.
 *
 * 보안 강화:
 * - AES-256-GCM 모드 사용 (ECB 모드 취약점 해결)
 * - 매 암호화마다 랜덤 IV 생성 (재사용 공격 방지)
 * - 암호화 키 길이 검증 (32바이트 필수)
 */
@Slf4j
@Component
public class AesEncryptionUtil {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // GCM 표준 IV 길이 (96비트)
    private static final int GCM_TAG_LENGTH = 128; // GCM 인증 태그 길이 (128비트)
    private static final int AES_KEY_SIZE = 32; // AES-256 키 길이 (256비트 = 32바이트)

    @Value("${security.encryption.key}")
    private String encryptionKey;

    private SecureRandom secureRandom;

    /**
     * 초기화 시 암호화 키 검증 및 SecureRandom 초기화
     */
    @PostConstruct
    public void init() {
        validateEncryptionKey();
        initSecureRandom();
    }

    /**
     * 암호화 키 길이 검증
     */
    private void validateEncryptionKey() {
        if (encryptionKey == null || encryptionKey.isEmpty()) {
            throw new IllegalStateException(
                "암호화 키가 설정되지 않았습니다. ENCRYPTION_KEY 환경변수를 설정해주세요."
            );
        }

        byte[] keyBytes = encryptionKey.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length != AES_KEY_SIZE) {
            throw new IllegalStateException(
                String.format(
                    "암호화 키는 정확히 %d바이트여야 합니다. 현재: %d바이트",
                    AES_KEY_SIZE,
                    keyBytes.length
                )
            );
        }

        log.info("AES-256 암호화 키 검증 완료 ({}바이트)", AES_KEY_SIZE);
    }

    /**
     * 강력한 난수 생성기 초기화
     */
    private void initSecureRandom() {
        try {
            this.secureRandom = SecureRandom.getInstanceStrong();
            log.info("SecureRandom 초기화 완료: {}", secureRandom.getAlgorithm());
        } catch (Exception e) {
            log.warn("SecureRandom.getInstanceStrong() 실패, 기본 SecureRandom 사용: {}", e.getMessage());
            this.secureRandom = new SecureRandom();
        }
    }

    /**
     * 문자열을 AES-256-GCM으로 암호화
     *
     * @param plainText 평문
     * @return Base64로 인코딩된 암호문 (IV + 암호문)
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }

        try {
            // 1. 랜덤 IV 생성 (12바이트)
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            // 2. 암호화 키 설정
            SecretKeySpec keySpec = new SecretKeySpec(
                encryptionKey.getBytes(StandardCharsets.UTF_8),
                ALGORITHM
            );

            // 3. GCM 파라미터 설정
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            // 4. 암호화 수행
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmParameterSpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // 5. IV + 암호문 결합 후 Base64 인코딩
            ByteBuffer byteBuffer = ByteBuffer.allocate(GCM_IV_LENGTH + encrypted.length);
            byteBuffer.put(iv);
            byteBuffer.put(encrypted);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            log.error("암호화 실패: {}", e.getMessage());
            throw new RuntimeException("암호화 처리 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * AES-256-GCM으로 암호화된 문자열을 복호화
     *
     * @param encryptedText Base64로 인코딩된 암호문 (IV + 암호문)
     * @return 평문
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }

        try {
            // 1. Base64 디코딩
            byte[] decoded = Base64.getDecoder().decode(encryptedText);

            // 2. IV와 암호문 분리
            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);

            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            // 3. 복호화 키 설정
            SecretKeySpec keySpec = new SecretKeySpec(
                encryptionKey.getBytes(StandardCharsets.UTF_8),
                ALGORITHM
            );

            // 4. GCM 파라미터 설정
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            // 5. 복호화 수행
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmParameterSpec);
            byte[] decrypted = cipher.doFinal(cipherText);

            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("복호화 실패: {}", e.getMessage());
            throw new RuntimeException("복호화 처리 중 오류가 발생했습니다.", e);
        }
    }
}
