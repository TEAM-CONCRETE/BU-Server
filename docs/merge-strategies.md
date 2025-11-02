# Git Merge 전략

## 개요

Git에는 세 가지 주요 Merge 전략이 있습니다: Merge Commit, Squash Merge, Rebase Merge. 각 전략마다 장단점이 있으며, Build-Up 프로젝트에서는 상황에 따라 적절한 전략을 선택합니다.

---

## 세 가지 Merge 전략 비교

### 1. Merge Commit (기본 Merge)

**작동 방식:**
- 모든 커밋 히스토리를 유지
- 새로운 Merge Commit 생성
- 브랜치 구조가 그대로 보존됨

**Git 명령어:**
```bash
git checkout develop
git merge feature/BU-123-user-login
```

**히스토리 예시:**
```
* a1b2c3d (develop) Merge branch 'feature/BU-123-user-login' into develop
|\
| * d4e5f6g feat(auth): add login controller
| * g7h8i9j feat(auth): add auth service
| * j0k1l2m feat(auth): add user entity
|/
* m3n4o5p Previous commit on develop
```

**장점:**
- ✅ 전체 커밋 히스토리 보존
- ✅ 브랜치 구조 명확히 보임
- ✅ 개별 커밋 추적 가능
- ✅ Revert 시 브랜치 단위로 가능

**단점:**
- ❌ 히스토리가 복잡해짐 (특히 브랜치가 많을 때)
- ❌ Merge commit이 많아짐
- ❌ 작은 커밋들도 모두 히스토리에 남음

**사용 시나리오:**
- Epic 크기의 대형 기능 (여러 Story 포함)
- 여러 개발자가 협업한 장기 브랜치
- 개별 커밋 히스토리가 중요한 경우

---

### 2. Squash Merge (권장)

**작동 방식:**
- 모든 커밋을 하나로 압축
- 하나의 새로운 커밋으로 develop에 추가
- 브랜치 히스토리는 삭제됨

**Git 명령어:**
```bash
git checkout develop
git merge --squash feature/BU-123-user-login
git commit -m "feat(auth): implement JWT-based user login

- Add JwtTokenProvider for token generation
- Implement AuthService with login logic
- Add /api/auth/login endpoint
- Create LoginRequestDto and LoginResponseDto

Refs: BU-123"
```

**GitHub에서:**
- PR 화면에서 "Squash and merge" 버튼 클릭

**히스토리 예시:**
```
* a1b2c3d (develop) feat(auth): implement JWT-based user login
|
* m3n4o5p Previous commit on develop
```

**장점:**
- ✅ 깔끔하고 선형적인 히스토리
- ✅ develop 브랜치가 간결함
- ✅ 각 Story당 하나의 커밋
- ✅ Revert 시 Story 단위로 쉽게 가능
- ✅ 로그 읽기 편함

**단점:**
- ❌ 개별 커밋 히스토리 손실
- ❌ 세밀한 변경 추적 어려움
- ❌ 브랜치 작업 과정이 안 보임

**사용 시나리오 (권장):**
- Story 단위 Feature 브랜치
- 작은 버그 수정
- 단일 개발자가 작업한 브랜치
- WIP 커밋이 많은 경우

---

### 3. Rebase Merge (Rebase and Merge)

**작동 방식:**
- 브랜치 커밋들을 develop 끝에 재배치
- Merge commit 없이 선형 히스토리 유지
- 모든 커밋 히스토리 보존

**Git 명령어:**
```bash
git checkout feature/BU-123-user-login
git rebase develop
git checkout develop
git merge feature/BU-123-user-login  # Fast-forward merge
```

**GitHub에서:**
- PR 화면에서 "Rebase and merge" 버튼 클릭

**히스토리 예시:**
```
* d4e5f6g (develop) feat(auth): add login controller
* g7h8i9j feat(auth): add auth service
* j0k1l2m feat(auth): add user entity
* m3n4o5p Previous commit on develop
```

**장점:**
- ✅ 선형적이고 깔끔한 히스토리
- ✅ Merge commit이 없음
- ✅ 모든 커밋 히스토리 보존
- ✅ 브랜치 구조 없이 깔끔

**단점:**
- ❌ 커밋 SHA가 변경됨 (history rewrite)
- ❌ 이미 push한 브랜치에 rebase 시 문제 발생 가능
- ❌ 잘못된 rebase는 되돌리기 어려움
- ❌ 팀원이 같은 브랜치 작업 시 충돌 가능

**사용 시나리오:**
- 개인 작업 브랜치
- 커밋 히스토리가 깔끔한 경우
- Git rebase에 익숙한 팀

---

## Build-Up 프로젝트 권장 전략

### 기본 원칙: **Squash Merge**

