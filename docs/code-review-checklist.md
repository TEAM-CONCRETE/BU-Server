# 코드 리뷰 체크리스트

## 개요

효과적인 코드 리뷰는 코드 품질 향상, 버그 조기 발견, 팀 내 지식 공유에 필수적입니다. 이 문서는 Build-Up 프로젝트의 코드 리뷰 시 확인해야 할 항목들을 정리한 체크리스트입니다.

---

## 코드 리뷰 원칙

### 1. 리뷰어의 자세
- ✅ **건설적인 피드백**: 비판이 아닌 개선 제안
- ✅ **존중과 친절**: "이렇게 하는 게 어떨까요?" 식의 제안
- ✅ **구체적인 근거**: "이 방식은 성능이 더 좋습니다" 등 이유 설명
- ✅ **칭찬도 함께**: 좋은 코드에 대한 긍정적 피드백

### 2. 작성자의 자세
- ✅ **열린 마음**: 피드백을 개선의 기회로 받아들이기
- ✅ **설명과 토론**: 다른 관점이 있다면 정중히 설명
- ✅ **빠른 대응**: 피드백에 신속히 반영 또는 회신
- ✅ **감사 표현**: 리뷰에 대한 감사

### 3. 리뷰 타이밍
- 🕐 **PR 생성 후 4시간 이내** 리뷰 시작
- 🕐 **24시간 이내** 초기 리뷰 완료
- 🕐 **수정 요청 후 12시간 이내** 대응

---

## 체크리스트

### 1. PR 기본 정보 (필수)

- [ ] **Jira 연동**: Jira 이슈 키가 PR에 포함되어 있는가?
- [ ] **PR 제목**: 명확하고 간결한가? (type(scope): subject)
- [ ] **PR 설명**: 변경 내용, 목적, 테스트 방법이 명시되어 있는가?
- [ ] **PR 크기**: 변경 파일 10개 이내, 변경 라인 300줄 이내인가?
  - ❌ 너무 큰 PR은 여러 개로 분할 요청
- [ ] **체크리스트 작성**: PR 템플릿의 체크리스트가 완료되어 있는가?

---

### 2. 코드 품질

#### 2.1 가독성
- [ ] **변수명**: 의미가 명확한가? (약어 지양, 풀네임 사용)
  ```java
  // Bad
  int cnt = 0;
  String usr = "admin";

  // Good
  int employeeCount = 0;
  String userId = "admin";
  ```

- [ ] **메서드명**: 동작을 명확히 나타내는가? (동사 + 명사)
  ```java
  // Bad
  void data() {}

  // Good
  void calculateTotalPay() {}
  List<Employee> findEmployeesByName(String name) {}
  ```

- [ ] **주석**: 복잡한 로직에만 필요한 주석이 있는가?
  - ✅ **Why**를 설명하는 주석 (왜 이렇게 구현했는지)
  - ❌ **What**을 설명하는 주석 (코드가 무엇을 하는지는 코드 자체로 명확해야 함)

- [ ] **매직 넘버**: 하드코딩된 숫자나 문자열을 상수로 분리했는가?
  ```java
  // Bad
  if (status == 1) { ... }

  // Good
  private static final int STATUS_ACTIVE = 1;
  if (status == STATUS_ACTIVE) { ... }
  ```

#### 2.2 구조와 설계
- [ ] **단일 책임 원칙**: 클래스와 메서드가 하나의 책임만 가지는가?
- [ ] **메서드 길이**: 한 메서드가 50줄 이하인가?
  - 긴 메서드는 여러 개로 분리
- [ ] **중복 코드**: DRY 원칙을 준수하는가?
  - 중복 코드는 공통 메서드로 추출
- [ ] **레이어 분리**: Controller-Service-Repository 책임이 명확한가?
  - Controller: 요청/응답 처리만
  - Service: 비즈니스 로직
  - Repository: 데이터 접근만

#### 2.3 에러 처리
- [ ] **예외 처리**: 적절한 예외 처리가 되어 있는가?
  ```java
  // Bad
  try {
      // ...
  } catch (Exception e) {
      e.printStackTrace(); // 운영 환경에서 금지
  }

  // Good
  try {
      // ...
  } catch (DataNotFoundException e) {
      log.error("Employee not found: {}", employeeId, e);
      throw new BusinessException(ErrorCode.EMPLOYEE_NOT_FOUND);
  }
  ```

