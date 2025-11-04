# Jira-GitHub 워크플로우 가이드

## 개요

Build-Up 프로젝트는 Jira를 이슈 트래킹 도구로, GitHub를 코드 관리 도구로 사용합니다. 이 문서는 Jira와 GitHub를 효과적으로 연동하여 작업하는 전체 워크플로우를 설명합니다.

---

## Jira-GitHub 통합 설정

### 1. GitHub for Jira 앱 설치

#### Jira Cloud 설정
1. Jira → **Apps** → **Find new apps**
2. "GitHub for Jira" 검색 및 설치
3. **Configure** → GitHub 계정 연결
4. Repository 선택: `your-org/BU-Server`

#### 연동 확인
- Jira Issue에서 GitHub 탭 확인
- Development 섹션에 Branch, Commit, PR 정보 표시

### 2. Jira 이슈 키 형식

**프로젝트 키**: `BU` (Build-Up)

**이슈 형식**:
- Epic: `BU-100`, `BU-200` (100 단위)
- Story: `BU-101`, `BU-102` (Epic 하위)
- Bug: `BU-999` (개별 번호)
- Task: `BU-50` (소규모 작업)

---

## 전체 워크플로우

### Epic → Story → Branch → Commit → PR → Merge

```
┌─────────────────────────────────────────────────────────────┐
│                     Jira Epic 생성                           │
│                  BU-100: 사용자 관리 시스템                   │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ├──► Story 1: BU-101 (JWT 로그인)
                     ├──► Story 2: BU-102 (회원가입)
                     └──► Story 3: BU-103 (프로필 조회)
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│              GitHub: Story 단위 브랜치 생성                   │
│          feature/BU-101-jwt-login                           │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ├──► Commit 1: feat(auth): add user entity
                     ├──► Commit 2: feat(auth): add auth service
                     └──► Commit 3: feat(auth): add login endpoint
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                  GitHub: PR 생성                            │
│   Title: feat(auth): implement JWT-based user login        │
│   Body: Refs: BU-101                                        │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ├──► 코드 리뷰
                     ├──► 테스트 통과
                     └──► Squash and Merge
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│              Jira: Story 자동 업데이트                       │
│   Status: In Progress → Done                                │
│   Development: 1 commit, 1 PR merged                        │
└─────────────────────────────────────────────────────────────┘
```

---

## STEP 1: Jira Epic 생성 및 관리

### 1.1 Epic 생성

**Jira에서:**
1. **Create** → Issue Type: **Epic**
2. **Epic Name**: 사용자 관리 시스템
3. **Summary**: BU-100: 사용자 관리 시스템 구축
4. **Description**:
   ```markdown
   ## 목표
   사용자 인증 및 프로필 관리 기능 구현

   ## 범위
   - JWT 기반 로그인
   - 회원가입
   - 프로필 조회/수정
   - 비밀번호 재설정

   ## 기술 스택
   - Spring Security
   - JWT (jjwt 0.12.6)
   - BCrypt 암호화

   ## 완료 조건
   - 모든 하위 Story 완료
   - API 문서 작성 완료
   - 테스트 커버리지 80% 이상
   ```
5. **Assignee**: 담당 PM 또는 Tech Lead
6. **Labels**: backend, authentication
7. **Sprint**: 해당 스프린트 선택 (또는 Backlog)

### 1.2 Epic 하위 Story 생성

**Story 1: JWT 로그인**
- **Issue Type**: Story
- **Summary**: BU-101: JWT 기반 사용자 로그인 구현
- **Epic Link**: BU-100 (사용자 관리 시스템)
- **Description**:
  ```markdown
  ## User Story
  As a 사용자
  I want 로그인 기능
  So that 시스템에 안전하게 접근할 수 있다

  ## Acceptance Criteria
  - [ ] 사용자 ID와 비밀번호로 로그인 가능
  - [ ] Access Token (1시간)과 Refresh Token (7일) 발급
  - [ ] 잘못된 인증 정보 시 401 에러 반환
  - [ ] Swagger 문서 작성

  ## Technical Notes
  - JwtTokenProvider 구현
  - AuthService.login() 메서드
  - POST /api/auth/login 엔드포인트
  - BCrypt로 비밀번호 검증

  ## API Spec
  POST /api/auth/login
  Request: { "userId": "admin", "password": "admin123" }
  Response: { "accessToken": "...", "refreshToken": "..." }
  ```
