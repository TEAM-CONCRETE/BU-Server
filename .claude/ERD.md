# ERD (Entity Relationship Diagram)

> **⚠️ 개발 필수 참고 문서**
이 ERD는 Build-Up Platform의 데이터베이스 구조를 정의한 문서입니다.
모든 개발 작업 시 이 ERD 구조를 기준으로 엔티티, Repository, Service를 구현하십시오.
테이블 변경이 필요한 경우 반드시 팀 논의 후 이 문서를 먼저 업데이트하십시오.

Build-Up Platform 데이터베이스 구조 설계 문서입니다.

## 데이터베이스 개요

**데이터베이스명:** buildup  
**DBMS:** MySQL 8.0  
**인코딩:** UTF-8  
**목적:** 건설 현장 근로 계약, 급여, 안전 관리 시스템

---

## 테이블 목록

| 테이블명 | 설명 | 주요 관계 |
|----------|------|-----------|
| `roles` | 역할 관리 | users → roles |
| `users` | 사용자 기본 정보 | 1:1 → employees/managers/corporations |
| `employees` | 근로자 정보 | users ← employees |
| `managers` | 현장 관리자 정보 | users ← managers |
| `corporations` | 기업 정보 | users ← corporations |
| `contracts` | 근로 계약 기본 정보 | employees, corporations, managers |
| `contract_details` | 계약 상세 정보 (스냅샷) | 1:1 with contracts |
| `contract_sign_logs` | 계약 서명 이력 | contracts → sign_logs |
| `sites` | 현장 정보 | corporation, manager |
| `attendances` | 근태 기록 | employee, contract, site |
| `payrolls` | 급여 정보 | employee, contract, corporation |
| `payslip_items` | 급여 명세 항목 | payrolls → items |
| `safety_docs` | 안전교육 문서 | site, manager |
| `safety_doc_attendees` | 안전교육 참석자 | safety_docs, employees |
| `safety_sign_logs` | 안전교육 서명 이력 | 1:1 with attendees |
| `work_reports` | 작업일보 | site, manager, corporation |
| `work_report_employees` | 작업일보 투입 인력 | work_reports, employees |
| `work_report_materials` | 작업일보 자재 사용 | work_reports |

---

## 테이블 상세

### 1. roles (역할)

**설명:** 사용자 역할 관리 (EMPLOYEE, MANAGER, CORPORATION, ADMIN)

**컬럼:**

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 역할 ID |
| `role_name` | VARCHAR(50) | NOT NULL, UNIQUE | 역할명 |
| `description` | TEXT | NULL | 역할 설명 |
| `created_at` | DATETIME | DEFAULT now() | 생성 일시 |

**인덱스:**
- PRIMARY KEY: `id`
- UNIQUE INDEX: `role_name`

**관계:**
- 1:N → users (한 역할에 여러 사용자)

---

### 2. users (사용자)

**설명:** 플랫폼 사용자 기본 정보 및 인증 정보

**컬럼:**

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 사용자 ID |
| `user_id` | VARCHAR(50) | NOT NULL, UNIQUE | 로그인용 ID |
| `password` | VARCHAR(255) | NOT NULL | 비밀번호 (해시) |
| `phone` | VARCHAR(20) | NOT NULL | 전화번호 |
| `email` | VARCHAR(100) | NULL | 이메일 |
| `secret_key` | VARCHAR(100) | NULL | 인증용 시크릿키 |
| `role_id` | BIGINT | FK, NULLABLE | 역할 (계약 시 할당) |
| `created_at` | DATETIME | DEFAULT now() | 생성 일시 |
| `updated_at` | DATETIME | DEFAULT now() | 수정 일시 |

**인덱스:**
- PRIMARY KEY: `id`
- UNIQUE INDEX: `user_id`
- INDEX: `role_id`
- FOREIGN KEY: `role_id` REFERENCES `roles(id)`

**관계:**
- N:1 → roles
- 1:1 → employees (user_id)
- 1:1 → managers (user_id)
- 1:1 → corporations (user_id)

---

### 3. employees (근로자)

**설명:** 근로자 상세 정보

