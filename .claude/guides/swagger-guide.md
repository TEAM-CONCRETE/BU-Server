# Swagger/OpenAPI 가이드

## 📋 목차
1. [개요](#개요)
2. [설정 방법](#설정-방법)
3. [Controller 문서화](#controller-문서화)
4. [DTO 문서화](#dto-문서화)
5. [보안 설정](#보안-설정)
6. [실전 예시](#실전-예시)
7. [베스트 프랙티스](#베스트-프랙티스)

---

## 개요

### Swagger란?
- REST API를 설계, 빌드, 문서화, 사용하기 위한 오픈소스 프레임워크
- SpringDoc OpenAPI를 사용하여 Spring Boot 3.x와 호환

### 주요 기능
- **자동 API 문서 생성**: 코드에서 자동으로 API 문서 생성
- **API 테스트**: Swagger UI를 통한 실시간 API 테스트
- **명세서 표준화**: OpenAPI 3.0 표준 준수

### 접속 URL
- **Swagger UI**: `http://localhost:8080/api/swagger-ui/index.html`
- **OpenAPI JSON**: `http://localhost:8080/api/v3/api-docs`
- **OpenAPI YAML**: `http://localhost:8080/api/v3/api-docs.yaml`

---

## 설정 방법

### 1. 의존성 추가

```gradle
// build.gradle
dependencies {
    // SpringDoc OpenAPI (Swagger UI 포함)
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.2.0'
}
```

### 2. application.yml 설정

```yaml
# application.yml
springdoc:
  api-docs:
    path: /v3/api-docs
    enabled: true
  swagger-ui:
    path: /swagger-ui.html
    enabled: true
    operations-sorter: method  # HTTP 메서드별 정렬
    tags-sorter: alpha         # 태그 알파벳 순 정렬
    display-request-duration: true
    doc-expansion: none        # 기본적으로 접혀있음
  show-actuator: false
  default-consumes-media-type: application/json
  default-produces-media-type: application/json
```

### 3. Swagger 설정 클래스

```java
package com.concrete.buildup.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger/OpenAPI 설정
 */
@Configuration
public class SwaggerConfig {

    @Value("${spring.application.name:Build-Up Platform}")
    private String applicationName;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(servers())
                .components(securitySchemes())
                .addSecurityItem(securityRequirement());
    }

    /**
     * API 기본 정보
     */
    private Info apiInfo() {
        return new Info()
                .title(applicationName + " API")
                .description("Build-Up Platform REST API 문서")
                .version("v1.0.0")
                .contact(new Contact()
                        .name("TEAM CONCRETE")
                        .email("support@build-up.kr")
                        .url("https://build-up.kr"))
                .license(new License()
                        .name("Apache 2.0")
                        .url("https://www.apache.org/licenses/LICENSE-2.0.html"));
    }

    /**
     * 서버 정보
     */
    private List<Server> servers() {
        Server devServer = new Server()
                .url("http://localhost:8080/api")
                .description("개발 서버");

        Server prodServer = new Server()
                .url("https://api.build-up.kr")
                .description("운영 서버");

        return List.of(devServer, prodServer);
    }

    /**
     * 보안 스키마 설정
     */
    private Components securitySchemes() {
        return new Components()
                .addSecuritySchemes("Bearer Authentication",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT 토큰을 입력하세요 (Bearer 제외)"))
                .addSecuritySchemes("Basic Authentication",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("basic")
                                .description("사용자명과 비밀번호"));
    }

    /**
     * 보안 요구사항
     */
    private SecurityRequirement securityRequirement() {
        return new SecurityRequirement()
                .addList("Bearer Authentication")
                .addList("Basic Authentication");
    }
}
```

### 4. Security 설정 업데이트

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            // Swagger 접근 허용
            .requestMatchers(
                "/api/swagger-ui/**",
                "/api/v3/api-docs/**",
                "/api/swagger-ui.html"
            ).permitAll()
            // ...
        );
    return http.build();
}
```

---

## Controller 문서화

### 1. 기본 Controller 어노테이션

```java
package com.concrete.buildup.domain.employee.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 사원 관리 API
 */
@Slf4j
@RestController
@RequestMapping("/v1/employees")
@RequiredArgsConstructor
@Tag(name = "Employee", description = "사원 관리 API")
@SecurityRequirement(name = "Bearer Authentication")
public class EmployeeController {

    private final EmployeeService employeeService;

    /**
     * 사원 목록 조회
     */
    @GetMapping
    @Operation(
        summary = "사원 목록 조회",
        description = "현장별 사원 전체 목록을 조회합니다. 필터링 및 페이징을 지원합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = EmployeeListResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 요청",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "인증 실패"
        )
    })
    public ResponseEntity<ApiResponse<List<EmployeeResponse>>> getEmployees(
            @Parameter(description = "현장 ID", required = true, example = "1")
            @PathVariable Long siteId,

            @Parameter(description = "사원 유형", example = "정규")
            @RequestParam(required = false) String type,

            @Parameter(description = "검색어 (이름)", example = "홍길동")
            @RequestParam(required = false) String search) {

        List<EmployeeResponse> employees = employeeService.getEmployees(siteId, type, search);
        return ResponseEntity.ok(ApiResponse.success(employees));
    }
}
```

### 2. POST 요청 문서화

```java
@PostMapping
@Operation(
    summary = "사원 등록",
    description = "새로운 사원을 등록합니다. 중복된 전화번호는 등록할 수 없습니다."
)
@ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "등록 성공",
        content = @Content(schema = @Schema(implementation = EmployeeResponse.class))
    ),
    @ApiResponse(
        responseCode = "400",
        description = "잘못된 요청 (Validation 실패)",
        content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))
    ),
    @ApiResponse(
        responseCode = "409",
        description = "중복된 전화번호",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
})
public ResponseEntity<ApiResponse<EmployeeResponse>> createEmployee(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "사원 등록 정보",
            required = true,
            content = @Content(schema = @Schema(implementation = EmployeeCreateRequest.class))
        )
        @Valid @RequestBody EmployeeCreateRequest request) {

    EmployeeResponse employee = employeeService.createEmployee(request);
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(employee, "사원이 등록되었습니다."));
}
```

### 3. 공통 응답 모델

```java
/**
 * API 공통 응답 형식
 */
