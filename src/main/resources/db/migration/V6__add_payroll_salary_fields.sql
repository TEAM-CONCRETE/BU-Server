-- V6__add_payroll_salary_fields.sql
-- 급여명세서 자동 생성 기능을 위한 payrolls 테이블 컬럼 추가
-- 작성일: 2025-11-15
-- 작성자: 문현민

-- 1. 급여 대상 기간 컬럼 추가
ALTER TABLE payrolls
    ADD COLUMN salary_year INT NOT NULL COMMENT '급여 대상 연도 (예: 2025)' AFTER corporation_id,
    ADD COLUMN salary_month INT NOT NULL COMMENT '급여 대상 월 (1~12)' AFTER salary_year,
    ADD COLUMN salary_week INT NULL COMMENT '급여 대상 주차 (주급인 경우, 1~5)' AFTER salary_month,
    ADD COLUMN salary_day DATE NULL COMMENT '급여 대상 일자 (일급인 경우)' AFTER salary_week;

-- 2. S3 저장 경로 및 생성 시각 컬럼 추가
ALTER TABLE payrolls
    ADD COLUMN s3_key VARCHAR(500) NULL COMMENT '급여명세서 PDF 저장 S3 경로' AFTER pay_status,
    ADD COLUMN generated_at DATETIME NULL COMMENT '급여명세서 자동 생성 시각' AFTER s3_key;

-- 3. s3_key 조회를 위한 인덱스 추가
CREATE INDEX idx_payrolls_s3_key ON payrolls(s3_key);

-- 4. 중복 방지를 위한 UNIQUE INDEX 추가
-- (employee_id, salary_year, salary_month, pay_cycle, salary_week, salary_day) 조합으로 중복 방지
CREATE UNIQUE INDEX idx_payroll_unique
ON payrolls(employee_id, salary_year, salary_month, pay_cycle, salary_week, salary_day);