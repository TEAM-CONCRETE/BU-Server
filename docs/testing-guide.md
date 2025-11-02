# 테스트 전략 및 가이드

## 개요

테스트는 코드 품질을 보장하고 버그를 조기에 발견하는 핵심 활동입니다. Build-Up 프로젝트는 **JUnit 5**, **Mockito**, **AssertJ**를 사용하여 단위 테스트 및 통합 테스트를 작성합니다.

---

## 테스트 원칙

### 1. 테스트 피라미드

```
         ┌─────────────┐
         │   E2E       │  ← 소수 (5%)
         │   Tests     │
      ┌──┴─────────────┴──┐
      │  Integration      │  ← 중간 (20%)
      │  Tests            │
   ┌──┴───────────────────┴──┐
   │    Unit Tests           │  ← 다수 (75%)
   │                         │
   └─────────────────────────┘
```

**Build-Up 테스트 비율:**
- **Unit Tests (75%)**: 빠르고 독립적인 단위 테스트
- **Integration Tests (20%)**: 여러 컴포넌트 통합 테스트
- **E2E Tests (5%)**: 전체 시스템 테스트 (추후 도입)

### 2. 테스트 작성 원칙

- ✅ **FIRST 원칙**
  - **Fast**: 빠르게 실행됨
  - **Independent**: 테스트 간 독립적
  - **Repeatable**: 어디서나 반복 가능
  - **Self-Validating**: 자동으로 성공/실패 판단
  - **Timely**: 프로덕션 코드와 함께 작성

- ✅ **Given-When-Then** 구조 사용
- ✅ **하나의 테스트는 하나의 검증**
- ✅ **테스트 코드도 프로덕션 코드처럼 관리**

### 3. 테스트 커버리지 목표

| 레이어 | 목표 커버리지 | 비고 |
|--------|---------------|------|
| **Service** | 90% 이상 | 비즈니스 로직 핵심 |
| **Controller** | 80% 이상 | API 엔드포인트 |
| **Repository** | 70% 이상 | 복잡한 쿼리만 |
| **Entity** | 50% 이상 | Getter/Setter 제외 |
| **전체** | 80% 이상 | 프로젝트 평균 |

---

## 테스트 도구

### 1. JUnit 5 (Jupiter)

**Spring Boot 3.x 기본 포함**

```gradle
dependencies {
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    // JUnit 5 포함됨
}
```

**주요 어노테이션:**
- `@Test`: 테스트 메서드 표시
- `@BeforeEach`: 각 테스트 실행 전
- `@AfterEach`: 각 테스트 실행 후
- `@BeforeAll`: 모든 테스트 실행 전 (static)
- `@AfterAll`: 모든 테스트 실행 후 (static)
- `@DisplayName`: 테스트 이름 지정
- `@Disabled`: 테스트 비활성화
- `@Nested`: 중첩 테스트 클래스

### 2. Mockito

**Mock 객체 생성 및 Stubbing**

```java
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
```

**주요 메서드:**
- `mock()`: Mock 객체 생성
- `when().thenReturn()`: Stubbing
- `verify()`: 메서드 호출 검증
- `@Mock`: Mock 객체 주입
- `@InjectMocks`: Mock 자동 주입

### 3. AssertJ

**Fluent Assertions**

```java
import static org.assertj.core.api.Assertions.*;
```

**JUnit 기본 assert 대신 사용 (더 읽기 쉬움):**
```java
// JUnit
assertEquals(expected, actual);

// AssertJ
assertThat(actual).isEqualTo(expected);
```

---

## 단위 테스트 (Unit Tests)

### 1. Service Layer 테스트

#### 예시: AuthService 테스트

**파일 위치**: `src/test/java/com/concrete/buildup/domain/auth/service/AuthServiceTest.java`