- **Story Points**: 5 (팀 기준에 맞게 조정)
- **Assignee**: 백엔드 개발자
- **Priority**: High

**Story 2, 3도 동일한 방식으로 생성**

---

## STEP 2: Story 개발 시작

### 2.1 Jira Story 상태 변경

**Story BU-101 선택:**
1. Status: **To Do** → **In Progress**
2. **Start Progress** 버튼 클릭
3. Assignee 본인 확인

### 2.2 GitHub 브랜치 생성

**로컬 환경:**
```bash
# 1. develop 브랜치 최신화
git checkout develop
git pull origin develop

# 2. Story 브랜치 생성 (Jira 키 포함!)
git checkout -b feature/BU-101-jwt-login

# 3. 빈 커밋으로 브랜치 생성 (선택사항)
git commit --allow-empty -m "chore: initialize feature branch

Refs: BU-101"

# 4. 원격 브랜치 push
git push -u origin feature/BU-101-jwt-login
```

**Jira에서 확인:**
- Story BU-101 → Development 섹션
- "1 branch" 표시됨

---

## STEP 3: 개발 및 커밋

### 3.1 개발 진행

**작업 순서:**
1. Entity 작성 (User.java)
2. Repository 작성 (UserRepository.java)
3. Service 작성 (AuthService.java)
4. DTO 작성 (LoginRequestDto, LoginResponseDto)
5. Controller 작성 (AuthController.java)
6. 테스트 작성

### 3.2 커밋 작성 (Jira Smart Commits)

**일반 커밋:**
```bash
git add src/main/java/com/concrete/buildup/domain/auth/entity/User.java
git commit -m "feat(auth): add User entity with password encryption

- Add User entity with JPA annotations
- Use BCryptPasswordEncoder for password hashing
- Add email, phone validation

Refs: BU-101"
```

**Jira Smart Commits 활용:**

#### 시간 기록 (Logged Time)
```bash
git commit -m "feat(auth): implement AuthService login logic

- Add login method with JWT token generation
- Validate user credentials
- Handle authentication failures

Refs: BU-101
Time: 2h 30m"
```

#### 이슈 상태 변경
```bash
git commit -m "feat(auth): add login endpoint

POST /api/auth/login endpoint with Swagger docs

Refs: BU-101
Status: In Review"
```

#### 코멘트 추가
```bash
git commit -m "test(auth): add login service unit tests

Refs: BU-101
Comment: Login service tests complete, coverage 90%"
```

### 3.3 정기적으로 Push

```bash
# 작업 종료 시마다 push
git push origin feature/BU-101-jwt-login
```

**Jira에서 확인:**
- Development 섹션: "3 commits" 표시
- 각 커밋 메시지 확인 가능

---

## STEP 4: PR 생성

### 4.1 PR 생성 전 체크

```bash
# 1. develop 최신화 및 rebase
git checkout develop
git pull origin develop
git checkout feature/BU-101-jwt-login
git rebase develop

# 2. 충돌 해결 (있는 경우)
# 3. 테스트 실행
./gradlew test

# 4. 빌드 확인
./gradlew clean build

# 5. Push
git push -f origin feature/BU-101-jwt-login
```

### 4.2 GitHub에서 PR 생성

**PR 제목 (Jira 키 필수!):**
```
feat(auth): implement JWT-based user login
```

