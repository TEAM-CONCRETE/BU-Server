# Commit Message 컨벤션

## 개요

Build-Up 프로젝트는 **Conventional Commits** 규칙을 따릅니다. 명확하고 일관된 커밋 메시지는 코드 히스토리를 이해하기 쉽게 만들고, 자동화된 도구(릴리즈 노트, 체인지로그)와의 통합을 가능하게 합니다.

---

## 커밋 메시지 기본 형식

```
<type>(<scope>): <subject>

<body>

<footer>
```

### 구성 요소

#### 1. **Type** (필수)
커밋의 종류를 나타냅니다.

| Type | 설명 | 예시 |
|------|------|------|
| `feat` | 새로운 기능 추가 | 로그인 API 구현 |
| `fix` | 버그 수정 | NPE 예외 처리 |
| `docs` | 문서 수정 | README 업데이트 |
| `style` | 코드 포맷팅 (기능 변경 없음) | 들여쓰기, 세미콜론 추가 |
| `refactor` | 코드 리팩토링 (기능 변경 없음) | 중복 코드 제거 |
| `test` | 테스트 코드 추가/수정 | 단위 테스트 작성 |
| `chore` | 빌드, 설정 변경 | Gradle 의존성 업데이트 |
| `perf` | 성능 개선 | 쿼리 최적화 |
| `ci` | CI/CD 설정 변경 | GitHub Actions 추가 |
| `revert` | 이전 커밋 되돌리기 | "Revert feat: add login" |

#### 2. **Scope** (선택사항)
커밋이 영향을 미치는 범위 (도메인, 모듈, 파일 등)

```
feat(auth): implement JWT token generation
fix(payroll): correct overtime calculation
docs(readme): update installation guide
test(employee): add service layer unit tests
```

**Build-Up 프로젝트의 주요 Scope:**
- `auth` - 인증/인가
- `employee` - 사원 관리
- `site` - 현장 관리
- `contract` - 계약 관리
- `payroll` - 급여 관리
- `attendance` - 근태 관리
- `workreport` - 작업일보
- `safetydoc` - 안전교육일지
- `config` - 설정 파일
- `deps` - 의존성 관리

#### 3. **Subject** (필수)
커밋의 간단한 설명 (50자 이내)

**규칙:**
- 명령형 현재 시제 사용 ("add" not "added" or "adds")
- 첫 글자 소문자
- 마침표(`.`) 사용 안 함
- 한글 또는 영어 사용 가능

**좋은 예시:**
```
feat(auth): implement JWT-based login
fix(payroll): correct salary calculation logic
docs: update API documentation
```

**나쁜 예시:**
```
feat(auth): Implemented JWT login.     # 과거형, 대문자, 마침표
fix: bug fix                            # 모호함
Updated files                           # type 누락
```

#### 4. **Body** (선택사항)
커밋의 상세한 설명 (72자 줄바꿈)

**포함 내용:**
- 변경한 이유 (What & Why)
- 변경 전/후 비교
- 주의사항이나 부작용

**예시:**
```
feat(auth): implement JWT-based authentication

- Add JwtTokenProvider for token generation and validation
- Implement login endpoint with access/refresh token
- Add token refresh endpoint for session extension

The previous session-based auth was stateless-unfriendly.
JWT allows for better scalability and mobile app support.
```

#### 5. **Footer** (선택사항)
이슈 트래커 참조, Breaking Changes 표시

**형식:**
- `Refs: <Jira-Key>` - Jira 이슈 참조
- `Closes: <Jira-Key>` - Jira 이슈 종료
- `BREAKING CHANGE:` - 호환성 깨지는 변경

**예시:**
```
feat(api): change response format to standardized structure

BREAKING CHANGE: All API responses now use ApiResponse wrapper.
Clients must update response parsing logic.

Refs: BU-123
```

---

## 커밋 메시지 예시

### 예시 1: 간단한 기능 추가
```
feat(auth): add user registration endpoint
```

### 예시 2: Scope와 Body 포함
```
feat(employee): implement employee search functionality

- Add search by name, phone, position
- Support pagination and sorting
- Return EmployeeResponseDto with basic info

Refs: BU-234
```

