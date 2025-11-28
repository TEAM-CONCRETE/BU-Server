-- ============================================
-- Build-Up 급여 생성 테스트 데이터 (완전판)
-- ============================================
-- 목적: 일급/주급/월급(일용직)/월급(상용직) 모든 급여 타입 테스트
-- 생성일: 2025-11-15
-- 특징: 모든 필수 필드 포함, zero date 없음
-- ============================================

USE buildup;

-- ============================================
-- 0. 기존 테스트 데이터 정리
-- ============================================
DELETE FROM payslip_items WHERE payroll_id IN (
    SELECT id FROM payrolls WHERE employee_id IN (
        SELECT id FROM employees WHERE emp_name IN ('박상용', '김일용', '이주간', '정월급')
    )
);
DELETE FROM payrolls WHERE employee_id IN (
    SELECT id FROM employees WHERE emp_name IN ('박상용', '김일용', '이주간', '정월급')
);
DELETE FROM attendances WHERE employee_id IN (
    SELECT id FROM employees WHERE emp_name IN ('박상용', '김일용', '이주간', '정월급')
);
DELETE FROM contract_details WHERE contract_id IN (
    SELECT id FROM contracts WHERE employee_id IN (
        SELECT id FROM employees WHERE emp_name IN ('박상용', '김일용', '이주간', '정월급')
    )
);
DELETE FROM contracts WHERE employee_id IN (
    SELECT id FROM employees WHERE emp_name IN ('박상용', '김일용', '이주간', '정월급')
);
DELETE FROM employees WHERE emp_name IN ('박상용', '김일용', '이주간', '정월급');
DELETE FROM users WHERE user_id IN ('emp001', 'emp002', 'emp003', 'emp004');

-- ============================================
-- 1. 근로자 생성 (4명)
-- ============================================

SET @role_emp = (SELECT id FROM roles WHERE role_name = 'ROLE_EMPLOYEE' LIMIT 1);
SET @corp_id = (SELECT id FROM corporations LIMIT 1);
SET @mgr_id = (SELECT id FROM managers LIMIT 1);
SET @site_id = (SELECT id FROM sites LIMIT 1);

-- 1-1. 박상용 (상용직 - 월급)
INSERT INTO users (user_id, password, phone, email, role_id, profile_completed, is_deleted, created_at, updated_at) 
VALUES ('emp001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z2EHdsDGF.fWVFR7A1tJDG3a', 
        '010-2001-0001', 'emp001@buildup.com', @role_emp, TRUE, FALSE, NOW(), NOW());

INSERT INTO employees (user_id, emp_name, sub_phone, resident_num, emp_address, emp_type, 
                      is_deleted, created_at, updated_at)
VALUES (LAST_INSERT_ID(), '박상용', '010-2001-0002', '900101-1234567', 
        '서울시 강남구 역삼동 123', 'PERMANENT', FALSE, NOW(), NOW());

SET @emp1_id = LAST_INSERT_ID();

-- 1-2. 김일용 (일용직 - 일급)
INSERT INTO users (user_id, password, phone, email, role_id, profile_completed, is_deleted, created_at, updated_at)
VALUES ('emp002', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z2EHdsDGF.fWVFR7A1tJDG3a',
        '010-2002-0001', 'emp002@buildup.com', @role_emp, TRUE, FALSE, NOW(), NOW());

INSERT INTO employees (user_id, emp_name, sub_phone, resident_num, emp_address, emp_type,
                      is_deleted, created_at, updated_at)
VALUES (LAST_INSERT_ID(), '김일용', '010-2002-0002', '850505-1234567',
        '서울시 송파구 문정동 456', 'DAILY', FALSE, NOW(), NOW());

SET @emp2_id = LAST_INSERT_ID();

-- 1-3. 이주간 (일용직 - 주급)
INSERT INTO users (user_id, password, phone, email, role_id, profile_completed, is_deleted, created_at, updated_at)
VALUES ('emp003', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z2EHdsDGF.fWVFR7A1tJDG3a',
        '010-2003-0001', 'emp003@buildup.com', @role_emp, TRUE, FALSE, NOW(), NOW());

INSERT INTO employees (user_id, emp_name, sub_phone, resident_num, emp_address, emp_type,
                      is_deleted, created_at, updated_at)
