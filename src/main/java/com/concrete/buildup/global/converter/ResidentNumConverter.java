package com.concrete.buildup.global.converter;

import com.concrete.buildup.global.util.AesEncryptionUtil;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 주민등록번호 컨버터
 *
 * DB에 주민등록번호를 AES-256-GCM으로 암호화하여 저장하고,
 * DB에서 읽어올 때 자동으로 복호화합니다.
 * 실제 API 응답 시 마스킹은 ResidentNumMaskingSerializer에서 수행됩니다.
 */
@Slf4j
@Component
@Converter
@RequiredArgsConstructor
public class ResidentNumConverter implements AttributeConverter<String, String> {

    private final AesEncryptionUtil aesEncryptionUtil;

    /**
     * Entity -> DB 변환 (암호화하여 저장)
     */
    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        try {
            String encrypted = aesEncryptionUtil.encrypt(attribute);
            log.debug("주민등록번호 암호화 후 DB 저장");
            return encrypted;
        } catch (Exception e) {
            log.error("주민등록번호 암호화 실패: {}", e.getMessage());
            throw new RuntimeException("주민등록번호 암호화 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * DB -> Entity 변환 (복호화하여 반환)
     */
    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        try {
            String decrypted = aesEncryptionUtil.decrypt(dbData);
            log.debug("주민등록번호 복호화 후 Entity 반환");
            return decrypted;
        } catch (Exception e) {
            log.error("주민등록번호 복호화 실패: {}", e.getMessage());
            throw new RuntimeException("주민등록번호 복호화 중 오류가 발생했습니다.", e);
        }
    }
}
