-- Build-Up 인증/인가, 현장, 근태, 얼굴인식 테스트 데이터
-- 담당자: 김세원
-- 기능: 인증/인가, 근태 관리, 현장 추가, 얼굴인식 기반 출퇴근
-- 모든 계정 비밀번호: Admin123!@

USE buildup;

-- ============================================
-- 1. 테스트용 기업(Corporation) 생성
-- ============================================
-- 비밀번호: Admin123!@
INSERT INTO users (user_id, password, role_id, phone, email, secret_key, profile_completed, is_deleted, created_at, updated_at)
SELECT 'testcorp', '$2a$10$d48wVDxrG/Paw.ULUn5.ae0IGrOu5415tyKW24RR5lKmqQcdgLdoy', r.id, '02-1234-5678', 'corp@test.com', NULL, b'1', b'0', NOW(), NOW()
FROM roles r
WHERE r.role_name = 'ROLE_CORPORATION'
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id);

INSERT INTO corporations (user_id, corp_name, corp_address, corp_ceo_name, corp_phone, business_license_number, is_deleted, created_at, updated_at)
SELECT u.id, '테스트건설(주)', '서울특별시 강남구 테헤란로 123', '김대표', '02-1234-5678', '123-45-67890', b'0', NOW(), NOW()
FROM users u
WHERE u.user_id = 'testcorp'
ON DUPLICATE KEY UPDATE corp_name = VALUES(corp_name);

-- ============================================
-- 2. 테스트용 현장 관리자(Manager) 생성
-- ============================================
-- 관리자 1: manager1
-- 비밀번호: Admin123!@
INSERT INTO users (user_id, password, role_id, phone, email, secret_key, profile_completed, is_deleted, created_at, updated_at)
SELECT 'manager1', '$2a$10$d48wVDxrG/Paw.ULUn5.ae0IGrOu5415tyKW24RR5lKmqQcdgLdoy', r.id, '010-1111-2222', 'manager1@test.com', NULL, b'1', b'0', NOW(), NOW()
FROM roles r
WHERE r.role_name = 'ROLE_MANAGER'
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id);

INSERT INTO managers (user_id, manager_name, is_deleted, created_at, updated_at)
SELECT u.id, '김관리', b'0', NOW(), NOW()
FROM users u
WHERE u.user_id = 'manager1'
ON DUPLICATE KEY UPDATE manager_name = VALUES(manager_name);

-- 관리자 2: manager2
-- 비밀번호: Admin123!@
INSERT INTO users (user_id, password, role_id, phone, email, secret_key, profile_completed, is_deleted, created_at, updated_at)
SELECT 'manager2', '$2a$10$d48wVDxrG/Paw.ULUn5.ae0IGrOu5415tyKW24RR5lKmqQcdgLdoy', r.id, '010-2222-3333', 'manager2@test.com', NULL, b'1', b'0', NOW(), NOW()
FROM roles r
WHERE r.role_name = 'ROLE_MANAGER'
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id);

INSERT INTO managers (user_id, manager_name, is_deleted, created_at, updated_at)
SELECT u.id, '박관리', b'0', NOW(), NOW()
FROM users u
WHERE u.user_id = 'manager2'
ON DUPLICATE KEY UPDATE manager_name = VALUES(manager_name);

-- ============================================
-- 3. 테스트용 현장(Site) 생성
-- ============================================
-- 현장 1: 역삼 오피스 신축공사 (employee1, employee2 소속)
-- 관리자 시크릿키: SITE-1-ABC-1234
-- 근로자 시크릿키: SITE-1-DEF-5678
INSERT INTO sites (
    site_name,
    site_address,
    client_name,
    start_date,
    end_date,
    corporation_id,
    manager_id,
    manager_secret_key,
    employee_secret_key,
    secret_key_expires_at,
    is_deleted,
    created_at,
    updated_at
)
SELECT
    '역삼 오피스 신축공사',
    '서울특별시 강남구 역삼동 123-45',
    '대한주택공사',
    '2025-01-01',
    '2025-12-31',
    c.id,
    m.id,
    'SITE-1-ABC-1234',
    'SITE-1-DEF-5678',
    '2025-12-31 23:59:59',
    b'0',
    NOW(),
    NOW()