VALUES (LAST_INSERT_ID(), '이주간', '010-2003-0002', '920315-1234567',
        '서울시 서초구 반포동 789', 'DAILY', FALSE, NOW(), NOW());

SET @emp3_id = LAST_INSERT_ID();

-- 1-4. 정월급 (일용직 - 월급)
INSERT INTO users (user_id, password, phone, email, role_id, profile_completed, is_deleted, created_at, updated_at)
VALUES ('emp004', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z2EHdsDGF.fWVFR7A1tJDG3a',
        '010-2004-0001', 'emp004@buildup.com', @role_emp, TRUE, FALSE, NOW(), NOW());

INSERT INTO employees (user_id, emp_name, sub_phone, resident_num, emp_address, emp_type,
                      is_deleted, created_at, updated_at)
VALUES (LAST_INSERT_ID(), '정월급', '010-2004-0002', '880720-1234567',
        '서울시 강동구 천호동 321', 'DAILY', FALSE, NOW(), NOW());

SET @emp4_id = LAST_INSERT_ID();

-- ============================================
-- 2. 계약 생성
-- ============================================

-- 2-1. 박상용 계약 (상용직 월급)
INSERT INTO contracts (employee_id, corporation_id, manager_id, role, emp_type, contract_state,
                      employee_start_date, employee_end_date, 
                      written_at, corp_signed_at, emp_signed_at,
                      is_deleted, created_at, updated_at)
VALUES (@emp1_id, @corp_id, @mgr_id, '기술직', 'PERMANENT', 'FULLY_SIGNED',
        '2024-10-01', '2025-12-31',
        '2024-09-20 10:00:00', '2024-09-21 14:30:00', '2024-09-21 16:00:00',
        FALSE, NOW(), NOW());

SET @contract1_id = LAST_INSERT_ID();

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
    @contract1_id, '(주)빌드업건설', '박상용', '강남 아파트 신축공사', '건설 현장 관리',
    '09:00:00', '18:00:00', '12:00:00', '13:00:00',
    '월,화,수,목,금', '토,일', 15000.00,
    7500.00, 7500.00, 15000.00,
    10, 'MONTHLY', 'TRANSFER',
    TRUE, TRUE, TRUE, TRUE,
    '서울특별시 강남구 테헤란로 123', '김철수', '서울시 강남구 역삼동 123',
    FALSE, NOW(), NOW()
);

-- 2-2. 김일용 계약 (일용직 일급)
INSERT INTO contracts (employee_id, corporation_id, manager_id, role, emp_type, contract_state,
                      employee_start_date, employee_end_date,
                      written_at, corp_signed_at, emp_signed_at,
                      is_deleted, created_at, updated_at)
VALUES (@emp2_id, @corp_id, @mgr_id, '일반노무', 'DAILY', 'FULLY_SIGNED',
        '2024-11-01', '2025-12-31',
        '2024-10-25 10:00:00', '2024-10-26 14:30:00', '2024-10-26 16:00:00',
        FALSE, NOW(), NOW());

SET @contract2_id = LAST_INSERT_ID();

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
    @contract2_id, '(주)빌드업건설', '김일용', '강남 아파트 신축공사', '일반 노무',
    '08:00:00', '17:00:00', '12:00:00', '13:00:00',
    '월,화,수,목,금', '토,일', 12000.00,
    6000.00, 6000.00, 12000.00,
    5, 'DAILY', 'TRANSFER',
    FALSE, TRUE, FALSE, FALSE,
    '서울특별시 강남구 테헤란로 123', '김철수', '서울시 송파구 문정동 456',
    FALSE, NOW(), NOW()
);

-- 2-3. 이주간 계약 (일용직 주급)
INSERT INTO contracts (employee_id, corporation_id, manager_id, role, emp_type, contract_state,
                      employee_start_date, employee_end_date,
                      written_at, corp_signed_at, emp_signed_at,
                      is_deleted, created_at, updated_at)
VALUES (@emp3_id, @corp_id, @mgr_id, '일반노무', 'DAILY', 'FULLY_SIGNED',
        '2024-11-01', '2025-12-31',
        '2024-10-25 10:00:00', '2024-10-26 14:30:00', '2024-10-26 16:00:00',
        FALSE, NOW(), NOW());

