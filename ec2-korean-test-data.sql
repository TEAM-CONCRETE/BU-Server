-- EC2 한국어 테스트 데이터
-- UTF-8 인코딩으로 저장된 SQL 파일

-- 기업 추가
INSERT INTO corporations (corp_name, corp_ceo_name, corp_address, corp_phone, corp_registration_num, user_id, is_deleted, created_at, updated_at)
SELECT '(주)테스트건설', '김대표', '서울특별시 강남구 테헤란로 123', '02-1234-5678', '123-45-67890', id, 0, NOW(), NOW()
FROM users WHERE user_id = 'apitestcorp';

-- 현장 추가
INSERT INTO sites (site_name, site_address, client_name, start_date, end_date, manager_secret_key, employee_secret_key, corporation_id, manager_id, is_deleted, created_at, updated_at)
SELECT
    '강남 오피스텔 신축현장',
    '서울특별시 강남구 역삼동 123-45',
    '서울시설공단',
    '2025-01-01',
    '2025-12-31',
    CONCAT('MGR_', UUID()),
    CONCAT('EMP_', UUID()),
    c.id,
    199,
    0,
    NOW(),
    NOW()
FROM corporations c
WHERE c.corp_name = '(주)테스트건설'
LIMIT 1;

-- 근로자 추가
INSERT INTO users (user_id, password, phone, email, role_id, is_deleted, profile_completed, created_at, updated_at)
VALUES
('kimcs', '$2a$10$d48wVDxrG/Paw.ULUn5.ae0IGrOu5415tyKW24RR5lKmqQcdgLdoy', '010-1111-2222', 'kimcs@test.com', 3, 0, 1, NOW(), NOW()),
('leeyh', '$2a$10$d48wVDxrG/Paw.ULUn5.ae0IGrOu5415tyKW24RR5lKmqQcdgLdoy', '010-2222-3333', 'leeyh@test.com', 3, 0, 1, NOW(), NOW());

INSERT INTO employees (user_id, emp_name, emp_type, sub_phone, emp_address, is_deleted, created_at, updated_at)
SELECT id, '김철수', 'PERMANENT', '010-1111-2222', '서울특별시 강남구', 0, NOW(), NOW()
FROM users WHERE user_id = 'kimcs'
UNION ALL
SELECT id, '이영희', 'DAILY', '010-2222-3333', '서울특별시 송파구', 0, NOW(), NOW()
FROM users WHERE user_id = 'leeyh';

SELECT '데이터 생성 완료' as Status;