FROM corporations c
JOIN users cu ON c.user_id = cu.id
JOIN managers m ON m.user_id = (SELECT id FROM users WHERE user_id = 'manager1')
WHERE cu.user_id = 'testcorp'
ON DUPLICATE KEY UPDATE site_name = VALUES(site_name);

-- 현장 2: 서초 오피스텔 리모델링 (employee3 소속)
-- 관리자 시크릿키: SITE-2-GHI-9012
-- 근로자 시크릿키: SITE-2-JKL-3456
INSERT INTO sites (
    site_name,
    site_address,
    client_name,
    start_date,
    end_date,
    corporation_id,
    manager_id,
    manager_secret_key,
    employee_secret_key,
    secret_key_expires_at,
    is_deleted,
    created_at,
    updated_at
)
SELECT
    '서초 오피스텔 리모델링',
    '서울특별시 서초구 서초동 456-78',
    '서초건설',
    '2025-02-01',
    '2025-06-30',
    c.id,
    m.id,
    'SITE-2-GHI-9012',
    'SITE-2-JKL-3456',
    '2025-06-30 23:59:59',
    b'0',
    NOW(),
    NOW()
FROM corporations c
JOIN users cu ON c.user_id = cu.id
JOIN managers m ON m.user_id = (SELECT id FROM users WHERE user_id = 'manager2')
WHERE cu.user_id = 'testcorp'
ON DUPLICATE KEY UPDATE site_name = VALUES(site_name);

-- ============================================
-- 4. 테스트용 근로자(Employee) 생성 (얼굴인식 포함)
-- ============================================
-- 근로자 1: employee1 (일용직, 프로필 완성, 얼굴 미등록)
-- 비밀번호: Admin123!@
-- 회원가입 완료, 얼굴 등록 API 테스트용
-- 소속: 역삼 오피스 신축공사 (현장 1)
INSERT INTO users (user_id, password, role_id, phone, email, secret_key, profile_completed, is_deleted, created_at, updated_at)
SELECT 'employee1', '$2a$10$d48wVDxrG/Paw.ULUn5.ae0IGrOu5415tyKW24RR5lKmqQcdgLdoy', r.id, '010-3333-4444', 'emp1@test.com', NULL, b'1', b'0', NOW(), NOW()
FROM roles r
WHERE r.role_name = 'ROLE_EMPLOYEE'
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id);

INSERT INTO employees (user_id, emp_name, sub_phone, resident_num, emp_address, emp_type, profile_image_url, is_deleted, created_at, updated_at)
SELECT
    u.id,
    '김근로',
    '010-3333-5555',
    NULL,  -- 주민번호는 JPA Converter 암호화 문제로 NULL 처리 (필요시 API로 입력)
    '서울특별시 강동구 천호동 111-22',
    'DAILY',
    NULL,  -- 얼굴 등록 API로 입력 필요
    b'0',
    NOW(),
    NOW()
FROM users u
WHERE u.user_id = 'employee1'
ON DUPLICATE KEY UPDATE emp_name = VALUES(emp_name);

-- 근로자 2: employee2 (상용직, 프로필 완성, 얼굴 미등록)
-- 비밀번호: Admin123!@
-- 회원가입 완료, 얼굴 등록 API 테스트용
-- 소속: 역삼 오피스 신축공사 (현장 1)
INSERT INTO users (user_id, password, role_id, phone, email, secret_key, profile_completed, is_deleted, created_at, updated_at)
SELECT 'employee2', '$2a$10$d48wVDxrG/Paw.ULUn5.ae0IGrOu5415tyKW24RR5lKmqQcdgLdoy', r.id, '010-4444-5555', 'emp2@test.com', NULL, b'1', b'0', NOW(), NOW()
FROM roles r
WHERE r.role_name = 'ROLE_EMPLOYEE'
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id);

