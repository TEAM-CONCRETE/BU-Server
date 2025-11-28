# 프론트엔드 테스트 데이터 관리 가이드

## 현재 상황

### EC2 서버에 이미 주입된 데이터
- `ec2-test-data.sql` ✅
- `ec2-additional-test-data.sql` ✅

### 워크플로우

```
로컬 개발 → SQL 작성 → 로컬 테스트 → Git 커밋 → EC2 배포 → EC2 SQL 실행
```

---

## 1. 스키마 동일성 확인 (필수!)

### 방법 A: 간단한 확인

**로컬에서**:
```bash
# 테이블 목록 확인
docker exec buildup-mysql mysql -u buildup -pbuildup123 -e "
USE buildup;
SHOW TABLES;
" | wc -l
```

**EC2에서** (SSH 접속):
```bash
# 테이블 목록 확인
docker exec buildup-mysql-prod mysql -u buildup -pbuildup123 -e "
USE buildup;
SHOW TABLES;
" | wc -l
```

→ 테이블 개수가 다르면 스키마 불일치!

### 방법 B: 상세 비교

**로컬에서**:
```bash
# 스키마 덤프
docker exec buildup-mysql mysqldump \
  -u buildup \
  -pbuildup123 \
  --no-data \
  --skip-comments \
  buildup > local-schema.sql

# 테이블 목록만 추출
grep "CREATE TABLE" local-schema.sql | sort > local-tables.txt
cat local-tables.txt
```

**EC2에서** (SSH 접속):
```bash
# 스키마 덤프
docker exec buildup-mysql-prod mysqldump \
  -u buildup \
  -pbuildup123 \
  --no-data \
  --skip-comments \
  buildup > ec2-schema.sql

# 테이블 목록만 추출
grep "CREATE TABLE" ec2-schema.sql | sort
```

→ 테이블 목록을 비교하여 차이 확인

---

## 2. 스키마가 동일한 경우

✅ **로컬 테스트 → EC2 적용 가능**

### STEP 1: 로컬에서 SQL 작성

```bash
# test-data/ 폴더에 새 SQL 파일 생성
cd test-data/
cat > my-test-data-$(date +%Y%m%d).sql << 'EOF'
USE buildup;

-- 테스트 데이터
INSERT INTO users (user_id, password, phone, email, role_id, is_deleted, profile_completed, created_at, updated_at)
VALUES
('mytest001', '$2a$10$d48wVDxrG/Paw.ULUn5.ae0IGrOu5415tyKW24RR5lKmqQcdgLdoy', '010-9999-9999', 'test@test.com', 1, 0, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE user_id = user_id;

-- 검증 쿼리
SELECT '데이터 주입 완료' AS Status;
SELECT COUNT(*) AS '생성된 사용자' FROM users WHERE user_id LIKE 'mytest%';
EOF
```

### STEP 2: 로컬 DB 테스트

```bash
# 로컬 DB에 적용
docker exec -i buildup-mysql mysql -u buildup -pbuildup123 buildup < my-test-data-$(date +%Y%m%d).sql

# 결과 확인
docker exec buildup-mysql mysql -u buildup -pbuildup123 -e "
USE buildup;
SELECT * FROM users WHERE user_id LIKE 'mytest%';
"
```

✅ 성공하면 다음 단계로!

### STEP 3: Git 커밋 및 푸시

```bash
# Git 커밋
git add test-data/my-test-data-*.sql
git commit -m "feat: 프론트엔드 테스트 데이터 추가"
git push origin develop
```

### STEP 4: EC2 배포 대기

GitHub Actions CI/CD가 자동으로 EC2에 배포합니다.
(몇 분 소요)

### STEP 5: EC2에서 SQL 실행

```bash
# EC2 SSH 접속
ssh ec2-user@your-ec2-ip

# Git Pull
cd /home/ec2-user/BU-Server
git pull origin develop

# SQL 실행
docker exec -i buildup-mysql-prod mysql -u buildup -pbuildup123 buildup < test-data/my-test-data-*.sql

# 검증
docker exec buildup-mysql-prod mysql -u buildup -pbuildup123 -e "
USE buildup;
SELECT * FROM users WHERE user_id LIKE 'mytest%';
"
```

---

## 3. 스키마가 다른 경우

❌ **먼저 스키마 동기화 필요**

### 원인
- 로컬에서 Entity 수정 → `ddl-auto: update`로 자동 반영
- EC2는 `ddl-auto: validate`라서 수동 마이그레이션 필요

### 해결 방법

#### Option 1: 로컬 스키마를 EC2에 적용 (권장)

```bash
# 1. 로컬에서 변경된 DDL 확인
# 애플리케이션 로그에서 CREATE TABLE, ALTER TABLE 문 찾기

# 2. 마이그레이션 SQL 작성
cat > migrations/sync-schema-$(date +%Y%m%d).sql << 'EOF'
USE buildup;

-- 예시: 새 컬럼 추가
ALTER TABLE employees ADD COLUMN IF NOT EXISTS new_field VARCHAR(100);

-- 예시: 새 테이블 추가
CREATE TABLE IF NOT EXISTS new_table (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
EOF

# 3. 로컬에서 테스트 (이미 있는 컬럼/테이블이면 무시됨)
docker exec -i buildup-mysql mysql -u buildup -pbuildup123 buildup < migrations/sync-schema-*.sql

# 4. Git 커밋
git add migrations/
git commit -m "feat: 스키마 동기화 마이그레이션"
git push

# 5. EC2에 적용
ssh ec2-user@your-ec2-ip
cd /home/ec2-user/BU-Server
git pull
docker exec -i buildup-mysql-prod mysql -u buildup -pbuildup123 buildup < migrations/sync-schema-*.sql
```

