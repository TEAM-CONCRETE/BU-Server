# Build-Up Backend 개발 환경 설정 가이드

이 문서는 Build-Up Platform 백엔드 서비스의 로컬 개발 환경을 설정하는 방법을 설명합니다.

## 📋 사전 요구사항

개발을 시작하기 전에 다음 소프트웨어가 설치되어 있어야 합니다:

- **Java 21** (LTS)
- **Docker Desktop** (MySQL, phpMyAdmin 실행용)
- **Git**
- **IDE** (IntelliJ IDEA 또는 VS Code 권장)

### Java 설치 확인
```bash
java -version
# java version "21.x.x" 확인
```

### Docker 설치 확인
```bash
docker --version
docker-compose --version
```

## 🚀 빠른 시작

### 1. 프로젝트 클론
```bash
git clone <repository-url>
cd buildup
```

### 2. 실행 권한 부여 (Unix/Mac)
```bash
chmod +x start-dev.sh
chmod +x gradlew
```

### 3. 개발 환경 시작
```bash
./start-dev.sh
```

이 스크립트는 다음 작업을 자동으로 수행합니다:
- Docker를 통한 MySQL 및 phpMyAdmin 시작
- MySQL 연결 대기 및 확인
- Spring Boot 애플리케이션을 `dev` 프로파일로 실행

### 4. 서비스 접속

개발 환경이 시작되면 다음 주소로 접속할 수 있습니다:

| 서비스 | URL | 설명 |
|--------|-----|------|
| **API Server** | http://localhost:8080/api | REST API 엔드포인트 |
| **Swagger UI** | http://localhost:8080/api/swagger-ui.html | API 문서 (SpringDoc 추가 후) |
| **phpMyAdmin** | http://localhost:8081 | 데이터베이스 관리 도구 |

## 🗄️ 데이터베이스 정보

### 개발 환경 (docker-compose.dev.yml)

```yaml
Host: localhost
Port: 3306
Database: buildup
Username: buildup
Password: buildup123
Root Password: root
```

### phpMyAdmin 로그인
- URL: http://localhost:8081
- 서버: mysql
- 사용자명: `buildup` 또는 `root`
- 비밀번호: `buildup123` 또는 `root`

## 🛠️ 수동 설정 (선택사항)

자동 스크립트 대신 수동으로 실행하려면:

### 1. Docker 서비스만 시작
```bash
docker-compose -f docker-compose.dev.yml up -d
```

### 2. Spring Boot 애플리케이션 실행
```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

또는 IDE에서:
- IntelliJ: Run Configuration → Active profiles: `dev`
- VS Code: `launch.json`에 `"args": "--spring.profiles.active=dev"` 추가

## 📁 프로젝트 구조

```
buildup/
├── src/
│   ├── main/
│   │   ├── java/com/concrete/buildup/
│   │   └── resources/
│   │       ├── application.properties        # 기본 설정
│   │       └── application-dev.yml           # 개발 환경 설정
│   └── test/
├── docker-compose.dev.yml                    # 개발용 Docker Compose
├── docker-compose.yml                        # 프로덕션용 Docker Compose
├── start-dev.sh                              # 개발 환경 시작 스크립트
└── .claude/                                  # Claude Code 설정
    ├── CLAUDE.md                             # 프로젝트 가이드
    └── commands/                             # 커스텀 슬래시 명령어
```

## 🔧 환경별 프로파일

### 개발 (dev)
- 파일: `application-dev.yml`
- JPA ddl-auto: `update` (자동 스키마 업데이트)
- 상세 로깅 활성화
- Swagger UI 활성화

### 운영 (prod)
- 파일: `application-prod.yml` (추가 필요)
- JPA ddl-auto: `validate` (변경 불가)
- 최소 로깅
- 보안 강화

## 🧪 테스트 실행

```bash
# 전체 테스트
./gradlew test

# 특정 테스트
./gradlew test --tests com.concrete.buildup.BuildupApplicationTests

# 테스트 + 빌드
./gradlew build
```

## 🐳 Docker 명령어

### 서비스 상태 확인
```bash
docker-compose -f docker-compose.dev.yml ps
```

### 로그 확인
```bash
# 전체 로그
docker-compose -f docker-compose.dev.yml logs

# MySQL 로그만
docker-compose -f docker-compose.dev.yml logs mysql

# 실시간 로그
docker-compose -f docker-compose.dev.yml logs -f
```

### 서비스 중지 및 정리
```bash
# 중지 (컨테이너 유지)
docker-compose -f docker-compose.dev.yml stop

# 중지 + 삭제
docker-compose -f docker-compose.dev.yml down

# 중지 + 삭제 + 볼륨 삭제 (데이터 완전 삭제)
docker-compose -f docker-compose.dev.yml down -v
```

### 새로 시작 (깨끗한 상태)
```bash
docker-compose -f docker-compose.dev.yml down -v
docker-compose -f docker-compose.dev.yml up -d
```

## 💡 개발 팁

### 1. Hot Reload 설정
`build.gradle`에 Spring Boot DevTools 추가:
```gradle
dependencies {
    developmentOnly 'org.springframework.boot:spring-boot-devtools'
}
```

### 2. Lombok 설정
- IntelliJ: Preferences → Plugins → Lombok 설치 → Enable annotation processing
- VS Code: Extension 'Lombok Annotations Support' 설치

### 3. 데이터베이스 초기화
초기 데이터가 필요한 경우 `docker/mysql/init/` 디렉토리에 SQL 파일을 추가하세요:
```bash
docker/mysql/init/01-schema.sql
docker/mysql/init/02-data.sql
```

### 4. Claude Code 활용
프로젝트에는 유용한 슬래시 명령어가 준비되어 있습니다:
```
/setup-config      # 기본 설정 클래스 생성
/create-common     # 공통 클래스 생성
/create-entity     # Entity + Repository 생성
/create-api        # 전체 CRUD API 생성
```

## ❗ 문제 해결

### MySQL 연결 실패
```bash
# MySQL 상태 확인
docker-compose -f docker-compose.dev.yml ps

# MySQL 재시작
docker-compose -f docker-compose.dev.yml restart mysql

# 로그 확인
docker-compose -f docker-compose.dev.yml logs mysql
```

### 포트 충돌 (3306, 8080, 8081)
다른 서비스가 해당 포트를 사용 중인 경우:
```bash
# 포트 사용 확인 (Mac/Linux)
lsof -i :3306
lsof -i :8080
lsof -i :8081

# Windows
netstat -ano | findstr :3306
```

### Gradle 빌드 실패
```bash
# Gradle 캐시 정리
./gradlew clean

# Gradle Wrapper 재설치
./gradlew wrapper
```

### Docker 용량 부족
```bash
# 사용하지 않는 이미지/컨테이너 정리
docker system prune -a

# 볼륨 정리
docker volume prune
```

## 📚 추가 리소스

- [Spring Boot 공식 문서](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [MySQL 8.0 문서](https://dev.mysql.com/doc/refman/8.0/en/)
- [Docker Compose 문서](https://docs.docker.com/compose/)
- 프로젝트 가이드: `.claude/CLAUDE.md`

## 🤝 기여 가이드

개발 시 다음 사항을 준수해주세요:
1. 브랜치 전략: `feature/*`, `bugfix/*`, `hotfix/*`
2. 커밋 메시지: 명확하고 간결하게
3. 코드 스타일: `.claude/CLAUDE.md`의 컨벤션 준수
4. 테스트: 새 기능에 대한 테스트 코드 작성

---

**문제가 발생하거나 질문이 있으면 팀에 문의하세요!** 🚀