**컬럼:**

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 근로자 ID |
| `user_id` | BIGINT | FK, NOT NULL | 사용자 ID |
| `emp_name` | VARCHAR(50) | NOT NULL | 근로자 이름 |
| `sub_phone` | VARCHAR(20) | NULL | 비상 연락망 |
| `resident_num` | VARCHAR(20) | NULL | 주민등록번호 |
| `emp_address` | VARCHAR(255) | NULL | 주소 |
| `emp_type` | VARCHAR(30) | NULL | 근로자 유형 (REGULAR/DAILY) |
| `created_at` | DATETIME | DEFAULT now() | 생성 일시 |
| `updated_at` | DATETIME | DEFAULT now() | 수정 일시 |

**인덱스:**
- PRIMARY KEY: `id`
- UNIQUE INDEX: `user_id`
- INDEX: `emp_type`
- FOREIGN KEY: `user_id` REFERENCES `users(id)`

**관계:**
- 1:1 → users
- 1:N ← contracts
- 1:N ← attendances

---

### 4. managers (현장 관리자)

**설명:** 현장 관리자 정보

**컬럼:**

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 관리자 ID |
| `user_id` | BIGINT | FK, NOT NULL | 사용자 ID |
| `manager_name` | VARCHAR(50) | NOT NULL | 관리자 이름 |
| `created_at` | DATETIME | DEFAULT now() | 생성 일시 |
| `updated_at` | DATETIME | DEFAULT now() | 수정 일시 |

**인덱스:**
- PRIMARY KEY: `id`
- UNIQUE INDEX: `user_id`
- FOREIGN KEY: `user_id` REFERENCES `users(id)`

**관계:**
- 1:1 → users
- 1:N ← contracts
- 1:N ← sites

---

### 5. corporations (기업)

**설명:** 기업 정보

**컬럼:**

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 기업 ID |
| `user_id` | BIGINT | FK, NOT NULL | 사용자 ID |
| `corp_name` | VARCHAR(100) | NOT NULL | 회사명 |
| `corp_address` | VARCHAR(255) | NULL | 본사 주소 |
| `corp_ceo_name` | VARCHAR(50) | NULL | 대표자 이름 |
| `created_at` | DATETIME | DEFAULT now() | 생성 일시 |
| `updated_at` | DATETIME | DEFAULT now() | 수정 일시 |

**인덱스:**
- PRIMARY KEY: `id`
- UNIQUE INDEX: `user_id`
- FOREIGN KEY: `user_id` REFERENCES `users(id)`

**관계:**
- 1:1 → users
- 1:N ← contracts
- 1:N ← sites

---

### 6. contracts (근로 계약)

**설명:** 근로 계약 기본 정보 및 상태 관리

**컬럼:**

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 계약 ID |
| `employee_id` | BIGINT | FK, NOT NULL | 근로자 ID |
| `corporation_id` | BIGINT | FK, NOT NULL | 기업 ID |
| `manager_id` | BIGINT | FK, NULL | 관리자 ID |
| `role` | VARCHAR(30) | NULL | 계약 시 역할 |
| `contract_state` | VARCHAR(30) | NULL | 계약 상태 (DRAFT/SENT/CORP_SIGNED/FULLY_SIGNED/TERMINATED) |
| `employee_start_date` | DATE | NOT NULL | 근로 시작일 |
| `employee_end_date` | DATE | NULL | 근로 종료일 |
| `written_at` | DATETIME | DEFAULT now() | 계약서 작성일 |
| `corp_signed_at` | DATETIME | NULL | 기업 서명일 |
| `emp_signed_at` | DATETIME | NULL | 근로자 서명일 |
| `created_at` | DATETIME | DEFAULT now() | 생성 일시 |
| `updated_at` | DATETIME | DEFAULT now() | 수정 일시 |

**인덱스:**
- PRIMARY KEY: `id`
- INDEX: `employee_id`, `corporation_id`, `manager_id`
- INDEX: `contract_state`
- INDEX: `employee_start_date`
- FOREIGN KEY: `employee_id` REFERENCES `employees(id)`
- FOREIGN KEY: `corporation_id` REFERENCES `corporations(id)`
- FOREIGN KEY: `manager_id` REFERENCES `managers(id)`

**관계:**
- N:1 → employees
- N:1 → corporations
- N:1 → managers
- 1:1 ← contract_details
- 1:N ← contract_sign_logs
- 1:N ← attendances

---

### 7. contract_details (계약 상세)

**설명:** 계약 상세 정보 (스냅샷 데이터, 계약 당시 정보 보존)

