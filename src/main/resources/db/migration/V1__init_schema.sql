-- ============================================
-- Build-Up Platform - Initial Schema (ERD 기반)
-- Version: 1.0
-- Date: 2025-11-02
-- Description: 실제 ERD에 맞춘 초기 데이터베이스 스키마
-- Reference: Build-Up_ERD-5.pdf
-- ============================================

-- ============================================
-- 1. 역할 및 사용자 관리
-- ============================================

-- 역할 테이블
CREATE TABLE IF NOT EXISTS roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '역할 ID',
    role_name VARCHAR(50) NOT NULL UNIQUE COMMENT '역할명',
    description TEXT COMMENT '역할 설명',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    INDEX idx_role_name (role_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='역할';

-- 통합 사용자 테이블
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '사용자 ID',
    user_id VARCHAR(50) NOT NULL UNIQUE COMMENT '사용자 아이디 (로그인 ID)',
    password VARCHAR(255) NOT NULL COMMENT '암호화된 비밀번호',
    phone VARCHAR(20) COMMENT '전화번호',
    email VARCHAR(100) COMMENT '이메일',
    role_id BIGINT COMMENT '역할 ID (계약 시 할당)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
    is_deleted BIT(1) NOT NULL DEFAULT 0 COMMENT '삭제 여부 (BaseEntity)',
    profile_completed BIT(1) NOT NULL DEFAULT 0 COMMENT '프로필 완성 여부',
    profile_token VARCHAR(36) UNIQUE COMMENT '프로필 완성 토큰 (2단계 회원가입용)',
    profile_token_expires_at DATETIME COMMENT '프로필 토큰 만료 시간',
    refresh_token VARCHAR(500) COMMENT 'Refresh Token (JWT)',
    refresh_token_expires_at DATETIME COMMENT 'Refresh Token 만료 시간',
    site_id BIGINT COMMENT '소속 현장 ID (근로자/관리자만, 기업은 NULL)',
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE RESTRICT,
    INDEX idx_user_id (user_id),
    INDEX idx_email (email),
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자';

-- 근로자 테이블
CREATE TABLE IF NOT EXISTS employees (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '근로자 ID',
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    emp_name VARCHAR(50) NOT NULL COMMENT '근로자 이름',
    sub_phone VARCHAR(20) COMMENT '보조 전화번호',
    resident_num VARCHAR(20) COMMENT '주민등록번호 (암호화 권장)',
    emp_address VARCHAR(255) COMMENT '주소',
    emp_type VARCHAR(30) COMMENT '근로자 유형 (정규직, 계약직 등)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_emp_name (emp_name),
    INDEX idx_emp_type (emp_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='근로자';

-- 관리자 테이블
CREATE TABLE IF NOT EXISTS managers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '관리자 ID',
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    manager_name VARCHAR(50) NOT NULL COMMENT '관리자 이름',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_manager_name (manager_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='관리자';

-- 법인 테이블
CREATE TABLE IF NOT EXISTS corporations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '법인 ID',
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    corp_name VARCHAR(100) NOT NULL COMMENT '법인명',
    corp_address VARCHAR(255) COMMENT '법인 주소',
    corp_ceo_name VARCHAR(50) COMMENT '대표자명',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_corp_name (corp_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='법인';

-- ============================================
-- 2. 현장 관리
-- ============================================

-- 현장 테이블
CREATE TABLE IF NOT EXISTS sites (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '현장 ID',
    site_name VARCHAR(100) NOT NULL COMMENT '현장명',
    site_address VARCHAR(255) NOT NULL COMMENT '현장 주소',
    corporation_id BIGINT NOT NULL COMMENT '법인 ID',
    manager_id BIGINT NOT NULL COMMENT '관리자 ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
    FOREIGN KEY (corporation_id) REFERENCES corporations(id) ON DELETE CASCADE,
    FOREIGN KEY (manager_id) REFERENCES managers(id) ON DELETE CASCADE,
    INDEX idx_corporation_id (corporation_id),
    INDEX idx_manager_id (manager_id),
    INDEX idx_site_name (site_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='현장';

-- ============================================
-- 3. 계약 관리
-- ============================================

-- 계약서 테이블
CREATE TABLE IF NOT EXISTS contracts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '계약 ID',
    employee_id BIGINT NOT NULL COMMENT '근로자 ID',
    corporation_id BIGINT NOT NULL COMMENT '법인 ID',
    manager_id BIGINT NOT NULL COMMENT '관리자 ID',
    role VARCHAR(30) COMMENT '역할/직책',
    emp_type VARCHAR(30) NOT NULL COMMENT '근로자 유형 (DAILY, PERMANENT)',
    contract_state VARCHAR(30) NOT NULL DEFAULT 'DRAFT' COMMENT '계약 상태 (DRAFT, SIGNED, ACTIVE, TERMINATED)',
    employee_start_date DATE COMMENT '근무 시작일',
    employee_end_date DATE COMMENT '근무 종료일',
    written_at DATETIME COMMENT '작성 시각',
    corp_signed_at DATETIME COMMENT '법인 서명 시각',
    emp_signed_at DATETIME COMMENT '근로자 서명 시각',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    FOREIGN KEY (corporation_id) REFERENCES corporations(id) ON DELETE CASCADE,
    FOREIGN KEY (manager_id) REFERENCES managers(id) ON DELETE CASCADE,
    INDEX idx_employee_id (employee_id),
    INDEX idx_corporation_id (corporation_id),
    INDEX idx_manager_id (manager_id),
    INDEX idx_contract_state (contract_state),
    INDEX idx_emp_type (emp_type),
    INDEX idx_dates (employee_start_date, employee_end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='계약서';

-- 계약 상세 테이블
CREATE TABLE IF NOT EXISTS contract_details (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '계약 상세 ID',
    contract_id BIGINT NOT NULL UNIQUE COMMENT '계약 ID (1:1 관계)',
    corp_name VARCHAR(100) COMMENT '법인명 (스냅샷)',
    emp_name VARCHAR(50) COMMENT '근로자명 (스냅샷)',
    work_place VARCHAR(255) COMMENT '근무 장소',
    work_type VARCHAR(100) COMMENT '업무 내용',
    work_start_time TIME COMMENT '근무 시작 시간',
    work_end_time TIME COMMENT '근무 종료 시간',
    break_start_time TIME COMMENT '휴게 시작 시간',
    break_end_time TIME COMMENT '휴게 종료 시간',
    work_on_days VARCHAR(100) COMMENT '근무일',
    work_off_days VARCHAR(100) COMMENT '휴무일',
    work_pay DECIMAL(15,2) COMMENT '기본급',
    additional_hour_pay DECIMAL(15,2) COMMENT '시간외수당',
    additional_night_pay DECIMAL(15,2) COMMENT '야간수당',
    additional_holiday_pay DECIMAL(15,2) COMMENT '휴일수당',
    pay_day INT COMMENT '급여 지급일 (1~31)',
    pay_period VARCHAR(30) COMMENT '급여 지급 주기',
    pay_type VARCHAR(30) COMMENT '급여 지급 방식',
    is_eoi_applicable BOOLEAN DEFAULT FALSE COMMENT '고용보험 적용 여부',
    is_wci_applicable BOOLEAN DEFAULT FALSE COMMENT '산재보험 적용 여부',
    is_nps_applicable BOOLEAN DEFAULT FALSE COMMENT '국민연금 적용 여부',
    is_nhi_applicable BOOLEAN DEFAULT FALSE COMMENT '건강보험 적용 여부',
    corp_address VARCHAR(255) COMMENT '법인 주소 (스냅샷)',
    corp_ceo_name VARCHAR(50) COMMENT '대표자명 (스냅샷)',
    emp_address VARCHAR(255) COMMENT '근로자 주소 (스냅샷)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    FOREIGN KEY (contract_id) REFERENCES contracts(id) ON DELETE CASCADE,
    INDEX idx_contract_id (contract_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='계약 상세';

-- ============================================
-- 4. 근태 관리
-- ============================================

-- 근태 테이블
CREATE TABLE IF NOT EXISTS attendances (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '근태 ID',
    contract_id BIGINT NOT NULL COMMENT '계약 ID',
    employee_id BIGINT NOT NULL COMMENT '근로자 ID',
    site_id BIGINT NOT NULL COMMENT '현장 ID',
    search_date DATE NOT NULL COMMENT '근무일',
    emp_type VARCHAR(30) COMMENT '근로자 유형',
    emp_name VARCHAR(50) COMMENT '근로자명 (스냅샷)',
    resident_num VARCHAR(20) COMMENT '주민등록번호 (스냅샷)',
    attendance_status VARCHAR(20) NOT NULL DEFAULT 'PRESENT' COMMENT '근태 상태 (PRESENT, ABSENT, LATE, EARLY_LEAVE)',
    check_in_time DATETIME COMMENT '출근 시각',
    check_out_time DATETIME COMMENT '퇴근 시각',
    total_work_hour DECIMAL(6,2) COMMENT '총 근무 시간',
    night_work_hour DECIMAL(6,2) DEFAULT 0 COMMENT '야간 근무 시간',
    additional_work_hour DECIMAL(6,2) DEFAULT 0 COMMENT '연장 근무 시간',
    holiday_work_hour DECIMAL(6,2) DEFAULT 0 COMMENT '휴일 근무 시간',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
    FOREIGN KEY (contract_id) REFERENCES contracts(id) ON DELETE CASCADE,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    FOREIGN KEY (site_id) REFERENCES sites(id) ON DELETE CASCADE,
    INDEX idx_contract_id (contract_id),
    INDEX idx_employee_id (employee_id),
    INDEX idx_site_id (site_id),
    INDEX idx_search_date (search_date),
    INDEX idx_attendance_status (attendance_status),
    UNIQUE KEY unique_attendance (site_id, employee_id, search_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='근태';

-- ============================================
-- 5. 급여 관리
-- ============================================

-- 급여 테이블
CREATE TABLE IF NOT EXISTS payrolls (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '급여 ID',
    employee_id BIGINT NOT NULL COMMENT '근로자 ID',
    contract_id BIGINT NOT NULL COMMENT '계약 ID',
    corporation_id BIGINT NOT NULL COMMENT '법인 ID',
    search_date DATE NOT NULL COMMENT '급여 기준일',
    pay_due_date DATE COMMENT '급여 지급 예정일',
    emp_type VARCHAR(30) COMMENT '근로자 유형',
    emp_name VARCHAR(50) COMMENT '근로자명 (스냅샷)',
    resident_num VARCHAR(20) COMMENT '주민등록번호 (스냅샷)',
    total_work_hour DECIMAL(8,2) COMMENT '총 근무 시간',
    total_pay DECIMAL(15,2) NOT NULL COMMENT '총 급여',
    total_pay_by_day DECIMAL(15,2) COMMENT '일일 급여',
    none_tax_income DECIMAL(15,2) DEFAULT 0 COMMENT '비과세 소득',
    income_tax DECIMAL(15,2) DEFAULT 0 COMMENT '소득세',
    resident_tax DECIMAL(15,2) DEFAULT 0 COMMENT '주민세',
    pay_cycle VARCHAR(20) COMMENT '급여 주기',
    pay_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '지급 상태 (PENDING, PAID)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    FOREIGN KEY (contract_id) REFERENCES contracts(id) ON DELETE CASCADE,
    FOREIGN KEY (corporation_id) REFERENCES corporations(id) ON DELETE CASCADE,
    INDEX idx_employee_id (employee_id),
    INDEX idx_contract_id (contract_id),
    INDEX idx_corporation_id (corporation_id),
    INDEX idx_search_date (search_date),
    INDEX idx_pay_status (pay_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='급여';

-- ============================================
-- 6. 안전교육 관리
-- ============================================

-- 안전교육 문서 테이블
CREATE TABLE IF NOT EXISTS safety_docs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '안전교육 문서 ID',
    site_id BIGINT NOT NULL COMMENT '현장 ID',
    manager_id BIGINT NOT NULL COMMENT '관리자 ID',
    safetydoc_title VARCHAR(200) NOT NULL COMMENT '안전교육 제목',
    safetydoc_type VARCHAR(50) COMMENT '안전교육 유형',
    safetydoc_context TEXT COMMENT '안전교육 내용',
    safetydoc_employee_cnt INT DEFAULT 0 COMMENT '참석 인원 수',
    safetydoc_status VARCHAR(30) NOT NULL DEFAULT 'DRAFT' COMMENT '문서 상태 (DRAFT, PUBLISHED, COMPLETED)',
    safetydoc_created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '교육 생성 시각',
    safetydoc_updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '교육 수정 시각',
    FOREIGN KEY (site_id) REFERENCES sites(id) ON DELETE CASCADE,
    FOREIGN KEY (manager_id) REFERENCES managers(id) ON DELETE CASCADE,
    INDEX idx_site_id (site_id),
    INDEX idx_manager_id (manager_id),
    INDEX idx_safetydoc_status (safetydoc_status),
    INDEX idx_safetydoc_created_at (safetydoc_created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='안전교육 문서';

-- 안전교육 참석자 테이블
CREATE TABLE IF NOT EXISTS safety_doc_attendees (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '참석자 ID',
    safety_doc_id BIGINT NOT NULL COMMENT '안전교육 문서 ID',
    employee_id BIGINT NOT NULL COMMENT '근로자 ID',
    emp_name VARCHAR(50) COMMENT '근로자명 (스냅샷)',
    emp_type VARCHAR(30) COMMENT '근로자 유형',
    attendance_status VARCHAR(20) NOT NULL DEFAULT 'ATTENDED' COMMENT '참석 상태 (ATTENDED, ABSENT)',
    signed_at DATETIME COMMENT '서명 시각',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    FOREIGN KEY (safety_doc_id) REFERENCES safety_docs(id) ON DELETE CASCADE,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    INDEX idx_safety_doc_id (safety_doc_id),
    INDEX idx_employee_id (employee_id),
    INDEX idx_attendance_status (attendance_status),
    UNIQUE KEY unique_attendee (safety_doc_id, employee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='안전교육 참석자';

-- 안전교육 서명 로그 테이블
CREATE TABLE IF NOT EXISTS safety_sign_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '서명 로그 ID',
    attendee_id BIGINT NOT NULL COMMENT '참석자 ID',
    signer_type VARCHAR(20) NOT NULL COMMENT '서명자 유형 (EMPLOYEE, MANAGER)',
    signature_hash VARCHAR(255) COMMENT '서명 해시값',
    signature_image_url VARCHAR(255) COMMENT '서명 이미지 URL (S3)',
    signed_device VARCHAR(100) COMMENT '서명 기기 정보',
    signed_ip VARCHAR(45) COMMENT '서명 IP 주소',
    signed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '서명 시각',
    verified_at DATETIME COMMENT '검증 시각',
    verification_status VARCHAR(20) DEFAULT 'PENDING' COMMENT '검증 상태 (PENDING, VERIFIED, FAILED)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    FOREIGN KEY (attendee_id) REFERENCES safety_doc_attendees(id) ON DELETE CASCADE,
    INDEX idx_attendee_id (attendee_id),
    INDEX idx_signer_type (signer_type),
    INDEX idx_verification_status (verification_status),
    INDEX idx_signed_at (signed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='안전교육 서명 로그';

-- ============================================
-- 7. 작업일보 관리
-- ============================================

-- 작업일보 테이블
CREATE TABLE IF NOT EXISTS work_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '작업일보 ID',
    site_id BIGINT NOT NULL COMMENT '현장 ID',
    manager_id BIGINT NOT NULL COMMENT '관리자 ID',
    corporation_id BIGINT NOT NULL COMMENT '법인 ID',
    work_report_title VARCHAR(200) NOT NULL COMMENT '작업일보 제목',
    work_report_created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '작성 시각',
    work_report_started_at DATETIME COMMENT '작업 시작 시각',
    work_report_ended_at DATETIME COMMENT '작업 종료 시각',
    work_report_context TEXT COMMENT '작업 내용',
    work_section_name VARCHAR(100) COMMENT '작업 구역명',
    work_section_employee_num INT DEFAULT 0 COMMENT '투입 인원 수',
    work_report_status VARCHAR(30) NOT NULL DEFAULT 'DRAFT' COMMENT '작업일보 상태 (DRAFT, SUBMITTED, APPROVED)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
    FOREIGN KEY (site_id) REFERENCES sites(id) ON DELETE CASCADE,
    FOREIGN KEY (manager_id) REFERENCES managers(id) ON DELETE CASCADE,
    FOREIGN KEY (corporation_id) REFERENCES corporations(id) ON DELETE CASCADE,
    INDEX idx_site_id (site_id),
    INDEX idx_manager_id (manager_id),
    INDEX idx_corporation_id (corporation_id),
    INDEX idx_work_report_status (work_report_status),
    INDEX idx_work_report_created_at (work_report_created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='작업일보';

-- 작업일보 근로자 테이블
CREATE TABLE IF NOT EXISTS work_report_employees (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '작업일보 근로자 ID',
    work_report_id BIGINT NOT NULL COMMENT '작업일보 ID',
    employee_id BIGINT NOT NULL COMMENT '근로자 ID',
    emp_name VARCHAR(50) COMMENT '근로자명 (스냅샷)',
    emp_type VARCHAR(30) COMMENT '근로자 유형',
    work_hours DECIMAL(6,2) COMMENT '작업 시간',
    role_in_section VARCHAR(50) COMMENT '작업 구역 내 역할',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    FOREIGN KEY (work_report_id) REFERENCES work_reports(id) ON DELETE CASCADE,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    INDEX idx_work_report_id (work_report_id),
    INDEX idx_employee_id (employee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='작업일보 근로자';

-- 작업일보 자재 테이블
CREATE TABLE IF NOT EXISTS work_report_materials (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '작업일보 자재 ID',
    work_report_id BIGINT NOT NULL COMMENT '작업일보 ID',
    material_name VARCHAR(100) NOT NULL COMMENT '자재명',
    material_standard VARCHAR(100) COMMENT '규격',
    material_unit VARCHAR(50) COMMENT '단위',
    material_today DECIMAL(10,2) COMMENT '금일 사용량',
    material_sum DECIMAL(10,2) COMMENT '누적 사용량',
    note VARCHAR(255) COMMENT '비고',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    FOREIGN KEY (work_report_id) REFERENCES work_reports(id) ON DELETE CASCADE,
    INDEX idx_work_report_id (work_report_id),
    INDEX idx_material_name (material_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='작업일보 자재';

-- ============================================
-- 초기 데이터 삽입
-- ============================================

-- 기본 역할 데이터
INSERT INTO roles (role_name, description) VALUES
('ROLE_ADMIN', '시스템 관리자'),
('ROLE_MANAGER', '현장 관리자'),
('ROLE_EMPLOYEE', '근로자'),
('ROLE_CORPORATION', '법인 담당자')
ON DUPLICATE KEY UPDATE role_name = role_name;

-- 기본 관리자 계정
-- 비밀번호: admin123 (실제 운영 환경에서는 반드시 변경)
INSERT INTO users (user_id, password, phone, email, role_id)
SELECT 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z2EHdsDGF.fWVFR7A1tJDG3a', '010-0000-0000', 'admin@buildup.com', r.id
FROM roles r
WHERE r.role_name = 'ROLE_ADMIN'
ON DUPLICATE KEY UPDATE password = VALUES(password);

-- 기본 관리자 프로필
INSERT INTO managers (user_id, manager_name)
SELECT u.id, '시스템 관리자'
FROM users u
WHERE u.user_id = 'admin'
ON DUPLICATE KEY UPDATE manager_name = VALUES(manager_name);
