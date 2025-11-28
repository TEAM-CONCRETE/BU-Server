-- Dashboard Test Data for Site 179 (Manager ID 199)
-- BCrypt hash for Admin123!@: $2a$10$d48wVDxrG/Paw.ULUn5.ae0IGrOu5415tyKW24RR5lKmqQcdgLdoy

-- Clean up existing test data
DELETE FROM attendance_records WHERE site_id = 179;
DELETE FROM contract_details WHERE contract_id IN (SELECT id FROM contracts WHERE manager_id = 199);
DELETE FROM contracts WHERE manager_id = 199;
DELETE FROM safety_education_logs WHERE site_id = 179;
DELETE FROM employees WHERE user_id IN (SELECT id FROM users WHERE user_id LIKE 'emptest%');
DELETE FROM users WHERE user_id LIKE 'emptest%';

-- 1. Create Employees (근로자)
-- Employee Users
INSERT INTO users (user_id, password, phone, email, role_id, is_deleted, profile_completed, created_at, updated_at)
VALUES
('emptest001', '$2a$10$d48wVDxrG/Paw.ULUn5.ae0IGrOu5415tyKW24RR5lKmqQcdgLdoy', '010-1111-2222', 'emp001@test.com', 3, 0, 1, NOW(), NOW()),
('emptest002', '$2a$10$d48wVDxrG/Paw.ULUn5.ae0IGrOu5415tyKW24RR5lKmqQcdgLdoy', '010-2222-3333', 'emp002@test.com', 3, 0, 1, NOW(), NOW()),
('emptest003', '$2a$10$d48wVDxrG/Paw.ULUn5.ae0IGrOu5415tyKW24RR5lKmqQcdgLdoy', '010-3333-4444', 'emp003@test.com', 3, 0, 1, NOW(), NOW()),
('emptest004', '$2a$10$d48wVDxrG/Paw.ULUn5.ae0IGrOu5415tyKW24RR5lKmqQcdgLdoy', '010-4444-5555', 'emp004@test.com', 3, 0, 1, NOW(), NOW()),
('emptest005', '$2a$10$d48wVDxrG/Paw.ULUn5.ae0IGrOu5415tyKW24RR5lKmqQcdgLdoy', '010-5555-6666', 'emp005@test.com', 3, 0, 1, NOW(), NOW());

-- Employee Details
INSERT INTO employees (user_id, emp_name, emp_type, sub_phone, emp_address, is_deleted, created_at, updated_at)
SELECT id,
    CASE
        WHEN user_id = 'emptest001' THEN 'Kim Cheol-su'
        WHEN user_id = 'emptest002' THEN 'Lee Young-hee'
        WHEN user_id = 'emptest003' THEN 'Park Min-jun'
        WHEN user_id = 'emptest004' THEN 'Choi Ji-eun'
        WHEN user_id = 'emptest005' THEN 'Jung Seo-yeon'
    END,
    CASE
        WHEN user_id IN ('emptest001', 'emptest002', 'emptest003') THEN 'PERMANENT'
        ELSE 'DAILY'
    END,
    '010-9999-9999',
    'Seoul Test Address',
    0,
    NOW(),
    NOW()
FROM users
WHERE user_id IN ('emptest001', 'emptest002', 'emptest003', 'emptest004', 'emptest005');

-- 2. Create Contracts (계약)
-- Fully Signed Contracts (Active)
INSERT INTO contracts (corporation_id, manager_id, employee_id, final_pdf_url, employee_start_date, employee_end_date, contract_state, emp_type, is_deleted, created_at, updated_at)
SELECT
    197 as corporation_id,
    199 as manager_id,
    e.id as employee_id,
    CONCAT('https://test.s3.amazonaws.com/contract_', e.id, '.pdf') as final_pdf_url,
    '2025-01-01' as employee_start_date,
    '2025-12-31' as employee_end_date,
    'FULLY_SIGNED' as contract_state,
    e.emp_type as emp_type,
    0 as is_deleted,
    NOW() as created_at,
    NOW() as updated_at
FROM employees e
WHERE e.emp_name IN ('Kim Cheol-su', 'Lee Young-hee', 'Park Min-jun');

