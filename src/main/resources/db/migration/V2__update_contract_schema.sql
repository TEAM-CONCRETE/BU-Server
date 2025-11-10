-- ============================================
-- Build-Up Platform - Contract Schema Update
-- Version: 2.0
-- Date: 2025-11-10
-- Description: 근로계약서 필드 구조 변경
-- - Contract 테이블에 emp_type 추가
-- - ContractDetail 테이블 필드 변경
-- ============================================

-- ============================================
-- 1. contracts 테이블 수정
-- ============================================

-- emp_type 컬럼 추가 (일용직/상용직 구분)
ALTER TABLE contracts
ADD COLUMN emp_type VARCHAR(30) COMMENT '근로자 유형 (DAILY, PERMANENT)' AFTER role;

-- emp_type 인덱스 추가
ALTER TABLE contracts
ADD INDEX idx_emp_type (emp_type);

-- ============================================
-- 2. contract_details 테이블 수정
-- ============================================

-- 근무시간 관련 컬럼 변경 (기존 컬럼 삭제 후 새 컬럼 추가)
ALTER TABLE contract_details
DROP COLUMN IF EXISTS work_time,
DROP COLUMN IF EXISTS break_time;

-- 근무시간 세분화 (시작/종료)
ALTER TABLE contract_details
ADD COLUMN work_start_time TIME COMMENT '근무 시작 시간' AFTER work_type,
ADD COLUMN work_end_time TIME COMMENT '근무 종료 시간' AFTER work_start_time,
ADD COLUMN break_start_time TIME COMMENT '휴게 시작 시간' AFTER work_end_time,
ADD COLUMN break_end_time TIME COMMENT '휴게 종료 시간' AFTER break_start_time;

-- 근무일/휴무일 컬럼명 변경 (복수형으로 통일)
ALTER TABLE contract_details
CHANGE COLUMN work_on_day work_on_days VARCHAR(100) COMMENT '근무일',
CHANGE COLUMN work_off_day work_off_days VARCHAR(100) COMMENT '휴무일';

-- 상여금 컬럼 삭제
ALTER TABLE contract_details
DROP COLUMN IF EXISTS work_bonus;

-- 급여 지급일 타입 변경 (String -> Integer)
ALTER TABLE contract_details
CHANGE COLUMN payday pay_day INT COMMENT '급여 지급일 (1~31)';

-- ============================================
-- 주의사항:
-- 1. 이 마이그레이션은 기존 데이터를 변경합니다
-- 2. work_time, break_time 데이터는 삭제됩니다
-- 3. payday 문자열 데이터는 정수로 변환 시도됩니다
-- 4. 운영 환경 적용 전 반드시 백업하세요
-- ============================================