```java
package com.concrete.buildup.domain.auth.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.concrete.buildup.domain.auth.dto.LoginRequestDto;
import com.concrete.buildup.domain.auth.dto.LoginResponseDto;
import com.concrete.buildup.domain.auth.entity.User;
import com.concrete.buildup.domain.auth.repository.UserRepository;
import com.concrete.buildup.global.exception.AuthenticationException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 테스트")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private LoginRequestDto loginRequest;

    @BeforeEach
    void setUp() {
        // Given: 테스트 데이터 준비
        testUser = User.builder()
            .userId("testuser")
            .password("$2a$10$encoded_password")
            .email("test@example.com")
            .build();

        loginRequest = LoginRequestDto.builder()
            .userId("testuser")
            .password("password123")
            .build();
    }

    @Nested
    @DisplayName("로그인 테스트")
    class LoginTests {

        @Test
        @DisplayName("유효한 인증 정보로 로그인 성공")
        void login_WithValidCredentials_ShouldReturnTokens() {
            // Given
            when(userRepository.findByUserId(anyString()))
                .thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(anyString(), anyString()))
                .thenReturn(true);
            when(jwtTokenProvider.generateAccessToken(any(User.class)))
                .thenReturn("access_token");
            when(jwtTokenProvider.generateRefreshToken(any(User.class)))
                .thenReturn("refresh_token");

            // When
            LoginResponseDto response = authService.login(loginRequest);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("access_token");
            assertThat(response.getRefreshToken()).isEqualTo("refresh_token");

            // Verify
            verify(userRepository, times(1)).findByUserId("testuser");
            verify(passwordEncoder, times(1)).matches("password123", testUser.getPassword());
            verify(jwtTokenProvider, times(1)).generateAccessToken(testUser);
            verify(jwtTokenProvider, times(1)).generateRefreshToken(testUser);
        }

        @Test
        @DisplayName("존재하지 않는 사용자로 로그인 실패")
        void login_WithNonExistentUser_ShouldThrowException() {
            // Given
            when(userRepository.findByUserId(anyString()))
                .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Invalid credentials");

            // Verify
            verify(userRepository, times(1)).findByUserId("testuser");
            verify(passwordEncoder, never()).matches(anyString(), anyString());
        }

        @Test
        @DisplayName("잘못된 비밀번호로 로그인 실패")
        void login_WithInvalidPassword_ShouldThrowException() {
            // Given
            when(userRepository.findByUserId(anyString()))
                .thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(anyString(), anyString()))
                .thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Invalid credentials");

            // Verify
            verify(userRepository, times(1)).findByUserId("testuser");
            verify(passwordEncoder, times(1)).matches("password123", testUser.getPassword());
            verify(jwtTokenProvider, never()).generateAccessToken(any());
        }

        @Test
        @DisplayName("null 입력으로 로그인 실패")
        void login_WithNullInput_ShouldThrowException() {
            // When & Then
            assertThatThrownBy(() -> authService.login(null))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("토큰 재발급 테스트")
    class RefreshTokenTests {

        @Test
        @DisplayName("유효한 Refresh Token으로 재발급 성공")
        void refreshToken_WithValidToken_ShouldReturnNewTokens() {
            // Given
            String refreshToken = "valid_refresh_token";
            when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(true);
            when(jwtTokenProvider.getUserIdFromToken(refreshToken)).thenReturn("testuser");
            when(userRepository.findByUserId("testuser"))
                .thenReturn(Optional.of(testUser));
            when(jwtTokenProvider.generateAccessToken(testUser))
                .thenReturn("new_access_token");
            when(jwtTokenProvider.generateRefreshToken(testUser))
                .thenReturn("new_refresh_token");

            // When
            LoginResponseDto response = authService.refreshToken(refreshToken);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("new_access_token");
            assertThat(response.getRefreshToken()).isEqualTo("new_refresh_token");
        }

        @Test
        @DisplayName("만료된 Refresh Token으로 재발급 실패")
        void refreshToken_WithExpiredToken_ShouldThrowException() {
            // Given
            String expiredToken = "expired_refresh_token";
            when(jwtTokenProvider.validateToken(expiredToken)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> authService.refreshToken(expiredToken))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Invalid or expired refresh token");
        }
    }
}
```

