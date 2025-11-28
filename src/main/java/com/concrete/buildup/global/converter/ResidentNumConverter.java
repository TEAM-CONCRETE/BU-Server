package com.concrete.buildup.global.converter;

import com.concrete.buildup.global.util.AesEncryptionUtil;
import com.concrete.buildup.global.util.ApplicationContextHolder;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

/**
 * 주민등록번호 컨버터
 *
 * <p>DB에 주민등록번호를 AES-256-GCM으로 암호화하여 저장하고,
 * DB에서 읽어올 때 자동으로 복호화합니다.</p>
 *
 * <p>실제 API 응답 시 마스킹은 ResidentNumMaskingSerializer에서 수행됩니다.</p>
 *
 * <p><b>구현 참고:</b> JPA AttributeConverter는 Hibernate가 직접 인스턴스를 생성하므로
 * Spring의 의존성 주입이 적용되지 않습니다. 따라서 ApplicationContextHolder를 통해
 * Spring Bean에 정적으로 접근합니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Slf4j
@Converter
public class ResidentNumConverter implements AttributeConverter<String, String> {

    /**
     * AesEncryptionUtil Bean 조회 (Lazy 로딩)
     *
     * <p>Hibernate가 Converter를 인스턴스화하는 시점에는 Spring 컨텍스트가
     * 완전히 로드되지 않았을 수 있으므로, 실제 암/복호화가 필요한 시점에
     * Bean을 조회합니다.</p>
     *
     * @return AesEncryptionUtil Bean
     */
    private AesEncryptionUtil getAesEncryptionUtil() {
        return ApplicationContextHolder.getBean(AesEncryptionUtil.class);
    }

    /**
     * Entity -> DB 변환 (암호화하여 저장)
     *
     * @param attribute 평문 주민등록번호
     * @return 암호화된 주민등록번호
     */
    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        try {
            String encrypted = getAesEncryptionUtil().encrypt(attribute);
            log.debug("주민등록번호 암호화 후 DB 저장");
            return encrypted;
        } catch (Exception e) {
            log.error("주민등록번호 암호화 실패: {}", e.getMessage());
            throw new RuntimeException("주민등록번호 암호화 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * DB -> Entity 변환 (복호화하여 반환)
     *
     * @param dbData 암호화된 주민등록번호
     * @return 복호화된 주민등록번호
     */
    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        try {
            String decrypted = getAesEncryptionUtil().decrypt(dbData);
            log.debug("주민등록번호 복호화 후 Entity 반환");
            return decrypted;
        } catch (Exception e) {
            log.error("주민등록번호 복호화 실패: {}", e.getMessage());
            throw new RuntimeException("주민등록번호 복호화 중 오류가 발생했습니다.", e);
        }
    }
}
