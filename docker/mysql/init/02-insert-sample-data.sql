-- Build-Up Sample Data Insertion
-- 개발/테스트용 샘플 데이터를 삽입합니다.

USE buildup;

-- ============================================
-- 1. 역할(Roles) 삽입
-- ============================================
INSERT INTO roles (role_name, description, is_deleted) VALUES
    ('ROLE_ADMIN', '시스템 관리자', b'0'),
    ('ROLE_MANAGER', '현장 관리자', b'0'),
    ('ROLE_EMPLOYEE', '근로자', b'0'),
    ('ROLE_CORPORATION', '기업', b'0')
ON DUPLICATE KEY UPDATE
    description = VALUES(description),
    is_deleted = VALUES(is_deleted);

-- ============================================
-- 2. Admin 계정 생성
-- ============================================
-- 비밀번호: Admin123!@ (BCrypt 해시)
INSERT INTO users (user_id, password, role_id, phone, email, secret_key, profile_completed, is_deleted)
SELECT 'admin', '$2a$10$d48wVDxrG/Paw.ULUn5.ae0IGrOu5415tyKW24RR5lKmqQcdgLdoy', r.id, NULL, 'admin@buildup.com', NULL, b'1', b'0'
FROM roles r
WHERE r.role_name = 'ROLE_ADMIN'
ON DUPLICATE KEY UPDATE
    password = VALUES(password),
    email = VALUES(email);

-- 샘플 데이터 삽입 완료 메시지
SELECT 'Sample data insertion completed!' AS Status;
SELECT 'Admin account created - ID: admin, Password: Admin123!@' AS Info;
