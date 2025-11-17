#!/bin/bash
# Build-Up Backend 개발 환경 시작 스크립트

set -e  # 에러 발생 시 중단

echo "🚀 Build-Up Backend 개발 환경을 시작합니다..."
echo ""

# 색상 정의
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# ✅ Docker Compose 버전 감지
if command -v docker-compose &> /dev/null; then
    COMPOSE_CMD="docker-compose"
elif docker compose version &> /dev/null; then
    COMPOSE_CMD="docker compose"
else
    echo -e "${RED}❌ Docker Compose가 설치되어 있지 않습니다.${NC}"
    echo "설치 명령:"
    echo "  sudo apt-get install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin -y"
    exit 1
fi

echo -e "${BLUE}🧩 Docker Compose 명령어 감지됨: ${GREEN}${COMPOSE_CMD}${NC}"
echo ""

# .env 파일 로드
if [ -f ".env" ]; then
    echo -e "${BLUE}📄 .env 파일을 로드합니다...${NC}"
    while IFS= read -r line || [ -n "$line" ]; do
        [[ -z "$line" || "$line" =~ ^[[:space:]]*# ]] && continue
        line="${line%%#*}"
        line="$(echo "$line" | xargs)"
        if [[ "$line" == *"="* ]]; then
            export "$line"
        fi
    done < .env
    echo -e "${GREEN}✅ 환경 변수가 로드되었습니다.${NC}"
    echo ""
else
    echo -e "${YELLOW}⚠️  .env 파일이 없습니다.${NC}"
    echo "  cp .env.example .env"
    echo ""
fi

# Compose 파일 확인
if [ ! -f "docker-compose.dev.yml" ]; then
    echo -e "${RED}❌ docker-compose.dev.yml 파일이 없습니다.${NC}"
    exit 1
fi

# Docker 실행 여부 확인
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}❌ Docker가 실행 중이 아닙니다.${NC}"
    echo "  sudo systemctl start docker"
    exit 1
fi

# 기존 컨테이너 정리
echo -e "${BLUE}🧹 기존 컨테이너 정리 중...${NC}"
$COMPOSE_CMD -f docker-compose.dev.yml down 2>/dev/null || true

# MySQL과 phpMyAdmin 시작
echo -e "${BLUE}📦 MySQL과 phpMyAdmin을 시작합니다...${NC}"
$COMPOSE_CMD -f docker-compose.dev.yml up -d

# 서비스 대기
echo -e "${YELLOW}⏳ 서비스 시작 대기 중...${NC}"
sleep 5

# 상태 확인
echo -e "${BLUE}🔍 서비스 상태 확인:${NC}"
$COMPOSE_CMD -f docker-compose.dev.yml ps
echo ""

# MySQL 준비 대기
echo -e "${BLUE}🔗 MySQL 연결을 확인합니다...${NC}"
MAX_RETRIES=30
RETRY_COUNT=0

until $COMPOSE_CMD -f docker-compose.dev.yml exec -T mysql mysqladmin ping -h localhost --silent 2>/dev/null; do
    RETRY_COUNT=$((RETRY_COUNT+1))
    if [ $RETRY_COUNT -ge $MAX_RETRIES ]; then
        echo -e "${RED}❌ MySQL 시작 시간이 초과되었습니다.${NC}"
        echo "로그 확인: $COMPOSE_CMD -f docker-compose.dev.yml logs mysql"
        exit 1
    fi
    echo "MySQL이 시작될 때까지 대기 중... ($RETRY_COUNT/$MAX_RETRIES)"
    sleep 2
done

echo -e "${GREEN}✅ MySQL이 준비되었습니다!${NC}"
echo ""

# Gradle Wrapper 실행 권한 확인
if [ ! -x "./gradlew" ]; then
    echo -e "${YELLOW}⚠️  gradlew 권한 부여 중...${NC}"
    chmod +x ./gradlew
fi

# 환경 정보 출력
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}🎉 개발 환경이 준비되었습니다!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "${BLUE}📌 서비스 접속 정보:${NC}"
echo "  - 애플리케이션: http://localhost:8080/api"
echo "  - phpMyAdmin: http://localhost:8081"
echo ""
echo -e "${BLUE}📌 데이터베이스:${NC}"
echo "  - Host: localhost"
echo "  - Port: 3306"
echo "  - DB: buildup"
echo "  - User: root (또는 ${MYSQL_USER})"
echo ""
echo -e "${BLUE}📌 프로파일: ${GREEN}dev${NC}"
echo ""
echo -e "${YELLOW}⚠️  중지하려면 Ctrl+C${NC}"
echo ""

# Spring Boot 애플리케이션 실행
echo -e "${BLUE}☕ Spring Boot 애플리케이션을 시작합니다...${NC}"
./gradlew bootRun --args='--spring.profiles.active=dev'

# 종료 처리
trap cleanup EXIT

cleanup() {
    echo ""
    echo -e "${YELLOW}🛑 애플리케이션 종료 중...${NC}"
    echo -e "${BLUE}Docker 컨테이너를 유지하시겠습니까? (y/N)${NC}"
    read -t 10 -n 1 -r REPLY || REPLY="N"
    echo ""
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo -e "${BLUE}🧹 Docker 컨테이너 정리 중...${NC}"
        $COMPOSE_CMD -f docker-compose.dev.yml down
        echo -e "${GREEN}✅ 정리 완료${NC}"
    else
        echo -e "${GREEN}✅ 컨테이너 유지됨${NC}"
        echo "수동 종료: $COMPOSE_CMD -f docker-compose.dev.yml down"
    fi
}
