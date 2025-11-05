package com.concrete.buildup.domain.auth.repository;

import com.concrete.buildup.domain.auth.entity.Employee;
import com.concrete.buildup.domain.auth.entity.Role;
import com.concrete.buildup.domain.auth.entity.User;
import com.concrete.buildup.global.config.QueryDslConfig;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EmployeeRepository 단위 테스트
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@EnableJpaAuditing
@Import(QueryDslConfig.class) // QueryDSL 설정
@DisplayName("EmployeeRepository 테스트")
class EmployeeRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private EmployeeRepository employeeRepository;

    private User testUser;
    private Role testRole;

    @BeforeEach
    void setUp() {
        // 테스트용 Role 생성
        testRole = Role.builder()
                .roleName("EMPLOYEE")
                .description("근로자 역할")
                .build();
        entityManager.persist(testRole);

        // 테스트용 User 생성
        testUser = User.builder()
                .userId("empuser")
                .password("password")
                .phone("010-1234-5678")
                .role(testRole)
                .build();
        entityManager.persist(testUser);
        entityManager.flush();
    }

    @Test
    @DisplayName("근로자 저장 테스트")
    void saveEmployee() {
        // given
        Employee employee = Employee.builder()
                .user(testUser)
                .empName("홍길동")
                .subPhone("010-9999-8888")
                .empAddress("서울특별시 강남구")
                .empType("PERMANENT")
                .build();

        // when
        Employee savedEmployee = employeeRepository.save(employee);

        // then
        assertThat(savedEmployee.getId()).isNotNull();
        assertThat(savedEmployee.getEmpName()).isEqualTo("홍길동");
        assertThat(savedEmployee.getSubPhone()).isEqualTo("010-9999-8888");
        assertThat(savedEmployee.getEmpType()).isEqualTo("PERMANENT");
        assertThat(savedEmployee.getUser().getUserId()).isEqualTo("empuser");
    }

    @Test
    @DisplayName("User ID로 근로자 조회 테스트")
    void findByUserId() {
        // given
        Employee employee = Employee.builder()
                .user(testUser)
                .empName("김철수")
                .empType("DAILY")
                .build();
        entityManager.persist(employee);
        entityManager.flush();

        // when
        Optional<Employee> foundEmployee = employeeRepository.findByUserId(testUser.getId());

        // then
        assertThat(foundEmployee).isPresent();
        assertThat(foundEmployee.get().getEmpName()).isEqualTo("김철수");
        assertThat(foundEmployee.get().getEmpType()).isEqualTo("DAILY");
    }

    @Test
    @DisplayName("근로자 유형으로 근로자 목록 조회 테스트")
    void findByEmpType() {
        // given
        User user1 = User.builder()
                .userId("user1")
                .password("password")
                .phone("010-1111-1111")
                .build();
        entityManager.persist(user1);

        User user2 = User.builder()
                .userId("user2")
                .password("password")
                .phone("010-2222-2222")
                .build();
        entityManager.persist(user2);

        Employee emp1 = Employee.builder()
                .user(user1)
                .empName("정규직1")
                .empType("PERMANENT")
                .build();
        entityManager.persist(emp1);

        Employee emp2 = Employee.builder()
                .user(user2)
                .empName("정규직2")
                .empType("PERMANENT")
                .build();
        entityManager.persist(emp2);
        entityManager.flush();

        // when
        List<Employee> permanentEmployees = employeeRepository.findByEmpType("PERMANENT");

        // then
        assertThat(permanentEmployees).hasSize(2);
        assertThat(permanentEmployees).extracting("empType")
                .containsOnly("PERMANENT");
    }

    @Test
    @DisplayName("근로자 이름으로 근로자 목록 조회 테스트 (Like 검색)")
    void findByEmpNameContaining() {
        // given
        User user1 = User.builder()
                .userId("user3")
                .password("password")
                .phone("010-3333-3333")
                .build();
        entityManager.persist(user1);

        User user2 = User.builder()
                .userId("user4")
                .password("password")
                .phone("010-4444-4444")
                .build();
        entityManager.persist(user2);

        Employee emp1 = Employee.builder()
                .user(user1)
                .empName("홍길동")
                .empType("DAILY")
                .build();
        entityManager.persist(emp1);

        Employee emp2 = Employee.builder()
                .user(user2)
                .empName("홍진호")
                .empType("DAILY")
                .build();
        entityManager.persist(emp2);
        entityManager.flush();

        // when
        List<Employee> employees = employeeRepository.findByEmpNameContaining("홍");

        // then
        assertThat(employees).hasSize(2);
        assertThat(employees).extracting("empName")
                .contains("홍길동", "홍진호");
    }

    @Test
    @DisplayName("Employee ID로 근로자 + User 조회 테스트 (Fetch Join)")
    void findByIdWithUser() {
        // given
        Employee employee = Employee.builder()
                .user(testUser)
                .empName("이영희")
                .empType("PERMANENT")
                .build();
        Employee savedEmployee = entityManager.persist(employee);
        entityManager.flush();
        entityManager.clear(); // 영속성 컨텍스트 초기화

        // when
        Optional<Employee> foundEmployee = employeeRepository.findByIdWithUser(savedEmployee.getId());

        // then
        assertThat(foundEmployee).isPresent();
        assertThat(foundEmployee.get().getUser()).isNotNull();
        assertThat(foundEmployee.get().getUser().getUserId()).isEqualTo("empuser");
    }

    @Test
    @DisplayName("User ID로 근로자 + User + Role 조회 테스트 (Fetch Join)")
    void findByUserIdWithUserAndRole() {
        // given
        Employee employee = Employee.builder()
                .user(testUser)
                .empName("박철수")
                .empType("DAILY")
                .build();
        entityManager.persist(employee);
        entityManager.flush();
        entityManager.clear(); // 영속성 컨텍스트 초기화

        // when
        Optional<Employee> foundEmployee = employeeRepository.findByUserIdWithUserAndRole(testUser.getId());

        // then
        assertThat(foundEmployee).isPresent();
        assertThat(foundEmployee.get().getUser()).isNotNull();
        assertThat(foundEmployee.get().getUser().getRole()).isNotNull();
        assertThat(foundEmployee.get().getUser().getRole().getRoleName()).isEqualTo("EMPLOYEE");
    }
}
