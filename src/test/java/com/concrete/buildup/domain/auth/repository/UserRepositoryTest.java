package com.concrete.buildup.domain.auth.repository;

import com.concrete.buildup.domain.auth.entity.Role;
import com.concrete.buildup.domain.auth.entity.User;
import com.concrete.buildup.global.config.QueryDslConfig;
import com.concrete.buildup.global.util.AesEncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
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
 * UserRepository 단위 테스트
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@EnableJpaAuditing
@Import({QueryDslConfig.class, AesEncryptionUtil.class}) // QueryDSL 설정 및 암호화 유틸리티 포함
@DisplayName("UserRepository 테스트")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private Role testRole;

    @BeforeEach
    void setUp() {
        // 테스트용 Role 생성
        testRole = Role.builder()
                .roleName("EMPLOYEE")
                .description("근로자 역할")
                .build();
        entityManager.persist(testRole);
        entityManager.flush();
    }

    @Test
    @DisplayName("사용자 저장 테스트")
    void saveUser() {
        // given
        User user = User.builder()
                .userId("testuser")
                .password("encodedPassword123")
                .phone("010-1234-5678")
                .email("test@example.com")
                .role(testRole)
                .build();

        // when
        User savedUser = userRepository.save(user);

        // then
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getUserId()).isEqualTo("testuser");
        assertThat(savedUser.getPhone()).isEqualTo("010-1234-5678");
        assertThat(savedUser.getEmail()).isEqualTo("test@example.com");
        assertThat(savedUser.getRole().getRoleName()).isEqualTo("EMPLOYEE");
    }

    @Test
    @DisplayName("로그인 ID로 사용자 조회 테스트")
    void findByUserId() {
        // given
        User user = User.builder()
                .userId("testuser123")
                .password("password")
                .phone("010-1111-2222")
                .build();
        entityManager.persist(user);
        entityManager.flush();

        // when
        Optional<User> foundUser = userRepository.findByUserId("testuser123");

        // then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUserId()).isEqualTo("testuser123");
        assertThat(foundUser.get().getPhone()).isEqualTo("010-1111-2222");
    }

    @Test
    @DisplayName("존재하지 않는 로그인 ID 조회 시 빈 Optional 반환")
    void findByUserId_NotFound() {
        // when
        Optional<User> foundUser = userRepository.findByUserId("nonexistent");

        // then
        assertThat(foundUser).isEmpty();
    }

    @Test
    @DisplayName("로그인 ID 존재 여부 확인 테스트")
    void existsByUserId() {
        // given
        User user = User.builder()
                .userId("existinguser")
                .password("password")
                .phone("010-3333-4444")
                .build();
        entityManager.persist(user);
        entityManager.flush();

        // when
        boolean exists = userRepository.existsByUserId("existinguser");
        boolean notExists = userRepository.existsByUserId("nonexistent");

        // then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("로그인 ID로 사용자 + 역할 조회 테스트 (Fetch Join)")
    void findByUserIdWithRole() {
        // given
        User user = User.builder()
                .userId("userwithrole")
                .password("password")
                .phone("010-5555-6666")
                .role(testRole)
                .build();
        entityManager.persist(user);
        entityManager.flush();
        entityManager.clear(); // 영속성 컨텍스트 초기화

        // when
        Optional<User> foundUser = userRepository.findByUserIdWithRole("userwithrole");

        // then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getRole()).isNotNull();
        assertThat(foundUser.get().getRole().getRoleName()).isEqualTo("EMPLOYEE");
    }

    @Test
    @DisplayName("ID로 사용자 + 역할 조회 테스트 (Fetch Join)")
    void findByIdWithRole() {
        // given
        User user = User.builder()
                .userId("useridtest")
                .password("password")
                .phone("010-7777-8888")
                .role(testRole)
                .build();
        User savedUser = entityManager.persist(user);
        entityManager.flush();
        entityManager.clear(); // 영속성 컨텍스트 초기화

        // when
        Optional<User> foundUser = userRepository.findByIdWithRole(savedUser.getId());

        // then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getRole()).isNotNull();
        assertThat(foundUser.get().getRole().getRoleName()).isEqualTo("EMPLOYEE");
    }

    @Test
    @DisplayName("사용자 정보 수정 테스트")
    void updateUserInfo() {
        // given
        User user = User.builder()
                .userId("updatetest")
                .password("password")
                .phone("010-1111-1111")
                .email("old@example.com")
                .build();
        User savedUser = entityManager.persist(user);
        entityManager.flush();

        // when
        savedUser.updateInfo("010-9999-9999", "new@example.com");
        entityManager.flush();
        entityManager.clear();

        // then
        Optional<User> updatedUser = userRepository.findByUserId("updatetest");
        assertThat(updatedUser).isPresent();
        assertThat(updatedUser.get().getPhone()).isEqualTo("010-9999-9999");
        assertThat(updatedUser.get().getEmail()).isEqualTo("new@example.com");
    }

    @Test
    @DisplayName("사용자 역할 할당 테스트")
    void assignRole() {
        // given
        User user = User.builder()
                .userId("roletest")
                .password("password")
                .phone("010-2222-2222")
                .build();
        User savedUser = entityManager.persist(user);
        entityManager.flush();

        // when
        savedUser.assignRole(testRole);
        entityManager.flush();
        entityManager.clear();

        // then
        Optional<User> updatedUser = userRepository.findByUserId("roletest");
        assertThat(updatedUser).isPresent();
        assertThat(updatedUser.get().getRole()).isNotNull();
        assertThat(updatedUser.get().getRole().getRoleName()).isEqualTo("EMPLOYEE");
    }
}
