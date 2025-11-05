package com.concrete.buildup.global.converter;

import com.concrete.buildup.global.util.AesEncryptionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * JPA Converter에 Spring Bean을 주입하기 위한 헬퍼 클래스
 *
 * JPA Converter는 JPA가 직접 인스턴스화하므로 Spring의 의존성 주입을 받을 수 없습니다.
 * 이 클래스는 애플리케이션 시작 시 static 필드에 Bean을 주입하여 Converter에서 사용할 수 있도록 합니다.
 */
@Component
@RequiredArgsConstructor
public class ConverterInjector {

    private final AesEncryptionUtil aesEncryptionUtil;

    @PostConstruct
    public void init() {
        ResidentNumConverter.setAesEncryptionUtil(aesEncryptionUtil);
    }
}