@Schema(description = "API 공통 응답")
public class ApiResponse<T> {

    @Schema(description = "성공 여부", example = "true")
    private boolean success;

    @Schema(description = "응답 메시지", example = "요청이 성공적으로 처리되었습니다")
    private String message;

    @Schema(description = "응답 데이터")
    private T data;
}
```

---

## DTO 문서화

### 1. Request DTO

```java
package com.concrete.buildup.domain.employee.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * 사원 등록 요청 DTO
 */
@Schema(description = "사원 등록 요청")
public class EmployeeCreateRequest {

    @Schema(
        description = "사원 이름",
        example = "홍길동",
        requiredMode = Schema.RequiredMode.REQUIRED,
        minLength = 2,
        maxLength = 50
    )
    @NotBlank(message = "이름은 필수입니다")
    @Size(min = 2, max = 50, message = "이름은 2-50자 사이여야 합니다")
    private String name;

    @Schema(
        description = "전화번호 (형식: 010-1234-5678)",
        example = "010-1234-5678",
        requiredMode = Schema.RequiredMode.REQUIRED,
        pattern = "^01[0-9]-\\d{3,4}-\\d{4}$"
    )
    @NotBlank(message = "전화번호는 필수입니다")
    @Pattern(
        regexp = "^01[0-9]-\\d{3,4}-\\d{4}$",
        message = "전화번호 형식이 올바르지 않습니다"
    )
    private String phoneNumber;

    @Schema(
        description = "사원 유형",
        example = "PERMANENT",
        requiredMode = Schema.RequiredMode.REQUIRED,
        allowableValues = {"DAILY", "PERMANENT"}
    )
    @NotNull(message = "사원 유형은 필수입니다")
    private EmployeeType employeeType;

