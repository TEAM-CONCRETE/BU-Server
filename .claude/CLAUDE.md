/# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

Build-Up Platform의 백엔드 서비스입니다. Spring Boot 3.5.7과 Java 21을 사용하며, JPA, Spring Security, MySQL을 핵심 기술로 사용합니다.

**기술 스택:**
- Java 21 (LTS)
- Spring Boot 3.5.7
- Spring Data JPA + MySQL
- Spring Security
- Lombok
- Gradle

## 빌드 및 실행 명령어

### Prerequisites
- Java 21 이상
- MySQL 8.0 이상
- Gradle 8.x (또는 Gradle Wrapper 사용)
- Docker Desktop (개발 스크립트 사용 시)

### 실행 방법

#### 방법 1: 개발 스크립트 사용 (권장)
Docker를 사용하여 MySQL과 phpMyAdmin을 자동으로 설정하고 애플리케이션을 실행합니다.

```bash
# 스크립트에 실행 권한 부여 (최초 1회)
chmod +x ./start-dev.sh

# 개발 환경 시작
./start-dev.sh
```

**서비스 접속:**
- 애플리케이션: http://localhost:8080/api
- Swagger UI: http://localhost:8080/api/swagger-ui/index.html
- phpMyAdmin: http://localhost:8081

#### 방법 2: Gradle 직접 실행
로컬 MySQL을 사용하거나 Docker 없이 실행하는 경우:

```bash
# 빌드
./gradlew build

# 애플리케이션 실행
./gradlew bootRun

# 빌드된 JAR 직접 실행
java -jar build/libs/buildup-0.0.1-SNAPSHOT.jar
```

### 테스트 및 빌드

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

**Domain 중심 구조**를 채택하여 각 도메인별로 관련 레이어를 그룹화합니다. 이는 중소 규모 프로젝트에서 도메인별 응집도를 높이고 유지보수를 용이하게 합니다.

```
src/main/java/com/concrete/buildup/
├── domain/                     # 도메인별 기능 모듈
│   ├── auth/                   # 인증/인가 (회원가입, 로그인, 토큰)
│   │   ├── controller/         # AuthController
│   │   ├── service/            # AuthService
│   │   ├── repository/         # UserRepository
│   │   ├── dto/                # 요청/응답 DTO
│   │   └── entity/             # User, Manager, Employee
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
│   ├── config/                 # Security, JPA, Web 설정
│   ├── exception/              # 전역 예외 처리
│   ├── common/                 # 공통 응답 포맷, BaseEntity
│   └── util/                   # 유틸리티 클래스
└── BuildupApplication.java     # 메인 애플리케이션
```

### 주요 도메인

| 도메인 | 패키지명 | 주요 기능 |
|--------|----------|-----------|
| 인증/인가 | `auth` | 회원가입, 로그인, 토큰 재발급 |
| 사원 관리 | `employee` | 사원 목록, 검색 |
| 현장 관리 | `site` | 현장 등록, 조회, 대시보드 |
| 계약 관리 | `contract` | 근로계약서 생성, 전자서명 |
| 급여 관리 | `payroll` | 급여 목록, 명세서 PDF 생성 |
| 근태 관리 | `attendance` | 출퇴근 기록, 근태 조회 |
| 작업일보 | `workreport` | 작업일보 생성, PDF |
| 안전교육 | `safetydoc` | 안전교육일지, 참석자 서명 |

## 주요 컨벤션

### Domain 중심 구조 규칙
- 각 도메인은 독립적인 패키지로 구성
- 도메인 내부에 controller, service, repository, dto, entity 포함
- 도메인 간 의존성은 service 레이어에서만 허용
- 공통 기능은 `global` 패키지에 배치

### 레이어별 역할

프로젝트는 **Rich Domain Model (DDD 패턴)**을 따릅니다.

- **Entity** (`domain/{domain}/entity/`):
  - JPA 엔티티, `@Entity` 사용, Lombok 활용
  - **도메인 로직 포함**: 엔티티 자신의 데이터를 다루는 비즈니스 로직 (상태 변경, 계산, 검증 등)
  - 예: `contract.signByCorporation()`, `contractDetail.calculateTotalPay()`