INSERT INTO employees (user_id, emp_name, sub_phone, resident_num, emp_address, emp_type, profile_image_url, is_deleted, created_at, updated_at)
SELECT
    u.id,
    '이노동',
    '010-4444-6666',
    NULL,  -- 주민번호는 JPA Converter 암호화 문제로 NULL 처리 (필요시 API로 입력)
    '서울특별시 송파구 잠실동 222-33',
    'PERMANENT',
    NULL,  -- 얼굴 등록 API로 입력 필요
    b'0',
    NOW(),
    NOW()
FROM users u
WHERE u.user_id = 'employee2'
ON DUPLICATE KEY UPDATE emp_name = VALUES(emp_name);

-- 근로자 3: employee3 (일용직, 프로필 완성, 얼굴 미등록)
-- 비밀번호: Admin123!@
-- 회원가입 완료, 얼굴 등록 API 테스트용
-- 소속: 서초 오피스텔 리모델링 (현장 2)
INSERT INTO users (user_id, password, role_id, phone, email, secret_key, profile_completed, is_deleted, created_at, updated_at)
SELECT 'employee3', '$2a$10$d48wVDxrG/Paw.ULUn5.ae0IGrOu5415tyKW24RR5lKmqQcdgLdoy', r.id, '010-5555-6666', 'emp3@test.com', NULL, b'1', b'0', NOW(), NOW()
FROM roles r
WHERE r.role_name = 'ROLE_EMPLOYEE'
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id);

INSERT INTO employees (user_id, emp_name, sub_phone, resident_num, emp_address, emp_type, profile_image_url, is_deleted, created_at, updated_at)
SELECT
    u.id,
    '박현장',
    '010-5555-7777',
    NULL,  -- 주민번호는 JPA Converter 암호화 문제로 NULL 처리 (필요시 API로 입력)
    '경기도 성남시 분당구 정자동 333-44',
    'DAILY',
    NULL,  -- 얼굴 등록 API로 입력 필요
    b'0',
    NOW(),
    NOW()
FROM users u
WHERE u.user_id = 'employee3'
ON DUPLICATE KEY UPDATE emp_name = VALUES(emp_name);

-- ============================================
-- 5. 테스트용 근로계약서(Contract) 생성
-- ============================================
-- Employee1 (김근로) - 일용직 계약
INSERT INTO contracts (
    employee_id,
    corporation_id,
    manager_id,
    role,
    emp_type,
    contract_state,
    employee_start_date,
    employee_end_date,
    written_at,
    corp_signed_at,
    employee_signed_at,
    is_deleted,
    created_at,
    updated_at
)
SELECT
    e.id,
    c.id,
    m.id,
    '일반 근로자',
    'DAILY',
    'FULLY_SIGNED',
    '2025-01-01',
    '2025-12-31',
    '2024-12-25 10:00:00',
    '2024-12-26 14:00:00',
    '2024-12-27 09:00:00',
    b'0',
    NOW(),
    NOW()
FROM employees e
JOIN users eu ON e.user_id = eu.id
JOIN corporations c ON c.user_id = (SELECT id FROM users WHERE user_id = 'testcorp')
JOIN managers m ON m.user_id = (SELECT id FROM users WHERE user_id = 'manager1')
WHERE eu.user_id = 'employee1'
ON DUPLICATE KEY UPDATE employee_id = VALUES(employee_id);

-- Employee2 (이노동) - 상용직 계약
INSERT INTO contracts (
    employee_id,
    corporation_id,
    manager_id,
    role,
    emp_type,
    contract_state,
    employee_start_date,
    employee_end_date,
    written_at,
    corp_signed_at,
    employee_signed_at,
    is_deleted,
    created_at,
    updated_at
)
SELECT
    e.id,
    c.id,
    m.id,
    '일반 근로자',
    'PERMANENT',
    'FULLY_SIGNED',
    '2025-01-01',
    NULL,  -- 상용직은 종료일 없음
    '2024-12-25 11:00:00',
    '2024-12-26 15:00:00',
    '2024-12-27 10:00:00',
    b'0',
    NOW(),
    NOW()
