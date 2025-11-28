-- ============================================
-- EC2 프론트엔드 테스트용 데이터 (2025-11-18 기준)
-- ============================================
-- 목적: 출퇴근 시스템 프론트엔드 테스트
-- 실행 환경: AWS EC2 MySQL
-- ERD 기준: .claude/ERD.md
-- ============================================

USE buildup;

-- ============================================
-- 0. 기존 테스트 데이터 삭제 (역순으로 삭제하여 FK 제약 조건 위반 방지)
-- ============================================

-- contract_details 삭제
DELETE cd FROM contract_details cd
JOIN contracts c ON cd.contract_id = c.id
JOIN employees e ON c.employee_id = e.id
JOIN users u ON e.user_id = u.id
WHERE u.user_id LIKE 'test%';

-- contracts 삭제
DELETE c FROM contracts c
JOIN employees e ON c.employee_id = e.id
JOIN users u ON e.user_id = u.id
WHERE u.user_id LIKE 'test%';

-- employees 삭제
DELETE e FROM employees e
JOIN users u ON e.user_id = u.id
WHERE u.user_id LIKE 'test%';

-- sites 삭제 (corporation_id 참조하므로 corporations보다 먼저)
DELETE s FROM sites s
JOIN corporations c ON s.corporation_id = c.id
JOIN users u ON c.user_id = u.id
WHERE u.user_id LIKE 'test%';

-- managers 삭제
DELETE m FROM managers m
JOIN users u ON m.user_id = u.id
WHERE u.user_id LIKE 'test%';

-- corporations 삭제
DELETE corp FROM corporations corp
JOIN users u ON corp.user_id = u.id
WHERE u.user_id LIKE 'test%';

-- users 삭제 (마지막)
DELETE FROM users WHERE user_id LIKE 'test%';

-- Role ID 조회
SET @role_corp = (SELECT id FROM roles WHERE role_name = 'ROLE_CORPORATION');
SET @role_manager = (SELECT id FROM roles WHERE role_name = 'ROLE_MANAGER');
SET @role_emp = (SELECT id FROM roles WHERE role_name = 'ROLE_EMPLOYEE');

