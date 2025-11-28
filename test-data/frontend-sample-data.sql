-- 프론트엔드 테스트용 샘플 데이터
-- 생성일: 2025-01-26
-- 목적: 프론트엔드 개발자가 API 연동 테스트를 위한 기본 데이터
-- ⚠️ 주의: 기존 데이터를 삭제하지 않고 추가만 함 (ON DUPLICATE KEY UPDATE)

USE buildup;

-- ================================================
-- 1. 기업 관리자 계정 (CORPORATION)
-- ================================================
-- ID: frontcorp / PW: Admin123!@

INSERT INTO users (user_id, password, phone, email, role_id, is_deleted, profile_completed, created_at, updated_at)
VALUES
('frontcorp', '$2a$10$d48wVDxrG/Paw.ULUn5.ae0IGrOu5415tyKW24RR5lKmqQcdgLdoy', '010-1000-0001', 'frontcorp@test.com', 1, 0, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE user_id = user_id;

-- 기업 정보
INSERT INTO corporations (corp_name, corp_ceo_name, corp_address, corp_phone, corp_registration_num, user_id, is_deleted, created_at, updated_at)
SELECT
    '(주)프론트테스트건설',
    '김프론트',
    '서울특별시 강남구 테헤란로 152',
    '02-9999-0001',
    '111-22-33444',
    u.id,
    0,
    NOW(),
    NOW()
FROM users u
WHERE u.user_id = 'frontcorp'
ON DUPLICATE KEY UPDATE corp_name = VALUES(corp_name);

-- ================================================
-- 2. 현장 관리자 계정 (MANAGER)
-- ================================================
-- ID: frontmgr / PW: Admin123!@

INSERT INTO users (user_id, password, phone, email, role_id, is_deleted, profile_completed, created_at, updated_at)
VALUES
('frontmgr', '$2a$10$d48wVDxrG/Paw.ULUn5.ae0IGrOu5415tyKW24RR5lKmqQcdgLdoy', '010-2000-0001', 'frontmgr@test.com', 2, 0, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE user_id = user_id;

-- 현장 관리자 정보
INSERT INTO managers (user_id, manager_name, manager_position, is_deleted, created_at, updated_at)
SELECT
    u.id,
    '이현장',
    '현장소장',
    0,
    NOW(),
    NOW()
FROM users u
WHERE u.user_id = 'frontmgr'
ON DUPLICATE KEY UPDATE manager_name = VALUES(manager_name);

-- ================================================
-- 3. 현장 정보 (2개)
-- ================================================

INSERT INTO sites (site_name, site_address, client_name, start_date, end_date, manager_secret_key, employee_secret_key, corporation_id, manager_id, is_deleted, created_at, updated_at)
SELECT
    '강남 오피스텔 A동 신축현장',
    '서울특별시 강남구 역삼동 123-45',
    '서울시설공단',
    '2025-01-01',
    '2025-12-31',
    CONCAT('MGR_', SUBSTRING(MD5(CONCAT('fronttest1', NOW())), 1, 20)),
    CONCAT('EMP_', SUBSTRING(MD5(CONCAT('fronttest1', NOW())), 1, 20)),
    c.id,
    m.id,
    0,
    NOW(),
    NOW()
FROM corporations c
CROSS JOIN managers m
CROSS JOIN users u_corp
CROSS JOIN users u_mgr
WHERE c.corp_name = '(주)프론트테스트건설'
  AND u_corp.user_id = 'frontcorp'
  AND c.user_id = u_corp.id
  AND m.manager_name = '이현장'
  AND u_mgr.user_id = 'frontmgr'
  AND m.user_id = u_mgr.id
LIMIT 1
ON DUPLICATE KEY UPDATE site_name = VALUES(site_name);

INSERT INTO sites (site_name, site_address, client_name, start_date, end_date, manager_secret_key, employee_secret_key, corporation_id, manager_id, is_deleted, created_at, updated_at)
SELECT
    '송파 아파트 B동 리모델링',
    '서울특별시 송파구 잠실동 456-78',
    '대한주택공사',
    '2025-02-01',
    '2025-11-30',
    CONCAT('MGR_', SUBSTRING(MD5(CONCAT('fronttest2', NOW())), 1, 20)),
    CONCAT('EMP_', SUBSTRING(MD5(CONCAT('fronttest2', NOW())), 1, 20)),
    c.id,
    m.id,
    0,
    NOW(),
    NOW()
FROM corporations c
CROSS JOIN managers m
CROSS JOIN users u_corp
CROSS JOIN users u_mgr
WHERE c.corp_name = '(주)프론트테스트건설'
  AND u_corp.user_id = 'frontcorp'
  AND c.user_id = u_corp.id
  AND m.manager_name = '이현장'
  AND u_mgr.user_id = 'frontmgr'
  AND m.user_id = u_mgr.id
LIMIT 1
ON DUPLICATE KEY UPDATE site_name = VALUES(site_name);

-- ================================================
-- 4. 근로자 계정 (10명)
-- ================================================
-- ID: frontemp001 ~ frontemp010 / PW: Admin123!@

INSERT INTO users (user_id, password, phone, email, role_id, is_deleted, profile_completed, created_at, updated_at)
VALUES
('frontemp001', '$2a$10$d48wVDxrG/Paw.ULUn5.ae0IGrOu5415tyKW24RR5lKmqQcdgLdoy', '010-3001-0001', 'emp001@test.com', 3, 0, 1, NOW(), NOW()),
('frontemp002', '$2a$10$d48wVDxrG/Paw.ULUn5.ae0IGrOu5415tyKW24RR5lKmqQcdgLdoy', '010-3002-0002', 'emp002@test.com', 3, 0, 1, NOW(), NOW()),
('frontemp003', '$2a$10$d48wVDxrG/Paw.ULUn5.ae0IGrOu5415tyKW24RR5lKmqQcdgLdoy', '010-3003-0003', 'emp003@test.com', 3, 0, 1, NOW(), NOW()),
('frontemp004', '$2a$10$d48wVDxrG/Paw.ULUn5.ae0IGrOu5415tyKW24RR5lKmqQcdgLdoy', '010-3004-0004', 'emp004@test.com', 3, 0, 1, NOW(), NOW()),
('frontemp005', '$2a$10$d48wVDxrG/Paw.ULUn5.ae0IGrOu5415tyKW24RR5lKmqQcdgLdoy', '010-3005-0005', 'emp005@test.com', 3, 0, 1, NOW(), NOW()),
('frontemp006', '$2a$10$d48wVDxrG/Paw.ULUn5.ae0IGrOu5415tyKW24RR5lKmqQcdgLdoy', '010-3006-0006', 'emp006@test.com', 3, 0, 1, NOW(), NOW()),
('frontemp007', '$2a$10$d48wVDxrG/Paw.ULUn5.ae0IGrOu5415tyKW24RR5lKmqQcdgLdoy', '010-3007-0007', 'emp007@test.com', 3, 0, 1, NOW(), NOW()),
('frontemp008', '$2a$10$d48wVDxrG/Paw.ULUn5.ae0IGrOu5415tyKW24RR5lKmqQcdgLdoy', '010-3008-0008', 'emp008@test.com', 3, 0, 1, NOW(), NOW()),
('frontemp009', '$2a$10$d48wVDxrG/Paw.ULUn5.ae0IGrOu5415tyKW24RR5lKmqQcdgLdoy', '010-3009-0009', 'emp009@test.com', 3, 0, 1, NOW(), NOW()),
('frontemp010', '$2a$10$d48wVDxrG/Paw.ULUn5.ae0IGrOu5415tyKW24RR5lKmqQcdgLdoy', '010-3010-0010', 'emp010@test.com', 3, 0, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE user_id = user_id;

-- 근로자 상세 정보
INSERT INTO employees (user_id, emp_name, emp_type, sub_phone, emp_address, is_deleted, created_at, updated_at)
SELECT u.id, '김철수', 'PERMANENT', '010-9001-0001', '서울특별시 강남구 대치동', 0, NOW(), NOW()
FROM users u WHERE u.user_id = 'frontemp001'
ON DUPLICATE KEY UPDATE emp_name = VALUES(emp_name);

INSERT INTO employees (user_id, emp_name, emp_type, sub_phone, emp_address, is_deleted, created_at, updated_at)
SELECT u.id, '이영희', 'PERMANENT', '010-9002-0002', '서울특별시 송파구 잠실동', 0, NOW(), NOW()
FROM users u WHERE u.user_id = 'frontemp002'
ON DUPLICATE KEY UPDATE emp_name = VALUES(emp_name);

INSERT INTO employees (user_id, emp_name, emp_type, sub_phone, emp_address, is_deleted, created_at, updated_at)
SELECT u.id, '박민준', 'PERMANENT', '010-9003-0003', '서울특별시 서초구 서초동', 0, NOW(), NOW()
FROM users u WHERE u.user_id = 'frontemp003'
ON DUPLICATE KEY UPDATE emp_name = VALUES(emp_name);

INSERT INTO employees (user_id, emp_name, emp_type, sub_phone, emp_address, is_deleted, created_at, updated_at)
SELECT u.id, '최지은', 'DAILY', '010-9004-0004', '서울특별시 관악구 봉천동', 0, NOW(), NOW()
FROM users u WHERE u.user_id = 'frontemp004'
ON DUPLICATE KEY UPDATE emp_name = VALUES(emp_name);

INSERT INTO employees (user_id, emp_name, emp_type, sub_phone, emp_address, is_deleted, created_at, updated_at)
SELECT u.id, '정서연', 'DAILY', '010-9005-0005', '서울특별시 강서구 화곡동', 0, NOW(), NOW()
FROM users u WHERE u.user_id = 'frontemp005'
ON DUPLICATE KEY UPDATE emp_name = VALUES(emp_name);

INSERT INTO employees (user_id, emp_name, emp_type, sub_phone, emp_address, is_deleted, created_at, updated_at)
SELECT u.id, '한지민', 'DAILY', '010-9006-0006', '서울특별시 마포구 상암동', 0, NOW(), NOW()
FROM users u WHERE u.user_id = 'frontemp006'
ON DUPLICATE KEY UPDATE emp_name = VALUES(emp_name);

INSERT INTO employees (user_id, emp_name, emp_type, sub_phone, emp_address, is_deleted, created_at, updated_at)
SELECT u.id, '윤석열', 'PERMANENT', '010-9007-0007', '서울특별시 용산구 한남동', 0, NOW(), NOW()
FROM users u WHERE u.user_id = 'frontemp007'
ON DUPLICATE KEY UPDATE emp_name = VALUES(emp_name);

INSERT INTO employees (user_id, emp_name, emp_type, sub_phone, emp_address, is_deleted, created_at, updated_at)
SELECT u.id, '문재인', 'PERMANENT', '010-9008-0008', '서울특별시 강남구 논현동', 0, NOW(), NOW()
FROM users u WHERE u.user_id = 'frontemp008'
ON DUPLICATE KEY UPDATE emp_name = VALUES(emp_name);

INSERT INTO employees (user_id, emp_name, emp_type, sub_phone, emp_address, is_deleted, created_at, updated_at)
SELECT u.id, '안철수', 'DAILY', '010-9009-0009', '서울특별시 서대문구 연희동', 0, NOW(), NOW()
FROM users u WHERE u.user_id = 'frontemp009'
ON DUPLICATE KEY UPDATE emp_name = VALUES(emp_name);

INSERT INTO employees (user_id, emp_name, emp_type, sub_phone, emp_address, is_deleted, created_at, updated_at)
SELECT u.id, '이재명', 'DAILY', '010-9010-0010', '서울특별시 영등포구 여의도동', 0, NOW(), NOW()
FROM users u WHERE u.user_id = 'frontemp010'
ON DUPLICATE KEY UPDATE emp_name = VALUES(emp_name);

-- ================================================
-- 검증 쿼리
-- ================================================

SELECT '============================================' AS '';
SELECT '프론트엔드 테스트 데이터 주입 완료' AS 'Status';
SELECT '============================================' AS '';
SELECT '' AS '';

SELECT '📊 생성된 데이터 요약' AS '';
SELECT COUNT(*) AS '기업 수' FROM corporations WHERE corp_name LIKE '%프론트%';
SELECT COUNT(*) AS '현장 수' FROM sites WHERE site_name LIKE '%강남%' OR site_name LIKE '%송파%';
SELECT COUNT(*) AS '근로자 수' FROM employees WHERE emp_name IN ('김철수', '이영희', '박민준', '최지은', '정서연', '한지민', '윤석열', '문재인', '안철수', '이재명');

SELECT '' AS '';
SELECT '🔑 테스트 계정 정보' AS '';
SELECT '기업 관리자: frontcorp / Admin123!@' AS 'Corporation';
SELECT '현장 관리자: frontmgr / Admin123!@' AS 'Manager';
SELECT '근로자: frontemp001~010 / Admin123!@' AS 'Employee';
