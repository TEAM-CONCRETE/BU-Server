package com.concrete.buildup.domain.auth.repository;

import com.concrete.buildup.domain.auth.entity.Role;
import com.concrete.buildup.global.config.QueryDslConfig;
import com.concrete.buildup.global.util.AesEncryptionUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RoleRepository 단위 테스트
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@EnableJpaAuditing
@Import({QueryDslConfig.class, AesEncryptionUtil.class}) // QueryDSL 설정 및 암호화 유틸리티 포함
@DisplayName("RoleRepository 테스트")
class RoleRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    @DisplayName("역할 저장 테스트")
    void saveRole() {
        // given
        Role role = Role.builder()
                .roleName("EMPLOYEE")
                .description("근로자 역할")
                .build();

        // when
        Role savedRole = roleRepository.save(role);

        // then
        assertThat(savedRole.getId()).isNotNull();
        assertThat(savedRole.getRoleName()).isEqualTo("EMPLOYEE");
        assertThat(savedRole.getDescription()).isEqualTo("근로자 역할");
    }

    @Test
    @DisplayName("역할명으로 역할 조회 테스트")
    void findByRoleName() {
        // given
        Role role = Role.builder()
                .roleName("MANAGER")
                .description("현장 관리자 역할")
                .build();
        entityManager.persist(role);
        entityManager.flush();

        // when
        Optional<Role> foundRole = roleRepository.findByRoleName("MANAGER");

        // then
        assertThat(foundRole).isPresent();
        assertThat(foundRole.get().getRoleName()).isEqualTo("MANAGER");
        assertThat(foundRole.get().getDescription()).isEqualTo("현장 관리자 역할");
    }

    @Test
    @DisplayName("존재하지 않는 역할명 조회 시 빈 Optional 반환")
    void findByRoleName_NotFound() {
        // when
        Optional<Role> foundRole = roleRepository.findByRoleName("NONEXISTENT");

        // then
        assertThat(foundRole).isEmpty();
    }

    @Test
    @DisplayName("역할명 존재 여부 확인 테스트")
    void existsByRoleName() {
        // given
        Role role = Role.builder()
                .roleName("CORPORATION")
                .description("기업 역할")
                .build();
        entityManager.persist(role);
        entityManager.flush();

        // when
        boolean exists = roleRepository.existsByRoleName("CORPORATION");
        boolean notExists = roleRepository.existsByRoleName("NONEXISTENT");

        // then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }
}
