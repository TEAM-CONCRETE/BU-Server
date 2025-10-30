---
description: Build-Up Platform 비즈니스 예외처리 가이드
alwaysApply: true
---

# Business Exception Pattern for Build-Up Platform

Build-Up Platform의 비즈니스 예외 처리 표준 가이드입니다. 모든 도메인에서 일관된 예외 처리를 위해 이 패턴을 따라야 합니다.

## 목차
1. [기본 구조](#기본-구조)
2. [도메인별 에러 코드](#도메인별-에러-코드)
3. [예외 클래스](#예외-클래스)
4. [GlobalExceptionHandler](#globalexceptionhandler)
5. [실제 사용 예시](#실제-사용-예시)

---

## 기본 구조

### 1. BaseErrorCode 인터페이스

모든 에러 코드는 이 인터페이스를 구현해야 합니다.

```java
package com.concrete.buildup.global.exception;

import org.springframework.http.HttpStatus;

public interface BaseErrorCode {
    HttpStatus getHttpStatus();
    String getCode();
    String getMessage();
}
```

### 2. ErrorCategory (에러 코드 카테고리)

Build-Up Platform의 도메인 구조에 맞춘 에러 카테고리입니다.

```java
package com.concrete.buildup.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 에러 코드 카테고리
 * 각 도메인별로 1000 단위로 번호 범위를 할당합니다.
 */
@Getter
@RequiredArgsConstructor
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

    private final String prefix;

    /**
     * 에러 코드 생성
     * @param codeNumber 도메인별 에러 번호
     * @return 전체 에러 코드 (예: "AUTH_1001")
     */
    public String generate(int codeNumber) {
        return prefix + codeNumber;
    }
}
```

---

## 도메인별 에러 코드

### 1. CommonErrorCode (공통 에러 0-999)

```java
package com.concrete.buildup.global.exception.errorcode;

import com.concrete.buildup.global.exception.BaseErrorCode;
import com.concrete.buildup.global.exception.ErrorCategory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 공통 에러 코드
 */
@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements BaseErrorCode {

    // 400 Bad Request
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, 400, "입력값이 올바르지 않습니다."),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, 401, "입력 타입이 올바르지 않습니다."),
    FIELD_VALIDATION_ERROR(HttpStatus.BAD_REQUEST, 402, "필드 검증에 실패했습니다."),
    MISSING_REQUEST_PARAMETER(HttpStatus.BAD_REQUEST, 403, "필수 파라미터가 누락되었습니다."),

    // 404 Not Found
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "요청한 리소스를 찾을 수 없습니다."),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 500, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final int codeNumber;
    private final String message;

    @Override
    public String getCode() {
        return ErrorCategory.COMMON.generate(codeNumber);
    }
}
```

### 2. AuthErrorCode (인증/인가 1000-1999)

```java
package com.concrete.buildup.global.exception.errorcode;

import com.concrete.buildup.global.exception.BaseErrorCode;
import com.concrete.buildup.global.exception.ErrorCategory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 인증/인가 도메인 에러 코드
 */
@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements BaseErrorCode {

    // 회원가입
    DUPLICATE_USER_ID(HttpStatus.CONFLICT, 1001, "이미 사용 중인 아이디입니다."),
    DUPLICATE_PHONE_NUMBER(HttpStatus.CONFLICT, 1002, "이미 등록된 전화번호입니다."),
    INVALID_SECRET_KEY(HttpStatus.BAD_REQUEST, 1003, "유효하지 않은 시크릿 키입니다."),

    // 로그인
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, 1010, "아이디 또는 비밀번호가 올바르지 않습니다."),
    ACCOUNT_LOCKED(HttpStatus.FORBIDDEN, 1011, "계정이 잠겨있습니다."),
    ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, 1012, "비활성화된 계정입니다."),

    // 토큰
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, 1020, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, 1021, "만료된 토큰입니다."),
    TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, 1022, "토큰을 찾을 수 없습니다."),

    // 권한
    PERMISSION_DENIED(HttpStatus.FORBIDDEN, 1030, "권한이 없습니다."),
    INSUFFICIENT_ROLE(HttpStatus.FORBIDDEN, 1031, "필요한 역할이 부족합니다.");

    private final HttpStatus httpStatus;
    private final int codeNumber;
    private final String message;

    @Override
    public String getCode() {
        return ErrorCategory.AUTH.generate(codeNumber);
    }
}
```

### 3. EmployeeErrorCode (사원 관리 2000-2999)

```java
package com.concrete.buildup.global.exception.errorcode;

import com.concrete.buildup.global.exception.BaseErrorCode;
import com.concrete.buildup.global.exception.ErrorCategory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 사원 관리 도메인 에러 코드
 */
@Getter
@RequiredArgsConstructor
public enum EmployeeErrorCode implements BaseErrorCode {

    EMPLOYEE_NOT_FOUND(HttpStatus.NOT_FOUND, 2001, "사원을 찾을 수 없습니다."),
    EMPLOYEE_ALREADY_EXISTS(HttpStatus.CONFLICT, 2002, "이미 등록된 사원입니다."),
    EMPLOYEE_NOT_IN_SITE(HttpStatus.BAD_REQUEST, 2003, "해당 현장에 소속되지 않은 사원입니다."),
    INVALID_EMPLOYEE_TYPE(HttpStatus.BAD_REQUEST, 2004, "유효하지 않은 사원 유형입니다.");

    private final HttpStatus httpStatus;
    private final int codeNumber;
    private final String message;

    @Override
    public String getCode() {
        return ErrorCategory.EMPLOYEE.generate(codeNumber);
    }
}
```

### 4. SiteErrorCode (현장 관리 3000-3999)

```java
package com.concrete.buildup.global.exception.errorcode;

import com.concrete.buildup.global.exception.BaseErrorCode;
import com.concrete.buildup.global.exception.ErrorCategory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 현장 관리 도메인 에러 코드
 */
@Getter
@RequiredArgsConstructor
public enum SiteErrorCode implements BaseErrorCode {

    SITE_NOT_FOUND(HttpStatus.NOT_FOUND, 3001, "현장을 찾을 수 없습니다."),
    SITE_ALREADY_EXISTS(HttpStatus.CONFLICT, 3002, "이미 등록된 현장입니다."),
    SITE_ACCESS_DENIED(HttpStatus.FORBIDDEN, 3003, "현장 접근 권한이 없습니다."),
    INVALID_SITE_PERIOD(HttpStatus.BAD_REQUEST, 3004, "현장 기간이 올바르지 않습니다.");

    private final HttpStatus httpStatus;
    private final int codeNumber;
    private final String message;

    @Override
    public String getCode() {
        return ErrorCategory.SITE.generate(codeNumber);
    }
}
```

### 5. ContractErrorCode (계약 관리 4000-4999)

```java
package com.concrete.buildup.global.exception.errorcode;

import com.concrete.buildup.global.exception.BaseErrorCode;
import com.concrete.buildup.global.exception.ErrorCategory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 계약 관리 도메인 에러 코드
 */
@Getter
@RequiredArgsConstructor
public enum ContractErrorCode implements BaseErrorCode {

    CONTRACT_NOT_FOUND(HttpStatus.NOT_FOUND, 4001, "계약서를 찾을 수 없습니다."),
    CONTRACT_ALREADY_SIGNED(HttpStatus.BAD_REQUEST, 4002, "이미 서명된 계약서입니다."),
    CONTRACT_NOT_SENT(HttpStatus.BAD_REQUEST, 4003, "전송되지 않은 계약서입니다."),
    CONTRACT_ALREADY_TERMINATED(HttpStatus.BAD_REQUEST, 4004, "이미 해지된 계약서입니다."),
    INVALID_CONTRACT_STATUS(HttpStatus.BAD_REQUEST, 4005, "유효하지 않은 계약 상태입니다."),
    SIGNATURE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 4006, "전자서명에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final int codeNumber;
    private final String message;

    @Override
    public String getCode() {
        return ErrorCategory.CONTRACT.generate(codeNumber);
    }
}
```

### 6. PayrollErrorCode (급여 관리 5000-5999)

```java
package com.concrete.buildup.global.exception.errorcode;

import com.concrete.buildup.global.exception.BaseErrorCode;
import com.concrete.buildup.global.exception.ErrorCategory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 급여 관리 도메인 에러 코드
 */
@Getter
@RequiredArgsConstructor
public enum PayrollErrorCode implements BaseErrorCode {

    PAYROLL_NOT_FOUND(HttpStatus.NOT_FOUND, 5001, "급여 내역을 찾을 수 없습니다."),
    PAYROLL_ALREADY_GENERATED(HttpStatus.CONFLICT, 5002, "이미 생성된 급여 내역입니다."),
    INVALID_PAYROLL_PERIOD(HttpStatus.BAD_REQUEST, 5003, "유효하지 않은 급여 기간입니다."),
    PDF_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 5004, "급여명세서 PDF 생성에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final int codeNumber;
    private final String message;

    @Override
    public String getCode() {
        return ErrorCategory.PAYROLL.generate(codeNumber);
    }
}
```

### 7. AttendanceErrorCode (근태 관리 6000-6999)

```java
package com.concrete.buildup.global.exception.errorcode;

import com.concrete.buildup.global.exception.BaseErrorCode;
import com.concrete.buildup.global.exception.ErrorCategory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 근태 관리 도메인 에러 코드
 */
@Getter
@RequiredArgsConstructor
public enum AttendanceErrorCode implements BaseErrorCode {

    ATTENDANCE_NOT_FOUND(HttpStatus.NOT_FOUND, 6001, "근태 기록을 찾을 수 없습니다."),
    ALREADY_CHECKED_IN(HttpStatus.BAD_REQUEST, 6002, "이미 출근 처리되었습니다."),
    NOT_CHECKED_IN(HttpStatus.BAD_REQUEST, 6003, "출근 기록이 없습니다."),
    ALREADY_CHECKED_OUT(HttpStatus.BAD_REQUEST, 6004, "이미 퇴근 처리되었습니다."),
    INVALID_ATTENDANCE_TIME(HttpStatus.BAD_REQUEST, 6005, "유효하지 않은 근태 시간입니다.");

    private final HttpStatus httpStatus;
    private final int codeNumber;
    private final String message;

    @Override
    public String getCode() {
        return ErrorCategory.ATTENDANCE.generate(codeNumber);
    }
}
```

### 8. WorkReportErrorCode (작업일보 7000-7999)

```java
package com.concrete.buildup.global.exception.errorcode;

import com.concrete.buildup.global.exception.BaseErrorCode;
import com.concrete.buildup.global.exception.ErrorCategory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 작업일보 도메인 에러 코드
 */
@Getter
@RequiredArgsConstructor
public enum WorkReportErrorCode implements BaseErrorCode {

    WORKREPORT_NOT_FOUND(HttpStatus.NOT_FOUND, 7001, "작업일보를 찾을 수 없습니다."),
    WORKREPORT_ALREADY_EXISTS(HttpStatus.CONFLICT, 7002, "해당 날짜의 작업일보가 이미 존재합니다."),
    PDF_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 7003, "작업일보 PDF 생성에 실패했습니다."),
    PDF_NOT_READY(HttpStatus.BAD_REQUEST, 7004, "PDF가 아직 준비되지 않았습니다.");

    private final HttpStatus httpStatus;
    private final int codeNumber;
    private final String message;

    @Override
    public String getCode() {
        return ErrorCategory.WORKREPORT.generate(codeNumber);
    }
}
```

### 9. SafetyDocErrorCode (안전교육일지 8000-8999)

```java
package com.concrete.buildup.global.exception.errorcode;

import com.concrete.buildup.global.exception.BaseErrorCode;
import com.concrete.buildup.global.exception.ErrorCategory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 안전교육일지 도메인 에러 코드
 */
@Getter
@RequiredArgsConstructor
public enum SafetyDocErrorCode implements BaseErrorCode {

    SAFETYDOC_NOT_FOUND(HttpStatus.NOT_FOUND, 8001, "안전교육일지를 찾을 수 없습니다."),
    SAFETYDOC_ALREADY_EXISTS(HttpStatus.CONFLICT, 8002, "해당 날짜의 안전교육일지가 이미 존재합니다."),
    ATTENDEE_NOT_FOUND(HttpStatus.NOT_FOUND, 8003, "참석자를 찾을 수 없습니다."),
    ALREADY_SIGNED(HttpStatus.BAD_REQUEST, 8004, "이미 서명되었습니다."),
    PDF_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 8005, "안전교육일지 PDF 생성에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final int codeNumber;
    private final String message;

    @Override
    public String getCode() {
        return ErrorCategory.SAFETYDOC.generate(codeNumber);
    }
}
```

---

## 예외 클래스

### 1. BusinessException (기본 비즈니스 예외)

```java
package com.concrete.buildup.global.exception;

import lombok.Getter;

/**
 * 비즈니스 로직 예외의 기본 클래스
 *
 * 사용 시점:
 * - 비즈니스 규칙 위반
 * - 사용자 입력 오류
 * - 예측 가능한 예외 상황
 */
@Getter
public class BusinessException extends RuntimeException {

    private final BaseErrorCode errorCode;

    public BusinessException(BaseErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(BaseErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }

    public BusinessException(BaseErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
}
```

### 2. ResourceNotFoundException (리소스 없음 - 404)

```java
package com.concrete.buildup.global.exception;

/**
 * 리소스를 찾을 수 없을 때 발생하는 예외
 *
 * 사용 시점:
 * - 데이터베이스 조회 결과 없음
 * - 존재하지 않는 리소스 접근
 * - findById().orElseThrow() 사용 시
 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(BaseErrorCode errorCode) {
        super(errorCode);
    }

    public ResourceNotFoundException(BaseErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }
}
```

### 3. AuthorizationException (권한 없음 - 403)

```java
package com.concrete.buildup.global.exception;

/**
 * 권한이 없을 때 발생하는 예외
 *
 * 사용 시점:
 * - 인증은 되었지만 권한 부족
 * - 리소스 소유권 확인 실패
 * - 역할 기반 접근 제어 실패
 */
public class AuthorizationException extends BusinessException {

    // 기본 생성자
    public AuthorizationException() {
        super(AuthErrorCode.PERMISSION_DENIED);
    }

    // 리소스별 권한 확인
    public AuthorizationException(Long userId, String resource, String action) {
        super(AuthErrorCode.PERMISSION_DENIED,
              String.format("User %d does not have permission to %s %s", userId, action, resource));
    }

    // 역할 기반 권한 확인
    public AuthorizationException(Long userId, String requiredRole) {
        super(AuthErrorCode.INSUFFICIENT_ROLE,
              String.format("User %d requires %s role", userId, requiredRole));
    }

    // 현장별 권한 확인 (Build-Up Platform 특화)
    public AuthorizationException(Long userId, Long siteId) {
        super(AuthErrorCode.PERMISSION_DENIED,
              String.format("User %d does not have access to site %d", userId, siteId));
    }
}
```

---

## GlobalExceptionHandler

전역 예외를 처리하는 핸들러입니다.

```java
package com.concrete.buildup.global.exception;

import com.concrete.buildup.global.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 전역 예외 처리 핸들러
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * BusinessException 처리
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        log.error("[BusinessException] code={}, message={}",
                  e.getErrorCode().getCode(), e.getMessage());

        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(ApiResponse.error(
                    e.getErrorCode().getCode(),
                    e.getMessage()
                ));
    }

    /**
     * Validation 예외 처리
     * @Valid, @Validated 어노테이션 사용 시 발생
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException e) {

        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.error("[ValidationException] errors={}", errors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(
                    "COMMON_402",
                    "필드 검증에 실패했습니다.",
                    errors
                ));
    }

    /**
     * 예상치 못한 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception e) {
        log.error("[UnexpectedException] Unexpected exception occurred", e);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                    "COMMON_500",
                    "서버 내부 오류가 발생했습니다."
                ));
    }
}
```

---

## 실제 사용 예시

### 1. Service Layer에서 예외 사용

#### EmployeeService 예시
```java
package com.concrete.buildup.domain.employee.service;

import com.concrete.buildup.domain.employee.entity.Employee;
import com.concrete.buildup.domain.employee.repository.EmployeeRepository;
import com.concrete.buildup.global.exception.ResourceNotFoundException;
import com.concrete.buildup.global.exception.BusinessException;
import com.concrete.buildup.global.exception.errorcode.EmployeeErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    /**
     * ID로 사원 조회 (없으면 예외 발생)
     */
    public Employee getEmployeeById(Long employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    EmployeeErrorCode.EMPLOYEE_NOT_FOUND
                ));
    }

    /**
     * 사원 등록
     */
    @Transactional
    public Employee registerEmployee(EmployeeCreateRequest request) {
        // 중복 검증
        if (employeeRepository.existsByEmployeeNumber(request.getEmployeeNumber())) {
            throw new BusinessException(EmployeeErrorCode.EMPLOYEE_ALREADY_EXISTS);
        }

        Employee employee = Employee.builder()
                .employeeName(request.getEmployeeName())
                .employeeNumber(request.getEmployeeNumber())
                .build();

        return employeeRepository.save(employee);
    }
}
```

#### ContractService 예시
```java
package com.concrete.buildup.domain.contract.service;

import com.concrete.buildup.domain.contract.entity.Contract;
import com.concrete.buildup.domain.contract.repository.ContractRepository;
import com.concrete.buildup.global.exception.BusinessException;
import com.concrete.buildup.global.exception.ResourceNotFoundException;
import com.concrete.buildup.global.exception.AuthorizationException;
import com.concrete.buildup.global.exception.errorcode.ContractErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContractService {

    private final ContractRepository contractRepository;

    /**
     * 계약서 서명
     */
    @Transactional
    public void signContract(Long userId, Long contractId, SignatureRequest request) {
        // 계약서 조회
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    ContractErrorCode.CONTRACT_NOT_FOUND
                ));

        // 상태 검증
        if (contract.isSigned()) {
            throw new BusinessException(ContractErrorCode.CONTRACT_ALREADY_SIGNED);
        }

        if (!contract.isSent()) {
            throw new BusinessException(ContractErrorCode.CONTRACT_NOT_SENT);
        }

        // 권한 검증 (본인 계약서만 서명 가능)
        if (!contract.getEmployeeId().equals(userId)) {
            throw new AuthorizationException(userId, "Contract", "sign");
        }

        // 서명 처리
        contract.sign(request.getSignatureHash(), request.getSignatureImageUrl());
    }

    /**
     * 계약 해지
     */
    @Transactional
    public void terminateContract(Long managerId, Long contractId, String reason) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    ContractErrorCode.CONTRACT_NOT_FOUND
                ));

        if (contract.isTerminated()) {
            throw new BusinessException(ContractErrorCode.CONTRACT_ALREADY_TERMINATED);
        }

        contract.terminate(reason);
    }
}
```

### 2. Controller Layer에서 예외 사용

```java
package com.concrete.buildup.domain.site.controller;

import com.concrete.buildup.domain.site.dto.SiteCreateRequest;
import com.concrete.buildup.domain.site.dto.SiteResponse;
import com.concrete.buildup.domain.site.service.SiteService;
import com.concrete.buildup.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/sites")
@RequiredArgsConstructor
public class SiteController {

    private final SiteService siteService;

    /**
     * 현장 등록
     * Validation 실패 시 GlobalExceptionHandler에서 자동 처리
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<SiteResponse>> registerSite(
            @Valid @RequestBody SiteCreateRequest request) {

        SiteResponse response = siteService.registerSite(request);

        return ResponseEntity.ok(
            ApiResponse.success("현장이 등록되었습니다.", response)
        );
    }

    /**
     * 현장 조회
     * ResourceNotFoundException 발생 시 GlobalExceptionHandler에서 자동 처리
     */
    @GetMapping("/{siteId}")
    public ResponseEntity<ApiResponse<SiteResponse>> getSite(
            @PathVariable Long siteId) {

        SiteResponse response = siteService.getSiteById(siteId);

        return ResponseEntity.ok(
            ApiResponse.success(response)
        );
    }
}
```

---

## 체크리스트

### 새 도메인 추가 시
- [ ] `ErrorCategory`에 새 카테고리 추가 (1000 단위 번호 할당)
- [ ] `XxxErrorCode` enum 생성
- [ ] `BaseErrorCode` 인터페이스 구현
- [ ] 도메인별 에러 번호 범위 문서화

### 예외 처리 작성 시
- [ ] 적절한 예외 클래스 선택 (BusinessException, ResourceNotFoundException, AuthorizationException)
- [ ] 명확한 에러 메시지 작성
- [ ] HTTP 상태 코드 매핑 확인
- [ ] 로그 레벨 확인 (error, warn, info)

### 코드 리뷰 시
- [ ] 예외 처리가 일관되게 적용되었는지 확인
- [ ] 에러 코드가 중복되지 않는지 확인
- [ ] 에러 메시지가 명확한지 확인
- [ ] GlobalExceptionHandler에서 처리 가능한지 확인