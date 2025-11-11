-- V4: Add profile_image_url column to employees table
-- 얼굴 인식 출퇴근 시스템을 위한 프로필 이미지 URL 추가

ALTER TABLE employees
ADD COLUMN profile_image_url VARCHAR(500) NULL
COMMENT '등록된 얼굴 이미지 S3 URL';