### 예시 3: 버그 수정
```
fix(payroll): prevent negative salary values

Validation was missing for hourly_rate and hours_worked.
Added @Positive constraint and service-level validation.

Closes: BU-456
```

### 예시 4: 리팩토링
```
refactor(service): extract common logic to base service

- Create BaseService with shared utility methods
- Remove duplicate code from 5 service classes
- Improve testability and maintainability

Refs: BU-567
```

### 예시 5: 문서 업데이트
```
docs: update README with Docker setup instructions
```

### 예시 6: 테스트 추가
```
test(contract): add unit tests for contract service

- Test contract creation with valid/invalid data
- Test contract status transitions
- Achieve 90% coverage for ContractService

Refs: BU-678
```

### 예시 7: 의존성 업데이트
```
chore(deps): upgrade Spring Boot to 3.5.7

- Update Spring Boot from 3.5.0 to 3.5.7
- Update related dependencies
- Fix deprecated API usage

Refs: BU-789
```

### 예시 8: 성능 개선
```
perf(attendance): optimize attendance query with index

Added composite index on (employee_id, attendance_date)
to improve query performance from 2s to 50ms.

Refs: BU-890
```

### 예시 9: Breaking Change
```
feat(api)!: standardize error response format

BREAKING CHANGE: Error responses now use ErrorResponse DTO
with 'success', 'message', 'data' fields. Clients must
update error handling logic.

Before:
{
  "error": "Invalid request",
  "status": 400
}

After:
{
  "success": false,
  "message": "Invalid request",
  "data": null
}

Refs: BU-901
```

### 예시 10: Revert
```
revert: "feat(auth): implement OAuth2 login"

This reverts commit abc123def456.
OAuth2 integration is postponed to v2.0.

Refs: BU-912
```

---

## 작성 규칙

### DO (권장)
- ✅ **영어 또는 한글 일관성 유지** (팀 내 합의 필요)
- ✅ **명령형 현재 시제** ("add" not "added")
- ✅ **Subject 50자 이내**
- ✅ **Body 72자 줄바꿈**
- ✅ **Type과 Subject는 필수**
- ✅ **Jira 키 반드시 포함** (Footer에 `Refs: BU-XXX`)
- ✅ **의미 있는 단위로 커밋** (빌드 가능한 상태)
- ✅ **하나의 논리적 변경만 포함**

### DON'T (지양)
- ❌ **"WIP", "temp", "fix bug" 같은 모호한 메시지**
- ❌ **여러 기능을 한 커밋에 포함**
- ❌ **Subject에 마침표(.) 사용**
- ❌ **과거형 사용** ("added", "fixed")
- ❌ **너무 긴 Subject** (50자 초과)
- ❌ **Jira 키 누락**

---

## 실전 예시 (개발 시나리오)

### 시나리오 1: JWT 로그인 기능 구현

**작업 내용:**
1. JwtTokenProvider.java 작성
2. AuthService.login() 메서드 구현
3. AuthController POST /api/auth/login 엔드포인트 추가
4. LoginRequestDto, LoginResponseDto 작성

**커밋 메시지:**
```
feat(auth): implement JWT-based user login

- Add JwtTokenProvider for token generation and validation
- Implement AuthService.login() with user authentication
- Add POST /api/auth/login endpoint
- Create LoginRequestDto and LoginResponseDto

Access token expires in 1 hour, refresh token in 7 days.
Passwords are validated using BCryptPasswordEncoder.

Refs: BU-101
```

### 시나리오 2: 급여 계산 버그 수정

**작업 내용:**
1. PayrollService.calculateTotalPay() 버그 수정
2. 음수 급여 검증 로직 추가

**커밋 메시지:**
```
fix(payroll): prevent negative salary calculation

The overtime hours calculation was using wrong formula,
resulting in negative total pay when overtime < 0.

- Fix calculateTotalPay() logic
- Add validation for negative hourly_rate
- Add validation for negative hours_worked

Closes: BU-456
```

### 시나리오 3: 중복 코드 리팩토링

**작업 내용:**
1. BaseEntity 공통 클래스 생성
2. 모든 Entity에서 중복 코드 제거