**컬럼:**

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 상세 ID |
| `contract_id` | BIGINT | FK, NOT NULL | 계약 ID |
| `corp_name` | VARCHAR(100) | NOT NULL | 회사명 (스냅샷) |
| `emp_name` | VARCHAR(50) | NOT NULL | 근로자 이름 (스냅샷) |
| `work_place` | VARCHAR(255) | NULL | 근무 장소 |
| `work_type` | VARCHAR(100) | NULL | 직종 |
| `work_time` | VARCHAR(100) | NULL | 근로 시간 |
| `break_time` | VARCHAR(100) | NULL | 휴게 시간 |
| `work_on_day` | VARCHAR(100) | NULL | 근무일 |
| `work_off_day` | VARCHAR(100) | NULL | 휴일 |
| `work_pay` | DECIMAL(15,2) | NULL | 기본 임금 |
| `work_bonus` | DECIMAL(15,2) | NULL | 상여금 |
| `additional_hour_pay` | DECIMAL(15,2) | NULL | 시간 외 근로 수당 |
| `additional_night_pay` | DECIMAL(15,2) | NULL | 야간 근로 수당 |
| `additional_holiday_pay` | DECIMAL(15,2) | NULL | 휴일 근로 수당 |
| `payday` | VARCHAR(30) | NULL | 임금 지급일 |
| `pay_period` | VARCHAR(30) | NULL | 지급 주기 (DAILY/WEEKLY/MONTHLY) |
| `pay_type` | VARCHAR(30) | NULL | 지급 방법 (CASH/TRANSFER) |
| `is_eoi_applicable` | BOOLEAN | NULL | 고용보험 적용 여부 |
| `is_wci_applicable` | BOOLEAN | NULL | 산재보험 적용 여부 |
| `is_nps_applicable` | BOOLEAN | NULL | 국민연금 적용 여부 |
| `is_nhi_applicable` | BOOLEAN | NULL | 건강보험 적용 여부 |
| `corp_address` | VARCHAR(255) | NULL | 회사 주소 (스냅샷) |
| `corp_ceo_name` | VARCHAR(50) | NULL | 대표자 이름 (스냅샷) |
| `emp_address` | VARCHAR(255) | NULL | 근로자 주소 (스냅샷) |
| `created_at` | DATETIME | DEFAULT now() | 생성 일시 |

**인덱스:**
- PRIMARY KEY: `id`
- UNIQUE INDEX: `contract_id`
- FOREIGN KEY: `contract_id` REFERENCES `contracts(id)`

**관계:**
- 1:1 → contracts

---

### 8. contract_sign_logs (계약 서명 로그)

**설명:** 계약서별 서명 이력 및 증적 데이터

**컬럼:**

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 서명 로그 ID |
| `contract_id` | BIGINT | FK, NOT NULL | 계약 ID |
| `signer_role` | VARCHAR(30) | NULL | 서명자 역할 (EMPLOYEE/CORPORATION/MANAGER) |
| `signer_id` | BIGINT | FK, NULL | 서명자 사용자 ID (외부인 NULL) |
| `signer_name` | VARCHAR(50) | NULL | 서명자 이름 |
| `signature_image_url` | VARCHAR(255) | NULL | S3 서명 이미지 URL |
| `signature_hash` | VARCHAR(255) | NULL | SHA-256 해시값 |
| `signed_ip` | VARCHAR(45) | NULL | 서명 시점 IP 주소 |
| `signed_device` | VARCHAR(100) | NULL | 서명 디바이스 정보 |
| `signed_at` | DATETIME | NULL | 서명 시각 |
| `verified_at` | DATETIME | NULL | 서명 검증 완료 시각 |
| `verification_status` | VARCHAR(20) | NULL | 검증 상태 (PENDING/VERIFIED/FAILED) |
| `created_at` | DATETIME | DEFAULT now() | 생성 일시 |
| `updated_at` | DATETIME | DEFAULT now() | 수정 일시 |

**인덱스:**
- PRIMARY KEY: `id`
- INDEX: `contract_id`, `signer_id`
- INDEX: `(contract_id, signer_role)` - 복합 인덱스
- INDEX: `verification_status`
- FOREIGN KEY: `contract_id` REFERENCES `contracts(id)`
- FOREIGN KEY: `signer_id` REFERENCES `users(id)`

**관계:**
- N:1 → contracts
- N:1 → users

---

### 9. sites (현장)

**설명:** 건설 현장 정보

