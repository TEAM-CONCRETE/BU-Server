# IntelliJ IDEA 설정 가이드

## Code Formatter 설정

### 1. Google Java Style 적용

#### 방법 1: IntelliJ Plugin 사용 (권장)
1. `Settings` → `Plugins`
2. "google-java-format" 검색 및 설치
3. `Settings` → `Other Settings` → `google-java-format Settings`
4. "Enable google-java-format" 체크
5. IDE 재시작

#### 방법 2: Code Style XML 가져오기
1. [Google Java Style Guide XML](https://github.com/google/styleguide/blob/gh-pages/intellij-java-google-style.xml) 다운로드
2. `Settings` → `Editor` → `Code Style` → `Java`
3. 톱니바퀴 아이콘 → `Import Scheme` → `IntelliJ IDEA code style XML`
4. 다운로드한 XML 파일 선택

### 2. EditorConfig 지원 활성화
1. `Settings` → `Editor` → `Code Style`
2. "Enable EditorConfig support" 체크 (기본적으로 활성화되어 있음)

### 3. 저장 시 자동 포맷팅 (선택사항)
1. `Settings` → `Tools` → `Actions on Save`
2. "Reformat code" 체크
3. "Optimize imports" 체크

## Hot Reload 설정 (Spring DevTools)

### 1. Build Automatically 활성화
1. `Settings` → `Build, Execution, Deployment` → `Compiler`
2. "Build project automatically" 체크

### 2. Registry 설정 (선택사항)
1. `Help` → `Find Action` (Cmd+Shift+A / Ctrl+Shift+A)
2. "Registry" 입력 후 선택
3. `compiler.automake.allow.when.app.running` 검색 후 체크

## Database Tool 설정

### MySQL 연결 설정
1. `View` → `Tool Windows` → `Database`
2. `+` 버튼 → `Data Source` → `MySQL`
3. 연결 정보 입력:
   - Host: `localhost`
   - Port: `3306`
   - Database: `buildup`
   - User: `buildup`
   - Password: `buildup123`
4. `Test Connection` → `OK`

## Lombok 설정

### 1. Lombok Plugin 활성화
1. `Settings` → `Plugins`
2. "Lombok" 검색 (기본 설치되어 있음)
3. 활성화 확인

### 2. Annotation Processing 활성화
1. `Settings` → `Build, Execution, Deployment` → `Compiler` → `Annotation Processors`
2. "Enable annotation processing" 체크

## 코드 스타일 가이드

### 주요 규칙
- **들여쓰기**: 4 spaces (Java)
- **최대 줄 길이**: 100 characters
- **줄바꿈**: LF (Unix style)
- **인코딩**: UTF-8
- **Import 순서**:
  1. java.*
  2. javax.*
  3. 빈 줄
  4. org.*
  5. com.*
  6. 빈 줄
  7. 프로젝트 패키지

### 네이밍 컨벤션
- 클래스: `PascalCase`
- 메서드/변수: `camelCase`
- 상수: `UPPER_SNAKE_CASE`
- 패키지: `lowercase`

## Git 설정

### 1. Line Separator 설정
1. `Settings` → `Editor` → `Code Style`
2. "Line separator" → `Unix and macOS (\n)`

### 2. Git Commit Template (선택사항)
1. `Settings` → `Version Control` → `Commit`
2. Commit message 형식 확인

## 추천 Plugins

### 필수
- [x] Lombok
- [x] google-java-format

### 권장
- [ ] SonarLint (코드 품질)
- [ ] Rainbow Brackets (가독성)
- [ ] GitToolBox (Git 통합)
- [ ] Key Promoter X (단축키 학습)

## 문제 해결

### Hot Reload가 작동하지 않는 경우
1. DevTools 의존성 확인
2. "Build project automatically" 설정 확인
3. IDE 재시작

### Lombok이 작동하지 않는 경우
1. Lombok Plugin 활성화 확인
2. Annotation Processing 활성화 확인
3. Gradle 리로드 (`View` → `Tool Windows` → `Gradle` → 새로고침)

### Database 연결 오류
1. Docker MySQL 컨테이너 실행 확인: `docker ps`
2. 포트 충돌 확인: `lsof -i :3306`
3. 방화벽 설정 확인