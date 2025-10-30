---
description: Build-Up Platform Entity 패턴 가이드
globs: "**/entity/*.java"
alwaysApply: true
---

# Entity Pattern for Build-Up Platform

Build-Up Platform의 JPA Entity 작성 표준 가이드입니다. 일관된 데이터 모델링과 효율적인 데이터베이스 설계를 위한 패턴을 제공합니다.

## 목차
1. [기본 구조](#기본-구조)
2. [BaseEntity](#baseentity)
3. [연관관계 매핑](#연관관계-매핑)
4. [인덱스 전략](#인덱스-전략)
5. [도메인별 예시](#도메인별-예시)
6. [비즈니스 메서드](#비즈니스-메서드)

---

## 기본 구조

### 1. Entity 클래스 구조

```java
package com.concrete.buildup.domain.{domain}.entity;

import com.concrete.buildup.global.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * {리소스} 엔티티
 */
@Entity
@Table(name = "table_name", indexes = {
    @Index(name = "idx_table_column", columnList = "column_name")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Resource extends BaseEntity {

    @NotBlank(message = "이름은 필수입니다")
    @Size(max = 100, message = "이름은 100자를 초과할 수 없습니다")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.ACTIVE;

    // 비즈니스 메서드
    public void activate() {
        this.status = Status.ACTIVE;
    }

    public boolean isActive() {
        return Status.ACTIVE.equals(this.status);
    }
}
```

### 2. 필수 어노테이션

#### 클래스 레벨
```java
@Entity                     // JPA 엔티티 선언
@Table(name = "table_name") // 테이블명 (snake_case)
@Getter                     // Lombok Getter
@Setter                     // Lombok Setter
@Builder                    // Builder 패턴
@NoArgsConstructor         // JPA 필수 (기본 생성자)
@AllArgsConstructor        // Builder와 함께 사용
public class Entity extends BaseEntity {
    // ...
}
```

**Lombok 어노테이션 선택 이유:**
- `@Data` 대신 `@Getter`, `@Setter` 사용 권장 (equals/hashCode 충돌 방지)
- `@NoArgsConstructor`: JPA가 리플렉션으로 엔티티 생성 시 필수
- `@AllArgsConstructor`: Builder 내부 사용
- `@Builder`: 가독성 좋은 객체 생성

#### 필드 레벨
```java
// 기본 키
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

// 컬럼 매핑
@Column(name = "column_name", nullable = false, length = 100)
private String fieldName;

// Enum 타입 (항상 STRING 사용)
@Enumerated(EnumType.STRING)
@Column(name = "status", length = 20)
private Status status;

// 날짜/시간
@Column(name = "joined_date")
private LocalDate joinedDate;

@Column(name = "created_at")
private LocalDateTime createdAt;
```

---

## BaseEntity

모든 Entity는 `BaseEntity`를 상속받아 공통 필드를 사용합니다.

### BaseEntity 구조

```java
package com.concrete.buildup.global.common;

import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    // 소프트 삭제
    public void delete() {
        this.isDeleted = true;
    }

    // 삭제 복구
    public void restore() {
        this.isDeleted = false;
    }
}
```

### 상속 예시

```java
@Entity
@Table(name = "employees")
public class Employee extends BaseEntity {
    // BaseEntity의 id, createdAt, updatedAt, isDeleted 자동 포함
    // 추가 필드만 정의
    private String name;
    private String phone;
}
```

---

## 연관관계 매핑

### 1. ManyToOne (다대일)

**항상 LAZY 로딩 사용**

```java
@Entity
@Table(name = "employees")
public class Employee extends BaseEntity {

    // 사원 -> 현장 (다대일)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    // 사원 -> 사용자 (다대일)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}
```

### 2. OneToMany (일대다)

**항상 LAZY 로딩, mappedBy 필수**

```java
@Entity
@Table(name = "sites")
public class Site extends BaseEntity {

    // 현장 -> 사원들 (일대다)
    @OneToMany(mappedBy = "site", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Employee> employees = new ArrayList<>();

    // 연관관계 편의 메서드
    public void addEmployee(Employee employee) {
        this.employees.add(employee);
        employee.setSite(this);
    }
}
```

### 3. OneToOne (일대일)

```java
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    // 사용자 -> 사원 (일대일)
    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    private Employee employee;

    // 사용자 -> 관리자 (일대일)
    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    private Manager manager;
}
```

### 4. 연관관계 주의사항

```java
// ✅ 올바른 예시: LAZY 로딩
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "site_id")
private Site site;

// ❌ 잘못된 예시: EAGER 로딩 (N+1 문제 발생)
@ManyToOne(fetch = FetchType.EAGER)  // 금지!
@JoinColumn(name = "site_id")
private Site site;

// ✅ 올바른 예시: Enum STRING 타입
@Enumerated(EnumType.STRING)
private Status status;

// ❌ 잘못된 예시: Enum ORDINAL 타입 (순서 변경 시 데이터 오류)
@Enumerated(EnumType.ORDINAL)  // 금지!
private Status status;
```

---

## 인덱스 전략

### 1. 단일 컬럼 인덱스

```java
@Table(name = "employees", indexes = {
    @Index(name = "idx_employees_name", columnList = "name"),
    @Index(name = "idx_employees_phone", columnList = "phone"),
    @Index(name = "idx_employees_status", columnList = "status")
})
```

### 2. 복합 인덱스

```java
@Table(name = "contracts", indexes = {
    // 현장별, 상태별 조회
    @Index(name = "idx_contracts_site_status", columnList = "site_id, status"),
    // 사원별, 날짜 범위 조회
    @Index(name = "idx_contracts_employee_date", columnList = "employee_id, start_date, end_date")
})
```

### 3. 유니크 인덱스

```java
@Table(name = "users", uniqueConstraints = {
    @UniqueConstraint(name = "uk_users_user_id", columnNames = "user_id"),
    @UniqueConstraint(name = "uk_users_phone", columnNames = "phone")
})
```

### 4. 인덱스 네이밍 규칙

```
idx_{table_name}_{column1}_{column2}  // 일반 인덱스
uk_{table_name}_{column}              // 유니크 인덱스
fk_{table_name}_{ref_table}           // 외래 키
```

---

## 도메인별 예시

### 1. User (사용자)

```java
package com.concrete.buildup.domain.auth.entity;

import com.concrete.buildup.global.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 사용자 엔티티
 * - 근로자, 현장 관리자, 기업 관리자 공통
 */
@Entity
@Table(name = "users",
    indexes = {
        @Index(name = "idx_users_user_id", columnList = "user_id"),
        @Index(name = "idx_users_phone", columnList = "phone")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_user_id", columnNames = "user_id"),
        @UniqueConstraint(name = "uk_users_phone", columnNames = "phone")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {

    @NotBlank(message = "아이디는 필수입니다")
    @Size(min = 4, max = 20, message = "아이디는 4-20자 사이여야 합니다")
    @Column(name = "user_id", unique = true, nullable = false, length = 20)
    private String userId;

    @NotBlank(message = "비밀번호는 필수입니다")
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @NotBlank(message = "전화번호는 필수입니다")
    @Pattern(regexp = "^01[0-9]-\\d{3,4}-\\d{4}$")
    @Column(name = "phone", unique = true, nullable = false, length = 20)
    private String phone;

    @NotNull(message = "사용자 타입은 필수입니다")
    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false, length = 20)
    private UserType userType;  // WORKER, SITE_MANAGER, COMPANY

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    // 비즈니스 메서드
    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return UserStatus.ACTIVE.equals(this.status);
    }

    public void suspend() {
        this.status = UserStatus.SUSPENDED;
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
    }
}
```

### 2. Employee (사원)

```java
package com.concrete.buildup.domain.employee.entity;

import com.concrete.buildup.domain.auth.entity.User;
import com.concrete.buildup.domain.site.entity.Site;
import com.concrete.buildup.global.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

/**
 * 사원 엔티티
 */
@Entity
@Table(name = "employees",
    indexes = {
        @Index(name = "idx_employees_site", columnList = "site_id"),
        @Index(name = "idx_employees_name", columnList = "name"),
        @Index(name = "idx_employees_type", columnList = "employee_type")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Employee extends BaseEntity {

    @NotBlank(message = "사원명은 필수입니다")
    @Size(max = 50, message = "사원명은 50자를 초과할 수 없습니다")
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Pattern(regexp = "^01[0-9]-\\d{3,4}-\\d{4}$", message = "전화번호 형식이 올바르지 않습니다")
    @Column(name = "phone", length = 20)
    private String phone;

    @Email(message = "이메일 형식이 올바르지 않습니다")
    @Column(name = "email", length = 100)
    private String email;

    @NotNull(message = "사원 유형은 필수입니다")
    @Enumerated(EnumType.STRING)
    @Column(name = "employee_type", nullable = false, length = 20)
    private EmployeeType employeeType;  // REGULAR, DAILY

    @Column(name = "joined_date")
    private LocalDate joinedDate;

    @Column(name = "position", length = 50)
    private String position;  // 직책

    @Column(name = "department", length = 50)
    private String department;  // 부서

    // 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // 비즈니스 메서드
    public boolean isRegular() {
        return EmployeeType.REGULAR.equals(this.employeeType);
    }

    public boolean isDaily() {
        return EmployeeType.DAILY.equals(this.employeeType);
    }

    public void updateSite(Site newSite) {
        this.site = newSite;
    }
}
```

### 3. Site (현장)

```java
package com.concrete.buildup.domain.site.entity;

import com.concrete.buildup.global.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

/**
 * 현장 엔티티
 */
@Entity
@Table(name = "sites",
    indexes = {
        @Index(name = "idx_sites_name", columnList = "name"),
        @Index(name = "idx_sites_status", columnList = "status")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Site extends BaseEntity {

    @NotNull(message = "기업 ID는 필수입니다")
    @Column(name = "corporation_id", nullable = false)
    private Long corporationId;

    @NotBlank(message = "현장명은 필수입니다")
    @Size(min = 2, max = 100, message = "현장명은 2-100자 사이여야 합니다")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @NotBlank(message = "현장 주소는 필수입니다")
    @Size(max = 200, message = "현장 주소는 200자를 초과할 수 없습니다")
    @Column(name = "address", nullable = false, length = 200)
    private String address;

    @NotNull(message = "시작일은 필수입니다")
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SiteStatus status = SiteStatus.ACTIVE;

    // 시크릿 키
    @Column(name = "worker_secret_key", nullable = false, length = 50)
    private String workerSecretKey;

    @Column(name = "manager_secret_key", nullable = false, length = 50)
    private String managerSecretKey;

    // 비즈니스 메서드
    public boolean isActive() {
        return SiteStatus.ACTIVE.equals(this.status);
    }

    public void complete() {
        this.status = SiteStatus.COMPLETED;
        this.endDate = LocalDate.now();
    }

    public void suspend() {
        this.status = SiteStatus.SUSPENDED;
    }

    public boolean isInProgress() {
        LocalDate now = LocalDate.now();
        return (now.isEqual(startDate) || now.isAfter(startDate))
                && (endDate == null || now.isBefore(endDate) || now.isEqual(endDate));
    }
}
```

### 4. Contract (계약)

```java
package com.concrete.buildup.domain.contract.entity;

import com.concrete.buildup.domain.employee.entity.Employee;
import com.concrete.buildup.domain.site.entity.Site;
import com.concrete.buildup.global.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 근로계약서 엔티티
 */
@Entity
@Table(name = "contracts",
    indexes = {
        @Index(name = "idx_contracts_employee", columnList = "employee_id"),
        @Index(name = "idx_contracts_site", columnList = "site_id"),
        @Index(name = "idx_contracts_status", columnList = "status"),
        @Index(name = "idx_contracts_site_status", columnList = "site_id, status")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Contract extends BaseEntity {

    @NotNull(message = "계약 유형은 필수입니다")
    @Enumerated(EnumType.STRING)
    @Column(name = "contract_type", nullable = false, length = 20)
    private ContractType contractType;  // REGULAR, DAILY

    @NotNull(message = "계약 시작일은 필수입니다")
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @NotNull(message = "계약 종료일은 필수입니다")
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @NotNull(message = "기본급은 필수입니다")
    @Positive(message = "기본급은 양수여야 합니다")
    @Column(name = "base_salary", nullable = false)
    private Integer baseSalary;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ContractStatus status = ContractStatus.DRAFT;  // DRAFT, SENT, SIGNED, TERMINATED

    @Column(name = "signature_hash", length = 255)
    private String signatureHash;

    @Column(name = "signature_image_url", length = 500)
    private String signatureImageUrl;

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @Column(name = "special_notes", columnDefinition = "TEXT")
    private String specialNotes;

    // 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    // 비즈니스 메서드
    public void send() {
        this.status = ContractStatus.SENT;
    }

    public void sign(String signatureHash, String signatureImageUrl) {
        this.status = ContractStatus.SIGNED;
        this.signatureHash = signatureHash;
        this.signatureImageUrl = signatureImageUrl;
        this.signedAt = LocalDateTime.now();
    }

    public void terminate(String reason) {
        this.status = ContractStatus.TERMINATED;
        this.specialNotes = (this.specialNotes != null ? this.specialNotes + "\n\n" : "")
                + "해지 사유: " + reason + "\n해지 일시: " + LocalDateTime.now();
    }

    public boolean isSigned() {
        return ContractStatus.SIGNED.equals(this.status);
    }

    public boolean isSent() {
        return ContractStatus.SENT.equals(this.status);
    }

    public boolean isTerminated() {
        return ContractStatus.TERMINATED.equals(this.status);
    }
}
```

### 5. Attendance (근태)

```java
package com.concrete.buildup.domain.attendance.entity;

import com.concrete.buildup.domain.employee.entity.Employee;
import com.concrete.buildup.domain.site.entity.Site;
import com.concrete.buildup.global.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 근태 엔티티
 */
@Entity
@Table(name = "attendances",
    indexes = {
        @Index(name = "idx_attendances_employee_date", columnList = "employee_id, date"),
        @Index(name = "idx_attendances_site_date", columnList = "site_id, date")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Attendance extends BaseEntity {

    @NotNull(message = "날짜는 필수입니다")
    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;

    @Column(name = "check_out_time")
    private LocalDateTime checkOutTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AttendanceStatus status = AttendanceStatus.ABSENT;  // PRESENT, ABSENT, LATE, EARLY_LEAVE

    @Column(name = "working_hours")
    private Integer workingHours;  // 분 단위

    @Column(name = "overtime_hours")
    private Integer overtimeHours;  // 분 단위

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    // 비즈니스 메서드
    public void checkIn() {
        this.checkInTime = LocalDateTime.now();
        this.status = AttendanceStatus.PRESENT;
    }

    public void checkOut() {
        this.checkOutTime = LocalDateTime.now();
        calculateWorkingHours();
    }

    private void calculateWorkingHours() {
        if (checkInTime != null && checkOutTime != null) {
            long minutes = java.time.Duration.between(checkInTime, checkOutTime).toMinutes();
            this.workingHours = (int) minutes;

            // 8시간(480분) 초과 시 연장 근무
            if (minutes > 480) {
                this.overtimeHours = (int) (minutes - 480);
            }
        }
    }

    public boolean isCheckedIn() {
        return checkInTime != null;
    }

    public boolean isCheckedOut() {
        return checkOutTime != null;
    }
}
```

---

## 비즈니스 메서드

Entity에는 도메인 로직을 포함한 비즈니스 메서드를 작성합니다.

### 1. 상태 변경 메서드

```java
// 상태 전환
public void activate() {
    this.status = Status.ACTIVE;
}

public void suspend() {
    this.status = Status.SUSPENDED;
}

public void complete() {
    this.status = Status.COMPLETED;
    this.endDate = LocalDate.now();
}
```

### 2. 검증 메서드

```java
// 상태 확인
public boolean isActive() {
    return Status.ACTIVE.equals(this.status);
}

public boolean isSigned() {
    return ContractStatus.SIGNED.equals(this.status);
}

public boolean isExpired() {
    return endDate != null && endDate.isBefore(LocalDate.now());
}
```

### 3. 계산 메서드

```java
// 근무 시간 계산
public void calculateWorkingHours() {
    if (checkInTime != null && checkOutTime != null) {
        long minutes = Duration.between(checkInTime, checkOutTime).toMinutes();
        this.workingHours = (int) minutes;

        if (minutes > 480) {  // 8시간 초과
            this.overtimeHours = (int) (minutes - 480);
        }
    }
}

// 급여 계산
public int calculateTotalSalary() {
    int total = baseSalary;
    if (overtimePay != null) {
        total += overtimePay;
    }
    if (bonuses != null) {
        total += bonuses;
    }
    return total;
}
```

### 4. 연관관계 편의 메서드

```java
// 양방향 연관관계 설정
public void addEmployee(Employee employee) {
    this.employees.add(employee);
    employee.setSite(this);
}

public void removeEmployee(Employee employee) {
    this.employees.remove(employee);
    employee.setSite(null);
}
```

---

## 추가 규칙

### 1. Enum 정의

```java
package com.concrete.buildup.domain.auth.entity;

public enum UserType {
    WORKER("근로자"),
    SITE_MANAGER("현장 관리자"),
    COMPANY("기업 관리자");

    private final String description;

    UserType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
```

### 2. 컬럼 타입 지정

```java
// TEXT 타입
@Column(name = "content", columnDefinition = "TEXT")
private String content;

// JSON 타입 (MySQL 5.7+)
@Column(name = "metadata", columnDefinition = "JSON")
private String metadata;

// 날짜
@Column(name = "birth_date")
private LocalDate birthDate;

// 날짜+시간
@Column(name = "created_at")
private LocalDateTime createdAt;
```

### 3. 소프트 삭제

```java
// BaseEntity에 isDeleted 필드 포함
public class BaseEntity {
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    public void delete() {
        this.isDeleted = true;
    }

    public void restore() {
        this.isDeleted = false;
    }
}

// Repository에서 소프트 삭제 쿼리
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    List<Employee> findByIsDeletedFalse();

    @Query("SELECT e FROM Employee e WHERE e.isDeleted = false")
    List<Employee> findAllActive();
}
```

---

## 체크리스트

### Entity 작성 시
- [ ] `domain/{domain}/entity/` 폴더에 위치
- [ ] `BaseEntity` 상속
- [ ] `@Entity`, `@Table`, Lombok 어노테이션 추가
- [ ] 테이블명과 컬럼명은 snake_case 사용
- [ ] `@Enumerated(EnumType.STRING)` 사용
- [ ] 연관관계는 `FetchType.LAZY` 사용
- [ ] 인덱스 추가 (검색 조건이 되는 컬럼)
- [ ] Validation 어노테이션 추가
- [ ] 비즈니스 메서드 작성

### 코드 리뷰 시
- [ ] BaseEntity 상속 확인
- [ ] EAGER 로딩 사용 여부 확인 (금지)
- [ ] Enum ORDINAL 타입 사용 여부 확인 (금지)
- [ ] 인덱스가 적절히 설정되었는지 확인
- [ ] 비즈니스 로직이 Entity에 포함되었는지 확인
- [ ] 양방향 연관관계의 경우 편의 메서드 확인