-- ============================================
-- 1. 법인 데이터 생성
-- ============================================
-- 비밀번호: Test123!@
INSERT INTO users (user_id, password, phone, email, role_id, profile_completed, created_at, updated_at)
VALUES ('testcorp', '$2a$10$d48wVDxrG/Paw.ULUn5.ae0IGrOu5415tyKW24RR5lKmqQcdgLdoy', '02-1234-5678', 'testcorp@buildup.com', @role_corp, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO corporations (user_id, corp_name, corp_address, corp_ceo_name, is_deleted, created_at, updated_at)
VALUES ((SELECT id FROM users WHERE user_id = 'testcorp'), '(주)테스트건설', '서울특별시 강남구 테헤란로 123', '김대표', 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- ============================================
-- 2. 관리자 데이터 생성
-- ============================================
-- 비밀번호: Test123!@
INSERT INTO users (user_id, password, phone, email, role_id, profile_completed, created_at, updated_at)
VALUES ('testmanager', '$2a$10$d48wVDxrG/Paw.ULUn5.ae0IGrOu5415tyKW24RR5lKmqQcdgLdoy', '010-9999-0001', 'testmanager@buildup.com', @role_manager, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO managers (user_id, manager_name, is_deleted, created_at, updated_at)
VALUES ((SELECT id FROM users WHERE user_id = 'testmanager'), '테스트관리자', 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- ============================================
-- 3. 현장 데이터 생성
-- ============================================
INSERT INTO sites (site_name, site_address, client_name, corporation_id, manager_id, manager_secret_key, is_deleted, created_at, updated_at)
VALUES (
    '테스트 건설현장',
    '서울특별시 강남구 역삼동 789',
    '테스트 발주처',
    (SELECT id FROM corporations WHERE corp_name = '(주)테스트건설'),
    (SELECT id FROM managers WHERE manager_name = '테스트관리자'),
    'TEST2025',
    0,
    NOW(),
    NOW()
)
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- ============================================
-- 4. 근로자 데이터 생성 (3명)
-- ============================================

-- 4-1. 상용직 근로자 (홍길동)
-- 비밀번호: Test123!@
INSERT INTO users (user_id, password, phone, email, role_id, profile_completed, created_at, updated_at)
VALUES ('testEmp01', '$2a$10$d48wVDxrG/Paw.ULUn5.ae0IGrOu5415tyKW24RR5lKmqQcdgLdoy', '010-1111-0001', 'emp01@test.com', @role_emp, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO employees (user_id, emp_name, sub_phone, resident_num, emp_address, emp_type, profile_image_url, is_deleted, created_at, updated_at)
VALUES (
    (SELECT id FROM users WHERE user_id = 'testEmp01'),
    '홍길동',
    '010-1111-0002',
    NULL,
    '서울시 강남구 역삼동 111',
    'PERMANENT',
    NULL,
    0,
    NOW(),
    NOW()
)
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 4-2. 일용직 근로자 1 (김철수)
-- 비밀번호: Test123!@
INSERT INTO users (user_id, password, phone, email, role_id, profile_completed, created_at, updated_at)
VALUES ('testEmp02', '$2a$10$d48wVDxrG/Paw.ULUn5.ae0IGrOu5415tyKW24RR5lKmqQcdgLdoy', '010-2222-0001', 'emp02@test.com', @role_emp, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO employees (user_id, emp_name, sub_phone, resident_num, emp_address, emp_type, profile_image_url, is_deleted, created_at, updated_at)
VALUES (
    (SELECT id FROM users WHERE user_id = 'testEmp02'),
    '김철수',
    '010-2222-0002',
    NULL,
    '서울시 송파구 잠실동 222',
    'DAILY',
    NULL,
    0,
    NOW(),
    NOW()
)
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 4-3. 일용직 근로자 2 (이영희)
-- 비밀번호: Test123!@
INSERT INTO users (user_id, password, phone, email, role_id, profile_completed, created_at, updated_at)
VALUES ('testEmp03', '$2a$10$d48wVDxrG/Paw.ULUn5.ae0IGrOu5415tyKW24RR5lKmqQcdgLdoy', '010-3333-0001', 'emp03@test.com', @role_emp, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO employees (user_id, emp_name, sub_phone, resident_num, emp_address, emp_type, profile_image_url, is_deleted, created_at, updated_at)
VALUES (
    (SELECT id FROM users WHERE user_id = 'testEmp03'),
    '이영희',
    '010-3333-0002',
    NULL,
    '서울시 서초구 방배동 333',
    'DAILY',
    NULL,
    0,
    NOW(),
    NOW()
)
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- ============================================
-- 5. 근로계약 데이터 생성 (현재 유효한 계약)
-- ============================================

-- 5-1. 상용직 근로자 계약 (2025-11-01 ~ 2026-10-31, 1년)
INSERT INTO contracts (employee_id, corporation_id, manager_id, role, emp_type, contract_state,
                      employee_start_date, employee_end_date, written_at, corp_signed_at, emp_signed_at,
                      is_deleted, created_at, updated_at)
VALUES (
    (SELECT id FROM employees WHERE emp_name = '홍길동'),
    (SELECT id FROM corporations WHERE corp_name = '(주)테스트건설'),
    (SELECT id FROM managers WHERE manager_name = '테스트관리자'),
    '현장 기술직',
    'PERMANENT',
    'FULLY_SIGNED',
    '2025-11-01',
    '2026-10-31',
    '2025-10-25 10:00:00',
    '2025-10-26 14:30:00',
    '2025-10-27 16:00:00',
    0,
    NOW(),
    NOW()
)
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 5-2. 일용직 근로자 1 계약 (2025-11-01 ~ 2026-01-31, 3개월)
INSERT INTO contracts (employee_id, corporation_id, manager_id, role, emp_type, contract_state,
                      employee_start_date, employee_end_date, written_at, corp_signed_at, emp_signed_at,
                      is_deleted, created_at, updated_at)
VALUES (
    (SELECT id FROM employees WHERE emp_name = '김철수'),
    (SELECT id FROM corporations WHERE corp_name = '(주)테스트건설'),
    (SELECT id FROM managers WHERE manager_name = '테스트관리자'),
    '일반 노무',
    'DAILY',
    'FULLY_SIGNED',
    '2025-11-01',
    '2026-01-31',
    '2025-10-25 10:00:00',
    '2025-10-26 14:30:00',
    '2025-10-27 16:00:00',
    0,
    NOW(),
    NOW()
)
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 5-3. 일용직 근로자 2 계약 (2025-11-01 ~ 2026-01-31, 3개월)
INSERT INTO contracts (employee_id, corporation_id, manager_id, role, emp_type, contract_state,
                      employee_start_date, employee_end_date, written_at, corp_signed_at, emp_signed_at,
                      is_deleted, created_at, updated_at)
VALUES (
    (SELECT id FROM employees WHERE emp_name = '이영희'),
    (SELECT id FROM corporations WHERE corp_name = '(주)테스트건설'),
    (SELECT id FROM managers WHERE manager_name = '테스트관리자'),
    '일반 노무',
    'DAILY',
    'FULLY_SIGNED',
    '2025-11-01',
    '2026-01-31',
    '2025-10-25 10:00:00',
    '2025-10-26 14:30:00',
    '2025-10-27 16:00:00',
    0,
    NOW(),
    NOW()
)
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- ============================================
-- 6. 계약 상세 데이터 생성
-- ============================================

-- 6-1. 상용직 계약 상세 (시급 15,000원, 월급제)
INSERT INTO contract_details (
    contract_id, corp_name, emp_name, work_place, work_type,
    work_start_time, work_end_time, break_start_time, break_end_time,
    work_on_days, work_off_days, work_pay,
    additional_hour_pay, additional_night_pay, additional_holiday_pay,
    pay_day, pay_period, pay_type,
    is_eoi_applicable, is_wci_applicable, is_nps_applicable, is_nhi_applicable,
    corp_address, corp_ceo_name, emp_address,
    is_deleted, created_at, updated_at
)
VALUES (
    (SELECT c.id FROM contracts c
     JOIN employees e ON c.employee_id = e.id
     WHERE e.emp_name = '홍길동' LIMIT 1),
    '(주)테스트건설',
    '홍길동',
    '테스트 건설현장',
    '건설 현장 관리',
    '09:00:00',
    '18:00:00',
    '12:00:00',
    '13:00:00',
    '월,화,수,목,금',
    '토,일',
    15000.00,
    7500.00,
    7500.00,
    15000.00,
    10,
    'MONTHLY',
    'TRANSFER',
    TRUE,
    TRUE,
    TRUE,
    TRUE,
    '서울특별시 강남구 테헤란로 123',
    '김대표',
    '서울시 강남구 역삼동 111',
    0,
    NOW(),
    NOW()
)
ON DUPLICATE KEY UPDATE contract_id = VALUES(contract_id);

-- 6-2. 일용직 계약 상세 1 (시급 12,000원, 일급제)
INSERT INTO contract_details (
    contract_id, corp_name, emp_name, work_place, work_type,
    work_start_time, work_end_time, break_start_time, break_end_time,
    work_on_days, work_off_days, work_pay,
    additional_hour_pay, additional_night_pay, additional_holiday_pay,
    pay_day, pay_period, pay_type,
    is_eoi_applicable, is_wci_applicable, is_nps_applicable, is_nhi_applicable,
    corp_address, corp_ceo_name, emp_address,
    is_deleted, created_at, updated_at
)
VALUES (
    (SELECT c.id FROM contracts c
     JOIN employees e ON c.employee_id = e.id
     WHERE e.emp_name = '김철수' LIMIT 1),
    '(주)테스트건설',
    '김철수',
    '테스트 건설현장',
    '일반 노무',
    '08:00:00',
    '17:00:00',
    '12:00:00',
    '13:00:00',
    '월,화,수,목,금',
    '토,일',
    12000.00,
    6000.00,
    6000.00,
    12000.00,
    5,
    'DAILY',
    'TRANSFER',
    FALSE,
    TRUE,
    FALSE,
    FALSE,
    '서울특별시 강남구 테헤란로 123',
    '김대표',
    '서울시 송파구 잠실동 222',
    0,
    NOW(),
    NOW()
)
ON DUPLICATE KEY UPDATE contract_id = VALUES(contract_id);

-- 6-3. 일용직 계약 상세 2 (시급 13,000원, 주급제)
INSERT INTO contract_details (
    contract_id, corp_name, emp_name, work_place, work_type,
    work_start_time, work_end_time, break_start_time, break_end_time,
    work_on_days, work_off_days, work_pay,
    additional_hour_pay, additional_night_pay, additional_holiday_pay,
    pay_day, pay_period, pay_type,
    is_eoi_applicable, is_wci_applicable, is_nps_applicable, is_nhi_applicable,
    corp_address, corp_ceo_name, emp_address,
    is_deleted, created_at, updated_at
)
VALUES (
    (SELECT c.id FROM contracts c
     JOIN employees e ON c.employee_id = e.id
     WHERE e.emp_name = '이영희' LIMIT 1),
    '(주)테스트건설',
    '이영희',
    '테스트 건설현장',
    '일반 노무',
    '08:00:00',
    '17:00:00',
    '12:00:00',
    '13:00:00',
    '월,화,수,목,금,토',
    '일',
    13000.00,
    6500.00,
    6500.00,
    13000.00,
    10,
    'WEEKLY',
    'TRANSFER',
    FALSE,
    TRUE,
    FALSE,
    FALSE,
    '서울특별시 강남구 테헤란로 123',
    '김대표',
    '서울시 서초구 방배동 333',
    0,
    NOW(),
    NOW()
)
ON DUPLICATE KEY UPDATE contract_id = VALUES(contract_id);

-- ============================================
-- 완료 메시지
-- ============================================
SELECT '✅ EC2 테스트 데이터 생성 완료!' AS Status;
SELECT '
📋 생성된 테스트 계정:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

🏢 법인 계정:
   - ID: testcorp
   - 비밀번호: Test123!@
   - 법인명: (주)테스트건설

👔 현장 관리자 계정 (로그인용):
   - ID: testmanager
   - 비밀번호: Test123!@
   - 이름: 테스트관리자
   - 전화번호: 010-9999-0001

🏗️ 현장 정보:
   - 현장명: 테스트 건설현장
   - 시크릿키: TEST2025

👷 근로자 계정 (출퇴근 테스트용):
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

1️⃣ 홍길동 (상용직)
   - ID: testEmp01
   - 비밀번호: Test123!@
   - 전화번호: 010-1111-0001 ⭐
   - 고용형태: PERMANENT
   - 출근시간: 09:00 ~ 18:00
   - 시급: 15,000원
   - 급여주기: 월급

2️⃣ 김철수 (일용직)
   - ID: testEmp02
   - 비밀번호: Test123!@
   - 전화번호: 010-2222-0001 ⭐
   - 고용형태: DAILY
   - 출근시간: 08:00 ~ 17:00
   - 시급: 12,000원
   - 급여주기: 일급

3️⃣ 이영희 (일용직)
   - ID: testEmp03
   - 비밀번호: Test123!@
   - 전화번호: 010-3333-0001 ⭐
   - 고용형태: DAILY
   - 출근시간: 08:00 ~ 17:00
   - 시급: 13,000원
   - 급여주기: 주급

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📱 출퇴근 테스트 방법:

1. 관리자 로그인:
   POST /api/v1/auth/login
   { "username": "testmanager", "password": "Test123!@" }

2. 근로자 출퇴근:
   POST /api/v1/attendance
   - 전화번호: 010-1111-0001 (홍길동, PERMANENT)
   - 전화번호: 010-2222-0001 (김철수, DAILY)
   - 전화번호: 010-3333-0001 (이영희, DAILY)
   - 얼굴 사진: 업로드 필요

⚠️ 주의:
- 계약 유효기간: 2025-11-01 ~ 2026-01-31 (또는 2026-10-31)
- 얼굴 이미지는 최초 출퇴근 시 등록 필요
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
' AS Info;
