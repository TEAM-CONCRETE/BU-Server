-- ============================================
-- Build-Up 급여 생성 테스트 데이터
-- ============================================
-- 목적: 일급/주급/월급(일용직)/월급(상용직) 급여 명세서 생성 테스트
-- 생성일: 2025-11-15
-- ============================================

USE buildup;

-- ============================================
-- 1. 법인 데이터 생성
-- ============================================
INSERT INTO users (user_id, password, phone, email, role_id) 
SELECT 'corp001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z2EHdsDGF.fWVFR7A1tJDG3a', '02-1234-5678', 'corp@buildup.com', r.id
FROM roles r WHERE r.role_name = 'ROLE_CORPORATION'
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id);

INSERT INTO corporations (user_id, corp_name, corp_address, corp_ceo_name)
SELECT u.id, '(주)빌드업건설', '서울특별시 강남구 테헤란로 123', '김철수'
FROM users u WHERE u.user_id = 'corp001'
ON DUPLICATE KEY UPDATE corp_name = VALUES(corp_name);

-- ============================================
-- 2. 관리자 데이터 생성
-- ============================================
INSERT INTO users (user_id, password, phone, email, role_id)
SELECT 'manager001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z2EHdsDGF.fWVFR7A1tJDG3a', '010-1111-2222', 'manager@buildup.com', r.id
FROM roles r WHERE r.role_name = 'ROLE_MANAGER'
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id);

INSERT INTO managers (user_id, manager_name)
SELECT u.id, '이현장'
FROM users u WHERE u.user_id = 'manager001'
ON DUPLICATE KEY UPDATE manager_name = VALUES(manager_name);

-- ============================================
-- 3. 현장 데이터 생성
-- ============================================
INSERT INTO sites (site_name, site_address, corporation_id, manager_id)
SELECT '강남 아파트 신축공사', '서울특별시 강남구 논현로 456', c.id, m.id
FROM corporations c
JOIN managers m ON c.user_id = (SELECT id FROM users WHERE user_id = 'corp001')
WHERE c.corp_name = '(주)빌드업건설'
  AND m.manager_name = '이현장'
ON DUPLICATE KEY UPDATE site_name = VALUES(site_name);

-- ============================================
-- 4. 근로자 데이터 생성 (4명)
-- ============================================

-- 4-1. 상용직 근로자 (월급)
INSERT INTO users (user_id, password, phone, email, role_id)
SELECT 'emp001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z2EHdsDGF.fWVFR7A1tJDG3a', '010-2001-0001', 'emp001@buildup.com', r.id
FROM roles r WHERE r.role_name = 'ROLE_EMPLOYEE'
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id);

INSERT INTO employees (user_id, emp_name, sub_phone, resident_num, emp_address, emp_type)
SELECT u.id, '박상용', '010-2001-0002', '900101-1234567', '서울시 강남구 역삼동 123', 'PERMANENT'
FROM users u WHERE u.user_id = 'emp001'
ON DUPLICATE KEY UPDATE emp_name = VALUES(emp_name);

-- 4-2. 일용직 근로자 (일급)
INSERT INTO users (user_id, password, phone, email, role_id)
SELECT 'emp002', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z2EHdsDGF.fWVFR7A1tJDG3a', '010-2002-0001', 'emp002@buildup.com', r.id
FROM roles r WHERE r.role_name = 'ROLE_EMPLOYEE'
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id);

INSERT INTO employees (user_id, emp_name, sub_phone, resident_num, emp_address, emp_type)
SELECT u.id, '김일용', '010-2002-0002', '850505-1234567', '서울시 송파구 문정동 456', 'DAILY'
FROM users u WHERE u.user_id = 'emp002'
ON DUPLICATE KEY UPDATE emp_name = VALUES(emp_name);

-- 4-3. 일용직 근로자 (주급)
INSERT INTO users (user_id, password, phone, email, role_id)
SELECT 'emp003', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z2EHdsDGF.fWVFR7A1tJDG3a', '010-2003-0001', 'emp003@buildup.com', r.id
FROM roles r WHERE r.role_name = 'ROLE_EMPLOYEE'
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id);

INSERT INTO employees (user_id, emp_name, sub_phone, resident_num, emp_address, emp_type)
SELECT u.id, '이주간', '010-2003-0002', '920315-1234567', '서울시 서초구 반포동 789', 'DAILY'
FROM users u WHERE u.user_id = 'emp003'
ON DUPLICATE KEY UPDATE emp_name = VALUES(emp_name);