**컬럼:**

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 현장 ID |
| `site_name` | VARCHAR(100) | NOT NULL | 현장명 |
| `site_address` | VARCHAR(255) | NULL | 현장 주소 |
| `corporation_id` | BIGINT | FK, NULL | 소속 기업 ID |
| `manager_id` | BIGINT | FK, NULL | 현장 관리자 ID |
| `created_at` | DATETIME | DEFAULT now() | 생성 일시 |
| `updated_at` | DATETIME | DEFAULT now() | 수정 일시 |

**인덱스:**
- PRIMARY KEY: `id`
- INDEX: `corporation_id`, `manager_id`
- FOREIGN KEY: `corporation_id` REFERENCES `corporations(id)`
- FOREIGN KEY: `manager_id` REFERENCES `managers(id)`

**관계:**
- N:1 → corporations
- N:1 → managers
- 1:N ← attendances

---

### 10. attendances (근태)

**설명:** 근로자 근태 기록

**컬럼:**

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 근태 ID |
| `contract_id` | BIGINT | FK, NOT NULL | 계약 ID |
| `employee_id` | BIGINT | FK, NOT NULL | 근로자 ID |
| `site_id` | BIGINT | FK, NOT NULL | 현장 ID |
| `search_date` | DATE | NOT NULL | 근무일자 (yyyy-mm-dd) |
| `emp_type` | VARCHAR(30) | NULL | 근로자 유형 |
| `emp_name` | VARCHAR(50) | NULL | 근로자 이름 |
| `resident_num` | VARCHAR(20) | NULL | 주민등록번호 |
| `attendance_status` | VARCHAR(20) | NULL | 출근 상태 (NORMAL/LATE/EARLY_LEAVE/ABSENT/DAY_OFF) |
| `check_in_time` | DATETIME | NULL | 출근 시간 |
| `check_out_time` | DATETIME | NULL | 퇴근 시간 |
| `total_work_hour` | DECIMAL(6,2) | NULL | 총 근로시간 |
| `night_work_hour` | DECIMAL(6,2) | NULL | 야간 근로시간 |
| `additional_work_hour` | DECIMAL(6,2) | NULL | 연장 근로시간 |
| `holiday_work_hour` | DECIMAL(6,2) | NULL | 휴일 근로시간 |
| `created_at` | DATETIME | DEFAULT now() | 생성 일시 |
| `updated_at` | DATETIME | DEFAULT now() | 수정 일시 |

**인덱스:**
- PRIMARY KEY: `id`
- INDEX: `contract_id`, `employee_id`, `site_id`
- INDEX: `search_date`
- INDEX: `(employee_id, search_date)` - 복합 인덱스
- FOREIGN KEY: `contract_id` REFERENCES `contracts(id)`
- FOREIGN KEY: `employee_id` REFERENCES `employees(id)`
- FOREIGN KEY: `site_id` REFERENCES `sites(id)`

**관계:**
- N:1 → contracts
- N:1 → employees
- N:1 → sites

---

### 11. payrolls (급여)

**설명:** 급여 정보

**컬럼:**

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 급여 ID |
| `employee_id` | BIGINT | FK, NOT NULL | 근로자 ID |
| `contract_id` | BIGINT | FK, NOT NULL | 계약 ID |
| `corporation_id` | BIGINT | FK, NOT NULL | 기업 ID |
| `search_date` | DATE | NOT NULL | 지급 기준월 (yyyy-mm) |
| `pay_due_date` | DATE | NULL | 지급 예정일 |
| `emp_type` | VARCHAR(30) | NULL | 근로자 유형 |
| `emp_name` | VARCHAR(50) | NULL | 근로자 이름 |
| `resident_num` | VARCHAR(20) | NULL | 주민등록번호 |
| `total_work_hour` | DECIMAL(8,2) | NULL | 총 근로시간 |
| `total_pay` | DECIMAL(15,2) | NULL | 총 지급액 |
| `total_pay_by_day` | DECIMAL(15,2) | NULL | 일급 기준 지급액 |
| `none_tax_income` | DECIMAL(15,2) | NULL | 비과세 소득 |
| `income_tax` | DECIMAL(15,2) | NULL | 소득세 |
| `resident_tax` | DECIMAL(15,2) | NULL | 주민세 |
| `pay_cycle` | VARCHAR(20) | NULL | 급여 주기 (DAILY/WEEKLY/MONTHLY) |
| `pay_status` | VARCHAR(20) | NULL | 지급 상태 (PENDING/PAID/CANCELLED) |
| `created_at` | DATETIME | DEFAULT now() | 생성 일시 |
| `updated_at` | DATETIME | DEFAULT now() | 수정 일시 |