- [ ] **예외 타입**: 구체적인 예외를 사용하는가?
  - ❌ `Exception`, `RuntimeException` 사용 지양
  - ✅ 커스텀 예외 또는 구체적인 예외 사용

- [ ] **리소스 정리**: try-with-resources 또는 finally 블록 사용
  ```java
  // Good
  try (InputStream is = new FileInputStream(file)) {
      // ...
  } // 자동으로 close() 호출
  ```

---

### 3. 보안

#### 3.1 인증/인가
- [ ] **권한 검증**: API 엔드포인트에 적절한 권한 체크가 있는가?
  ```java
  @PreAuthorize("hasRole('ROLE_MANAGER')")
  @PostMapping("/employees")
  public ResponseEntity<?> createEmployee(...) { ... }
  ```

- [ ] **사용자 검증**: 현재 사용자가 해당 리소스에 접근 권한이 있는가?

#### 3.2 입력 검증
- [ ] **Validation**: DTO에 `@Valid`, `@NotNull` 등 검증 어노테이션 사용
  ```java
  public class LoginRequestDto {
      @NotBlank(message = "사용자 ID는 필수입니다")
      private String userId;

      @NotBlank(message = "비밀번호는 필수입니다")
      @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다")
      private String password;
  }
  ```

- [ ] **SQL Injection**: JPA 사용 시 파라미터 바인딩 확인
  ```java
  // Bad (SQL Injection 위험)
  String sql = "SELECT * FROM users WHERE user_id = '" + userId + "'";

  // Good
  @Query("SELECT u FROM User u WHERE u.userId = :userId")
  Optional<User> findByUserId(@Param("userId") String userId);
  ```

- [ ] **XSS 방지**: HTML 이스케이프 처리 확인
  - Spring Boot는 기본적으로 처리하지만, 직접 HTML 렌더링 시 주의

#### 3.3 민감 정보
- [ ] **로그 보안**: 로그에 비밀번호, API 키 등 민감 정보 포함 안 됨
  ```java
  // Bad
  log.debug("Login attempt: userId={}, password={}", userId, password);

  // Good
  log.debug("Login attempt: userId={}", userId);
  ```

- [ ] **환경변수**: 비밀번호, API 키는 코드에 하드코딩 안 됨
  ```yaml
  # Good
  jwt:
    secret: ${JWT_SECRET}
  ```

---

### 4. 성능

#### 4.1 데이터베이스
- [ ] **N+1 문제**: Lazy Loading으로 인한 N+1 문제가 없는가?
  ```java
  // Bad (N+1 발생)
  List<Contract> contracts = contractRepository.findAll();
  contracts.forEach(c -> c.getEmployee().getName()); // N번의 추가 쿼리

  // Good (Fetch Join 사용)
  @Query("SELECT c FROM Contract c JOIN FETCH c.employee")
  List<Contract> findAllWithEmployee();
  ```

- [ ] **인덱스**: 자주 조회하는 컬럼에 인덱스가 있는가?
  - WHERE, JOIN, ORDER BY에 사용되는 컬럼 확인

- [ ] **쿼리 최적화**: 필요한 컬럼만 조회하는가?
  ```java
  // Bad
  List<Employee> employees = employeeRepository.findAll();

  // Good (DTO Projection)
  @Query("SELECT new com.example.dto.EmployeeDto(e.id, e.name) FROM Employee e")
  List<EmployeeDto> findAllEmployeeDtos();
  ```

#### 4.2 메모리
- [ ] **대용량 데이터**: 페이징 처리를 사용하는가?
  ```java
  // Good
  @GetMapping("/employees")
  public Page<Employee> getEmployees(Pageable pageable) {
      return employeeRepository.findAll(pageable);
  }
  ```

- [ ] **스트림 처리**: 컬렉션 크기가 클 때 Stream API 적절히 사용

---

### 5. 테스트

#### 5.1 테스트 코드
- [ ] **단위 테스트**: 새로운 메서드에 테스트 코드가 있는가?
- [ ] **테스트 커버리지**: 핵심 비즈니스 로직의 테스트 커버리지가 80% 이상인가?
- [ ] **테스트 네이밍**: 테스트 메서드명이 명확한가?
  ```java
  @Test
  void calculateTotalPay_WhenOvertimeHoursIsPositive_ShouldIncludeOvertimePay() {
      // given
      // when
      // then
  }
  ```

