# Claude Code 협업 가이드

## 개요

Claude Code는 AI 페어 프로그래밍 도구로, Build-Up 백엔드 개발을 효율적으로 지원합니다. 이 가이드는 Claude Code를 효과적으로 활용하는 방법을 설명합니다.

---

## Claude Code란?

**Claude Code**는 Anthropic의 Claude AI를 기반으로 한 코드 작성 도우미입니다.

### 주요 기능
- 코드 생성 및 리팩토링
- 버그 수정 및 디버깅
- 테스트 코드 작성
- 문서 작성
- 코드 리뷰 및 분석

---

## 효과적인 프롬프팅

### 1. 명확하고 구체적으로 요청

#### ❌ 나쁜 예시
```
"로그인 기능 만들어줘"
```

#### ✅ 좋은 예시
```
"JWT 기반 사용자 로그인 API를 구현해줘.

요구사항:
1. POST /api/auth/login 엔드포인트
2. 요청: { userId: string, password: string }
3. 응답: { accessToken: string, refreshToken: string }
4. Access Token 1시간, Refresh Token 7일 유효
5. BCrypt로 비밀번호 검증
6. Spring Security 사용
7. AuthController, AuthService, UserRepository 레이어 분리
8. 단위 테스트 포함

참고:
- Entity: User (이미 존재)
- 프로젝트 구조: domain 중심 (com.concrete.buildup.domain.auth)
- 응답 형식: ApiResponse 래퍼 사용
"
```

### 2. 컨텍스트 제공

#### 프로젝트 정보 공유
```
"Build-Up 프로젝트에서 급여 계산 기능을 추가하려고 해.

현재 상황:
- Employee 엔티티 존재 (hourlyRate 필드 있음)
- Attendance 엔티티로 근태 관리 중
- PayrollService를 새로 만들어야 함

요구사항:
- 기본급 + 초과근무 수당(1.5배) 계산
- 급여 명세서 PDF 생성
- PayrollController, PayrollService, PayrollRepository 생성
"
```

### 3. 단계별 작업 요청

#### 큰 작업은 분할
```
"사용자 관리 시스템을 구현하려고 해.

먼저 1단계부터 시작:
1단계: User 엔티티 및 UserRepository 작성
2단계: UserService 비즈니스 로직 구현
3단계: UserController API 엔드포인트 작성
4단계: 단위 테스트 작성

지금은 1단계만 진행해줘.
"
```

---

## 실전 시나리오

### 시나리오 1: 새로운 기능 개발

**상황**: Jira Story BU-101 (JWT 로그인) 구현

```
"BU-101 Story를 구현하려고 해.

Story 내용:
- JWT 기반 사용자 로그인 API 구현
- POST /api/auth/login
- BCrypt 비밀번호 검증
- Access Token + Refresh Token 발급

프로젝트 정보:
- Spring Boot 3.5.7, Java 21
- 패키지: com.concrete.buildup.domain.auth
- User 엔티티 이미 존재 (userId, password, email 필드)
- JWT Secret은 application-dev.yml에 설정됨

단계별로 진행해줘:
1. JwtTokenProvider 유틸리티 클래스
2. LoginRequestDto, LoginResponseDto
3. AuthService.login() 메서드
4. AuthController POST /api/auth/login
5. 단위 테스트 (AuthServiceTest)

지금은 1단계부터 시작해줘.
"
```

### 시나리오 2: 버그 수정

```
"급여 계산에서 버그가 발생했어.

증상:
- 초과근무 시간이 음수일 때 급여가 이상하게 계산됨
- PayrollService.calculateTotalPay() 메서드 문제로 추정

코드 위치:
- com.concrete.buildup.domain.payroll.service.PayrollService
- calculateTotalPay(Employee employee, int workedHours) 메서드

요청:
1. 버그 원인 분석
2. 수정 방법 제안
3. 테스트 케이스 추가 (음수 시간 처리)
"
```

### 시나리오 3: 테스트 코드 작성

```
"AuthService에 대한 단위 테스트를 작성해줘.

테스트 대상:
- AuthService.login() 메서드

테스트 케이스:
1. 유효한 인증 정보로 로그인 성공
2. 존재하지 않는 사용자로 로그인 실패
3. 잘못된 비밀번호로 로그인 실패
4. null 입력으로 로그인 실패

요구사항:
- JUnit 5, Mockito, AssertJ 사용
- Given-When-Then 구조
- @DisplayName으로 테스트 설명
- Mock: UserRepository, PasswordEncoder, JwtTokenProvider
"
```

### 시나리오 4: 코드 리팩토링

```
"UserService의 중복 코드를 리팩토링해줘.

문제:
- createUser(), updateUser() 메서드에 검증 로직 중복
- 이메일, 전화번호 검증이 반복됨

요청:
1. 공통 검증 로직을 별도 메서드로 추출
2. Validator 패턴 적용 고려
3. 기존 테스트는 모두 통과해야 함
"
```

---

## 도메인별 개발 가이드

### 1. 새 도메인 추가