**인덱스:**
- PRIMARY KEY: `id`
- INDEX: `employee_id`, `contract_id`, `corporation_id`
- INDEX: `search_date`
- INDEX: `pay_status`
- FOREIGN KEY: `employee_id` REFERENCES `employees(id)`
- FOREIGN KEY: `contract_id` REFERENCES `contracts(id)`
- FOREIGN KEY: `corporation_id` REFERENCES `corporations(id)`

**관계:**
- N:1 → employees
- N:1 → contracts
- N:1 → corporations
- 1:N ← payslip_items

---

### 12. payslip_items (급여 명세 항목)

**설명:** 급여명세 세부 항목 (각 급여 항목별 금액 및 타입 저장)

**컬럼:**

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 항목 ID |
| `payroll_id` | BIGINT | FK, NOT NULL | 급여 ID |
| `item_name` | VARCHAR(100) | NOT NULL | 항목명 (기본급, 식대, 소득세 등) |
| `item_type` | VARCHAR(30) | NOT NULL | 항목 구분 (EARNING/DEDUCTION) |
| `amount` | DECIMAL(15,2) | NOT NULL | 금액 |
| `effective_date` | DATE | NOT NULL | 적용 날짜 |
| `created_at` | DATETIME | DEFAULT now() | 생성 일시 |

**인덱스:**
- PRIMARY KEY: `id`
- INDEX: `payroll_id`
- FOREIGN KEY: `payroll_id` REFERENCES `payrolls(id)`

**관계:**
- N:1 → payrolls

---

### 13. safety_docs (안전교육 문서)

**설명:** 안전교육 일지

**컬럼:**

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 문서 ID |
| `site_id` | BIGINT | FK, NOT NULL | 현장 ID |
| `manager_id` | BIGINT | FK, NOT NULL | 작성 관리자 ID |
| `safetydoc_title` | VARCHAR(200) | NOT NULL | 교육 제목 |
| `safetydoc_type` | VARCHAR(50) | NULL | 교육 유형 (REGULAR/SPECIAL/EMERGENCY) |
| `safetydoc_context` | TEXT | NULL | 교육 내용 |
| `safetydoc_employee_cnt` | INT | NULL | 교육 대상 근로자 수 |
| `safetydoc_status` | VARCHAR(30) | NULL | 상태 (DRAFT/ONGOING/COMPLETED) |
| `safetydoc_created_at` | DATETIME | DEFAULT now() | 생성 일시 |
| `safetydoc_updated_at` | DATETIME | DEFAULT now() | 수정 일시 |

**인덱스:**
- PRIMARY KEY: `id`
- INDEX: `site_id`, `manager_id`
- INDEX: `safetydoc_status`
- FOREIGN KEY: `site_id` REFERENCES `sites(id)`
- FOREIGN KEY: `manager_id` REFERENCES `managers(id)`

**관계:**
- N:1 → sites
- N:1 → managers
- 1:N ← safety_doc_attendees

---

### 14. safety_doc_attendees (안전교육 참석자)

**설명:** 안전교육 참석 근로자 목록

**컬럼:**

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 참석자 ID |
| `safety_doc_id` | BIGINT | FK, NOT NULL | 안전교육 문서 ID |
| `employee_id` | BIGINT | FK, NOT NULL | 근로자 ID |
| `emp_name` | VARCHAR(50) | NULL | 근로자 이름 |
| `emp_type` | VARCHAR(30) | NULL | 근로자 유형 |
| `attendance_status` | VARCHAR(20) | NULL | 출석 상태 (PRESENT/ABSENT) |
| `signed_at` | DATETIME | NULL | 서명 시간 |
| `created_at` | DATETIME | DEFAULT now() | 생성 일시 |

**인덱스:**
- PRIMARY KEY: `id`
- INDEX: `safety_doc_id`, `employee_id`
- FOREIGN KEY: `safety_doc_id` REFERENCES `safety_docs(id)`
- FOREIGN KEY: `employee_id` REFERENCES `employees(id)`

**관계:**
- N:1 → safety_docs
- N:1 → employees
- 1:1 ← safety_sign_logs

---

### 15. safety_sign_logs (안전교육 서명 로그)

**설명:** 안전교육 전자서명 증적 데이터

