# 프론트엔드 API 테스트 데이터 가이드

생성일: 2025-01-26

## 📋 목차

1. [데이터 주입 방법](#데이터-주입-방법)
2. [포함된 테스트 데이터](#포함된-테스트-데이터)
3. [테스트 계정 정보](#테스트-계정-정보)
4. [API별 테스트 가능 항목](#api별-테스트-가능-항목)
5. [주의사항](#주의사항)

---

## 데이터 주입 방법

### ✅ 로컬 DB 테스트 (이미 완료)

```bash
# 1단계: 기본 데이터 주입
docker exec -i buildup-mysql mysql -u buildup -pbuildup123 buildup < test-data/frontend-complete-test-data.sql

# 2단계: 추가 데이터 주입 (계약, 급여, 근태 등)
docker exec -i buildup-mysql mysql -u buildup -pbuildup123 buildup < test-data/frontend-additional-data.sql
```

### 📤 EC2 서버에 적용

#### 전제조건: 스키마 동기화 필수!

```bash
# EC2에서 먼저 스키마 동기화
docker exec -i buildup-mysql-prod mysql -u buildup -pbuildup123 buildup < migrations/sync-schema-20250126.sql
```

#### 데이터 주입

```bash
# 방법 1: Git을 통한 배포 (권장)
git add test-data/
git commit -m "feat: 프론트엔드 종합 테스트 데이터 추가"
git push origin develop

# EC2에서
cd /home/ec2-user/BU-Server
git pull origin develop
docker exec -i buildup-mysql-prod mysql -u buildup -pbuildup123 buildup < test-data/frontend-complete-test-data.sql
docker exec -i buildup-mysql-prod mysql -u buildup -pbuildup123 buildup < test-data/frontend-additional-data.sql

# 방법 2: 직접 복사
scp -i your-key.pem test-data/frontend-*.sql ec2-user@your-ec2-ip:/home/ec2-user/
# 그 다음 EC2에서 실행
```

---

## 포함된 테스트 데이터

### 1. 기본 데이터 (`frontend-complete-test-data.sql`)

| 항목 | 개수 | 설명 |
|------|------|------|
| 기업 | 1개 | (주)프론트테스트건설 |
| 관리자 | 1명 | 이현장 |
| 현장 | 2개 | 강남 오피스텔, 송파 아파트 |
| 근로자 | 10명 | frontemp001~010 |

### 2. 추가 데이터 (`frontend-additional-data.sql`)

| 항목 | 개수 | 상태 | 설명 |
|------|------|------|------|
| **계약서** | 5개 | 3개 FULLY_SIGNED<br>2개 MANAGER_SIGNING_PENDING | 김철수, 이영희, 박민준 (정규직)<br>최지은, 정서연 (일용직) |
| **계약 상세** | 5개 | - | 급여, 근무시간, 4대보험 정보 포함 |
| **출근 기록** | 3개 | 오늘 날짜 | 정상 1명, 지각 2명 |
| **안전교육일지** | 2개 | MANAGER_SIGNING_PENDING | 추락재해, 전기작업 교육 |

---

## 테스트 계정 정보

### 🏢 기업 관리자 (CORPORATION)

**계정 정보:**
- ID: `frontcorp`
- PW: `Admin123!@`
- Role: `ROLE_CORPORATION`

**접근 가능한 API:**
- 현장 목록 조회
- 대시보드 조회
- 급여 목록/명세서
- 사원 관리
- 계약 관리

### 👷 현장 관리자 (MANAGER)

**계정 정보:**
- ID: `frontmgr`
- PW: `Admin123!@`
- Role: `ROLE_MANAGER`

**접근 가능한 API:**
- 현장 관리
- 근태 승인
- 작업일보 작성
- 안전교육일지 서명

### 👤 근로자 (EMPLOYEE)

**계정 정보:**
- ID: `frontemp001` ~ `frontemp010`
- PW: `Admin123!@` (공통)
- Role: `ROLE_EMPLOYEE`

**근로자 목록:**

| ID | 이름 | 고용 형태 | 계약 상태 |
|----|------|-----------|-----------|
| frontemp001 | 김철수 | 정규직 | ✅ FULLY_SIGNED |
| frontemp002 | 이영희 | 정규직 | ✅ FULLY_SIGNED |
| frontemp003 | 박민준 | 정규직 | ✅ FULLY_SIGNED |
| frontemp004 | 최지은 | 일용직 | ⏳ MANAGER_SIGNING_PENDING |
| frontemp005 | 정서연 | 일용직 | ⏳ MANAGER_SIGNING_PENDING |
| frontemp006 | 한지민 | 일용직 | - |
| frontemp007 | 강동원 | 정규직 | - |
| frontemp008 | 송혜교 | 정규직 | - |
| frontemp009 | 유재석 | 일용직 | - |
| frontemp010 | 아이유 | 일용직 | - |

---

## API별 테스트 가능 항목

### ✅ 대시보드 API

**Endpoint:** `GET /v1/sites`

**로그인:** `frontcorp` / `Admin123!@`

**예상 응답:**
```json
{
  "success": true,
  "data": [
    {
      "siteName": "강남 오피스텔 A동 신축현장",
      "siteAddress": "서울특별시 강남구 역삼동 123-45",
      "clientName": "서울시설공단",
      "manager": {
        "name": "이현장"
      }
    },
    {
      "siteName": "송파 아파트 B동 리모델링",
      ...
    }
  ]
}
```

### ✅ 계약 API

**Endpoint:** `GET /v1/contracts`

**로그인:** `frontcorp` / `Admin123!@`

**테스트 가능:**
- 전체 계약 목록 조회 (5개)
- 계약 상태 필터링 (FULLY_SIGNED: 3개, MANAGER_SIGNING_PENDING: 2개)
- 계약 상세 정보 조회

### ✅ 근태 API

**Endpoint:** `GET /v1/attendance`

**로그인:** `frontcorp` / `Admin123!@`

**테스트 가능:**
- 오늘 출근 기록 조회 (3개)
- 지각 판단 기능 (2명 지각)
  - 이영희: 08:10 출근 (08:00 + 5분 초과)
  - 박민준: 08:40 출근 (08:30 + 5분 초과)

### ✅ 안전교육일지 API

**Endpoint:** `GET /v1/safety-education-logs`

**로그인:** `frontcorp` / `Admin123!@`

**테스트 가능:**
- 서명 대기 중인 안전교육일지 조회 (2개)
- 안전교육 상세 정보

### ✅ 근로자 API

**Endpoint:** `GET /v1/employees`

**로그인:** `frontcorp` / `Admin123!@`

**테스트 가능:**
- 전체 근로자 목록 (10명)
- 고용 형태별 필터링 (정규직 4명, 일용직 6명)
- 근로자 검색

### ⚠️ 급여 API (추가 데이터 필요 시)

현재는 계약 데이터만 있습니다. 실제 급여 데이터가 필요하면 별도로 생성해야 합니다.

### ⚠️ 작업일보 API (추가 데이터 필요 시)

현재는 작업일보 데이터가 없습니다. 필요 시 별도 생성 가능합니다.

---

## 주의사항

### ⚠️ 스키마 동기화 필수

EC2에 데이터를 주입하기 전에 **반드시** 스키마 동기화를 먼저 수행하세요:

```bash
docker exec -i buildup-mysql-prod mysql -u buildup -pbuildup123 buildup < migrations/sync-schema-20250126.sql
```

**스키마 차이:**
- `contract_details`: 로컬 33개 컬럼, EC2 29개 컬럼
- `work_reports`: 로컬 16개 컬럼, EC2 18개 컬럼

### 🔄 재실행 안전성

모든 SQL 파일은 `ON DUPLICATE KEY UPDATE`를 사용하여 **여러 번 실행해도 안전**합니다.

### 🗑️ 데이터 정리

테스트 데이터를 삭제하려면:

```sql
USE buildup;
DELETE FROM attendance_records WHERE employee_id IN (SELECT id FROM employees WHERE user_id LIKE 'frontemp%');
DELETE FROM safety_education_logs WHERE instructor_name LIKE '이현장%' OR instructor_name = '김전기';
DELETE FROM contract_details WHERE contract_id IN (SELECT id FROM contracts WHERE employee_id IN (SELECT id FROM employees WHERE user_id LIKE 'frontemp%'));
DELETE FROM contracts WHERE employee_id IN (SELECT id FROM employees WHERE user_id LIKE 'frontemp%');
DELETE FROM employees WHERE user_id LIKE 'frontemp%';
DELETE FROM sites WHERE site_name LIKE '%강남%' OR site_name LIKE '%송파%';
DELETE FROM managers WHERE manager_name = '이현장';
DELETE FROM corporations WHERE corp_name LIKE '%프론트%';
DELETE FROM users WHERE user_id IN ('frontcorp', 'frontmgr') OR user_id LIKE 'frontemp%';
```

### 📊 데이터 검증

주입 후 데이터를 확인하려면:

```sql
-- 기본 데이터 확인
SELECT COUNT(*) AS '기업' FROM corporations WHERE corp_name LIKE '%프론트%';
SELECT COUNT(*) AS '현장' FROM sites WHERE site_name LIKE '%강남%' OR site_name LIKE '%송파%';
SELECT COUNT(*) AS '근로자' FROM employees WHERE user_id LIKE 'frontemp%';

-- 추가 데이터 확인
SELECT COUNT(*) AS '계약' FROM contracts WHERE is_deleted = 0;
SELECT COUNT(*) AS '출근기록' FROM attendance_records WHERE DATE(timestamp) = CURDATE();
SELECT COUNT(*) AS '안전교육' FROM safety_education_logs WHERE status = 'MANAGER_SIGNING_PENDING';
```

---

## 📞 문의 및 추가 데이터 요청

추가 테스트 데이터가 필요하거나 문제가 발생하면 개발팀에 문의하세요.

**생성된 파일:**
- `test-data/frontend-complete-test-data.sql` - 기본 데이터
- `test-data/frontend-additional-data.sql` - 계약/근태/안전교육 데이터
- `migrations/sync-schema-20250126.sql` - 스키마 동기화 SQL
