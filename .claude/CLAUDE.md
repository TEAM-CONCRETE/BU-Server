# CLAUDE.md

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

```bash
# 빌드
./gradlew build

# 애플리케이션 실행
./gradlew bootRun

# 테스트 실행
./gradlew test

# 특정 테스트 실행
./gradlew test --tests com.concrete.buildup.BuildupApplicationTests

# 빌드 정리
./gradlew clean
```

## 프로젝트 구조

**기본 패키지:** `com.concrete.buildup`

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

## 주요 컨벤션

### 레이어별 역할
- **Entity**: JPA 엔티티, `@Entity` 사용, Lombok 활용
- **Repository**: `JpaRepository` 상속, 메서드명 기반 쿼리
- **Service**: `@Service`, `@Transactional` 활용, 비즈니스 로직
- **Controller**: `@RestController`, RESTful API, ResponseEntity 사용

### 네이밍
- 클래스: PascalCase (UserService, OrderController)
- 메서드/변수: camelCase (findUserById, userName)
- 상수: UPPER_SNAKE_CASE (MAX_RETRY_COUNT)

### API 응답 형식
성공/실패 모두 일관된 형식 사용:
```json
{
  "success": true,
  "message": "메시지",
  "data": {}
}
```

## 환경 설정

### application.yml 필수 설정
- 데이터베이스 연결 정보 (MySQL)
- JPA ddl-auto: 개발(update), 운영(validate)
- Spring Security 설정

### 환경 변수
- `DB_USERNAME`: DB 사용자명
- `DB_PASSWORD`: DB 비밀번호
- `JWT_SECRET`: JWT 시크릿 키 (필요시)

## 개발 시 주의사항

1. **Spring Security 활성화**: 모든 엔드포인트는 기본적으로 인증 필요
2. **Lombok 사용**: `@Data`, `@Builder`, `@RequiredArgsConstructor` 등 적극 활용
3. **JPA 최적화**: N+1 문제 주의, fetch join/EntityGraph 활용
4. **트랜잭션**: 읽기 전용은 `@Transactional(readOnly = true)` 사용

## 새 기능 개발 순서

1. Entity 정의 (domain/)
2. Repository 작성 (repository/)
3. DTO 작성 (dto/request, dto/response)
4. Service 작성 (service/)
5. Controller 작성 (controller/)
6. 테스트 코드 작성

## 추가 리소스

자주 사용하는 작업은 `.claude/commands/`의 슬래시 명령어를 활용하세요.