FROM employees e
JOIN users eu ON e.user_id = eu.id
JOIN corporations c ON c.user_id = (SELECT id FROM users WHERE user_id = 'testcorp')
JOIN managers m ON m.user_id = (SELECT id FROM users WHERE user_id = 'manager1')
WHERE eu.user_id = 'employee2'
ON DUPLICATE KEY UPDATE employee_id = VALUES(employee_id);

-- Employee3 (박현장) - 일용직 계약
INSERT INTO contracts (
    employee_id,
    corporation_id,
    manager_id,
    role,
    emp_type,
    contract_state,
    employee_start_date,
    employee_end_date,
    written_at,
    corp_signed_at,
    employee_signed_at,
    is_deleted,
    created_at,
    updated_at
)
SELECT
    e.id,
    c.id,
    m.id,
    '일반 근로자',
    'DAILY',
    'FULLY_SIGNED',
    '2025-01-15',
    '2025-06-30',
    '2025-01-10 10:00:00',
    '2025-01-11 14:00:00',
    '2025-01-12 09:00:00',
    b'0',
    NOW(),
    NOW()
FROM employees e
JOIN users eu ON e.user_id = eu.id
JOIN corporations c ON c.user_id = (SELECT id FROM users WHERE user_id = 'testcorp')
JOIN managers m ON m.user_id = (SELECT id FROM users WHERE user_id = 'manager2')
WHERE eu.user_id = 'employee3'
ON DUPLICATE KEY UPDATE employee_id = VALUES(employee_id);

-- ============================================
-- 6. 테스트용 계약 상세(ContractDetail) 생성
-- ============================================
-- Employee1 계약 상세
INSERT INTO contract_details (
    contract_id,
    corp_name,
    emp_name,
    corp_address,
    corp_ceo_name,
    emp_address,
    work_place,
    work_type,
    work_start_time,
    work_end_time,
    break_time,
    work_days,
    holidays,
    pay_type,
    base_pay,
    overtime_pay_rate,
    night_pay_rate,
    holiday_pay_rate,
    total_pay,
    pay_period,
    pay_day,
    pay_method,
    four_insurance,
    etc_note,
    is_deleted,
    created_at,
    updated_at
)
SELECT
    ct.id,
    '테스트건설(주)',
    '김근로',
    '서울특별시 강남구 테헤란로 123',
    '김대표',
    '서울특별시 강동구 천호동 111-22',
    '역삼 오피스 신축공사',
    '건설 일용직',
    '08:00:00',
    '18:00:00',
    60,
    '월,화,수,목,금,토',
    '일요일, 법정공휴일',
    'HOURLY',
    15000,
    1.5,
    1.5,
    2.0,
    150000,
    'DAILY',
    '당일 지급',
    '현금',
    '국민연금, 건강보험, 고용보험, 산재보험',
    '일용직 근로자',
    b'0',
    NOW(),
    NOW()
FROM contracts ct
JOIN employees e ON ct.employee_id = e.id
JOIN users u ON e.user_id = u.id
WHERE u.user_id = 'employee1'
ON DUPLICATE KEY UPDATE contract_id = VALUES(contract_id);

-- Employee2 계약 상세
INSERT INTO contract_details (
    contract_id,
    corp_name,
    emp_name,
    corp_address,
    corp_ceo_name,
    emp_address,
    work_place,
    work_type,
    work_start_time,
    work_end_time,
    break_time,
    work_days,
    holidays,
    pay_type,
    base_pay,
    overtime_pay_rate,
    night_pay_rate,
    holiday_pay_rate,
    total_pay,
    pay_period,
    pay_day,
    pay_method,
    four_insurance,
    etc_note,
    is_deleted,
    created_at,
    updated_at
)
SELECT
    ct.id,
    '테스트건설(주)',
    '이노동',
    '서울특별시 강남구 테헤란로 123',
    '김대표',
    '서울특별시 송파구 잠실동 222-33',
    '역삼 오피스 신축공사',
    '건설 상용직',
    '08:00:00',
    '18:00:00',
    60,
    '월,화,수,목,금,토',
    '일요일, 법정공휴일',
    'MONTHLY',
    3500000,
    1.5,
    1.5,
    2.0,
    3500000,
    'MONTHLY',
    '매월 25일',
    '계좌이체',
    '국민연금, 건강보험, 고용보험, 산재보험',
    '상용직 근로자',
    b'0',
    NOW(),
    NOW()