-- Contract Details
INSERT INTO contract_details (contract_id, corp_name, emp_name, work_type, work_place, work_start_time, work_end_time, work_on_days, work_pay, is_deleted, created_at, updated_at)
SELECT
    c.id as contract_id,
    'API Test Corp' as corp_name,
    e.emp_name,
    'Construction Worker' as work_type,
    'API Test Site 1' as work_place,
    '09:00:00' as work_start_time,
    '18:00:00' as work_end_time,
    'Monday,Tuesday,Wednesday,Thursday,Friday' as work_on_days,
    3000000 as work_pay,
    0 as is_deleted,
    NOW() as created_at,
    NOW() as updated_at
FROM contracts c
JOIN employees e ON c.employee_id = e.id
WHERE c.manager_id = 199 AND c.contract_state = 'FULLY_SIGNED';

-- Pending Contracts (미결 계약)
INSERT INTO contracts (corporation_id, manager_id, employee_id, employee_start_date, employee_end_date, contract_state, emp_type, is_deleted, created_at, updated_at)
SELECT
    197 as corporation_id,
    199 as manager_id,
    e.id as employee_id,
    '2025-02-01' as employee_start_date,
    '2025-12-31' as employee_end_date,
    'MANAGER_SIGNING_PENDING' as contract_state,
    e.emp_type as emp_type,
    0 as is_deleted,
    NOW() as created_at,
    NOW() as updated_at
FROM employees e
WHERE e.emp_name IN ('Choi Ji-eun', 'Jung Seo-yeon');

-- 3. Create Attendance Records (근태 기록 - 금일)
INSERT INTO attendance_records (site_id, employee_id, timestamp, attendance_type, state, captured_face_image_url, similarity_score, is_deleted, created_at, updated_at)
SELECT
    179 as site_id,
    e.id as employee_id,
    CASE
        WHEN e.emp_name = 'Kim Cheol-su' THEN CONCAT(CURDATE(), ' 08:55:00')  -- 정상 출근
        WHEN e.emp_name = 'Lee Young-hee' THEN CONCAT(CURDATE(), ' 09:10:00') -- 지각 (09:00 + 5분 = 09:05 기준)
        WHEN e.emp_name = 'Park Min-jun' THEN CONCAT(CURDATE(), ' 09:08:00')  -- 지각
    END as timestamp,
    'CHECK_IN' as attendance_type,
    'CONFIRMED' as state,
    'https://test.s3.amazonaws.com/face_default.jpg' as captured_face_image_url,
    0.95 as similarity_score,
    0 as is_deleted,
    NOW() as created_at,
    NOW() as updated_at
FROM employees e
WHERE e.emp_name IN ('Kim Cheol-su', 'Lee Young-hee', 'Park Min-jun');

-- 4. Create Safety Education Logs (안전교육일지 - 미결)
INSERT INTO safety_education_logs (corporation_id, manager_id, site_id, education_subject, education_content, education_location, education_type, instructor_name, status, is_deleted, created_at, updated_at)
VALUES
(197, 199, 179, 'Fall Prevention Training', 'Training on preventing falls from heights', 'API Test Site 1', 'REGULAR', 'Safety Officer Kim', 'MANAGER_SIGNING_PENDING', 0, NOW(), NOW()),
(197, 199, 179, 'Fire Safety Training', 'Fire extinguisher usage and evacuation procedures', 'API Test Site 1', 'SPECIAL', 'Safety Officer Lee', 'MANAGER_SIGNING_PENDING', 0, NOW(), NOW());

-- Verify data
SELECT 'Employees Created' as Status, COUNT(*) as Count FROM employees WHERE emp_name LIKE '%Cheol-su%' OR emp_name LIKE '%Young-hee%' OR emp_name LIKE '%Min-jun%' OR emp_name LIKE '%Ji-eun%' OR emp_name LIKE '%Seo-yeon%';
SELECT 'Contracts Created' as Status, COUNT(*) as Count FROM contracts WHERE manager_id = 199;
SELECT 'Attendance Records' as Status, COUNT(*) as Count FROM attendance_records WHERE site_id = 179 AND DATE(timestamp) = CURDATE();
SELECT 'Safety Logs' as Status, COUNT(*) as Count FROM safety_education_logs WHERE site_id = 179 AND is_deleted = 0;
