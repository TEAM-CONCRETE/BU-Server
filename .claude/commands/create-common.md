---
description: 공통 클래스 생성 (BaseEntity, ApiResponse, ErrorResponse, GlobalExceptionHandler)
---

프로젝트 전반에서 사용할 공통 클래스들을 생성합니다.

**생성할 클래스:**

1. **BaseEntity.java** (`domain/BaseEntity.java`)
   - 모든 Entity의 공통 필드 정의
   - `id` (Long, @Id, @GeneratedValue)
   - `createdAt` (LocalDateTime, @CreatedDate)
   - `updatedAt` (LocalDateTime, @LastModifiedDate)
   - `@MappedSuperclass` 사용

2. **ApiResponse.java** (`common/dto/ApiResponse.java`)
   - 성공 응답 래퍼 클래스
   - 제네릭 타입으로 data 포함
   - static 팩토리 메서드 (success, fail)
   ```java
   {
     "success": true,
     "message": "메시지",
     "data": T
   }
   ```

3. **ErrorResponse.java** (`common/dto/ErrorResponse.java`)
   - 에러 응답 DTO
   - errorCode, message, timestamp 필드

4. **GlobalExceptionHandler.java** (`exception/GlobalExceptionHandler.java`)
   - `@RestControllerAdvice` 사용
   - 공통 예외 처리 (RuntimeException, IllegalArgumentException 등)
   - EntityNotFoundException 처리
   - MethodArgumentNotValidException 처리 (Validation 에러)

5. **커스텀 예외** (`exception/custom/`)
   - `EntityNotFoundException.java`
   - `BusinessException.java`
   - `UnauthorizedException.java`

모든 클래스에 Lombok을 적절히 활용하고, JavaDoc 주석을 포함해주세요.