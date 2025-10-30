# Build-Up Platform Business Exception 개발 규칙

## 📋 목차
1. [개요](#개요)
2. [예외 처리 구조](#예외-처리-구조)
3. [도메인별 에러 코드 범위](#도메인별-에러-코드-범위)
4. [예외 타입별 사용 가이드](#예외-타입별-사용-가이드)
5. [ErrorCode 구현 규칙](#errorcode-구현-규칙)
6. [ApiResponse 사용법](#apiresponse-사용법)
7. [실제 구현 예시](#실제-구현-예시)
8. [개발 체크리스트](#개발-체크리스트)
9. [금지사항 및 권장사항](#금지사항-및-권장사항)

---

## 개요

Build-Up Platform의 비즈니스 예외 처리는 **일관성**, **명확성**, **유지보수성**을 핵심 원칙으로 합니다.

### 핵심 원칙
- 모든 비즈니스 예외는 `BusinessException` 계층구조를 따름
- 도메인별 `ErrorCode` enum으로 명확한 에러 분류
- `GlobalExceptionHandler`에서 자동으로 `ApiResponse` 변환
- HTTP 상태 코드와 비즈니스 에러 코드의 명확한 분리

---

## 예외 처리 구조

### 클래스 다이어그램

```
BaseErrorCode (interface)
    ├── CommonErrorCode (enum)
    ├── AuthErrorCode (enum)
    ├── EmployeeErrorCode (enum)
    ├── SiteErrorCode (enum)
    ├── ContractErrorCode (enum)
    ├── PayrollErrorCode (enum)
    ├── AttendanceErrorCode (enum)
    ├── WorkReportErrorCode (enum)
    └── SafetyDocErrorCode (enum)

BusinessException
    ├── ResourceNotFoundException (404)
    └── AuthorizationException (403)

GlobalExceptionHandler
    └── ApiResponse<T>
```

### 핵심 컴포넌트

#### 1. BaseErrorCode (인터페이스)
```java
public interface BaseErrorCode {
    HttpStatus getHttpStatus();  // HTTP 상태 코드
    String getCode();           // 에러 코드 (예: "EMPLOYEE_2001")
    String getMessage();        // 에러 메시지
}
```

#### 2. ErrorCategory (enum)
```java
public enum ErrorCategory {
    COMMON("COMMON_"),              // 0-999: 공통 에러
    AUTH("AUTH_"),                  // 1000-1999: 인증/인가
    EMPLOYEE("EMPLOYEE_"),          // 2000-2999: 사원 관리
    SITE("SITE_"),                  // 3000-3999: 현장 관리
    CONTRACT("CONTRACT_"),          // 4000-4999: 계약 관리
    PAYROLL("PAYROLL_"),            // 5000-5999: 급여 관리
    ATTENDANCE("ATTENDANCE_"),      // 6000-6999: 근태 관리
    WORKREPORT("WORKREPORT_"),      // 7000-7999: 작업일보
    SAFETYDOC("SAFETYDOC_");        // 8000-8999: 안전교육일지
}
```

#### 3. BusinessException (기본 예외 클래스)
```java
public class BusinessException extends RuntimeException {
    private final BaseErrorCode errorCode;

    public BusinessException(BaseErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
```

---

## 도메인별 에러 코드 범위

| 도메인 | ErrorCategory | 코드 범위 | 패키지 |
|--------|--------------|----------|--------|
| 공통 | COMMON | 0-999 | `global.exception.errorcode` |
| 인증/인가 | AUTH | 1000-1999 | `domain.auth` |
| 사원 관리 | EMPLOYEE | 2000-2999 | `domain.employee` |
| 현장 관리 | SITE | 3000-3999 | `domain.site` |
| 계약 관리 | CONTRACT | 4000-4999 | `domain.contract` |
| 급여 관리 | PAYROLL | 5000-5999 | `domain.payroll` |
| 근태 관리 | ATTENDANCE | 6000-6999 | `domain.attendance` |
| 작업일보 | WORKREPORT | 7000-7999 | `domain.workreport` |
| 안전교육일지 | SAFETYDOC | 8000-8999 | `domain.safetydoc` |

### 에러 코드 네이밍 규칙

```java
// ✅ 올바른 예시
ErrorCategory.EMPLOYEE.generate(2001)  // → "EMPLOYEE_2001"
ErrorCategory.SITE.generate(3005)      // → "SITE_3005"
ErrorCategory.AUTH.generate(1401)      // → "AUTH_1401"

// ❌ 잘못된 예시
ErrorCategory.EMPLOYEE.generate(5001)  // EMPLOYEE 범위(2000-2999) 벗어남
ErrorCategory.SITE.generate(2001)      // SITE가 아닌 EMPLOYEE 범위 사용
```

---

## 예외 타입별 사용 가이드

### 1. BusinessException - 비즈니스 로직 예외

**사용 시점:**
- 비즈니스 로직에서 예측 가능한 예외 상황
- 사용자 입력이나 비즈니스 규칙 위반 시
- 개발자 실수나 시스템 장애가 아닌 경우

**사용 예시:**
```java
// 중복 체크
if (employeeRepository.existsByPhoneNumber(phoneNumber)) {
    throw new BusinessException(EmployeeErrorCode.DUPLICATE_PHONE_NUMBER);
}

// 상태 검증
if (site.getStatus() == SiteStatus.CLOSED) {
    throw new BusinessException(SiteErrorCode.SITE_ALREADY_CLOSED);
}

// 날짜 검증
if (endDate.isBefore(startDate)) {
    throw new BusinessException(ContractErrorCode.INVALID_CONTRACT_PERIOD);
}

// 커스텀 메시지 사용
if (payroll.getStatus() == PayrollStatus.PAID) {
    throw new BusinessException(
        PayrollErrorCode.PAYROLL_ALREADY_PAID,
        "이미 지급된 급여는 수정할 수 없습니다."
    );
}
```

### 2. ResourceNotFoundException - 리소스 없음 (404)

**사용 시점:**
- 데이터베이스 조회 결과가 없을 때
- NPE 대신 명확한 "데이터 없음" 상황 표현

**사용 예시:**
```java
// Optional.orElseThrow 사용
User user = userRepository.findById(userId)
    .orElseThrow(() -> new ResourceNotFoundException(AuthErrorCode.USER_NOT_FOUND));

Employee employee = employeeRepository.findById(employeeId)
    .orElseThrow(() -> new ResourceNotFoundException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND));

Site site = siteRepository.findById(siteId)
    .orElseThrow(() -> new ResourceNotFoundException(SiteErrorCode.SITE_NOT_FOUND));

// 커스텀 메시지와 함께
Contract contract = contractRepository.findById(contractId)
    .orElseThrow(() -> new ResourceNotFoundException(
        ContractErrorCode.CONTRACT_NOT_FOUND,
        "ID가 " + contractId + "인 계약서를 찾을 수 없습니다."
    ));
```

### 3. AuthorizationException - 권한 없음 (403)

**사용 시점:**
- 인증은 되었지만 특정 리소스에 대한 권한이 없을 때
- 역할 기반 접근 제어에서 권한 확인 실패 시
- 현장별 접근 권한 확인 실패 시

**생성자 종류:**
```java
// 1. 기본 권한 없음
throw new AuthorizationException();

// 2. 리소스별 권한 확인
throw new AuthorizationException(userId, "Site", "delete");

// 3. 역할 기반 권한 확인
throw new AuthorizationException(userId, "ADMIN");

// 4. 현장별 권한 확인 (Build-Up 특화)
throw new AuthorizationException(userId, siteId);

// 5. 커스텀 에러 코드 사용
throw new AuthorizationException(AuthErrorCode.ACCESS_DENIED);
```

**실제 사용 예시:**
```java
// 현장 접근 권한 확인
if (!siteService.hasAccessToSite(userId, siteId)) {
    throw new AuthorizationException(userId, siteId);
}

// 사원 삭제 권한 확인 (관리자만)
if (!user.hasRole("ADMIN")) {
    throw new AuthorizationException(userId, "ADMIN");
}

// 급여 수정 권한 확인
if (!payroll.getCreatedBy().equals(userId)) {
    throw new AuthorizationException(userId, "Payroll", "update");
}
```

---

## ErrorCode 구현 규칙

### 1. BaseErrorCode 인터페이스 구현

모든 ErrorCode enum은 반드시 `BaseErrorCode` 인터페이스를 구현해야 합니다.

```java
public interface BaseErrorCode {
    HttpStatus getHttpStatus();
    String getCode();
    String getMessage();
}
```

### 2. 도메인별 ErrorCode Enum 생성

**파일 위치:** `domain/{domain-name}/exception/` 또는 `global/exception/errorcode/`

**기본 구조:**
```java
package com.concrete.buildup.domain.employee.exception;

import com.concrete.buildup.global.exception.BaseErrorCode;
import com.concrete.buildup.global.exception.ErrorCategory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 사원 관리 에러 코드 (2000-2999)
 */
@Getter
@RequiredArgsConstructor
public enum EmployeeErrorCode implements BaseErrorCode {

    // 404 Not Found
    EMPLOYEE_NOT_FOUND(HttpStatus.NOT_FOUND, 2001, "사원을 찾을 수 없습니다."),

    // 409 Conflict
    DUPLICATE_PHONE_NUMBER(HttpStatus.CONFLICT, 2002, "이미 등록된 전화번호입니다."),
    DUPLICATE_RESIDENT_NUMBER(HttpStatus.CONFLICT, 2003, "이미 등록된 주민등록번호입니다."),

    // 400 Bad Request
    INVALID_EMPLOYEE_STATUS(HttpStatus.BAD_REQUEST, 2004, "유효하지 않은 사원 상태입니다."),
    EMPLOYEE_ALREADY_TERMINATED(HttpStatus.BAD_REQUEST, 2005, "이미 퇴사 처리된 사원입니다.");

    private final HttpStatus httpStatus;
    private final int codeNumber;
    private final String message;

    @Override
    public String getCode() {
        return ErrorCategory.EMPLOYEE.generate(codeNumber);
    }
}
```

### 3. HTTP 상태 코드 매핑 가이드

| HTTP 상태 | 사용 시점 | 예시 |
|-----------|----------|------|
| 400 BAD_REQUEST | 잘못된 요청, 검증 실패 | `INVALID_CONTRACT_PERIOD` |
| 401 UNAUTHORIZED | 인증 실패 | `INVALID_TOKEN`, `TOKEN_EXPIRED` |
| 403 FORBIDDEN | 권한 없음 | `ACCESS_DENIED`, `INSUFFICIENT_PERMISSION` |
| 404 NOT_FOUND | 리소스 없음 | `EMPLOYEE_NOT_FOUND`, `SITE_NOT_FOUND` |
| 409 CONFLICT | 중복, 상태 충돌 | `DUPLICATE_PHONE_NUMBER`, `SITE_ALREADY_CLOSED` |
| 500 INTERNAL_SERVER_ERROR | 서버 오류 | `INTERNAL_SERVER_ERROR` |

---

## ApiResponse 사용법

### ApiResponse 구조

```json
{
  "success": true/false,
  "message": "메시지",
  "data": { ... }
}
```

### 1. 성공 응답

#### 데이터만 포함
```java
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<EmployeeResponse>> getEmployee(@PathVariable Long id) {
    EmployeeResponse employee = employeeService.getEmployee(id);
    return ResponseEntity.ok(ApiResponse.success(employee));
}
```

#### 메시지와 함께
```java
@PostMapping
public ResponseEntity<ApiResponse<SiteResponse>> createSite(@RequestBody SiteRequest request) {
    SiteResponse site = siteService.createSite(request);
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(ApiResponse.success(site, "현장이 성공적으로 등록되었습니다."));
}
```

#### 메시지만 (데이터 없음)
```java
@DeleteMapping("/{id}")
public ResponseEntity<ApiResponse<Void>> deleteEmployee(@PathVariable Long id) {
    employeeService.deleteEmployee(id);
    return ResponseEntity.ok(ApiResponse.success("사원이 삭제되었습니다."));
}
```

### 2. 에러 응답

에러 응답은 **GlobalExceptionHandler에서 자동으로 처리**되므로, Service/Controller에서 직접 사용할 필요가 없습니다.

```java
// ❌ 직접 사용하지 않음
return ResponseEntity
    .status(HttpStatus.NOT_FOUND)
    .body(ApiResponse.error("사원을 찾을 수 없습니다."));

// ✅ 대신 예외를 던지면 GlobalExceptionHandler가 자동 처리
throw new ResourceNotFoundException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND);
```

---

## 실제 구현 예시

### 예시 1: 사원 관리 (Employee)

#### 1. ErrorCode 정의
```java
// domain/employee/exception/EmployeeErrorCode.java
@Getter
@RequiredArgsConstructor
public enum EmployeeErrorCode implements BaseErrorCode {
    EMPLOYEE_NOT_FOUND(HttpStatus.NOT_FOUND, 2001, "사원을 찾을 수 없습니다."),
    DUPLICATE_PHONE_NUMBER(HttpStatus.CONFLICT, 2002, "이미 등록된 전화번호입니다."),
    INVALID_EMPLOYEE_STATUS(HttpStatus.BAD_REQUEST, 2003, "유효하지 않은 사원 상태입니다.");

    private final HttpStatus httpStatus;
    private final int codeNumber;
    private final String message;

    @Override
    public String getCode() {
        return ErrorCategory.EMPLOYEE.generate(codeNumber);
    }
}
```

#### 2. Service에서 예외 사용
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    public EmployeeResponse getEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND));

        return EmployeeResponse.from(employee);
    }

    @Transactional
    public EmployeeResponse createEmployee(EmployeeCreateRequest request) {
        // 중복 체크
        if (employeeRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new BusinessException(EmployeeErrorCode.DUPLICATE_PHONE_NUMBER);
        }

        Employee employee = Employee.builder()
            .name(request.getName())
            .phoneNumber(request.getPhoneNumber())
            .build();

        return EmployeeResponse.from(employeeRepository.save(employee));
    }

    @Transactional
    public void terminateEmployee(Long id, EmployeeTerminationRequest request) {
        Employee employee = employeeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND));

        if (employee.getStatus() == EmployeeStatus.TERMINATED) {
            throw new BusinessException(EmployeeErrorCode.EMPLOYEE_ALREADY_TERMINATED);
        }

        employee.terminate(request.getTerminationDate(), request.getReason());
    }
}
```

#### 3. Controller에서 응답
```java
@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getEmployee(@PathVariable Long id) {
        EmployeeResponse employee = employeeService.getEmployee(id);
        return ResponseEntity.ok(ApiResponse.success(employee));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EmployeeResponse>> createEmployee(
            @Valid @RequestBody EmployeeCreateRequest request) {
        EmployeeResponse employee = employeeService.createEmployee(request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(employee, "사원이 성공적으로 등록되었습니다."));
    }

    @PatchMapping("/{id}/terminate")
    public ResponseEntity<ApiResponse<Void>> terminateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeTerminationRequest request) {
        employeeService.terminateEmployee(id, request);
        return ResponseEntity.ok(ApiResponse.success("사원이 퇴사 처리되었습니다."));
    }
}
```

### 예시 2: 현장 관리 (Site)

#### 1. ErrorCode 정의
```java
// domain/site/exception/SiteErrorCode.java
@Getter
@RequiredArgsConstructor
public enum SiteErrorCode implements BaseErrorCode {
    SITE_NOT_FOUND(HttpStatus.NOT_FOUND, 3001, "현장을 찾을 수 없습니다."),
    DUPLICATE_SITE_CODE(HttpStatus.CONFLICT, 3002, "이미 사용 중인 현장 코드입니다."),
    SITE_ALREADY_CLOSED(HttpStatus.BAD_REQUEST, 3003, "이미 종료된 현장입니다."),
    INVALID_SITE_PERIOD(HttpStatus.BAD_REQUEST, 3004, "현장 기간이 유효하지 않습니다."),
    SITE_ACCESS_DENIED(HttpStatus.FORBIDDEN, 3005, "해당 현장에 접근 권한이 없습니다.");

    private final HttpStatus httpStatus;
    private final int codeNumber;
    private final String message;

    @Override
    public String getCode() {
        return ErrorCategory.SITE.generate(codeNumber);
    }
}
```

#### 2. Service에서 예외 사용
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SiteService {

    private final SiteRepository siteRepository;

    public SiteResponse getSite(Long userId, Long siteId) {
        Site site = siteRepository.findById(siteId)
            .orElseThrow(() -> new ResourceNotFoundException(SiteErrorCode.SITE_NOT_FOUND));

        // 권한 확인
        if (!hasAccessToSite(userId, siteId)) {
            throw new AuthorizationException(userId, siteId);
        }

        return SiteResponse.from(site);
    }

    @Transactional
    public SiteResponse createSite(SiteCreateRequest request) {
        // 중복 체크
        if (siteRepository.existsByCode(request.getCode())) {
            throw new BusinessException(SiteErrorCode.DUPLICATE_SITE_CODE);
        }

        // 날짜 검증
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BusinessException(SiteErrorCode.INVALID_SITE_PERIOD);
        }

        Site site = Site.builder()
            .code(request.getCode())
            .name(request.getName())
            .startDate(request.getStartDate())
            .endDate(request.getEndDate())
            .build();

        return SiteResponse.from(siteRepository.save(site));
    }

    @Transactional
    public void closeSite(Long userId, Long siteId) {
        Site site = siteRepository.findById(siteId)
            .orElseThrow(() -> new ResourceNotFoundException(SiteErrorCode.SITE_NOT_FOUND));

        // 권한 확인
        if (!hasAccessToSite(userId, siteId)) {
            throw new AuthorizationException(userId, "Site", "close");
        }

        // 상태 확인
        if (site.getStatus() == SiteStatus.CLOSED) {
            throw new BusinessException(SiteErrorCode.SITE_ALREADY_CLOSED);
        }

        site.close();
    }
}
```

### 예시 3: 급여 관리 (Payroll)

#### 1. ErrorCode 정의
```java
// domain/payroll/exception/PayrollErrorCode.java
@Getter
@RequiredArgsConstructor
public enum PayrollErrorCode implements BaseErrorCode {
    PAYROLL_NOT_FOUND(HttpStatus.NOT_FOUND, 5001, "급여 내역을 찾을 수 없습니다."),
    PAYROLL_ALREADY_PAID(HttpStatus.CONFLICT, 5002, "이미 지급 완료된 급여입니다."),
    INVALID_PAYMENT_AMOUNT(HttpStatus.BAD_REQUEST, 5003, "지급 금액이 유효하지 않습니다."),
    PAYROLL_CALCULATION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 5004, "급여 계산 중 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final int codeNumber;
    private final String message;

    @Override
    public String getCode() {
        return ErrorCategory.PAYROLL.generate(codeNumber);
    }
}
```

#### 2. Service에서 예외 사용
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PayrollService {

    private final PayrollRepository payrollRepository;

    @Transactional
    public void payPayroll(Long payrollId) {
        Payroll payroll = payrollRepository.findById(payrollId)
            .orElseThrow(() -> new ResourceNotFoundException(PayrollErrorCode.PAYROLL_NOT_FOUND));

        if (payroll.getStatus() == PayrollStatus.PAID) {
            throw new BusinessException(PayrollErrorCode.PAYROLL_ALREADY_PAID);
        }

        if (payroll.getTotalAmount() <= 0) {
            throw new BusinessException(PayrollErrorCode.INVALID_PAYMENT_AMOUNT);
        }

        payroll.markAsPaid();
    }
}
```

---

## 개발 체크리스트

### 새로운 도메인 예외 추가 시

- [ ] **ErrorCode Enum 생성**
  - [ ] `domain/{domain}/exception/` 패키지에 생성
  - [ ] `BaseErrorCode` 인터페이스 구현
  - [ ] 적절한 `ErrorCategory` 사용
  - [ ] 에러 코드 번호가 할당된 범위 내에 있는지 확인
  - [ ] HTTP 상태 코드 적절히 매핑

- [ ] **예외 타입 선택**
  - [ ] `BusinessException`: 비즈니스 로직 예외
  - [ ] `ResourceNotFoundException`: 리소스 없음 (404)
  - [ ] `AuthorizationException`: 권한 없음 (403)

- [ ] **Service 레이어에서 사용**
  - [ ] 적절한 시점에 예외 던지기
  - [ ] 사용자 친화적인 에러 메시지 작성
  - [ ] 필요시 커스텀 메시지 사용

- [ ] **Controller 레이어**
  - [ ] `ApiResponse.success()` 사용
  - [ ] 예외는 GlobalExceptionHandler가 자동 처리

- [ ] **테스트 작성**
  - [ ] 정상 케이스
  - [ ] 예외 발생 케이스
  - [ ] HTTP 상태 코드 확인
  - [ ] 응답 형식 확인

---

## 금지사항 및 권장사항

### ❌ 금지사항

1. **도메인 범위를 벗어난 에러 코드 번호 사용**
   ```java
   // ❌ EMPLOYEE는 2000-2999 범위만 사용 가능
   EMPLOYEE_NOT_FOUND(HttpStatus.NOT_FOUND, 5001, "사원을 찾을 수 없습니다.")
   ```

2. **시스템 오류에 BusinessException 사용**
   ```java
   // ❌ 시스템 오류는 CommonErrorCode 사용
   throw new BusinessException(EmployeeErrorCode.DATABASE_CONNECTION_ERROR);

   // ✅ 시스템 오류는 그냥 Exception 던지면 GlobalExceptionHandler가 처리
   throw new RuntimeException("Database connection failed");
   ```

3. **NPE 대신 ResourceNotFoundException 사용하지 않기**
   ```java
   // ❌ NPE 발생 가능
   Employee employee = employeeRepository.findById(id).get();

   // ✅ ResourceNotFoundException 사용
   Employee employee = employeeRepository.findById(id)
       .orElseThrow(() -> new ResourceNotFoundException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND));
   ```

4. **Controller에서 직접 에러 응답 생성**
   ```java
   // ❌ Controller에서 직접 에러 응답 생성
   if (employee == null) {
       return ResponseEntity
           .status(HttpStatus.NOT_FOUND)
           .body(ApiResponse.error("사원을 찾을 수 없습니다."));
   }

   // ✅ Service에서 예외를 던지면 GlobalExceptionHandler가 처리
   Employee employee = employeeService.getEmployee(id);
   return ResponseEntity.ok(ApiResponse.success(employee));
   ```

5. **일관성 없는 예외 처리**
   ```java
   // ❌ 같은 상황에 다른 예외 사용
   // Service A
   throw new RuntimeException("사원을 찾을 수 없습니다.");
   // Service B
   throw new IllegalArgumentException("사원을 찾을 수 없습니다.");

   // ✅ 일관된 예외 사용
   throw new ResourceNotFoundException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND);
   ```

### ✅ 권장사항

1. **예외 메시지는 사용자 친화적으로 작성**
   ```java
   // ✅ 사용자가 이해하기 쉬운 메시지
   DUPLICATE_PHONE_NUMBER(HttpStatus.CONFLICT, 2002, "이미 등록된 전화번호입니다.")

   // ❌ 기술적인 메시지
   DUPLICATE_PHONE_NUMBER(HttpStatus.CONFLICT, 2002, "Unique constraint violation: phone_number")
   ```

2. **에러 코드는 도메인별로 명확하게 분류**
   ```java
   // ✅ 도메인별로 명확한 분류
   EmployeeErrorCode.EMPLOYEE_NOT_FOUND
   SiteErrorCode.SITE_NOT_FOUND

   // ❌ 일반적인 이름 사용
   CommonErrorCode.NOT_FOUND
   ```

3. **HTTP 상태 코드와 비즈니스 에러 코드를 적절히 매핑**
   ```java
   // ✅ 명확한 매핑
   EMPLOYEE_NOT_FOUND(HttpStatus.NOT_FOUND, 2001, "사원을 찾을 수 없습니다.")
   DUPLICATE_PHONE_NUMBER(HttpStatus.CONFLICT, 2002, "이미 등록된 전화번호입니다.")

   // ❌ 부적절한 매핑
   EMPLOYEE_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, 2001, "사원을 찾을 수 없습니다.")
   ```

4. **GlobalExceptionHandler에서 자동 처리되도록 구조화**
   ```java
   // ✅ Service에서 예외만 던지면 됨
   @Service
   public class EmployeeService {
       public Employee getEmployee(Long id) {
           return employeeRepository.findById(id)
               .orElseThrow(() -> new ResourceNotFoundException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND));
       }
   }

   // GlobalExceptionHandler가 자동으로 ApiResponse로 변환
   ```

5. **테스트에서 예외 상황도 함께 검증**
   ```java
   @Test
   void getEmployee_WhenNotFound_ShouldThrowException() {
       // given
       Long employeeId = 999L;
       when(employeeRepository.findById(employeeId)).thenReturn(Optional.empty());

       // when & then
       assertThatThrownBy(() -> employeeService.getEmployee(employeeId))
           .isInstanceOf(ResourceNotFoundException.class)
           .hasMessageContaining("사원을 찾을 수 없습니다.");
   }
   ```

---

## 추가 참고 자료

### 파일 위치
- **기본 예외 구조**: `src/main/java/com/concrete/buildup/global/exception/`
- **공통 에러 코드**: `src/main/java/com/concrete/buildup/global/exception/errorcode/CommonErrorCode.java`
- **도메인별 에러 코드**: `src/main/java/com/concrete/buildup/domain/{domain}/exception/{Domain}ErrorCode.java`

### 관련 문서
- [CLAUDE.md](../CLAUDE.md) - 프로젝트 전체 구조 및 컨벤션
- [API 응답 형식](../CLAUDE.md#api-응답-형식)
- [도메인 구조](../CLAUDE.md#프로젝트-구조)

---

## 버전 정보
- **최초 작성**: 2025-10-30
- **Build-Up Platform 버전**: Spring Boot 3.5.7, Java 21
- **작성자**: Claude Code