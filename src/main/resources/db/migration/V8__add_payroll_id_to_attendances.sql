-- attendances 테이블에 payroll_id 컬럼 추가
-- 급여명세서 상세 조회를 위한 연관관계 설정

ALTER TABLE attendances
ADD COLUMN payroll_id BIGINT NULL COMMENT '급여 ID (급여 생성 시 연결)';

-- Foreign Key 추가
ALTER TABLE attendances
ADD CONSTRAINT fk_attendances_payroll
FOREIGN KEY (payroll_id) REFERENCES payrolls(id) ON DELETE SET NULL;

-- 인덱스 추가 (조회 성능 향상)
CREATE INDEX idx_payroll_id ON attendances(payroll_id);