#### Option 2: EC2 DB를 완전히 리셋 (위험! 데이터 손실)

```bash
# ⚠️ 주의: 모든 데이터 삭제됨!

# EC2에서
docker-compose down mysql
docker volume rm buildup_mysql_data_prod
docker-compose up -d mysql

# 애플리케이션 재시작 (ddl-auto: validate가 테이블 생성하지 않으므로)
# JPA가 스키마를 생성하려면 임시로 ddl-auto: update로 변경 필요
```

---

## 4. NULL, BaseEntity 문제 해결

### 문제 원인
EC2 Instance Connect에서 수동 입력 시 누락되는 필드:
- `created_at`, `updated_at` (BaseEntity)
- `is_deleted`

### 해결책: SQL에 모든 필수 필드 포함

**나쁜 예** (Instance Connect에서 수동 입력):
```sql
-- ❌ 누락된 필드로 에러 발생
INSERT INTO users (user_id, password) VALUES ('test', 'hash');
```

**좋은 예** (SQL 파일 사용):
```sql
-- ✅ 모든 필수 필드 포함
INSERT INTO users (
    user_id,
    password,
    phone,
    email,
    role_id,
    is_deleted,           -- BaseEntity
    profile_completed,
    created_at,           -- BaseEntity
    updated_at            -- BaseEntity
)
VALUES (
    'test',
    '$2a$10$hash...',
    '010-1234-5678',
    'test@test.com',
    1,
    0,                    -- is_deleted = false
    1,                    -- profile_completed = true
    NOW(),                -- created_at
    NOW()                 -- updated_at
)
ON DUPLICATE KEY UPDATE updated_at = NOW();
```

---

## 5. 추천 워크플로우 (요약)

```bash
# === 1단계: 스키마 확인 ===
# 로컬
docker exec buildup-mysql mysql -u buildup -p -e "SHOW TABLES FROM buildup" | wc -l

# EC2 (비교)
ssh ec2-user@ip "docker exec buildup-mysql-prod mysql -u buildup -p -e 'SHOW TABLES FROM buildup'" | wc -l

# 개수가 다르면 → 스키마 동기화 먼저!

# === 2단계: SQL 파일 작성 ===
cat > test-data/new-data.sql << 'EOF'
USE buildup;

INSERT INTO ... (모든 필수 필드 포함)
VALUES (...);

ON DUPLICATE KEY UPDATE ...;

SELECT '완료' AS Status;
EOF

# === 3단계: 로컬 테스트 ===
docker exec -i buildup-mysql mysql -u buildup -pbuildup123 buildup < test-data/new-data.sql

# 에러 없으면 계속 진행

# === 4단계: Git 배포 ===
git add test-data/
git commit -m "feat: 테스트 데이터 추가"
git push

# === 5단계: EC2 적용 (CI/CD 완료 후) ===
ssh ec2-user@ip
cd BU-Server
git pull
docker exec -i buildup-mysql-prod mysql -u buildup -pbuildup123 buildup < test-data/new-data.sql
```

---

## 6. 자주 묻는 질문 (FAQ)

### Q1: 로컬과 EC2 스키마가 같은지 어떻게 확인?
```bash
# 간단 확인: 테이블 개수 비교
docker exec buildup-mysql mysql -u buildup -p -e "SHOW TABLES FROM buildup" | wc -l
```

### Q2: 한국어 데이터가 EC2에서 깨지는 이유?
EC2 Instance Connect 터미널이 UTF-8을 완벽 지원하지 않음.
→ **해결**: SQL 파일 사용 (로컬에서 UTF-8로 작성)

### Q3: 로컬에서 성공한 SQL이 EC2에서 실패?
1. **스키마 불일치**: 로컬과 EC2의 테이블 구조가 다름
2. **Role ID 불일치**: EC2의 roles 테이블 ID가 다를 수 있음
   ```sql
   -- 해결: 하드코딩 대신 SELECT 사용
   SET @role_corp = (SELECT id FROM roles WHERE role_name = 'ROLE_CORPORATION');
   INSERT INTO users (..., role_id, ...) VALUES (..., @role_corp, ...);
   ```

### Q4: ON DUPLICATE KEY UPDATE를 왜 사용?
```sql
ON DUPLICATE KEY UPDATE user_id = user_id;
```
- 같은 SQL을 여러 번 실행해도 안전
- 기존 데이터가 있으면 무시, 없으면 추가
- **기존 데이터 보호**

---

## 7. 테스트 계정 정보

### 기존 EC2 데이터 (`ec2-test-data.sql`)
- 기업: `testcorp` / Admin123!@
- 관리자: `testmanager` / Admin123!@
- 근로자: `testEmp01~03` / Admin123!@

### 추가 EC2 데이터 (`ec2-additional-test-data.sql`)
- 기업: `hangangcorp`, `namsancorp` / Admin123!@
- 현장 4개, 근로자 9명

---

## 8. 문제 발생 시

### 에러: "Unknown column 'xxx'"
→ 로컬과 EC2 스키마 불일치. 스키마 동기화 필요.

### 에러: "Duplicate entry"
→ 이미 존재하는 데이터. `ON DUPLICATE KEY UPDATE` 추가.

### 에러: "Cannot be null"
→ 필수 필드 누락. `created_at`, `updated_at`, `is_deleted` 추가.

---

**핵심**: 로컬 테스트 성공 → EC2 적용은 **스키마가 동일할 때만** 안전합니다!
