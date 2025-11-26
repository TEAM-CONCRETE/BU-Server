-- ============================================
-- 프론트엔드 추가 테스트 데이터
-- (계약, 급여, 근태, 안전교육)
-- frontend-complete-test-data.sql 실행 후 사용
-- ============================================

USE buildup;

-- ============================================
-- 1. 계약서 5개 (FULLY_SIGNED 3개, MANAGER_SIGNING_PENDING 2개)
-- ============================================

-- frontemp001 - FULLY_SIGNED (정규직)
INSERT INTO contracts (
    employee_id, corporation_id, manager_id, role, contract_state,
    employee_start_date, employee_end_date,
    written_at, corp_signed_at, emp_signed_at,
    emp_type, is_deleted, created_at, updated_at
)
SELECT
    e.id, c.id, m.id,
    '현장작업자',
    'FULLY_SIGNED',
    DATE_SUB(CURDATE(), INTERVAL 60 DAY),
    DATE_ADD(CURDATE(), INTERVAL 305 DAY),
    DATE_SUB(NOW(), INTERVAL 60 DAY),
    DATE_SUB(NOW(), INTERVAL 59 DAY),
    DATE_SUB(NOW(), INTERVAL 58 DAY),
    'PERMANENT',
    0,
    NOW(),
    NOW()
FROM employees e
CROSS JOIN corporations c
CROSS JOIN managers m
WHERE e.emp_name = '김철수'
  AND c.corp_name = '(주)프론트테스트건설'
  AND m.manager_name = '이현장';

-- frontemp002 - FULLY_SIGNED (정규직)
INSERT INTO contracts (
    employee_id, corporation_id, manager_id, role, contract_state,
    employee_start_date, employee_end_date,
    written_at, corp_signed_at, emp_signed_at,
    emp_type, is_deleted, created_at, updated_at
)
SELECT
    e.id, c.id, m.id,
    '안전관리자',
    'FULLY_SIGNED',
    DATE_SUB(CURDATE(), INTERVAL 50 DAY),
    DATE_ADD(CURDATE(), INTERVAL 315 DAY),
    DATE_SUB(NOW(), INTERVAL 50 DAY),
    DATE_SUB(NOW(), INTERVAL 49 DAY),
    DATE_SUB(NOW(), INTERVAL 48 DAY),
    'PERMANENT',
    0,
    NOW(),
    NOW()
FROM employees e
CROSS JOIN corporations c
CROSS JOIN managers m
WHERE e.emp_name = '이영희'
  AND c.corp_name = '(주)프론트테스트건설'
  AND m.manager_name = '이현장';

-- frontemp003 - FULLY_SIGNED (정규직)
INSERT INTO contracts (
    employee_id, corporation_id, manager_id, role, contract_state,
    employee_start_date, employee_end_date,
    written_at, corp_signed_at, emp_signed_at,
    emp_type, is_deleted, created_at, updated_at
)
SELECT
    e.id, c.id, m.id,
    '중장비 기사',
    'FULLY_SIGNED',
    DATE_SUB(CURDATE(), INTERVAL 40 DAY),
    DATE_ADD(CURDATE(), INTERVAL 325 DAY),
    DATE_SUB(NOW(), INTERVAL 40 DAY),
    DATE_SUB(NOW(), INTERVAL 39 DAY),
    DATE_SUB(NOW(), INTERVAL 38 DAY),
    'PERMANENT',
    0,
    NOW(),
    NOW()
FROM employees e
CROSS JOIN corporations c
CROSS JOIN managers m
WHERE e.emp_name = '박민준'
  AND c.corp_name = '(주)프론트테스트건설'
  AND m.manager_name = '이현장';

-- frontemp004 - MANAGER_SIGNING_PENDING (일용직)
INSERT INTO contracts (
    employee_id, corporation_id, manager_id, role, contract_state,
    employee_start_date, employee_end_date,
    written_at, corp_signed_at,
    emp_type, is_deleted, created_at, updated_at
)
SELECT
    e.id, c.id, m.id,
    '보조 작업자',
    'MANAGER_SIGNING_PENDING',
    CURDATE(),
    DATE_ADD(CURDATE(), INTERVAL 90 DAY),
    DATE_SUB(NOW(), INTERVAL 1 DAY),
    DATE_SUB(NOW(), INTERVAL 1 DAY),
    'DAILY',
    0,
    NOW(),
    NOW()
FROM employees e
CROSS JOIN corporations c
CROSS JOIN managers m
WHERE e.emp_name = '최지은'
  AND c.corp_name = '(주)프론트테스트건설'
  AND m.manager_name = '이현장';

-- frontemp005 - MANAGER_SIGNING_PENDING (일용직)
INSERT INTO contracts (
    employee_id, corporation_id, manager_id, role, contract_state,
    employee_start_date, employee_end_date,
    written_at, corp_signed_at,
    emp_type, is_deleted, created_at, updated_at
)
SELECT
    e.id, c.id, m.id,
    '일용 작업자',
    'MANAGER_SIGNING_PENDING',
    DATE_ADD(CURDATE(), INTERVAL 3 DAY),
    DATE_ADD(CURDATE(), INTERVAL 93 DAY),
    NOW(),
    NOW(),
    'DAILY',
    0,
    NOW(),
    NOW()