- [ ] **Given-When-Then**: 테스트 구조가 명확한가?
- [ ] **경계 값 테스트**: Edge case가 테스트되고 있는가?
  - null, 빈 리스트, 0, 음수, 최대값 등

#### 5.2 테스트 실행
- [ ] **테스트 통과**: 모든 테스트가 통과하는가?
- [ ] **빌드 성공**: `./gradlew clean build`가 성공하는가?

---

### 6. API 설계

#### 6.1 RESTful API
- [ ] **HTTP 메서드**: 적절한 HTTP 메서드를 사용하는가?
  - GET: 조회
  - POST: 생성
  - PUT: 전체 수정
  - PATCH: 부분 수정
  - DELETE: 삭제

- [ ] **URL 설계**: RESTful 원칙을 따르는가?
  ```java
  // Bad
  @GetMapping("/getEmployee")
  @PostMapping("/createEmployee")

  // Good
  @GetMapping("/employees/{id}")
  @PostMapping("/employees")
  ```

- [ ] **상태 코드**: 적절한 HTTP 상태 코드를 사용하는가?
  - 200: 성공
  - 201: 생성 성공
  - 400: 잘못된 요청
  - 401: 인증 실패
  - 403: 권한 없음
  - 404: 리소스 없음
  - 500: 서버 오류

#### 6.2 응답 형식
- [ ] **일관된 응답**: ApiResponse 래퍼를 사용하는가?
  ```json
  {
    "success": true,
    "message": "조회 성공",
    "data": { ... }
  }
  ```

- [ ] **에러 응답**: ErrorResponse 형식을 따르는가?
  ```json
  {
    "success": false,
    "message": "사용자를 찾을 수 없습니다",
    "data": null
  }
  ```

#### 6.3 Swagger 문서
- [ ] **API 문서**: `@Operation`, `@ApiResponse` 주석이 작성되어 있는가?
  ```java
  @Operation(summary = "사용자 로그인", description = "사용자 ID와 비밀번호로 로그인합니다")
  @ApiResponse(responseCode = "200", description = "로그인 성공")
  @ApiResponse(responseCode = "401", description = "인증 실패")
  @PostMapping("/login")
  public ResponseEntity<?> login(@RequestBody LoginRequestDto dto) { ... }
  ```

---

### 7. 데이터베이스

#### 7.1 Flyway 마이그레이션
- [ ] **마이그레이션 파일**: 스키마 변경 시 Flyway 마이그레이션 파일이 있는가?
- [ ] **네이밍**: `V{버전}__{설명}.sql` 형식을 따르는가?
- [ ] **멱등성**: `IF NOT EXISTS` 사용으로 멱등성을 보장하는가?
- [ ] **롤백 계획**: 롤백이 필요한 경우 방법이 있는가?

#### 7.2 엔티티 설계
- [ ] **연관관계**: JPA 연관관계가 올바르게 설정되어 있는가?
  - `@OneToMany`, `@ManyToOne`, `@OneToOne`, `@ManyToMany`
- [ ] **Cascade**: Cascade 옵션이 적절한가?
  - 삭제 cascade는 신중히 사용
- [ ] **FetchType**: Lazy/Eager Loading이 적절한가?
  - 기본적으로 Lazy Loading 사용 권장

---

### 8. 로깅

- [ ] **로그 레벨**: 적절한 로그 레벨을 사용하는가?
  - TRACE: 매우 상세한 정보
  - DEBUG: 개발 시 디버깅 정보
  - INFO: 일반 정보 (운영 환경)
  - WARN: 경고 (잠재적 문제)
  - ERROR: 오류 (예외 발생)

- [ ] **로그 메시지**: 의미 있는 정보를 포함하는가?
  ```java
  // Bad
  log.info("Error");

  // Good
  log.error("Failed to calculate payroll for employee: {}", employeeId, e);
  ```

- [ ] **구조화된 로그**: 파라미터를 사용한 구조화된 로그 작성
  ```java
  // Good
  log.info("User login: userId={}, ip={}", userId, ipAddress);
  ```

---

### 9. Git 및 커밋

- [ ] **Conventional Commits**: 커밋 메시지가 규칙을 따르는가?
  - `feat(auth): add JWT login endpoint`
