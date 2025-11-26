-- 스키마 동기화 마이그레이션
-- 생성일: 2025-01-26
-- 목적: 로컬과 EC2 DB 스키마를 동일하게 맞춤
-- 적용 순서: 1) 로컬에서 테스트 → 2) EC2에 적용

USE buildup;

-- ============================================
-- 1. contract_details 테이블 동기화 (EC2용)
-- ============================================
-- EC2에 없는 컬럼 추가 (로컬 기준으로 맞춤)
-- 주의: 로컬에서는 이미 컬럼이 있으므로 에러가 발생할 수 있음 (정상)

-- payday 컬럼이 없으면 추가
SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'buildup'
    AND TABLE_NAME = 'contract_details'
    AND COLUMN_NAME = 'payday'
);

SET @query = IF(@col_exists = 0,
    'ALTER TABLE contract_details ADD COLUMN payday VARCHAR(255) AFTER pay_type',
    'SELECT "payday already exists" AS Info'
);
PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- work_bonus 컬럼이 없으면 추가
SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'buildup'
    AND TABLE_NAME = 'contract_details'
    AND COLUMN_NAME = 'work_bonus'
);

SET @query = IF(@col_exists = 0,
    'ALTER TABLE contract_details ADD COLUMN work_bonus DECIMAL(10,2) AFTER payday',
    'SELECT "work_bonus already exists" AS Info'
);
PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- work_off_day 컬럼이 없으면 추가
SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'buildup'
    AND TABLE_NAME = 'contract_details'
    AND COLUMN_NAME = 'work_off_day'
);

SET @query = IF(@col_exists = 0,
    'ALTER TABLE contract_details ADD COLUMN work_off_day VARCHAR(255) AFTER work_end_time',
    'SELECT "work_off_day already exists" AS Info'
);
PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- work_on_day 컬럼이 없으면 추가
SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'buildup'
    AND TABLE_NAME = 'contract_details'
    AND COLUMN_NAME = 'work_on_day'
);

SET @query = IF(@col_exists = 0,
    'ALTER TABLE contract_details ADD COLUMN work_on_day VARCHAR(255) AFTER work_off_day',
    'SELECT "work_on_day already exists" AS Info'
);
PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================
-- 2. work_reports 테이블 동기화 (로컬용)
-- ============================================
-- 로컬에 없는 컬럼 추가 (EC2 기준으로 맞춤)

-- work_report_context 컬럼이 없으면 추가
SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'buildup'
    AND TABLE_NAME = 'work_reports'
    AND COLUMN_NAME = 'work_report_context'
);

SET @query = IF(@col_exists = 0,
    'ALTER TABLE work_reports ADD COLUMN work_report_context TEXT AFTER work_report_ended_at',
    'SELECT "work_report_context already exists" AS Info'
);
PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- work_section_employee_num 컬럼이 없으면 추가
SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'buildup'
    AND TABLE_NAME = 'work_reports'
    AND COLUMN_NAME = 'work_section_employee_num'
);

SET @query = IF(@col_exists = 0,
    'ALTER TABLE work_reports ADD COLUMN work_section_employee_num INT AFTER work_section_name',
    'SELECT "work_section_employee_num already exists" AS Info'
);
PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================
-- 검증 쿼리
-- ============================================

SELECT '=== 스키마 동기화 완료 ===' AS Status;

-- contract_details 컬럼 수 확인 (33개여야 함)
SELECT
    'contract_details' AS 'Table',
    COUNT(*) AS 'Column Count (Expected: 33)'
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'buildup' AND TABLE_NAME = 'contract_details';

-- work_reports 컬럼 수 확인 (18개여야 함)
SELECT
    'work_reports' AS 'Table',
    COUNT(*) AS 'Column Count (Expected: 18)'
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'buildup' AND TABLE_NAME = 'work_reports';

SELECT '=== 동기화 후 두 DB의 스키마가 동일해야 합니다 ===' AS Note;