**PR 본문:**
```markdown
## 📋 Jira Issue
- Jira: [BU-101](https://your-jira-domain.atlassian.net/browse/BU-101)

## 📝 변경 사항 요약
JWT 기반 사용자 로그인 기능을 구현했습니다.

### 주요 변경사항
- User entity 및 UserRepository 추가
- JwtTokenProvider를 사용한 토큰 생성/검증 로직
- AuthService.login() 메서드 구현
- POST /api/auth/login 엔드포인트 추가
- LoginRequestDto, LoginResponseDto 작성

## 🎯 변경 목적
사용자 인증을 위한 JWT 기반 로그인 기능을 제공합니다.
Access Token(1시간)과 Refresh Token(7일)을 발급합니다.

## 🔍 변경 내역 상세

### 추가된 기능 (Features)
- [x] JWT 토큰 생성 및 검증 (JwtTokenProvider)
- [x] 로그인 비즈니스 로직 (AuthService)
- [x] 로그인 API 엔드포인트 (AuthController)
- [x] 비밀번호 암호화 (BCrypt)

## 🧪 테스트 방법

### 단위 테스트
- [x] 단위 테스트 작성 완료
- [x] 기존 테스트 통과 확인
- 테스트 커버리지: 85%

### API 테스트 예시
\`\`\`bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"userId": "admin", "password": "admin123"}'
\`\`\`

**예상 결과:**
\`\`\`json
{
  "success": true,
  "message": "로그인 성공",
  "data": {
    "accessToken": "eyJhbGc...",
    "refreshToken": "eyJhbGc..."
  }
}
\`\`\`

## ✅ 체크리스트

### 코드 품질
- [x] 코드 스타일 가이드 준수 (Google Java Style)
- [x] Lombok 어노테이션 적절히 사용
- [x] 불필요한 주석 제거
- [x] 하드코딩된 값 제거 (환경변수 사용)

### 테스트
- [x] 단위 테스트 작성 완료
- [x] 모든 테스트 통과 (`./gradlew test`)
- [x] 빌드 성공 (`./gradlew clean build`)

### 보안
- [x] SQL Injection 취약점 검토
- [x] 인증/인가 로직 검토
- [x] 민감 정보 노출 검토
- [x] 입력 검증 로직 추가

### 문서
- [x] API 문서 업데이트 (Swagger 주석)
- [x] Jira Story 상태 업데이트

### Git
- [x] Conventional Commits 규칙 준수
- [x] Jira 키 포함 (`Refs: BU-101`)
- [x] develop 브랜치 최신 코드 반영 (rebase)
```

**Base 브랜치**: `develop`

**Labels 추가**: `enhancement`, `authentication`

**Reviewers 지정**: 팀 멤버

### 4.3 Jira에서 확인

**Story BU-101:**
- Development 섹션: "1 pull request" 표시
- PR 제목 및 상태 확인

---

## STEP 5: 코드 리뷰 및 피드백

### 5.1 리뷰어 액션

**GitHub PR에서:**
1. 코드 리뷰 진행
2. 코멘트 작성 (docs/code-review-checklist.md 참고)
3. **Approve** 또는 **Request Changes**

**코멘트 예시:**
```
💡 AuthService.login()에서 예외 처리가 잘 되어 있네요!

다만, 이 부분은 Optional을 사용하면 더 안전할 것 같습니다:

User user = userRepository.findByUserId(userId)
    .orElseThrow(() -> new AuthenticationException("Invalid credentials"));

어떻게 생각하시나요?
```

### 5.2 개발자 액션 (피드백 반영)

**수정 커밋:**
```bash
# 피드백 반영
git add src/main/java/com/concrete/buildup/domain/auth/service/AuthService.java
git commit -m "refactor(auth): use Optional for safer null handling

Apply code review feedback to use Optional.

Refs: BU-101"

git push origin feature/BU-101-jwt-login
```

**GitHub PR:**
- 코멘트에 회신: "피드백 감사합니다! Optional로 수정했습니다."
- "Re-request review" 버튼 클릭

**Jira:**
- Story에 코멘트: "코드 리뷰 피드백 반영 완료"

---

## STEP 6: PR Merge 및 Story 완료

### 6.1 Merge 조건 확인

- [x] 코드 리뷰 Approve (최소 1명)
- [x] CI/CD 테스트 통과
- [x] Conflict 없음
- [x] 모든 체크리스트 완료

### 6.2 Squash and Merge

**GitHub PR:**
1. "Squash and merge" 버튼 클릭
2. 커밋 메시지 확인/수정
3. "Confirm squash and merge" 클릭

