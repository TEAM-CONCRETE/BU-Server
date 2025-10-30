---
description: Build-Up Platform DTO 패턴 가이드
globs: "**/*Dto.java,**/*Request.java,**/*Response.java"
alwaysApply: true
---

# DTO Pattern for Build-Up Platform

Build-Up Platform의 DTO (Data Transfer Object) 작성 표준 가이드입니다. Controller와 Service 계층 간의 데이터 전송을 위한 일관된 패턴을 제공합니다.

## 목차
1. [기본 구조](#기본-구조)
2. [Validation 규칙](#validation-규칙)
3. [Response DTO 변환 패턴](#response-dto-변환-패턴)
4. [도메인별 예시](#도메인별-예시)
5. [공통 DTO](#공통-dto)

---

## 기본 구조

### 1. DTO 분류

| 유형 | 네이밍 | 용도 | 위치 |
|------|--------|------|------|
| **Request DTO** | `XxxCreateRequest`<br>`XxxUpdateRequest`<br>`XxxSearchRequest` | API 요청 데이터 | `domain/{domain}/dto/` |
| **Response DTO** | `XxxResponse`<br>`XxxDetailResponse`<br>`XxxSummaryResponse` | API 응답 데이터 | `domain/{domain}/dto/` |
| **Common DTO** | `ApiResponse<T>`<br>`PageResponse<T>` | 공통 응답 래퍼 | `global/common/` |

### 2. Request DTO 기본 구조

```java
package com.concrete.buildup.domain.{domain}.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * {리소스} 생성 요청 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceCreateRequest {

    @NotBlank(message = "이름은 필수입니다")
    @Size(min = 2, max = 100, message = "이름은 2-100자 사이여야 합니다")
    private String name;

    @Size(max = 500, message = "설명은 500자를 초과할 수 없습니다")
    private String description;

    @NotNull(message = "타입은 필수입니다")
    private ResourceType type;
}
```

### 3. Response DTO 기본 구조

```java
package com.concrete.buildup.domain.{domain}.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {리소스} 응답 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResourceResponse {

    private Long id;
    private String name;
    private String description;
    private ResourceType type;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Entity를 Response DTO로 변환
     */
    public static ResourceResponse from(Resource resource) {
        if (resource == null) {
            return null;
        }

        return ResourceResponse.builder()
                .id(resource.getId())
                .name(resource.getName())
                .description(resource.getDescription())
                .type(resource.getType())
                .createdAt(resource.getCreatedAt())
                .updatedAt(resource.getUpdatedAt())
                .build();
    }

    /**
     * Entity 목록을 Response DTO 목록으로 변환
     */
    public static List<ResourceResponse> fromList(List<Resource> resources) {
        return resources.stream()
                .map(ResourceResponse::from)
                .collect(Collectors.toList());
    }
}
```

---

## Validation 규칙

### 1. 주요 Validation 어노테이션

```java
// 필수 필드
@NotNull(message = "값은 필수입니다")           // null 불가
@NotBlank(message = "값은 필수입니다")         // null, "", " " 불가
@NotEmpty(message = "값은 필수입니다")         // null, 빈 컬렉션 불가

// 문자열
@Size(min = 2, max = 100, message = "2-100자 사이여야 합니다")
@Pattern(regexp = "정규식", message = "형식이 올바르지 않습니다")

// 숫자
@Min(value = 0, message = "0 이상이어야 합니다")
@Max(value = 100, message = "100 이하여야 합니다")
@Positive(message = "양수여야 합니다")
@PositiveOrZero(message = "0 또는 양수여야 합니다")

// 날짜/시간
@Past(message = "과거 날짜여야 합니다")
@Future(message = "미래 날짜여야 합니다")

// 기타
@Email(message = "이메일 형식이 올바르지 않습니다")
@Valid  // 중첩 객체 검증
```

### 2. 공통 Validation 패턴

#### 전화번호
```java
@Pattern(
    regexp = "^01[0-9]-\\d{3,4}-\\d{4}$",
    message = "전화번호 형식이 올바르지 않습니다 (예: 010-1234-5678)"
)
private String phone;
```

#### 비밀번호
```java
@NotBlank(message = "비밀번호는 필수입니다")
@Size(min = 8, max = 100, message = "비밀번호는 8-100자 사이여야 합니다")
@Pattern(
    regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]+$",
    message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다"
)
private String password;
```

#### 날짜 범위
```java
@NotNull(message = "시작일은 필수입니다")
@Past(message = "시작일은 과거 날짜여야 합니다")
private LocalDate startDate;

@NotNull(message = "종료일은 필수입니다")
private LocalDate endDate;

// Service에서 검증: endDate가 startDate보다 이후인지
```

### 3. 커스텀 Validator

```java
/**
 * 날짜 범위 검증 어노테이션
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DateRangeValidator.class)
public @interface ValidDateRange {
    String message() default "종료일은 시작일보다 이후여야 합니다";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    String startField();
    String endField();
}

/**
 * 날짜 범위 검증기
 */
public class DateRangeValidator implements ConstraintValidator<ValidDateRange, Object> {

    private String startField;
    private String endField;

    @Override
    public void initialize(ValidDateRange constraintAnnotation) {
        this.startField = constraintAnnotation.startField();
        this.endField = constraintAnnotation.endField();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        try {
            Field startDateField = value.getClass().getDeclaredField(startField);
            Field endDateField = value.getClass().getDeclaredField(endField);

            startDateField.setAccessible(true);
            endDateField.setAccessible(true);

            LocalDate startDate = (LocalDate) startDateField.get(value);
            LocalDate endDate = (LocalDate) endDateField.get(value);

            if (startDate == null || endDate == null) {
                return true;  // @NotNull로 별도 검증
            }

            return !endDate.isBefore(startDate);
        } catch (Exception e) {
            return false;
        }
    }
}

// 사용 예시
@ValidDateRange(startField = "startDate", endField = "endDate")
public class ContractCreateRequest {
    private LocalDate startDate;
    private LocalDate endDate;
}
```

---

## Response DTO 변환 패턴

### 1. from() 메서드 (필수)

```java
/**
 * Entity를 Response DTO로 변환
 */
public static EmployeeResponse from(Employee employee) {
    if (employee == null) {
        return null;
    }

    return EmployeeResponse.builder()
            .employeeId(employee.getId())
            .employeeName(employee.getName())
            .phone(employee.getPhone())
            .employeeType(employee.getType())
            .createdAt(employee.getCreatedAt())
            .build();
}
```

### 2. fromList() 메서드 (목록 변환)

```java
/**
 * Entity 목록을 Response DTO 목록으로 변환
 */
public static List<EmployeeResponse> fromList(List<Employee> employees) {
    if (employees == null || employees.isEmpty()) {
        return Collections.emptyList();
    }

    return employees.stream()
            .map(EmployeeResponse::from)
            .collect(Collectors.toList());
}
```

### 3. 중첩 DTO 변환

```java
/**
 * 계약 상세 응답 DTO (사원 정보 포함)
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContractDetailResponse {

    private Long contractId;
    private String contractType;
    private LocalDate startDate;
    private LocalDate endDate;

    // 중첩 DTO
    private EmployeeSummaryResponse employee;
    private SiteSummaryResponse site;

    public static ContractDetailResponse from(Contract contract) {
        if (contract == null) {
            return null;
        }

        return ContractDetailResponse.builder()
                .contractId(contract.getId())
                .contractType(contract.getType())
                .startDate(contract.getStartDate())
                .endDate(contract.getEndDate())
                // 중첩 변환
                .employee(EmployeeSummaryResponse.from(contract.getEmployee()))
                .site(SiteSummaryResponse.from(contract.getSite()))
                .build();
    }
}
```

---

## 도메인별 예시

### 1. Auth Domain

#### EmployeeRegisterRequest
```java
package com.concrete.buildup.domain.auth.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * 근로자 회원가입 요청 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeRegisterRequest {

    @NotBlank(message = "아이디는 필수입니다")
    @Size(min = 4, max = 20, message = "아이디는 4-20자 사이여야 합니다")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "아이디는 영문, 숫자, 언더스코어만 사용 가능합니다")
    private String userId;

    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, max = 100, message = "비밀번호는 8-100자 사이여야 합니다")
    private String password;

    @NotBlank(message = "이름은 필수입니다")
    @Size(max = 50, message = "이름은 50자를 초과할 수 없습니다")
    private String employeeName;

    @NotBlank(message = "전화번호는 필수입니다")
    @Pattern(regexp = "^01[0-9]-\\d{3,4}-\\d{4}$", message = "전화번호 형식이 올바르지 않습니다")
    private String phone;

    @NotNull(message = "사원 유형은 필수입니다")
    private EmployeeType employeeType;
}
```

#### LoginRequest
```java
package com.concrete.buildup.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * 로그인 요청 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "아이디는 필수입니다")
    private String username;

    @NotBlank(message = "비밀번호는 필수입니다")
    private String password;

    private Boolean rememberMe;  // 자동 로그인
}
```

#### AuthResponse
```java
package com.concrete.buildup.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

/**
 * 인증 응답 DTO (회원가입, 로그인)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    private Long userId;
    private String username;
    private String userType;  // WORKER, SITE_MANAGER, COMPANY
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;  // 토큰 만료 시간 (초)
}
```

### 2. Employee Domain

#### EmployeeSearchCriteria
```java
package com.concrete.buildup.domain.employee.dto;

import lombok.*;

/**
 * 사원 검색 조건 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeSearchCriteria {

    private Long siteId;
    private String type;        // 사원 유형 (정규/일용)
    private String search;      // 검색어 (이름)
    private String joinedFrom;  // 입사일 시작
    private String joinedTo;    // 입사일 종료
    private String sort;        // 정렬 기준
}
```

#### EmployeeResponse
```java
package com.concrete.buildup.domain.employee.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 사원 응답 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmployeeResponse {

    private Long employeeId;
    private String employeeName;
    private String phone;
    private String employeeType;
    private LocalDate joinedDate;
    private String status;
    private LocalDateTime createdAt;

    /**
     * Entity를 Response DTO로 변환
     */
    public static EmployeeResponse from(Employee employee) {
        if (employee == null) {
            return null;
        }

        return EmployeeResponse.builder()
                .employeeId(employee.getId())
                .employeeName(employee.getName())
                .phone(employee.getPhone())
                .employeeType(employee.getType())
                .joinedDate(employee.getJoinedDate())
                .status(employee.getStatus())
                .createdAt(employee.getCreatedAt())
                .build();
    }

    /**
     * Entity 목록을 Response DTO 목록으로 변환
     */
    public static List<EmployeeResponse> fromList(List<Employee> employees) {
        return employees.stream()
                .map(EmployeeResponse::from)
                .collect(Collectors.toList());
    }
}
```

#### EmployeeDetailResponse
```java
package com.concrete.buildup.domain.employee.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.LocalDate;

/**
 * 사원 상세 응답 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmployeeDetailResponse {

    private Long employeeId;
    private String employeeName;
    private String phone;
    private String email;
    private String employeeType;
    private LocalDate joinedDate;
    private String status;

    // 추가 정보
    private String position;        // 직책
    private String department;      // 부서
    private Integer workingDays;    // 근무 일수
    private ContractSummary currentContract;  // 현재 계약

    @Getter
    @Builder
    public static class ContractSummary {
        private Long contractId;
        private String contractType;
        private LocalDate startDate;
        private LocalDate endDate;
        private String status;
    }

    public static EmployeeDetailResponse from(Employee employee, Contract currentContract) {
        EmployeeDetailResponse response = EmployeeDetailResponse.builder()
                .employeeId(employee.getId())
                .employeeName(employee.getName())
                .phone(employee.getPhone())
                .email(employee.getEmail())
                .employeeType(employee.getType())
                .joinedDate(employee.getJoinedDate())
                .status(employee.getStatus())
                .position(employee.getPosition())
                .department(employee.getDepartment())
                .workingDays(employee.getWorkingDays())
                .build();

        if (currentContract != null) {
            response.setCurrentContract(ContractSummary.builder()
                    .contractId(currentContract.getId())
                    .contractType(currentContract.getType())
                    .startDate(currentContract.getStartDate())
                    .endDate(currentContract.getEndDate())
                    .status(currentContract.getStatus())
                    .build());
        }

        return response;
    }
}
```

### 3. Contract Domain

#### ContractCreateRequest
```java
package com.concrete.buildup.domain.contract.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

/**
 * 계약 생성 요청 DTO (공통)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidDateRange(startField = "startDate", endField = "endDate")
public class ContractCreateRequest {

    @NotNull(message = "사원 ID는 필수입니다")
    @Positive(message = "사원 ID는 양수여야 합니다")
    private Long employeeId;

    @NotBlank(message = "계약 유형은 필수입니다")
    private String contractType;  // REGULAR, DAILY

    @NotNull(message = "계약 시작일은 필수입니다")
    private LocalDate startDate;

    @NotNull(message = "계약 종료일은 필수입니다")
    private LocalDate endDate;

    @NotNull(message = "기본급은 필수입니다")
    @Positive(message = "기본급은 양수여야 합니다")
    private Integer baseSalary;

    @Size(max = 1000, message = "특이사항은 1000자를 초과할 수 없습니다")
    private String specialNotes;
}
```

#### ContractSignRequest
```java
package com.concrete.buildup.domain.contract.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * 계약서 서명 요청 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractSignRequest {

    @NotBlank(message = "서명 해시는 필수입니다")
    private String signatureHash;

    @NotBlank(message = "서명 이미지 URL은 필수입니다")
    private String signatureImageUrl;

    private String device;  // 서명 디바이스 정보
}
```

#### ContractResponse
```java
package com.concrete.buildup.domain.contract.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 계약 응답 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContractResponse {

    private Long contractId;
    private Long employeeId;
    private String employeeName;
    private String contractType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;  // DRAFT, SENT, SIGNED, TERMINATED
    private LocalDateTime createdAt;

    public static ContractResponse from(Contract contract) {
        if (contract == null) {
            return null;
        }

        return ContractResponse.builder()
                .contractId(contract.getId())
                .employeeId(contract.getEmployee().getId())
                .employeeName(contract.getEmployee().getName())
                .contractType(contract.getType())
                .startDate(contract.getStartDate())
                .endDate(contract.getEndDate())
                .status(contract.getStatus())
                .createdAt(contract.getCreatedAt())
                .build();
    }

    public static List<ContractResponse> fromList(List<Contract> contracts) {
        return contracts.stream()
                .map(ContractResponse::from)
                .collect(Collectors.toList());
    }
}
```

### 4. Site Domain

#### SiteCreateRequest
```java
package com.concrete.buildup.domain.site.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

/**
 * 현장 등록 요청 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidDateRange(startField = "startDate", endField = "endDate")
public class SiteCreateRequest {

    @NotNull(message = "기업 ID는 필수입니다")
    private Long corporationId;

    @NotBlank(message = "현장명은 필수입니다")
    @Size(min = 2, max = 100, message = "현장명은 2-100자 사이여야 합니다")
    private String siteName;

    @NotBlank(message = "현장 주소는 필수입니다")
    @Size(max = 200, message = "현장 주소는 200자를 초과할 수 없습니다")
    private String siteAddress;

    @NotNull(message = "시작일은 필수입니다")
    private LocalDate startDate;

    private LocalDate endDate;  // 선택사항
}
```

#### SiteResponse
```java
package com.concrete.buildup.domain.site.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 현장 응답 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SiteResponse {

    private Long siteId;
    private String siteName;
    private String siteAddress;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;

    // 시크릿 키 (현장 등록 시에만 반환)
    private String workerSecretKey;
    private String managerSecretKey;

    private LocalDateTime createdAt;

    public static SiteResponse from(Site site) {
        if (site == null) {
            return null;
        }

        return SiteResponse.builder()
                .siteId(site.getId())
                .siteName(site.getName())
                .siteAddress(site.getAddress())
                .startDate(site.getStartDate())
                .endDate(site.getEndDate())
                .status(site.getStatus())
                .createdAt(site.getCreatedAt())
                .build();
    }

    /**
     * 시크릿 키 포함 응답 (현장 등록 시)
     */
    public static SiteResponse fromWithSecretKeys(Site site) {
        SiteResponse response = from(site);
        if (response != null) {
            response.setWorkerSecretKey(site.getWorkerSecretKey());
            response.setManagerSecretKey(site.getManagerSecretKey());
        }
        return response;
    }
}
```

---

## 공통 DTO

### 1. ApiResponse (이미 구현됨)

```java
package com.concrete.buildup.global.common;

/**
 * API 응답 공통 포맷
 * 이미 구현되어 있음 (global/common/ApiResponse.java)
 */
@Getter
@Builder
public class ApiResponse<T> {
    private final boolean success;
    private final String message;
    private final T data;
}
```

### 2. PageResponse

```java
package com.concrete.buildup.global.common;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 페이징 응답 DTO
 */
@Getter
@Builder
public class PageResponse<T> {

    private List<T> content;        // 데이터 목록
    private int pageNumber;         // 현재 페이지 번호 (0부터 시작)
    private int pageSize;           // 페이지 크기
    private long totalElements;     // 전체 요소 수
    private int totalPages;         // 전체 페이지 수
    private boolean first;          // 첫 페이지 여부
    private boolean last;           // 마지막 페이지 여부

    /**
     * Spring Data JPA Page 객체를 PageResponse로 변환
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
```

---

## 추가 규칙

### 1. JsonInclude 사용

```java
// 클래스 레벨: null 필드 제외
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResourceResponse {
    private String optionalField;  // null이면 JSON에서 제외
}

// 필드 레벨: 특정 필드만 제외
public class ResourceResponse {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String optionalField;
}
```

### 2. 날짜/시간 포맷

```java
import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(pattern = "yyyy-MM-dd")
private LocalDate date;

@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
private LocalDateTime dateTime;
```

### 3. Enum 직렬화

```java
import com.fasterxml.jackson.annotation.JsonValue;

public enum EmployeeType {
    REGULAR("정규직"),
    DAILY("일용직");

    private final String description;

    EmployeeType(String description) {
        this.description = description;
    }

    @JsonValue  // JSON 직렬화 시 이 메서드의 반환값 사용
    public String getDescription() {
        return description;
    }
}
```

### 4. 민감 정보 제외

```java
import com.fasterxml.jackson.annotation.JsonIgnore;

public class UserResponse {
    private String username;

    @JsonIgnore  // JSON 직렬화 시 제외
    private String password;

    @JsonIgnore
    private String refreshToken;
}
```

---

## 체크리스트

### DTO 작성 시
- [ ] Request DTO는 `domain/{domain}/dto/` 폴더에 위치
- [ ] Response DTO도 `domain/{domain}/dto/` 폴더에 위치
- [ ] 적절한 Validation 어노테이션 추가
- [ ] Response DTO에 `from()` 메서드 구현
- [ ] 목록 변환이 필요한 경우 `fromList()` 메서드 구현
- [ ] `@JsonInclude(JsonInclude.Include.NON_NULL)` 추가
- [ ] 민감 정보는 `@JsonIgnore`로 제외

### 코드 리뷰 시
- [ ] Validation 메시지가 명확한지 확인
- [ ] null 체크가 적절히 되어 있는지 확인
- [ ] from() 메서드가 null-safe한지 확인
- [ ] 민감 정보가 노출되지 않는지 확인
- [ ] 날짜 형식이 일관되게 사용되는지 확인