FROM contracts ct
JOIN employees e ON ct.employee_id = e.id
JOIN users u ON e.user_id = u.id
WHERE u.user_id = 'employee2'
ON DUPLICATE KEY UPDATE contract_id = VALUES(contract_id);

-- Employee3 계약 상세
INSERT INTO contract_details (
    contract_id,
    corp_name,
    emp_name,
    corp_address,
    corp_ceo_name,
    emp_address,
    work_place,
    work_type,
    work_start_time,
    work_end_time,
    break_time,
    work_days,
    holidays,
    pay_type,
    base_pay,
    overtime_pay_rate,
    night_pay_rate,
    holiday_pay_rate,
    total_pay,
    pay_period,
    pay_day,
    pay_method,
    four_insurance,
    etc_note,
    is_deleted,
    created_at,
    updated_at
)
SELECT
    ct.id,
    '테스트건설(주)',
    '박현장',
    '서울특별시 강남구 테헤란로 123',
    '김대표',
    '경기도 성남시 분당구 정자동 333-44',
    '서초 오피스텔 리모델링',
    '건설 일용직',
    '08:00:00',
    '22:00:00',  -- 야간근무
    60,
    '월,화,수,목,금,토',
    '일요일, 법정공휴일',
    'HOURLY',
    16000,
    1.5,
    1.5,
    2.0,
    160000,
    'DAILY',
    '당일 지급',
    '현금',
    '국민연금, 건강보험, 고용보험, 산재보험',
    '야간근무 가능 일용직',
    b'0',
    NOW(),
    NOW()
FROM contracts ct
JOIN employees e ON ct.employee_id = e.id
JOIN users u ON e.user_id = u.id
WHERE u.user_id = 'employee3'
ON DUPLICATE KEY UPDATE contract_id = VALUES(contract_id);

-- ============================================
-- 7. 테스트용 근태(Attendance) 데이터 생성
-- ============================================
-- 이제 실제 contract_id를 사용합니다

-- 2025-01-15 근태 데이터 (정상 출근)
INSERT INTO attendances (
    contract_id,
    employee_id,
    site_id,
    search_date,
    emp_type,
    emp_name,
    resident_num,
    attendance_status,
    check_in_time,
    check_out_time,
    total_work_hour,
    night_work_hour,
    additional_work_hour,
    holiday_work_hour,
    is_late,
    is_deleted,
    created_at,
    updated_at
)
SELECT
    ct.id,
    e.id,
    s.id,
    '2025-01-15',
    e.emp_type,
    e.emp_name,
    e.resident_num,
    'PRESENT',
    '2025-01-15 08:00:00',
    '2025-01-15 18:00:00',
    10.00,
    0.00,
    0.00,
    0.00,
    b'0',
    b'0',
    NOW(),
    NOW()
FROM employees e
JOIN users u ON e.user_id = u.id
JOIN sites s ON s.site_name = '역삼 오피스 신축공사'
JOIN contracts ct ON ct.employee_id = e.id
WHERE u.user_id = 'employee1'
ON DUPLICATE KEY UPDATE search_date = VALUES(search_date);

