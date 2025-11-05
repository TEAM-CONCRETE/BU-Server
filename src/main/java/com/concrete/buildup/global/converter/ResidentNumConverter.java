package com.concrete.buildup.global.converter;

import com.concrete.buildup.global.util.AesEncryptionUtil;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 주민등록번호 암호화 컨버터
 *
 * JPA Entity의 주민등록번호 필드를 DB에 저장할 때 자동으로 암호화하고,
 * 조회 시 자동으로 복호화합니다.
 */
@Slf4j
@Component
@Converter
public class ResidentNumConverter implements AttributeConverter<String, String> {

    private AesEncryptionUtil aesEncryptionUtil;

    /**
     * JPA를 위한 기본 생성자
     */
    protected ResidentNumConverter() {
    }

    /**
     * Spring을 통한 의존성 주입
     */
    @Autowired
    public void setAesEncryptionUtil(AesEncryptionUtil aesEncryptionUtil) {
        this.aesEncryptionUtil = aesEncryptionUtil;
    }

    /**
     * Entity -> DB 변환 (암호화)
     */
    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
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
        log.debug("주민등록번호 복호화 수행");
        return aesEncryptionUtil.decrypt(dbData);
    }
}
