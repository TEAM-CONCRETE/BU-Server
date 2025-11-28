package com.concrete.buildup.global.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * ApplicationContext 정적 접근 유틸리티
 *
 * <p>JPA AttributeConverter와 같이 Hibernate가 직접 인스턴스를 생성하는 클래스에서
 * Spring Bean에 접근해야 할 때 사용합니다.</p>
 *
 * <p>일반적인 상황에서는 생성자 주입을 사용해야 하지만,
 * Hibernate가 직접 인스턴스화하는 경우에는 Spring DI가 적용되지 않으므로
 * 이 유틸리티를 통해 Bean에 접근합니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Component
public class ApplicationContextHolder implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        applicationContext = context;
    }

    /**
     * ApplicationContext 반환
     *
     * @return ApplicationContext
     * @throws IllegalStateException ApplicationContext가 초기화되지 않은 경우
     */
    public static ApplicationContext getApplicationContext() {
        if (applicationContext == null) {
            throw new IllegalStateException(
                "ApplicationContext가 초기화되지 않았습니다. " +
                "Spring 컨텍스트가 완전히 로드된 후에 호출해야 합니다."
            );
        }
        return applicationContext;
    }

    /**
     * Bean 이름으로 Bean 조회
     *
     * @param beanName Bean 이름
     * @return Bean 인스턴스
     */
    public static Object getBean(String beanName) {
        return getApplicationContext().getBean(beanName);
    }

    /**
     * Bean 타입으로 Bean 조회
     *
     * @param beanClass Bean 클래스 타입
     * @param <T> Bean 타입
     * @return Bean 인스턴스
     */
    public static <T> T getBean(Class<T> beanClass) {
        return getApplicationContext().getBean(beanClass);
    }
}