**커밋 메시지:**
```
refactor: extract common fields to BaseEntity

- Create BaseEntity with id, createdAt, updatedAt
- Update all entities to extend BaseEntity
- Remove duplicate code from 10+ entity classes

This improves maintainability and reduces code duplication
by ~200 lines.

Refs: BU-567
```

---

## 커밋 히스토리 관리

### 작은 단위로 자주 커밋
```bash
# 좋은 예시 (논리적 단위로 분리)
git commit -m "feat(auth): add User entity"
git commit -m "feat(auth): add UserRepository"
git commit -m "feat(auth): implement AuthService"
git commit -m "feat(auth): add login endpoint"

# 나쁜 예시 (한 번에 모든 변경)
git commit -m "feat(auth): implement entire login feature"
```

### Squash Commits (선택사항)
PR merge 전에 여러 작은 커밋을 하나로 합치기:

```bash
# 최근 3개 커밋 squash
git rebase -i HEAD~3

# 편집기에서:
pick abc123 feat(auth): add User entity
squash def456 feat(auth): add UserRepository
squash ghi789 feat(auth): implement AuthService

# 커밋 메시지 수정
feat(auth): implement user authentication

- Add User entity with password encryption
- Create UserRepository with findByUserId
- Implement AuthService with login logic

Refs: BU-101
```

### Amend (마지막 커밋 수정)
```bash
# 마지막 커밋에 추가 변경사항 포함
git add .
git commit --amend

# 마지막 커밋 메시지만 수정
git commit --amend -m "fix(auth): correct typo in login validation"
```

---

## 자동화 및 검증

### Commit Message Lint (추후 도입 예정)

#### commitlint 설정
```bash
# commitlint 설치
npm install --save-dev @commitlint/cli @commitlint/config-conventional

# .commitlintrc.json 생성
{
  "extends": ["@commitlint/config-conventional"],
  "rules": {
    "type-enum": [2, "always", [
      "feat", "fix", "docs", "style", "refactor",
      "test", "chore", "perf", "ci", "revert"
    ]],
    "subject-max-length": [2, "always", 50]
  }
}
```

#### Husky Pre-commit Hook
```bash
# Husky 설치
npm install --save-dev husky

# Pre-commit hook 설정
npx husky add .husky/commit-msg 'npx --no -- commitlint --edit $1'
```

---

## IntelliJ IDEA 플러그인

### Git Commit Template Plugin

**설치:**
1. `Settings` → `Plugins`
2. "Git Commit Template" 검색 및 설치

**설정:**
1. `Settings` → `Other Settings` → `Git Commit Template`
2. 템플릿 설정:
```
<type>(<scope>): <subject>

<body>

Refs: BU-
```

---

## Conventional Commits 요약

| 요소 | 필수 여부 | 설명 |
|------|-----------|------|
| **type** | 필수 | feat, fix, docs, style, refactor, test, chore 등 |
| **scope** | 선택 | auth, employee, payroll 등 도메인 |
| **subject** | 필수 | 50자 이내, 명령형 현재 시제 |
| **body** | 선택 | 상세 설명, 72자 줄바꿈 |
| **footer** | 선택 | Jira 참조, Breaking Changes |

---

## 체크리스트

커밋 전 확인 사항:

- [ ] Type이 올바른가? (feat, fix, docs, ...)
- [ ] Scope가 명확한가? (auth, employee, ...)
- [ ] Subject가 50자 이내인가?
- [ ] 명령형 현재 시제를 사용했는가?
- [ ] Subject에 마침표를 사용하지 않았는가?
- [ ] Jira 키를 포함했는가? (Refs: BU-XXX)
- [ ] Body에 변경 이유를 설명했는가? (선택사항)
- [ ] 하나의 논리적 변경만 포함했는가?
- [ ] 빌드가 가능한 상태인가?

---

## 참고 자료

- [Conventional Commits](https://www.conventionalcommits.org/)
- [Angular Commit Guidelines](https://github.com/angular/angular/blob/main/CONTRIBUTING.md#commit)
- [Semantic Versioning](https://semver.org/)
- [How to Write a Git Commit Message](https://chris.beams.io/posts/git-commit/)