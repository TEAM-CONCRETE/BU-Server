---
description: Spring Boot 기본 설정 클래스 생성 (Security, JPA, Web)
---

Spring Boot 프로젝트의 기본 설정 클래스들을 생성합니다.

**생성할 설정 클래스:**

1. **SecurityConfig.java** (`config/SecurityConfig.java`)
   - Spring Security 기본 설정
   - CORS 설정
   - CSRF 설정 (REST API용 비활성화)
   - 공개 엔드포인트 설정 (/api/auth/**, /api/public/**)
   - BCryptPasswordEncoder Bean

2. **JpaConfig.java** (`config/JpaConfig.java`)
   - JPA Auditing 활성화 (`@EnableJpaAuditing`)
   - AuditorAware 설정 (생성자/수정자 자동 저장)

3. **WebConfig.java** (`config/WebConfig.java`)
   - CORS 전역 설정
   - Interceptor 설정 준비
   - MessageConverter 설정

4. **application.yml**
   - 기본 설정 템플릿 생성
   - 데이터베이스 설정
   - JPA 설정
   - 로깅 설정

모든 설정은 Spring Boot 3.5.7 기준으로 작성하고, 주석을 포함해주세요.