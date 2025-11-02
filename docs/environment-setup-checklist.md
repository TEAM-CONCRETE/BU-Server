# 개발 환경 설정 체크리스트

## 개요

이 문서는 Build-Up 백엔드 프로젝트의 개발 환경이 올바르게 설정되었는지 확인하는 체크리스트입니다. 신규 팀원이 합류하거나 새로운 개발 환경을 설정할 때 사용합니다.

---

## 전체 체크리스트

### 1. 필수 소프트웨어 설치

- [ ] **Java 21 (LTS)**
  ```bash
  java -version
  # 출력: openjdk version "21.x.x"
  ```

- [ ] **Gradle 8.x** (또는 Gradle Wrapper 사용)
  ```bash
  ./gradlew --version
  # 출력: Gradle 8.x
  ```

- [ ] **Docker Desktop**
  ```bash
  docker --version
  # 출력: Docker version 24.x.x
  docker ps
  # 실행 중인 컨테이너 확인
  ```

- [ ] **Git**
  ```bash
  git --version
  # 출력: git version 2.x.x
  ```

- [ ] **IntelliJ IDEA** (또는 다른 Java IDE)
  - 버전: 2023.x 이상 권장

---

### 2. Docker 환경 설정

#### MySQL Container

- [ ] MySQL 8.0 컨테이너 실행 중
  ```bash
  docker ps | grep buildup-mysql
  # 출력: buildup-mysql   mysql:8.0   Up X hours (healthy)
  ```

- [ ] MySQL 접속 테스트
  ```bash
  docker exec -it buildup-mysql mysql -ubuildup -pbuildup123
  # MySQL 프롬프트 진입 확인
  ```

- [ ] 데이터베이스 존재 확인
  ```bash
  docker exec buildup-mysql mysql -ubuildup -pbuildup123 -e "SHOW DATABASES;"
  # buildup 데이터베이스 확인
  ```

- [ ] 테이블 확인 (Flyway 마이그레이션 후)
  ```bash
  docker exec buildup-mysql mysql -ubuildup -pbuildup123 buildup -e "SHOW TABLES;"
  # 17개 테이블 확인
  ```

#### phpMyAdmin (선택사항)

- [ ] phpMyAdmin 컨테이너 실행 중
  ```bash
  docker ps | grep buildup-phpmyadmin
  ```

- [ ] 웹 접속 확인
  - URL: http://localhost:8081
  - Login: `buildup` / `buildup123`

---

### 3. 환경 변수 설정

#### .env 파일 생성

- [ ] `.env` 파일이 프로젝트 루트에 존재
  ```bash
  ls -la .env
  ```

- [ ] `.env.example`을 복사하여 생성
  ```bash
  cp .env.example .env
  ```

#### 필수 환경 변수 확인

- [ ] **데이터베이스 설정**
  ```env
  SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/buildup?serverTimezone=Asia/Seoul&characterEncoding=UTF-8&useUnicode=true
  SPRING_DATASOURCE_USERNAME=buildup
  SPRING_DATASOURCE_PASSWORD=buildup123
  ```

- [ ] **JWT 설정**
  ```env
  JWT_SECRET=(64자 이상 랜덤 문자열)
  JWT_ACCESS_TOKEN_EXPIRATION=3600000
  JWT_REFRESH_TOKEN_EXPIRATION=604800000
  ```

- [ ] **AWS S3 설정** (운영 환경)
  ```env
  AWS_ACCESS_KEY_ID=your-key
  AWS_SECRET_ACCESS_KEY=your-secret
  AWS_S3_BUCKET_NAME=buildup-files
  AWS_S3_REGION=ap-northeast-2
  ```

- [ ] **FastAPI 설정**
  ```env
  FASTAPI_BASE_URL=http://localhost:8000
  FASTAPI_TIMEOUT=30000
  ```

---

### 4. IntelliJ IDEA 설정

#### 기본 설정

- [ ] **Project SDK**: Java 21
  - `File` → `Project Structure` → `Project` → `SDK: 21`

- [ ] **Gradle JVM**: Java 21
  - `Settings` → `Build, Execution, Deployment` → `Build Tools` → `Gradle` → `Gradle JVM: 21`

#### Code Formatter

- [ ] **Google Java Style** 플러그인 설치
  - `Settings` → `Plugins` → "google-java-format" 설치

