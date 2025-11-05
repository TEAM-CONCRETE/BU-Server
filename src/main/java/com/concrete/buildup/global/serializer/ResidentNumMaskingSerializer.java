package com.concrete.buildup.global.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * 주민등록번호 마스킹 Serializer
 *
 * API 응답 시 주민등록번호를 마스킹 처리합니다.
 * 예: 901234-1234567 -> 901234-1******
 */
@Slf4j
public class ResidentNumMaskingSerializer extends JsonSerializer<String> {

    private static final String HYPHEN = "-";
    private static final String MASKED_CHARS = "******";

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null || value.isEmpty()) {
            gen.writeNull();
            return;
        }

        String maskedValue = maskResidentNum(value);
        gen.writeString(maskedValue);
    }

    /**
     * 주민등록번호 마스킹 처리
     *
     * @param residentNum 주민등록번호 (예: 901234-1234567 또는 9012341234567)
     * @return 마스킹된 주민등록번호 (예: 901234-1******)
     */
    private String maskResidentNum(String residentNum) {
        if (residentNum == null || residentNum.isEmpty()) {
            return residentNum;
        }

        try {
            // 하이픈 제거
            String cleanNum = residentNum.replace(HYPHEN, "");

            // 주민등록번호 길이 체크 (13자리)
            if (cleanNum.length() != 13) {
                log.warn("주민등록번호 형식이 올바르지 않습니다. 길이: {}", cleanNum.length());
                return MASKED_CHARS + HYPHEN + MASKED_CHARS; // 전체 마스킹 반환 (민감 데이터 노출 방지)
            }

            // 앞 6자리 + '-' + 뒤 첫 자리 + '******'
            String front = cleanNum.substring(0, 6);
            String genderDigit = cleanNum.substring(6, 7);

            return front + HYPHEN + genderDigit + MASKED_CHARS;
        } catch (Exception e) {
            log.error("주민등록번호 마스킹 처리 중 오류 발생: {}", e.getMessage());
            return MASKED_CHARS + HYPHEN + MASKED_CHARS; // 전체 마스킹 반환
        }
    }
}