-- 4-4. 일용직 근로자 (월급)
INSERT INTO users (user_id, password, phone, email, role_id)
SELECT 'emp004', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z2EHdsDGF.fWVFR7A1tJDG3a', '010-2004-0001', 'emp004@buildup.com', r.id
FROM roles r WHERE r.role_name = 'ROLE_EMPLOYEE'
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id);

INSERT INTO employees (user_id, emp_name, sub_phone, resident_num, emp_address, emp_type)
SELECT u.id, '정월급', '010-2004-0002', '880720-1234567', '서울시 강동구 천호동 321', 'DAILY'
FROM users u WHERE u.user_id = 'emp004'
ON DUPLICATE KEY UPDATE emp_name = VALUES(emp_name);

-- ============================================
-- 5. 근로계약 데이터 생성
-- ============================================

-- 5-1. 상용직 근로자 계약 (월급)
INSERT INTO contracts (employee_id, corporation_id, manager_id, role, emp_type, contract_state, 
                      employee_start_date, employee_end_date, written_at, corp_signed_at, emp_signed_at)
SELECT e.id, c.id, m.id, '기술직', 'PERMANENT', 'FULLY_SIGNED',
       '2024-10-01', '2025-12-31', 
       '2024-09-20 10:00:00', '2024-09-21 14:30:00', '2024-09-21 16:00:00'
FROM employees e
JOIN corporations c ON c.corp_name = '(주)빌드업건설'
JOIN managers m ON m.manager_name = '이현장'
WHERE e.emp_name = '박상용'
ON DUPLICATE KEY UPDATE contract_state = VALUES(contract_state);

-- 5-2. 일용직 근로자 계약 (일급)
INSERT INTO contracts (employee_id, corporation_id, manager_id, role, emp_type, contract_state,
                      employee_start_date, employee_end_date, written_at, corp_signed_at, emp_signed_at)
SELECT e.id, c.id, m.id, '일반노무', 'DAILY', 'FULLY_SIGNED',
       '2024-11-01', '2025-02-28',
       '2024-10-25 10:00:00', '2024-10-26 14:30:00', '2024-10-26 16:00:00'
FROM employees e
JOIN corporations c ON c.corp_name = '(주)빌드업건설'
JOIN managers m ON m.manager_name = '이현장'
WHERE e.emp_name = '김일용'
ON DUPLICATE KEY UPDATE contract_state = VALUES(contract_state);

-- 5-3. 일용직 근로자 계약 (주급)
INSERT INTO contracts (employee_id, corporation_id, manager_id, role, emp_type, contract_state,
                      employee_start_date, employee_end_date, written_at, corp_signed_at, emp_signed_at)
SELECT e.id, c.id, m.id, '일반노무', 'DAILY', 'FULLY_SIGNED',
       '2024-11-01', '2025-02-28',
       '2024-10-25 10:00:00', '2024-10-26 14:30:00', '2024-10-26 16:00:00'
FROM employees e
JOIN corporations c ON c.corp_name = '(주)빌드업건설'
JOIN managers m ON m.manager_name = '이현장'
WHERE e.emp_name = '이주간'
ON DUPLICATE KEY UPDATE contract_state = VALUES(contract_state);

-- 5-4. 일용직 근로자 계약 (월급)
INSERT INTO contracts (employee_id, corporation_id, manager_id, role, emp_type, contract_state,
                      employee_start_date, employee_end_date, written_at, corp_signed_at, emp_signed_at)
SELECT e.id, c.id, m.id, '일반노무', 'DAILY', 'FULLY_SIGNED',
       '2024-11-01', '2025-02-28',
       '2024-10-25 10:00:00', '2024-10-26 14:30:00', '2024-10-26 16:00:00'
FROM employees e
JOIN corporations c ON c.corp_name = '(주)빌드업건설'
JOIN managers m ON m.manager_name = '이현장'
WHERE e.emp_name = '정월급'
ON DUPLICATE KEY UPDATE contract_state = VALUES(contract_state);

-- ============================================
-- 6. 계약 상세 데이터 생성
-- ============================================

