# Contributing to Build-Up Backend

Build-Up 백엔드 프로젝트에 기여해주셔서 감사합니다! 이 문서는 프로젝트에 기여하는 방법과 개발 워크플로우를 설명합니다.

---

## 📋 목차

1. [시작하기](#시작하기)
2. [개발 워크플로우](#개발-워크플로우)
3. [브랜치 전략](#브랜치-전략)
4. [커밋 컨벤션](#커밋-컨벤션)
5. [코드 스타일](#코드-스타일)
6. [테스트 작성](#테스트-작성)
7. [Pull Request 프로세스](#pull-request-프로세스)
8. [코드 리뷰](#코드-리뷰)
9. [문서화](#문서화)
10. [도움말](#도움말)

---

## 🚀 개발 환경 세팅

### 1. 사전 준비사항

- Java 21 이상
- IntelliJ IDEA (권장) 또는 Eclipse
- Docker Desktop
- MySQL 클라이언트 (선택사항)
- Git

### 2. 프로젝트 클론

```bash
# 프로젝트 클론
git clone <repository-url>
cd BU-Server

# develop 브랜치로 이동 (메인 개발 브랜치)
git checkout develop
```

### 3. 환경변수 설정

```bash
# .env.example을 복사하여 .env 생성
cp .env.example .env

# .env 파일 편집 (필요시)
# 로컬 개발 환경에서는 기본값 사용 가능
```

### 4. 개발 환경 실행

**방법 1: 개발 스크립트 사용 (권장)**

```bash
# 스크립트 실행 권한 부여 (최초 1회)
chmod +x ./start-dev.sh

# 개발 환경 시작
./start-dev.sh
```

**방법 2: IntelliJ에서 직접 실행**

1. IntelliJ에서 프로젝트 열기
2. Active Profile을 `dev`로 설정
3. `BuildupApplication` 실행

### 5. 접속 확인

- 애플리케이션: http://localhost:8080/api
- Swagger UI: http://localhost:8080/api/swagger-ui/index.html
- phpMyAdmin: http://localhost:8081

---

## 🌿 Git 워크플로우

### 브랜치 전략

```
main (또는 master)
  └── develop (메인 개발 브랜치)
        ├── feature/JIRA-123-user-auth (기능 개발)
        ├── feature/JIRA-124-employee-list
        ├── bugfix/JIRA-125-login-error (버그 수정)
        └── hotfix/JIRA-126-critical-fix (긴급 수정)
```

### 브랜치 네이밍 규칙

| 타입 | 형식 | 예시 |
|------|------|------|
| 기능 개발 | `feature/JIRA-번호-간단한설명` | `feature/JIRA-123-user-auth` |
| 버그 수정 | `bugfix/JIRA-번호-간단한설명` | `bugfix/JIRA-125-login-error` |
| 긴급 수정 | `hotfix/JIRA-번호-간단한설명` | `hotfix/JIRA-126-critical-fix` |

### 작업 프로세스

#### 1. Jira Epic/Story 생성
- Jira에서 Epic 또는 Story 생성
- Story를 적절한 크기로 분할 (1-2일 이내 완료 가능한 단위)

#### 2. GitHub Issue 생성
- Jira Story를 기반으로 GitHub Issue 생성
- 제목: `[JIRA-123] 사용자 인증 기능 구현`
- 본문: Jira 링크 및 상세 설명

#### 3. 브랜치 생성 및 개발

```bash
# develop 브랜치에서 최신 코드 받기
git checkout develop
git pull origin develop

# 새 기능 브랜치 생성
git checkout -b feature/JIRA-123-user-auth

# 개발 작업 수행...

# 커밋 (커밋 메시지 규칙 참조)
git add .
git commit -m "feat: [JIRA-123] 사용자 인증 API 구현"

# 원격 브랜치에 푸시
git push origin feature/JIRA-123-user-auth
```

#### 4. Pull Request 생성
- GitHub에서 PR 생성
- PR 템플릿에 따라 작성
- Reviewer 지정 (팀원)

#### 5. 코드 리뷰 및 수정
- 리뷰어는 24시간 이내 리뷰
- 수정 사항 반영 후 재요청

#### 6. 머지 및 브랜치 삭제
- Approve 받은 후 develop에 머지
- Squash and Merge 또는 Rebase and Merge 사용
- 브랜치 삭제

---

## 💻 코딩 컨벤션

### 패키지 구조

Domain 중심 구조를 사용합니다:

```
com.concrete.buildup/
├── domain/
│   └── {domain-name}/
│       ├── controller/
│       ├── service/
│       ├── repository/
│       ├── dto/
│       └── entity/
└── global/
    ├── config/
    ├── exception/
    ├── common/
    └── util/
```

### 네이밍 규칙

#### 클래스
```java
// Controller
public class UserController { }

// Service
public class UserService { }

// Repository
public interface UserRepository extends JpaRepository<User, Long> { }

// DTO
public class UserRequestDto { }
public class UserResponseDto { }

// Entity
@Entity
public class User { }
```

#### 메서드
```java
// CRUD 메서드
public UserResponseDto createUser(UserRequestDto request) { }
public UserResponseDto getUserById(Long id) { }
public List<UserResponseDto> getAllUsers() { }
public UserResponseDto updateUser(Long id, UserRequestDto request) { }
public void deleteUser(Long id) { }

// 조회 메서드
public List<UserResponseDto> findUsersByName(String name) { }
public Optional<User> findUserByEmail(String email) { }

// 비즈니스 로직
public void activateUser(Long userId) { }
public boolean isUserActive(Long userId) { }
```

#### 변수
```java
// camelCase 사용
private String userName;
private Long userId;
private LocalDateTime createdAt;

// boolean은 is/has 접두사
private boolean isActive;
private boolean hasPermission;

// Collection은 복수형
private List<User> users;
private Set<Role> roles;
```

#### 상수
```java
// UPPER_SNAKE_CASE 사용
public static final int MAX_RETRY_COUNT = 3;
public static final String DEFAULT_ROLE = "USER";
```

### API 응답 형식

모든 API는 `ApiResponse`를 사용합니다:

```java
// 성공 응답
// return ResponseEntity.ok(ApiResponse.success(data));
// return ResponseEntity.ok(ApiResponse.success(data, "조회에 성공했습니다"));

// 201 Created
// return ResponseEntity
    // .status(HttpStatus.CREATED)
   // .body(ApiResponse.success(data, "생성되었습니다"));

// 에러 응답 (GlobalExceptionHandler에서 자동 처리)
// throw new BusinessException(ErrorCode.USER_NOT_FOUND);
```

### 어노테이션 순서

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    @GetMapping("/{id}")
    @Operation(summary = "사용자 조회")
    public ResponseEntity<ApiResponse<UserResponseDto>> getUser(
        @PathVariable Long id
    ) {
        // ...
    }
}
```

---

## 📝 커밋 메시지 규칙

### 형식

```
<type>: [JIRA-번호] <subject>

<body> (선택사항)

<footer> (선택사항)
```

### Type 종류

| Type | 설명 | 예시 |
|------|------|------|
| `feat` | 새로운 기능 추가 | `feat: [JIRA-123] 사용자 인증 API 구현` |
| `fix` | 버그 수정 | `fix: [JIRA-125] 로그인 시 NPE 오류 수정` |
| `refactor` | 코드 리팩토링 | `refactor: [JIRA-130] UserService 로직 개선` |
| `style` | 코드 포맷팅, 세미콜론 누락 등 | `style: 코드 포맷팅 적용` |
| `docs` | 문서 수정 | `docs: README 업데이트` |
| `test` | 테스트 코드 추가/수정 | `test: UserService 단위 테스트 추가` |
| `chore` | 빌드, 설정 파일 수정 | `chore: Gradle 의존성 업데이트` |

### 커밋 예시

```bash
# 단일 커밋
git commit -m "feat: [JIRA-123] 사용자 인증 API 구현"

# 상세 설명 포함
git commit -m "feat: [JIRA-123] 사용자 인증 API 구현

- JWT 기반 인증 로직 구현
- UserController에 login, logout API 추가
- UserService에 토큰 생성/검증 로직 추가

Closes #123"
```

---

## 🔍 PR(Pull Request) 가이드

### PR 제목 형식

```
[JIRA-123] 사용자 인증 기능 구현
```

### PR 템플릿

```markdown
## 📌 관련 이슈
- Jira: [JIRA-123](jira-link)
- GitHub Issue: #123

## ✨ 작업 내용
- [ ] JWT 기반 인증 로직 구현
- [ ] UserController 추가
- [ ] UserService 추가
- [ ] 단위 테스트 작성

## 🧪 테스트
- [ ] 단위 테스트 통과
- [ ] 통합 테스트 통과
- [ ] 수동 테스트 완료

## 📸 스크린샷 (필요시)
(Swagger UI 캡처 또는 결과 화면)

## 📝 리뷰 요청 사항
- 인증 로직의 보안 검토 부탁드립니다
- 예외 처리 로직 확인 부탁드립니다

## 🔗 참고 자료
- [Spring Security 공식 문서](https://spring.io/projects/spring-security)
```

### PR 생성 전 체크리스트

- [ ] 코드가 빌드되는지 확인 (`./gradlew build`)
- [ ] 테스트가 통과하는지 확인 (`./gradlew test`)
- [ ] 코드 포맷팅 적용
- [ ] 주석 및 문서 업데이트
- [ ] 불필요한 console.log, 주석 제거
- [ ] .gitignore 확인 (민감 정보 제외)

---

## 👀 코드 리뷰 가이드

### 리뷰어 책임

- PR 생성 후 **24시간 이내** 리뷰
- 건설적인 피드백 제공
- 코드 품질, 로직, 보안, 성능 검토

### 리뷰 시 확인사항

#### 기능
- [ ] 요구사항대로 구현되었는가?
- [ ] 엣지 케이스가 고려되었는가?
- [ ] 에러 핸들링이 적절한가?

#### 코드 품질
- [ ] 코딩 컨벤션을 따르는가?
- [ ] 변수명, 메서드명이 명확한가?
- [ ] 중복 코드가 없는가?
- [ ] SOLID 원칙을 따르는가?

#### 보안
- [ ] SQL Injection 취약점은 없는가?
- [ ] XSS 취약점은 없는가?
- [ ] 민감 정보가 노출되지 않는가?
- [ ] 인증/인가가 적절한가?

#### 성능
- [ ] N+1 쿼리 문제는 없는가?
- [ ] 불필요한 DB 조회는 없는가?
- [ ] 적절한 인덱스가 있는가?

#### 테스트
- [ ] 테스트 커버리지가 충분한가?
- [ ] 테스트 케이스가 적절한가?

### 리뷰 코멘트 예시

```markdown
# 👍 좋은 점
- 예외 처리가 잘 되어 있습니다!

# 💡 제안
- `findUserById` 메서드에서 `orElseThrow` 대신 `Optional`을 반환하는 게 어떨까요?

# ❓ 질문
- 이 로직에서 트랜잭션 범위는 어떻게 되나요?

# ⚠️ 문제
- 이 부분에서 NPE가 발생할 수 있을 것 같습니다.
```

---

## 🧪 테스트 가이드

### 테스트 작성 원칙

- **단위 테스트**: 각 메서드의 기능 검증
- **통합 테스트**: API 엔드포인트 전체 플로우 검증
- **AAA 패턴**: Arrange(준비), Act(실행), Assert(검증)

### 테스트 예시

```java
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("사용자 조회 - 성공")
    void getUserById_Success() throws Exception {
        // Arrange (준비)
        Long userId = 1L;

        // Act & Assert (실행 및 검증)
        mockMvc.perform(get("/api/users/{id}", userId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(userId));
    }

    @Test
    @DisplayName("사용자 조회 - 존재하지 않는 사용자")
    void getUserById_NotFound() throws Exception {
        // Arrange
        Long userId = 999L;

        // Act & Assert
        mockMvc.perform(get("/api/users/{id}", userId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false));
    }
}
```

### 테스트 실행

```bash
# 전체 테스트 실행
./gradlew test

# 특정 클래스만 실행
./gradlew test --tests UserControllerTest

# 테스트 커버리지 리포트 생성
./gradlew jacocoTestReport
```

---

## 📞 문의 및 지원

- **Jira**: [프로젝트 Jira 링크]
- **Notion**: [프로젝트 Notion 페이지]
- **Slack**: #buildup-backend 채널

---

## 📚 참고 자료

- [Spring Boot 공식 문서](https://spring.io/projects/spring-boot)
- [Spring Data JPA 공식 문서](https://spring.io/projects/spring-data-jpa)
- [Effective Java](https://www.oreilly.com/library/view/effective-java/9780134686097/)
- [Clean Code](https://www.oreilly.com/library/view/clean-code-a/9780136083238/)

---

**Happy Coding!** 🚀
