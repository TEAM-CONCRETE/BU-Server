package com.concrete.buildup.global.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 주민등록번호 컨버터
 *
 * DB에 암호화되지 않은 상태로 저장된 주민등록번호를 처리합니다.
 * 실제 마스킹은 ResidentNumMaskingSerializer에서 수행됩니다.
 */
@Slf4j
@Component
@Converter
public class ResidentNumConverter implements AttributeConverter<String, String> {

    /**
     * Entity -> DB 변환 (그대로 저장)
     */
    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        log.debug("주민등록번호 DB 저장");
        return attribute;
    }

    /**
     * DB -> Entity 변환 (그대로 반환)
     */
    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        log.debug("주민등록번호 DB 조회");
        return dbData;
    }
}