### 2. Repository Layer 테스트

**파일 위치**: `src/test/java/com/concrete/buildup/domain/auth/repository/UserRepositoryTest.java`

```java
package com.concrete.buildup.domain.auth.repository;

import static org.assertj.core.api.Assertions.*;

import com.concrete.buildup.domain.auth.entity.User;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
@DisplayName("UserRepository 테스트")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("사용자 ID로 조회 성공")
    void findByUserId_WithExistingUser_ShouldReturnUser() {
        // Given
        User user = User.builder()
            .userId("testuser")
            .password("password123")
            .email("test@example.com")
            .build();
        entityManager.persist(user);
        entityManager.flush();

        // When
        Optional<User> found = userRepository.findByUserId("testuser");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo("testuser");
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("존재하지 않는 사용자 ID로 조회 실패")
    void findByUserId_WithNonExistentUser_ShouldReturnEmpty() {
        // When
        Optional<User> found = userRepository.findByUserId("nonexistent");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("이메일로 사용자 조회 성공")
    void findByEmail_WithExistingEmail_ShouldReturnUser() {
        // Given
        User user = User.builder()
            .userId("testuser")
            .password("password123")
            .email("test@example.com")
            .build();
        entityManager.persist(user);
        entityManager.flush();

        // When
        Optional<User> found = userRepository.findByEmail("test@example.com");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("사용자 ID 존재 여부 확인")
    void existsByUserId_WithExistingUser_ShouldReturnTrue() {
        // Given
        User user = User.builder()
            .userId("testuser")
            .password("password123")
            .email("test@example.com")
            .build();
        entityManager.persist(user);
        entityManager.flush();

        // When
        boolean exists = userRepository.existsByUserId("testuser");

        // Then
        assertThat(exists).isTrue();
    }
}
```

---

## 통합 테스트 (Integration Tests)

### 1. Controller Layer 테스트

**파일 위치**: `src/test/java/com/concrete/buildup/domain/auth/controller/AuthControllerIntegrationTest.java`

```java
package com.concrete.buildup.domain.auth.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.concrete.buildup.domain.auth.dto.LoginRequestDto;
import com.concrete.buildup.domain.auth.entity.User;
import com.concrete.buildup.domain.auth.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("AuthController 통합 테스트")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Given: 테스트 사용자 생성
        testUser = User.builder()
            .userId("testuser")
            .password(passwordEncoder.encode("password123"))
            .email("test@example.com")
            .build();
        userRepository.save(testUser);
    }

    @Test
    @DisplayName("POST /api/auth/login - 로그인 성공")
    void login_WithValidCredentials_ShouldReturn200() throws Exception {
        // Given
        LoginRequestDto request = LoginRequestDto.builder()
            .userId("testuser")
            .password("password123")
            .build();

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("로그인 성공"))
            .andExpect(jsonPath("$.data.accessToken").exists())
            .andExpect(jsonPath("$.data.refreshToken").exists());
    }

    @Test
    @DisplayName("POST /api/auth/login - 잘못된 비밀번호로 401")
    void login_WithInvalidPassword_ShouldReturn401() throws Exception {
        // Given
        LoginRequestDto request = LoginRequestDto.builder()
            .userId("testuser")
            .password("wrongpassword")
            .build();

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("POST /api/auth/login - 존재하지 않는 사용자로 401")
    void login_WithNonExistentUser_ShouldReturn401() throws Exception {
        // Given
        LoginRequestDto request = LoginRequestDto.builder()
            .userId("nonexistent")
            .password("password123")
            .build();

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/auth/login - Validation 실패 (빈 값)")
    void login_WithEmptyFields_ShouldReturn400() throws Exception {
        // Given
        LoginRequestDto request = LoginRequestDto.builder()
            .userId("")
            .password("")
            .build();

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }
}
```

