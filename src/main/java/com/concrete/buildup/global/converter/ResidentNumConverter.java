package com.concrete.buildup.global.converter;

import com.concrete.buildup.global.util.AesEncryptionUtil;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

/**
 * 주민등록번호 암호화 컨버터
 *
 * JPA Entity의 주민등록번호 필드를 DB에 저장할 때 자동으로 암호화하고,
 * 조회 시 자동으로 복호화합니다.
 *
 * @see ConverterInjector - Spring Bean 주입을 위한 헬퍼 클래스
 */
@Slf4j
@Converter
public class ResidentNumConverter implements AttributeConverter<String, String> {

    private static AesEncryptionUtil aesEncryptionUtil;

    /**
     * Spring Bean 주입 (ConverterInjector에서 호출)
     */
    public static void setAesEncryptionUtil(AesEncryptionUtil aesEncryptionUtil) {
        ResidentNumConverter.aesEncryptionUtil = aesEncryptionUtil;
    }

    /**
     * Entity -> DB 변환 (암호화)
     */
    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }

        if (aesEncryptionUtil == null) {
            throw new IllegalStateException("AesEncryptionUtil이 초기화되지 않았습니다.");
        }

        log.debug("주민등록번호 암호화 수행");
        return aesEncryptionUtil.encrypt(attribute);
    }

    /**
     * DB -> Entity 변환 (복호화)
     */
    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }

        if (aesEncryptionUtil == null) {
            throw new IllegalStateException("AesEncryptionUtil이 초기화되지 않았습니다.");
        }

        log.debug("주민등록번호 복호화 수행");
        return aesEncryptionUtil.decrypt(dbData);
    }
}
