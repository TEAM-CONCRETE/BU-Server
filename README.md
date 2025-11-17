# Build-Up Platform

Build-Up Platform의 백엔드 서비스입니다.

## 기술 스택

- **Language**: Java 21 (LTS)
- **Framework**: Spring Boot 3.5.7
- **Database**: MySQL
- **ORM**: Spring Data JPA
- **Security**: Spring Security
- **Build Tool**: Gradle
- **Others**: Lombok

## 시작하기

### Prerequisites

- Java 21 이상
- MySQL 8.0 이상
- Gradle 8.x (또는 Gradle Wrapper 사용)

### 환경 설정

#### 1. 환경 변수 파일 생성

프로젝트 루트에 `.env` 파일을 생성하여 환경 변수를 관리합니다:

```bash
# env.example을 .env로 복사
cp env.example .env
```

`.env` 파일 예시:
```bash
# Spring 프로파일
SPRING_PROFILES_ACTIVE=prod

# JWT 및 암호화
JWT_SECRET=your-secret-key-min-256-bits-long-for-hs256-algorithm
ENCRYPTION_KEY=your_32_byte_secure_encryption_key

# 관리자 계정
ADMIN_USERNAME=admin
ADMIN_PASSWORD=your_secure_admin_password

# 데이터베이스
MYSQL_ROOT_PASSWORD=your_root_password
MYSQL_DATABASE=buildup
MYSQL_USER=buildup_user
MYSQL_PASSWORD=your_mysql_password
MYSQL_PORT=3306

# AWS S3
AWS_S3_ENABLED=true
AWS_S3_BUCKET=your-s3-bucket-name
AWS_REGION=ap-northeast-2
AWS_ACCESS_KEY_ID=your_aws_access_key_id
AWS_SECRET_ACCESS_KEY=your_aws_secret_access_key

# AI Face Similarity API (선택사항)
AI_FACE_SIMILARITY_BASE_URL=http://localhost:8000
AI_FACE_SIMILARITY_API_KEY=your_face_api_key
```

**환경 변수 생성 도구:**
```bash
# JWT Secret 생성 (32바이트 이상)
openssl rand -hex 32

# 암호화 키 생성 (32바이트)
openssl rand -hex 32
```

#### 2. AWS S3 설정 (선택사항)

파일 업로드 기능을 사용하려면 AWS S3 설정이 필요합니다.

**📘 상세 가이드:** [AWS S3 설정 가이드](docs/AWS_S3_SETUP.md)

주요 단계:
1. AWS S3 버킷 생성
2. IAM 사용자 생성 및 권한 설정
3. 액세스 키 발급
4. `.env` 파일에 자격 증명 입력

### 설치 및 실행

#### 1. 저장소 클론

```bash
git clone <repository-url>
cd buildup
```

#### 2. 실행 방법

프로젝트 실행 방법은 세 가지가 있습니다:

##### 방법 1: Docker Compose 사용 (운영 환경 권장)

Docker Compose를 사용하여 모든 서비스(애플리케이션, MySQL, Nginx)를 한 번에 실행합니다.

**Prerequisites:**
- Docker Desktop 실행 중이어야 함
- `.env` 파일 설정 완료

```bash
# 모든 서비스 시작
docker-compose up -d

# 로그 확인
docker-compose logs -f app

# 서비스 중지
docker-compose down

# 볼륨까지 삭제 (데이터베이스 초기화)
docker-compose down -v
```

**서비스 접속:**
- 애플리케이션: http://localhost:8080/api
- Swagger UI: http://localhost:8080/api/swagger-ui/index.html (dev 프로파일에서만)
- MySQL: localhost:3306
- Nginx: http://localhost (80), https://localhost (443)

##### 방법 2: 개발 스크립트 사용 (로컬 개발 권장)

Docker를 사용하여 MySQL과 phpMyAdmin을 자동으로 설정하고 애플리케이션을 실행합니다.

**Prerequisites:**
- Docker Desktop 실행 중이어야 함
- `docker-compose.dev.yml` 파일 필요

```bash
# 스크립트에 실행 권한 부여
chmod +x ./start-dev.sh

# 개발 환경 시작
./start-dev.sh
```

이 스크립트는 다음을 자동으로 수행합니다:
- Docker Compose로 MySQL과 phpMyAdmin 컨테이너 시작
- MySQL 연결 대기 및 확인
- Spring Boot 애플리케이션을 `dev` 프로파일로 실행
- 종료 시(Ctrl+C) Docker 컨테이너 정리 옵션 제공

**서비스 접속:**
- 애플리케이션: http://localhost:8080/api
- Swagger UI: http://localhost:8080/api/swagger-ui/index.html
- phpMyAdmin: http://localhost:8081

##### 방법 3: Gradle 직접 실행

로컬 MySQL을 사용하거나 Docker 없이 실행하는 경우:

```bash
# 데이터베이스 생성 (MySQL이 이미 설치되어 있어야 함)
mysql -u root -p
CREATE DATABASE buildup CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
exit;

# 애플리케이션 빌드
./gradlew build

# 애플리케이션 실행
./gradlew bootRun
```

또는 빌드된 JAR 파일 직접 실행:

```bash
java -jar build/libs/buildup-0.0.1-SNAPSHOT.jar
```

애플리케이션이 정상적으로 실행되면 `http://localhost:8080`에서 접근할 수 있습니다.

#### 3. 테스트 실행

```bash
# 모든 테스트 실행
./gradlew test

# 특정 테스트 실행
./gradlew test --tests com.concrete.buildup.BuildupApplicationTests

# 빌드 정리
./gradlew clean
```

