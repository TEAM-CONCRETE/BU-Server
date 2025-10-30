# Build-Up Platform Repository 가이드

## 📋 목차
1. [개요](#개요)
2. [기본 구조](#기본-구조)
3. [필수 규칙](#필수-규칙)
4. [소프트 삭제 처리](#소프트-삭제-처리)
5. [도메인별 Repository 예시](#도메인별-repository-예시)
6. [페이징 및 정렬](#페이징-및-정렬)
7. [검색 쿼리 패턴](#검색-쿼리-패턴)
8. [성능 최적화](#성능-최적화)
9. [금지사항 및 권장사항](#금지사항-및-권장사항)

---

## 개요

Build-Up Platform은 **Domain 중심 구조**를 채택하여 각 도메인별로 Repository를 관리합니다.

### Repository 위치
```
src/main/java/com/concrete/buildup/domain/
├── auth/
│   └── repository/
│       └── UserRepository.java
├── employee/
│   └── repository/
│       └── EmployeeRepository.java
├── site/
│   └── repository/
│       └── SiteRepository.java
├── contract/
│   └── repository/
│       └── ContractRepository.java
├── payroll/
│   └── repository/
│       └── PayrollRepository.java
├── attendance/
│   └── repository/
│       └── AttendanceRepository.java
├── workreport/
│   └── repository/
│       └── WorkReportRepository.java
└── safetydoc/
    └── repository/
        └── SafetyDocRepository.java
```

### 핵심 원칙
- 모든 Entity는 `BaseEntity`를 상속 (`id`, `createdAt`, `updatedAt`, `isDeleted` 포함)
- **소프트 삭제** 사용 (`isDeleted = true`로 삭제 표시)
- JPA 메서드 네이밍 컨벤션 준수
- 복잡한 쿼리는 `@Query` 사용
- 페이징과 정렬은 `Pageable` 사용

---

## 기본 구조

### 기본 템플릿

```java
package com.concrete.buildup.domain.{domain}.repository;

import com.concrete.buildup.domain.{domain}.entity.{Entity};
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * {Entity} Repository
 *
 * 주요 기능:
 * - {기능 1}
 * - {기능 2}
 * - {기능 3}
 */
@Repository
public interface {Entity}Repository extends JpaRepository<{Entity}, Long> {

    /**
     * ID로 {Entity} 조회 (삭제되지 않은 것만)
     * @param id {Entity} ID
     * @return {Entity} 정보 (Optional)
     */
    @Query("SELECT e FROM {Entity} e WHERE e.id = :id AND e.isDeleted = false")
    Optional<{Entity}> findByIdAndNotDeleted(@Param("id") Long id);

    /**
     * 모든 {Entity} 조회 (삭제되지 않은 것만)
     * @return {Entity} 목록
     */
    @Query("SELECT e FROM {Entity} e WHERE e.isDeleted = false ORDER BY e.createdAt DESC")
    List<{Entity}> findAllNotDeleted();

    // 추가 메서드...
}
```

---

## 필수 규칙

### 1. 인터페이스 선언

```java
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    // 메서드 정의
}
```

**필수 요소:**
- `@Repository` 어노테이션
- `JpaRepository<Entity, Long>` 상속 (ID 타입은 항상 `Long`)
- 패키지: `domain.{domain-name}.repository`

### 2. 메서드 명명 규칙

| 작업 | 메서드 명명 | 예시 |
|------|-----------|------|
| 단건 조회 | `findByXxx` | `findByPhoneNumber(String phoneNumber)` |
| 목록 조회 | `findAllByXxx` | `findAllBySiteId(Long siteId)` |
| 조건 조합 | `findByXxxAndYyy` | `findByNameAndStatus(String name, Status status)` |
| 존재 여부 | `existsByXxx` | `existsByPhoneNumber(String phoneNumber)` |
| 개수 세기 | `countByXxx` | `countBySiteId(Long siteId)` |
| 정렬 | `findByXxxOrderByYyyDesc` | `findBySiteIdOrderByCreatedAtDesc(Long siteId)` |

### 3. 반환 타입

```java
// ✅ 단건 조회 - Optional 사용 (권장)
Optional<Employee> findByPhoneNumber(String phoneNumber);
Optional<Employee> findById(Long id);  // JpaRepository 기본 제공

// ✅ 목록 조회 - List 사용
List<Employee> findAllBySiteId(Long siteId);
List<Employee> findAll();  // JpaRepository 기본 제공

// ✅ 페이징 조회 - Page 사용
Page<Employee> findAllBySiteId(Long siteId, Pageable pageable);

// ✅ 존재 여부 - boolean
boolean existsByPhoneNumber(String phoneNumber);

// ✅ 개수 - long (Long 아님)
long countBySiteId(Long siteId);

// ❌ null 반환 가능 (권장하지 않음)
Employee findByPhoneNumber(String phoneNumber);  // null 가능성
```

### 4. 주석 작성

```java
/**
 * 전화번호로 사원 조회 (삭제되지 않은 것만)
 *
 * @param phoneNumber 전화번호 (하이픈 포함)
 * @return 사원 정보 (Optional)
 */
@Query("SELECT e FROM Employee e WHERE e.phoneNumber = :phoneNumber AND e.isDeleted = false")
Optional<Employee> findByPhoneNumberAndNotDeleted(@Param("phoneNumber") String phoneNumber);
```

**주석 포함 내용:**
- 메서드의 목적과 기능
- 소프트 삭제 필터링 여부 명시
- 파라미터 설명 (`@param`)
- 반환값 설명 (`@return`)

---

## 소프트 삭제 처리

Build-Up Platform은 **소프트 삭제(Soft Delete)**를 사용합니다. 모든 Entity는 `isDeleted` 필드를 가지며, 삭제 시 `isDeleted = true`로 설정됩니다.

### 기본 원칙

1. **조회 시 항상 `isDeleted = false` 조건 추가**
2. **실제 DELETE는 사용하지 않음**
3. **삭제된 데이터 조회는 명시적으로만**

### 소프트 삭제 쿼리 패턴

#### 1. 단건 조회 (삭제되지 않은 것만)

```java
/**
 * ID로 사원 조회 (삭제되지 않은 것만)
 */
@Query("SELECT e FROM Employee e WHERE e.id = :id AND e.isDeleted = false")
Optional<Employee> findByIdAndNotDeleted(@Param("id") Long id);

/**
 * 전화번호로 사원 조회 (삭제되지 않은 것만)
 */
@Query("SELECT e FROM Employee e WHERE e.phoneNumber = :phoneNumber AND e.isDeleted = false")
Optional<Employee> findByPhoneNumberAndNotDeleted(@Param("phoneNumber") String phoneNumber);
```

#### 2. 목록 조회 (삭제되지 않은 것만)

```java
/**
 * 모든 사원 조회 (삭제되지 않은 것만)
 */
@Query("SELECT e FROM Employee e WHERE e.isDeleted = false ORDER BY e.createdAt DESC")
List<Employee> findAllNotDeleted();

/**
 * 현장별 사원 조회 (삭제되지 않은 것만)
 */
@Query("SELECT e FROM Employee e WHERE e.siteId = :siteId AND e.isDeleted = false")
List<Employee> findAllBySiteIdAndNotDeleted(@Param("siteId") Long siteId);
```

#### 3. 존재 여부 (삭제되지 않은 것만)

```java
/**
 * 전화번호 중복 확인 (삭제되지 않은 것만)
 */
@Query("SELECT COUNT(e) > 0 FROM Employee e WHERE e.phoneNumber = :phoneNumber AND e.isDeleted = false")
boolean existsByPhoneNumberAndNotDeleted(@Param("phoneNumber") String phoneNumber);
```

#### 4. 개수 세기 (삭제되지 않은 것만)

```java
/**
 * 현장별 사원 수 (삭제되지 않은 것만)
 */
@Query("SELECT COUNT(e) FROM Employee e WHERE e.siteId = :siteId AND e.isDeleted = false")
long countBySiteIdAndNotDeleted(@Param("siteId") Long siteId);
```

#### 5. 삭제된 데이터 조회 (명시적)

```java
/**
 * 삭제된 사원 목록 조회 (관리자용)
 */
@Query("SELECT e FROM Employee e WHERE e.isDeleted = true ORDER BY e.updatedAt DESC")
List<Employee> findAllDeleted();

/**
 * 특정 기간 내 삭제된 사원 조회
 */
@Query("SELECT e FROM Employee e WHERE e.isDeleted = true AND e.updatedAt >= :startDate AND e.updatedAt <= :endDate")
List<Employee> findDeletedBetween(
    @Param("startDate") LocalDateTime startDate,
    @Param("endDate") LocalDateTime endDate
);
```

---

## 도메인별 Repository 예시

### 1. Auth Domain - UserRepository

```java
package com.concrete.buildup.domain.auth.repository;

import com.concrete.buildup.domain.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * User Repository
 *
 * 주요 기능:
 * - 사용자 인증 및 조회
 * - 이메일/사용자명 중복 체크
 * - 역할별 사용자 조회
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 사용자명으로 사용자 조회 (삭제되지 않은 것만)
     *
     * @param username 사용자명
     * @return 사용자 정보 (Optional)
     */
    @Query("SELECT u FROM User u WHERE u.username = :username AND u.isDeleted = false")
    Optional<User> findByUsernameAndNotDeleted(@Param("username") String username);

    /**
     * 이메일로 사용자 조회 (삭제되지 않은 것만)
     *
     * @param email 이메일
     * @return 사용자 정보 (Optional)
     */
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.isDeleted = false")
    Optional<User> findByEmailAndNotDeleted(@Param("email") String email);

    /**
     * 사용자명 중복 확인 (삭제되지 않은 것만)
     *
     * @param username 사용자명
     * @return 중복 여부
     */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.username = :username AND u.isDeleted = false")
    boolean existsByUsernameAndNotDeleted(@Param("username") String username);

    /**
     * 이메일 중복 확인 (삭제되지 않은 것만)
     *
     * @param email 이메일
     * @return 중복 여부
     */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.email = :email AND u.isDeleted = false")
    boolean existsByEmailAndNotDeleted(@Param("email") String email);
}
```

### 2. Employee Domain - EmployeeRepository

```java
package com.concrete.buildup.domain.employee.repository;

import com.concrete.buildup.domain.employee.entity.Employee;
import com.concrete.buildup.domain.employee.entity.EmployeeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Employee Repository
 *
 * 주요 기능:
 * - 사원 조회 및 검색
 * - 현장별 사원 관리
 * - 사원 상태별 필터링
 */
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    /**
     * ID로 사원 조회 (삭제되지 않은 것만)
     *
     * @param id 사원 ID
     * @return 사원 정보 (Optional)
     */
    @Query("SELECT e FROM Employee e WHERE e.id = :id AND e.isDeleted = false")
    Optional<Employee> findByIdAndNotDeleted(@Param("id") Long id);

    /**
     * 모든 사원 조회 (삭제되지 않은 것만, 페이징)
     *
     * @param pageable 페이징 정보
     * @return 사원 목록 (Page)
     */
    @Query("SELECT e FROM Employee e WHERE e.isDeleted = false")
    Page<Employee> findAllNotDeleted(Pageable pageable);

    /**
     * 전화번호로 사원 조회 (삭제되지 않은 것만)
     *
     * @param phoneNumber 전화번호
     * @return 사원 정보 (Optional)
     */
    @Query("SELECT e FROM Employee e WHERE e.phoneNumber = :phoneNumber AND e.isDeleted = false")
    Optional<Employee> findByPhoneNumberAndNotDeleted(@Param("phoneNumber") String phoneNumber);

    /**
     * 전화번호 중복 확인 (삭제되지 않은 것만)
     *
     * @param phoneNumber 전화번호
     * @return 중복 여부
     */
    @Query("SELECT COUNT(e) > 0 FROM Employee e WHERE e.phoneNumber = :phoneNumber AND e.isDeleted = false")
    boolean existsByPhoneNumberAndNotDeleted(@Param("phoneNumber") String phoneNumber);

    /**
     * 현장별 사원 조회 (삭제되지 않은 것만)
     *
     * @param siteId 현장 ID
     * @return 사원 목록
     */
    @Query("SELECT e FROM Employee e WHERE e.siteId = :siteId AND e.isDeleted = false ORDER BY e.name ASC")
    List<Employee> findAllBySiteIdAndNotDeleted(@Param("siteId") Long siteId);

    /**
     * 현장별 활성 사원 수 (삭제되지 않은 것만)
     *
     * @param siteId 현장 ID
     * @param status 사원 상태
     * @return 사원 수
     */
    @Query("SELECT COUNT(e) FROM Employee e WHERE e.siteId = :siteId AND e.status = :status AND e.isDeleted = false")
    long countBySiteIdAndStatusAndNotDeleted(
        @Param("siteId") Long siteId,
        @Param("status") EmployeeStatus status
    );

    /**
     * 이름으로 사원 검색 (삭제되지 않은 것만, 페이징)
     *
     * @param name 사원 이름 (부분 검색)
     * @param pageable 페이징 정보
     * @return 사원 목록 (Page)
     */
    @Query("SELECT e FROM Employee e WHERE e.name LIKE %:name% AND e.isDeleted = false")
    Page<Employee> searchByNameAndNotDeleted(@Param("name") String name, Pageable pageable);

    /**
     * 현장 및 상태별 사원 조회 (삭제되지 않은 것만)
     *
     * @param siteId 현장 ID
     * @param status 사원 상태
     * @return 사원 목록
     */
    @Query("SELECT e FROM Employee e WHERE e.siteId = :siteId AND e.status = :status AND e.isDeleted = false ORDER BY e.createdAt DESC")
    List<Employee> findBySiteIdAndStatusAndNotDeleted(
        @Param("siteId") Long siteId,
        @Param("status") EmployeeStatus status
    );
}
```

### 3. Site Domain - SiteRepository

```java
package com.concrete.buildup.domain.site.repository;

import com.concrete.buildup.domain.site.entity.Site;
import com.concrete.buildup.domain.site.entity.SiteStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Site Repository
 *
 * 주요 기능:
 * - 현장 조회 및 검색
 * - 상태별 현장 필터링
 * - 관리자별 현장 조회
 */
@Repository
public interface SiteRepository extends JpaRepository<Site, Long> {

    /**
     * ID로 현장 조회 (삭제되지 않은 것만)
     *
     * @param id 현장 ID
     * @return 현장 정보 (Optional)
     */
    @Query("SELECT s FROM Site s WHERE s.id = :id AND s.isDeleted = false")
    Optional<Site> findByIdAndNotDeleted(@Param("id") Long id);

    /**
     * 모든 현장 조회 (삭제되지 않은 것만, 페이징)
     *
     * @param pageable 페이징 정보
     * @return 현장 목록 (Page)
     */
    @Query("SELECT s FROM Site s WHERE s.isDeleted = false ORDER BY s.createdAt DESC")
    Page<Site> findAllNotDeleted(Pageable pageable);

    /**
     * 현장 코드로 조회 (삭제되지 않은 것만)
     *
     * @param code 현장 코드
     * @return 현장 정보 (Optional)
     */
    @Query("SELECT s FROM Site s WHERE s.code = :code AND s.isDeleted = false")
    Optional<Site> findByCodeAndNotDeleted(@Param("code") String code);

    /**
     * 현장 코드 중복 확인 (삭제되지 않은 것만)
     *
     * @param code 현장 코드
     * @return 중복 여부
     */
    @Query("SELECT COUNT(s) > 0 FROM Site s WHERE s.code = :code AND s.isDeleted = false")
    boolean existsByCodeAndNotDeleted(@Param("code") String code);

    /**
     * 상태별 현장 조회 (삭제되지 않은 것만)
     *
     * @param status 현장 상태
     * @return 현장 목록
     */
    @Query("SELECT s FROM Site s WHERE s.status = :status AND s.isDeleted = false ORDER BY s.startDate DESC")
    List<Site> findByStatusAndNotDeleted(@Param("status") SiteStatus status);

    /**
     * 관리자별 현장 조회 (삭제되지 않은 것만)
     *
     * @param managerId 관리자 ID
     * @return 현장 목록
     */
    @Query("SELECT s FROM Site s WHERE s.managerId = :managerId AND s.isDeleted = false ORDER BY s.createdAt DESC")
    List<Site> findByManagerIdAndNotDeleted(@Param("managerId") Long managerId);

    /**
     * 현장명 검색 (삭제되지 않은 것만, 페이징)
     *
     * @param name 현장명 (부분 검색)
     * @param pageable 페이징 정보
     * @return 현장 목록 (Page)
     */
    @Query("SELECT s FROM Site s WHERE s.name LIKE %:name% AND s.isDeleted = false")
    Page<Site> searchByNameAndNotDeleted(@Param("name") String name, Pageable pageable);

    /**
     * 기간별 활성 현장 조회 (삭제되지 않은 것만)
     *
     * @param startDate 시작일
     * @param endDate 종료일
     * @return 현장 목록
     */
    @Query("""
        SELECT s FROM Site s
        WHERE s.startDate <= :endDate
        AND s.endDate >= :startDate
        AND s.status = 'ACTIVE'
        AND s.isDeleted = false
        ORDER BY s.startDate ASC
    """)
    List<Site> findActiveSitesBetween(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}
```

### 4. Contract Domain - ContractRepository

```java
package com.concrete.buildup.domain.contract.repository;

import com.concrete.buildup.domain.contract.entity.Contract;
import com.concrete.buildup.domain.contract.entity.ContractStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Contract Repository
 *
 * 주요 기능:
 * - 근로계약서 조회 및 관리
 * - 사원별 계약 이력 조회
 * - 계약 상태별 필터링
 */
@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {

    /**
     * ID로 계약 조회 (삭제되지 않은 것만)
     *
     * @param id 계약 ID
     * @return 계약 정보 (Optional)
     */
    @Query("SELECT c FROM Contract c WHERE c.id = :id AND c.isDeleted = false")
    Optional<Contract> findByIdAndNotDeleted(@Param("id") Long id);

    /**
     * 사원별 계약 이력 조회 (삭제되지 않은 것만)
     *
     * @param employeeId 사원 ID
     * @return 계약 목록
     */
    @Query("SELECT c FROM Contract c WHERE c.employeeId = :employeeId AND c.isDeleted = false ORDER BY c.startDate DESC")
    List<Contract> findByEmployeeIdAndNotDeleted(@Param("employeeId") Long employeeId);

    /**
     * 사원별 유효한 계약 조회 (삭제되지 않은 것만)
     *
     * @param employeeId 사원 ID
     * @param status 계약 상태
     * @return 계약 정보 (Optional)
     */
    @Query("SELECT c FROM Contract c WHERE c.employeeId = :employeeId AND c.status = :status AND c.isDeleted = false ORDER BY c.startDate DESC LIMIT 1")
    Optional<Contract> findActiveByEmployeeIdAndNotDeleted(
        @Param("employeeId") Long employeeId,
        @Param("status") ContractStatus status
    );

    /**
     * 현장별 계약 조회 (삭제되지 않은 것만, 페이징)
     *
     * @param siteId 현장 ID
     * @param pageable 페이징 정보
     * @return 계약 목록 (Page)
     */
    @Query("SELECT c FROM Contract c WHERE c.siteId = :siteId AND c.isDeleted = false")
    Page<Contract> findBySiteIdAndNotDeleted(@Param("siteId") Long siteId, Pageable pageable);

    /**
     * 상태별 계약 수 (삭제되지 않은 것만)
     *
     * @param status 계약 상태
     * @return 계약 수
     */
    @Query("SELECT COUNT(c) FROM Contract c WHERE c.status = :status AND c.isDeleted = false")
    long countByStatusAndNotDeleted(@Param("status") ContractStatus status);
}
```

### 5. Payroll Domain - PayrollRepository

```java
package com.concrete.buildup.domain.payroll.repository;

import com.concrete.buildup.domain.payroll.entity.Payroll;
import com.concrete.buildup.domain.payroll.entity.PayrollStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * Payroll Repository
 *
 * 주요 기능:
 * - 급여 조회 및 관리
 * - 사원별 급여 이력 조회
 * - 월별 급여 조회
 */
@Repository
public interface PayrollRepository extends JpaRepository<Payroll, Long> {

    /**
     * ID로 급여 조회 (삭제되지 않은 것만)
     *
     * @param id 급여 ID
     * @return 급여 정보 (Optional)
     */
    @Query("SELECT p FROM Payroll p WHERE p.id = :id AND p.isDeleted = false")
    Optional<Payroll> findByIdAndNotDeleted(@Param("id") Long id);

    /**
     * 사원별 급여 이력 조회 (삭제되지 않은 것만)
     *
     * @param employeeId 사원 ID
     * @return 급여 목록
     */
    @Query("SELECT p FROM Payroll p WHERE p.employeeId = :employeeId AND p.isDeleted = false ORDER BY p.paymentMonth DESC")
    List<Payroll> findByEmployeeIdAndNotDeleted(@Param("employeeId") Long employeeId);

    /**
     * 사원 및 월별 급여 조회 (삭제되지 않은 것만)
     *
     * @param employeeId 사원 ID
     * @param paymentMonth 지급 월 (YYYY-MM)
     * @return 급여 정보 (Optional)
     */
    @Query("SELECT p FROM Payroll p WHERE p.employeeId = :employeeId AND p.paymentMonth = :paymentMonth AND p.isDeleted = false")
    Optional<Payroll> findByEmployeeIdAndPaymentMonthAndNotDeleted(
        @Param("employeeId") Long employeeId,
        @Param("paymentMonth") YearMonth paymentMonth
    );

    /**
     * 현장 및 월별 급여 조회 (삭제되지 않은 것만, 페이징)
     *
     * @param siteId 현장 ID
     * @param paymentMonth 지급 월
     * @param pageable 페이징 정보
     * @return 급여 목록 (Page)
     */
    @Query("SELECT p FROM Payroll p WHERE p.siteId = :siteId AND p.paymentMonth = :paymentMonth AND p.isDeleted = false")
    Page<Payroll> findBySiteIdAndPaymentMonthAndNotDeleted(
        @Param("siteId") Long siteId,
        @Param("paymentMonth") YearMonth paymentMonth,
        Pageable pageable
    );

    /**
     * 월별 미지급 급여 조회 (삭제되지 않은 것만)
     *
     * @param paymentMonth 지급 월
     * @param status 급여 상태
     * @return 급여 목록
     */
    @Query("SELECT p FROM Payroll p WHERE p.paymentMonth = :paymentMonth AND p.status = :status AND p.isDeleted = false")
    List<Payroll> findByPaymentMonthAndStatusAndNotDeleted(
        @Param("paymentMonth") YearMonth paymentMonth,
        @Param("status") PayrollStatus status
    );

    /**
     * 월별 총 급여액 계산 (삭제되지 않은 것만)
     *
     * @param paymentMonth 지급 월
     * @return 총 급여액
     */
    @Query("SELECT SUM(p.totalAmount) FROM Payroll p WHERE p.paymentMonth = :paymentMonth AND p.isDeleted = false")
    Long sumTotalAmountByPaymentMonthAndNotDeleted(@Param("paymentMonth") YearMonth paymentMonth);
}
```

### 6. Attendance Domain - AttendanceRepository

```java
package com.concrete.buildup.domain.attendance.repository;

import com.concrete.buildup.domain.attendance.entity.Attendance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Attendance Repository
 *
 * 주요 기능:
 * - 근태 기록 조회
 * - 사원별 근태 이력 조회
 * - 기간별 근태 통계
 */
@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    /**
     * ID로 근태 조회 (삭제되지 않은 것만)
     *
     * @param id 근태 ID
     * @return 근태 정보 (Optional)
     */
    @Query("SELECT a FROM Attendance a WHERE a.id = :id AND a.isDeleted = false")
    Optional<Attendance> findByIdAndNotDeleted(@Param("id") Long id);

    /**
     * 사원 및 날짜별 근태 조회 (삭제되지 않은 것만)
     *
     * @param employeeId 사원 ID
     * @param workDate 근무일
     * @return 근태 정보 (Optional)
     */
    @Query("SELECT a FROM Attendance a WHERE a.employeeId = :employeeId AND a.workDate = :workDate AND a.isDeleted = false")
    Optional<Attendance> findByEmployeeIdAndWorkDateAndNotDeleted(
        @Param("employeeId") Long employeeId,
        @Param("workDate") LocalDate workDate
    );

    /**
     * 사원별 기간 내 근태 조회 (삭제되지 않은 것만)
     *
     * @param employeeId 사원 ID
     * @param startDate 시작일
     * @param endDate 종료일
     * @return 근태 목록
     */
    @Query("SELECT a FROM Attendance a WHERE a.employeeId = :employeeId AND a.workDate BETWEEN :startDate AND :endDate AND a.isDeleted = false ORDER BY a.workDate DESC")
    List<Attendance> findByEmployeeIdAndWorkDateBetweenAndNotDeleted(
        @Param("employeeId") Long employeeId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * 현장 및 날짜별 근태 조회 (삭제되지 않은 것만)
     *
     * @param siteId 현장 ID
     * @param workDate 근무일
     * @return 근태 목록
     */
    @Query("SELECT a FROM Attendance a WHERE a.siteId = :siteId AND a.workDate = :workDate AND a.isDeleted = false ORDER BY a.checkInTime ASC")
    List<Attendance> findBySiteIdAndWorkDateAndNotDeleted(
        @Param("siteId") Long siteId,
        @Param("workDate") LocalDate workDate
    );

    /**
     * 사원별 근무일수 계산 (삭제되지 않은 것만)
     *
     * @param employeeId 사원 ID
     * @param startDate 시작일
     * @param endDate 종료일
     * @return 근무일수
     */
    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.employeeId = :employeeId AND a.workDate BETWEEN :startDate AND :endDate AND a.isDeleted = false")
    long countWorkDaysByEmployeeIdAndNotDeleted(
        @Param("employeeId") Long employeeId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}
```

### 7. WorkReport Domain - WorkReportRepository

```java
package com.concrete.buildup.domain.workreport.repository;

import com.concrete.buildup.domain.workreport.entity.WorkReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * WorkReport Repository
 *
 * 주요 기능:
 * - 작업일보 조회 및 관리
 * - 현장별 작업일보 조회
 * - 기간별 작업일보 조회
 */
@Repository
public interface WorkReportRepository extends JpaRepository<WorkReport, Long> {

    /**
     * ID로 작업일보 조회 (삭제되지 않은 것만)
     *
     * @param id 작업일보 ID
     * @return 작업일보 정보 (Optional)
     */
    @Query("SELECT w FROM WorkReport w WHERE w.id = :id AND w.isDeleted = false")
    Optional<WorkReport> findByIdAndNotDeleted(@Param("id") Long id);

    /**
     * 현장 및 날짜별 작업일보 조회 (삭제되지 않은 것만)
     *
     * @param siteId 현장 ID
     * @param reportDate 작업일
     * @return 작업일보 정보 (Optional)
     */
    @Query("SELECT w FROM WorkReport w WHERE w.siteId = :siteId AND w.reportDate = :reportDate AND w.isDeleted = false")
    Optional<WorkReport> findBySiteIdAndReportDateAndNotDeleted(
        @Param("siteId") Long siteId,
        @Param("reportDate") LocalDate reportDate
    );

    /**
     * 현장별 기간 내 작업일보 조회 (삭제되지 않은 것만)
     *
     * @param siteId 현장 ID
     * @param startDate 시작일
     * @param endDate 종료일
     * @return 작업일보 목록
     */
    @Query("SELECT w FROM WorkReport w WHERE w.siteId = :siteId AND w.reportDate BETWEEN :startDate AND :endDate AND w.isDeleted = false ORDER BY w.reportDate DESC")
    List<WorkReport> findBySiteIdAndReportDateBetweenAndNotDeleted(
        @Param("siteId") Long siteId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * 현장별 작업일보 조회 (삭제되지 않은 것만, 페이징)
     *
     * @param siteId 현장 ID
     * @param pageable 페이징 정보
     * @return 작업일보 목록 (Page)
     */
    @Query("SELECT w FROM WorkReport w WHERE w.siteId = :siteId AND w.isDeleted = false ORDER BY w.reportDate DESC")
    Page<WorkReport> findBySiteIdAndNotDeleted(@Param("siteId") Long siteId, Pageable pageable);
}
```

### 8. SafetyDoc Domain - SafetyDocRepository

```java
package com.concrete.buildup.domain.safetydoc.repository;

import com.concrete.buildup.domain.safetydoc.entity.SafetyDoc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * SafetyDoc Repository
 *
 * 주요 기능:
 * - 안전교육일지 조회 및 관리
 * - 현장별 안전교육 이력 조회
 * - 기간별 안전교육 통계
 */
@Repository
public interface SafetyDocRepository extends JpaRepository<SafetyDoc, Long> {

    /**
     * ID로 안전교육일지 조회 (삭제되지 않은 것만)
     *
     * @param id 안전교육일지 ID
     * @return 안전교육일지 정보 (Optional)
     */
    @Query("SELECT s FROM SafetyDoc s WHERE s.id = :id AND s.isDeleted = false")
    Optional<SafetyDoc> findByIdAndNotDeleted(@Param("id") Long id);

    /**
     * 현장별 안전교육일지 조회 (삭제되지 않은 것만, 페이징)
     *
     * @param siteId 현장 ID
     * @param pageable 페이징 정보
     * @return 안전교육일지 목록 (Page)
     */
    @Query("SELECT s FROM SafetyDoc s WHERE s.siteId = :siteId AND s.isDeleted = false ORDER BY s.educationDate DESC")
    Page<SafetyDoc> findBySiteIdAndNotDeleted(@Param("siteId") Long siteId, Pageable pageable);

    /**
     * 현장 및 날짜별 안전교육일지 조회 (삭제되지 않은 것만)
     *
     * @param siteId 현장 ID
     * @param educationDate 교육일
     * @return 안전교육일지 정보 (Optional)
     */
    @Query("SELECT s FROM SafetyDoc s WHERE s.siteId = :siteId AND s.educationDate = :educationDate AND s.isDeleted = false")
    Optional<SafetyDoc> findBySiteIdAndEducationDateAndNotDeleted(
        @Param("siteId") Long siteId,
        @Param("educationDate") LocalDate educationDate
    );

    /**
     * 현장별 기간 내 안전교육일지 조회 (삭제되지 않은 것만)
     *
     * @param siteId 현장 ID
     * @param startDate 시작일
     * @param endDate 종료일
     * @return 안전교육일지 목록
     */
    @Query("SELECT s FROM SafetyDoc s WHERE s.siteId = :siteId AND s.educationDate BETWEEN :startDate AND :endDate AND s.isDeleted = false ORDER BY s.educationDate DESC")
    List<SafetyDoc> findBySiteIdAndEducationDateBetweenAndNotDeleted(
        @Param("siteId") Long siteId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * 현장별 안전교육 실시 횟수 (삭제되지 않은 것만)
     *
     * @param siteId 현장 ID
     * @param startDate 시작일
     * @param endDate 종료일
     * @return 교육 횟수
     */
    @Query("SELECT COUNT(s) FROM SafetyDoc s WHERE s.siteId = :siteId AND s.educationDate BETWEEN :startDate AND :endDate AND s.isDeleted = false")
    long countBySiteIdAndEducationDateBetweenAndNotDeleted(
        @Param("siteId") Long siteId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}
```

---

## 페이징 및 정렬

### 1. Pageable 사용

```java
// Controller
@GetMapping
public ResponseEntity<ApiResponse<Page<EmployeeResponse>>> getEmployees(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "createdAt,desc") String[] sort) {

    Pageable pageable = PageRequest.of(page, size, Sort.by(parseSort(sort)));
    Page<EmployeeResponse> employees = employeeService.getEmployees(pageable);
    return ResponseEntity.ok(ApiResponse.success(employees));
}

// Service
@Transactional(readOnly = true)
public Page<EmployeeResponse> getEmployees(Pageable pageable) {
    Page<Employee> employees = employeeRepository.findAllNotDeleted(pageable);
    return employees.map(EmployeeResponse::from);
}

// Repository
@Query("SELECT e FROM Employee e WHERE e.isDeleted = false")
Page<Employee> findAllNotDeleted(Pageable pageable);
```

### 2. 동적 정렬

```java
// 여러 정렬 조건 조합
Sort sort = Sort.by(
    Sort.Order.desc("status"),
    Sort.Order.asc("name")
);
Pageable pageable = PageRequest.of(0, 20, sort);

// @Query에 정렬 자동 적용
@Query("SELECT e FROM Employee e WHERE e.siteId = :siteId AND e.isDeleted = false")
Page<Employee> findBySiteIdAndNotDeleted(@Param("siteId") Long siteId, Pageable pageable);
```

### 3. 커스텀 정렬

```java
// @Query에 ORDER BY 명시 (Pageable 정렬 무시됨)
@Query("SELECT e FROM Employee e WHERE e.isDeleted = false ORDER BY e.name ASC")
List<Employee> findAllNotDeletedOrderByName();

// Pageable 정렬과 함께 사용하려면 ORDER BY 제거
@Query("SELECT e FROM Employee e WHERE e.isDeleted = false")
Page<Employee> findAllNotDeleted(Pageable pageable);
```

---

## 검색 쿼리 패턴

### 1. LIKE 검색 (부분 일치)

```java
/**
 * 이름으로 사원 검색 (부분 일치)
 */
@Query("SELECT e FROM Employee e WHERE e.name LIKE %:keyword% AND e.isDeleted = false")
List<Employee> searchByName(@Param("keyword") String keyword);

/**
 * 여러 필드 검색 (OR 조건)
 */
@Query("""
    SELECT e FROM Employee e
    WHERE (e.name LIKE %:keyword%
        OR e.phoneNumber LIKE %:keyword%
        OR e.email LIKE %:keyword%)
    AND e.isDeleted = false
""")
List<Employee> search(@Param("keyword") String keyword);
```

### 2. IN 절 (여러 값 중 하나)

```java
/**
 * 여러 상태 중 하나에 해당하는 사원 조회
 */
@Query("SELECT e FROM Employee e WHERE e.status IN :statuses AND e.isDeleted = false")
List<Employee> findByStatusIn(@Param("statuses") List<EmployeeStatus> statuses);

/**
 * 여러 현장의 사원 조회
 */
@Query("SELECT e FROM Employee e WHERE e.siteId IN :siteIds AND e.isDeleted = false")
List<Employee> findBySiteIdIn(@Param("siteIds") List<Long> siteIds);
```

### 3. BETWEEN 절 (범위 검색)

```java
/**
 * 기간별 근태 조회
 */
@Query("SELECT a FROM Attendance a WHERE a.workDate BETWEEN :startDate AND :endDate AND a.isDeleted = false")
List<Attendance> findByWorkDateBetween(
    @Param("startDate") LocalDate startDate,
    @Param("endDate") LocalDate endDate
);

/**
 * 생성일 범위 조회
 */
@Query("SELECT e FROM Employee e WHERE e.createdAt BETWEEN :startDateTime AND :endDateTime AND e.isDeleted = false")
List<Employee> findByCreatedAtBetween(
    @Param("startDateTime") LocalDateTime startDateTime,
    @Param("endDateTime") LocalDateTime endDateTime
);
```

### 4. NULL 체크

```java
/**
 * 퇴사일이 없는 사원 조회 (재직 중)
 */
@Query("SELECT e FROM Employee e WHERE e.terminationDate IS NULL AND e.isDeleted = false")
List<Employee> findActiveEmployees();

/**
 * 퇴사일이 있는 사원 조회 (퇴사자)
 */
@Query("SELECT e FROM Employee e WHERE e.terminationDate IS NOT NULL AND e.isDeleted = false")
List<Employee> findTerminatedEmployees();
```

---

## 성능 최적화

### 1. JOIN FETCH (N+1 문제 해결)

```java
/**
 * Employee와 Site를 한 번에 조회 (N+1 문제 방지)
 */
@Query("SELECT e FROM Employee e JOIN FETCH e.site WHERE e.id = :id AND e.isDeleted = false")
Optional<Employee> findByIdWithSite(@Param("id") Long id);

/**
 * 여러 연관 엔티티를 한 번에 조회
 */
@Query("""
    SELECT c FROM Contract c
    JOIN FETCH c.employee e
    JOIN FETCH c.site s
    WHERE c.id = :id
    AND c.isDeleted = false
""")
Optional<Contract> findByIdWithEmployeeAndSite(@Param("id") Long id);
```

### 2. DTO Projection (필요한 컬럼만 조회)

```java
/**
 * DTO로 직접 매핑 (성능 최적화)
 */
@Query("""
    SELECT new com.concrete.buildup.domain.employee.dto.EmployeeSummaryDto(
        e.id, e.name, e.phoneNumber, e.status
    )
    FROM Employee e
    WHERE e.siteId = :siteId
    AND e.isDeleted = false
""")
List<EmployeeSummaryDto> findSummaryBySiteId(@Param("siteId") Long siteId);
```

### 3. 일괄 삭제/수정 (@Modifying)

```java
/**
 * 일괄 소프트 삭제
 */
@Modifying
@Query("UPDATE Employee e SET e.isDeleted = true WHERE e.siteId = :siteId")
int softDeleteBySiteId(@Param("siteId") Long siteId);

/**
 * 일괄 상태 변경
 */
@Modifying
@Query("UPDATE Employee e SET e.status = :newStatus WHERE e.id IN :ids")
int updateStatusByIds(
    @Param("ids") List<Long> ids,
    @Param("newStatus") EmployeeStatus newStatus
);
```

**주의사항:**
- `@Modifying` 사용 시 영속성 컨텍스트와 동기화되지 않음
- Service 레이어에서 `@Transactional` 필수
- 수정 후 `entityManager.clear()` 권장

---

## 금지사항 및 권장사항

### ❌ 금지사항

#### 1. 소프트 삭제 조건 누락

```java
// ❌ isDeleted 조건 누락 (삭제된 데이터도 조회됨)
@Query("SELECT e FROM Employee e WHERE e.siteId = :siteId")
List<Employee> findBySiteId(@Param("siteId") Long siteId);

// ✅ isDeleted 조건 포함
@Query("SELECT e FROM Employee e WHERE e.siteId = :siteId AND e.isDeleted = false")
List<Employee> findBySiteIdAndNotDeleted(@Param("siteId") Long siteId);
```

#### 2. NULL 반환 가능한 단건 조회

```java
// ❌ null 반환 가능 (NullPointerException 위험)
Employee findByPhoneNumber(String phoneNumber);

// ✅ Optional 사용
Optional<Employee> findByPhoneNumber(String phoneNumber);
```

#### 3. SQL Injection 위험

```java
// ❌ 문자열 결합 (SQL Injection 위험)
@Query("SELECT e FROM Employee e WHERE e.name = '" + name + "'")
List<Employee> findByName(String name);

// ✅ 파라미터 바인딩 사용
@Query("SELECT e FROM Employee e WHERE e.name = :name")
List<Employee> findByName(@Param("name") String name);
```

#### 4. N+1 문제 무시

```java
// ❌ N+1 문제 발생
@Query("SELECT e FROM Employee e WHERE e.siteId = :siteId")
List<Employee> findBySiteId(@Param("siteId") Long siteId);
// 이후 e.getSite()를 호출하면 N번의 추가 쿼리 발생

// ✅ JOIN FETCH 사용
@Query("SELECT e FROM Employee e JOIN FETCH e.site WHERE e.siteId = :siteId")
List<Employee> findBySiteIdWithSite(@Param("siteId") Long siteId);
```

#### 5. 실제 DELETE 사용

```java
// ❌ 실제 데이터 삭제 (복구 불가능)
void deleteById(Long id);

// ✅ 소프트 삭제 사용 (Service에서 처리)
// Service 레이어에서 employee.delete() 호출
```

### ✅ 권장사항

#### 1. 메서드명은 명확하게

```java
// ✅ 명확한 메서드명
findByIdAndNotDeleted(Long id)
findAllBySiteIdAndNotDeleted(Long siteId)
existsByPhoneNumberAndNotDeleted(String phoneNumber)

// ❌ 불명확한 메서드명
findById(Long id)  // 삭제된 것도 조회되는지 불명확
findBySite(Long siteId)  // 파라미터가 siteId인지 Site 객체인지 불명확
```

#### 2. 복잡한 쿼리는 @Query 사용

```java
// ✅ @Query 사용 (가독성 좋음)
@Query("""
    SELECT e FROM Employee e
    WHERE e.siteId = :siteId
    AND e.status = :status
    AND e.isDeleted = false
    ORDER BY e.createdAt DESC
""")
List<Employee> findBySiteIdAndStatusAndNotDeleted(
    @Param("siteId") Long siteId,
    @Param("status") EmployeeStatus status
);

// ❌ 메서드명으로 표현 (너무 길고 복잡)
List<Employee> findBySiteIdAndStatusAndIsDeletedFalseOrderByCreatedAtDesc(
    Long siteId, EmployeeStatus status
);
```

#### 3. 페이징은 Pageable 사용

```java
// ✅ Pageable 사용 (표준화)
Page<Employee> findAllNotDeleted(Pageable pageable);

// ❌ 직접 구현 (비표준)
@Query("SELECT e FROM Employee e WHERE e.isDeleted = false LIMIT :limit OFFSET :offset")
List<Employee> findAllWithPaging(@Param("limit") int limit, @Param("offset") int offset);
```

#### 4. DTO Projection으로 성능 최적화

```java
// ✅ DTO Projection (필요한 컬럼만 조회)
@Query("""
    SELECT new com.concrete.buildup.domain.employee.dto.EmployeeSummaryDto(
        e.id, e.name, e.phoneNumber
    )
    FROM Employee e
    WHERE e.isDeleted = false
""")
List<EmployeeSummaryDto> findAllSummary();

// ❌ 전체 엔티티 조회 (불필요한 컬럼 포함)
@Query("SELECT e FROM Employee e WHERE e.isDeleted = false")
List<Employee> findAll();  // 모든 컬럼 조회
```

#### 5. 주석으로 의도 명확히

```java
/**
 * 현장별 활성 사원 수 조회
 *
 * 삭제되지 않고 상태가 ACTIVE인 사원만 계산합니다.
 *
 * @param siteId 현장 ID
 * @param status 사원 상태 (일반적으로 ACTIVE)
 * @return 활성 사원 수
 */
@Query("SELECT COUNT(e) FROM Employee e WHERE e.siteId = :siteId AND e.status = :status AND e.isDeleted = false")
long countBySiteIdAndStatusAndNotDeleted(
    @Param("siteId") Long siteId,
    @Param("status") EmployeeStatus status
);
```

---

## 추가 참고 자료

### 파일 위치
- **BaseEntity**: `src/main/java/com/concrete/buildup/global/common/BaseEntity.java`
- **도메인별 Repository**: `src/main/java/com/concrete/buildup/domain/{domain}/repository/`

### 관련 문서
- [CLAUDE.md](../CLAUDE.md) - 프로젝트 전체 구조 및 컨벤션
- [Business Exception Guide](./business-exception-guide.md) - 예외 처리 가이드
- [도메인 구조](../CLAUDE.md#프로젝트-구조)

---

## 버전 정보
- **최초 작성**: 2025-10-30
- **Build-Up Platform 버전**: Spring Boot 3.5.7, Java 21
- **작성자**: Claude Code