**이유:**
1. **Story 단위 관리**: 각 Jira Story가 하나의 커밋으로 관리됨
2. **깔끔한 히스토리**: develop 브랜치가 간결하고 읽기 쉬움
3. **쉬운 Revert**: 문제 발생 시 Story 단위로 쉽게 되돌림
4. **WIP 커밋 정리**: 개발 중 작은 커밋들이 히스토리에 남지 않음

### 예외: 상황별 전략

| 상황 | 전략 | 이유 |
|------|------|------|
| **일반 Story 브랜치** | Squash Merge | 깔끔한 히스토리, Story 단위 관리 |
| **Epic 크기 기능** | Merge Commit | 여러 Story 추적 필요 |
| **Hotfix** | Squash Merge | 빠른 배포와 명확한 기록 |
| **Docs 업데이트** | Squash Merge | 단순 문서 변경 |
| **Refactoring** | Merge Commit | 여러 파일 변경 추적 |

---

## GitHub에서 Merge 전략 설정

### 1. Repository 설정
1. GitHub Repository → Settings
2. "Pull Requests" 섹션

### 2. 권장 설정
- ✅ **Allow squash merging** (권장)
- ✅ **Allow merge commits** (Epic용)
- ❌ **Allow rebase merging** (선택적)

### 3. 기본 전략 설정
- **Default merge method**: Squash and merge

### 4. PR 머지 후 브랜치 자동 삭제
- ✅ **Automatically delete head branches**

---

## Squash Merge 워크플로우 (권장)

### 1. Feature 브랜치 개발

```bash
# 1. develop에서 브랜치 생성
git checkout develop
git pull origin develop
git checkout -b feature/BU-123-user-login

# 2. 개발 중 자유롭게 커밋
git commit -m "wip: add user entity"
git commit -m "wip: add auth service"
git commit -m "fix: typo in service"
git commit -m "feat: add login endpoint"

# 3. PR 생성 전 develop 동기화 (Rebase)
git checkout develop
git pull origin develop
git checkout feature/BU-123-user-login
git rebase develop

# 충돌 해결 후
git add .
git rebase --continue

# 4. 원격 브랜치 push
git push -f origin feature/BU-123-user-login
```

### 2. PR 생성 및 리뷰

```markdown
# PR 제목 (Squash 시 커밋 메시지가 됨)
feat(auth): implement JWT-based user login

# PR 설명 (Squash 시 커밋 본문이 됨)
- Add JwtTokenProvider for token generation
- Implement AuthService with login logic
- Add /api/auth/login endpoint
- Create LoginRequestDto and LoginResponseDto

Refs: BU-123
```

### 3. Squash and Merge

GitHub PR 화면에서:
1. 코드 리뷰 완료 및 Approve
2. "Squash and merge" 버튼 클릭
3. 커밋 메시지 확인 및 수정 (필요시)
4. "Confirm squash and merge" 클릭

### 4. 브랜치 정리

```bash
# 로컬 브랜치 삭제
git branch -d feature/BU-123-user-login

# develop 최신화
git checkout develop
git pull origin develop
```

---

## Merge Commit 워크플로우 (Epic용)

### 사용 케이스
- Epic 크기의 대형 기능 (여러 Story 포함)
- 여러 개발자가 협업한 장기 브랜치

### 워크플로우

```bash
# 1. Epic 브랜치 생성
git checkout develop
git checkout -b feature/BU-100-user-management-epic

# 2. Epic 하위 Story 브랜치들 개발
git checkout -b feature/BU-101-user-login
# 개발 및 커밋...
git checkout feature/BU-100-user-management-epic
git merge feature/BU-101-user-login  # Epic 브랜치로 Squash Merge

git checkout -b feature/BU-102-user-registration
# 개발 및 커밋...
git checkout feature/BU-100-user-management-epic
git merge feature/BU-102-user-registration  # Epic 브랜치로 Squash Merge

# 3. Epic 완료 후 develop으로 Merge Commit
git checkout develop
git merge feature/BU-100-user-management-epic  # Merge Commit 사용
```

GitHub에서:
- Epic 브랜치 → develop PR 생성
- "Create a merge commit" 선택

---

## Hotfix 워크플로우

긴급 버그 수정 시:

```bash
# 1. main에서 hotfix 브랜치 생성
git checkout main
git pull origin main
git checkout -b hotfix/BU-999-critical-bug

# 2. 버그 수정 및 커밋
git commit -m "fix(payroll): correct salary calculation"

# 3. main으로 PR 생성 (Squash Merge)
# GitHub: hotfix/BU-999 → main (Squash and merge)

# 4. develop으로도 PR 생성 (Squash Merge)
# GitHub: hotfix/BU-999 → develop (Squash and merge)

# 5. Tag 생성 (배포 버전)
git checkout main
git pull origin main
git tag -a v1.0.1 -m "Hotfix: Critical bug fix"
git push origin v1.0.1
```

---

## 충돌 해결

### Squash Merge 시 충돌

1. **로컬에서 Rebase로 충돌 미리 해결 (권장)**