SET @contract3_id = LAST_INSERT_ID();

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
    @contract3_id, '(주)빌드업건설', '이주간', '강남 아파트 신축공사', '일반 노무',
    '08:00:00', '17:00:00', '12:00:00', '13:00:00',
    '월,화,수,목,금,토', '일', 13000.00,
    6500.00, 6500.00, 13000.00,
    10, 'WEEKLY', 'TRANSFER',
    FALSE, TRUE, FALSE, FALSE,
    '서울특별시 강남구 테헤란로 123', '김철수', '서울시 서초구 반포동 789',
    FALSE, NOW(), NOW()
);

-- 2-4. 정월급 계약 (일용직 월급)
INSERT INTO contracts (employee_id, corporation_id, manager_id, role, emp_type, contract_state,
                      employee_start_date, employee_end_date,
                      written_at, corp_signed_at, emp_signed_at,
                      is_deleted, created_at, updated_at)
VALUES (@emp4_id, @corp_id, @mgr_id, '일반노무', 'DAILY', 'FULLY_SIGNED',
        '2024-11-01', '2025-12-31',
        '2024-10-25 10:00:00', '2024-10-26 14:30:00', '2024-10-26 16:00:00',
        FALSE, NOW(), NOW());

SET @contract4_id = LAST_INSERT_ID();

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
    @contract4_id, '(주)빌드업건설', '정월급', '강남 아파트 신축공사', '일반 노무',
    '08:00:00', '17:00:00', '12:00:00', '13:00:00',
    '월,화,수,목,금', '토,일', 14000.00,
    7000.00, 7000.00, 14000.00,
    25, 'MONTHLY', 'TRANSFER',
    FALSE, TRUE, FALSE, FALSE,
    '서울특별시 강남구 테헤란로 123', '김철수', '서울시 강동구 천호동 321',
    FALSE, NOW(), NOW()
);

-- ============================================
-- 3. 출퇴근 기록 생성
-- ============================================

-- 3-1. 2025년 10월 출퇴근 기록 (월급 테스트용 - 모든 근로자)
-- 박상용 (월~금, 23일)
INSERT INTO attendances (contract_id, employee_id, site_id, search_date, emp_type, emp_name, resident_num,
                        attendance_status, check_in_time, check_out_time,
                        total_work_hour, night_work_hour, additional_work_hour, holiday_work_hour,
                        is_deleted, created_at, updated_at)
