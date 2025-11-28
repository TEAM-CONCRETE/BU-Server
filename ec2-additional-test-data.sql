-- ============================================
-- EC2 추가 테스트 데이터 (기업 + 현장 + 근로자 + 급여)
-- ============================================
-- 목적: 기업 관리자 로그인 시 현장 목록 조회 테스트
-- 비밀번호: Admin123!@ (모든 계정 동일)
-- 해시값: $2a$10$d48wVDxrG/Paw.ULUn5.ae0IGrOu5415tyKW24RR5lKmqQcdgLdoy
-- ============================================

USE buildup;

-- Role ID 조회
SET @role_corp = (SELECT id FROM roles WHERE role_name = 'ROLE_CORPORATION');
SET @role_manager = (SELECT id FROM roles WHERE role_name = 'ROLE_MANAGER');
SET @role_emp = (SELECT id FROM roles WHERE role_name = 'ROLE_EMPLOYEE');
SET @pw_hash = '$2a$10$d48wVDxrG/Paw.ULUn5.ae0IGrOu5415tyKW24RR5lKmqQcdgLdoy';

-- ============================================
-- 1. 기업 계정 2개 추가
-- ============================================

-- 기업 1: (주)한강건설 - 여러 현장 보유
INSERT INTO users (user_id, password, phone, email, role_id, profile_completed, created_at, updated_at)
VALUES ('hangangcorp', @pw_hash, '02-555-1000', 'hangang@buildup.com', @role_corp, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO corporations (user_id, corp_name, corp_address, corp_ceo_name, is_deleted, created_at, updated_at)
VALUES ((SELECT id FROM users WHERE user_id = 'hangangcorp'), '(주)한강건설', '서울특별시 영등포구 여의대로 108', '박한강', 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 기업 2: (주)남산토건 - 단일 현장
INSERT INTO users (user_id, password, phone, email, role_id, profile_completed, created_at, updated_at)
VALUES ('namsancorp', @pw_hash, '02-777-2000', 'namsan@buildup.com', @role_corp, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO corporations (user_id, corp_name, corp_address, corp_ceo_name, is_deleted, created_at, updated_at)
VALUES ((SELECT id FROM users WHERE user_id = 'namsancorp'), '(주)남산토건', '서울특별시 중구 남산동 100', '김남산', 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- ============================================
-- 2. 현장 관리자 추가 (한강건설 소속 3명, 남산토건 소속 1명)
-- ============================================

-- 한강건설 현장관리자 1 (강남현장)
INSERT INTO users (user_id, password, phone, email, role_id, profile_completed, created_at, updated_at)
VALUES ('hg_manager01', @pw_hash, '010-5501-0001', 'hg_mgr01@buildup.com', @role_manager, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO managers (user_id, manager_name, is_deleted, created_at, updated_at)
VALUES ((SELECT id FROM users WHERE user_id = 'hg_manager01'), '이강남', 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 한강건설 현장관리자 2 (송파현장)
INSERT INTO users (user_id, password, phone, email, role_id, profile_completed, created_at, updated_at)
VALUES ('hg_manager02', @pw_hash, '010-5502-0001', 'hg_mgr02@buildup.com', @role_manager, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO managers (user_id, manager_name, is_deleted, created_at, updated_at)
VALUES ((SELECT id FROM users WHERE user_id = 'hg_manager02'), '박송파', 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 한강건설 현장관리자 3 (마포현장)
INSERT INTO users (user_id, password, phone, email, role_id, profile_completed, created_at, updated_at)
VALUES ('hg_manager03', @pw_hash, '010-5503-0001', 'hg_mgr03@buildup.com', @role_manager, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO managers (user_id, manager_name, is_deleted, created_at, updated_at)
VALUES ((SELECT id FROM users WHERE user_id = 'hg_manager03'), '최마포', 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 남산토건 현장관리자 1
INSERT INTO users (user_id, password, phone, email, role_id, profile_completed, created_at, updated_at)
VALUES ('ns_manager01', @pw_hash, '010-7701-0001', 'ns_mgr01@buildup.com', @role_manager, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO managers (user_id, manager_name, is_deleted, created_at, updated_at)
VALUES ((SELECT id FROM users WHERE user_id = 'ns_manager01'), '정종로', 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- ============================================
-- 3. 현장 추가 (한강건설 3개, 남산토건 1개)
-- ============================================

-- 한강건설 현장 1: 강남 오피스텔 신축
INSERT INTO sites (site_name, site_address, client_name, start_date, end_date, corporation_id, manager_id, manager_secret_key, employee_secret_key, is_deleted, created_at, updated_at)
VALUES (
    '강남 오피스텔 신축현장',
    '서울특별시 강남구 역삼동 123-45',
    '강남디벨로퍼(주)',
    '2025-01-01',
    '2026-12-31',
    (SELECT id FROM corporations WHERE corp_name = '(주)한강건설'),
    (SELECT id FROM managers WHERE manager_name = '이강남'),
    'CONC-HG01-MGR-2025',
    'CONC-HG01-EMP-2025',
    0, NOW(), NOW()
)
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 한강건설 현장 2: 송파 아파트 리모델링
INSERT INTO sites (site_name, site_address, client_name, start_date, end_date, corporation_id, manager_id, manager_secret_key, employee_secret_key, is_deleted, created_at, updated_at)
VALUES (
    '송파 아파트 리모델링',
    '서울특별시 송파구 잠실동 456-78',
    '송파주택조합',
    '2025-03-01',
    '2025-12-31',
    (SELECT id FROM corporations WHERE corp_name = '(주)한강건설'),
    (SELECT id FROM managers WHERE manager_name = '박송파'),
    'CONC-HG02-MGR-2025',
    'CONC-HG02-EMP-2025',
    0, NOW(), NOW()
)
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 한강건설 현장 3: 마포 상가 증축
INSERT INTO sites (site_name, site_address, client_name, start_date, end_date, corporation_id, manager_id, manager_secret_key, employee_secret_key, is_deleted, created_at, updated_at)
VALUES (
    '마포 상가 증축공사',
    '서울특별시 마포구 합정동 789-12',
    '마포상가번영회',
    '2025-06-01',
    '2025-11-30',
    (SELECT id FROM corporations WHERE corp_name = '(주)한강건설'),
    (SELECT id FROM managers WHERE manager_name = '최마포'),
    'CONC-HG03-MGR-2025',
    'CONC-HG03-EMP-2025',
    0, NOW(), NOW()
)
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 남산토건 현장 1: 종로 빌딩 보수
INSERT INTO sites (site_name, site_address, client_name, start_date, end_date, corporation_id, manager_id, manager_secret_key, employee_secret_key, is_deleted, created_at, updated_at)
VALUES (
    '종로 빌딩 보수공사',
    '서울특별시 종로구 종로3가 100',
    '종로빌딩관리(주)',
    '2025-04-01',
    '2025-09-30',
    (SELECT id FROM corporations WHERE corp_name = '(주)남산토건'),
    (SELECT id FROM managers WHERE manager_name = '정종로'),
    'CONC-NS01-MGR-2025',
    'CONC-NS01-EMP-2025',
    0, NOW(), NOW()
)
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- ============================================
-- 4. 근로자 추가 (각 현장별 2~3명)
-- ============================================

-- === 강남 오피스텔 현장 근로자 (3명) ===

-- 근로자 GN-01
INSERT INTO users (user_id, password, phone, email, role_id, profile_completed, created_at, updated_at)
VALUES ('hg_emp_gn01', @pw_hash, '010-6601-0001', 'gn_emp01@test.com', @role_emp, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO employees (user_id, emp_name, sub_phone, emp_address, emp_type, is_deleted, created_at, updated_at)
VALUES ((SELECT id FROM users WHERE user_id = 'hg_emp_gn01'), '강태공', '010-6601-0002', '서울시 강남구 대치동 100', 'PERMANENT', 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 근로자 GN-02
INSERT INTO users (user_id, password, phone, email, role_id, profile_completed, created_at, updated_at)
VALUES ('hg_emp_gn02', @pw_hash, '010-6602-0001', 'gn_emp02@test.com', @role_emp, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO employees (user_id, emp_name, sub_phone, emp_address, emp_type, is_deleted, created_at, updated_at)
VALUES ((SELECT id FROM users WHERE user_id = 'hg_emp_gn02'), '남궁민', '010-6602-0002', '서울시 강남구 삼성동 200', 'DAILY', 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 근로자 GN-03
INSERT INTO users (user_id, password, phone, email, role_id, profile_completed, created_at, updated_at)
VALUES ('hg_emp_gn03', @pw_hash, '010-6603-0001', 'gn_emp03@test.com', @role_emp, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO employees (user_id, emp_name, sub_phone, emp_address, emp_type, is_deleted, created_at, updated_at)
VALUES ((SELECT id FROM users WHERE user_id = 'hg_emp_gn03'), '도경수', '010-6603-0002', '서울시 강남구 논현동 300', 'DAILY', 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- === 송파 아파트 현장 근로자 (2명) ===

-- 근로자 SP-01
INSERT INTO users (user_id, password, phone, email, role_id, profile_completed, created_at, updated_at)
VALUES ('hg_emp_sp01', @pw_hash, '010-6701-0001', 'sp_emp01@test.com', @role_emp, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO employees (user_id, emp_name, sub_phone, emp_address, emp_type, is_deleted, created_at, updated_at)
VALUES ((SELECT id FROM users WHERE user_id = 'hg_emp_sp01'), '송중기', '010-6701-0002', '서울시 송파구 방이동 100', 'PERMANENT', 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 근로자 SP-02
INSERT INTO users (user_id, password, phone, email, role_id, profile_completed, created_at, updated_at)
VALUES ('hg_emp_sp02', @pw_hash, '010-6702-0001', 'sp_emp02@test.com', @role_emp, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO employees (user_id, emp_name, sub_phone, emp_address, emp_type, is_deleted, created_at, updated_at)
VALUES ((SELECT id FROM users WHERE user_id = 'hg_emp_sp02'), '파송송', '010-6702-0002', '서울시 송파구 문정동 200', 'DAILY', 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- === 마포 상가 현장 근로자 (2명) ===

-- 근로자 MP-01
INSERT INTO users (user_id, password, phone, email, role_id, profile_completed, created_at, updated_at)
VALUES ('hg_emp_mp01', @pw_hash, '010-6801-0001', 'mp_emp01@test.com', @role_emp, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO employees (user_id, emp_name, sub_phone, emp_address, emp_type, is_deleted, created_at, updated_at)
VALUES ((SELECT id FROM users WHERE user_id = 'hg_emp_mp01'), '마동석', '010-6801-0002', '서울시 마포구 서교동 100', 'PERMANENT', 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 근로자 MP-02
INSERT INTO users (user_id, password, phone, email, role_id, profile_completed, created_at, updated_at)
VALUES ('hg_emp_mp02', @pw_hash, '010-6802-0001', 'mp_emp02@test.com', @role_emp, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO employees (user_id, emp_name, sub_phone, emp_address, emp_type, is_deleted, created_at, updated_at)
VALUES ((SELECT id FROM users WHERE user_id = 'hg_emp_mp02'), '포마포', '010-6802-0002', '서울시 마포구 망원동 200', 'DAILY', 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- === 종로 빌딩 현장 근로자 (2명) ===

-- 근로자 JR-01
INSERT INTO users (user_id, password, phone, email, role_id, profile_completed, created_at, updated_at)
VALUES ('ns_emp_jr01', @pw_hash, '010-7801-0001', 'jr_emp01@test.com', @role_emp, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO employees (user_id, emp_name, sub_phone, emp_address, emp_type, is_deleted, created_at, updated_at)
VALUES ((SELECT id FROM users WHERE user_id = 'ns_emp_jr01'), '종로삼', '010-7801-0002', '서울시 종로구 혜화동 100', 'PERMANENT', 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 근로자 JR-02
INSERT INTO users (user_id, password, phone, email, role_id, profile_completed, created_at, updated_at)
VALUES ('ns_emp_jr02', @pw_hash, '010-7802-0001', 'jr_emp02@test.com', @role_emp, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO employees (user_id, emp_name, sub_phone, emp_address, emp_type, is_deleted, created_at, updated_at)
VALUES ((SELECT id FROM users WHERE user_id = 'ns_emp_jr02'), '노종로', '010-7802-0002', '서울시 종로구 명륜동 200', 'DAILY', 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- ============================================
-- 5. 근로계약 생성 (각 근로자별 계약)
-- ============================================

-- 강남 현장 근로자 계약
INSERT INTO contracts (employee_id, corporation_id, manager_id, role, emp_type, contract_state,
                      employee_start_date, employee_end_date, written_at, corp_signed_at, emp_signed_at,
                      is_deleted, created_at, updated_at)
SELECT
    e.id,
    (SELECT id FROM corporations WHERE corp_name = '(주)한강건설'),
    (SELECT id FROM managers WHERE manager_name = '이강남'),
    CASE WHEN e.emp_type = 'PERMANENT' THEN '현장 기술직' ELSE '일반 노무' END,
    e.emp_type,
    'FULLY_SIGNED',
    '2025-01-01',
    '2026-12-31',
    '2025-01-01 09:00:00',
    '2025-01-01 10:00:00',
    '2025-01-01 11:00:00',
    0, NOW(), NOW()
FROM employees e
JOIN users u ON e.user_id = u.id
WHERE u.user_id LIKE 'hg_emp_gn%'
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 송파 현장 근로자 계약
INSERT INTO contracts (employee_id, corporation_id, manager_id, role, emp_type, contract_state,
                      employee_start_date, employee_end_date, written_at, corp_signed_at, emp_signed_at,
                      is_deleted, created_at, updated_at)
SELECT
    e.id,
    (SELECT id FROM corporations WHERE corp_name = '(주)한강건설'),
    (SELECT id FROM managers WHERE manager_name = '박송파'),
    CASE WHEN e.emp_type = 'PERMANENT' THEN '현장 기술직' ELSE '일반 노무' END,
    e.emp_type,
    'FULLY_SIGNED',
    '2025-03-01',
    '2025-12-31',
    '2025-03-01 09:00:00',
    '2025-03-01 10:00:00',
    '2025-03-01 11:00:00',
    0, NOW(), NOW()
FROM employees e
JOIN users u ON e.user_id = u.id
WHERE u.user_id LIKE 'hg_emp_sp%'
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 마포 현장 근로자 계약
INSERT INTO contracts (employee_id, corporation_id, manager_id, role, emp_type, contract_state,
                      employee_start_date, employee_end_date, written_at, corp_signed_at, emp_signed_at,
                      is_deleted, created_at, updated_at)
SELECT
    e.id,
    (SELECT id FROM corporations WHERE corp_name = '(주)한강건설'),
    (SELECT id FROM managers WHERE manager_name = '최마포'),
    CASE WHEN e.emp_type = 'PERMANENT' THEN '현장 기술직' ELSE '일반 노무' END,
    e.emp_type,
    'FULLY_SIGNED',
    '2025-06-01',
    '2025-11-30',
    '2025-06-01 09:00:00',
    '2025-06-01 10:00:00',
    '2025-06-01 11:00:00',
    0, NOW(), NOW()
FROM employees e
JOIN users u ON e.user_id = u.id
WHERE u.user_id LIKE 'hg_emp_mp%'
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 종로 현장 근로자 계약
INSERT INTO contracts (employee_id, corporation_id, manager_id, role, emp_type, contract_state,
                      employee_start_date, employee_end_date, written_at, corp_signed_at, emp_signed_at,
                      is_deleted, created_at, updated_at)
SELECT
    e.id,
    (SELECT id FROM corporations WHERE corp_name = '(주)남산토건'),
    (SELECT id FROM managers WHERE manager_name = '정종로'),
    CASE WHEN e.emp_type = 'PERMANENT' THEN '현장 기술직' ELSE '일반 노무' END,
    e.emp_type,
    'FULLY_SIGNED',
    '2025-04-01',
    '2025-09-30',
    '2025-04-01 09:00:00',
    '2025-04-01 10:00:00',
    '2025-04-01 11:00:00',
    0, NOW(), NOW()
FROM employees e
JOIN users u ON e.user_id = u.id
WHERE u.user_id LIKE 'ns_emp_jr%'
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- ============================================
-- 6. 계약 상세 생성
-- ============================================

-- 강남 현장 계약 상세
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
SELECT
    c.id,
    '(주)한강건설',
    e.emp_name,
    '강남 오피스텔 신축현장',
    CASE WHEN e.emp_type = 'PERMANENT' THEN '현장 관리' ELSE '일반 노무' END,
    '08:00:00', '17:00:00', '12:00:00', '13:00:00',
    '월,화,수,목,금', '토,일',
    CASE WHEN e.emp_type = 'PERMANENT' THEN 18000.00 ELSE 15000.00 END,
    9000.00, 9000.00, 18000.00,
    10,
    CASE WHEN e.emp_type = 'PERMANENT' THEN 'MONTHLY' ELSE 'DAILY' END,
    'TRANSFER',
    TRUE, TRUE, TRUE, TRUE,
    '서울특별시 영등포구 여의대로 108', '박한강', e.emp_address,
    0, NOW(), NOW()
FROM contracts c
JOIN employees e ON c.employee_id = e.id
JOIN users u ON e.user_id = u.id
WHERE u.user_id LIKE 'hg_emp_gn%'
ON DUPLICATE KEY UPDATE contract_id = VALUES(contract_id);

-- 송파 현장 계약 상세
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
SELECT
    c.id,
    '(주)한강건설',
    e.emp_name,
    '송파 아파트 리모델링',
    CASE WHEN e.emp_type = 'PERMANENT' THEN '현장 관리' ELSE '일반 노무' END,
    '09:00:00', '18:00:00', '12:00:00', '13:00:00',
    '월,화,수,목,금', '토,일',
    CASE WHEN e.emp_type = 'PERMANENT' THEN 17000.00 ELSE 14000.00 END,
    8500.00, 8500.00, 17000.00,
    15,
    CASE WHEN e.emp_type = 'PERMANENT' THEN 'MONTHLY' ELSE 'WEEKLY' END,
    'TRANSFER',
    TRUE, TRUE, TRUE, TRUE,
    '서울특별시 영등포구 여의대로 108', '박한강', e.emp_address,
    0, NOW(), NOW()
FROM contracts c
JOIN employees e ON c.employee_id = e.id
JOIN users u ON e.user_id = u.id
WHERE u.user_id LIKE 'hg_emp_sp%'
ON DUPLICATE KEY UPDATE contract_id = VALUES(contract_id);

-- 마포 현장 계약 상세
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
SELECT
    c.id,
    '(주)한강건설',
    e.emp_name,
    '마포 상가 증축공사',
    CASE WHEN e.emp_type = 'PERMANENT' THEN '현장 관리' ELSE '일반 노무' END,
    '08:30:00', '17:30:00', '12:00:00', '13:00:00',
    '월,화,수,목,금,토', '일',
    CASE WHEN e.emp_type = 'PERMANENT' THEN 16000.00 ELSE 13000.00 END,
    8000.00, 8000.00, 16000.00,
    5,
    CASE WHEN e.emp_type = 'PERMANENT' THEN 'MONTHLY' ELSE 'DAILY' END,
    'TRANSFER',
    TRUE, TRUE, FALSE, FALSE,
    '서울특별시 영등포구 여의대로 108', '박한강', e.emp_address,
    0, NOW(), NOW()
FROM contracts c
JOIN employees e ON c.employee_id = e.id
JOIN users u ON e.user_id = u.id
WHERE u.user_id LIKE 'hg_emp_mp%'
ON DUPLICATE KEY UPDATE contract_id = VALUES(contract_id);

-- 종로 현장 계약 상세
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
SELECT
    c.id,
    '(주)남산토건',
    e.emp_name,
    '종로 빌딩 보수공사',
    CASE WHEN e.emp_type = 'PERMANENT' THEN '현장 관리' ELSE '일반 노무' END,
    '09:00:00', '18:00:00', '12:00:00', '13:00:00',
    '월,화,수,목,금', '토,일',
    CASE WHEN e.emp_type = 'PERMANENT' THEN 15000.00 ELSE 12000.00 END,
    7500.00, 7500.00, 15000.00,
    10,
    CASE WHEN e.emp_type = 'PERMANENT' THEN 'MONTHLY' ELSE 'DAILY' END,
    'TRANSFER',
    TRUE, TRUE, TRUE, TRUE,
    '서울특별시 중구 남산동 100', '김남산', e.emp_address,
    0, NOW(), NOW()
FROM contracts c
JOIN employees e ON c.employee_id = e.id
JOIN users u ON e.user_id = u.id
WHERE u.user_id LIKE 'ns_emp_jr%'
ON DUPLICATE KEY UPDATE contract_id = VALUES(contract_id);

-- ============================================
-- 7. 급여 데이터 생성 (최근 3개월)
-- ============================================

-- 강남 현장 근로자 급여 (11월)
INSERT INTO payrolls (employee_id, site_id, contract_id, corporation_id, salary_year, salary_month, salary_week, salary_day, search_date,
                     emp_type, emp_name, pay_cycle, total_pay, income_tax, resident_tax, pay_status, is_deleted, created_at, updated_at)
SELECT
    e.id,
    (SELECT id FROM sites WHERE site_name = '강남 오피스텔 신축현장'),
    c.id,
    (SELECT id FROM corporations WHERE corp_name = '(주)한강건설'),
    2025, 11, 0, '2025-11-01', '2025-11-10',
    e.emp_type,
    e.emp_name,
    CASE WHEN e.emp_type = 'PERMANENT' THEN 'MONTHLY' ELSE 'DAILY' END,
    CASE WHEN e.emp_type = 'PERMANENT' THEN 3500000.00 ELSE 2800000.00 END,
    CASE WHEN e.emp_type = 'PERMANENT' THEN 70000.00 ELSE 50000.00 END,
    CASE WHEN e.emp_type = 'PERMANENT' THEN 7000.00 ELSE 5000.00 END,
    'PAID',
    0, NOW(), NOW()
FROM employees e
JOIN users u ON e.user_id = u.id
JOIN contracts c ON c.employee_id = e.id
WHERE u.user_id LIKE 'hg_emp_gn%'
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 강남 현장 근로자 급여 (10월)
INSERT INTO payrolls (employee_id, site_id, contract_id, corporation_id, salary_year, salary_month, salary_week, salary_day, search_date,
                     emp_type, emp_name, pay_cycle, total_pay, income_tax, resident_tax, pay_status, is_deleted, created_at, updated_at)
SELECT
    e.id,
    (SELECT id FROM sites WHERE site_name = '강남 오피스텔 신축현장'),
    c.id,
    (SELECT id FROM corporations WHERE corp_name = '(주)한강건설'),
    2025, 10, 0, '2025-10-01', '2025-10-10',
    e.emp_type,
    e.emp_name,
    CASE WHEN e.emp_type = 'PERMANENT' THEN 'MONTHLY' ELSE 'DAILY' END,
    CASE WHEN e.emp_type = 'PERMANENT' THEN 3400000.00 ELSE 2700000.00 END,
    CASE WHEN e.emp_type = 'PERMANENT' THEN 68000.00 ELSE 48000.00 END,
    CASE WHEN e.emp_type = 'PERMANENT' THEN 6800.00 ELSE 4800.00 END,
    'PAID',
    0, NOW(), NOW()
FROM employees e
JOIN users u ON e.user_id = u.id
JOIN contracts c ON c.employee_id = e.id
WHERE u.user_id LIKE 'hg_emp_gn%'
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 송파 현장 근로자 급여 (11월)
INSERT INTO payrolls (employee_id, site_id, contract_id, corporation_id, salary_year, salary_month, salary_week, salary_day, search_date,
                     emp_type, emp_name, pay_cycle, total_pay, income_tax, resident_tax, pay_status, is_deleted, created_at, updated_at)
SELECT
    e.id,
    (SELECT id FROM sites WHERE site_name = '송파 아파트 리모델링'),
    c.id,
    (SELECT id FROM corporations WHERE corp_name = '(주)한강건설'),
    2025, 11, 0, '2025-11-01', '2025-11-15',
    e.emp_type,
    e.emp_name,
    CASE WHEN e.emp_type = 'PERMANENT' THEN 'MONTHLY' ELSE 'WEEKLY' END,
    CASE WHEN e.emp_type = 'PERMANENT' THEN 3300000.00 ELSE 2600000.00 END,
    CASE WHEN e.emp_type = 'PERMANENT' THEN 66000.00 ELSE 46000.00 END,
    CASE WHEN e.emp_type = 'PERMANENT' THEN 6600.00 ELSE 4600.00 END,
    'PAID',
    0, NOW(), NOW()
FROM employees e
JOIN users u ON e.user_id = u.id
JOIN contracts c ON c.employee_id = e.id
WHERE u.user_id LIKE 'hg_emp_sp%'
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 마포 현장 근로자 급여 (11월)
INSERT INTO payrolls (employee_id, site_id, contract_id, corporation_id, salary_year, salary_month, salary_week, salary_day, search_date,
                     emp_type, emp_name, pay_cycle, total_pay, income_tax, resident_tax, pay_status, is_deleted, created_at, updated_at)
SELECT
    e.id,
    (SELECT id FROM sites WHERE site_name = '마포 상가 증축공사'),
    c.id,
    (SELECT id FROM corporations WHERE corp_name = '(주)한강건설'),
    2025, 11, 0, '2025-11-01', '2025-11-05',
    e.emp_type,
    e.emp_name,
    CASE WHEN e.emp_type = 'PERMANENT' THEN 'MONTHLY' ELSE 'DAILY' END,
    CASE WHEN e.emp_type = 'PERMANENT' THEN 3200000.00 ELSE 2500000.00 END,
    CASE WHEN e.emp_type = 'PERMANENT' THEN 64000.00 ELSE 45000.00 END,
    CASE WHEN e.emp_type = 'PERMANENT' THEN 6400.00 ELSE 4500.00 END,
    'PENDING',
    0, NOW(), NOW()
FROM employees e
JOIN users u ON e.user_id = u.id
JOIN contracts c ON c.employee_id = e.id
WHERE u.user_id LIKE 'hg_emp_mp%'
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 종로 현장 근로자 급여 (11월)
INSERT INTO payrolls (employee_id, site_id, contract_id, corporation_id, salary_year, salary_month, salary_week, salary_day, search_date,
                     emp_type, emp_name, pay_cycle, total_pay, income_tax, resident_tax, pay_status, is_deleted, created_at, updated_at)
SELECT
    e.id,
    (SELECT id FROM sites WHERE site_name = '종로 빌딩 보수공사'),
    c.id,
    (SELECT id FROM corporations WHERE corp_name = '(주)남산토건'),
    2025, 11, 0, '2025-11-01', '2025-11-10',
    e.emp_type,
    e.emp_name,
    CASE WHEN e.emp_type = 'PERMANENT' THEN 'MONTHLY' ELSE 'DAILY' END,
    CASE WHEN e.emp_type = 'PERMANENT' THEN 3000000.00 ELSE 2400000.00 END,
    CASE WHEN e.emp_type = 'PERMANENT' THEN 60000.00 ELSE 42000.00 END,
    CASE WHEN e.emp_type = 'PERMANENT' THEN 6000.00 ELSE 4200.00 END,
    'PAID',
    0, NOW(), NOW()
FROM employees e
JOIN users u ON e.user_id = u.id
JOIN contracts c ON c.employee_id = e.id
WHERE u.user_id LIKE 'ns_emp_jr%'
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- ============================================
-- 8. 급여 상세 항목 (PayslipItem) 생성
-- ============================================

-- 강남 현장 11월 급여 상세
INSERT INTO payslip_items (payroll_id, item_name, amount, item_type, is_deleted, created_at, updated_at)
SELECT
    p.id, '기본급',
    CASE WHEN e.emp_type = 'PERMANENT' THEN 3000000.00 ELSE 2400000.00 END,
    'EARNING', 0, NOW(), NOW()
FROM payrolls p
JOIN employees e ON p.employee_id = e.id
JOIN users u ON e.user_id = u.id
WHERE u.user_id LIKE 'hg_emp_gn%' AND p.salary_month = 11
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO payslip_items (payroll_id, item_name, amount, item_type, is_deleted, created_at, updated_at)
SELECT
    p.id, '연장근로수당',
    CASE WHEN e.emp_type = 'PERMANENT' THEN 300000.00 ELSE 250000.00 END,
    'EARNING', 0, NOW(), NOW()
FROM payrolls p
JOIN employees e ON p.employee_id = e.id
JOIN users u ON e.user_id = u.id
WHERE u.user_id LIKE 'hg_emp_gn%' AND p.salary_month = 11
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO payslip_items (payroll_id, item_name, amount, item_type, is_deleted, created_at, updated_at)
SELECT
    p.id, '야간근로수당',
    CASE WHEN e.emp_type = 'PERMANENT' THEN 200000.00 ELSE 150000.00 END,
    'EARNING', 0, NOW(), NOW()
FROM payrolls p
JOIN employees e ON p.employee_id = e.id
JOIN users u ON e.user_id = u.id
WHERE u.user_id LIKE 'hg_emp_gn%' AND p.salary_month = 11
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO payslip_items (payroll_id, item_name, amount, item_type, is_deleted, created_at, updated_at)
SELECT
    p.id, '국민연금', 135000.00, 'DEDUCTION', 0, NOW(), NOW()
FROM payrolls p
JOIN employees e ON p.employee_id = e.id
JOIN users u ON e.user_id = u.id
WHERE u.user_id LIKE 'hg_emp_gn%' AND p.salary_month = 11 AND e.emp_type = 'PERMANENT'
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO payslip_items (payroll_id, item_name, amount, item_type, is_deleted, created_at, updated_at)
SELECT
    p.id, '건강보험', 110000.00, 'DEDUCTION', 0, NOW(), NOW()
FROM payrolls p
JOIN employees e ON p.employee_id = e.id
JOIN users u ON e.user_id = u.id
WHERE u.user_id LIKE 'hg_emp_gn%' AND p.salary_month = 11 AND e.emp_type = 'PERMANENT'
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO payslip_items (payroll_id, item_name, amount, item_type, is_deleted, created_at, updated_at)
SELECT
    p.id, '고용보험', 27000.00, 'DEDUCTION', 0, NOW(), NOW()
FROM payrolls p
JOIN employees e ON p.employee_id = e.id
JOIN users u ON e.user_id = u.id
WHERE u.user_id LIKE 'hg_emp_gn%' AND p.salary_month = 11
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- ============================================
-- 9. 출퇴근 기록 생성 (최근 일주일)
-- ============================================

-- 강남 현장 출퇴근 (오늘)
INSERT INTO attendance_records (employee_id, site_id, attendance_type, timestamp, state, captured_face_image_url, is_deleted, created_at, updated_at)
SELECT
    e.id,
    (SELECT id FROM sites WHERE site_name = '강남 오피스텔 신축현장'),
    'CHECK_IN',
    CONCAT(CURDATE(), ' 08:05:00'),
    'CONFIRMED',
    'https://buildup-bucket.s3.ap-northeast-2.amazonaws.com/attendance/test-face.jpg',
    0, NOW(), NOW()
FROM employees e
JOIN users u ON e.user_id = u.id
WHERE u.user_id LIKE 'hg_emp_gn%'
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 송파 현장 출퇴근 (오늘)
INSERT INTO attendance_records (employee_id, site_id, attendance_type, timestamp, state, captured_face_image_url, is_deleted, created_at, updated_at)
SELECT
    e.id,
    (SELECT id FROM sites WHERE site_name = '송파 아파트 리모델링'),
    'CHECK_IN',
    CONCAT(CURDATE(), ' 09:02:00'),
    'CONFIRMED',
    'https://buildup-bucket.s3.ap-northeast-2.amazonaws.com/attendance/test-face.jpg',
    0, NOW(), NOW()
FROM employees e
JOIN users u ON e.user_id = u.id
WHERE u.user_id LIKE 'hg_emp_sp%'
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- ============================================
-- 완료 메시지
-- ============================================
SELECT '✅ 추가 테스트 데이터 생성 완료!' AS Status;

SELECT '
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📋 추가된 테스트 계정 (비밀번호: Admin123!@)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

🏢 기업 계정:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
1️⃣ (주)한강건설 (현장 3개 보유)
   - ID: hangangcorp
   - 대표: 박한강

2️⃣ (주)남산토건 (현장 1개 보유)
   - ID: namsancorp
   - 대표: 김남산

👔 현장 관리자 계정:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[한강건설]
  - hg_manager01 (이강남) → 강남 오피스텔 신축현장
  - hg_manager02 (박송파) → 송파 아파트 리모델링
  - hg_manager03 (최마포) → 마포 상가 증축공사

[남산토건]
  - ns_manager01 (정종로) → 종로 빌딩 보수공사

🏗️ 현장 정보:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[한강건설 소속 - 3개]
  1. 강남 오피스텔 신축현장
     - 관리자키: CONC-HG01-MGR-2025
     - 근로자키: CONC-HG01-EMP-2025
  2. 송파 아파트 리모델링
     - 관리자키: CONC-HG02-MGR-2025
     - 근로자키: CONC-HG02-EMP-2025
  3. 마포 상가 증축공사
     - 관리자키: CONC-HG03-MGR-2025
     - 근로자키: CONC-HG03-EMP-2025

[남산토건 소속 - 1개]
  1. 종로 빌딩 보수공사
     - 관리자키: CONC-NS01-MGR-2025
     - 근로자키: CONC-NS01-EMP-2025

👷 근로자 계정:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[강남 현장] hg_emp_gn01(강태공), hg_emp_gn02(남궁민), hg_emp_gn03(도경수)
[송파 현장] hg_emp_sp01(송중기), hg_emp_sp02(파송송)
[마포 현장] hg_emp_mp01(마동석), hg_emp_mp02(포마포)
[종로 현장] ns_emp_jr01(종로삼), ns_emp_jr02(노종로)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🧪 API 테스트 시나리오:

1. 기업 관리자 로그인 후 현장 목록 조회:
   POST /api/v1/auth/login
   { "username": "hangangcorp", "password": "Admin123!@" }
   → 한강건설 소속 현장 3개 조회 가능

2. 현장 관리자 로그인:
   { "username": "hg_manager01", "password": "Admin123!@" }
   → 강남 오피스텔 현장 관리

3. 근로자 로그인 후 급여 조회:
   { "username": "hg_emp_gn01", "password": "Admin123!@" }
   → 강태공 급여 내역 조회

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
' AS Info;
