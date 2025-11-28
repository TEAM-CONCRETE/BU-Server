# Build-Up Platform 운영 가이드

## 📋 목차
1. [프로젝트 구조 분석](#프로젝트-구조-분석)
2. [개발 vs 운영 환경](#개발-vs-운영-환경)
3. [DB 구조 관리 전략](#db-구조-관리-전략)
4. [운영 배포 가이드](#운영-배포-가이드)
5. [시연용 테스트 데이터 주입](#시연용-테스트-데이터-주입)
6. [모니터링 및 유지보수](#모니터링-및-유지보수)

---

## 프로젝트 구조 분석

### 환경별 설정 파일

```
src/main/resources/
├── application.yml              # 공통 설정 (JWT, 서버 포트 등)
├── application-dev.yml          # 개발 환경 (로컬 Docker MySQL)
├── application-prod.yml         # 운영 환경 (AWS RDS MySQL)
└── application-test.yml         # 테스트 환경 (H2 In-Memory)

docker/mysql/init/               # 개발 환경용 DB 초기화 스크립트
├── 01-create-schema.sql         # 스키마 생성
├── 02-insert-sample-data.sql    # 샘플 데이터
└── 05-insert-auth-site-attendance-data.sql  # 기본 데이터
```

### 프로파일 전환

- **로컬 개발**: `spring.profiles.active=dev` (기본값)
- **운영 환경**: `spring.profiles.active=prod`
- **테스트**: `spring.profiles.active=test`

---

## 개발 vs 운영 환경

### 개발 환경 (dev)

| 항목 | 설정 | 설명 |
|------|------|------|
| **DB 관리** | `ddl-auto: update` | JPA 엔티티 기준으로 자동 스키마 생성/변경 |
| **Flyway** | `disabled` | 마이그레이션 사용 안 함 |
| **SQL 로깅** | `show-sql: true` | 모든 SQL 쿼리 출력 |
| **로그 레벨** | `DEBUG` | 상세 로그 |
| **Swagger** | `enabled: true` | API 문서 활성화 |
| **에러 메시지** | 상세 출력 | 스택트레이스 포함 |

**장점**: 빠른 개발, 엔티티 변경 시 자동 반영
**단점**: 프로덕션 부적합 (데이터 유실 위험)

### 운영 환경 (prod)

| 항목 | 설정 | 설명 |
|------|------|------|
| **DB 관리** | `ddl-auto: validate` | 스키마 변경 금지, 검증만 수행 |
| **Flyway** | `disabled` | 현재 비활성화 (수동 마이그레이션) |
| **SQL 로깅** | `show-sql: false` | SQL 출력 안 함 |
| **로그 레벨** | `WARN` | 최소 로그 |
| **Swagger** | `enabled: true` | **⚠️ 보안상 비활성화 권장** |
| **에러 메시지** | 최소화 | 스택트레이스 노출 안 함 |

**장점**: 안정성, 성능, 보안
**단점**: 스키마 변경 시 수동 작업 필요

---

## DB 구조 관리 전략

### 현재 개발 단계: JPA 엔티티 기반

**개발 환경에서**:
1. Entity 클래스 작성/수정
2. 애플리케이션 재시작 → `ddl-auto: update`가 자동으로 테이블 생성/수정
3. `docker/mysql/init/` 스크립트는 초기 데이터용

### 운영 배포 시: 수동 마이그레이션

**방법 1: SQL 스크립트 직접 실행 (현재 방식)**

1. **스키마 변경 시**:
   ```bash
   # 개발 환경에서 JPA가 생성한 DDL 확인
   # application-dev.yml에서 show-sql: true로 설정
   # 로그에서 CREATE TABLE, ALTER TABLE 추출
   ```

2. **운영 DB에 적용**:
   ```sql
   -- 변경 사항을 SQL 파일로 작성
   -- migration/V1__add_new_column.sql

   ALTER TABLE employees ADD COLUMN new_field VARCHAR(100);
   ```

3. **RDS에 직접 실행**:
   ```bash
   mysql -h your-rds-endpoint.rds.amazonaws.com \
         -u admin \
         -p \
         buildup < migration/V1__add_new_column.sql
   ```

**방법 2: Flyway 활성화 (권장)**

현재 프로젝트는 Flyway가 비활성화되어 있지만, 운영에서는 활성화를 권장합니다.

1. **Flyway 활성화**:
   ```yaml
   # application-prod.yml
   spring:
     flyway:
       enabled: true
       baseline-on-migrate: true  # 기존 DB에 적용
       locations: classpath:db/migration
   ```

2. **마이그레이션 파일 작성**:
   ```
   src/main/resources/db/migration/
   ├── V1__initial_schema.sql
   ├── V2__add_employee_fields.sql
   └── V3__add_safety_education_log.sql
   ```

3. **배포 시 자동 적용**: 애플리케이션 시작 시 Flyway가 자동으로 마이그레이션 실행

---

## 운영 배포 가이드

### 1. 사전 준비

#### 필수 환경 변수

```bash
# DB 연결
export SPRING_DATASOURCE_URL="jdbc:mysql://your-rds-endpoint:3306/buildup?serverTimezone=Asia/Seoul&characterEncoding=UTF-8"
export SPRING_DATASOURCE_USERNAME="admin"
export SPRING_DATASOURCE_PASSWORD="your-secure-password"

# JWT
export JWT_SECRET="your-64-character-jwt-secret-key-for-hs512-algorithm-must-be-secure"

# 암호화
export ENCRYPTION_KEY="your-32-byte-encryption-key-for-aes256"

# AWS S3
export AWS_S3_ENABLED="true"
export AWS_S3_BUCKET="buildup-production"
export AWS_REGION="ap-northeast-2"
export AWS_ACCESS_KEY_ID="your-access-key"
export AWS_SECRET_ACCESS_KEY="your-secret-key"

# AI 서비스
export AI_FACE_SIMILARITY_BASE_URL="https://your-ai-service.com"
export AI_FACE_SIMILARITY_API_KEY="your-api-key"
```

### 2. 빌드

```bash
# JAR 파일 생성
./gradlew clean build -Pprofile=prod

# 생성된 파일 확인
ls -lh build/libs/buildup-0.0.1-SNAPSHOT.jar
```

### 3. EC2 배포

#### 방법 A: 직접 실행

```bash
# JAR 업로드
scp -i your-key.pem build/libs/buildup-0.0.1-SNAPSHOT.jar ec2-user@your-ec2-ip:/home/ec2-user/

# EC2에서 실행
ssh -i your-key.pem ec2-user@your-ec2-ip
cd /home/ec2-user

# 환경 변수 설정
source /etc/environment  # 또는 .env 파일 사용

# 실행
java -jar \
  -Dspring.profiles.active=prod \
  -Xms512m -Xmx2g \
  buildup-0.0.1-SNAPSHOT.jar
```

#### 방법 B: Docker (권장)

1. **Dockerfile 작성**:
   ```dockerfile
   FROM openjdk:21-jdk-slim

   WORKDIR /app

   COPY build/libs/buildup-0.0.1-SNAPSHOT.jar app.jar

   EXPOSE 8080

   ENTRYPOINT ["java", \
               "-Dspring.profiles.active=prod", \
               "-Xms512m", "-Xmx2g", \
               "-jar", "app.jar"]
   ```

2. **빌드 및 실행**:
   ```bash
   # 이미지 빌드
   docker build -t buildup-backend:latest .

   # 실행
   docker run -d \
     --name buildup-backend \
     -p 8080:8080 \
     --env-file .env \
     buildup-backend:latest
   ```

#### 방법 C: Systemd 서비스 등록

```bash
# /etc/systemd/system/buildup.service
[Unit]
Description=Build-Up Backend Service
After=network.target

[Service]
Type=simple
User=ec2-user
WorkingDirectory=/home/ec2-user
EnvironmentFile=/home/ec2-user/.env
ExecStart=/usr/bin/java -jar \
  -Dspring.profiles.active=prod \
  -Xms512m -Xmx2g \
  /home/ec2-user/buildup-0.0.1-SNAPSHOT.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

```bash
# 서비스 시작
sudo systemctl daemon-reload
sudo systemctl enable buildup
sudo systemctl start buildup

# 상태 확인
sudo systemctl status buildup
sudo journalctl -u buildup -f
```

### 4. 헬스 체크

```bash
# 애플리케이션 상태 확인
curl http://your-ec2-ip:8080/api/actuator/health

# 응답 예시
{
  "status": "UP"
}
```

---

## 시연용 테스트 데이터 주입

### 운영 환경에서 안전하게 테스트 데이터 주입하기

**⚠️ "기존 데이터에 영향을 주지 않도록 주의" 의미**:
- 시연용 데이터를 추가할 때, **이미 운영 중인 실제 데이터를 삭제하거나 덮어쓰지 말 것**
- `DELETE FROM ...` 사용 금지
- `INSERT`에 `ON DUPLICATE KEY UPDATE` 또는 중복 체크 사용

**실제 DB 구성**:
- RDS 사용 안 함 ❌
- EC2 내부 Docker MySQL 컨테이너 사용 ✅
- 컨테이너명: `buildup-mysql-prod`
- 접속: `docker exec -it buildup-mysql-prod mysql -u buildup -p`

#### 방법 1: SQL 파일을 통한 주입 (권장)

**1. 로컬에서 테스트 데이터 SQL 작성**

```bash
# 파일 생성
cat > demo-test-data.sql << 'EOF'
-- 시연용 테스트 데이터
-- ⚠️ DELETE 사용 금지! 기존 데이터 보호!
-- ✅ INSERT ... ON DUPLICATE KEY 또는 WHERE NOT EXISTS 사용

USE buildup;

-- 1. 테스트 기업 추가
INSERT INTO corporations (corp_name, corp_ceo_name, corp_address, corp_phone, corp_registration_num, user_id, is_deleted, created_at, updated_at)
SELECT '(주)시연건설', '김대표', '서울특별시 강남구 테헤란로 123', '02-1234-5678', '123-45-67890', id, 0, NOW(), NOW()
FROM users WHERE user_id = 'democorp'
ON DUPLICATE KEY UPDATE corp_name = corp_name;

-- 2. 테스트 현장 추가
INSERT INTO sites (site_name, site_address, client_name, start_date, end_date, manager_secret_key, employee_secret_key, corporation_id, manager_id, is_deleted, created_at, updated_at)
SELECT
    '강남 오피스텔 신축현장',
    '서울특별시 강남구 역삼동 123-45',
    '서울시설공단',
    '2025-01-01',
    '2025-12-31',
    CONCAT('MGR_', UUID()),
    CONCAT('EMP_', UUID()),
    c.id,
    (SELECT id FROM managers WHERE manager_name = 'Demo Manager' LIMIT 1),
    0,
    NOW(),
    NOW()
FROM corporations c
WHERE c.corp_name = '(주)시연건설'
LIMIT 1
ON DUPLICATE KEY UPDATE site_name = site_name;

-- 검증
SELECT '시연 데이터 주입 완료' as Status;
SELECT COUNT(*) as '기업 수' FROM corporations WHERE corp_name LIKE '%시연%';
SELECT COUNT(*) as '현장 수' FROM sites WHERE site_name LIKE '%강남%';
EOF
```

**2. Git을 통한 배포**

```bash
# 로컬에서
git add demo-test-data.sql
git commit -m "feat: 시연용 테스트 데이터 SQL 추가"
git push origin main

# EC2에서
cd /home/ec2-user/BU-Server
git pull origin main

# RDS에 적용
mysql -h your-rds-endpoint.rds.amazonaws.com \
      -u admin \
      -p \
      buildup < demo-test-data.sql
```

**3. SCP를 통한 직접 업로드**

```bash
# 로컬에서
scp -i your-key.pem demo-test-data.sql ec2-user@your-ec2-ip:/home/ec2-user/

# EC2에서
ssh -i your-key.pem ec2-user@your-ec2-ip
mysql -h your-rds-endpoint.rds.amazonaws.com \
      -u admin \
      -p \
      buildup < /home/ec2-user/demo-test-data.sql
```

#### 방법 2: API를 통한 주입

운영 환경에서 회원가입/데이터 생성 API를 직접 호출합니다.

```bash
# 1. 기업 관리자 회원가입
curl -X POST http://your-domain.com/api/v1/auth/register/corporation \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "democorp",
    "password": "DemoPassword123!",
    "corpName": "(주)시연건설",
    "corpCeoName": "김대표",
    "corpAddress": "서울특별시 강남구",
    "corpPhone": "02-1234-5678"
  }'

# 2. 로그인
curl -X POST http://your-domain.com/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "democorp",
    "password": "DemoPassword123!"
  }' \
  -c cookies.txt

# 3. 현장 등록
curl -X POST http://your-domain.com/api/v1/sites \
  -H "Content-Type: application/json" \
  -b cookies.txt \
  -d '{
    "siteName": "강남 오피스텔 신축현장",
    "siteAddress": "서울특별시 강남구 역삼동 123-45",
    "clientName": "서울시설공단",
    "startDate": "2025-01-01",
    "endDate": "2025-12-31"
  }'
```

#### 방법 3: 관리자 대시보드 (향후 개발)

운영 환경용 관리자 웹 대시보드를 개발하여 GUI로 테스트 데이터 관리.

---

## EC2 한국어 인코딩 문제 해결

### 문제: EC2 Instance Connect에서 한국어 깨짐

EC2 Instance Connect 터미널은 UTF-8을 완벽히 지원하지 않습니다.

### 해결책

#### 1. SSH 클라이언트 사용 (권장)

```bash
# macOS/Linux Terminal에서
ssh -i your-key.pem ec2-user@your-ec2-ip

# EC2에서 로케일 확인
locale

# UTF-8 설정
export LANG=ko_KR.UTF-8
export LC_ALL=ko_KR.UTF-8

# MySQL에서 한국어 입력
mysql -h rds-endpoint -u admin -p buildup
```

#### 2. SQL 파일 업로드 (가장 안전)

위의 "방법 1: SQL 파일을 통한 주입" 참고

#### 3. Base64 인코딩 (임시 방편)

```bash
# 로컬에서 Base64 인코딩
echo "INSERT INTO corporations (corp_name) VALUES ('(주)한글테스트');" | base64

# EC2에서 디코딩 후 실행
echo "SW5TRVJUIE..." | base64 -d | docker exec -i buildup-mysql-prod mysql -u buildup -pbuildup123 buildup
```

---

## 모니터링 및 유지보수

### 1. 로그 확인

```bash
# Systemd 서비스 로그
sudo journalctl -u buildup -f

# Docker 로그
docker logs -f buildup-backend

# 애플리케이션 로그 파일 (설정 시)
tail -f /var/log/buildup/app.log
```

### 2. 성능 모니터링

```bash
# Actuator 메트릭
curl http://localhost:8080/api/actuator/metrics

# JVM 메모리
curl http://localhost:8080/api/actuator/metrics/jvm.memory.used

# DB 커넥션 풀
curl http://localhost:8080/api/actuator/metrics/hikaricp.connections
```

### 3. DB 백업

```bash
# Docker MySQL 백업
docker exec buildup-mysql-prod mysqldump \
  -u buildup \
  -pbuildup123 \
  --databases buildup \
  --single-transaction \
  --routines \
  --triggers \
  > backup-$(date +%Y%m%d-%H%M%S).sql

# S3에 업로드
aws s3 cp backup-*.sql s3://buildup-backups/

# 복구
docker exec -i buildup-mysql-prod mysql -u buildup -pbuildup123 buildup < backup-20250126.sql

# 정기 백업 Cron 설정
crontab -e
# 매일 새벽 3시 백업
0 3 * * * docker exec buildup-mysql-prod mysqldump -u buildup -pbuildup123 buildup > /home/ec2-user/backups/backup-$(date +\%Y\%m\%d).sql && aws s3 cp /home/ec2-user/backups/backup-$(date +\%Y\%m\%d).sql s3://buildup-backups/
```

### 4. 스키마 변경 프로세스

```
1. 개발 환경에서 Entity 수정
   ↓
2. 로컬에서 테스트 (ddl-auto: update가 자동 적용)
   ↓
3. 변경된 DDL 추출 (로그 확인)
   ↓
4. 마이그레이션 SQL 파일 작성
   ↓
5. 스테이징 환경에서 테스트
   ↓
6. 운영 DB 백업
   ↓
7. 운영 DB에 마이그레이션 적용
   ↓
8. application-prod.yml의 ddl-auto: validate가 검증
```

---

## 체크리스트

### 배포 전

- [ ] 환경 변수 모두 설정
- [ ] DB 마이그레이션 준비
- [ ] DB 백업 완료
- [ ] 테스트 통과 (`./gradlew test`)
- [ ] 보안 설정 확인 (Swagger 비활성화, 에러 메시지 최소화)
- [ ] S3 버킷 권한 확인

### 배포 후

- [ ] 헬스 체크 통과
- [ ] Swagger 접속 확인 (http://your-domain/api/swagger-ui.html)
- [ ] 로그인 테스트
- [ ] 주요 API 테스트
- [ ] 모니터링 대시보드 확인
- [ ] 로그 레벨 확인

### 시연 준비

- [ ] 시연용 계정 생성 (democorp, demomgr 등)
- [ ] 테스트 데이터 주입 (현장, 근로자, 계약 등)
- [ ] 대시보드 데이터 확인
- [ ] PDF 생성 테스트 (계약서, 급여명세서)
- [ ] 얼굴 인식 테스트 (AI 서비스 연동)

---

## 문제 해결

### 자주 발생하는 문제

**1. DB 커넥션 에러**
```
원인: RDS 보안 그룹에서 EC2 IP 미허용
해결: RDS 보안 그룹에 EC2 보안 그룹 추가
```

**2. JPA ValidationException**
```
원인: 엔티티와 실제 DB 스키마 불일치
해결: ddl-auto: validate이므로 DB 스키마를 먼저 수정
```

**3. S3 업로드 실패**
```
원인: IAM 권한 부족
해결: S3 버킷 정책 확인, IAM 역할에 s3:PutObject 권한 추가
```

**4. 한국어 인코딩 깨짐**
```
원인: DB charset 설정 또는 애플리케이션 인코딩 문제
해결:
- DB: ALTER DATABASE buildup CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
- 애플리케이션: server.servlet.encoding.charset=UTF-8 (이미 설정됨)
```

---

## 결론

### DB 구성 요약

| 항목 | 개발 환경 | 운영 환경 |
|------|-----------|-----------|
| **DB 위치** | Docker (로컬) | Docker (EC2) |
| **컨테이너명** | `buildup-mysql` | `buildup-mysql-prod` |
| **JPA 설정** | `ddl-auto: update` | `ddl-auto: validate` |
| **스키마 관리** | 자동 생성 | 수동 마이그레이션 |
| **접속 방법** | `localhost:3306` | `docker exec -it ...` |

### 핵심 포인트

- ✅ **RDS 사용 안 함** - EC2 Docker MySQL 사용
- ✅ **기존 데이터 보호** - `DELETE` 금지, `ON DUPLICATE KEY` 사용
- ✅ **한국어 인코딩** - SSH 클라이언트 또는 SQL 파일 업로드
- ✅ **백업** - `mysqldump` + S3 업로드 + Cron 자동화
- ✅ **스키마 변경** - 개발에서 DDL 추출 → SQL 파일 작성 → Docker exec 실행

### 시연 데이터 주입 (3단계)

```bash
# 1. 로컬에서 SQL 작성 (한국어 OK)
cat > demo.sql << 'EOF'
INSERT INTO corporations (...) VALUES ('(주)시연건설', ...);
EOF

# 2. EC2로 전송
scp demo.sql ec2-user@ip:/home/ec2-user/

# 3. Docker MySQL에 적용
docker exec -i buildup-mysql-prod mysql -u buildup -p < demo.sql
```

운영 환경 안정화를 위해 Flyway 활성화를 권장합니다.