-- 6-1. 상용직 계약 상세 (시급 15,000원 → 월 209시간 기준)
INSERT INTO contract_details (
    contract_id, corp_name, emp_name, work_place, work_type,
    work_start_time, work_end_time, break_start_time, break_end_time,
    work_on_days, work_off_days, work_pay, 
    additional_hour_pay, additional_night_pay, additional_holiday_pay,
    pay_day, pay_period, pay_type,
    is_eoi_applicable, is_wci_applicable, is_nps_applicable, is_nhi_applicable,
    corp_address, corp_ceo_name, emp_address
)
SELECT 
    c.id, corp.corp_name, e.emp_name, '강남 아파트 신축공사', '건설 현장 관리',
    '09:00:00', '18:00:00', '12:00:00', '13:00:00',
    '월,화,수,목,금', '토,일', 15000.00,
    7500.00, 7500.00, 15000.00,
    10, 'MONTHLY', 'BANK_TRANSFER',
    TRUE, TRUE, TRUE, TRUE,
    corp.corp_address, corp.corp_ceo_name, e.emp_address
FROM contracts c
JOIN employees e ON c.employee_id = e.id
JOIN corporations corp ON c.corporation_id = corp.id
WHERE e.emp_name = '박상용'
ON DUPLICATE KEY UPDATE work_pay = VALUES(work_pay);

-- 6-2. 일용직 계약 상세 - 일급 (시급 12,000원)
INSERT INTO contract_details (
    contract_id, corp_name, emp_name, work_place, work_type,
    work_start_time, work_end_time, break_start_time, break_end_time,
    work_on_days, work_off_days, work_pay,
    additional_hour_pay, additional_night_pay, additional_holiday_pay,
    pay_day, pay_period, pay_type,
    is_eoi_applicable, is_wci_applicable, is_nps_applicable, is_nhi_applicable,
    corp_address, corp_ceo_name, emp_address
)
SELECT 
    c.id, corp.corp_name, e.emp_name, '강남 아파트 신축공사', '일반 노무',
    '08:00:00', '17:00:00', '12:00:00', '13:00:00',
    '월,화,수,목,금', '토,일', 12000.00,
    6000.00, 6000.00, 12000.00,
    5, 'DAILY', 'BANK_TRANSFER',
    FALSE, TRUE, FALSE, FALSE,
    corp.corp_address, corp.corp_ceo_name, e.emp_address
FROM contracts c
JOIN employees e ON c.employee_id = e.id
JOIN corporations corp ON c.corporation_id = corp.id
WHERE e.emp_name = '김일용'
ON DUPLICATE KEY UPDATE work_pay = VALUES(work_pay);

-- 6-3. 일용직 계약 상세 - 주급 (시급 13,000원)
INSERT INTO contract_details (
    contract_id, corp_name, emp_name, work_place, work_type,
    work_start_time, work_end_time, break_start_time, break_end_time,
    work_on_days, work_off_days, work_pay,
    additional_hour_pay, additional_night_pay, additional_holiday_pay,
    pay_day, pay_period, pay_type,
    is_eoi_applicable, is_wci_applicable, is_nps_applicable, is_nhi_applicable,
    corp_address, corp_ceo_name, emp_address
)
SELECT 
    c.id, corp.corp_name, e.emp_name, '강남 아파트 신축공사', '일반 노무',
    '08:00:00', '17:00:00', '12:00:00', '13:00:00',
    '월,화,수,목,금,토', '일', 13000.00,
    6500.00, 6500.00, 13000.00,
    10, 'WEEKLY', 'BANK_TRANSFER',
    FALSE, TRUE, FALSE, FALSE,
    corp.corp_address, corp.corp_ceo_name, e.emp_address
FROM contracts c
JOIN employees e ON c.employee_id = e.id
JOIN corporations corp ON c.corporation_id = corp.id
WHERE e.emp_name = '이주간'
ON DUPLICATE KEY UPDATE work_pay = VALUES(work_pay);

-- 6-4. 일용직 계약 상세 - 월급 (시급 14,000원)
INSERT INTO contract_details (
    contract_id, corp_name, emp_name, work_place, work_type,
    work_start_time, work_end_time, break_start_time, break_end_time,
    work_on_days, work_off_days, work_pay,
    additional_hour_pay, additional_night_pay, additional_holiday_pay,
    pay_day, pay_period, pay_type,
    is_eoi_applicable, is_wci_applicable, is_nps_applicable, is_nhi_applicable,
    corp_address, corp_ceo_name, emp_address
)
SELECT 
    c.id, corp.corp_name, e.emp_name, '강남 아파트 신축공사', '일반 노무',
    '08:00:00', '17:00:00', '12:00:00', '13:00:00',
    '월,화,수,목,금', '토,일', 14000.00,
    7000.00, 7000.00, 14000.00,
    25, 'MONTHLY', 'BANK_TRANSFER',
    FALSE, TRUE, FALSE, FALSE,
    corp.corp_address, corp.corp_ceo_name, e.emp_address