- [ ] **EditorConfig 지원** 활성화
  - `Settings` → `Editor` → `Code Style` → "Enable EditorConfig support"

- [ ] **저장 시 자동 포맷팅** (선택사항)
  - `Settings` → `Tools` → `Actions on Save`
  - "Reformat code" 체크
  - "Optimize imports" 체크

#### Lombok

- [ ] **Lombok 플러그인** 활성화
  - `Settings` → `Plugins` → "Lombok" 확인

- [ ] **Annotation Processing** 활성화
  - `Settings` → `Build, Execution, Deployment` → `Compiler` → `Annotation Processors`
  - "Enable annotation processing" 체크

#### Hot Reload (DevTools)

- [ ] **Build Automatically** 활성화
  - `Settings` → `Build, Execution, Deployment` → `Compiler`
  - "Build project automatically" 체크

- [ ] **Registry 설정** (선택사항)
  - `Help` → `Find Action` → "Registry"
  - `compiler.automake.allow.when.app.running` 체크

#### Database Tool

- [ ] **MySQL 연결** 설정
  - `View` → `Tool Windows` → `Database`
  - `+` → `Data Source` → `MySQL`
  - Host: `localhost`, Port: `3306`, Database: `buildup`
  - User: `buildup`, Password: `buildup123`
  - `Test Connection` 성공 확인

---

### 5. 프로젝트 빌드 및 실행

#### Gradle 빌드

- [ ] **Clean 빌드 성공**
  ```bash
  ./gradlew clean build
  # BUILD SUCCESSFUL 확인
  ```

- [ ] **빌드 시간 확인**
  ```bash
  # 예상 시간: 10~30초 (첫 빌드는 더 오래 걸릴 수 있음)
  ```

- [ ] **빌드 아티팩트 확인**
  ```bash
  ls -lh build/libs/
  # buildup-0.0.1-SNAPSHOT.jar 파일 확인
  ```

#### 테스트 실행

- [ ] **모든 테스트 통과**
  ```bash
  ./gradlew test
  # BUILD SUCCESSFUL 확인
  # 모든 테스트 PASSED 확인
  ```

- [ ] **테스트 리포트 확인**
  ```bash
  open build/reports/tests/test/index.html
  # 테스트 결과 HTML 확인
  ```

#### 애플리케이션 실행

- [ ] **개발 환경 실행 (start-dev.sh)**
  ```bash
  ./start-dev.sh
  # Docker 컨테이너 시작
  # 애플리케이션 시작
  # "Started BuildupApplication" 로그 확인
  ```

- [ ] **직접 실행 (Gradle)**
  ```bash
  ./gradlew bootRun
  # Started BuildupApplication in X seconds
  ```

- [ ] **포트 확인**
  ```bash
  lsof -i :8080
  # 애플리케이션이 8080 포트 사용 확인
  ```

---

### 6. API 엔드포인트 확인

#### Swagger UI

- [ ] **Swagger UI 접속**
  - URL: http://localhost:8080/api/swagger-ui/index.html
  - 화면 로딩 확인

- [ ] **API 문서 확인**
  - 등록된 엔드포인트 목록 확인

#### Health Check

- [ ] **Actuator Health 확인**
  ```bash
  curl http://localhost:8080/api/actuator/health
  # 출력: {"status":"UP"}
  ```

#### 기본 엔드포인트 테스트 (구현 후)

- [ ] **로그인 API 테스트** (구현 후)
  ```bash
  curl -X POST http://localhost:8080/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"userId": "admin", "password": "admin123"}'
  # 응답 확인
  ```

---

### 7. Flyway 마이그레이션 확인

#### 마이그레이션 실행 확인

- [ ] **Flyway 히스토리 확인**
  ```bash
  docker exec buildup-mysql mysql -ubuildup -pbuildup123 buildup \
    -e "SELECT * FROM flyway_schema_history;"
  # V1__init_schema.sql 마이그레이션 성공 확인
  ```

- [ ] **테이블 생성 확인**
  ```bash
  docker exec buildup-mysql mysql -ubuildup -pbuildup123 buildup \
    -e "SHOW TABLES;"
  # 17개 테이블 확인:
  # attendances, contract_details, contracts, corporations,
  # employees, managers, payrolls, roles, safety_doc_attendees,
  # safety_docs, safety_sign_logs, sites, users,
  # work_report_employees, work_report_materials, work_reports
  ```