## 프로젝트 구조

**기본 패키지:** `com.concrete.buildup`

Domain 중심 구조를 채택하여 각 도메인별로 관련 레이어를 그룹화합니다.

```
src/main/java/com/concrete/buildup/
├── domain/                     # 도메인별 기능 모듈
│   ├── auth/                   # 인증/인가 (회원가입, 로그인, 토큰)
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── dto/
│   │   └── entity/
│   ├── employee/               # 사원 관리
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── dto/
│   │   └── entity/
│   ├── site/                   # 현장 관리
│   ├── contract/               # 계약 관리
│   ├── payroll/                # 급여 관리
│   ├── attendance/             # 근태 관리
│   ├── workreport/             # 작업일보
│   └── safetydoc/              # 안전교육일지
├── global/                     # 전역 설정 및 공통 기능
│   ├── config/                 # 설정 클래스 (Security, JPA, Web)
│   ├── exception/              # 예외 처리
│   ├── common/                 # 공통 유틸리티, 상수
│   └── util/                   # 유틸리티 클래스
└── BuildupApplication.java     # 메인 애플리케이션
```

### 도메인 목록

| 도메인 | 설명 | 주요 기능 |
|--------|------|-----------|
| **auth** | 인증/인가 | 회원가입, 로그인, 토큰 관리 |
| **employee** | 사원 관리 | 사원 목록, 검색 |
| **site** | 현장 관리 | 현장 등록, 조회, 대시보드 |
| **contract** | 계약 관리 | 근로계약서 생성, 서명 |
| **payroll** | 급여 관리 | 급여 목록, 명세서 생성 |
| **attendance** | 근태 관리 | 출퇴근 기록 |
| **workreport** | 작업일보 | 작업일보 생성, PDF |
| **safetydoc** | 안전교육일지 | 안전교육 생성, 서명 |

## API 엔드포인트

> 추후 추가 예정

API 엔드포인트는 다음과 같은 형식으로 문서화될 예정입니다:

### 예시

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET    | `/api/users` | 사용자 목록 조회 | Yes |
| POST   | `/api/users` | 새 사용자 생성 | Yes |

## 주요 기능

> 추후 추가 예정

프로젝트의 주요 기능들이 여기에 문서화될 예정입니다:

- 사용자 관리
- 인증 및 권한 관리
- [추가 기능들]

## API 응답 형식

모든 API는 일관된 응답 형식을 사용합니다:

### 성공 응답

```json
{
  "success": true,
  "message": "요청이 성공적으로 처리되었습니다",
  "data": {
    // 응답 데이터
  }
}
```

### 에러 응답

```json
{
  "success": false,
  "message": "에러 메시지",
  "data": null
}
```

## CI/CD

프로젝트는 GitHub Actions를 사용한 자동화된 CI/CD 파이프라인을 제공합니다.

### CI (Continuous Integration)
- **자동 실행**: PR 생성 시, `main`/`develop` 브랜치 푸시 시
- **작업 내용**: 자동 테스트, 빌드, Docker 이미지 빌드
- **필수 설정**: 없음 (즉시 사용 가능)

### CD (Continuous Deployment)
- **자동 배포**: 
  - `main` 브랜치 → 운영 환경 (Production) 배포
  - `develop` 브랜치 → 개발 환경 (Staging) 배포
- **필수 설정**: GitHub Secrets 등록 필요

**📘 상세 가이드**: [GitHub Actions 설정 가이드](docs/github-actions-setup.md)

---

## 개발 가이드

### 코딩 컨벤션

#### 네이밍
- **클래스**: PascalCase (예: `UserService`, `OrderController`)
- **메서드/변수**: camelCase (예: `findUserById`, `userName`)
- **상수**: UPPER_SNAKE_CASE (예: `MAX_RETRY_COUNT`)

#### 레이어별 역할
- **Entity**: JPA 엔티티, `@Entity` 사용, Lombok 활용
- **Repository**: `JpaRepository` 상속, 메서드명 기반 쿼리
- **Service**: `@Service`, `@Transactional` 활용, 비즈니스 로직 구현
- **Controller**: `@RestController`, RESTful API, `ResponseEntity` 사용

### 새 기능 개발 순서

Domain 중심 구조에서의 개발 순서:

1. 도메인 패키지 생성 (`domain/{domain-name}/`)
2. Entity 정의 (`domain/{domain-name}/entity/`)
3. Repository 작성 (`domain/{domain-name}/repository/`)
4. DTO 작성 (`domain/{domain-name}/dto/`)
5. Service 작성 (`domain/{domain-name}/service/`)
6. Controller 작성 (`domain/{domain-name}/controller/`)
7. 테스트 코드 작성

**예시:** 새로운 `project` 도메인 추가 시
```
domain/project/
├── controller/
│   └── ProjectController.java
├── service/
│   └── ProjectService.java
├── repository/
│   └── ProjectRepository.java
├── dto/
│   ├── ProjectRequestDto.java
│   └── ProjectResponseDto.java
└── entity/
    └── Project.java
```

### 주의사항

1. **Spring Security**: 모든 엔드포인트는 기본적으로 인증 필요
2. **Lombok**: `@Data`, `@Builder`, `@RequiredArgsConstructor` 등 적극 활용
3. **JPA 최적화**: N+1 문제 주의, fetch join/EntityGraph 활용
4. **트랜잭션**: 읽기 전용은 `@Transactional(readOnly = true)` 사용

## 라이선스

> 라이선스 정보 추가 예정

## 기여

> 기여 가이드라인 추가 예정

## 문의

> 문의처 정보 추가 예정