---

## 테스트 네이밍 컨벤션

### 1. 테스트 클래스명

**형식**: `{ClassName}Test` 또는 `{ClassName}IntegrationTest`

```java
// 단위 테스트
AuthServiceTest
PayrollServiceTest

// 통합 테스트
AuthControllerIntegrationTest
PayrollControllerIntegrationTest
```

### 2. 테스트 메서드명

**형식**: `methodName_WithCondition_ShouldExpectedResult`

```java
// Good
login_WithValidCredentials_ShouldReturnTokens()
login_WithInvalidPassword_ShouldThrowException()
calculatePay_WithOvertimeHours_ShouldIncludeOvertimePay()

// Bad
testLogin()
test1()
loginSuccess()
```

### 3. @DisplayName 활용

```java
@Test
@DisplayName("유효한 인증 정보로 로그인 성공")
void login_WithValidCredentials_ShouldReturnTokens() {
    // ...
}
```

---

## Given-When-Then 패턴

모든 테스트는 Given-When-Then 구조를 따릅니다.

```java
@Test
void testExample() {
    // Given: 테스트 준비 (데이터, Mock 설정)
    User user = User.builder()
        .userId("testuser")
        .build();
    when(userRepository.findByUserId(anyString()))
        .thenReturn(Optional.of(user));

    // When: 테스트 실행 (실제 메서드 호출)
    LoginResponseDto response = authService.login(loginRequest);

    // Then: 결과 검증 (Assertions, Verify)
    assertThat(response).isNotNull();
    assertThat(response.getAccessToken()).isNotEmpty();
    verify(userRepository, times(1)).findByUserId("testuser");
}
```

---

## AssertJ 주요 메서드

### 1. 기본 Assertions

```java
// 동등성
assertThat(actual).isEqualTo(expected);
assertThat(actual).isNotEqualTo(notExpected);

// Null 체크
assertThat(object).isNull();
assertThat(object).isNotNull();

// Boolean
assertThat(condition).isTrue();
assertThat(condition).isFalse();

// 비교
assertThat(number).isGreaterThan(5);
assertThat(number).isLessThan(10);
assertThat(number).isBetween(5, 10);
```

### 2. 문자열 Assertions

```java
assertThat(string).isEmpty();
assertThat(string).isNotEmpty();
assertThat(string).contains("substring");
assertThat(string).startsWith("prefix");
assertThat(string).endsWith("suffix");
assertThat(string).matches("regex");
```

### 3. 컬렉션 Assertions

```java
List<String> list = Arrays.asList("a", "b", "c");

assertThat(list).isEmpty();
assertThat(list).isNotEmpty();
assertThat(list).hasSize(3);
assertThat(list).contains("a", "b");
assertThat(list).containsExactly("a", "b", "c");
assertThat(list).containsExactlyInAnyOrder("c", "b", "a");
assertThat(list).doesNotContain("d");
```

### 4. 예외 Assertions

```java
// 예외 발생 확인
assertThatThrownBy(() -> {
    authService.login(null);
})
    .isInstanceOf(IllegalArgumentException.class)
    .hasMessage("Request cannot be null");

// 예외 발생하지 않음 확인
assertThatNoException().isThrownBy(() -> {
    authService.login(validRequest);
});
```

### 5. 객체 필드 Assertions

```java
User user = userRepository.findByUserId("testuser");

assertThat(user)
    .isNotNull()
    .extracting("userId", "email")
    .containsExactly("testuser", "test@example.com");

// 또는
assertThat(user.getUserId()).isEqualTo("testuser");
assertThat(user.getEmail()).isEqualTo("test@example.com");
```

---

## Mockito 주요 메서드

### 1. Mock 생성

```java
// @Mock 어노테이션 (권장)
@Mock
private UserRepository userRepository;

// 또는 직접 생성
UserRepository userRepository = mock(UserRepository.class);
```