    @Schema(
        description = "입사일",
        example = "2024-01-01",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "입사일은 필수입니다")
    private LocalDate joinedDate;
}
```

### 2. Response DTO

```java
package com.concrete.buildup.domain.employee.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 사원 응답 DTO
 */
@Schema(description = "사원 정보")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmployeeResponse {

    @Schema(description = "사원 ID", example = "1")
    private Long employeeId;

    @Schema(description = "사원 이름", example = "홍길동")
    private String employeeName;

    @Schema(description = "전화번호", example = "010-1234-5678")
    private String phoneNumber;

    @Schema(description = "사원 유형", example = "PERMANENT")
    private String employeeType;

    @Schema(description = "입사일", example = "2024-01-01")
    private LocalDate joinedDate;

    @Schema(description = "상태", example = "ACTIVE")
    private String status;

    @Schema(description = "생성일시", example = "2024-01-01T10:00:00")
    private LocalDateTime createdAt;
}
```

### 3. Enum 문서화

```java
@Schema(description = "사원 유형")
public enum EmployeeType {

    @Schema(description = "일용직")
    DAILY("일용직"),

    @Schema(description = "상용직")
    PERMANENT("상용직");

    private final String description;

    EmployeeType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
```

---

## 보안 설정

### 1. 전역 보안 설정

```java
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement()
                        .addList("Bearer Authentication"));
    }
}
```

### 2. Controller별 보안 설정

```java
// 전체 Controller에 보안 적용
@SecurityRequirement(name = "Bearer Authentication")
@RestController
public class EmployeeController {
    // ...
}

// 특정 메서드만 보안 적용
@SecurityRequirement(name = "Bearer Authentication")
@GetMapping("/{id}")
public ResponseEntity<EmployeeResponse> getEmployee(@PathVariable Long id) {
    // ...
}

// 공개 API (보안 제외)
@Operation(security = {})
@GetMapping("/public/info")
public ResponseEntity<InfoResponse> getPublicInfo() {
    // ...
}
```

---

## 실전 예시

### 예시 1: Upload Controller

```java
package com.concrete.buildup.domain.upload.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Slf4j
@RestController
@RequestMapping("/v1/uploads")
@RequiredArgsConstructor
@Tag(name = "Upload", description = "파일 업로드 API")
@SecurityRequirement(name = "Bearer Authentication")
public class UploadController {

    private final S3Service s3Service;

