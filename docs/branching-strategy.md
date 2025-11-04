# Git 브랜치 전략

## 브랜치 모델

Build-Up 프로젝트는 **GitHub Flow** 기반의 단순화된 브랜치 전략을 채택합니다.

### 주요 브랜치

#### 1. `main` (또는 `master`)
- **용도**: 운영 환경에 배포 가능한 안정적인 코드
- **보호 규칙**:
  - 직접 push 금지
  - PR을 통해서만 merge 가능
  - 코드 리뷰 최소 1명 이상 필수
  - CI/CD 테스트 통과 필수
- **배포**: `main` 브랜치로 merge 시 자동 배포 (추후 설정)

#### 2. `develop`
- **용도**: 개발 중인 기능들의 통합 브랜치
- **보호 규칙**:
  - 직접 push 금지
  - PR을 통해서만 merge 가능
  - 코드 리뷰 권장 (필수 아님)
  - 테스트 통과 필수
- **배포**: 개발/스테이징 환경에 배포

#### 3. Feature 브랜치
- **용도**: 새로운 기능 개발
- **생명주기**: 기능 개발 시작 ~ PR merge까지
- **삭제**: merge 후 자동 삭제 권장

---

## Jira 기반 브랜치 전략

### Epic vs Story 단위 브랜치

| 구분 | Epic 단위 | Story 단위 (권장) |
|------|-----------|-------------------|
| **브랜치 크기** | 대형 (여러 Story 포함) | 소형 (단일 Story) |
| **개발 기간** | 1~2주+ | 1~3일 |
| **PR 크기** | 대형 (리뷰 어려움) | 소형 (리뷰 용이) |
| **충돌 가능성** | 높음 | 낮음 |
| **배포 속도** | 느림 | 빠름 |
| **롤백 용이성** | 어려움 | 쉬움 |

#### 권장 전략: **Story 단위 브랜치**

**이유:**
1. **작은 PR**: 코드 리뷰가 빠르고 정확함
2. **빠른 피드백**: 문제 조기 발견 및 수정
3. **낮은 충돌**: develop 브랜치와의 충돌 최소화
4. **쉬운 롤백**: 문제 발생 시 특정 Story만 revert 가능
5. **CI/CD 친화적**: 작은 단위로 테스트 및 배포

#### Epic 관리 방법
- Epic은 Jira에서만 관리
- Epic 하위 Story들을 각각 별도 브랜치로 개발
- Epic 완료 = 모든 Story PR이 merge됨

---

## 브랜치 네이밍 컨벤션

### 기본 형식
```
<type>/<jira-key>-<short-description>
```

### Type 종류
- `feature/`: 새로운 기능 개발
- `fix/`: 버그 수정
- `hotfix/`: 긴급 버그 수정 (운영 환경)
- `refactor/`: 코드 리팩토링
- `docs/`: 문서 수정
- `test/`: 테스트 코드 추가/수정
- `chore/`: 빌드 설정, 의존성 업데이트 등

### 예시
```bash
# 좋은 예시
feature/BU-123-user-authentication
fix/BU-456-null-pointer-in-payroll
hotfix/BU-789-security-patch
refactor/BU-234-simplify-service-layer
docs/BU-567-update-api-documentation
test/BU-890-add-integration-tests

# 나쁜 예시 (지양)
feature/login              # Jira 키 누락
BU-123                     # type 누락
feature/BU-123             # 설명 누락
feature/new_feature        # Jira 키 누락
```

### 브랜치명 규칙
- **소문자 사용**: 모든 문자는 소문자
- **하이픈 구분**: 단어는 하이픈(`-`)으로 구분
- **간결한 설명**: 3~5 단어 이내
- **명확한 의미**: 브랜치명만 보고 작업 내용 파악 가능

---

## 브랜치 생성 및 관리

### 1. Feature 브랜치 생성

#### develop 브랜치 최신화
```bash
# develop 브랜치로 이동
git checkout develop

# 최신 코드 pull
git pull origin develop
```

#### 새 브랜치 생성
```bash
# Jira Story: BU-123 "사용자 로그인 기능"
git checkout -b feature/BU-123-user-login

# 첫 커밋 후 원격에 push
git push -u origin feature/BU-123-user-login
```

### 2. 개발 중 develop 동기화

다른 팀원의 PR이 먼저 merge되어 충돌이 예상되는 경우:

```bash
# 현재 브랜치에서 develop 최신 변경사항 가져오기
git checkout develop
git pull origin develop

git checkout feature/BU-123-user-login
git rebase develop

# 충돌 해결 후
git add .
git rebase --continue

# 원격 브랜치 강제 업데이트 (rebase 후)
git push -f origin feature/BU-123-user-login
```

### 3. PR 생성 전 체크리스트

```bash
# 1. 최신 develop과 동기화
git checkout develop && git pull
git checkout feature/BU-123-user-login
git rebase develop

# 2. 빌드 테스트
./gradlew clean build

# 3. 단위 테스트 실행
./gradlew test

# 4. 코드 포맷 확인 (IntelliJ에서 Reformat Code)

# 5. 원격 브랜치 push
git push origin feature/BU-123-user-login
```

### 4. PR Merge 후

```bash
# 로컬 브랜치 삭제
git branch -d feature/BU-123-user-login

# 원격 브랜치 삭제 (GitHub에서 자동 삭제 설정 권장)
git push origin --delete feature/BU-123-user-login

# develop 브랜치 최신화
git checkout develop
git pull origin develop
```

---

## Hotfix 전략

운영 환경에서 긴급 버그 발생 시:

### 1. Hotfix 브랜치 생성
```bash
# main 브랜치에서 생성
git checkout main
git pull origin main
git checkout -b hotfix/BU-999-critical-bug
```

