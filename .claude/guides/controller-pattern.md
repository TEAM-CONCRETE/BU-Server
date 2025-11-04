---
description: Build-Up Platform Controller 패턴 가이드
globs: "**/*Controller.java"
alwaysApply: true
---

# Controller Pattern for Build-Up Platform

Build-Up Platform의 REST API Controller 작성 표준 가이드입니다. 일관된 API 설계와 문서화를 위해 이 패턴을 따라야 합니다.

## 목차
1. [기본 구조](#기본-구조)
2. [URL 설계 원칙](#url-설계-원칙)
3. [HTTP 메서드 사용](#http-메서드-사용)
4. [응답 형식](#응답-형식)
5. [Swagger 문서화](#swagger-문서화)
6. [도메인별 예시](#도메인별-예시)

---

## 기본 구조

### 1. Controller 클래스 구조

```java
package com.concrete.buildup.domain.{domain}.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

@Slf4j
@RestController
@RequestMapping("/api/{resource-path}")
@RequiredArgsConstructor
@Tag(name = "리소스명", description = "리소스 설명")
public class ResourceController {

    private final ResourceService resourceService;

    @GetMapping("/{id}")
    @Operation(summary = "리소스 조회", description = "ID로 리소스를 조회합니다.")
    public ResponseEntity<ApiResponse<ResourceResponse>> getResource(@PathVariable Long id) {
        log.info("[ResourceController] getResource - id={}", id);
        ResourceResponse response = resourceService.getResourceById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
```

### 2. 필수 어노테이션

#### 클래스 레벨
- `@Slf4j`: 로깅 (필수)
- `@RestController`: REST API 컨트롤러 선언
- `@RequestMapping("/api/...")`: 기본 경로 설정
- `@RequiredArgsConstructor`: 생성자 주입 (필드 주입 금지)
- `@Tag`: Swagger 문서화

#### 메서드 레벨
- `@GetMapping`, `@PostMapping`, `@PutMapping`, `@PatchMapping`, `@DeleteMapping`
- `@Operation`: Swagger 메서드 문서화
- `@ApiResponses`: 응답 코드별 설명

---

## URL 설계 원칙

Build-Up Platform은 **현장(Site) 중심** 설계를 따릅니다.

### 1. 기본 원칙

```
/api/{resource}                      # 독립 리소스 (인증, 현장)
/api/{siteId}/{resource}             # 현장 하위 리소스
```

### 2. 도메인별 URL 패턴

| 도메인 | URL 패턴 | 예시 |
|--------|----------|------|
| 인증 | `/api/auth/*` | `/api/auth/login` |
| 현장 | `/api/sites/*` | `/api/sites/1` |
| 사원 | `/api/{siteId}/employees` | `/api/1/employees` |
| 계약 | `/api/{siteId}/contracts` | `/api/1/contracts` |
| 급여 | `/api/{siteId}/payrolls` | `/api/1/payrolls` |
| 근태 | `/api/{siteId}/attendances` | `/api/1/attendances` |
| 작업일보 | `/api/{siteId}/work-reports` | `/api/1/work-reports` |
| 안전교육 | `/api/{siteId}/safety-docs` | `/api/1/safety-docs` |

### 3. 리소스 네이밍 규칙

#### ✅ 올바른 예시
```java
GET    /api/sites                    // 현장 목록 조회
POST   /api/sites/register           // 현장 등록
GET    /api/sites/{siteId}           // 현장 상세 조회
GET    /api/{siteId}/employees       // 사원 목록 조회
POST   /api/{siteId}/contracts       // 계약 생성
GET    /api/auth/me                  // 내 정보 조회
```

#### ❌ 잘못된 예시
```java
GET    /api/getSites                 // 동사 사용 금지
POST   /api/createEmployee           // 동사 사용 금지
GET    /api/employees?siteId=1       // 계층 구조 무시
```

### 4. 하위 리소스와 액션

```java
// 하위 리소스
GET    /api/{siteId}/contracts/{contractId}/pdf          // 계약서 PDF 조회

// 액션 (동사 허용되는 경우)
POST   /api/{siteId}/contracts/{contractId}/send         // 계약서 전송
POST   /api/{siteId}/contracts/{contractId}/sign         // 계약서 서명
POST   /api/{siteId}/work-reports/{reportId}/pdf         // PDF 생성 트리거
```

---

## HTTP 메서드 사용

### 1. 메서드별 용도와 응답 코드

| 메서드 | 용도 | 성공 코드 | 응답 Body |
|--------|------|-----------|-----------|
| `GET` | 조회 | 200 OK | 있음 |
| `POST` | 생성 | 201 CREATED | 있음 |
| `PUT` | 전체 수정 | 200 OK | 있음 |
| `PATCH` | 부분 수정 | 200 OK | 있음 |
| `DELETE` | 삭제 | 204 NO CONTENT | 없음 |

### 2. 메서드 선택 가이드

```java
// GET - 조회 (멱등성 O, 안전함)
@GetMapping
public ResponseEntity<ApiResponse<List<EmployeeResponse>>> getEmployees() { ... }

@GetMapping("/{id}")
public ResponseEntity<ApiResponse<EmployeeResponse>> getEmployee(@PathVariable Long id) { ... }

// POST - 생성 (멱등성 X)
@PostMapping
public ResponseEntity<ApiResponse<EmployeeResponse>> createEmployee(
        @Valid @RequestBody EmployeeCreateRequest request) { ... }

// PUT - 전체 수정 (멱등성 O)
@PutMapping("/{id}")
public ResponseEntity<ApiResponse<EmployeeResponse>> updateEmployee(
        @PathVariable Long id,
        @Valid @RequestBody EmployeeUpdateRequest request) { ... }

// PATCH - 부분 수정 (멱등성 X)
@PatchMapping("/{id}/status")
public ResponseEntity<ApiResponse<EmployeeResponse>> updateEmployeeStatus(
        @PathVariable Long id,
        @RequestBody EmployeeStatusUpdateRequest request) { ... }

// DELETE - 삭제 (멱등성 O)
@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) { ... }
```

---

## 응답 형식

### 1. ApiResponse 구조

모든 응답은 `ApiResponse<T>` 형식을 따릅니다.

```java
package com.concrete.buildup.global.common;

import lombok.Getter;
import lombok.Builder;

@Getter
@Builder
public class ApiResponse<T> {
    private final boolean success;
    private final String message;
    private final T data;

    // 성공 응답
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    // 에러 응답 (GlobalExceptionHandler에서 사용)
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .build();
    }
}
```

### 2. 응답 예시

#### 성공 응답 (200 OK)
```json
{
  "success": true,
  "message": "사원 조회에 성공했습니다",
  "data": {
    "employeeId": 1,
    "employeeName": "홍길동",
    "phone": "010-1234-5678"
  }
}
```

#### 에러 응답 (404 NOT FOUND)
```json
{
  "success": false,
  "message": "사원을 찾을 수 없습니다",
  "data": null
}
```

#### Validation 에러 (400 BAD REQUEST)
```json
{
  "success": false,
  "message": "필드 검증에 실패했습니다",
  "data": {
    "employeeName": "사원 이름은 필수입니다",
    "phone": "전화번호 형식이 올바르지 않습니다"
  }
}
```

### 3. ResponseEntity 사용

```java
// 200 OK
return ResponseEntity.ok(ApiResponse.success(data));

// 201 CREATED
return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(ApiResponse.success(data, "생성되었습니다"));

// 204 NO CONTENT
return ResponseEntity.noContent().build();

// 400 BAD REQUEST (GlobalExceptionHandler가 처리)
throw new BusinessException(ErrorCode.INVALID_INPUT);

// 404 NOT FOUND (GlobalExceptionHandler가 처리)
throw new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND);
```

---

## Swagger 문서화

### 1. OpenAPI 의존성

```gradle
// build.gradle
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.2.0'
```

### 2. 클래스 레벨 문서화

```java
@Tag(name = "Employee Management", description = "사원 관리 API")
@RestController
@RequestMapping("/api/{siteId}/employees")
public class EmployeeController {
    // ...
}
```

### 3. 메서드 레벨 문서화

```java
@GetMapping("/{id}")
@Operation(
    summary = "사원 상세 조회",
    description = "사원 ID로 특정 사원의 상세 정보를 조회합니다."
)
@ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "조회 성공",
        content = @Content(schema = @Schema(implementation = EmployeeResponse.class))
    ),
    @ApiResponse(
        responseCode = "404",
        description = "사원을 찾을 수 없음",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
})
public ResponseEntity<ApiResponse<EmployeeResponse>> getEmployee(
        @Parameter(description = "현장 ID", required = true, example = "1")
        @PathVariable Long siteId,
        @Parameter(description = "사원 ID", required = true, example = "1")
        @PathVariable Long id) {
    // ...
}
```

### 4. Request Body 문서화

```java
@PostMapping
@Operation(summary = "사원 등록", description = "새로운 사원을 등록합니다.")
public ResponseEntity<ApiResponse<EmployeeResponse>> createEmployee(
        @Parameter(description = "현장 ID", required = true)
        @PathVariable Long siteId,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "사원 등록 정보",
            required = true,
            content = @Content(schema = @Schema(implementation = EmployeeCreateRequest.class))
        )
        @Valid @RequestBody EmployeeCreateRequest request) {
    // ...
}
```

---

## 도메인별 예시

### 1. AuthController (인증)

```java
package com.concrete.buildup.domain.auth.controller;

import com.concrete.buildup.domain.auth.dto.*;
import com.concrete.buildup.domain.auth.service.AuthService;
import com.concrete.buildup.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 인증/인가 컨트롤러
 * - 회원가입, 로그인, 토큰 관리
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "인증/인가 API")
public class AuthController {

    private final AuthService authService;

    /**
     * 근로자 회원가입
     */
    @PostMapping("/register/employee")
    @Operation(
        summary = "근로자 회원가입",
        description = "근로자가 회원가입을 합니다. users + employees 동시 생성"
    )
    public ResponseEntity<ApiResponse<AuthResponse>> registerEmployee(
            @Valid @RequestBody EmployeeRegisterRequest request) {
        log.info("[AuthController] registerEmployee - userId={}", request.getUserId());
        AuthResponse response = authService.registerEmployee(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "회원가입에 성공했습니다."));
    }

    /**
     * 현장 관리자 회원가입
     */
    @PostMapping("/register/manager")
    @Operation(
        summary = "현장 관리자 회원가입",
        description = "현장 관리자가 시크릿 키를 사용하여 회원가입을 합니다."
    )
    public ResponseEntity<ApiResponse<AuthResponse>> registerManager(
            @Valid @RequestBody ManagerRegisterRequest request) {
        log.info("[AuthController] registerManager - userId={}", request.getUserId());
        AuthResponse response = authService.registerManager(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "회원가입에 성공했습니다."));
    }

    /**
     * 로그인
     */
    @PostMapping("/login")
    @Operation(summary = "로그인", description = "사용자 로그인 후 JWT 토큰을 발급합니다.")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        log.info("[AuthController] login - username={}", request.getUsername());
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response, "로그인에 성공했습니다."));
    }

    /**
     * 토큰 재발급
     */
    @PostMapping("/token/refresh")
    @Operation(summary = "토큰 재발급", description = "Refresh Token으로 Access Token을 재발급합니다.")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(
            @Valid @RequestBody TokenRefreshRequest request) {
        log.info("[AuthController] refreshToken");
        TokenResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 내 정보 조회 (로그인 후 라우팅 분기용)
     */
    @GetMapping("/me")
    @Operation(
        summary = "내 정보 조회",
        description = "로그인한 사용자의 정보를 조회합니다. (라우팅 분기용)"
    )
    public ResponseEntity<ApiResponse<UserInfoResponse>> getMe(
            @RequestHeader("Authorization") String authHeader) {
        log.info("[AuthController] getMe");
        UserInfoResponse response = authService.getMyInfo(authHeader);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 아이디 중복 확인
     */
    @GetMapping("/exists")
    @Operation(summary = "아이디 중복 확인", description = "회원가입 시 아이디 중복을 확인합니다.")
    public ResponseEntity<ApiResponse<Boolean>> checkUserIdExists(
            @RequestParam String userId) {
        log.info("[AuthController] checkUserIdExists - userId={}", userId);
        boolean exists = authService.isUserIdExists(userId);
        return ResponseEntity.ok(ApiResponse.success(exists));
    }
}
```

### 2. EmployeeController (사원 관리)

```java
package com.concrete.buildup.domain.employee.controller;

import com.concrete.buildup.domain.employee.dto.*;
import com.concrete.buildup.domain.employee.service.EmployeeService;
import com.concrete.buildup.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 사원 관리 컨트롤러
 * - 현장별 사원 목록, 검색
 */
@Slf4j
@RestController
@RequestMapping("/api/{siteId}/employees")
@RequiredArgsConstructor
@Tag(name = "Employee", description = "사원 관리 API")
public class EmployeeController {

    private final EmployeeService employeeService;

    /**
     * 사원 목록 조회
     */
    @GetMapping
    @Operation(
        summary = "사원 목록 조회",
        description = "현장별 사원 전체 목록을 조회합니다. 필터링 및 페이징 지원"
    )
    public ResponseEntity<ApiResponse<List<EmployeeResponse>>> getEmployees(
            @Parameter(description = "현장 ID", required = true, example = "1")
            @PathVariable Long siteId,
            @Parameter(description = "사원 유형 (정규/일용)", example = "정규")
            @RequestParam(required = false) String type,
            @Parameter(description = "검색어", example = "홍길동")
            @RequestParam(required = false) String search,
            @Parameter(description = "입사일 시작", example = "2024-01-01")
            @RequestParam(required = false) String joinedFrom,
            @Parameter(description = "입사일 종료", example = "2024-12-31")
            @RequestParam(required = false) String joinedTo,
            @Parameter(description = "정렬 기준", example = "name")
            @RequestParam(required = false, defaultValue = "name") String sort) {
        log.info("[EmployeeController] getEmployees - siteId={}, type={}, search={}",
                 siteId, type, search);

        EmployeeSearchCriteria criteria = EmployeeSearchCriteria.builder()
                .siteId(siteId)
                .type(type)
                .search(search)
                .joinedFrom(joinedFrom)
                .joinedTo(joinedTo)
                .sort(sort)
                .build();

        List<EmployeeResponse> employees = employeeService.getEmployees(criteria);
        return ResponseEntity.ok(ApiResponse.success(employees));
    }

    /**
     * 사원 검색
     */
    @GetMapping("/search")
    @Operation(
        summary = "사원 검색",
        description = "사원 이름으로 검색합니다."
    )
    public ResponseEntity<ApiResponse<List<EmployeeResponse>>> searchEmployees(
            @Parameter(description = "현장 ID", required = true)
            @PathVariable Long siteId,
            @Parameter(description = "사원 이름", required = true, example = "홍길동")
            @RequestParam String empName) {
        log.info("[EmployeeController] searchEmployees - siteId={}, empName={}", siteId, empName);
        List<EmployeeResponse> employees = employeeService.searchByName(siteId, empName);
        return ResponseEntity.ok(ApiResponse.success(employees));
    }

    /**
     * 사원 상세 조회
     */
    @GetMapping("/{employeeId}")
    @Operation(summary = "사원 상세 조회", description = "사원 ID로 상세 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<EmployeeDetailResponse>> getEmployee(
            @PathVariable Long siteId,
            @PathVariable Long employeeId) {
        log.info("[EmployeeController] getEmployee - siteId={}, employeeId={}", siteId, employeeId);
        EmployeeDetailResponse employee = employeeService.getEmployeeDetail(siteId, employeeId);
        return ResponseEntity.ok(ApiResponse.success(employee));
    }
}
```

### 3. ContractController (계약 관리)

```java
package com.concrete.buildup.domain.contract.controller;

import com.concrete.buildup.domain.contract.dto.*;
import com.concrete.buildup.domain.contract.service.ContractService;
import com.concrete.buildup.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 계약 관리 컨트롤러
 * - 근로계약서 생성, 조회, 서명
 */
@Slf4j
@RestController
@RequestMapping("/api/{siteId}/contracts")
@RequiredArgsConstructor
@Tag(name = "Contract", description = "계약 관리 API")
public class ContractController {

    private final ContractService contractService;

    /**
     * 계약 목록 조회
     */
    @GetMapping
    @Operation(
        summary = "계약 목록 조회",
        description = "현장별 계약서 목록을 조회합니다. 현장 관리자는 전체, 근로자는 본인 계약만 조회"
    )
    public ResponseEntity<ApiResponse<List<ContractResponse>>> getContracts(
            @PathVariable Long siteId,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) String empType,
            @RequestParam(required = false) String status) {
        log.info("[ContractController] getContracts - siteId={}", siteId);

        ContractSearchCriteria criteria = ContractSearchCriteria.builder()
                .siteId(siteId)
                .employeeId(employeeId)
                .empType(empType)
                .status(status)
                .build();

        List<ContractResponse> contracts = contractService.getContracts(criteria);
        return ResponseEntity.ok(ApiResponse.success(contracts));
    }

    /**
     * 계약 상세 조회
     */
    @GetMapping("/{contractId}")
    @Operation(summary = "계약 상세 조회", description = "계약서 상세 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<ContractDetailResponse>> getContract(
            @PathVariable Long siteId,
            @PathVariable Long contractId) {
        log.info("[ContractController] getContract - contractId={}", contractId);
        ContractDetailResponse contract = contractService.getContractDetail(contractId);
        return ResponseEntity.ok(ApiResponse.success(contract));
    }

    /**
     * 상용직 계약 생성
     */
    @PostMapping("/permanent")
    @Operation(summary = "상용직 계약 생성", description = "상용직 근로계약서 초안을 생성합니다.")
    public ResponseEntity<ApiResponse<ContractResponse>> createPermanentContract(
            @PathVariable Long siteId,
            @Valid @RequestBody PermanentContractCreateRequest request) {
        log.info("[ContractController] createPermanentContract - siteId={}", siteId);
        ContractResponse contract = contractService.createPermanentContract(siteId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(contract, "계약서가 생성되었습니다."));
    }

    /**
     * 계약서 서명 요청
     */
    @PostMapping("/{contractId}/send")
    @Operation(
        summary = "계약서 서명 요청",
        description = "근로자에게 전자서명 요청을 보냅니다. 상태가 SENT로 변경됩니다."
    )
    public ResponseEntity<ApiResponse<Void>> sendContract(
            @PathVariable Long siteId,
            @PathVariable Long contractId,
            @Valid @RequestBody ContractSendRequest request) {
        log.info("[ContractController] sendContract - contractId={}", contractId);
        contractService.sendContractToEmployee(contractId, request);
        return ResponseEntity.ok(ApiResponse.success(null, "서명 요청이 전송되었습니다."));
    }

    /**
     * 계약서 전자서명 (근로자/기업)
     */
    @PostMapping("/{contractId}/sign")
    @Operation(
        summary = "계약서 전자서명",
        description = "근로자 또는 기업 관리자가 계약서에 전자서명합니다."
    )
    public ResponseEntity<ApiResponse<Void>> signContract(
            @PathVariable Long siteId,
            @PathVariable Long contractId,
            @Valid @RequestBody ContractSignRequest request) {
        log.info("[ContractController] signContract - contractId={}", contractId);
        contractService.signContract(contractId, request);
        return ResponseEntity.ok(ApiResponse.success(null, "서명이 완료되었습니다."));
    }

    /**
     * 계약 상태 변경
     */
    @PatchMapping("/{contractId}/status")
    @Operation(
        summary = "계약 상태 변경",
        description = "계약을 해지(TERMINATED) 또는 취소(CANCELLED)합니다."
    )
    public ResponseEntity<ApiResponse<Void>> updateContractStatus(
            @PathVariable Long siteId,
            @PathVariable Long contractId,
            @Valid @RequestBody ContractStatusUpdateRequest request) {
        log.info("[ContractController] updateContractStatus - contractId={}, action={}",
                 contractId, request.getAction());
        contractService.updateContractStatus(contractId, request);
        return ResponseEntity.ok(ApiResponse.success(null, "계약 상태가 변경되었습니다."));
    }
}
```

---

## 추가 규칙

### 1. 로깅 규칙

```java
// 진입 로그 (필수)
log.info("[ControllerName] methodName - param1={}, param2={}", param1, param2);

// 민감 정보 마스킹 (비밀번호, 토큰 등)
log.info("[AuthController] login - username={}", maskUsername(username));

// 에러 로그 (Controller에서는 최소화, GlobalExceptionHandler에서 처리)
log.error("[ControllerName] methodName - error", e);
```

### 2. 검증 어노테이션

```java
// Request Body
@Valid @RequestBody CreateRequest request

// Path Variable
@PathVariable @Positive Long id

// Request Param
@RequestParam @NotBlank String name
@RequestParam @Min(0) int page
@RequestParam @Email String email
```

### 3. 예외 처리

```java
// Controller에서는 try-catch 사용하지 않음
// Service에서 발생한 예외는 GlobalExceptionHandler가 처리

// ❌ 잘못된 예시
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<User>> getUser(@PathVariable Long id) {
    try {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    } catch (ResourceNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(e.getMessage()));
    }
}

// ✅ 올바른 예시
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<User>> getUser(@PathVariable Long id) {
    User user = userService.getUserById(id);  // 예외 발생 시 GlobalExceptionHandler가 처리
    return ResponseEntity.ok(ApiResponse.success(user));
}
```

### 4. Controller 테스트

```java
@WebMvcTest(EmployeeController.class)
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmployeeService employeeService;

    @Test
    @DisplayName("사원 목록 조회 성공")
    void getEmployees_Success() throws Exception {
        // given
        Long siteId = 1L;
        List<EmployeeResponse> employees = List.of(/* ... */);
        when(employeeService.getEmployees(any())).thenReturn(employees);

        // when & then
        mockMvc.perform(get("/api/{siteId}/employees", siteId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }
}
```

---

## 체크리스트

### Controller 작성 시
- [ ] `@Slf4j`, `@RestController`, `@RequestMapping`, `@RequiredArgsConstructor`, `@Tag` 모두 선언
- [ ] 생성자 주입 사용 (필드 주입 금지)
- [ ] 로깅 추가 (진입 로그 필수)
- [ ] Swagger 문서화 (`@Operation`, `@ApiResponses`)
- [ ] HTTP 메서드와 응답 코드 일치 확인
- [ ] `ApiResponse<T>` 응답 형식 사용
- [ ] `@Valid` 어노테이션으로 검증
- [ ] 예외를 GlobalExceptionHandler에 위임

### 코드 리뷰 시
- [ ] URL 설계가 RESTful한지 확인
- [ ] HTTP 메서드 선택이 적절한지 확인
- [ ] 응답 코드가 일관되게 사용되었는지 확인
- [ ] Swagger 문서화가 충분한지 확인
- [ ] 로깅이 적절히 추가되었는지 확인
- [ ] 민감 정보가 마스킹되었는지 확인