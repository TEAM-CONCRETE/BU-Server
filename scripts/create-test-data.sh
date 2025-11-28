#!/bin/bash
# 테스트 데이터 생성 및 검증 스크립트
# 로컬에서 먼저 테스트한 후 EC2에 안전하게 적용

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo "=== 테스트 데이터 생성 도구 ==="
echo ""

# 테스트 데이터 SQL 파일명
SQL_FILE="test-data/frontend-test-data-$(date +%Y%m%d-%H%M%S).sql"
mkdir -p test-data

echo "📝 테스트 데이터 SQL 파일 생성 중..."
echo "파일명: $SQL_FILE"
echo ""

# SQL 템플릿 생성
cat > "$SQL_FILE" << 'EOSQL'
-- 프론트엔드 테스트 데이터
-- 생성일시: __TIMESTAMP__
-- ⚠️ 주의: 기존 데이터를 삭제하지 않고 추가만 함

USE buildup;

-- ============================================
-- 1. 기업 관리자 및 기업 추가
-- ============================================

-- 기업 관리자 User 생성
INSERT INTO users (user_id, password, phone, email, role_id, is_deleted, profile_completed, created_at, updated_at)
VALUES
('testcorp001', '$2a$10$d48wVDxrG/Paw.ULUn5.ae0IGrOu5415tyKW24RR5lKmqQcdgLdoy', '010-1001-1001', 'testcorp001@test.com', 1, 0, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE user_id = user_id;

-- Corporation 생성
INSERT INTO corporations (corp_name, corp_ceo_name, corp_address, corp_phone, corp_registration_num, user_id, is_deleted, created_at, updated_at)
SELECT
    '(주)테스트건설1호',
    '김대표',
    '서울특별시 강남구 테헤란로 123',
    '02-1234-5678',
    '123-45-67890',
    u.id,
    0,
    NOW(),
    NOW()
FROM users u
WHERE u.user_id = 'testcorp001'
ON DUPLICATE KEY UPDATE corp_name = corp_name;

-- ============================================
-- 2. 현장 관리자 및 현장 추가
-- ============================================

-- 현장 관리자 User 생성
INSERT INTO users (user_id, password, phone, email, role_id, is_deleted, profile_completed, created_at, updated_at)
VALUES
('testmgr001', '$2a$10$d48wVDxrG/Paw.ULUn5.ae0IGrOu5415tyKW24RR5lKmqQcdgLdoy', '010-2001-2001', 'testmgr001@test.com', 2, 0, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE user_id = user_id;

-- Manager 생성
INSERT INTO managers (user_id, manager_name, manager_position, is_deleted, created_at, updated_at)
SELECT
    u.id,
    '김현장',
    '현장소장',
    0,
    NOW(),
    NOW()
FROM users u
WHERE u.user_id = 'testmgr001'
ON DUPLICATE KEY UPDATE manager_name = manager_name;

-- Site 생성
INSERT INTO sites (site_name, site_address, client_name, start_date, end_date, manager_secret_key, employee_secret_key, corporation_id, manager_id, is_deleted, created_at, updated_at)
SELECT
    '강남 오피스텔 A동 신축현장',
    '서울특별시 강남구 역삼동 123-45',
    '서울시설공단',
    '2025-01-01',
    '2025-12-31',
    CONCAT('MGR_', SUBSTRING(MD5(RAND()), 1, 20)),
    CONCAT('EMP_', SUBSTRING(MD5(RAND()), 1, 20)),
    c.id,
    m.id,
    0,
    NOW(),
    NOW()
FROM corporations c
CROSS JOIN managers m
CROSS JOIN users u_mgr
WHERE c.corp_name = '(주)테스트건설1호'
  AND m.manager_name = '김현장'
  AND u_mgr.user_id = 'testmgr001'
  AND m.user_id = u_mgr.id
ON DUPLICATE KEY UPDATE site_name = site_name;

-- ============================================
-- 3. 근로자 추가 (5명)
-- ============================================

INSERT INTO users (user_id, password, phone, email, role_id, is_deleted, profile_completed, created_at, updated_at)
VALUES
('emp001', '$2a$10$d48wVDxrG/Paw.ULUn5.ae0IGrOu5415tyKW24RR5lKmqQcdgLdoy', '010-3001-0001', 'emp001@test.com', 3, 0, 1, NOW(), NOW()),
('emp002', '$2a$10$d48wVDxrG/Paw.ULUn5.ae0IGrOu5415tyKW24RR5lKmqQcdgLdoy', '010-3002-0002', 'emp002@test.com', 3, 0, 1, NOW(), NOW()),
('emp003', '$2a$10$d48wVDxrG/Paw.ULUn5.ae0IGrOu5415tyKW24RR5lKmqQcdgLdoy', '010-3003-0003', 'emp003@test.com', 3, 0, 1, NOW(), NOW()),
('emp004', '$2a$10$d48wVDxrG/Paw.ULUn5.ae0IGrOu5415tyKW24RR5lKmqQcdgLdoy', '010-3004-0004', 'emp004@test.com', 3, 0, 1, NOW(), NOW()),
('emp005', '$2a$10$d48wVDxrG/Paw.ULUn5.ae0IGrOu5415tyKW24RR5lKmqQcdgLdoy', '010-3005-0005', 'emp005@test.com', 3, 0, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE user_id = user_id;

INSERT INTO employees (user_id, emp_name, emp_type, sub_phone, emp_address, is_deleted, created_at, updated_at)
SELECT u.id, '김철수', 'PERMANENT', '010-9999-0001', '서울특별시 강남구', 0, NOW(), NOW()
FROM users u WHERE u.user_id = 'emp001'
ON DUPLICATE KEY UPDATE emp_name = emp_name;

INSERT INTO employees (user_id, emp_name, emp_type, sub_phone, emp_address, is_deleted, created_at, updated_at)
SELECT u.id, '이영희', 'PERMANENT', '010-9999-0002', '서울특별시 송파구', 0, NOW(), NOW()
FROM users u WHERE u.user_id = 'emp002'
ON DUPLICATE KEY UPDATE emp_name = emp_name;

INSERT INTO employees (user_id, emp_name, emp_type, sub_phone, emp_address, is_deleted, created_at, updated_at)
SELECT u.id, '박민준', 'DAILY', '010-9999-0003', '서울특별시 서초구', 0, NOW(), NOW()
FROM users u WHERE u.user_id = 'emp003'
ON DUPLICATE KEY UPDATE emp_name = emp_name;

INSERT INTO employees (user_id, emp_name, emp_type, sub_phone, emp_address, is_deleted, created_at, updated_at)
SELECT u.id, '최지은', 'DAILY', '010-9999-0004', '서울특별시 관악구', 0, NOW(), NOW()
FROM users u WHERE u.user_id = 'emp004'
ON DUPLICATE KEY UPDATE emp_name = emp_name;

INSERT INTO employees (user_id, emp_name, emp_type, sub_phone, emp_address, is_deleted, created_at, updated_at)
SELECT u.id, '정서연', 'DAILY', '010-9999-0005', '서울특별시 강서구', 0, NOW(), NOW()
FROM users u WHERE u.user_id = 'emp005'
ON DUPLICATE KEY UPDATE emp_name = emp_name;

-- ============================================
-- 검증 쿼리
-- ============================================

SELECT '=== 데이터 주입 완료 ===' AS Status;
SELECT COUNT(*) AS '생성된 기업 수' FROM corporations WHERE corp_name LIKE '%테스트%';
SELECT COUNT(*) AS '생성된 현장 수' FROM sites WHERE site_name LIKE '%강남%';
SELECT COUNT(*) AS '생성된 근로자 수' FROM employees WHERE emp_name IN ('김철수', '이영희', '박민준', '최지은', '정서연');
EOSQL

# 타임스탬프 치환
sed -i.bak "s/__TIMESTAMP__/$(date '+%Y-%m-%d %H:%M:%S')/g" "$SQL_FILE"
rm "${SQL_FILE}.bak" 2>/dev/null || true

echo -e "${GREEN}✅ SQL 파일 생성 완료: $SQL_FILE${NC}"
echo ""

# 로컬 테스트 제안
echo "=== 다음 단계 ==="
echo ""
echo -e "${BLUE}1. 로컬 DB에서 테스트:${NC}"
echo "   docker exec -i buildup-mysql mysql -u buildup -pbuildup123 buildup < $SQL_FILE"
echo ""
echo -e "${BLUE}2. 로컬 DB 확인:${NC}"
echo "   docker exec -it buildup-mysql mysql -u buildup -pbuildup123 buildup"
echo "   mysql> SELECT * FROM corporations WHERE corp_name LIKE '%테스트%';"
echo ""
echo -e "${BLUE}3. 문제 없으면 EC2에 적용:${NC}"
echo "   scp -i your-key.pem $SQL_FILE ec2-user@your-ec2-ip:/home/ec2-user/"
echo "   ssh ec2-user@your-ec2-ip"
echo "   docker exec -i buildup-mysql-prod mysql -u buildup -pbuildup123 buildup < $SQL_FILE"
echo ""

# 자동 로컬 테스트 옵션
read -p "지금 로컬 DB에서 테스트하시겠습니까? (y/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo ""
    echo "🧪 로컬 DB 테스트 중..."

    if docker exec -i buildup-mysql mysql -u buildup -pbuildup123 buildup < "$SQL_FILE"; then
        echo -e "${GREEN}✅ 로컬 DB 테스트 성공!${NC}"
        echo ""
        echo "검증 결과:"
        docker exec -it buildup-mysql mysql -u buildup -pbuildup123 buildup -e "
            SELECT COUNT(*) AS '기업' FROM corporations WHERE corp_name LIKE '%테스트%';
            SELECT COUNT(*) AS '현장' FROM sites WHERE site_name LIKE '%강남%';
            SELECT COUNT(*) AS '근로자' FROM employees WHERE emp_name IN ('김철수', '이영희', '박민준', '최지은', '정서연');
        "
        echo ""
        echo -e "${YELLOW}📤 EC2 배포 준비 완료${NC}"
        echo "다음 명령어로 EC2에 배포하세요:"
        echo ""
        echo "  git add $SQL_FILE"
        echo "  git commit -m 'feat: 프론트엔드 테스트 데이터 추가'"
        echo "  git push"
        echo ""
        echo "  # EC2에서"
        echo "  cd /home/ec2-user/BU-Server"
        echo "  git pull"
        echo "  docker exec -i buildup-mysql-prod mysql -u buildup -pbuildup123 buildup < $SQL_FILE"
    else
        echo -e "${RED}❌ 로컬 DB 테스트 실패${NC}"
        echo "SQL 파일을 확인하세요: $SQL_FILE"
    fi
else
    echo ""
    echo "수동으로 테스트하세요."
fi

echo ""
echo "=== 완료 ==="