### 2. Stubbing (행동 정의)

```java
// 반환 값 설정
when(userRepository.findByUserId("testuser"))
    .thenReturn(Optional.of(user));

// 예외 발생 설정
when(userRepository.findByUserId("invalid"))
    .thenThrow(new RuntimeException("User not found"));

// 다양한 반환 값
when(service.calculate())
    .thenReturn(10)
    .thenReturn(20)
    .thenReturn(30);

// void 메서드 예외 발생
doThrow(new RuntimeException()).when(service).deleteUser(anyLong());
```

### 3. Argument Matchers

```java
import static org.mockito.ArgumentMatchers.*;

// Any
when(repository.findById(anyLong())).thenReturn(Optional.of(user));
when(service.login(any(LoginRequestDto.class))).thenReturn(response);

// Specific value
when(repository.findById(1L)).thenReturn(Optional.of(user));

// Eq (명시적 동등성)
when(repository.findByUserId(eq("testuser"))).thenReturn(Optional.of(user));
```

### 4. Verify (호출 검증)

```java
// 메서드 호출 검증
verify(userRepository).findByUserId("testuser");

// 호출 횟수 검증
verify(userRepository, times(1)).findByUserId("testuser");
verify(userRepository, never()).deleteById(anyLong());
verify(userRepository, atLeast(1)).findAll();
verify(userRepository, atMost(3)).save(any());

// 호출 순서 검증
InOrder inOrder = inOrder(repository, service);
inOrder.verify(repository).findById(1L);
inOrder.verify(service).processUser(any());
```

---

## 테스트 실행

### 1. Gradle 명령어

```bash
# 모든 테스트 실행
./gradlew test

# 특정 테스트 클래스 실행
./gradlew test --tests AuthServiceTest

# 특정 테스트 메서드 실행
./gradlew test --tests AuthServiceTest.login_WithValidCredentials_ShouldReturnTokens

# 테스트 결과 리포트 확인
open build/reports/tests/test/index.html
```

### 2. IntelliJ IDEA

- 테스트 클래스에서 `Ctrl+Shift+F10` (Windows) / `Cmd+Shift+R` (Mac)
- 개별 메서드 옆 ▶️ 버튼 클릭
- `Run` → `All Tests`

### 3. 테스트 커버리지 측정

```bash
# JaCoCo 리포트 생성
./gradlew test jacocoTestReport

# 리포트 확인
open build/reports/jacoco/test/html/index.html
```

---

## 테스트 모범 사례

### 1. DO (권장)

#### ✅ 명확한 테스트 이름
```java
// Good
@Test
@DisplayName("유효한 인증 정보로 로그인 성공")
void login_WithValidCredentials_ShouldReturnTokens() { }

// Bad
@Test
void test1() { }
```

#### ✅ Given-When-Then 구조
```java
@Test
void testExample() {
    // Given
    User user = createTestUser();

    // When
    Result result = service.process(user);

    // Then
    assertThat(result).isNotNull();
}
```

#### ✅ 하나의 검증만
```java
// Good
@Test
void shouldReturnAccessToken() {
    LoginResponseDto response = authService.login(request);
    assertThat(response.getAccessToken()).isNotEmpty();
}

@Test
void shouldReturnRefreshToken() {
    LoginResponseDto response = authService.login(request);
    assertThat(response.getRefreshToken()).isNotEmpty();
}

// Bad - 여러 검증을 한 테스트에
@Test
void shouldReturnTokens() {
    LoginResponseDto response = authService.login(request);
    assertThat(response.getAccessToken()).isNotEmpty();
    assertThat(response.getRefreshToken()).isNotEmpty();
    assertThat(response.getUserId()).isEqualTo("testuser");
    // ... 너무 많은 검증
}
```

