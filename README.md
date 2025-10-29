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

프로젝트 실행을 위해 다음 환경 변수를 설정하거나 `application.yml`을 구성해야 합니다:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/buildup?useSSL=false&serverTimezone=UTC
    username: ${DB_USERNAME:your_username}
    password: ${DB_PASSWORD:your_password}
  jpa:
    hibernate:
      ddl-auto: update  # 개발: update, 운영: validate
    show-sql: true
```

환경 변수:
- `DB_USERNAME`: 데이터베이스 사용자명
- `DB_PASSWORD`: 데이터베이스 비밀번호
- `JWT_SECRET`: JWT 시크릿 키 (필요시)

### 설치 및 실행

#### 1. 저장소 클론

```bash
git clone <repository-url>
cd buildup
```

#### 2. 실행 방법

프로젝트 실행 방법은 두 가지가 있습니다:

##### 방법 1: 개발 스크립트 사용 (권장)

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

##### 방법 2: Gradle 직접 실행

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

```
src/main/java/com/concrete/buildup/
├── BuildupApplication.java     # 메인 애플리케이션
├── config/                     # 설정 클래스 (Security, JPA, Web)
├── controller/                 # REST API 컨트롤러
├── service/                    # 비즈니스 로직
├── repository/                 # JPA Repository
├── domain/                     # Entity 클래스
├── dto/                        # DTO (request, response)
├── exception/                  # 예외 처리
└── common/                     # 공통 유틸리티, 상수
```

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

1. Entity 정의 (`domain/`)
2. Repository 작성 (`repository/`)
3. DTO 작성 (`dto/request`, `dto/response`)
4. Service 작성 (`service/`)
5. Controller 작성 (`controller/`)
6. 테스트 코드 작성

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