VALUES
    (@contract1_id, @emp1_id, @site_id, '2025-10-01', 'PERMANENT', '박상용', '900101-1234567', 'NORMAL', '2025-10-01 09:00:00', '2025-10-01 18:00:00', 8.00, 0.00, 0.00, 0.00, FALSE, NOW(), NOW()),
    (@contract1_id, @emp1_id, @site_id, '2025-10-02', 'PERMANENT', '박상용', '900101-1234567', 'NORMAL', '2025-10-02 09:00:00', '2025-10-02 18:00:00', 8.00, 0.00, 0.00, 0.00, FALSE, NOW(), NOW()),
    (@contract1_id, @emp1_id, @site_id, '2025-10-03', 'PERMANENT', '박상용', '900101-1234567', 'NORMAL', '2025-10-03 09:00:00', '2025-10-03 18:00:00', 8.00, 0.00, 0.00, 0.00, FALSE, NOW(), NOW()),
    (@contract1_id, @emp1_id, @site_id, '2025-10-06', 'PERMANENT', '박상용', '900101-1234567', 'NORMAL', '2025-10-06 09:00:00', '2025-10-06 18:00:00', 8.00, 0.00, 0.00, 0.00, FALSE, NOW(), NOW()),
    (@contract1_id, @emp1_id, @site_id, '2025-10-07', 'PERMANENT', '박상용', '900101-1234567', 'NORMAL', '2025-10-07 09:00:00', '2025-10-07 18:00:00', 8.00, 0.00, 0.00, 0.00, FALSE, NOW(), NOW()),
    (@contract1_id, @emp1_id, @site_id, '2025-10-08', 'PERMANENT', '박상용', '900101-1234567', 'NORMAL', '2025-10-08 09:00:00', '2025-10-08 18:00:00', 8.00, 0.00, 0.00, 0.00, FALSE, NOW(), NOW()),
    (@contract1_id, @emp1_id, @site_id, '2025-10-10', 'PERMANENT', '박상용', '900101-1234567', 'NORMAL', '2025-10-10 09:00:00', '2025-10-10 18:00:00', 8.00, 0.00, 0.00, 0.00, FALSE, NOW(), NOW()),
    (@contract1_id, @emp1_id, @site_id, '2025-10-13', 'PERMANENT', '박상용', '900101-1234567', 'NORMAL', '2025-10-13 09:00:00', '2025-10-13 18:00:00', 8.00, 0.00, 0.00, 0.00, FALSE, NOW(), NOW()),
    (@contract1_id, @emp1_id, @site_id, '2025-10-14', 'PERMANENT', '박상용', '900101-1234567', 'NORMAL', '2025-10-14 09:00:00', '2025-10-14 18:00:00', 8.00, 0.00, 0.00, 0.00, FALSE, NOW(), NOW()),
    (@contract1_id, @emp1_id, @site_id, '2025-10-15', 'PERMANENT', '박상용', '900101-1234567', 'NORMAL', '2025-10-15 09:00:00', '2025-10-15 18:00:00', 8.00, 0.00, 0.00, 0.00, FALSE, NOW(), NOW()),
    (@contract1_id, @emp1_id, @site_id, '2025-10-16', 'PERMANENT', '박상용', '900101-1234567', 'NORMAL', '2025-10-16 09:00:00', '2025-10-16 18:00:00', 8.00, 0.00, 0.00, 0.00, FALSE, NOW(), NOW()),
    (@contract1_id, @emp1_id, @site_id, '2025-10-17', 'PERMANENT', '박상용', '900101-1234567', 'NORMAL', '2025-10-17 09:00:00', '2025-10-17 18:00:00', 8.00, 0.00, 0.00, 0.00, FALSE, NOW(), NOW()),
    (@contract1_id, @emp1_id, @site_id, '2025-10-20', 'PERMANENT', '박상용', '900101-1234567', 'NORMAL', '2025-10-20 09:00:00', '2025-10-20 18:00:00', 8.00, 0.00, 0.00, 0.00, FALSE, NOW(), NOW()),
    (@contract1_id, @emp1_id, @site_id, '2025-10-21', 'PERMANENT', '박상용', '900101-1234567', 'NORMAL', '2025-10-21 09:00:00', '2025-10-21 18:00:00', 8.00, 0.00, 0.00, 0.00, FALSE, NOW(), NOW()),
    (@contract1_id, @emp1_id, @site_id, '2025-10-22', 'PERMANENT', '박상용', '900101-1234567', 'NORMAL', '2025-10-22 09:00:00', '2025-10-22 18:00:00', 8.00, 0.00, 0.00, 0.00, FALSE, NOW(), NOW()),
    (@contract1_id, @emp1_id, @site_id, '2025-10-23', 'PERMANENT', '박상용', '900101-1234567', 'NORMAL', '2025-10-23 09:00:00', '2025-10-23 18:00:00', 8.00, 0.00, 0.00, 0.00, FALSE, NOW(), NOW()),
    (@contract1_id, @emp1_id, @site_id, '2025-10-24', 'PERMANENT', '박상용', '900101-1234567', 'NORMAL', '2025-10-24 09:00:00', '2025-10-24 18:00:00', 8.00, 0.00, 0.00, 0.00, FALSE, NOW(), NOW()),
    (@contract1_id, @emp1_id, @site_id, '2025-10-27', 'PERMANENT', '박상용', '900101-1234567', 'NORMAL', '2025-10-27 09:00:00', '2025-10-27 18:00:00', 8.00, 0.00, 0.00, 0.00, FALSE, NOW(), NOW()),
    (@contract1_id, @emp1_id, @site_id, '2025-10-28', 'PERMANENT', '박상용', '900101-1234567', 'NORMAL', '2025-10-28 09:00:00', '2025-10-28 18:00:00', 8.00, 0.00, 0.00, 0.00, FALSE, NOW(), NOW()),
    (@contract1_id, @emp1_id, @site_id, '2025-10-29', 'PERMANENT', '박상용', '900101-1234567', 'NORMAL', '2025-10-29 09:00:00', '2025-10-29 18:00:00', 8.00, 0.00, 0.00, 0.00, FALSE, NOW(), NOW()),
    (@contract1_id, @emp1_id, @site_id, '2025-10-30', 'PERMANENT', '박상용', '900101-1234567', 'NORMAL', '2025-10-30 09:00:00', '2025-10-30 18:00:00', 8.00, 0.00, 0.00, 0.00, FALSE, NOW(), NOW()),
    (@contract1_id, @emp1_id, @site_id, '2025-10-31', 'PERMANENT', '박상용', '900101-1234567', 'NORMAL', '2025-10-31 09:00:00', '2025-10-31 18:00:00', 8.00, 0.00, 0.00, 0.00, FALSE, NOW(), NOW());

