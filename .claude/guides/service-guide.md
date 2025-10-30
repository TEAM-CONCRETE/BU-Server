# Build-Up Platform Service 가이드

## 📋 목차
1. [개요](#개요)
2. [기본 구조](#기본-구조)
3. [필수 규칙](#필수-규칙)
4. [트랜잭션 관리](#트랜잭션-관리)
5. [로깅 패턴](#로깅-패턴)
6. [예외 처리](#예외-처리)
7. [도메인별 Service 예시](#도메인별-service-예시)
8. [CRUD 패턴](#crud-패턴)
9. [DTO 변환 패턴](#dto-변환-패턴)
10. [비즈니스 로직 구조화](#비즈니스-로직-구조화)
11. [페이징 처리](#페이징-처리)
12. [금지사항 및 권장사항](#금지사항-및-권장사항)

---

## 개요

Build-Up Platform의 Service 레이어는 **비즈니스 로직의 핵심**으로, 다음 역할을 담당합니다:

### Service 레이어 역할
- 비즈니스 로직 처리
- 트랜잭션 관리
- 데이터 검증
- Entity ↔ DTO 변환
- Repository 계층 조율
- 예외 처리

### Service 위치
```
src/main/java/com/concrete/buildup/domain/
├── auth/
│   └── service/
│       └── AuthService.java
├── employee/
│   └── service/
│       └── EmployeeService.java
├── site/
│   └── service/
│       └── SiteService.java
├── contract/
│   └── service/
│       └── ContractService.java
├── payroll/
│   └── service/
│       └── PayrollService.java
├── attendance/
│   └── service/
│       └── AttendanceService.java
├── workreport/
│   └── service/
│       └── WorkReportService.java
└── safetydoc/
    └── service/
        └── SafetyDocService.java
```

### 핵심 원칙
- **단일 책임 원칙**: 하나의 Service는 하나의 도메인 담당
- **트랜잭션 일관성**: `@Transactional` 적절히 사용
- **명확한 예외 처리**: BusinessException 계층구조 활용
- **로깅 필수**: 모든 주요 작업에 로그 남기기
- **DTO 변환**: Controller와 Service 사이에서만 DTO 사용

---

## 기본 구조

### 기본 템플릿

```java
package com.concrete.buildup.domain.{domain}.service;

import com.concrete.buildup.domain.{domain}.dto.*;
import com.concrete.buildup.domain.{domain}.entity.{Entity};
import com.concrete.buildup.domain.{domain}.exception.{Entity}ErrorCode;
import com.concrete.buildup.domain.{domain}.repository.{Entity}Repository;
import com.concrete.buildup.global.exception.BusinessException;
import com.concrete.buildup.global.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {Entity} Service
 *
 * 주요 기능:
 * - {기능 1}
 * - {기능 2}
 * - {기능 3}
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class {Entity}Service {

    private final {Entity}Repository {entity}Repository;

    /**
     * {Entity} 조회
     *
     * @param id {Entity} ID
     * @return {Entity} 응답 DTO
     */
    public {Entity}Response get{Entity}(Long id) {
        log.info("[{Entity}Service] get{Entity} - id={}", id);

        {Entity} {entity} = {entity}Repository.findByIdAndNotDeleted(id)
            .orElseThrow(() -> new ResourceNotFoundException({Entity}ErrorCode.{ENTITY}_NOT_FOUND));

        log.info("[{Entity}Service] get{Entity} - success id={}", id);
        return {Entity}Response.from({entity});
    }

    /**
     * {Entity} 생성
     *
     * @param request {Entity} 생성 요청 DTO
     * @return {Entity} 응답 DTO
     */
    @Transactional
    public {Entity}Response create{Entity}({Entity}CreateRequest request) {
        log.info("[{Entity}Service] create{Entity} - {field}={}", request.get{Field}());

        // 검증
        validate{Entity}Uniqueness(request);

        // 엔티티 생성
        {Entity} {entity} = {Entity}.builder()
            .{field}(request.get{Field}())
            .build();

        // 저장
        {Entity} saved = {entity}Repository.save({entity});

        log.info("[{Entity}Service] create{Entity} - success id={}", saved.getId());
        return {Entity}Response.from(saved);
    }

    // 검증 로직
    private void validate{Entity}Uniqueness({Entity}CreateRequest request) {
        // 중복 체크 등
    }
}
```

---

## 필수 규칙

### 1. 클래스 레벨 어노테이션

```java
@Slf4j                              // 로깅 필수
@Service                            // 서비스 레이어 선언
@RequiredArgsConstructor            // 생성자 주입
@Transactional(readOnly = true)     // 기본 읽기 전용
public class EmployeeService {
    // ...
}
```

**필수 어노테이션:**
- `@Slf4j`: Lombok 로깅 지원
- `@Service`: Spring Service 컴포넌트
- `@RequiredArgsConstructor`: final 필드 생성자 주입
- `@Transactional(readOnly = true)`: 클래스 레벨 트랜잭션 설정

### 2. 의존성 주입

```java
// ✅ 생성자 주입 (권장)
@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeRepository employeeRepository;
    private final SiteRepository siteRepository;
    private final EmailService emailService;
}

// ❌ 필드 주입 (금지)
@Service
public class EmployeeService {
    @Autowired
    private EmployeeRepository employeeRepository;
}

// ❌ Setter 주입 (금지)
@Service
public class EmployeeService {
    private EmployeeRepository employeeRepository;

    @Autowired
    public void setEmployeeRepository(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }
}
```

### 3. 메서드 명명 규칙

| 작업 | 메서드 명명 | 예시 |
|------|-----------|------|
| 단건 조회 | `getXxx`, `getXxxById` | `getEmployee(Long id)` |
| 목록 조회 | `getAllXxx`, `getXxxList` | `getAllEmployees()` |
| 페이징 조회 | `getXxxPage` | `getEmployeePage(Pageable pageable)` |
| 생성 | `createXxx`, `registerXxx` | `createEmployee(EmployeeCreateRequest)` |
| 수정 | `updateXxx`, `modifyXxx` | `updateEmployee(Long id, EmployeeUpdateRequest)` |
| 삭제 | `deleteXxx`, `removeXxx` | `deleteEmployee(Long id)` |
| 상태 변경 | `activateXxx`, `terminateXxx` | `terminateEmployee(Long id)` |
| 검증 | `validateXxx` | `validatePhoneNumber(String phone)` |
| 검색 | `searchXxx` | `searchEmployees(String keyword)` |

### 4. 반환 타입

```java
// ✅ DTO 반환 (권장)
public EmployeeResponse getEmployee(Long id) {
    Employee employee = employeeRepository.findByIdAndNotDeleted(id)
        .orElseThrow(() -> new ResourceNotFoundException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND));
    return EmployeeResponse.from(employee);
}

// ✅ Page<DTO> 반환
public Page<EmployeeResponse> getEmployeePage(Pageable pageable) {
    Page<Employee> employees = employeeRepository.findAllNotDeleted(pageable);
    return employees.map(EmployeeResponse::from);
}

// ✅ List<DTO> 반환
public List<EmployeeSummaryDto> getEmployeeSummaries(Long siteId) {
    return employeeRepository.findAllBySiteIdAndNotDeleted(siteId)
        .stream()
        .map(EmployeeSummaryDto::from)
        .toList();
}

// ❌ Entity 직접 반환 (금지)
public Employee getEmployee(Long id) {
    return employeeRepository.findById(id).orElseThrow();
}
```

---

## 트랜잭션 관리

### 기본 원칙

1. **클래스 레벨**: `@Transactional(readOnly = true)` - 기본 읽기 전용
2. **조회 메서드**: 별도 어노테이션 불필요 (클래스 레벨 상속)
3. **쓰기 메서드**: `@Transactional` 명시 (readOnly=false 자동 적용)

### 트랜잭션 패턴

```java
@Transactional(readOnly = true)  // 클래스 레벨
public class EmployeeService {

    // ✅ 조회 메서드 - 별도 어노테이션 불필요
    public EmployeeResponse getEmployee(Long id) {
        // readOnly=true (클래스 레벨 상속)
        Employee employee = employeeRepository.findByIdAndNotDeleted(id)
            .orElseThrow(() -> new ResourceNotFoundException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND));
        return EmployeeResponse.from(employee);
    }

    // ✅ 생성 메서드 - @Transactional 명시
    @Transactional
    public EmployeeResponse createEmployee(EmployeeCreateRequest request) {
        // readOnly=false (쓰기 가능)
        validateEmployeeUniqueness(request);

        Employee employee = Employee.builder()
            .name(request.getName())
            .phoneNumber(request.getPhoneNumber())
            .build();

        Employee saved = employeeRepository.save(employee);
        return EmployeeResponse.from(saved);
    }

    // ✅ 수정 메서드 - @Transactional 명시
    @Transactional
    public EmployeeResponse updateEmployee(Long id, EmployeeUpdateRequest request) {
        Employee employee = employeeRepository.findByIdAndNotDeleted(id)
            .orElseThrow(() -> new ResourceNotFoundException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND));

        employee.updateInfo(request.getName(), request.getPhoneNumber());
        // JPA Dirty Checking으로 자동 저장

        return EmployeeResponse.from(employee);
    }

    // ✅ 삭제 메서드 - @Transactional 명시 (소프트 삭제)
    @Transactional
    public void deleteEmployee(Long id) {
        Employee employee = employeeRepository.findByIdAndNotDeleted(id)
            .orElseThrow(() -> new ResourceNotFoundException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND));

        employee.delete();  // isDeleted = true
        // JPA Dirty Checking으로 자동 저장
    }
}
```

### 트랜잭션 전파 (Propagation)

```java
// 기본: REQUIRED (기존 트랜잭션 사용, 없으면 새로 생성)
@Transactional
public void method1() {
    // ...
}

// REQUIRES_NEW: 항상 새 트랜잭션 생성
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void method2() {
    // 독립적인 트랜잭션
}

// 예시: 로그 저장은 별도 트랜잭션으로
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void saveAuditLog(AuditLog log) {
    auditLogRepository.save(log);
}
```

---

## 로깅 패턴

### 기본 로깅 패턴

```java
public EmployeeResponse getEmployee(Long id) {
    // 1. 진입 로그 (주요 파라미터 포함)
    log.info("[EmployeeService] getEmployee - id={}", id);

    // 2. 비즈니스 로직
    Employee employee = employeeRepository.findByIdAndNotDeleted(id)
        .orElseThrow(() -> new ResourceNotFoundException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND));

    // 3. 성공 로그
    log.info("[EmployeeService] getEmployee - success id={}", id);
    return EmployeeResponse.from(employee);
}
```

### 로그 레벨 가이드

```java
// INFO: 정상 흐름 (진입, 성공, 주요 상태 변경)
log.info("[EmployeeService] createEmployee - name={}", request.getName());
log.info("[EmployeeService] createEmployee - success id={}", saved.getId());

// DEBUG: 상세 디버깅 정보
log.debug("[EmployeeService] validatePhoneNumber - phoneNumber={}", phoneNumber);

// WARN: 경고 (예상 가능한 비정상 상황)
log.warn("[EmployeeService] deleteEmployee - already deleted id={}", id);

// ERROR: 에러 (예외 발생 시)
log.error("[EmployeeService] createEmployee - validation failed", exception);
```

### 민감정보 로깅 주의

```java
// ❌ 민감정보 직접 로깅 (금지)
log.info("[EmployeeService] createEmployee - residentNumber={}", request.getResidentNumber());
log.info("[EmployeeService] updateSalary - salary={}", request.getSalary());

// ✅ 마스킹 처리 (권장)
log.info("[EmployeeService] createEmployee - residentNumber={}",
    maskResidentNumber(request.getResidentNumber()));

// ✅ ID만 로깅 (권장)
log.info("[EmployeeService] updateSalary - employeeId={}", id);
```

### 복잡한 작업 로깅

```java
@Transactional
public PayrollResponse calculatePayroll(Long employeeId, YearMonth month) {
    log.info("[PayrollService] calculatePayroll - employeeId={}, month={}", employeeId, month);

    // 1. 사원 조회
    Employee employee = employeeRepository.findByIdAndNotDeleted(employeeId)
        .orElseThrow(() -> new ResourceNotFoundException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND));
    log.debug("[PayrollService] calculatePayroll - employee found name={}", employee.getName());

    // 2. 근태 조회
    List<Attendance> attendances = attendanceRepository.findByEmployeeIdAndMonth(employeeId, month);
    log.debug("[PayrollService] calculatePayroll - attendances count={}", attendances.size());

    // 3. 급여 계산
    long totalAmount = calculateTotalAmount(attendances);
    log.debug("[PayrollService] calculatePayroll - totalAmount={}", totalAmount);

    // 4. 급여 저장
    Payroll payroll = Payroll.builder()
        .employeeId(employeeId)
        .paymentMonth(month)
        .totalAmount(totalAmount)
        .build();

    Payroll saved = payrollRepository.save(payroll);
    log.info("[PayrollService] calculatePayroll - success payrollId={}", saved.getId());

    return PayrollResponse.from(saved);
}
```

---

## 예외 처리

### 예외 처리 원칙

1. **ResourceNotFoundException**: 데이터 없음 (404)
2. **BusinessException**: 비즈니스 규칙 위반 (400, 409 등)
3. **AuthorizationException**: 권한 없음 (403)
4. **예외는 Service에서 던지고, GlobalExceptionHandler가 처리**

### 예외 처리 패턴

#### 1. ResourceNotFoundException (데이터 없음)

```java
public EmployeeResponse getEmployee(Long id) {
    log.info("[EmployeeService] getEmployee - id={}", id);

    // ✅ Optional.orElseThrow() 사용
    Employee employee = employeeRepository.findByIdAndNotDeleted(id)
        .orElseThrow(() -> new ResourceNotFoundException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND));

    log.info("[EmployeeService] getEmployee - success id={}", id);
    return EmployeeResponse.from(employee);
}

// ✅ 커스텀 메시지와 함께
public SiteResponse getSite(Long id) {
    Site site = siteRepository.findByIdAndNotDeleted(id)
        .orElseThrow(() -> new ResourceNotFoundException(
            SiteErrorCode.SITE_NOT_FOUND,
            "ID가 " + id + "인 현장을 찾을 수 없습니다."
        ));
    return SiteResponse.from(site);
}
```

#### 2. BusinessException (비즈니스 규칙 위반)

```java
@Transactional
public EmployeeResponse createEmployee(EmployeeCreateRequest request) {
    log.info("[EmployeeService] createEmployee - name={}", request.getName());

    // ✅ 중복 체크
    if (employeeRepository.existsByPhoneNumberAndNotDeleted(request.getPhoneNumber())) {
        throw new BusinessException(EmployeeErrorCode.DUPLICATE_PHONE_NUMBER);
    }

    // ✅ 상태 검증
    if (request.getStartDate().isAfter(LocalDate.now())) {
        throw new BusinessException(EmployeeErrorCode.INVALID_START_DATE);
    }

    Employee employee = Employee.builder()
        .name(request.getName())
        .phoneNumber(request.getPhoneNumber())
        .build();

    Employee saved = employeeRepository.save(employee);
    log.info("[EmployeeService] createEmployee - success id={}", saved.getId());

    return EmployeeResponse.from(saved);
}
```

#### 3. AuthorizationException (권한 없음)

```java
@Transactional
public void deleteSite(Long userId, Long siteId) {
    log.info("[SiteService] deleteSite - userId={}, siteId={}", userId, siteId);

    Site site = siteRepository.findByIdAndNotDeleted(siteId)
        .orElseThrow(() -> new ResourceNotFoundException(SiteErrorCode.SITE_NOT_FOUND));

    // ✅ 권한 확인
    if (!site.getManagerId().equals(userId)) {
        throw new AuthorizationException(userId, "Site", "delete");
    }

    site.delete();
    log.info("[SiteService] deleteSite - success siteId={}", siteId);
}
```

#### 4. 검증 로직 분리

```java
@Transactional
public EmployeeResponse createEmployee(EmployeeCreateRequest request) {
    log.info("[EmployeeService] createEmployee - name={}", request.getName());

    // 검증 메서드 호출
    validateEmployeeUniqueness(request);
    validateEmployeeData(request);

    Employee employee = buildEmployee(request);
    Employee saved = employeeRepository.save(employee);

    log.info("[EmployeeService] createEmployee - success id={}", saved.getId());
    return EmployeeResponse.from(saved);
}

// 검증 로직 분리
private void validateEmployeeUniqueness(EmployeeCreateRequest request) {
    if (employeeRepository.existsByPhoneNumberAndNotDeleted(request.getPhoneNumber())) {
        throw new BusinessException(EmployeeErrorCode.DUPLICATE_PHONE_NUMBER);
    }

    if (employeeRepository.existsByResidentNumberAndNotDeleted(request.getResidentNumber())) {
        throw new BusinessException(EmployeeErrorCode.DUPLICATE_RESIDENT_NUMBER);
    }
}

private void validateEmployeeData(EmployeeCreateRequest request) {
    if (request.getStartDate().isAfter(LocalDate.now())) {
        throw new BusinessException(EmployeeErrorCode.INVALID_START_DATE);
    }

    if (request.getAge() < 18) {
        throw new BusinessException(EmployeeErrorCode.UNDERAGE_EMPLOYEE);
    }
}

private Employee buildEmployee(EmployeeCreateRequest request) {
    return Employee.builder()
        .name(request.getName())
        .phoneNumber(request.getPhoneNumber())
        .residentNumber(request.getResidentNumber())
        .startDate(request.getStartDate())
        .build();
}
```

---

## 도메인별 Service 예시

### 1. Employee Domain - EmployeeService

```java
package com.concrete.buildup.domain.employee.service;

import com.concrete.buildup.domain.employee.dto.*;
import com.concrete.buildup.domain.employee.entity.Employee;
import com.concrete.buildup.domain.employee.entity.EmployeeStatus;
import com.concrete.buildup.domain.employee.exception.EmployeeErrorCode;
import com.concrete.buildup.domain.employee.repository.EmployeeRepository;
import com.concrete.buildup.global.exception.BusinessException;
import com.concrete.buildup.global.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Employee Service
 *
 * 주요 기능:
 * - 사원 등록/조회/수정/삭제
 * - 사원 검색
 * - 사원 상태 관리
 * - 퇴사 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    /**
     * 사원 조회
     *
     * @param id 사원 ID
     * @return 사원 응답 DTO
     */
    public EmployeeResponse getEmployee(Long id) {
        log.info("[EmployeeService] getEmployee - id={}", id);

        Employee employee = employeeRepository.findByIdAndNotDeleted(id)
            .orElseThrow(() -> new ResourceNotFoundException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND));

        log.info("[EmployeeService] getEmployee - success id={}", id);
        return EmployeeResponse.from(employee);
    }

    /**
     * 모든 사원 조회 (페이징)
     *
     * @param pageable 페이징 정보
     * @return 사원 목록 (Page)
     */
    public Page<EmployeeResponse> getEmployeePage(Pageable pageable) {
        log.info("[EmployeeService] getEmployeePage - page={}, size={}",
            pageable.getPageNumber(), pageable.getPageSize());

        Page<Employee> employees = employeeRepository.findAllNotDeleted(pageable);

        log.info("[EmployeeService] getEmployeePage - success totalElements={}",
            employees.getTotalElements());
        return employees.map(EmployeeResponse::from);
    }

    /**
     * 현장별 사원 조회
     *
     * @param siteId 현장 ID
     * @return 사원 목록
     */
    public List<EmployeeResponse> getEmployeesBySite(Long siteId) {
        log.info("[EmployeeService] getEmployeesBySite - siteId={}", siteId);

        List<Employee> employees = employeeRepository.findAllBySiteIdAndNotDeleted(siteId);

        log.info("[EmployeeService] getEmployeesBySite - success count={}", employees.size());
        return employees.stream()
            .map(EmployeeResponse::from)
            .toList();
    }

    /**
     * 사원 등록
     *
     * @param request 사원 등록 요청 DTO
     * @return 사원 응답 DTO
     */
    @Transactional
    public EmployeeResponse createEmployee(EmployeeCreateRequest request) {
        log.info("[EmployeeService] createEmployee - name={}", request.getName());

        // 검증
        validateEmployeeUniqueness(request);
        validateEmployeeData(request);

        // 엔티티 생성
        Employee employee = Employee.builder()
            .name(request.getName())
            .phoneNumber(request.getPhoneNumber())
            .residentNumber(request.getResidentNumber())
            .siteId(request.getSiteId())
            .position(request.getPosition())
            .startDate(request.getStartDate())
            .status(EmployeeStatus.ACTIVE)
            .build();

        // 저장
        Employee saved = employeeRepository.save(employee);

        log.info("[EmployeeService] createEmployee - success id={}", saved.getId());
        return EmployeeResponse.from(saved);
    }

    /**
     * 사원 정보 수정
     *
     * @param id 사원 ID
     * @param request 사원 수정 요청 DTO
     * @return 사원 응답 DTO
     */
    @Transactional
    public EmployeeResponse updateEmployee(Long id, EmployeeUpdateRequest request) {
        log.info("[EmployeeService] updateEmployee - id={}", id);

        Employee employee = employeeRepository.findByIdAndNotDeleted(id)
            .orElseThrow(() -> new ResourceNotFoundException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND));

        // 전화번호 변경 시 중복 체크
        if (!employee.getPhoneNumber().equals(request.getPhoneNumber())) {
            if (employeeRepository.existsByPhoneNumberAndNotDeleted(request.getPhoneNumber())) {
                throw new BusinessException(EmployeeErrorCode.DUPLICATE_PHONE_NUMBER);
            }
        }

        // 엔티티 업데이트 (Dirty Checking)
        employee.updateInfo(
            request.getName(),
            request.getPhoneNumber(),
            request.getPosition()
        );

        log.info("[EmployeeService] updateEmployee - success id={}", id);
        return EmployeeResponse.from(employee);
    }

    /**
     * 사원 삭제 (소프트 삭제)
     *
     * @param id 사원 ID
     */
    @Transactional
    public void deleteEmployee(Long id) {
        log.info("[EmployeeService] deleteEmployee - id={}", id);

        Employee employee = employeeRepository.findByIdAndNotDeleted(id)
            .orElseThrow(() -> new ResourceNotFoundException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND));

        // 소프트 삭제
        employee.delete();

        log.info("[EmployeeService] deleteEmployee - success id={}", id);
    }

    /**
     * 사원 퇴사 처리
     *
     * @param id 사원 ID
     * @param request 퇴사 요청 DTO
     */
    @Transactional
    public void terminateEmployee(Long id, EmployeeTerminationRequest request) {
        log.info("[EmployeeService] terminateEmployee - id={}", id);

        Employee employee = employeeRepository.findByIdAndNotDeleted(id)
            .orElseThrow(() -> new ResourceNotFoundException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND));

        // 이미 퇴사 처리된 경우
        if (employee.getStatus() == EmployeeStatus.TERMINATED) {
            throw new BusinessException(EmployeeErrorCode.EMPLOYEE_ALREADY_TERMINATED);
        }

        // 퇴사 처리
        employee.terminate(request.getTerminationDate(), request.getReason());

        log.info("[EmployeeService] terminateEmployee - success id={}", id);
    }

    /**
     * 사원 검색 (이름)
     *
     * @param name 검색어
     * @param pageable 페이징 정보
     * @return 사원 목록 (Page)
     */
    public Page<EmployeeResponse> searchEmployeesByName(String name, Pageable pageable) {
        log.info("[EmployeeService] searchEmployeesByName - name={}", name);

        Page<Employee> employees = employeeRepository.searchByNameAndNotDeleted(name, pageable);

        log.info("[EmployeeService] searchEmployeesByName - success count={}",
            employees.getTotalElements());
        return employees.map(EmployeeResponse::from);
    }

    /**
     * 현장별 활성 사원 수 조회
     *
     * @param siteId 현장 ID
     * @return 활성 사원 수
     */
    public long countActiveBySite(Long siteId) {
        log.info("[EmployeeService] countActiveBySite - siteId={}", siteId);

        long count = employeeRepository.countBySiteIdAndStatusAndNotDeleted(
            siteId, EmployeeStatus.ACTIVE
        );

        log.info("[EmployeeService] countActiveBySite - success count={}", count);
        return count;
    }

    // === 검증 메서드 ===

    /**
     * 사원 중복 검증
     */
    private void validateEmployeeUniqueness(EmployeeCreateRequest request) {
        if (employeeRepository.existsByPhoneNumberAndNotDeleted(request.getPhoneNumber())) {
            throw new BusinessException(EmployeeErrorCode.DUPLICATE_PHONE_NUMBER);
        }

        if (employeeRepository.existsByResidentNumberAndNotDeleted(request.getResidentNumber())) {
            throw new BusinessException(EmployeeErrorCode.DUPLICATE_RESIDENT_NUMBER);
        }
    }

    /**
     * 사원 데이터 검증
     */
    private void validateEmployeeData(EmployeeCreateRequest request) {
        if (request.getStartDate().isAfter(LocalDate.now())) {
            throw new BusinessException(EmployeeErrorCode.INVALID_START_DATE);
        }
    }
}
```

### 2. Site Domain - SiteService

```java
package com.concrete.buildup.domain.site.service;

import com.concrete.buildup.domain.site.dto.*;
import com.concrete.buildup.domain.site.entity.Site;
import com.concrete.buildup.domain.site.entity.SiteStatus;
import com.concrete.buildup.domain.site.exception.SiteErrorCode;
import com.concrete.buildup.domain.site.repository.SiteRepository;
import com.concrete.buildup.global.exception.AuthorizationException;
import com.concrete.buildup.global.exception.BusinessException;
import com.concrete.buildup.global.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Site Service
 *
 * 주요 기능:
 * - 현장 등록/조회/수정/삭제
 * - 현장 상태 관리
 * - 관리자별 현장 조회
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SiteService {

    private final SiteRepository siteRepository;

    /**
     * 현장 조회
     *
     * @param id 현장 ID
     * @return 현장 응답 DTO
     */
    public SiteResponse getSite(Long id) {
        log.info("[SiteService] getSite - id={}", id);

        Site site = siteRepository.findByIdAndNotDeleted(id)
            .orElseThrow(() -> new ResourceNotFoundException(SiteErrorCode.SITE_NOT_FOUND));

        log.info("[SiteService] getSite - success id={}", id);
        return SiteResponse.from(site);
    }

    /**
     * 모든 현장 조회 (페이징)
     *
     * @param pageable 페이징 정보
     * @return 현장 목록 (Page)
     */
    public Page<SiteResponse> getSitePage(Pageable pageable) {
        log.info("[SiteService] getSitePage - page={}, size={}",
            pageable.getPageNumber(), pageable.getPageSize());

        Page<Site> sites = siteRepository.findAllNotDeleted(pageable);

        log.info("[SiteService] getSitePage - success totalElements={}", sites.getTotalElements());
        return sites.map(SiteResponse::from);
    }

    /**
     * 관리자별 현장 조회
     *
     * @param managerId 관리자 ID
     * @return 현장 목록
     */
    public List<SiteResponse> getSitesByManager(Long managerId) {
        log.info("[SiteService] getSitesByManager - managerId={}", managerId);

        List<Site> sites = siteRepository.findByManagerIdAndNotDeleted(managerId);

        log.info("[SiteService] getSitesByManager - success count={}", sites.size());
        return sites.stream()
            .map(SiteResponse::from)
            .toList();
    }

    /**
     * 현장 등록
     *
     * @param request 현장 등록 요청 DTO
     * @return 현장 응답 DTO
     */
    @Transactional
    public SiteResponse createSite(SiteCreateRequest request) {
        log.info("[SiteService] createSite - name={}", request.getName());

        // 검증
        validateSiteUniqueness(request);
        validateSitePeriod(request.getStartDate(), request.getEndDate());

        // 엔티티 생성
        Site site = Site.builder()
            .code(request.getCode())
            .name(request.getName())
            .address(request.getAddress())
            .managerId(request.getManagerId())
            .startDate(request.getStartDate())
            .endDate(request.getEndDate())
            .status(SiteStatus.ACTIVE)
            .build();

        // 저장
        Site saved = siteRepository.save(site);

        log.info("[SiteService] createSite - success id={}", saved.getId());
        return SiteResponse.from(saved);
    }

    /**
     * 현장 정보 수정
     *
     * @param id 현장 ID
     * @param request 현장 수정 요청 DTO
     * @return 현장 응답 DTO
     */
    @Transactional
    public SiteResponse updateSite(Long id, SiteUpdateRequest request) {
        log.info("[SiteService] updateSite - id={}", id);

        Site site = siteRepository.findByIdAndNotDeleted(id)
            .orElseThrow(() -> new ResourceNotFoundException(SiteErrorCode.SITE_NOT_FOUND));

        // 기간 검증
        validateSitePeriod(request.getStartDate(), request.getEndDate());

        // 엔티티 업데이트
        site.updateInfo(
            request.getName(),
            request.getAddress(),
            request.getStartDate(),
            request.getEndDate()
        );

        log.info("[SiteService] updateSite - success id={}", id);
        return SiteResponse.from(site);
    }

    /**
     * 현장 삭제 (소프트 삭제)
     *
     * @param userId 사용자 ID
     * @param siteId 현장 ID
     */
    @Transactional
    public void deleteSite(Long userId, Long siteId) {
        log.info("[SiteService] deleteSite - userId={}, siteId={}", userId, siteId);

        Site site = siteRepository.findByIdAndNotDeleted(siteId)
            .orElseThrow(() -> new ResourceNotFoundException(SiteErrorCode.SITE_NOT_FOUND));

        // 권한 확인 (관리자만 삭제 가능)
        if (!site.getManagerId().equals(userId)) {
            throw new AuthorizationException(userId, "Site", "delete");
        }

        // 소프트 삭제
        site.delete();

        log.info("[SiteService] deleteSite - success siteId={}", siteId);
    }

    /**
     * 현장 종료
     *
     * @param id 현장 ID
     */
    @Transactional
    public void closeSite(Long id) {
        log.info("[SiteService] closeSite - id={}", id);

        Site site = siteRepository.findByIdAndNotDeleted(id)
            .orElseThrow(() -> new ResourceNotFoundException(SiteErrorCode.SITE_NOT_FOUND));

        // 이미 종료된 현장인 경우
        if (site.getStatus() == SiteStatus.CLOSED) {
            throw new BusinessException(SiteErrorCode.SITE_ALREADY_CLOSED);
        }

        // 현장 종료
        site.close();

        log.info("[SiteService] closeSite - success id={}", id);
    }

    /**
     * 현장명 검색
     *
     * @param name 검색어
     * @param pageable 페이징 정보
     * @return 현장 목록 (Page)
     */
    public Page<SiteResponse> searchSitesByName(String name, Pageable pageable) {
        log.info("[SiteService] searchSitesByName - name={}", name);

        Page<Site> sites = siteRepository.searchByNameAndNotDeleted(name, pageable);

        log.info("[SiteService] searchSitesByName - success count={}", sites.getTotalElements());
        return sites.map(SiteResponse::from);
    }

    // === 검증 메서드 ===

    /**
     * 현장 중복 검증
     */
    private void validateSiteUniqueness(SiteCreateRequest request) {
        if (siteRepository.existsByCodeAndNotDeleted(request.getCode())) {
            throw new BusinessException(SiteErrorCode.DUPLICATE_SITE_CODE);
        }
    }

    /**
     * 현장 기간 검증
     */
    private void validateSitePeriod(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new BusinessException(SiteErrorCode.INVALID_SITE_PERIOD);
        }
    }
}
```

### 3. Payroll Domain - PayrollService

```java
package com.concrete.buildup.domain.payroll.service;

import com.concrete.buildup.domain.attendance.entity.Attendance;
import com.concrete.buildup.domain.attendance.repository.AttendanceRepository;
import com.concrete.buildup.domain.employee.entity.Employee;
import com.concrete.buildup.domain.employee.exception.EmployeeErrorCode;
import com.concrete.buildup.domain.employee.repository.EmployeeRepository;
import com.concrete.buildup.domain.payroll.dto.*;
import com.concrete.buildup.domain.payroll.entity.Payroll;
import com.concrete.buildup.domain.payroll.entity.PayrollStatus;
import com.concrete.buildup.domain.payroll.exception.PayrollErrorCode;
import com.concrete.buildup.domain.payroll.repository.PayrollRepository;
import com.concrete.buildup.global.exception.BusinessException;
import com.concrete.buildup.global.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;

/**
 * Payroll Service
 *
 * 주요 기능:
 * - 급여 계산 및 생성
 * - 급여 조회 및 관리
 * - 급여 지급 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PayrollService {

    private final PayrollRepository payrollRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;

    /**
     * 급여 조회
     *
     * @param id 급여 ID
     * @return 급여 응답 DTO
     */
    public PayrollResponse getPayroll(Long id) {
        log.info("[PayrollService] getPayroll - id={}", id);

        Payroll payroll = payrollRepository.findByIdAndNotDeleted(id)
            .orElseThrow(() -> new ResourceNotFoundException(PayrollErrorCode.PAYROLL_NOT_FOUND));

        log.info("[PayrollService] getPayroll - success id={}", id);
        return PayrollResponse.from(payroll);
    }

    /**
     * 사원별 급여 이력 조회
     *
     * @param employeeId 사원 ID
     * @return 급여 목록
     */
    public List<PayrollResponse> getPayrollsByEmployee(Long employeeId) {
        log.info("[PayrollService] getPayrollsByEmployee - employeeId={}", employeeId);

        List<Payroll> payrolls = payrollRepository.findByEmployeeIdAndNotDeleted(employeeId);

        log.info("[PayrollService] getPayrollsByEmployee - success count={}", payrolls.size());
        return payrolls.stream()
            .map(PayrollResponse::from)
            .toList();
    }

    /**
     * 현장 및 월별 급여 조회
     *
     * @param siteId 현장 ID
     * @param paymentMonth 지급 월
     * @param pageable 페이징 정보
     * @return 급여 목록 (Page)
     */
    public Page<PayrollResponse> getPayrollsBySiteAndMonth(
            Long siteId, YearMonth paymentMonth, Pageable pageable) {

        log.info("[PayrollService] getPayrollsBySiteAndMonth - siteId={}, month={}",
            siteId, paymentMonth);

        Page<Payroll> payrolls = payrollRepository.findBySiteIdAndPaymentMonthAndNotDeleted(
            siteId, paymentMonth, pageable
        );

        log.info("[PayrollService] getPayrollsBySiteAndMonth - success count={}",
            payrolls.getTotalElements());
        return payrolls.map(PayrollResponse::from);
    }

    /**
     * 급여 계산 및 생성
     *
     * @param request 급여 계산 요청 DTO
     * @return 급여 응답 DTO
     */
    @Transactional
    public PayrollResponse calculatePayroll(PayrollCalculateRequest request) {
        log.info("[PayrollService] calculatePayroll - employeeId={}, month={}",
            request.getEmployeeId(), request.getPaymentMonth());

        // 1. 사원 조회
        Employee employee = employeeRepository.findByIdAndNotDeleted(request.getEmployeeId())
            .orElseThrow(() -> new ResourceNotFoundException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND));

        // 2. 중복 체크 (이미 해당 월 급여가 있는지)
        if (payrollRepository.findByEmployeeIdAndPaymentMonthAndNotDeleted(
                request.getEmployeeId(), request.getPaymentMonth()).isPresent()) {
            throw new BusinessException(PayrollErrorCode.PAYROLL_ALREADY_EXISTS);
        }

        // 3. 근태 조회
        List<Attendance> attendances = attendanceRepository.findByEmployeeIdAndMonth(
            request.getEmployeeId(), request.getPaymentMonth()
        );
        log.debug("[PayrollService] calculatePayroll - attendances count={}", attendances.size());

        // 4. 급여 계산
        long totalAmount = calculateTotalAmount(attendances, employee.getDailySalary());
        log.debug("[PayrollService] calculatePayroll - totalAmount={}", totalAmount);

        // 5. 급여 엔티티 생성
        Payroll payroll = Payroll.builder()
            .employeeId(request.getEmployeeId())
            .siteId(employee.getSiteId())
            .paymentMonth(request.getPaymentMonth())
            .workDays(attendances.size())
            .dailySalary(employee.getDailySalary())
            .totalAmount(totalAmount)
            .status(PayrollStatus.PENDING)
            .build();

        // 6. 저장
        Payroll saved = payrollRepository.save(payroll);

        log.info("[PayrollService] calculatePayroll - success payrollId={}", saved.getId());
        return PayrollResponse.from(saved);
    }

    /**
     * 급여 지급 처리
     *
     * @param id 급여 ID
     */
    @Transactional
    public void payPayroll(Long id) {
        log.info("[PayrollService] payPayroll - id={}", id);

        Payroll payroll = payrollRepository.findByIdAndNotDeleted(id)
            .orElseThrow(() -> new ResourceNotFoundException(PayrollErrorCode.PAYROLL_NOT_FOUND));

        // 이미 지급된 경우
        if (payroll.getStatus() == PayrollStatus.PAID) {
            throw new BusinessException(PayrollErrorCode.PAYROLL_ALREADY_PAID);
        }

        // 지급 금액 검증
        if (payroll.getTotalAmount() <= 0) {
            throw new BusinessException(PayrollErrorCode.INVALID_PAYMENT_AMOUNT);
        }

        // 지급 처리
        payroll.markAsPaid();

        log.info("[PayrollService] payPayroll - success id={}", id);
    }

    /**
     * 월별 총 급여액 계산
     *
     * @param paymentMonth 지급 월
     * @return 총 급여액
     */
    public long getTotalAmountByMonth(YearMonth paymentMonth) {
        log.info("[PayrollService] getTotalAmountByMonth - month={}", paymentMonth);

        Long totalAmount = payrollRepository.sumTotalAmountByPaymentMonthAndNotDeleted(paymentMonth);

        log.info("[PayrollService] getTotalAmountByMonth - success amount={}", totalAmount);
        return totalAmount != null ? totalAmount : 0L;
    }

    // === 계산 메서드 ===

    /**
     * 총 급여 계산
     */
    private long calculateTotalAmount(List<Attendance> attendances, long dailySalary) {
        return attendances.size() * dailySalary;
    }
}
```

---

## CRUD 패턴

### 기본 CRUD 템플릿

```java
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResourceService {

    private final ResourceRepository resourceRepository;

    // ===== CREATE =====

    /**
     * 리소스 생성
     */
    @Transactional
    public ResourceResponse createResource(ResourceCreateRequest request) {
        log.info("[ResourceService] createResource - name={}", request.getName());

        // 1. 검증
        validateResourceUniqueness(request);

        // 2. 엔티티 생성
        Resource resource = Resource.builder()
            .name(request.getName())
            .build();

        // 3. 저장
        Resource saved = resourceRepository.save(resource);

        log.info("[ResourceService] createResource - success id={}", saved.getId());
        return ResourceResponse.from(saved);
    }

    // ===== READ =====

    /**
     * 리소스 단건 조회
     */
    public ResourceResponse getResource(Long id) {
        log.info("[ResourceService] getResource - id={}", id);

        Resource resource = resourceRepository.findByIdAndNotDeleted(id)
            .orElseThrow(() -> new ResourceNotFoundException(ResourceErrorCode.RESOURCE_NOT_FOUND));

        log.info("[ResourceService] getResource - success id={}", id);
        return ResourceResponse.from(resource);
    }

    /**
     * 리소스 목록 조회 (페이징)
     */
    public Page<ResourceResponse> getResourcePage(Pageable pageable) {
        log.info("[ResourceService] getResourcePage - page={}, size={}",
            pageable.getPageNumber(), pageable.getPageSize());

        Page<Resource> resources = resourceRepository.findAllNotDeleted(pageable);

        log.info("[ResourceService] getResourcePage - success totalElements={}",
            resources.getTotalElements());
        return resources.map(ResourceResponse::from);
    }

    // ===== UPDATE =====

    /**
     * 리소스 수정
     */
    @Transactional
    public ResourceResponse updateResource(Long id, ResourceUpdateRequest request) {
        log.info("[ResourceService] updateResource - id={}", id);

        // 1. 조회
        Resource resource = resourceRepository.findByIdAndNotDeleted(id)
            .orElseThrow(() -> new ResourceNotFoundException(ResourceErrorCode.RESOURCE_NOT_FOUND));

        // 2. 업데이트 (Dirty Checking)
        resource.updateInfo(request.getName());

        log.info("[ResourceService] updateResource - success id={}", id);
        return ResourceResponse.from(resource);
    }

    // ===== DELETE =====

    /**
     * 리소스 삭제 (소프트 삭제)
     */
    @Transactional
    public void deleteResource(Long id) {
        log.info("[ResourceService] deleteResource - id={}", id);

        Resource resource = resourceRepository.findByIdAndNotDeleted(id)
            .orElseThrow(() -> new ResourceNotFoundException(ResourceErrorCode.RESOURCE_NOT_FOUND));

        // 소프트 삭제
        resource.delete();

        log.info("[ResourceService] deleteResource - success id={}", id);
    }

    // ===== 검증 메서드 =====

    private void validateResourceUniqueness(ResourceCreateRequest request) {
        if (resourceRepository.existsByNameAndNotDeleted(request.getName())) {
            throw new BusinessException(ResourceErrorCode.DUPLICATE_RESOURCE_NAME);
        }
    }
}
```

---

## DTO 변환 패턴

### DTO 변환 위치

```
Controller ←→ DTO ←→ Service ←→ Entity ←→ Repository
           (변환)         (변환)
```

### DTO 변환 메서드

#### 1. Entity → DTO (from 메서드)

```java
// DTO 클래스 내부에 static from 메서드 정의
@Getter
@Builder
public class EmployeeResponse {
    private Long id;
    private String name;
    private String phoneNumber;
    private EmployeeStatus status;
    private LocalDateTime createdAt;

    /**
     * Entity → DTO 변환
     */
    public static EmployeeResponse from(Employee employee) {
        return EmployeeResponse.builder()
            .id(employee.getId())
            .name(employee.getName())
            .phoneNumber(employee.getPhoneNumber())
            .status(employee.getStatus())
            .createdAt(employee.getCreatedAt())
            .build();
    }
}

// Service에서 사용
public EmployeeResponse getEmployee(Long id) {
    Employee employee = employeeRepository.findByIdAndNotDeleted(id)
        .orElseThrow(() -> new ResourceNotFoundException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND));

    return EmployeeResponse.from(employee);  // Entity → DTO
}
```

#### 2. DTO → Entity (toEntity 메서드 또는 Builder)

```java
// DTO 클래스 내부에 toEntity 메서드 (선택사항)
@Getter
@Builder
public class EmployeeCreateRequest {
    private String name;
    private String phoneNumber;
    private Long siteId;

    /**
     * DTO → Entity 변환
     */
    public Employee toEntity() {
        return Employee.builder()
            .name(this.name)
            .phoneNumber(this.phoneNumber)
            .siteId(this.siteId)
            .status(EmployeeStatus.ACTIVE)
            .build();
    }
}

// Service에서 사용 (방법 1: toEntity 사용)
@Transactional
public EmployeeResponse createEmployee(EmployeeCreateRequest request) {
    Employee employee = request.toEntity();
    Employee saved = employeeRepository.save(employee);
    return EmployeeResponse.from(saved);
}

// Service에서 사용 (방법 2: Builder 직접 사용 - 권장)
@Transactional
public EmployeeResponse createEmployee(EmployeeCreateRequest request) {
    Employee employee = Employee.builder()
        .name(request.getName())
        .phoneNumber(request.getPhoneNumber())
        .siteId(request.getSiteId())
        .status(EmployeeStatus.ACTIVE)
        .build();

    Employee saved = employeeRepository.save(employee);
    return EmployeeResponse.from(saved);
}
```

#### 3. List/Page 변환

```java
// List<Entity> → List<DTO>
public List<EmployeeResponse> getEmployeesBySite(Long siteId) {
    List<Employee> employees = employeeRepository.findAllBySiteIdAndNotDeleted(siteId);

    return employees.stream()
        .map(EmployeeResponse::from)
        .toList();
}

// Page<Entity> → Page<DTO>
public Page<EmployeeResponse> getEmployeePage(Pageable pageable) {
    Page<Employee> employees = employeeRepository.findAllNotDeleted(pageable);

    return employees.map(EmployeeResponse::from);
}
```

---

## 비즈니스 로직 구조화

### 복잡한 비즈니스 로직 구조화 패턴

```java
@Transactional
public PayrollResponse calculatePayroll(PayrollCalculateRequest request) {
    // 1. 로깅
    log.info("[PayrollService] calculatePayroll - employeeId={}, month={}",
        request.getEmployeeId(), request.getPaymentMonth());

    // 2. 데이터 조회
    Employee employee = findEmployeeOrThrow(request.getEmployeeId());
    List<Attendance> attendances = findAttendances(
        request.getEmployeeId(),
        request.getPaymentMonth()
    );

    // 3. 검증
    validatePayrollDuplication(request.getEmployeeId(), request.getPaymentMonth());
    validateAttendances(attendances);

    // 4. 계산
    long totalAmount = calculateTotalAmount(attendances, employee.getDailySalary());

    // 5. 엔티티 생성
    Payroll payroll = buildPayroll(
        employee,
        request.getPaymentMonth(),
        attendances.size(),
        totalAmount
    );

    // 6. 저장
    Payroll saved = payrollRepository.save(payroll);

    // 7. 후속 처리
    publishPayrollCalculatedEvent(saved);

    // 8. 성공 로깅
    log.info("[PayrollService] calculatePayroll - success payrollId={}", saved.getId());
    return PayrollResponse.from(saved);
}

// === Private 메서드로 로직 분리 ===

private Employee findEmployeeOrThrow(Long employeeId) {
    return employeeRepository.findByIdAndNotDeleted(employeeId)
        .orElseThrow(() -> new ResourceNotFoundException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND));
}

private List<Attendance> findAttendances(Long employeeId, YearMonth month) {
    return attendanceRepository.findByEmployeeIdAndMonth(employeeId, month);
}

private void validatePayrollDuplication(Long employeeId, YearMonth month) {
    if (payrollRepository.findByEmployeeIdAndPaymentMonthAndNotDeleted(employeeId, month).isPresent()) {
        throw new BusinessException(PayrollErrorCode.PAYROLL_ALREADY_EXISTS);
    }
}

private void validateAttendances(List<Attendance> attendances) {
    if (attendances.isEmpty()) {
        throw new BusinessException(PayrollErrorCode.NO_ATTENDANCE_RECORDS);
    }
}

private long calculateTotalAmount(List<Attendance> attendances, long dailySalary) {
    return attendances.size() * dailySalary;
}

private Payroll buildPayroll(Employee employee, YearMonth month, int workDays, long totalAmount) {
    return Payroll.builder()
        .employeeId(employee.getId())
        .siteId(employee.getSiteId())
        .paymentMonth(month)
        .workDays(workDays)
        .dailySalary(employee.getDailySalary())
        .totalAmount(totalAmount)
        .status(PayrollStatus.PENDING)
        .build();
}

private void publishPayrollCalculatedEvent(Payroll payroll) {
    // 이벤트 발행 로직
    log.debug("[PayrollService] publishPayrollCalculatedEvent - payrollId={}", payroll.getId());
}
```

---

## 페이징 처리

### Service 레이어 페이징 패턴

```java
/**
 * 사원 목록 조회 (페이징, 정렬)
 *
 * @param pageable 페이징 정보 (page, size, sort)
 * @return 사원 목록 (Page)
 */
public Page<EmployeeResponse> getEmployeePage(Pageable pageable) {
    log.info("[EmployeeService] getEmployeePage - page={}, size={}, sort={}",
        pageable.getPageNumber(),
        pageable.getPageSize(),
        pageable.getSort());

    Page<Employee> employees = employeeRepository.findAllNotDeleted(pageable);

    log.info("[EmployeeService] getEmployeePage - success totalElements={}, totalPages={}",
        employees.getTotalElements(),
        employees.getTotalPages());

    return employees.map(EmployeeResponse::from);
}

/**
 * 현장별 사원 목록 조회 (페이징)
 *
 * @param siteId 현장 ID
 * @param pageable 페이징 정보
 * @return 사원 목록 (Page)
 */
public Page<EmployeeResponse> getEmployeePageBySite(Long siteId, Pageable pageable) {
    log.info("[EmployeeService] getEmployeePageBySite - siteId={}, page={}",
        siteId, pageable.getPageNumber());

    Page<Employee> employees = employeeRepository.findBySiteIdAndNotDeleted(siteId, pageable);

    log.info("[EmployeeService] getEmployeePageBySite - success count={}",
        employees.getTotalElements());

    return employees.map(EmployeeResponse::from);
}
```

---

## 금지사항 및 권장사항

### ❌ 금지사항

#### 1. Entity 직접 반환

```java
// ❌ Entity 직접 반환 (금지)
public Employee getEmployee(Long id) {
    return employeeRepository.findById(id).orElseThrow();
}

// ✅ DTO 반환 (권장)
public EmployeeResponse getEmployee(Long id) {
    Employee employee = employeeRepository.findByIdAndNotDeleted(id)
        .orElseThrow(() -> new ResourceNotFoundException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND));
    return EmployeeResponse.from(employee);
}
```

#### 2. 필드 주입

```java
// ❌ 필드 주입 (금지)
@Service
public class EmployeeService {
    @Autowired
    private EmployeeRepository employeeRepository;
}

// ✅ 생성자 주입 (권장)
@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeRepository employeeRepository;
}
```

#### 3. 예외 무시

```java
// ❌ 예외 무시 (금지)
public Employee getEmployee(Long id) {
    try {
        return employeeRepository.findById(id).get();
    } catch (Exception e) {
        return null;  // 예외 숨김
    }
}

// ✅ 명확한 예외 처리 (권장)
public EmployeeResponse getEmployee(Long id) {
    Employee employee = employeeRepository.findByIdAndNotDeleted(id)
        .orElseThrow(() -> new ResourceNotFoundException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND));
    return EmployeeResponse.from(employee);
}
```

#### 4. 트랜잭션 누락

```java
// ❌ 트랜잭션 누락 (금지)
public EmployeeResponse createEmployee(EmployeeCreateRequest request) {
    Employee employee = Employee.builder().build();
    Employee saved = employeeRepository.save(employee);
    return EmployeeResponse.from(saved);
}

// ✅ @Transactional 명시 (권장)
@Transactional
public EmployeeResponse createEmployee(EmployeeCreateRequest request) {
    Employee employee = Employee.builder().build();
    Employee saved = employeeRepository.save(employee);
    return EmployeeResponse.from(saved);
}
```

#### 5. 로깅 누락

```java
// ❌ 로깅 없음 (금지)
public EmployeeResponse getEmployee(Long id) {
    Employee employee = employeeRepository.findByIdAndNotDeleted(id)
        .orElseThrow(() -> new ResourceNotFoundException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND));
    return EmployeeResponse.from(employee);
}

// ✅ 진입/성공 로그 (권장)
public EmployeeResponse getEmployee(Long id) {
    log.info("[EmployeeService] getEmployee - id={}", id);

    Employee employee = employeeRepository.findByIdAndNotDeleted(id)
        .orElseThrow(() -> new ResourceNotFoundException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND));

    log.info("[EmployeeService] getEmployee - success id={}", id);
    return EmployeeResponse.from(employee);
}
```

#### 6. 검증 로직 누락

```java
// ❌ 검증 없이 저장 (금지)
@Transactional
public EmployeeResponse createEmployee(EmployeeCreateRequest request) {
    Employee employee = Employee.builder()
        .phoneNumber(request.getPhoneNumber())
        .build();
    Employee saved = employeeRepository.save(employee);
    return EmployeeResponse.from(saved);
}

// ✅ 검증 후 저장 (권장)
@Transactional
public EmployeeResponse createEmployee(EmployeeCreateRequest request) {
    // 중복 체크
    if (employeeRepository.existsByPhoneNumberAndNotDeleted(request.getPhoneNumber())) {
        throw new BusinessException(EmployeeErrorCode.DUPLICATE_PHONE_NUMBER);
    }

    Employee employee = Employee.builder()
        .phoneNumber(request.getPhoneNumber())
        .build();
    Employee saved = employeeRepository.save(employee);
    return EmployeeResponse.from(saved);
}
```

### ✅ 권장사항

#### 1. 단일 책임 원칙

```java
// ✅ 하나의 Service는 하나의 도메인 담당
@Service
public class EmployeeService {
    // 사원 관련 로직만
}

@Service
public class SiteService {
    // 현장 관련 로직만
}
```

#### 2. Private 메서드로 로직 분리

```java
// ✅ 검증 로직 분리
@Transactional
public EmployeeResponse createEmployee(EmployeeCreateRequest request) {
    validateEmployeeUniqueness(request);
    validateEmployeeData(request);

    Employee employee = buildEmployee(request);
    Employee saved = employeeRepository.save(employee);

    return EmployeeResponse.from(saved);
}

private void validateEmployeeUniqueness(EmployeeCreateRequest request) {
    // 중복 체크
}

private void validateEmployeeData(EmployeeCreateRequest request) {
    // 데이터 검증
}

private Employee buildEmployee(EmployeeCreateRequest request) {
    // 엔티티 생성
}
```

#### 3. 명확한 메서드명

```java
// ✅ 명확한 메서드명
getEmployee(Long id)
createEmployee(EmployeeCreateRequest request)
updateEmployee(Long id, EmployeeUpdateRequest request)
deleteEmployee(Long id)
terminateEmployee(Long id, EmployeeTerminationRequest request)
searchEmployeesByName(String name, Pageable pageable)
```

#### 4. 일관된 로깅 패턴

```java
// ✅ 일관된 로깅
log.info("[ServiceName] methodName - param1={}, param2={}", param1, param2);
// ... 로직 ...
log.info("[ServiceName] methodName - success result={}", result);
```

#### 5. DTO 변환은 Service에서

```java
// ✅ Service에서 Entity → DTO 변환
public EmployeeResponse getEmployee(Long id) {
    Employee employee = employeeRepository.findByIdAndNotDeleted(id)
        .orElseThrow(() -> new ResourceNotFoundException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND));
    return EmployeeResponse.from(employee);  // Service에서 변환
}
```

---

## 추가 참고 자료

### 파일 위치
- **도메인별 Service**: `src/main/java/com/concrete/buildup/domain/{domain}/service/`
- **예외 처리**: `src/main/java/com/concrete/buildup/global/exception/`
- **공통 DTO**: `src/main/java/com/concrete/buildup/global/common/`

### 관련 문서
- [CLAUDE.md](../CLAUDE.md) - 프로젝트 전체 구조 및 컨벤션
- [Business Exception Guide](./business-exception-guide.md) - 예외 처리 가이드
- [Repository Guide](./repository-guide.md) - Repository 가이드
- [도메인 구조](../CLAUDE.md#프로젝트-구조)

---

## 버전 정보
- **최초 작성**: 2025-10-30
- **Build-Up Platform 버전**: Spring Boot 3.5.7, Java 21
- **작성자**: Claude Code