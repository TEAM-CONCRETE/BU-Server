-- ============================================
-- EC2 Test Data (English Version)
-- ============================================
-- Password: Admin123!@
-- Hash: $2a$10$d48wVDxrG/Paw.ULUn5.ae0IGrOu5415tyKW24RR5lKmqQcdgLdoy
-- ============================================

USE buildup;

SET @pw_hash = '$2a$10$d48wVDxrG/Paw.ULUn5.ae0IGrOu5415tyKW24RR5lKmqQcdgLdoy';

-- Fix existing corporations (id=10, 11)
UPDATE corporations SET
    corp_name = 'Hangang Construction',
    corp_address = 'Seoul Yeongdeungpo-gu 108',
    corp_ceo_name = 'Park Hangang'
WHERE id = 10;

UPDATE corporations SET
    corp_name = 'Namsan Construction',
    corp_address = 'Seoul Jung-gu 100',
    corp_ceo_name = 'Kim Namsan'
WHERE id = 11;

-- ============================================
-- 2. Site Managers
-- ============================================

-- Hangang Manager 1 (Gangnam Site)
INSERT INTO users (user_id, password, phone, email, role_id, profile_completed, created_at, updated_at)
VALUES ('hg_manager01', @pw_hash, '010-5501-0001', 'hg_mgr01@buildup.com', 2, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO managers (user_id, manager_name, is_deleted, created_at, updated_at)
VALUES ((SELECT id FROM users WHERE user_id = 'hg_manager01'), 'Lee Gangnam', 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- Hangang Manager 2 (Songpa Site)
INSERT INTO users (user_id, password, phone, email, role_id, profile_completed, created_at, updated_at)
VALUES ('hg_manager02', @pw_hash, '010-5502-0001', 'hg_mgr02@buildup.com', 2, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO managers (user_id, manager_name, is_deleted, created_at, updated_at)
VALUES ((SELECT id FROM users WHERE user_id = 'hg_manager02'), 'Park Songpa', 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- Namsan Manager 1 (Jongro Site)
INSERT INTO users (user_id, password, phone, email, role_id, profile_completed, created_at, updated_at)
VALUES ('ns_manager01', @pw_hash, '010-7701-0001', 'ns_mgr01@buildup.com', 2, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO managers (user_id, manager_name, is_deleted, created_at, updated_at)
VALUES ((SELECT id FROM users WHERE user_id = 'ns_manager01'), 'Jung Jongro', 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- ============================================
-- 3. Sites
-- ============================================

-- Hangang Site 1: Gangnam Office
INSERT INTO sites (site_name, site_address, client_name, start_date, end_date, corporation_id, manager_id, manager_secret_key, employee_secret_key, is_deleted, created_at, updated_at)
VALUES (
    'Gangnam Office Building',
    'Seoul Gangnam-gu Yeoksam 123',
    'Gangnam Developer Co.',
    '2025-01-01',
    '2026-12-31',
    10,
    (SELECT id FROM managers WHERE manager_name = 'Lee Gangnam'),
    'CONC-HG01-MGR-2025',
    'CONC-HG01-EMP-2025',
    0, NOW(), NOW()
)
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- Hangang Site 2: Songpa Apartment
INSERT INTO sites (site_name, site_address, client_name, start_date, end_date, corporation_id, manager_id, manager_secret_key, employee_secret_key, is_deleted, created_at, updated_at)
VALUES (
    'Songpa Apartment Remodel',
    'Seoul Songpa-gu Jamsil 456',
    'Songpa Housing Assoc.',
    '2025-03-01',
    '2025-12-31',
    10,
    (SELECT id FROM managers WHERE manager_name = 'Park Songpa'),
    'CONC-HG02-MGR-2025',
    'CONC-HG02-EMP-2025',
    0, NOW(), NOW()
)
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- Namsan Site 1: Jongro Building
INSERT INTO sites (site_name, site_address, client_name, start_date, end_date, corporation_id, manager_id, manager_secret_key, employee_secret_key, is_deleted, created_at, updated_at)
VALUES (
    'Jongro Building Repair',
    'Seoul Jongro-gu Jongro3 100',
    'Jongro Building Mgmt.',
    '2025-04-01',
    '2025-09-30',
    11,
    (SELECT id FROM managers WHERE manager_name = 'Jung Jongro'),
    'CONC-NS01-MGR-2025',
    'CONC-NS01-EMP-2025',
    0, NOW(), NOW()
)
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- ============================================
-- 4. Employees
-- ============================================

-- Gangnam Site Employees
INSERT INTO users (user_id, password, phone, email, role_id, profile_completed, created_at, updated_at)
VALUES ('hg_emp_gn01', @pw_hash, '010-6601-0001', 'gn_emp01@test.com', 3, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO employees (user_id, emp_name, sub_phone, emp_address, emp_type, is_deleted, created_at, updated_at)
VALUES ((SELECT id FROM users WHERE user_id = 'hg_emp_gn01'), 'Kang Worker1', '010-6601-0002', 'Seoul Gangnam', 'PERMANENT', 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO users (user_id, password, phone, email, role_id, profile_completed, created_at, updated_at)
VALUES ('hg_emp_gn02', @pw_hash, '010-6602-0001', 'gn_emp02@test.com', 3, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO employees (user_id, emp_name, sub_phone, emp_address, emp_type, is_deleted, created_at, updated_at)
VALUES ((SELECT id FROM users WHERE user_id = 'hg_emp_gn02'), 'Nam Worker2', '010-6602-0002', 'Seoul Gangnam', 'DAILY', 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- Songpa Site Employees
INSERT INTO users (user_id, password, phone, email, role_id, profile_completed, created_at, updated_at)
VALUES ('hg_emp_sp01', @pw_hash, '010-6701-0001', 'sp_emp01@test.com', 3, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO employees (user_id, emp_name, sub_phone, emp_address, emp_type, is_deleted, created_at, updated_at)
VALUES ((SELECT id FROM users WHERE user_id = 'hg_emp_sp01'), 'Song Worker1', '010-6701-0002', 'Seoul Songpa', 'PERMANENT', 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- Jongro Site Employees
INSERT INTO users (user_id, password, phone, email, role_id, profile_completed, created_at, updated_at)
VALUES ('ns_emp_jr01', @pw_hash, '010-7801-0001', 'jr_emp01@test.com', 3, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO employees (user_id, emp_name, sub_phone, emp_address, emp_type, is_deleted, created_at, updated_at)
VALUES ((SELECT id FROM users WHERE user_id = 'ns_emp_jr01'), 'Jong Worker1', '010-7801-0002', 'Seoul Jongro', 'PERMANENT', 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO users (user_id, password, phone, email, role_id, profile_completed, created_at, updated_at)
VALUES ('ns_emp_jr02', @pw_hash, '010-7802-0001', 'jr_emp02@test.com', 3, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO employees (user_id, emp_name, sub_phone, emp_address, emp_type, is_deleted, created_at, updated_at)
VALUES ((SELECT id FROM users WHERE user_id = 'ns_emp_jr02'), 'Ro Worker2', '010-7802-0002', 'Seoul Jongro', 'DAILY', 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- ============================================
-- 5. Contracts
-- ============================================

-- Gangnam Site Contracts
INSERT INTO contracts (employee_id, corporation_id, manager_id, role, emp_type, contract_state,
                      employee_start_date, employee_end_date, written_at, corp_signed_at, emp_signed_at,
                      is_deleted, created_at, updated_at)
SELECT
    e.id,
    10,
    (SELECT id FROM managers WHERE manager_name = 'Lee Gangnam'),
    CASE WHEN e.emp_type = 'PERMANENT' THEN 'Site Engineer' ELSE 'General Labor' END,
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

-- Songpa Site Contracts
INSERT INTO contracts (employee_id, corporation_id, manager_id, role, emp_type, contract_state,
                      employee_start_date, employee_end_date, written_at, corp_signed_at, emp_signed_at,
                      is_deleted, created_at, updated_at)
SELECT
    e.id,
    10,
    (SELECT id FROM managers WHERE manager_name = 'Park Songpa'),
    CASE WHEN e.emp_type = 'PERMANENT' THEN 'Site Engineer' ELSE 'General Labor' END,
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

-- Jongro Site Contracts
INSERT INTO contracts (employee_id, corporation_id, manager_id, role, emp_type, contract_state,
                      employee_start_date, employee_end_date, written_at, corp_signed_at, emp_signed_at,
                      is_deleted, created_at, updated_at)
SELECT
    e.id,
    11,
    (SELECT id FROM managers WHERE manager_name = 'Jung Jongro'),
    CASE WHEN e.emp_type = 'PERMANENT' THEN 'Site Engineer' ELSE 'General Labor' END,
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
-- 6. Contract Details
-- ============================================

-- Gangnam Site Contract Details
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
    'Hangang Construction',
    e.emp_name,
    'Gangnam Office Building',
    CASE WHEN e.emp_type = 'PERMANENT' THEN 'Site Management' ELSE 'General Labor' END,
    '08:00:00', '17:00:00', '12:00:00', '13:00:00',
    'Mon,Tue,Wed,Thu,Fri', 'Sat,Sun',
    CASE WHEN e.emp_type = 'PERMANENT' THEN 18000.00 ELSE 15000.00 END,
    9000.00, 9000.00, 18000.00,
    10,
    CASE WHEN e.emp_type = 'PERMANENT' THEN 'MONTHLY' ELSE 'DAILY' END,
    'TRANSFER',
    TRUE, TRUE, TRUE, TRUE,
    'Seoul Yeongdeungpo-gu 108', 'Park Hangang', e.emp_address,
    0, NOW(), NOW()
FROM contracts c
JOIN employees e ON c.employee_id = e.id
JOIN users u ON e.user_id = u.id
WHERE u.user_id LIKE 'hg_emp_gn%'
ON DUPLICATE KEY UPDATE contract_id = VALUES(contract_id);

-- Songpa Site Contract Details
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
    'Hangang Construction',
    e.emp_name,
    'Songpa Apartment Remodel',
    CASE WHEN e.emp_type = 'PERMANENT' THEN 'Site Management' ELSE 'General Labor' END,
    '09:00:00', '18:00:00', '12:00:00', '13:00:00',
    'Mon,Tue,Wed,Thu,Fri', 'Sat,Sun',
    CASE WHEN e.emp_type = 'PERMANENT' THEN 17000.00 ELSE 14000.00 END,
    8500.00, 8500.00, 17000.00,
    15,
    CASE WHEN e.emp_type = 'PERMANENT' THEN 'MONTHLY' ELSE 'WEEKLY' END,
    'TRANSFER',
    TRUE, TRUE, TRUE, TRUE,
    'Seoul Yeongdeungpo-gu 108', 'Park Hangang', e.emp_address,
    0, NOW(), NOW()
FROM contracts c
JOIN employees e ON c.employee_id = e.id
JOIN users u ON e.user_id = u.id
WHERE u.user_id LIKE 'hg_emp_sp%'
ON DUPLICATE KEY UPDATE contract_id = VALUES(contract_id);

-- Jongro Site Contract Details
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
    'Namsan Construction',
    e.emp_name,
    'Jongro Building Repair',
    CASE WHEN e.emp_type = 'PERMANENT' THEN 'Site Management' ELSE 'General Labor' END,
    '09:00:00', '18:00:00', '12:00:00', '13:00:00',
    'Mon,Tue,Wed,Thu,Fri', 'Sat,Sun',
    CASE WHEN e.emp_type = 'PERMANENT' THEN 15000.00 ELSE 12000.00 END,
    7500.00, 7500.00, 15000.00,
    10,
    CASE WHEN e.emp_type = 'PERMANENT' THEN 'MONTHLY' ELSE 'DAILY' END,
    'TRANSFER',
    TRUE, TRUE, TRUE, TRUE,
    'Seoul Jung-gu 100', 'Kim Namsan', e.emp_address,
    0, NOW(), NOW()
FROM contracts c
JOIN employees e ON c.employee_id = e.id
JOIN users u ON e.user_id = u.id
WHERE u.user_id LIKE 'ns_emp_jr%'
ON DUPLICATE KEY UPDATE contract_id = VALUES(contract_id);

-- ============================================
-- 7. Payrolls
-- ============================================

-- Gangnam Site Payroll (Nov 2025)
INSERT INTO payrolls (employee_id, site_id, contract_id, corporation_id, salary_year, salary_month, salary_week, salary_day, search_date,
                     emp_type, emp_name, pay_cycle, total_pay, income_tax, resident_tax, pay_status, is_deleted, created_at, updated_at)
SELECT
    e.id,
    (SELECT id FROM sites WHERE site_name = 'Gangnam Office Building'),
    c.id,
    10,
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

-- Songpa Site Payroll (Nov 2025)
INSERT INTO payrolls (employee_id, site_id, contract_id, corporation_id, salary_year, salary_month, salary_week, salary_day, search_date,
                     emp_type, emp_name, pay_cycle, total_pay, income_tax, resident_tax, pay_status, is_deleted, created_at, updated_at)
SELECT
    e.id,
    (SELECT id FROM sites WHERE site_name = 'Songpa Apartment Remodel'),
    c.id,
    10,
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

-- Jongro Site Payroll (Nov 2025)
INSERT INTO payrolls (employee_id, site_id, contract_id, corporation_id, salary_year, salary_month, salary_week, salary_day, search_date,
                     emp_type, emp_name, pay_cycle, total_pay, income_tax, resident_tax, pay_status, is_deleted, created_at, updated_at)
SELECT
    e.id,
    (SELECT id FROM sites WHERE site_name = 'Jongro Building Repair'),
    c.id,
    11,
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
-- 8. Payslip Items
-- ============================================

-- Gangnam Payslip Items
INSERT INTO payslip_items (payroll_id, item_name, amount, item_type, is_deleted, created_at, updated_at)
SELECT p.id, 'Base Salary',
    CASE WHEN e.emp_type = 'PERMANENT' THEN 3000000.00 ELSE 2400000.00 END,
    'EARNING', 0, NOW(), NOW()
FROM payrolls p
JOIN employees e ON p.employee_id = e.id
JOIN users u ON e.user_id = u.id
WHERE u.user_id LIKE 'hg_emp_gn%' AND p.salary_month = 11
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO payslip_items (payroll_id, item_name, amount, item_type, is_deleted, created_at, updated_at)
SELECT p.id, 'Overtime Pay',
    CASE WHEN e.emp_type = 'PERMANENT' THEN 300000.00 ELSE 250000.00 END,
    'EARNING', 0, NOW(), NOW()
FROM payrolls p
JOIN employees e ON p.employee_id = e.id
JOIN users u ON e.user_id = u.id
WHERE u.user_id LIKE 'hg_emp_gn%' AND p.salary_month = 11
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO payslip_items (payroll_id, item_name, amount, item_type, is_deleted, created_at, updated_at)
SELECT p.id, 'Employment Insurance', 27000.00, 'DEDUCTION', 0, NOW(), NOW()
FROM payrolls p
JOIN employees e ON p.employee_id = e.id
JOIN users u ON e.user_id = u.id
WHERE u.user_id LIKE 'hg_emp_gn%' AND p.salary_month = 11
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- ============================================
-- 9. Attendance Records
-- ============================================

-- Gangnam Site Attendance (Today)
INSERT INTO attendance_records (employee_id, site_id, attendance_type, timestamp, state, captured_face_image_url, is_deleted, created_at, updated_at)
SELECT
    e.id,
    (SELECT id FROM sites WHERE site_name = 'Gangnam Office Building'),
    'CHECK_IN',
    CONCAT(CURDATE(), ' 08:05:00'),
    'CONFIRMED',
    'https://buildup-bucket.s3.ap-northeast-2.amazonaws.com/attendance/test-face.jpg',
    0, NOW(), NOW()
FROM employees e
JOIN users u ON e.user_id = u.id
WHERE u.user_id LIKE 'hg_emp_gn%'
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- Songpa Site Attendance (Today)
INSERT INTO attendance_records (employee_id, site_id, attendance_type, timestamp, state, captured_face_image_url, is_deleted, created_at, updated_at)
SELECT
    e.id,
    (SELECT id FROM sites WHERE site_name = 'Songpa Apartment Remodel'),
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
-- Done
-- ============================================
SELECT 'Test data created successfully!' AS Status;