-- 김일용, 이주간, 정월급 (2025-10월, 23일 근무)
INSERT INTO attendances (contract_id, employee_id, site_id, search_date, emp_type, emp_name, resident_num,
                        attendance_status, check_in_time, check_out_time,
                        total_work_hour, night_work_hour, additional_work_hour, holiday_work_hour,
                        is_deleted, created_at, updated_at)
SELECT c.id, e.id, @site_id, date_val, 'DAILY', e.emp_name, e.resident_num,
       'NORMAL', TIMESTAMP(date_val, '08:00:00'), TIMESTAMP(date_val, '17:00:00'),
       8.00, 0.00, 0.50, 0.00, FALSE, NOW(), NOW()
FROM employees e
JOIN contracts c ON e.id = c.employee_id
CROSS JOIN (
    SELECT '2025-10-01' as date_val UNION ALL SELECT '2025-10-02' UNION ALL SELECT '2025-10-03' UNION ALL
    SELECT '2025-10-06' UNION ALL SELECT '2025-10-07' UNION ALL SELECT '2025-10-08' UNION ALL SELECT '2025-10-10' UNION ALL
    SELECT '2025-10-13' UNION ALL SELECT '2025-10-14' UNION ALL SELECT '2025-10-15' UNION ALL SELECT '2025-10-16' UNION ALL SELECT '2025-10-17' UNION ALL
    SELECT '2025-10-20' UNION ALL SELECT '2025-10-21' UNION ALL SELECT '2025-10-22' UNION ALL SELECT '2025-10-23' UNION ALL SELECT '2025-10-24' UNION ALL
    SELECT '2025-10-27' UNION ALL SELECT '2025-10-28' UNION ALL SELECT '2025-10-29' UNION ALL SELECT '2025-10-30' UNION ALL SELECT '2025-10-31'
) dates
WHERE e.emp_name IN ('김일용', '정월급');

-- 3-2. 일급 테스트용: 어제(2025-11-14) 출퇴근 기록 - 김일용
INSERT INTO attendances (contract_id, employee_id, site_id, search_date, emp_type, emp_name, resident_num,
                        attendance_status, check_in_time, check_out_time,
                        total_work_hour, night_work_hour, additional_work_hour, holiday_work_hour,
                        is_deleted, created_at, updated_at)
VALUES (@contract2_id, @emp2_id, @site_id, '2025-11-14', 'DAILY', '김일용', '850505-1234567',
        'NORMAL', '2025-11-14 08:00:00', '2025-11-14 17:00:00',
        8.00, 0.00, 1.00, 0.00, FALSE, NOW(), NOW());

-- 3-3. 주급 테스트용: 지난 주(2025-11-04 ~ 2025-11-09) 출퇴근 기록 - 이주간
INSERT INTO attendances (contract_id, employee_id, site_id, search_date, emp_type, emp_name, resident_num,
                        attendance_status, check_in_time, check_out_time,
                        total_work_hour, night_work_hour, additional_work_hour, holiday_work_hour,
                        is_deleted, created_at, updated_at)
SELECT @contract3_id, @emp3_id, @site_id, date_val, 'DAILY', '이주간', '920315-1234567',
       'NORMAL', TIMESTAMP(date_val, '08:00:00'), TIMESTAMP(date_val, '17:00:00'),
       8.00, 0.00, 0.00, 0.00, FALSE, NOW(), NOW()
FROM (
    SELECT '2025-11-04' as date_val UNION ALL SELECT '2025-11-05' UNION ALL
    SELECT '2025-11-06' UNION ALL SELECT '2025-11-07' UNION ALL
    SELECT '2025-11-08' UNION ALL SELECT '2025-11-09'
) dates;