**최종 커밋 메시지 (develop 브랜치):**
```
feat(auth): implement JWT-based user login (#12)

- Add JwtTokenProvider for token generation and validation
- Implement AuthService.login() with user authentication
- Add POST /api/auth/login endpoint
- Create LoginRequestDto and LoginResponseDto

Access token expires in 1 hour, refresh token in 7 days.
Passwords are validated using BCryptPasswordEncoder.

Refs: BU-101
```

### 6.3 Jira Story 상태 업데이트

**자동 업데이트 (GitHub 연동 시):**
- Development 섹션: "1 PR merged" 표시
- Release 섹션: develop 브랜치 표시

**수동 업데이트:**
1. Status: **In Progress** → **Done**
2. Resolution: **Done**
3. 코멘트 추가: "PR #12 merged to develop"

### 6.4 브랜치 정리

```bash
# 로컬 브랜치 삭제
git branch -d feature/BU-101-jwt-login

# develop 최신화
git checkout develop
git pull origin develop
```

---

## Epic 완료 프로세스

### Epic 하위 모든 Story 완료 후

**Jira Epic BU-100:**
1. 모든 Story가 Done 상태 확인
2. Epic Status: **To Do** → **Done**
3. Epic 리뷰:
   - 모든 Acceptance Criteria 충족 확인
   - API 문서 완성 확인
   - 테스트 커버리지 확인

**GitHub:**
- Epic 관련 모든 PR이 merge됨
- develop 브랜치에 모든 기능 반영

**배포 준비:**
- develop → main PR 생성 (릴리즈 준비)
- 배포 노트 작성

---

## Jira Smart Commits 활용

### Smart Commits 문법

GitHub 커밋 메시지에 특정 명령어를 사용하면 Jira가 자동으로 처리합니다.

#### 1. 이슈 참조 (Reference)
```bash
git commit -m "feat(auth): add login endpoint

Refs: BU-101"
```
→ Jira Story BU-101에 커밋 링크 추가

#### 2. 시간 기록 (Log Work)
```bash
git commit -m "feat(auth): implement auth service

Refs: BU-101
Time: 3h"
```
→ Story BU-101에 3시간 작업 시간 기록

#### 3. 이슈 상태 변경 (Transition)
```bash
git commit -m "feat(auth): complete login feature

Refs: BU-101
Status: Done"
```
→ Story BU-101 상태를 Done으로 변경

#### 4. 코멘트 추가 (Comment)
```bash
git commit -m "test(auth): add integration tests

Refs: BU-101
Comment: Added integration tests for login flow"
```
→ Story BU-101에 코멘트 추가

#### 5. 복합 사용
```bash
git commit -m "feat(auth): complete JWT login implementation

All features implemented and tested.

Refs: BU-101
Time: 2h 30m
Status: In Review
Comment: Ready for code review"
```

---

## 실전 시나리오

### 시나리오 1: 신규 기능 개발 (Epic → Story)

**상황**: 급여 관리 기능을 새로 개발해야 함

#### 1. Jira Epic 생성
```
Epic: BU-200 (급여 관리 시스템)
Stories:
  - BU-201: 급여 계산 로직
  - BU-202: 급여 명세서 생성
  - BU-203: 급여 목록 조회 API
```

#### 2. Story BU-201 개발
```bash
# 브랜치 생성
git checkout -b feature/BU-201-payroll-calculation

# 개발 및 커밋
git commit -m "feat(payroll): add payroll calculation service

Refs: BU-201
Time: 4h"

# PR 생성 → 리뷰 → Merge
# Jira Status: To Do → In Progress → Done
```

#### 3. 나머지 Story 반복

#### 4. Epic 완료
```
모든 Story Done → Epic BU-200 Done
```

---

### 시나리오 2: 버그 수정

**상황**: 운영 환경에서 급여 계산 버그 발견

#### 1. Jira Bug 생성
```
Bug: BU-999 (급여 계산 오류)
Priority: Highest
```

#### 2. Hotfix 브랜치
```bash
git checkout main
git checkout -b hotfix/BU-999-payroll-calculation

# 버그 수정
git commit -m "fix(payroll): correct overtime calculation

Fixed incorrect overtime multiplier from 1.2 to 1.5.

Closes: BU-999
Time: 1h"

# main으로 PR → Merge
# develop으로도 PR → Merge
```