```
"payroll 도메인을 새로 추가하려고 해.

요구사항:
- 패키지: com.concrete.buildup.domain.payroll
- 레이어: entity, repository, dto, service, controller
- Entity: Payroll (급여 정보)
- 주요 기능: 급여 계산, 명세서 생성, 급여 목록 조회

Entity 필드:
- payrollId (PK)
- employeeId (FK)
- baseSalary (기본급)
- overtimePay (초과근무 수당)
- totalPay (총 급여)
- paymentDate (지급일)
- createdAt, updatedAt

먼저 Entity와 Repository부터 만들어줘.
"
```

### 2. API 엔드포인트 추가

```
"PayrollController에 급여 목록 조회 API를 추가해줘.

요구사항:
- GET /api/payrolls
- 페이징 지원 (Pageable)
- 필터: employeeId, 시작일, 종료일
- 응답: ApiResponse<Page<PayrollResponseDto>>
- Swagger 문서 (@Operation, @ApiResponse)
- 인증 필요 (@PreAuthorize("hasRole('ROLE_MANAGER')"))

참고:
- PayrollService.findPayrolls() 메서드는 이미 존재
- PayrollResponseDto도 이미 존재
"
```

---

## 토큰 절약 팁

### 1. 파일 컨텍스트 최소화

```
# 필요한 파일만 명시
"AuthService.java 파일을 읽고 login() 메서드만 수정해줘."

# 전체 프로젝트 스캔 지양
"프로젝트 전체에서 User를 찾아줘" ❌
"com.concrete.buildup.domain.auth.entity.User 클래스를 읽어줘" ✅
```

### 2. 반복 요청 최소화

```
# 한 번에 요청
"다음 3가지를 순서대로 해줘:
1. AuthService에 login() 추가
2. AuthController에 POST /api/auth/login 추가
3. AuthServiceTest 작성"

# 여러 번 나눠서 요청 지양
"AuthService에 login() 추가해줘"
"이제 AuthController 만들어줘"
"테스트도 만들어줘"
```

### 3. 코드 블록 지정

```
"AuthService.java의 login() 메서드만 수정해줘.
다른 메서드는 건드리지 마."
```

---

## 베스트 프랙티스

### DO (권장)

✅ **명확한 요구사항**
```
"POST /api/auth/login API를 구현해줘.
- 요청: { userId, password }
- 응답: { accessToken, refreshToken }
- BCrypt 비밀번호 검증
- JWT 토큰 생성 (1시간 유효)"
```

✅ **단계별 진행**
```
"큰 작업이니 단계별로 진행하자.
1단계: Entity 작성
2단계: Repository 작성
3단계: Service 작성
지금은 1단계만."
```

✅ **기존 코드 참고**
```
"EmployeeController를 참고해서 PayrollController를 만들어줘.
비슷한 구조로."
```

✅ **테스트 요청**
```
"테스트 코드도 함께 작성해줘.
- JUnit 5
- Mockito
- Given-When-Then 구조"
```

### DON'T (지양)

❌ **모호한 요청**
```
"로그인 만들어줘" (너무 추상적)
```

❌ **한 번에 너무 많은 작업**
```
"사용자 관리 시스템 전체를 만들어줘" (범위가 너무 큼)
```

❌ **컨텍스트 없는 요청**
```
"이 코드 수정해줘" (어떤 코드인지 불명확)
```

❌ **프로젝트 전체 스캔**
```
"프로젝트에서 User 관련 파일을 모두 찾아줘" (토큰 낭비)
```

---

## 자주 묻는 질문 (FAQ)

### Q1: Claude Code가 잘못된 코드를 생성했어요
**A**: 구체적으로 피드백을 주세요.
```
"방금 생성한 AuthService.login()에서 문제가 있어.
- 문제: NPE 발생 가능성
- 위치: user가 null일 때
- 요청: Optional.orElseThrow() 사용해서 수정해줘"
```

### Q2: 여러 파일을 한 번에 생성하고 싶어요
**A**: 순서를 명시하세요.
```
"다음 파일들을 순서대로 생성해줘:
1. Payroll.java (Entity)
2. PayrollRepository.java
3. PayrollRequestDto.java, PayrollResponseDto.java
4. PayrollService.java
5. PayrollController.java"
```

### Q3: 기존 코드를 수정해야 하는데 전체를 읽어야 하나요?
**A**: 필요한 부분만 지정하세요.
```
"AuthService.java의 login() 메서드만 읽고,
예외 처리 로직을 개선해줘."
```

---

## 체크리스트

### Claude Code 요청 전

- [ ] 요구사항이 명확한가?
- [ ] 필요한 컨텍스트를 제공했는가?
- [ ] 파일 경로나 클래스명을 명시했는가?
- [ ] 작업 범위가 적절한가? (너무 크지 않은가?)

### 코드 생성 후

- [ ] 생성된 코드가 요구사항을 충족하는가?
- [ ] 코딩 컨벤션을 따르는가?
- [ ] 테스트 코드가 포함되었는가?
- [ ] 빌드와 테스트가 통과하는가?

---

## 참고 자료

- Claude Code 공식 문서
- [프로젝트 README.md](../README.md)
- [CONTRIBUTING.md](../CONTRIBUTING.md)
- [docs/](./): 프로젝트 가이드 문서들

---

**Claude Code와 함께 효율적인 개발을!** 🤖✨