- **Repository** (`domain/{domain}/repository/`):
  - `JpaRepository` 상속, 메서드명 기반 쿼리
  - 데이터 접근 계층, 영속성 관리

- **Service** (`domain/{domain}/service/`):
  - `@Service`, `@Transactional` 활용
  - **애플리케이션 비즈니스 로직**: 여러 엔티티 조합, 외부 시스템 연동, 트랜잭션 관리
  - 엔티티의 도메인 로직을 호출하여 유스케이스 구현

- **Controller** (`domain/{domain}/controller/`):
  - `@RestController`, RESTful API, ResponseEntity 사용
  - 요청/응답 처리, DTO 변환

- **DTO** (`domain/{domain}/dto/`):
  - 요청/응답 데이터 전송 객체
  - 계층 간 데이터 전달

### 네이밍
- 클래스: PascalCase (UserService, OrderController)
- 메서드/변수: camelCase (findUserById, userName)
- 상수: UPPER_SNAKE_CASE (MAX_RETRY_COUNT)
- 패키지: 소문자, 단수형 (auth, employee, contract)

### API 응답 형식
성공/실패 모두 일관된 형식 사용:

**성공 응답:**
```json
{
  "success": true,
  "message": "요청이 성공적으로 처리되었습니다",
  "data": {
    
  }
}
```

**에러 응답:**
```json
{
  "success": false,
  "message": "에러 메시지",
  "data": null
}
```

## 환경 설정

### application.yml 필수 설정
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

### 환경 변수
- `DB_USERNAME`: 데이터베이스 사용자명
- `DB_PASSWORD`: 데이터베이스 비밀번호
- `JWT_SECRET`: JWT 시크릿 키 (필요시)

### Docker 개발 환경
- `docker-compose.dev.yml`을 사용한 MySQL과 phpMyAdmin 자동 설정
- `start-dev.sh` 스크립트로 원클릭 개발 환경 구성

## 개발 시 주의사항

1. **Spring Security 활성화**: 모든 엔드포인트는 기본적으로 인증 필요
2. **Lombok 사용**: `@Data`, `@Builder`, `@RequiredArgsConstructor` 등 적극 활용
3. **JPA 최적화**: N+1 문제 주의, fetch join/EntityGraph 활용
4. **트랜잭션**: 읽기 전용은 `@Transactional(readOnly = true)` 사용

## 새 기능 개발 순서

Domain 중심 구조에서의 개발 순서:

1. **도메인 패키지 생성**: `domain/{domain-name}/` 패키지 생성
2. **Entity 정의**: `domain/{domain-name}/entity/`에 JPA 엔티티 작성
3. **Repository 작성**: `domain/{domain-name}/repository/`에 Repository 인터페이스 작성
4. **DTO 작성**: `domain/{domain-name}/dto/`에 Request/Response DTO 작성
5. **Service 작성**: `domain/{domain-name}/service/`에 비즈니스 로직 구현
6. **Controller 작성**: `domain/{domain-name}/controller/`에 REST API 엔드포인트 작성
7. **테스트 코드 작성**: 각 레이어별 단위 테스트 및 통합 테스트

### 예시: 새로운 도메인 추가

새로운 `notification` 도메인을 추가한다면:

```
domain/notification/
├── controller/
│   └── NotificationController.java
├── service/
│   └── NotificationService.java
├── repository/
│   └── NotificationRepository.java
├── dto/
│   ├── NotificationRequestDto.java
│   └── NotificationResponseDto.java
└── entity/
    └── Notification.java
```

### 기존 도메인 확장

기존 도메인에 기능을 추가할 때는 해당 도메인 패키지 내에서 작업합니다.
예: `employee` 도메인에 퇴사 처리 기능 추가 시 `EmployeeService`에 메서드 추가

## 추가 리소스

자주 사용하는 작업은 `.claude/commands/`의 슬래시 명령어를 활용하세요.
