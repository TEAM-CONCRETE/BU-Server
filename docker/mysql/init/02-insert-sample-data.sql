-- Build-Up Sample Data Insertion
-- 개발/테스트용 샘플 데이터를 삽입합니다.

USE buildup;

-- 예시: 테스트 사용자 데이터
-- 실제 테이블이 생성된 후 활성화하세요
-- INSERT INTO users (username, email, password, enabled) VALUES
-- ('admin', 'admin@buildup.com', '$2a$10$...', TRUE),  -- 비밀번호는 BCrypt 해시로 변경 필요
-- ('testuser', 'test@buildup.com', '$2a$10$...', TRUE);

-- 샘플 데이터 삽입 완료 메시지
SELECT 'Sample data insertion completed!' AS Status;