- [ ] **Jira 참조**: 커밋 메시지에 Jira 키가 포함되어 있는가?
  - `Refs: BU-123`
- [ ] **커밋 크기**: 하나의 논리적 변경만 포함하는가?
- [ ] **브랜치 동기화**: develop 브랜치 최신 코드가 반영되어 있는가?

---

### 10. 문서

- [ ] **README 업데이트**: 새로운 설정이나 환경변수 추가 시 README 업데이트
- [ ] **API 문서**: Swagger 주석 업데이트
- [ ] **CLAUDE.md 업데이트**: 새로운 도메인 추가 시 프로젝트 구조 업데이트

---

## 리뷰 코멘트 작성 가이드

### 좋은 코멘트 예시

#### 제안형
```
💡 이 부분은 Stream API를 사용하면 더 간결해질 것 같습니다.

employees.stream()
    .filter(e -> e.getStatus() == ACTIVE)
    .collect(Collectors.toList());

어떻게 생각하시나요?
```

#### 질문형
```
🤔 이 메서드가 null을 반환할 수 있을까요?
Optional을 사용하는 게 더 안전할 것 같은데, 의도적인 설계인가요?
```

#### 칭찬형
```
👍 예외 처리를 정말 꼼꼼하게 하셨네요! 특히 커스텀 예외를 사용한 점이 좋습니다.
```

#### 중요도 표시
```
❗ [중요] 이 부분은 SQL Injection 취약점이 있습니다.
파라미터 바인딩을 사용해야 합니다.
```

### 나쁜 코멘트 예시

```
❌ "이건 틀렸어요"
✅ "이 부분은 XXX 문제가 있을 것 같습니다. YYY 방식으로 수정하는 게 어떨까요?"

❌ "왜 이렇게 했나요?"
✅ "이 접근 방식의 의도를 여쭤봐도 될까요? XXX 방식도 고려해보셨나요?"

❌ "코드가 읽기 어렵네요"
✅ "메서드명을 더 구체적으로 바꾸면 가독성이 좋아질 것 같습니다. 예: data() → calculateEmployeePayroll()"
```

---

## 리뷰 우선순위

### P0: 즉시 수정 필요 (Merge 차단)
- 보안 취약점 (SQL Injection, XSS 등)
- 데이터 손실 가능성
- 성능 심각한 문제 (N+1, 메모리 누수 등)
- 테스트 실패
- 빌드 실패

### P1: 수정 필요 (Merge 전 해결)
- 비즈니스 로직 오류
- 잘못된 예외 처리
- API 설계 문제
- 테스트 커버리지 부족

### P2: 권장 사항 (Merge 후 개선 가능)
- 코드 가독성 개선
- 리팩토링 제안
- 주석 추가
- 변수명 개선

### P3: 참고 사항 (선택적)
- 코딩 스타일
- 개인적 선호
- 추가 기능 제안

---

## 리뷰 프로세스

### 1. 리뷰 시작
- [ ] PR 제목과 설명 확인
- [ ] 변경 파일 수 확인 (너무 크면 분할 요청)
- [ ] 체크리스트 완료 여부 확인

### 2. 코드 리뷰
- [ ] 위 체크리스트 항목들 확인
- [ ] 우선순위별로 코멘트 작성
- [ ] 좋은 부분에 대한 칭찬 포함

### 3. 리뷰 완료
- [ ] **Approve**: 문제없음 또는 P2/P3만 있음
- [ ] **Request Changes**: P0/P1 이슈가 있음
- [ ] **Comment**: 질문이나 제안만 있음

### 4. 수정 후 재리뷰
- [ ] 수정사항 확인
- [ ] 추가 코멘트 또는 Approve

---

## 자동화 도구 (추후 도입 예정)

### SonarQube
- 코드 품질 분석
- 보안 취약점 검출
- 중복 코드 검출

### CheckStyle
- 코딩 스타일 검증
- Google Java Style 준수 확인

### JaCoCo
- 테스트 커버리지 측정
- 최소 커버리지 기준 설정

---

## 참고 자료

- [Google Code Review Guidelines](https://google.github.io/eng-practices/review/)
- [Effective Code Review](https://www.evoketechnologies.com/blog/code-review-checklist-perform-effective-code-reviews/)
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)