FROM employees e
CROSS JOIN corporations c
CROSS JOIN managers m
WHERE e.emp_name = '정서연'
  AND c.corp_name = '(주)프론트테스트건설'
  AND m.manager_name = '이현장';

-- ============================================
-- 2. 계약 상세 (contract_details) - 5개
-- ============================================

-- 김철수 계약 상세
INSERT INTO contract_details (
    contract_id, corp_name, emp_name, work_type, work_place,
    work_start_time, work_end_time, break_start_time, break_end_time,
    work_on_days, work_off_days,
    pay_type, work_pay, pay_period, pay_day,
    is_nps_applicable, is_nhi_applicable, is_eoi_applicable, is_wci_applicable,
    is_deleted, created_at, updated_at
)
SELECT
    c.id,
    '(주)프론트테스트건설',
    '김철수',
    '현장작업',
    '강남 오피스텔 A동 신축현장',
    '09:00:00',
    '18:00:00',
    '12:00:00',
    '13:00:00',
    '월,화,수,목,금',
    '토,일',
    'TRANSFER',
    3500000,
    'MONTHLY',
    25,
    1, 1, 1, 1,
    0, NOW(), NOW()
FROM contracts c
CROSS JOIN employees e
WHERE e.emp_name = '김철수'
  AND c.employee_id = e.id;

-- 이영희 계약 상세
INSERT INTO contract_details (
    contract_id, corp_name, emp_name, work_type, work_place,
    work_start_time, work_end_time, break_start_time, break_end_time,
    work_on_days, work_off_days,
    pay_type, work_pay, pay_period, pay_day,
    is_nps_applicable, is_nhi_applicable, is_eoi_applicable, is_wci_applicable,
    is_deleted, created_at, updated_at
)
SELECT
    c.id,
    '(주)프론트테스트건설',
    '이영희',
    '안전관리',
    '강남 오피스텔 A동 신축현장',
    '08:00:00',
    '17:00:00',
    '12:00:00',
    '13:00:00',
    '월,화,수,목,금',
    '토,일',
    'TRANSFER',
    4000000,
    'MONTHLY',
    25,
    1, 1, 1, 1,
    0, NOW(), NOW()
FROM contracts c
CROSS JOIN employees e
WHERE e.emp_name = '이영희'
  AND c.employee_id = e.id;

-- 박민준 계약 상세
INSERT INTO contract_details (
    contract_id, corp_name, emp_name, work_type, work_place,
    work_start_time, work_end_time, break_start_time, break_end_time,
    work_on_days, work_off_days,
    pay_type, work_pay, pay_period, pay_day,
    is_nps_applicable, is_nhi_applicable, is_eoi_applicable, is_wci_applicable,
    is_deleted, created_at, updated_at
)
SELECT
    c.id,
    '(주)프론트테스트건설',
    '박민준',
    '중장비 운전',
    '송파 아파트 B동 리모델링',
    '08:30:00',
    '17:30:00',
    '12:00:00',
    '13:00:00',
    '월,화,수,목,금',
    '토,일',
    'TRANSFER',
    4200000,
    'MONTHLY',
    25,
    1, 1, 1, 1,
    0, NOW(), NOW()
FROM contracts c
CROSS JOIN employees e
WHERE e.emp_name = '박민준'
  AND c.employee_id = e.id;

-- 최지은 계약 상세 (일용직)
INSERT INTO contract_details (
    contract_id, corp_name, emp_name, work_type, work_place,
    work_start_time, work_end_time, break_start_time, break_end_time,
    work_on_days, work_off_days,
    pay_type, work_pay, pay_period,
    is_nps_applicable, is_nhi_applicable, is_eoi_applicable, is_wci_applicable,
    is_deleted, created_at, updated_at
)
SELECT
    c.id,
    '(주)프론트테스트건설',
    '최지은',
    '보조작업',
    '강남 오피스텔 A동 신축현장',
    '09:00:00',
    '18:00:00',
    '12:00:00',
    '13:00:00',
    '월,화,수,목,금',
    '토,일',
    'CASH',
    150000,
    'DAILY',
    1, 1, 1, 1,
    0, NOW(), NOW()
FROM contracts c
CROSS JOIN employees e
WHERE e.emp_name = '최지은'
  AND c.employee_id = e.id;

-- 정서연 계약 상세 (일용직)
INSERT INTO contract_details (
    contract_id, corp_name, emp_name, work_type, work_place,
    work_start_time, work_end_time, break_start_time, break_end_time,
    work_on_days, work_off_days,
    pay_type, work_pay, pay_period,
    is_nps_applicable, is_nhi_applicable, is_eoi_applicable, is_wci_applicable,
    is_deleted, created_at, updated_at
)
SELECT
    c.id,
    '(주)프론트테스트건설',
    '정서연',
    '일용작업',
    '송파 아파트 B동 리모델링',
    '09:00:00',
    '17:00:00',
    '12:00:00',
    '13:00:00',
    '월,화,수,목,금,토',
    '일',
    'CASH',
    160000,
    'DAILY',
    1, 1, 1, 1,
    0, NOW(), NOW()
