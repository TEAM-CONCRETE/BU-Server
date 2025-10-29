---
description: CRUD API 전체 레이어 생성 (Entity, Repository, Service, Controller, DTO)
argument-hint: <ResourceName>
---

완전한 CRUD API를 위한 전체 레이어를 생성합니다.

**리소스 이름:** $1

**생성할 파일:**

1. **Entity** (`domain/${1}.java`)
   - JPA Entity 정의
   - Lombok 어노테이션 활용

2. **Repository** (`repository/${1}Repository.java`)
   - JpaRepository 상속

3. **DTOs** (`dto/`)
   - `dto/request/${1}CreateRequest.java`
   - `dto/request/${1}UpdateRequest.java`
   - `dto/response/${1}Response.java`

4. **Service** (`service/${1}Service.java`)
   - CRUD 메서드 구현 (create, findById, findAll, update, delete)
   - `@Transactional` 적절히 사용

5. **Controller** (`controller/${1}Controller.java`)
   - RESTful API 엔드포인트
   - `@RestController`, `@RequestMapping("/api/${1}s")`
   - CRUD 엔드포인트: POST, GET, PUT, DELETE

6. **Test** (`src/test/java/.../service/${1}ServiceTest.java`)
   - 기본 서비스 테스트

**API 응답 형식:**
```json
{
  "success": true,
  "message": "메시지",
  "data": {}
}
```

모든 클래스에 적절한 패키지와 import 문을 포함해주세요.