-- 2025-01-15 근태 데이터 (지각)
INSERT INTO attendances (
    contract_id,
    employee_id,
    site_id,
    search_date,
    emp_type,
    emp_name,
    resident_num,
    attendance_status,
    check_in_time,
    check_out_time,
    total_work_hour,
    night_work_hour,
    additional_work_hour,
    holiday_work_hour,
    is_late,
    is_deleted,
    created_at,
    updated_at
)
SELECT
    ct.id,
    e.id,
    s.id,
    '2025-01-15',
    e.emp_type,
    e.emp_name,
    e.resident_num,
    'PRESENT',
    '2025-01-15 08:15:00',  -- 출근시간 08:00 기준 +15분 지각
    '2025-01-15 18:00:00',
    9.75,
    0.00,
    0.00,
    0.00,
    b'1',  -- 지각!
    b'0',
    NOW(),
    NOW()
FROM employees e
JOIN users u ON e.user_id = u.id
JOIN sites s ON s.site_name = '역삼 오피스 신축공사'
JOIN contracts ct ON ct.employee_id = e.id
WHERE u.user_id = 'employee2'
ON DUPLICATE KEY UPDATE search_date = VALUES(search_date);

-- 2025-01-16 근태 데이터 (야간 근무)
INSERT INTO attendances (
    contract_id,
    employee_id,
    site_id,
    search_date,
    emp_type,
    emp_name,
    resident_num,
    attendance_status,
    check_in_time,
    check_out_time,
    total_work_hour,
    night_work_hour,
    additional_work_hour,
    holiday_work_hour,
    is_late,
    is_deleted,
    created_at,
    updated_at
)
SELECT
    ct.id,
    e.id,
    s.id,
    '2025-01-16',
    e.emp_type,
    e.emp_name,
    e.resident_num,
    'PRESENT',
    '2025-01-16 08:00:00',
    '2025-01-16 22:00:00',  -- 14시간 근무 (야간 포함)
    14.00,
    4.00,  -- 야간 근로 (22시~06시)
    4.00,  -- 연장 근로
    0.00,
    b'0',
    b'0',
    NOW(),
    NOW()
FROM employees e
JOIN users u ON e.user_id = u.id
JOIN sites s ON s.site_name = '서초 오피스텔 리모델링'
JOIN contracts ct ON ct.employee_id = e.id
WHERE u.user_id = 'employee3'
ON DUPLICATE KEY UPDATE search_date = VALUES(search_date);

-- ============================================
-- 완료 메시지
-- ============================================
SELECT '✅ 인증/인가, 현장, 근태, 얼굴인식 테스트 데이터 삽입 완료!' AS Status;
SELECT '' AS '';
SELECT '📋 테스트 계정 정보 (모든 비밀번호: Admin123!@)' AS Info;
SELECT '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━' AS '';
SELECT '🏢 기업: testcorp / Admin123!@' AS Corporation;
SELECT '👤 관리자1: manager1 / Admin123!@' AS Manager1;
SELECT '👤 관리자2: manager2 / Admin123!@' AS Manager2;
SELECT '👷 근로자1: employee1 / Admin123!@ (일용직, 현장1, 얼굴 미등록)' AS Employee1;
SELECT '👷 근로자2: employee2 / Admin123!@ (상용직, 현장1, 얼굴 미등록)' AS Employee2;
SELECT '👷 근로자3: employee3 / Admin123!@ (일용직, 현장2, 얼굴 미등록)' AS Employee3;
SELECT '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━' AS '';
SELECT '🏗️ 현장1: 역삼 오피스 신축공사 (employee1, employee2)' AS Site1;
SELECT '   ├─ Manager Key: SITE-1-ABC-1234' AS Site1_Manager_Key;
SELECT '   └─ Employee Key: SITE-1-DEF-5678' AS Site1_Employee_Key;
SELECT '🏗️ 현장2: 서초 오피스텔 리모델링 (employee3)' AS Site2;
SELECT '   ├─ Manager Key: SITE-2-GHI-9012' AS Site2_Manager_Key;
SELECT '   └─ Employee Key: SITE-2-JKL-3456' AS Site2_Employee_Key;
SELECT '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━' AS '';
SELECT '⚠️  주의: 주민번호는 암호화 문제로 NULL 처리됨 (필요시 API로 입력)' AS Warning;
