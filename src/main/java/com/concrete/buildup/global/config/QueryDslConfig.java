package com.concrete.buildup.global.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * QueryDSL 설정 클래스
 *
 * <p>QueryDSL을 사용하기 위한 JPAQueryFactory를 Bean으로 등록합니다.</p>
 *
 * <p>사용 예시:</p>
 * <pre>{@code
 * @RequiredArgsConstructor
 * public class CustomRepositoryImpl {
 *     private final JPAQueryFactory queryFactory;
 *
 *     public List<Contract> findCustom() {
 *         return queryFactory
 *             .selectFrom(contract)
 *             .where(contract.contractState.eq(ContractState.DRAFT))
 *             .fetch();
 *     }
 * }
 * }</pre>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Configuration
public class QueryDslConfig {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * JPAQueryFactory Bean 등록
     *
     * @return JPAQueryFactory 인스턴스
     */
    @Bean
    public JPAQueryFactory jpaQueryFactory() {
        return new JPAQueryFactory(entityManager);
    }
}