```bash
git checkout develop
git pull origin develop
git checkout feature/BU-123-user-login
git rebase develop

# 충돌 발생 시
# 1. 충돌 파일 수동 해결
# 2. git add <충돌-파일>
# 3. git rebase --continue
# 4. 모든 충돌 해결 완료

git push -f origin feature/BU-123-user-login
```

2. **GitHub에서 충돌 해결**

- GitHub이 충돌을 감지하면 "Resolve conflicts" 버튼 제공
- 웹 에디터에서 충돌 해결
- "Mark as resolved" → "Commit merge"

---

## 실전 예시

### 예시 1: 일반 Story (Squash Merge)

```bash
# 1. 브랜치 생성 및 개발
git checkout -b feature/BU-123-user-login

# 2. 개발 중 여러 커밋
git commit -m "wip: add user entity"
git commit -m "wip: add auth service"
git commit -m "wip: add login controller"
git commit -m "fix: validation error"
git commit -m "docs: add swagger annotations"

# 3. PR 생성 전 정리
git rebase develop
git push -f origin feature/BU-123-user-login

# 4. GitHub에서 PR 생성
# Title: feat(auth): implement JWT-based user login
# Body: 상세 설명

# 5. 코드 리뷰 및 Approve

# 6. Squash and merge
# develop에는 하나의 깔끔한 커밋만 추가됨
```

**결과 (develop 브랜치):**
```
* a1b2c3d feat(auth): implement JWT-based user login
```

### 예시 2: Epic 기능 (Merge Commit)

```bash
# 1. Epic 브랜치 생성
git checkout -b feature/BU-100-payroll-system-epic

# 2. Story 1 개발
git checkout -b feature/BU-101-payroll-calculation
# ... 개발 및 커밋 ...
git checkout feature/BU-100-payroll-system-epic
git merge --squash feature/BU-101-payroll-calculation
git commit -m "feat(payroll): add payroll calculation logic"

# 3. Story 2 개발
git checkout -b feature/BU-102-payroll-report
# ... 개발 및 커밋 ...
git checkout feature/BU-100-payroll-system-epic
git merge --squash feature/BU-102-payroll-report
git commit -m "feat(payroll): add payroll report generation"

# 4. Epic 완료 후 develop으로 Merge Commit
git checkout develop
git merge feature/BU-100-payroll-system-epic
```

**결과 (develop 브랜치):**
```
*   m1n2o3p Merge branch 'feature/BU-100-payroll-system-epic' into develop
|\
| * d4e5f6g feat(payroll): add payroll report generation
| * g7h8i9j feat(payroll): add payroll calculation logic
|/
* p4q5r6s Previous commit
```

---

## 잘못된 Merge 되돌리기

### Squash Merge 되돌리기

```bash
# 1. 잘못 머지된 커밋 찾기
git log --oneline

# 2. Revert (안전한 방법)
git revert <commit-sha>
git push origin develop

# 3. 또는 Hard Reset (주의! 이미 push된 경우 위험)
git reset --hard <이전-커밋-sha>
git push -f origin develop  # 팀원과 협의 후만 사용
```

### Merge Commit 되돌리기

```bash
# Merge commit 자체를 revert
git revert -m 1 <merge-commit-sha>
git push origin develop
```

---

## 베스트 프랙티스

### 1. Squash Merge 사용 시
- ✅ PR 생성 전 develop과 rebase
- ✅ PR 제목과 설명을 신중히 작성 (커밋 메시지가 됨)
- ✅ Jira 키 반드시 포함
- ✅ Merge 후 브랜치 즉시 삭제

### 2. Merge Commit 사용 시
- ✅ Epic 브랜치만 사용
- ✅ Epic 하위 Story들은 Epic 브랜치로 Squash Merge
- ✅ 브랜치 구조 명확히 문서화

### 3. 공통
- ✅ Merge 전 테스트와 빌드 성공 확인
- ✅ 코드 리뷰 완료 후 Merge
- ✅ main/develop 브랜치는 보호 설정
- ✅ CI/CD 통과 후 Merge

---

## GitHub Branch Protection Rules

develop과 main 브랜치 보호 설정:

### Settings → Branches → Add rule

**develop 브랜치:**
- ✅ Require pull request reviews before merging
- ✅ Require status checks to pass before merging
- ✅ Require branches to be up to date before merging
- ✅ Include administrators

**main 브랜치:**
- ✅ Require pull request reviews before merging (최소 1명)
- ✅ Require status checks to pass before merging
- ✅ Require branches to be up to date before merging
- ✅ Include administrators
- ✅ Require linear history (Squash 강제)

---

## 참고 자료

- [GitHub Docs: About merge methods](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/configuring-pull-request-merges/about-merge-methods-on-github)
- [Atlassian: Merging vs Rebasing](https://www.atlassian.com/git/tutorials/merging-vs-rebasing)
- [Git Documentation: git-merge](https://git-scm.com/docs/git-merge)