-- 3-4. 이주간 10월 데이터 추가 (토요일 근무 포함)
INSERT INTO attendances (contract_id, employee_id, site_id, search_date, emp_type, emp_name, resident_num,
                        attendance_status, check_in_time, check_out_time,
                        total_work_hour, night_work_hour, additional_work_hour, holiday_work_hour,
                        is_deleted, created_at, updated_at)
SELECT @contract3_id, @emp3_id, @site_id, date_val, 'DAILY', '이주간', '920315-1234567',
       'NORMAL', TIMESTAMP(date_val, '08:00:00'), TIMESTAMP(date_val, '17:00:00'),
       8.00, 0.00, 0.00, 0.00, FALSE, NOW(), NOW()
FROM (
    SELECT '2025-10-01' as date_val UNION ALL SELECT '2025-10-02' UNION ALL SELECT '2025-10-03' UNION ALL SELECT '2025-10-04' UNION ALL
    SELECT '2025-10-06' UNION ALL SELECT '2025-10-07' UNION ALL SELECT '2025-10-08' UNION ALL SELECT '2025-10-10' UNION ALL SELECT '2025-10-11' UNION ALL
    SELECT '2025-10-13' UNION ALL SELECT '2025-10-14' UNION ALL SELECT '2025-10-15' UNION ALL SELECT '2025-10-16' UNION ALL SELECT '2025-10-17' UNION ALL SELECT '2025-10-18' UNION ALL
    SELECT '2025-10-20' UNION ALL SELECT '2025-10-21' UNION ALL SELECT '2025-10-22' UNION ALL SELECT '2025-10-23' UNION ALL SELECT '2025-10-24' UNION ALL SELECT '2025-10-25' UNION ALL
    SELECT '2025-10-27' UNION ALL SELECT '2025-10-28' UNION ALL SELECT '2025-10-29' UNION ALL SELECT '2025-10-30' UNION ALL SELECT '2025-10-31'
) dates;

-- ============================================
-- 4. 완료 메시지
-- ============================================
SELECT '✅ 급여 생성 테스트 데이터 생성 완료!' AS Status;
SELECT CONCAT('근로자: ', COUNT(*), '명') AS Info FROM employees WHERE emp_name IN ('박상용', '김일용', '이주간', '정월급');
SELECT CONCAT('계약: ', COUNT(*), '건') AS Info FROM contracts WHERE employee_id IN (SELECT id FROM employees WHERE emp_name IN ('박상용', '김일용', '이주간', '정월급'));
SELECT CONCAT('출퇴근 기록: ', COUNT(*), '건') AS Info FROM attendances WHERE employee_id IN (SELECT id FROM employees WHERE emp_name IN ('박상용', '김일용', '이주간', '정월급'));

SELECT '
📋 생성된 테스트 데이터 요약:

1. 박상용 (상용직) - 월급제, 시급 15,000원, 4대보험 전체 가입
   - 2025년 10월: 22일 근무 (월급 테스트용)

2. 김일용 (일용직) - 일급제, 시급 12,000원, 산재보험만 가입
   - 2025년 10월: 22일 근무 (월급 비교용)
   - 2025년 11월 14일: 1일 근무 (일급 테스트용) ✓

3. 이주간 (일용직) - 주급제, 시급 13,000원, 토요일 근무
   - 2025년 10월: 26일 근무 (월급 비교용)
   - 2025년 11월 4~9일: 6일 근무 (주급 테스트용) ✓

4. 정월급 (일용직) - 월급제, 시급 14,000원
   - 2025년 10월: 22일 근무 (월급 테스트용)

💡 테스트 방법:
GET /api/v1/payroll/test/all          → 모든 급여 타입 생성
GET /api/v1/payroll/test/daily         → 일급만 생성 (김일용)
GET /api/v1/payroll/test/weekly        → 주급만 생성 (이주간)  
GET /api/v1/payroll/test/monthly-daily → 일용직 월급만 생성 (정월급)
GET /api/v1/payroll/test/monthly-permanent → 상용직 월급만 생성 (박상용)

✨ 모든 필드 완비, zero date 없음, 즉시 테스트 가능!
' AS Guide;