**컬럼:**

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 서명 로그 ID |
| `attendee_id` | BIGINT | FK, NOT NULL, UNIQUE | 참석자 ID (1:1) |
| `signer_type` | VARCHAR(20) | NULL | 서명자 유형 (EMPLOYEE/MANAGER) |
| `signature_hash` | VARCHAR(255) | NOT NULL | 서명 데이터 해시값 |
| `signature_image_url` | VARCHAR(255) | NULL | S3 서명 이미지 경로 |
| `signed_device` | VARCHAR(100) | NULL | 서명 디바이스 정보 |
| `signed_ip` | VARCHAR(45) | NULL | 서명자 IP 주소 |
| `retention_until` | DATETIME | NULL | 보존 기간 |
| `signed_at` | DATETIME | DEFAULT now() | 서명 시각 |
| `verified_at` | DATETIME | NULL | 검증 완료 시각 |
| `verification_status` | VARCHAR(20) | NULL | 검증 상태 (PENDING/VERIFIED/FAILED) |
| `created_at` | DATETIME | DEFAULT now() | 생성 일시 |

**인덱스:**
- PRIMARY KEY: `id`
- UNIQUE INDEX: `attendee_id`
- INDEX: `verification_status`
- FOREIGN KEY: `attendee_id` REFERENCES `safety_doc_attendees(id)`

**관계:**
- 1:1 → safety_doc_attendees

---

### 16. work_reports (작업일보)

**설명:** 일일 작업 보고서

**컬럼:**

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 작업일보 ID |
| `site_id` | BIGINT | FK, NOT NULL | 현장 ID |
| `manager_id` | BIGINT | FK, NOT NULL | 작성 관리자 ID |
| `corporation_id` | BIGINT | FK, NOT NULL | 소속 회사 ID |
| `work_report_title` | VARCHAR(200) | NOT NULL | 작업일보 제목 |
| `work_report_created_at` | DATETIME | DEFAULT now() | 작성일시 |
| `work_report_started_at` | DATETIME | NOT NULL | 작업 시작일시 |
| `work_report_ended_at` | DATETIME | NOT NULL | 작업 종료일시 |
| `work_report_context` | TEXT | NULL | 당일 작업 요약 |
| `work_section_name` | VARCHAR(100) | NULL | 공정 구분명 |
| `work_section_employee_num` | INT | NULL | 공정별 투입 인원수 |
| `work_report_status` | VARCHAR(30) | NULL | 상태 (DRAFT/SUBMITTED/APPROVED) |
| `created_at` | DATETIME | DEFAULT now() | 생성 일시 |
| `updated_at` | DATETIME | DEFAULT now() | 수정 일시 |

**인덱스:**
- PRIMARY KEY: `id`
- INDEX: `site_id`, `manager_id`, `corporation_id`
- INDEX: `work_report_status`
- INDEX: `work_report_started_at`
- FOREIGN KEY: `site_id` REFERENCES `sites(id)`
- FOREIGN KEY: `manager_id` REFERENCES `managers(id)`
- FOREIGN KEY: `corporation_id` REFERENCES `corporations(id)`

**관계:**
- N:1 → sites
- N:1 → managers
- N:1 → corporations
- 1:N ← work_report_employees
- 1:N ← work_report_materials

---

### 17. work_report_employees (작업일보 투입 인력)

**설명:** 작업일보별 투입된 근로자 정보

**컬럼:**

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 레코드 ID |
| `work_report_id` | BIGINT | FK, NOT NULL | 작업일보 ID |
| `employee_id` | BIGINT | FK, NOT NULL | 근로자 ID |
| `emp_name` | VARCHAR(50) | NULL | 근로자 이름 |
| `emp_type` | VARCHAR(30) | NULL | 근로자 유형 |
| `work_hours` | DECIMAL(6,2) | NULL | 투입 시간 |
| `role_in_section` | VARCHAR(50) | NULL | 담당 역할 (용접공, 목수 등) |
| `created_at` | DATETIME | DEFAULT now() | 생성 일시 |

**인덱스:**
- PRIMARY KEY: `id`
- INDEX: `work_report_id`, `employee_id`
- FOREIGN KEY: `work_report_id` REFERENCES `work_reports(id)`
- FOREIGN KEY: `employee_id` REFERENCES `employees(id)`

**관계:**
- N:1 → work_reports
- N:1 → employees

---

### 18. work_report_materials (작업일보 자재)