### 2. 버그 수정 및 테스트
```bash
# 버그 수정 코드 작성
# 테스트 실행
./gradlew test

# 빌드 확인
./gradlew clean build
```

### 3. main과 develop 모두에 merge
```bash
# main으로 PR 생성 (긴급 배포)
# develop으로도 PR 생성 (동기화)
```

### 4. 태그 생성 (배포 버전)
```bash
git checkout main
git pull origin main
git tag -a v1.0.1 -m "Hotfix: Critical bug fix"
git push origin v1.0.1
```

---

## 장기 Feature 브랜치 관리

Story가 예상보다 오래 걸리는 경우 (5일 이상):

### 전략 1: Story 분리 (권장)
```bash
# 기존 Story를 더 작은 Story로 분할
# 예: BU-123 → BU-123-1, BU-123-2, BU-123-3

feature/BU-123-1-user-model
feature/BU-123-2-user-service
feature/BU-123-3-user-controller
```

### 전략 2: 중간 PR
```bash
# WIP (Work In Progress) PR 생성
# Draft PR로 생성하여 중간 피드백 받기
# 완료되면 Ready for Review로 변경
```

---

## 충돌 해결 가이드

### Rebase 중 충돌 발생 시

```bash
# 1. 충돌 발생
git rebase develop
# CONFLICT (content): Merge conflict in ...

# 2. 충돌 파일 확인
git status

# 3. 충돌 해결 (IDE에서 수동 수정)
# <<<<<<< HEAD
# 현재 브랜치 코드
# =======
# develop 브랜치 코드
# >>>>>>>

# 4. 충돌 해결 완료 후
git add <충돌-파일>
git rebase --continue

# 5. 모든 충돌 해결 완료
git push -f origin feature/BU-123-user-login
```

### Rebase 취소 (문제 발생 시)
```bash
git rebase --abort
```

---

## 브랜치 보호 규칙 (GitHub Settings)

### main 브랜치
- [ ] Require pull request reviews before merging (1명 이상)
- [ ] Dismiss stale pull request approvals when new commits are pushed
- [ ] Require status checks to pass before merging
  - [ ] CI/CD 테스트 통과
  - [ ] 빌드 성공
- [ ] Require branches to be up to date before merging
- [ ] Include administrators (관리자도 규칙 적용)

### develop 브랜치
- [ ] Require pull request reviews before merging (선택사항)
- [ ] Require status checks to pass before merging
  - [ ] 빌드 성공
  - [ ] 테스트 통과

---

## 베스트 프랙티스

### 1. 브랜치 수명 최소화
- ✅ Feature 브랜치는 3일 이내 완료 목표
- ✅ 오래된 브랜치는 정기적으로 develop과 동기화
- ✅ Merge 후 즉시 브랜치 삭제

### 2. 작은 단위로 자주 Commit
- ✅ 의미 있는 단위로 커밋 (빌드 가능한 상태)
- ✅ 하루 작업 종료 시 반드시 push
- ❌ 모든 작업 완료 후 한 번에 커밋 (지양)

### 3. develop과 자주 동기화
- ✅ 매일 아침 develop pull
- ✅ 다른 팀원 PR merge 시 즉시 동기화
- ✅ 충돌 조기 발견 및 해결

### 4. PR 크기 관리
- ✅ 변경 파일 10개 이내
- ✅ 변경 라인 300줄 이내
- ✅ 리뷰어가 30분 내 리뷰 가능한 크기

### 5. 명확한 브랜치명
- ✅ Jira 키 반드시 포함
- ✅ 작업 내용이 명확히 드러나는 설명
- ❌ 모호하거나 추상적인 이름 지양

---

## 실전 예시

### 예시 1: 사용자 인증 기능 개발

```bash
# 1. Jira Story: BU-101 "JWT 기반 사용자 로그인"
git checkout develop
git pull origin develop
git checkout -b feature/BU-101-jwt-login

# 2. 개발 진행
# - JwtTokenProvider.java 작성
# - AuthService.java 수정
# - AuthController.java 엔드포인트 추가

# 3. 커밋 (Conventional Commits 규칙 준수)
git add .
git commit -m "feat: implement JWT-based user login

- Add JwtTokenProvider for token generation
- Update AuthService with login logic
- Add /api/auth/login endpoint

Refs: BU-101"

# 4. PR 생성 전 테스트
./gradlew test
./gradlew build

# 5. Push 및 PR 생성
git push -u origin feature/BU-101-jwt-login
# GitHub에서 PR 생성 → develop 브랜치로
```

### 예시 2: 긴급 버그 수정

```bash
# 1. Jira Bug: BU-999 "급여 계산 오류"
git checkout main
git pull origin main
git checkout -b hotfix/BU-999-payroll-calculation

# 2. 버그 수정
# PayrollService.java의 calculateTotalPay() 로직 수정

# 3. 커밋
git commit -m "fix: correct payroll calculation logic

- Fix overtime hours calculation bug
- Add validation for negative wage values

Refs: BU-999"

# 4. 테스트 및 빌드
./gradlew test
./gradlew build

# 5. main과 develop 모두에 PR 생성
git push -u origin hotfix/BU-999-payroll-calculation
# PR 1: main ← hotfix/BU-999-payroll-calculation
# PR 2: develop ← hotfix/BU-999-payroll-calculation
```

---

## 참고 자료

- [GitHub Flow](https://guides.github.com/introduction/flow/)
- [Conventional Commits](https://www.conventionalcommits.org/)
- [Atlassian Git Workflows](https://www.atlassian.com/git/tutorials/comparing-workflows)