- [ ] **기본 데이터 확인**
  ```bash
  docker exec buildup-mysql mysql -ubuildup -pbuildup123 buildup \
    -e "SELECT * FROM roles;"
  # ROLE_ADMIN, ROLE_MANAGER, ROLE_EMPLOYEE, ROLE_CORPORATION 확인
  ```

#### Flyway Gradle 명령어 테스트

- [ ] **Flyway Info**
  ```bash
  ./gradlew flywayInfo
  # 마이그레이션 상태 확인
  ```

- [ ] **Flyway Validate**
  ```bash
  ./gradlew flywayValidate
  # SUCCESS 확인
  ```

---

### 8. Git 설정

#### Git Config

- [ ] **사용자 정보 설정**
  ```bash
  git config user.name
  git config user.email
  # 본인 이름과 이메일 확인
  ```

- [ ] **Line Ending 설정**
  ```bash
  git config core.autocrlf
  # Mac/Linux: input 또는 false
  # Windows: true
  ```

#### Branch 확인

- [ ] **현재 브랜치**
  ```bash
  git branch
  # * develop (기본 개발 브랜치)
  ```

- [ ] **원격 연결 확인**
  ```bash
  git remote -v
  # origin 확인
  ```

---

### 9. 문서 확인

#### 필수 문서 존재 확인

- [ ] **README.md**: 프로젝트 개요 및 시작 가이드
- [ ] **.env.example**: 환경 변수 템플릿
- [ ] **CONTRIBUTING.md**: 개발 가이드 (STEP 7에서 생성)
- [ ] **.editorconfig**: 코드 포맷팅 규칙
- [ ] **.gitignore**: Git 제외 파일 목록

#### docs/ 디렉토리 문서

- [ ] **branching-strategy.md**: Git 브랜치 전략
- [ ] **commit-conventions.md**: 커밋 메시지 규칙
- [ ] **code-review-checklist.md**: 코드 리뷰 가이드
- [ ] **merge-strategies.md**: Merge 전략
- [ ] **jira-github-workflow.md**: Jira-GitHub 워크플로우
- [ ] **testing-guide.md**: 테스트 작성 가이드
- [ ] **flyway-guide.md**: Flyway 마이그레이션 가이드
- [ ] **intellij-setup.md**: IntelliJ 설정 가이드
- [ ] **environment-setup-checklist.md**: 환경 설정 체크리스트 (본 문서)

---

### 10. 팀 협업 도구 연동

#### Jira 설정 (팀 리더)

- [ ] Jira 프로젝트 생성 (프로젝트 키: `BU`)
- [ ] GitHub for Jira 앱 설치 및 연동
- [ ] Epic, Story 템플릿 설정
- [ ] Workflow 설정 (To Do → In Progress → Done)

#### GitHub 설정 (팀 리더)

- [ ] Repository 생성
- [ ] Branch Protection Rules 설정
  - `main`: PR 필수, 리뷰 1명 이상
  - `develop`: PR 필수, 테스트 통과 필수
- [ ] Pull Request 템플릿 확인 (`.github/pull_request_template.md`)
- [ ] GitHub Actions 설정 (CI/CD) - 추후 도입

---

## 빠른 검증 스크립트

### 전체 환경 검증 스크립트

**verify-environment.sh** (생성 예정):
```bash
#!/bin/bash

echo "=== Build-Up 개발 환경 검증 ==="
echo ""

# Java 버전 확인
echo "1. Java 버전 확인"
java -version 2>&1 | grep "openjdk version \"21"
if [ $? -eq 0 ]; then
  echo "✅ Java 21 설치 확인"
else
  echo "❌ Java 21이 설치되지 않았습니다"
fi
echo ""

# Docker 확인
echo "2. Docker 확인"
docker ps --format "table {{.Names}}\t{{.Status}}" | grep "buildup-mysql.*Up"
if [ $? -eq 0 ]; then
  echo "✅ MySQL 컨테이너 실행 중"
else
  echo "❌ MySQL 컨테이너가 실행되지 않았습니다"
fi
echo ""

# MySQL 접속 확인
echo "3. MySQL 접속 확인"
docker exec buildup-mysql mysql -ubuildup -pbuildup123 -e "SELECT 1;" > /dev/null 2>&1
if [ $? -eq 0 ]; then
  echo "✅ MySQL 접속 성공"
else
  echo "❌ MySQL 접속 실패"
fi
echo ""

# .env 파일 확인
echo "4. 환경 변수 파일 확인"
if [ -f ".env" ]; then
  echo "✅ .env 파일 존재"
else
  echo "⚠️  .env 파일이 없습니다. .env.example을 복사하세요."
fi
echo ""

# Flyway 마이그레이션 확인
echo "5. Flyway 마이그레이션 확인"
TABLES=$(docker exec buildup-mysql mysql -ubuildup -pbuildup123 buildup -e "SHOW TABLES;" 2>/dev/null | wc -l)
if [ $TABLES -gt 10 ]; then
  echo "✅ 데이터베이스 테이블 존재 ($TABLES개)"
else
  echo "⚠️  데이터베이스 테이블이 부족합니다. Flyway 마이그레이션을 실행하세요."
fi
echo ""

# 빌드 테스트
echo "6. 빌드 테스트"
./gradlew clean build -x test > /dev/null 2>&1
if [ $? -eq 0 ]; then
  echo "✅ 빌드 성공"
else
  echo "❌ 빌드 실패. 로그를 확인하세요."
fi
echo ""

echo "=== 검증 완료 ==="
```

