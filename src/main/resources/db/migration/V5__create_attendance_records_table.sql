-- V5: Create attendance_records table
-- 얼굴 인식 기반 출퇴근 기록 테이블 생성

CREATE TABLE attendance_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    attendance_type ENUM('CHECK_IN', 'CHECK_OUT') NOT NULL,
    timestamp DATETIME NOT NULL,
    captured_face_image_url VARCHAR(500) NOT NULL COMMENT '촬영된 얼굴 이미지 S3 URL',
    similarity_score DOUBLE NULL COMMENT '유사도 점수 (0.0~1.0)',
    state ENUM('CONFIRMED', 'PENDING_REVIEW', 'REJECTED') NOT NULL DEFAULT 'CONFIRMED',
    site_id BIGINT NOT NULL,
    failure_reason VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,

    INDEX idx_employee_timestamp (employee_id, timestamp),
    INDEX idx_site_timestamp (site_id, timestamp),
    INDEX idx_state (state),

    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE RESTRICT,
    FOREIGN KEY (site_id) REFERENCES sites(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='출퇴근 기록';