FROM contracts c
CROSS JOIN employees e
WHERE e.emp_name = '정서연'
  AND c.employee_id = e.id;

-- ============================================
-- 3. 오늘 출근 기록 3개 (지각 2명)
-- ============================================

-- 김철수 - 정상 출근 (09:00)
INSERT INTO attendance_records (
    employee_id, site_id, attendance_type, state,
    timestamp, captured_face_image_url, similarity_score,
    is_deleted, created_at, updated_at
)
SELECT
    e.id,
    s.id,
    'CHECK_IN',
    'CONFIRMED',
    CONCAT(CURDATE(), ' 09:00:00'),
    'https://example.com/faces/kim-checkin.jpg',
    98.5,
    0, NOW(), NOW()
FROM employees e
CROSS JOIN sites s
WHERE e.emp_name = '김철수'
  AND s.site_name LIKE '%강남%'
LIMIT 1;

-- 이영희 - 지각 (08:10, 출근시간 08:00 + 5분 초과)
INSERT INTO attendance_records (
    employee_id, site_id, attendance_type, state,
    timestamp, captured_face_image_url, similarity_score,
    is_deleted, created_at, updated_at
)
SELECT
    e.id,
    s.id,
    'CHECK_IN',
    'CONFIRMED',
    CONCAT(CURDATE(), ' 08:10:00'),
    'https://example.com/faces/lee-checkin.jpg',
    97.2,
    0, NOW(), NOW()
FROM employees e
CROSS JOIN sites s
WHERE e.emp_name = '이영희'
  AND s.site_name LIKE '%강남%'
LIMIT 1;

-- 박민준 - 지각 (08:40, 출근시간 08:30 + 5분 초과)
INSERT INTO attendance_records (
    employee_id, site_id, attendance_type, state,
    timestamp, captured_face_image_url, similarity_score,
    is_deleted, created_at, updated_at
)
SELECT
    e.id,
    s.id,
    'CHECK_IN',
    'CONFIRMED',
    CONCAT(CURDATE(), ' 08:40:00'),
    'https://example.com/faces/park-checkin.jpg',
    96.8,
    0, NOW(), NOW()
FROM employees e
CROSS JOIN sites s
WHERE e.emp_name = '박민준'
  AND s.site_name LIKE '%송파%'
LIMIT 1;

-- ============================================
-- 4. 안전교육일지 2개 (MANAGER_SIGNING_PENDING)
-- ============================================

INSERT INTO safety_education_logs (
    corporation_id, manager_id, site_id,
    education_subject, education_content, education_location,
    education_type, instructor_name, status,
    is_deleted, created_at, updated_at
)
SELECT
    c.id,
    m.id,
    s.id,
    '추락재해 예방 안전교육',
    '고소작업 시 안전수칙 및 안전장비 착용 방법',
    '강남 오피스텔 A동 현장사무실',
    'REGULAR',
    '이현장',
    'MANAGER_SIGNING_PENDING',
    0, NOW(), NOW()
FROM corporations c
CROSS JOIN managers m
CROSS JOIN sites s
WHERE c.corp_name = '(주)프론트테스트건설'
  AND m.manager_name = '이현장'
  AND s.site_name LIKE '%강남%'
LIMIT 1;

INSERT INTO safety_education_logs (
    corporation_id, manager_id, site_id,
    education_subject, education_content, education_location,
    education_type, instructor_name, status,
    is_deleted, created_at, updated_at
)
SELECT
    c.id,
    m.id,
    s.id,
    '전기작업 안전교육',
    '감전재해 예방 및 절연장갑 사용법',
    '송파 아파트 B동 현장',
    'SPECIAL',
    '김전기',
    'MANAGER_SIGNING_PENDING',
    0, NOW(), NOW()
FROM corporations c
CROSS JOIN managers m
CROSS JOIN sites s
WHERE c.corp_name = '(주)프론트테스트건설'
  AND m.manager_name = '이현장'
  AND s.site_name LIKE '%송파%'
LIMIT 1;

-- ============================================
-- 검증
-- ============================================

SELECT '=== 추가 데이터 주입 완료 ===' AS Status;
SELECT COUNT(*) AS '계약서' FROM contracts WHERE is_deleted = 0;
SELECT COUNT(*) AS '계약 상세' FROM contract_details WHERE is_deleted = 0;
SELECT COUNT(*) AS '오늘 출근' FROM attendance_records WHERE DATE(timestamp) = CURDATE();
SELECT COUNT(*) AS '안전교육일지' FROM safety_education_logs WHERE status = 'MANAGER_SIGNING_PENDING';

SELECT '' AS '';
SELECT '✅ 모든 API 테스트 데이터 준비 완료' AS '';
