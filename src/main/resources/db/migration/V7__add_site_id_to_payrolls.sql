-- V7: payrolls 테이블에 site_id 컬럼 및 기간별 조회 인덱스 추가
-- 급여 내역 조회 API (BU-155)

-- 1. site_id 컬럼 추가
ALTER TABLE payrolls
ADD COLUMN site_id BIGINT NOT NULL COMMENT '현장 ID'
AFTER corporation_id;

-- 2. site_id 외래키 제약조건 추가
ALTER TABLE payrolls
ADD CONSTRAINT fk_payrolls_site
FOREIGN KEY (site_id) REFERENCES sites(id);

-- 3. site_id 단일 인덱스 추가
CREATE INDEX idx_site_id ON payrolls(site_id);

-- 4. 기간별 조회 최적화를 위한 복합 인덱스 추가
CREATE INDEX idx_period_search ON payrolls(site_id, salary_year, salary_month, emp_type, pay_cycle);