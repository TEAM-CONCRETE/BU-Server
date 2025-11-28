#!/bin/bash
# DB 스키마 비교 스크립트
# 로컬 DB와 EC2 DB의 구조가 동일한지 확인

set -e

echo "=== DB 스키마 비교 도구 ==="
echo ""

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 1. 로컬 스키마 덤프
echo "📦 로컬 DB 스키마 추출 중..."
docker exec buildup-mysql mysqldump \
  -u buildup \
  -pbuildup123 \
  --no-data \
  --skip-comments \
  --skip-add-drop-table \
  buildup > local-schema.sql

echo "✅ 로컬 스키마 저장: local-schema.sql"
echo ""

# 2. EC2 스키마 덤프 (SSH 접속 필요)
echo "📦 EC2 DB 스키마 추출 안내"
echo "다음 명령어를 EC2에서 실행하세요:"
echo ""
echo -e "${YELLOW}ssh ec2-user@your-ec2-ip${NC}"
echo -e "${YELLOW}docker exec buildup-mysql-prod mysqldump -u buildup -pbuildup123 --no-data --skip-comments --skip-add-drop-table buildup > ec2-schema.sql${NC}"
echo -e "${YELLOW}exit${NC}"
echo ""
echo "그 다음 EC2에서 로컬로 파일 복사:"
echo -e "${YELLOW}scp -i your-key.pem ec2-user@your-ec2-ip:/home/ec2-user/ec2-schema.sql .${NC}"
echo ""

# ec2-schema.sql이 있으면 비교
if [ -f "ec2-schema.sql" ]; then
    echo "🔍 스키마 차이 비교 중..."

    # CREATE TABLE 문만 추출하여 비교
    grep "CREATE TABLE" local-schema.sql | sort > local-tables.txt
    grep "CREATE TABLE" ec2-schema.sql | sort > ec2-tables.txt

    if diff -q local-tables.txt ec2-tables.txt > /dev/null; then
        echo -e "${GREEN}✅ 테이블 목록 동일${NC}"
    else
        echo -e "${RED}❌ 테이블 목록 차이 발견${NC}"
        echo ""
        echo "차이점:"
        diff local-tables.txt ec2-tables.txt || true
    fi

    # 전체 스키마 비교
    echo ""
    echo "📊 전체 스키마 차이:"
    diff local-schema.sql ec2-schema.sql > schema-diff.txt || true

    if [ -s schema-diff.txt ]; then
        echo -e "${RED}❌ 스키마 차이 발견 (schema-diff.txt 참고)${NC}"
        head -50 schema-diff.txt
    else
        echo -e "${GREEN}✅ 스키마 완전 동일${NC}"
    fi

    # 정리
    rm local-tables.txt ec2-tables.txt
else
    echo "⚠️  ec2-schema.sql 파일이 없습니다."
    echo "위의 명령어를 실행하여 EC2 스키마를 가져오세요."
fi

echo ""
echo "=== 완료 ==="