FROM contracts c
JOIN employees e ON c.employee_id = e.id
JOIN corporations corp ON c.corporation_id = corp.id
WHERE e.emp_name = '정월급'
ON DUPLICATE KEY UPDATE work_pay = VALUES(work_pay);

-- ============================================
-- 7. 출퇴근 기록 데이터 생성 (최근 30일)
-- ============================================

-- 7-1. 상용직 근로자 출퇴근 기록 (10월 전체)
INSERT INTO attendances (contract_id, employee_id, site_id, search_date, emp_type, emp_name, resident_num,
                        attendance_status, check_in_time, check_out_time, 
                        total_work_hour, night_work_hour, additional_work_hour, holiday_work_hour)
SELECT 
    c.id, e.id, s.id, DATE('2024-10-01') + INTERVAL n DAY, 'PERMANENT', e.emp_name, e.resident_num,
    'PRESENT', 
    TIMESTAMP(DATE('2024-10-01') + INTERVAL n DAY, '09:00:00'),
    TIMESTAMP(DATE('2024-10-01') + INTERVAL n DAY, '18:00:00'),
    8.00, 0.00, 0.00, 0.00
FROM contracts c
JOIN employees e ON c.employee_id = e.id AND e.emp_name = '박상용'
JOIN sites s ON s.site_name = '강남 아파트 신축공사'
JOIN (
    SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL
    SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10 UNION ALL SELECT 11 UNION ALL
    SELECT 14 UNION ALL SELECT 15 UNION ALL SELECT 16 UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL
    SELECT 21 UNION ALL SELECT 22 UNION ALL SELECT 23 UNION ALL SELECT 24 UNION ALL SELECT 25 UNION ALL
    SELECT 28 UNION ALL SELECT 29 UNION ALL SELECT 30
) days
WHERE DAYOFWEEK(DATE('2024-10-01') + INTERVAL n DAY) NOT IN (1, 7)  -- 토일 제외
ON DUPLICATE KEY UPDATE total_work_hour = VALUES(total_work_hour);

-- 7-2. 일용직(일급) 근로자 출퇴근 기록 (10월 전체)
INSERT INTO attendances (contract_id, employee_id, site_id, search_date, emp_type, emp_name, resident_num,
                        attendance_status, check_in_time, check_out_time,
                        total_work_hour, night_work_hour, additional_work_hour, holiday_work_hour)
SELECT 
    c.id, e.id, s.id, DATE('2024-10-01') + INTERVAL n DAY, 'DAILY', e.emp_name, e.resident_num,
    'PRESENT',
    TIMESTAMP(DATE('2024-10-01') + INTERVAL n DAY, '08:00:00'),
    TIMESTAMP(DATE('2024-10-01') + INTERVAL n DAY, '17:00:00'),
    8.00, 0.00, 1.00, 0.00
FROM contracts c
JOIN employees e ON c.employee_id = e.id AND e.emp_name = '김일용'
JOIN sites s ON s.site_name = '강남 아파트 신축공사'
JOIN (
    SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL
    SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10 UNION ALL SELECT 11 UNION ALL
    SELECT 14 UNION ALL SELECT 15 UNION ALL SELECT 16 UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL
    SELECT 21 UNION ALL SELECT 22 UNION ALL SELECT 23 UNION ALL SELECT 24 UNION ALL SELECT 25 UNION ALL
    SELECT 28 UNION ALL SELECT 29 UNION ALL SELECT 30
) days
WHERE DAYOFWEEK(DATE('2024-10-01') + INTERVAL n DAY) NOT IN (1, 7)  -- 토일 제외
ON DUPLICATE KEY UPDATE total_work_hour = VALUES(total_work_hour);

-- 7-3. 일용직(주급) 근로자 출퇴근 기록 (10월 전체)
INSERT INTO attendances (contract_id, employee_id, site_id, search_date, emp_type, emp_name, resident_num,
                        attendance_status, check_in_time, check_out_time,
                        total_work_hour, night_work_hour, additional_work_hour, holiday_work_hour)
SELECT 
    c.id, e.id, s.id, DATE('2024-10-01') + INTERVAL n DAY, 'DAILY', e.emp_name, e.resident_num,
    'PRESENT',
    TIMESTAMP(DATE('2024-10-01') + INTERVAL n DAY, '08:00:00'),
    TIMESTAMP(DATE('2024-10-01') + INTERVAL n DAY, '17:00:00'),
    8.00, 0.00, 0.00, 0.00
