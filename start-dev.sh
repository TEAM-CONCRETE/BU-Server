#!/bin/bash
# Build-Up Backend 개발 환경 시작 스크립트

set -e  # 에러 발생 시 스크립트 중단

echo "🚀 Build-Up Backend 개발 환경을 시작합니다..."
echo ""

# 색상 정의
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# .env 파일 로드
if [ -f ".env" ]; then
    echo -e "${BLUE}📄 .env 파일을 로드합니다...${NC}"
    # 주석 제거 후 환경 변수 로드
    while IFS= read -r line || [ -n "$line" ]; do
        # 빈 줄이나 주석으로 시작하는 줄 건너뛰기
        [[ -z "$line" || "$line" =~ ^[[:space:]]*# ]] && continue

        # 인라인 주석 제거 (# 이후 제거)
        line="${line%%#*}"

        # 앞뒤 공백 제거
        line="$(echo "$line" | xargs)"

        # = 가 포함된 줄만 처리
        if [[ "$line" == *"="* ]]; then
            export "$line"
        fi
    done < .env
    echo -e "${GREEN}✅ 환경 변수가 로드되었습니다.${NC}"
    echo ""
else
    echo -e "${YELLOW}⚠️  .env 파일이 없습니다.${NC}"
    echo "AWS S3를 사용하려면 .env 파일을 생성하세요:"
    echo "  cp .env.example .env"
    echo ""
fi

# Docker Compose 파일 확인
if [ ! -f "docker-compose.dev.yml" ]; then
    echo -e "${RED}❌ docker-compose.dev.yml 파일이 없습니다.${NC}"
    echo "먼저 docker-compose.dev.yml 파일을 생성해주세요."
    exit 1
fi

# Docker 실행 여부 확인
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}❌ Docker가 실행되고 있지 않습니다.${NC}"
    echo "Docker Desktop을 실행해주세요."
    exit 1
fi

# 기존 컨테이너 정리 (선택사항)
echo -e "${BLUE}🧹 기존 컨테이너를 정리합니다...${NC}"
docker-compose -f docker-compose.dev.yml down 2>/dev/null || true

# MySQL과 phpMyAdmin 시작
echo -e "${BLUE}📦 MySQL과 phpMyAdmin을 시작합니다...${NC}"
docker-compose -f docker-compose.dev.yml up -d

# 서비스가 완전히 시작될 때까지 대기
echo -e "${YELLOW}⏳ 서비스가 시작될 때까지 대기 중...${NC}"
sleep 5

# 서비스 상태 확인
echo -e "${BLUE}🔍 서비스 상태를 확인합니다...${NC}"
docker-compose -f docker-compose.dev.yml ps
echo ""

# MySQL 연결 확인
echo -e "${BLUE}🔗 MySQL 연결을 확인합니다...${NC}"
MAX_RETRIES=30
RETRY_COUNT=0

until docker-compose -f docker-compose.dev.yml exec -T mysql mysqladmin ping -h localhost --silent 2>/dev/null; do
    RETRY_COUNT=$((RETRY_COUNT+1))
    if [ $RETRY_COUNT -ge $MAX_RETRIES ]; then
        echo -e "${RED}❌ MySQL 시작 시간이 초과되었습니다.${NC}"
        echo "로그를 확인하세요: docker-compose -f docker-compose.dev.yml logs mysql"
        exit 1
    fi
    echo "MySQL이 시작될 때까지 대기 중... ($RETRY_COUNT/$MAX_RETRIES)"
    sleep 2
done

echo -e "${GREEN}✅ MySQL이 준비되었습니다!${NC}"
echo ""

# Gradle Wrapper 실행 권한 확인
if [ ! -x "./gradlew" ]; then
    echo -e "${YELLOW}⚠️  gradlew에 실행 권한이 없습니다. 권한을 부여합니다...${NC}"
    chmod +x ./gradlew
fi

# 환경 정보 출력
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}🎉 개발 환경이 준비되었습니다!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "${BLUE}📌 서비스 접속 정보:${NC}"
echo "  - 애플리케이션: http://localhost:8080/api"
echo "  - Swagger UI: http://localhost:8080/api/swagger-ui/index.html"
echo "  - phpMyAdmin: http://localhost:8081"
echo ""
echo -e "${BLUE}📌 데이터베이스 정보:${NC}"
echo "  - Host: localhost"
echo "  - Port: 3306"
echo "  - Database: buildup"
echo "  - Username: root (또는 설정한 사용자)"
echo ""
echo -e "${BLUE}📌 프로파일: ${GREEN}dev${NC}"
echo ""
echo -e "${YELLOW}⚠️  애플리케이션을 중지하려면 Ctrl+C를 누르세요.${NC}"
echo ""

# Spring Boot 애플리케이션 시작
echo -e "${BLUE}☕ Spring Boot 애플리케이션을 시작합니다...${NC}"
echo ""

# Gradle로 Spring Boot 실행 (dev 프로파일)
./gradlew bootRun --args='--spring.profiles.active=dev'

# 스크립트 종료 시 (Ctrl+C) Docker 컨테이너도 정리
trap cleanup EXIT

cleanup() {
    echo ""
    echo -e "${YELLOW}🛑 애플리케이션을 종료합니다...${NC}"
    echo -e "${BLUE}Docker 컨테이너를 유지하시겠습니까? (y/N)${NC}"
    read -t 10 -n 1 -r REPLY || REPLY="N"
    echo ""

    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo -e "${BLUE}🧹 Docker 컨테이너를 정리합니다...${NC}"
        docker-compose -f docker-compose.dev.yml down
        echo -e "${GREEN}✅ 정리 완료!${NC}"
    else
        echo -e "${GREEN}✅ Docker 컨테이너가 계속 실행됩니다.${NC}"
        echo "수동으로 정리하려면: docker-compose -f docker-compose.dev.yml down"
    fi
}