---

## 트러블슈팅

### 문제 1: "java: invalid source release: 21"

**원인**: IntelliJ가 잘못된 Java 버전 사용

**해결**:
1. `File` → `Project Structure` → `Project` → `SDK: 21` 설정
2. `Settings` → `Build, Execution, Deployment` → `Compiler` → `Java Compiler` → `Project bytecode version: 21`
3. `Settings` → `Build, Execution, Deployment` → `Build Tools` → `Gradle` → `Gradle JVM: 21`

---

### 문제 2: MySQL 컨테이너 시작 실패

**원인**: 포트 3306 충돌

**해결**:
```bash
# 포트 사용 확인
lsof -i :3306

# 기존 MySQL 중지
brew services stop mysql  # Homebrew로 설치된 경우

# Docker 컨테이너 재시작
docker-compose -f docker-compose.dev.yml restart
```

---

### 문제 3: Lombok이 작동 안 함

**원인**: Annotation Processing 비활성화

**해결**:
1. `Settings` → `Build, Execution, Deployment` → `Compiler` → `Annotation Processors`
2. "Enable annotation processing" 체크
3. IntelliJ 재시작

---

### 문제 4: Flyway 마이그레이션 실패

**원인**: 이전 마이그레이션 오류

**해결**:
```bash
# 1. 데이터베이스 초기화
docker exec buildup-mysql mysql -ubuildup -pbuildup123 \
  -e "DROP DATABASE IF EXISTS buildup; CREATE DATABASE buildup;"

# 2. 애플리케이션 재시작 (Flyway 자동 실행)
./gradlew bootRun
```

---

### 문제 5: 포트 8080 이미 사용 중

**원인**: 다른 애플리케이션이 8080 포트 사용

**해결**:
```bash
# 포트 사용 프로세스 확인
lsof -i :8080

# 프로세스 종료
kill -9 <PID>

# 또는 application-dev.yml에서 포트 변경
server:
  port: 8081
```

---

## 신규 팀원 온보딩 체크리스트

### Day 1: 환경 설정

- [ ] Java 21 설치
- [ ] IntelliJ IDEA 설치 및 라이선스
- [ ] Docker Desktop 설치
- [ ] Git 설치 및 SSH 키 등록
- [ ] 프로젝트 Clone
- [ ] 본 체크리스트의 1~5번 완료
- [ ] 애플리케이션 첫 실행 성공

### Day 2: 문서 학습

- [ ] README.md 읽기
- [ ] docs/ 디렉토리의 모든 문서 읽기
- [ ] 브랜치 전략 이해
- [ ] 커밋 컨벤션 이해
- [ ] Jira-GitHub 워크플로우 이해

### Day 3: 실습

- [ ] 첫 Jira Story 받기
- [ ] Feature 브랜치 생성
- [ ] 간단한 기능 구현 (예: Hello World API)
- [ ] 테스트 작성
- [ ] PR 생성
- [ ] 코드 리뷰 받기
- [ ] Merge 완료

---

## 참고 자료

- [Docker 공식 문서](https://docs.docker.com/)
- [Spring Boot 공식 문서](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [IntelliJ IDEA 도움말](https://www.jetbrains.com/help/idea/)
- [Gradle 공식 가이드](https://docs.gradle.org/)