**설명:** 작업일보별 자재 투입 기록

**컬럼:**

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 자재 기록 ID |
| `work_report_id` | BIGINT | FK, NOT NULL | 작업일보 ID |
| `material_name` | VARCHAR(100) | NOT NULL | 자재명 |
| `material_standard` | VARCHAR(100) | NULL | 규격 |
| `material_unit` | VARCHAR(50) | NULL | 단위 (EA, M, KG 등) |
| `material_today` | DECIMAL(10,2) | NULL | 금일 투입량 |
| `material_sum` | DECIMAL(10,2) | NULL | 누적 투입량 |
| `note` | VARCHAR(255) | NULL | 비고 |
| `created_at` | DATETIME | DEFAULT now() | 생성 일시 |

**인덱스:**
- PRIMARY KEY: `id`
- INDEX: `work_report_id`
- FOREIGN KEY: `work_report_id` REFERENCES `work_reports(id)`

**관계:**
- N:1 → work_reports

---

## ERD 다이어그램

```
┌─────────────┐       ┌──────────────┐       ┌──────────────┐
│    roles    │◄──────│    users     │──────►│  employees   │
└─────────────┘       └──────────────┘       └──────────────┘
                             │                       │
                             ├──────►┌──────────────┐│
                             │       │   managers   ││
                             │       └──────────────┘│
                             │                       │
                             └──────►┌──────────────┐│
                                     │ corporations ││
                                     └──────────────┘│
                                            │        │
                      ┌─────────────────────┴────────┴──────┐
                      │                                      │
              ┌───────▼────────┐                    ┌───────▼────────┐
              │   contracts    │◄───────────────────│     sites      │
              └───────┬────────┘                    └────────────────┘
                      │
         ┌────────────┼────────────┐
         │            │            │
  ┌──────▼─────┐ ┌───▼────────┐ ┌▼─────────────┐
  │contract_   │ │contract_   │ │attendances   │
  │details     │ │sign_logs   │ └──────────────┘
  └────────────┘ └────────────┘
```

---

## 주요 관계 정리

### 1:1 관계
- `users` ↔ `employees` (user_id)
- `users` ↔ `managers` (user_id)
- `users` ↔ `corporations` (user_id)
- `contracts` ↔ `contract_details` (contract_id)
- `safety_doc_attendees` ↔ `safety_sign_logs` (attendee_id)

### 1:N 관계
- `roles` → `users`
- `employees` → `contracts`
- `corporations` → `contracts`
- `managers` → `contracts`
- `contracts` → `contract_sign_logs`
- `contracts` → `attendances`
- `employees` → `attendances`
- `sites` → `attendances`
- `employees` → `payrolls`
- `payrolls` → `payslip_items`
- `sites` → `safety_docs`
- `safety_docs` → `safety_doc_attendees`
- `sites` → `work_reports`
- `work_reports` → `work_report_employees`
- `work_reports` → `work_report_materials`

### N:M 관계
- 없음 (중간 테이블로 관계 해소)

---

## 인덱스 전략

### 복합 인덱스
```sql
-- 계약 서명 조회 최적화
CREATE INDEX idx_sign_logs_contract_signer 
ON contract_sign_logs(contract_id, signer_role);

-- 근태 조회 최적화
CREATE INDEX idx_attendances_employee_date 
ON attendances(employee_id, search_date);

-- 급여 조회 최적화
CREATE INDEX idx_payrolls_employee_date 
ON payrolls(employee_id, search_date);
```

### 성능 최적화 인덱스
```sql
-- 계약 상태별 조회
CREATE INDEX idx_contracts_state ON contracts(contract_state);

-- 서명 검증 상태별 조회
CREATE INDEX idx_sign_logs_verification 
ON contract_sign_logs(verification_status);

-- 급여 지급 상태별 조회
CREATE INDEX idx_payrolls_status ON payrolls(pay_status);

-- 안전교육 상태별 조회
CREATE INDEX idx_safety_docs_status 
ON safety_docs(safetydoc_status);

-- 작업일보 상태별 조회
CREATE INDEX idx_work_reports_status 
ON work_reports(work_report_status);
```

---

## 변경 이력

| 날짜 | 변경 내용 | 작성자 |
|------|-----------|--------|
| 2025-11-03 | ERD 문서 초안 작성 | 문현민 |
| 2025-11-03 | contract_sign_logs 테이블 추가 | 문현민 |

---