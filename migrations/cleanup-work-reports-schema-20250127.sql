-- ============================================
-- Work Reports 테이블 스키마 정리
-- ============================================
-- 작성일: 2025-01-27
-- 작성자: Claude
-- 목적: work_reports 테이블에서 사용하지 않는 구버전 컬럼 제거
--
-- 변경 사항:
-- 1. work_reports 테이블에서 구버전 컬럼 8개 제거
--    - work_report_title (사용 안 함)
--    - work_report_created_at (사용 안 함, created_at 사용)
--    - work_report_started_at (사용 안 함)
--    - work_report_ended_at (사용 안 함)
--    - work_report_context (사용 안 함, work_sections로 대체)
--    - work_section_name (사용 안 함, work_sections로 대체)
--    - work_section_employee_num (사용 안 함, work_sections로 대체)
--    - work_report_status (사용 안 함, 즉시 PDF 생성 방식)
--
-- 실행 방법:
-- docker exec -i buildup-mysql mysql -u buildup -pbuildup123 buildup < migrations/cleanup-work-reports-schema-20250127.sql
-- ============================================

USE buildup;

-- work_reports 테이블 구버전 컬럼 제거
ALTER TABLE work_reports
    DROP COLUMN IF EXISTS work_report_title,
    DROP COLUMN IF EXISTS work_report_created_at,
    DROP COLUMN IF EXISTS work_report_started_at,
    DROP COLUMN IF EXISTS work_report_ended_at,
    DROP COLUMN IF EXISTS work_report_context,
    DROP COLUMN IF EXISTS work_section_name,
    DROP COLUMN IF EXISTS work_section_employee_num,
    DROP COLUMN IF EXISTS work_report_status;

-- 변경 완료 확인
SELECT 'Work Reports 테이블 스키마 정리 완료' AS status;
SELECT
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE,
    COLUMN_KEY,
    COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'buildup'
  AND TABLE_NAME = 'work_reports'
ORDER BY ORDINAL_POSITION;
