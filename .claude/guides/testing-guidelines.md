# Build-Up 테스트 가이드라인

## 📋 목차

1. [테스트 전략](#테스트-전략)
2. [단위 테스트](#단위-테스트)
3. [통합 테스트](#통합-테스트)
4. [테스트 데이터 관리](#테스트-데이터-관리)
5. [테스트 환경 설정](#테스트-환경-설정)
6. [성능 테스트](#성능-테스트)
7. [테스트 자동화](#테스트-자동화)

---

## 🎯 테스트 전략

### 1. 테스트 피라미드

```
        /\
       /  \
      / E2E \     ← 적은 수, 높은 비용
     /______\
    /        \
   /Integration\  ← 중간 수, 중간 비용
  /____________\
 /              \
/   Unit Tests   \  ← 많은 수, 낮은 비용
/________________\
```

### 2. 테스트 유형별 비율

- **단위 테스트**: 70%
- **통합 테스트**: 20%
- **E2E 테스트**: 10%

### 3. 테스트 원칙

- **FIRST 원칙**
    - **F**ast: 빠르게 실행
    - **I**ndependent: 독립적
    - **R**epeatable: 반복 가능
    - **S**elf-validating: 자체 검증
    - **T**imely: 적시에 작성

---

## 🔬 단위 테스트

### 1. 테스트 클래스 구조

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private EmailService emailService;
    
    @InjectMocks
    private UserService userService;
    
    @Nested
    @DisplayName("사용자 생성 테스트")
    class CreateUserTest {
        
        @Test
        @DisplayName("정상적인 사용자 생성")
        void createUser_Success() {
            // Given
            UserCreateRequest request = UserCreateRequest.builder()
                    .username("testuser")
                    .email("test@example.com")
                    .name("테스트 사용자")
                    .build();
            
            User savedUser = User.builder()
                    .id(1L)
                    .username("testuser")
                    .email("test@example.com")
                    .name("테스트 사용자")
                    .status(UserStatus.ACTIVE)
                    .build();
            
            when(userRepository.existsByUsername("testuser")).thenReturn(false);
            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            
            // When
            User result = userService.createUser(request);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo("testuser");
            assertThat(result.getEmail()).isEqualTo("test@example.com");
            assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);
            
            verify(userRepository).save(any(User.class));
            verify(emailService).sendWelcomeEmail(savedUser);
        }
        
        @Test
        @DisplayName("중복된 사용자명으로 생성 시 예외 발생")
        void createUser_DuplicateUsername_ThrowsException() {
            // Given
            UserCreateRequest request = UserCreateRequest.builder()
                    .username("testuser")
                    .email("test@example.com")
                    .name("테스트 사용자")
                    .build();
            
            when(userRepository.existsByUsername("testuser")).thenReturn(true);
            
            // When & Then
            assertThatThrownBy(() -> userService.createUser(request))
                    .isInstanceOf(DuplicateUserException.class)
                    .hasMessage("사용자명이 이미 존재합니다: testuser");
            
            verify(userRepository, never()).save(any(User.class));
            verify(emailService, never()).sendWelcomeEmail(any(User.class));
        }
    }
    
    @Nested
    @DisplayName("사용자 조회 테스트")
    class GetUserTest {
        
        @Test
        @DisplayName("존재하는 사용자 조회")
        void getUserById_ExistingUser_ReturnsUser() {
            // Given
            User user = User.builder()
                    .id(1L)
                    .username("testuser")
                    .email("test@example.com")
                    .name("테스트 사용자")
                    .status(UserStatus.ACTIVE)
                    .build();
            
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            
            // When
            Optional<User> result = userService.getUserById(1L);
            
            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getUsername()).isEqualTo("testuser");
        }
        
        @Test
        @DisplayName("존재하지 않는 사용자 조회")
        void getUserById_NonExistingUser_ReturnsEmpty() {
            // Given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());
            
            // When
            Optional<User> result = userService.getUserById(999L);
            
            // Then
            assertThat(result).isEmpty();
        }
    }
}
```

### 2. 테스트 메서드 네이밍

```java
// ✅ 좋은 예 - Given-When-Then 패턴
@Test
@DisplayName("사용자명이 중복일 때 DuplicateUserException 발생")
void createUser_WhenUsernameExists_ThrowsDuplicateUserException() {
    // Given
    UserCreateRequest request = createUserRequest("existinguser");
    when(userRepository.existsByUsername("existinguser")).thenReturn(true);
    
    // When & Then
    assertThatThrownBy(() -> userService.createUser(request))
            .isInstanceOf(DuplicateUserException.class);
}

// ❌ 나쁜 예 - 불명확한 네이밍
@Test
void testCreateUser() {
    // 테스트 내용이 불명확
}
```

### 3. Mock 사용 가이드

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private EmailService emailService;
    
    @Spy
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    @InjectMocks
    private UserService userService;
    
    @Test
    void createUser_WithValidData_ReturnsUser() {
        // Given
        UserCreateRequest request = UserCreateRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .name("테스트 사용자")
                .build();
        
        User savedUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .name("테스트 사용자")
                .status(UserStatus.ACTIVE)
                .build();
        
        // Mock 설정
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        doNothing().when(emailService).sendWelcomeEmail(any(User.class));
        
        // When
        User result = userService.createUser(request);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        
        // 검증
        verify(userRepository).existsByUsername("testuser");
        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository).save(any(User.class));
        verify(emailService).sendWelcomeEmail(savedUser);
    }
}
```

---

## 🔗 통합 테스트

### 1. Spring Boot 통합 테스트

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Transactional
class UserControllerIntegrationTest {
    
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private UserRepository userRepository;
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }
    
    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }
    
    @Test
    @DisplayName("사용자 생성 API 테스트")
    void createUser_Success() {
        // Given
        UserCreateRequest request = UserCreateRequest.builder()
                .username("apiuser")
                .email("api@example.com")
                .name("API 사용자")
                .build();
        
        // When
        ResponseEntity<UserResponse> response = restTemplate.postForEntity(
                "/api/v1/users", request, UserResponse.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getUsername()).isEqualTo("apiuser");
        assertThat(response.getBody().getEmail()).isEqualTo("api@example.com");
        
        // 데이터베이스 검증
        List<User> users = userRepository.findAll();
        assertThat(users).hasSize(1);
        assertThat(users.get(0).getUsername()).isEqualTo("apiuser");
    }
    
    @Test
    @DisplayName("사용자 조회 API 테스트")
    void getUser_Success() {
        // Given
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .name("테스트 사용자")
                .status(UserStatus.ACTIVE)
                .build();
        User savedUser = userRepository.save(user);
        
        // When
        ResponseEntity<UserResponse> response = restTemplate.getForEntity(
                "/api/v1/users/" + savedUser.getId(), UserResponse.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getUsername()).isEqualTo("testuser");
    }
    
    @Test
    @DisplayName("존재하지 않는 사용자 조회 시 404 응답")
    void getUser_NotFound_Returns404() {
        // When
        ResponseEntity<ErrorResponse> response = restTemplate.getForEntity(
                "/api/v1/users/999", ErrorResponse.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
```

### 2. Repository 통합 테스트

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class UserRepositoryTest {
    
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private UserRepository userRepository;
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }
    
    @Test
    @DisplayName("사용자명으로 사용자 조회")
    void findByUsername_ExistingUser_ReturnsUser() {
        // Given
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .name("테스트 사용자")
                .status(UserStatus.ACTIVE)
                .build();
        entityManager.persistAndFlush(user);
        
        // When
        Optional<User> result = userRepository.findByUsername("testuser");
        
        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("testuser");
    }
    
    @Test
    @DisplayName("활성 사용자 목록 조회")
    void findActiveUsers_ReturnsActiveUsersOnly() {
        // Given
        User activeUser = User.builder()
                .username("activeuser")
                .email("active@example.com")
                .name("활성 사용자")
                .status(UserStatus.ACTIVE)
                .build();
        
        User inactiveUser = User.builder()
                .username("inactiveuser")
                .email("inactive@example.com")
                .name("비활성 사용자")
                .status(UserStatus.INACTIVE)
                .build();
        
        entityManager.persistAndFlush(activeUser);
        entityManager.persistAndFlush(inactiveUser);
        
        // When
        List<User> result = userRepository.findActiveUsers();
        
        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUsername()).isEqualTo("activeuser");
    }
}
```

---

## 📊 테스트 데이터 관리

### 1. 테스트 데이터 빌더

```java
public class TestDataBuilder {
    
    public static User.UserBuilder userBuilder() {
        return User.builder()
                .username("testuser")
                .email("test@example.com")
                .name("테스트 사용자")
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now());
    }
    
    public static UserCreateRequest.UserCreateRequestBuilder userCreateRequestBuilder() {
        return UserCreateRequest.builder()
                .username("newuser")
                .email("new@example.com")
                .name("새 사용자");
    }
    
    public static UserUpdateRequest.UserUpdateRequestBuilder userUpdateRequestBuilder() {
        return UserUpdateRequest.builder()
                .name("수정된 사용자")
                .bio("수정된 자기소개");
    }
}

// 사용 예시
@Test
void createUser_WithBuilder_ReturnsUser() {
    // Given
    User user = TestDataBuilder.userBuilder()
            .username("customuser")
            .email("custom@example.com")
            .build();
    
    UserCreateRequest request = TestDataBuilder.userCreateRequestBuilder()
            .username("customuser")
            .email("custom@example.com")
            .build();
    
    // When & Then
    // 테스트 로직...
}
```

### 2. 테스트 픽스처

```java
@Component
public class TestFixtures {
    
    public User createActiveUser() {
        return User.builder()
                .username("activeuser")
                .email("active@example.com")
                .name("활성 사용자")
                .status(UserStatus.ACTIVE)
                .build();
    }
    
    public User createInactiveUser() {
        return User.builder()
                .username("inactiveuser")
                .email("inactive@example.com")
                .name("비활성 사용자")
                .status(UserStatus.INACTIVE)
                .build();
    }
    
    public List<User> createMultipleUsers(int count) {
        List<User> users = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            users.add(User.builder()
                    .username("user" + i)
                    .email("user" + i + "@example.com")
                    .name("사용자 " + i)
                    .status(UserStatus.ACTIVE)
                    .build());
        }
        return users;
    }
}
```

### 3. 테스트 데이터베이스 설정

```java
@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = "/test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class UserServiceIntegrationTest {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    void getUserById_WithTestData_ReturnsUser() {
        // Given - test-data.sql에서 데이터 로드됨
        
        // When
        Optional<User> result = userService.getUserById(1L);
        
        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("testuser");
    }
}
```

---

## ⚙️ 테스트 환경 설정

### 1. 테스트 프로파일 설정

```yaml
# application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password: 
    
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
        
  h2:
    console:
      enabled: true

logging:
  level:
    com.agenticcp: DEBUG
    org.springframework.web: DEBUG
    org.hibernate.SQL: DEBUG
```

### 2. TestContainers 설정

```java
@SpringBootTest
@Testcontainers
class IntegrationTest {
    
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("test-schema.sql");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }
}
```

### 3. 테스트 설정 클래스

```java
@TestConfiguration
public class TestConfig {
    
    @Bean
    @Primary
    public PasswordEncoder testPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    @Primary
    public EmailService mockEmailService() {
        return Mockito.mock(EmailService.class);
    }
}
```

---

## 🚀 성능 테스트

### 1. JMH를 이용한 벤치마크 테스트

```java
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
public class UserServiceBenchmark {
    
    private UserService userService;
    private UserRepository userRepository;
    
    @Setup
    public void setup() {
        // 테스트 환경 설정
        userService = new UserService(userRepository);
    }
    
    @Benchmark
    public User createUser() {
        UserCreateRequest request = UserCreateRequest.builder()
                .username("benchmarkuser")
                .email("benchmark@example.com")
                .name("벤치마크 사용자")
                .build();
        
        return userService.createUser(request);
    }
    
    @Benchmark
    public Optional<User> getUserById() {
        return userService.getUserById(1L);
    }
}
```

### 2. 부하 테스트

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserControllerLoadTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    @DisplayName("동시 사용자 생성 부하 테스트")
    void createUser_LoadTest() throws InterruptedException {
        int threadCount = 10;
        int requestsPerThread = 100;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < requestsPerThread; j++) {
                        UserCreateRequest request = UserCreateRequest.builder()
                                .username("user" + threadId + "_" + j)
                                .email("user" + threadId + "_" + j + "@example.com")
                                .name("사용자 " + threadId + "_" + j)
                                .build();
                        
                        ResponseEntity<UserResponse> response = restTemplate.postForEntity(
                                "/api/v1/users", request, UserResponse.class);
                        
                        if (response.getStatusCode() == HttpStatus.CREATED) {
                            successCount.incrementAndGet();
                        } else {
                            errorCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        
        assertThat(successCount.get()).isEqualTo(threadCount * requestsPerThread);
        assertThat(errorCount.get()).isEqualTo(0);
    }
}
```

---

## 🤖 테스트 자동화

### 1. Gradle 설정 (build.gradle)

```groovy
plugins {
        id 'java'
        id 'jacoco'
        }
        
test {
    useJUnitPlatform()
    finalizedBy jacocoTestReport
}

jacocoTestReport {
    dependsOn test
    reports {
        xml.required = true
        html.required = true
    }
}
```

### 2. GitHub Actions 설정

```yaml
name: CI Test

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest

    services:
      mysql:
        image: mysql:8.0
        env:
          MYSQL_ROOT_PASSWORD: root
          MYSQL_DATABASE: testdb
        ports:
          - 3306:3306
        options: --health-cmd="mysqladmin ping" --health-interval=10s --health-retries=3

    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
      - name: Cache Gradle
        uses: actions/cache@v3
        with:
          path: ~/.gradle
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*') }}
      - name: Run tests
        run: ./gradlew clean test jacocoTestReport
```

### 3. 테스트 커버리지 설정

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.8</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

---

## 📋 테스트 체크리스트

### 단위 테스트 작성 시

- [ ] 테스트 메서드명이 명확한가?
- [ ] Given-When-Then 패턴을 따르는가?
- [ ] 모든 분기를 테스트하는가?
- [ ] 예외 상황을 테스트하는가?
- [ ] Mock을 적절히 사용하는가?
- [ ] 테스트가 독립적인가?

### 통합 테스트 작성 시

- [ ] 실제 데이터베이스를 사용하는가?
- [ ] 테스트 데이터를 적절히 관리하는가?
- [ ] 테스트 후 정리가 되는가?
- [ ] API 엔드포인트를 테스트하는가?
- [ ] 다양한 시나리오를 테스트하는가?

### 테스트 실행 시

- [ ] 모든 테스트가 통과하는가?
- [ ] 테스트 실행 시간이 적절한가?
- [ ] 테스트 커버리지가 충분한가?
- [ ] CI/CD 파이프라인에서 실행되는가?

---

이 테스트 가이드라인을 준수하여 안정적이고 신뢰할 수 있는 테스트를 작성하시기 바랍니다.