    @PostMapping("/signatures")
    @Operation(
        summary = "서명 이미지 업로드를 위한 Presigned URL 발급",
        description = """
            클라이언트가 S3에 직접 서명 이미지를 업로드할 수 있는 임시 URL을 발급합니다.

            **사용 방법:**
            1. 이 API를 호출하여 Presigned URL을 발급받습니다.
            2. 발급받은 uploadUrl로 PUT 요청하여 파일을 업로드합니다.
            3. 업로드 완료 후 별도의 complete API를 호출합니다.

            **주의사항:**
            - URL은 15분간만 유효합니다.
            - 지원 파일 형식: png, jpg, jpeg, pdf
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Presigned URL 발급 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PresignedUrlResponse.class),
                examples = @ExampleObject(
                    name = "성공 응답",
                    value = """
                        {
                          "success": true,
                          "message": "Presigned URL이 발급되었습니다.",
                          "data": {
                            "uploadUrl": "https://build-up-contracts.s3.ap-northeast-2.amazonaws.com/uploads/contracts/123/EMPLOYEE.png?signature=xxx",
                            "expiresAt": "2025-11-05T14:25:00",
                            "s3Key": "uploads/contracts/123/EMPLOYEE.png",
                            "bucket": "build-up-contracts"
                          }
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 요청 (필수 파라미터 누락, 지원하지 않는 파일 형식)",
            content = @Content(
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    name = "Validation 실패",
                    value = """
                        {
                          "success": false,
                          "message": "필드 검증에 실패했습니다.",
                          "data": {
                            "fileExtension": "지원하지 않는 파일 형식입니다. (png, jpg, jpeg, pdf만 가능)"
                          }
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "인증 실패"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Presigned URL 생성 실패 (S3 연결 오류 등)"
        )
    })
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> generatePresignedUrl(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Presigned URL 발급 요청 정보",
                required = true,
                content = @Content(
                    schema = @Schema(implementation = PresignedUrlRequest.class),
                    examples = @ExampleObject(
                        name = "계약서 서명 이미지",
                        value = """
                            {
                              "resourceType": "CONTRACT",
                              "resourceId": "123",
                              "signerRole": "EMPLOYEE",
                              "fileExtension": "png"
                            }
                            """
                    )
                )
            )
            @Valid @RequestBody PresignedUrlRequest request) {

        log.info("Presigned URL 발급 요청: resourceType={}, resourceId={}",
                request.getResourceType(), request.getResourceId());

        PresignedUrlResponse response = s3Service.generatePresignedUrl(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Presigned URL이 발급되었습니다."));
    }
}
```

### 예시 2: 에러 응답 공통화

```java
/**
 * 에러 응답 DTO
 */
@Schema(description = "에러 응답")
public class ErrorResponse {

    @Schema(description = "성공 여부", example = "false")
    private boolean success;

    @Schema(description = "에러 메시지", example = "리소스를 찾을 수 없습니다")
    private String message;

    @Schema(description = "에러 상세 정보")
    private Object data;
}
```

---

## 베스트 프랙티스

### 1. 명확한 설명 작성
```java
// ❌ 나쁜 예
@Operation(summary = "조회")
public ResponseEntity<User> getUser(@PathVariable Long id)

// ✅ 좋은 예
@Operation(
    summary = "사용자 상세 조회",
    description = "사용자 ID로 특정 사용자의 상세 정보를 조회합니다. 삭제된 사용자는 조회되지 않습니다."
)
public ResponseEntity<UserResponse> getUser(
    @Parameter(description = "사용자 ID", required = true, example = "1")
    @PathVariable Long id)
```

### 2. 예제 제공
```java
@Schema(
    description = "사원 등록 요청",
    example = """
        {
          "name": "홍길동",
          "phoneNumber": "010-1234-5678",
          "employeeType": "PERMANENT",
          "joinedDate": "2024-01-01"
        }
        """
)
public class EmployeeCreateRequest {
    // ...
}
```

### 3. 응답 코드별 설명
```java
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "조회 성공"),
    @ApiResponse(responseCode = "400", description = "잘못된 요청"),
    @ApiResponse(responseCode = "401", description = "인증 실패"),
    @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음"),
    @ApiResponse(responseCode = "500", description = "서버 내부 오류")
})
```

### 4. 그룹핑
```java
// 도메인별 그룹핑
@Tag(name = "Employee", description = "사원 관리 API")
@Tag(name = "Site", description = "현장 관리 API")
@Tag(name = "Contract", description = "계약 관리 API")
```

### 5. Hidden 처리
```java
// 내부 API는 숨김 처리
@Operation(hidden = true)
@GetMapping("/internal/health")
public ResponseEntity<String> internalHealth() {
    return ResponseEntity.ok("OK");
}
```

---

## 체크리스트

### Swagger 설정 시
- [ ] SpringDoc OpenAPI 의존성 추가
- [ ] SwaggerConfig 클래스 작성
- [ ] application.yml 설정
- [ ] Security 설정에서 Swagger URL 허용
- [ ] 서버 정보 설정 (dev, prod)
- [ ] 보안 스키마 설정

### Controller 문서화 시
- [ ] @Tag 어노테이션 추가
- [ ] @Operation으로 메서드 설명
- [ ] @ApiResponses로 응답 코드 정의
- [ ] @Parameter로 파라미터 설명
- [ ] 예제 값 제공

### DTO 문서화 시
- [ ] @Schema 어노테이션 추가
- [ ] description 작성
- [ ] example 값 제공
- [ ] requiredMode 설정
- [ ] allowableValues 정의 (Enum)

---

## 참고 자료

- [SpringDoc 공식 문서](https://springdoc.org/)
- [OpenAPI 3.0 Specification](https://swagger.io/specification/)
- [Swagger Annotations Guide](https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---Annotations)

---

이 가이드를 따라 일관된 API 문서를 작성하시기 바랍니다.