#### 3. Jira 자동 업데이트
- Commit에 `Closes: BU-999` 사용
- Merge 시 Jira Bug BU-999가 자동으로 Done 처리

---

### 시나리오 3: 다중 Story 협업

**상황**: 두 개발자가 같은 Epic의 다른 Story 작업

#### Developer A: BU-201 (급여 계산)
```bash
git checkout -b feature/BU-201-payroll-calculation
# 개발 진행...
```

#### Developer B: BU-202 (명세서 생성)
```bash
git checkout -b feature/BU-202-payslip-generation
# 개발 진행...
```

#### 동시 개발 및 Merge
```bash
# A가 먼저 Merge
# B는 develop 최신화 후 rebase
git checkout develop
git pull origin develop
git checkout feature/BU-202-payslip-generation
git rebase develop
# 충돌 해결 후 PR
```

**Jira Epic BU-200:**
- Development: 2 branches, 2 PRs
- 각 Story는 독립적으로 Done 처리

---

## 베스트 프랙티스

### Jira
- ✅ Story는 3일 이내 완료 가능한 크기로
- ✅ Acceptance Criteria 명확히 작성
- ✅ Technical Notes에 구현 힌트 포함
- ✅ Sprint 시작 전 Story 준비 완료

### GitHub
- ✅ 브랜치명에 Jira 키 반드시 포함
- ✅ 커밋 메시지에 `Refs: BU-XXX` 필수
- ✅ PR 제목은 Squash Merge 시 커밋 메시지가 됨을 기억
- ✅ PR 본문에 테스트 방법 명시

### 협업
- ✅ Daily Stand-up에서 Jira Story 진행 상황 공유
- ✅ Blocked 상태 시 즉시 팀에 알림
- ✅ 코드 리뷰는 24시간 이내 완료
- ✅ Merge 전 반드시 테스트 통과 확인

---

## 트러블슈팅

### 문제 1: Jira에서 GitHub 커밋이 안 보임

**원인**: 커밋 메시지에 Jira 키 누락

**해결**:
```bash
# 마지막 커밋 메시지 수정
git commit --amend -m "feat(auth): add login endpoint

Refs: BU-101"

git push -f origin feature/BU-101-jwt-login
```

### 문제 2: Smart Commits가 작동 안 함

**원인**: Jira-GitHub 연동 설정 문제

**해결**:
1. Jira → Apps → GitHub 연동 확인
2. Repository 권한 확인
3. Jira 프로젝트 키 확인 (BU)

### 문제 3: 여러 Story를 한 PR에 포함

**원인**: Story 단위로 브랜치를 생성하지 않음

**해결**:
- 원칙: 1 Story = 1 Branch = 1 PR
- 이미 작성된 경우 PR을 분리하거나 Story를 합침

---

## 체크리스트

### Story 시작 시
- [ ] Jira Story가 명확히 정의되어 있는가?
- [ ] Acceptance Criteria가 작성되어 있는가?
- [ ] Story를 In Progress로 변경했는가?
- [ ] Jira 키를 포함한 브랜치를 생성했는가?

### 개발 중
- [ ] 커밋 메시지에 Jira 키를 포함하는가?
- [ ] 정기적으로 develop과 동기화하는가?
- [ ] 테스트 코드를 작성하는가?

### PR 생성 전
- [ ] develop과 rebase 완료했는가?
- [ ] 테스트가 모두 통과하는가?
- [ ] 빌드가 성공하는가?
- [ ] PR 템플릿을 작성했는가?

### Merge 후
- [ ] Jira Story를 Done으로 변경했는가?
- [ ] 브랜치를 삭제했는가?
- [ ] develop을 최신화했는가?

---

## 참고 자료

- [Jira Smart Commits](https://support.atlassian.com/jira-software-cloud/docs/process-issues-with-smart-commits/)
- [GitHub for Jira](https://github.com/marketplace/jira-software-github)
- [Atlassian Jira Documentation](https://www.atlassian.com/software/jira/guides)