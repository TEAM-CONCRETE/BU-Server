-- Build-Up Database Schema Initialization
-- 이 파일은 Docker 컨테이너가 처음 시작될 때 자동으로 실행됩니다.

-- 데이터베이스가 없으면 생성 (docker-compose에서 이미 생성되므로 선택사항)
CREATE DATABASE IF NOT EXISTS buildup
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE buildup;

-- 예시: 사용자 테이블 (Spring Security 사용 시)
-- JPA가 자동으로 생성하므로 주석 처리
-- CREATE TABLE IF NOT EXISTS users (
--     id BIGINT AUTO_INCREMENT PRIMARY KEY,
--     username VARCHAR(50) NOT NULL UNIQUE,
--     email VARCHAR(100) NOT NULL UNIQUE,
--     password VARCHAR(255) NOT NULL,
--     enabled BOOLEAN DEFAULT TRUE,
--     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
--     INDEX idx_username (username),
--     INDEX idx_email (email)
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 초기화 완료 메시지
SELECT 'Database schema initialization completed!' AS Status;