FROM contracts c
JOIN employees e ON c.employee_id = e.id AND e.emp_name = '이주간'
JOIN sites s ON s.site_name = '강남 아파트 신축공사'
JOIN (
    SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL
    SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10 UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL
    SELECT 14 UNION ALL SELECT 15 UNION ALL SELECT 16 UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19 UNION ALL
    SELECT 21 UNION ALL SELECT 22 UNION ALL SELECT 23 UNION ALL SELECT 24 UNION ALL SELECT 25 UNION ALL SELECT 26 UNION ALL
    SELECT 28 UNION ALL SELECT 29 UNION ALL SELECT 30
) days
WHERE DAYOFWEEK(DATE('2024-10-01') + INTERVAL n DAY) != 1  -- 일요일만 제외 (토요일 근무)
ON DUPLICATE KEY UPDATE total_work_hour = VALUES(total_work_hour);

-- 7-4. 일용직(월급) 근로자 출퇴근 기록 (10월 전체)
INSERT INTO attendances (contract_id, employee_id, site_id, search_date, emp_type, emp_name, resident_num,
                        attendance_status, check_in_time, check_out_time,
                        total_work_hour, night_work_hour, additional_work_hour, holiday_work_hour)
SELECT 
    c.id, e.id, s.id, DATE('2024-10-01') + INTERVAL n DAY, 'DAILY', e.emp_name, e.resident_num,
    'PRESENT',
    TIMESTAMP(DATE('2024-10-01') + INTERVAL n DAY, '08:00:00'),
    TIMESTAMP(DATE('2024-10-01') + INTERVAL n DAY, '17:00:00'),
    8.00, 0.00, 0.00, 0.00
FROM contracts c
JOIN employees e ON c.employee_id = e.id AND e.emp_name = '정월급'
JOIN sites s ON s.site_name = '강남 아파트 신축공사'
JOIN (
    SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL
    SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10 UNION ALL SELECT 11 UNION ALL
    SELECT 14 UNION ALL SELECT 15 UNION ALL SELECT 16 UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL
    SELECT 21 UNION ALL SELECT 22 UNION ALL SELECT 23 UNION ALL SELECT 24 UNION ALL SELECT 25 UNION ALL
    SELECT 28 UNION ALL SELECT 29 UNION ALL SELECT 30
) days
WHERE DAYOFWEEK(DATE('2024-10-01') + INTERVAL n DAY) NOT IN (1, 7)  -- 토일 제외
ON DUPLICATE KEY UPDATE total_work_hour = VALUES(total_work_hour);

-- ============================================
-- 데이터 삽입 완료 메시지
-- ============================================
SELECT '✅ 급여 생성 테스트 데이터 삽입 완료!' AS Status;
SELECT CONCAT('- 법인: ', COUNT(*), '개') AS Info FROM corporations;
SELECT CONCAT('- 관리자: ', COUNT(*), '명') AS Info FROM managers;
SELECT CONCAT('- 현장: ', COUNT(*), '개') AS Info FROM sites;
SELECT CONCAT('- 근로자: ', COUNT(*), '명 (상용직 1명, 일용직 3명)') AS Info FROM employees WHERE emp_name IN ('박상용', '김일용', '이주간', '정월급');
SELECT CONCAT('- 계약: ', COUNT(*), '건') AS Info FROM contracts WHERE employee_id IN (SELECT id FROM employees WHERE emp_name IN ('박상용', '김일용', '이주간', '정월급'));
SELECT CONCAT('- 출퇴근 기록: ', COUNT(*), '건 (2024년 10월 전체)') AS Info FROM attendances WHERE employee_id IN (SELECT id FROM employees WHERE emp_name IN ('박상용', '김일용', '이주간', '정월급'));

SELECT '
📋 생성된 테스트 데이터:
1. 박상용 (상용직) - 월급제, 시급 15,000원, 4대보험 전체 가입
2. 김일용 (일용직) - 일급제, 시급 12,000원, 산재보험만 가입
3. 이주간 (일용직) - 주급제, 시급 13,000원, 토요일 근무
4. 정월급 (일용직) - 월급제, 시급 14,000원

💡 테스트 방법:
GET /api/v1/payroll/test/all
→ 모든 급여 타입 생성 테스트

GET /api/v1/payroll/test/monthly-permanent
→ 상용직 월급만 테스트

GET /api/v1/payroll/test/daily
→ 일용직 일급만 테스트

GET /api/v1/payroll/test/weekly
→ 일용직 주급만 테스트

GET /api/v1/payroll/test/monthly-daily
→ 일용직 월급만 테스트
' AS Guide;