#### ✅ Edge Case 테스트
```java
@Test
void login_WithNullInput_ShouldThrowException() { }

@Test
void calculatePay_WithZeroHours_ShouldReturnZero() { }

@Test
void findEmployees_WithEmptyDatabase_ShouldReturnEmptyList() { }
```

### 2. DON'T (지양)

#### ❌ 테스트 간 의존성
```java
// Bad
static User globalUser;

@Test
void test1() {
    globalUser = createUser();
}

@Test
void test2() {
    // test1에 의존
    assertThat(globalUser).isNotNull();
}
```

#### ❌ 프로덕션 코드 의존
```java
// Bad - 실제 외부 API 호출
@Test
void testExternalApi() {
    String result = apiClient.callRealApi(); // 실제 API 호출
    assertThat(result).isNotEmpty();
}

// Good - Mock 사용
@Test
void testExternalApi() {
    when(apiClient.callApi()).thenReturn("mocked_response");
    String result = service.process();
    assertThat(result).isEqualTo("mocked_response");
}
```

#### ❌ Thread.sleep() 사용
```java
// Bad
@Test
void testAsync() throws InterruptedException {
    asyncService.processAsync();
    Thread.sleep(5000); // 5초 대기
    verify(repository).save(any());
}

// Good - Awaitility 사용 (추가 의존성)
@Test
void testAsync() {
    asyncService.processAsync();
    await().atMost(5, SECONDS)
        .untilAsserted(() -> verify(repository).save(any()));
}
```

---

## 테스트 데이터 관리

### 1. Test Fixtures

```java
public class TestFixtures {

    public static User createTestUser() {
        return User.builder()
            .userId("testuser")
            .password("password123")
            .email("test@example.com")
            .build();
    }

    public static LoginRequestDto createLoginRequest() {
        return LoginRequestDto.builder()
            .userId("testuser")
            .password("password123")
            .build();
    }
}
```

### 2. @BeforeEach 활용

```java
@BeforeEach
void setUp() {
    testUser = TestFixtures.createTestUser();
    loginRequest = TestFixtures.createLoginRequest();
}
```

### 3. Test Builders

```java
public class UserBuilder {
    private String userId = "defaultuser";
    private String password = "defaultpass";
    private String email = "default@example.com";

    public UserBuilder withUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public UserBuilder withPassword(String password) {
        this.password = password;
        return this;
    }

    public User build() {
        return User.builder()
            .userId(userId)
            .password(password)
            .email(email)
            .build();
    }
}

// 사용
User user = new UserBuilder()
    .withUserId("custom")
    .withPassword("custom123")
    .build();
```

---

## 통합 테스트 전략

### 1. @SpringBootTest vs @WebMvcTest

#### @SpringBootTest
- 전체 Spring Context 로드
- 모든 Bean 사용 가능
- 느림
- E2E 테스트에 적합

```java
@SpringBootTest
@AutoConfigureMockMvc
class FullIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
}
```

#### @WebMvcTest
- Controller 레이어만 테스트
- Service, Repository는 Mock
- 빠름
- Controller 단위 테스트에 적합

```java
@WebMvcTest(AuthController.class)
class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService; // Mock으로 주입
}
```

### 2. Test Database

#### H2 In-Memory Database (권장)

**application-test.yml**:
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
```

#### Test Containers (추후 도입)

실제 MySQL 컨테이너 사용:
```java
@Testcontainers
@SpringBootTest
class IntegrationTest {
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");
}
```

---

## CI/CD 통합

### GitHub Actions (예시)

**.github/workflows/test.yml**:
```yaml
name: Run Tests

on:
  push:
    branches: [ develop, main ]
  pull_request:
    branches: [ develop, main ]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Grant execute permission for gradlew
        run: chmod +x gradlew
      - name: Run tests
        run: ./gradlew test
      - name: Generate JaCoCo report
        run: ./gradlew jacocoTestReport
      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v3
```

---

## 참고 자료

- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [AssertJ Documentation](https://assertj.github.io/doc/)
- [Spring Boot Testing](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)