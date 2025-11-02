# Flyway 마이그레이션 가이드

## 개요

Flyway는 데이터베이스 스키마의 버전 관리를 위한 도구입니다. SQL 기반 마이그레이션을 통해 데이터베이스 변경사항을 추적하고 관리합니다.

## 디렉토리 구조

```
src/main/resources/db/migration/
├── V1__init_schema.sql          # 초기 스키마
├── V2__add_user_profile.sql     # 사용자 프로필 추가 (예시)
└── V3__update_contract_table.sql # 계약 테이블 수정 (예시)
```

## 네이밍 규칙

### 일반 마이그레이션
- 형식: `V{버전}__{설명}.sql`
- 예시:
  - `V1__init_schema.sql`
  - `V2__add_employee_address.sql`
  - `V3__create_notification_table.sql`

### 반복 가능 마이그레이션 (Repeatable)
- 형식: `R__{설명}.sql`
- 매번 체크섬이 변경되면 실행됨
- 주로 뷰, 함수, 프로시저 등에 사용

## 작성 규칙

### 1. 기본 구조

```sql
-- ============================================
-- 설명: 테이블 추가/수정 내용
-- 버전: V2
-- 작성자: 홍길동
-- 작성일: 2025-11-02
-- ============================================

-- 변경사항 1
CREATE TABLE IF NOT EXISTS ...;

-- 변경사항 2
ALTER TABLE ... ADD COLUMN ...;

-- 인덱스 추가
CREATE INDEX idx_... ON ...;
```

### 2. 주의사항

**필수 사항:**
- ✅ `IF NOT EXISTS` 사용 (멱등성 보장)
- ✅ 트랜잭션 고려 (DDL은 자동 커밋됨)
- ✅ 롤백 불가능한 변경에 주의
- ✅ 운영 데이터 백업 후 실행

**금지 사항:**
- ❌ 이미 배포된 마이그레이션 파일 수정 금지
- ❌ 버전 번호 중복 금지
- ❌ DROP TABLE 사용 지양 (데이터 손실 위험)

### 3. 권장 패턴

#### 컬럼 추가
```sql
-- 안전한 방법
ALTER TABLE employee
ADD COLUMN email VARCHAR(255) NULL COMMENT '이메일';

-- 기본값 설정 (선택)
UPDATE employee SET email = CONCAT(name, '@example.com') WHERE email IS NULL;

-- NOT NULL 제약조건 추가 (필요시)
ALTER TABLE employee MODIFY COLUMN email VARCHAR(255) NOT NULL;
```

#### 테이블 추가
```sql
CREATE TABLE IF NOT EXISTS notification (
    notification_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES employee(employee_id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### 데이터 마이그레이션
```sql
-- 기존 데이터 마이그레이션
INSERT INTO new_table (column1, column2)
SELECT old_column1, old_column2
FROM old_table
WHERE condition;
```

## Flyway 실행

### 자동 실행 (권장)
애플리케이션 시작 시 자동으로 마이그레이션이 실행됩니다.

```bash
./gradlew bootRun
```

### Gradle 명령어

```bash
# 마이그레이션 정보 확인
./gradlew flywayInfo

# 마이그레이션 실행
./gradlew flywayMigrate

# 마이그레이션 검증
./gradlew flywayValidate

# 마이그레이션 정리 (주의: 모든 테이블 삭제)
./gradlew flywayClean
```

### CLI 명령어 (선택)

```bash
# Flyway CLI 설치 필요
flyway -configFiles=flyway.conf info
flyway -configFiles=flyway.conf migrate
```

## 마이그레이션 히스토리 확인

Flyway는 `flyway_schema_history` 테이블에 실행 이력을 저장합니다.

```sql
-- 마이그레이션 히스토리 조회
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
```

## 트러블슈팅

### 1. 마이그레이션 실패 시

```bash
# 오류 확인
SELECT * FROM flyway_schema_history WHERE success = 0;

# 수동으로 상태 업데이트 (주의!)
DELETE FROM flyway_schema_history WHERE version = '2';

# 다시 마이그레이션 시도
./gradlew flywayMigrate
```

### 2. 체크섬 불일치

**원인**: 이미 실행된 마이그레이션 파일이 수정됨

**해결방법**:
```bash
# 방법 1: Repair (권장)
./gradlew flywayRepair

# 방법 2: 새 마이그레이션 파일 생성 (운영 환경)
# V3__fix_previous_migration.sql 생성
```

### 3. Baseline 설정

기존 데이터베이스에 Flyway를 도입하는 경우:

```yaml
spring:
  flyway:
    baseline-on-migrate: true
    baseline-version: 1
```

## 개발 워크플로우

### 1. 로컬 개발

```bash
# 1. 새 마이그레이션 파일 작성
# src/main/resources/db/migration/V2__add_feature.sql

# 2. 애플리케이션 실행 (자동 마이그레이션)
./gradlew bootRun

# 3. 결과 확인
# Database Tool에서 변경사항 확인
```

### 2. PR 생성 전

```bash
# 마이그레이션 검증
./gradlew flywayValidate

# 테스트 실행
./gradlew test

# 로그 확인
# 마이그레이션 성공 메시지 확인
```

### 3. 운영 배포

```bash
# 1. 데이터베이스 백업 (필수!)

# 2. 마이그레이션 정보 확인
./gradlew flywayInfo

# 3. Dry-run (가능한 경우)
# 테스트 환경에서 먼저 실행

# 4. 운영 배포
# 애플리케이션 시작 시 자동 실행
```

## 베스트 프랙티스

### 1. 작은 단위로 분리
- ✅ 각 마이그레이션은 하나의 변경사항만 포함
- ✅ 여러 테이블을 한 번에 변경하지 않기
- ✅ 롤백 가능성 고려

### 2. 주석 작성
- ✅ 변경 이유와 목적 명시
- ✅ 영향받는 테이블과 컬럼 설명
- ✅ 관련 Jira 이슈 번호 추가

### 3. 테스트
- ✅ 로컬에서 충분히 테스트
- ✅ 개발/스테이징 환경에서 검증
- ✅ 롤백 계획 수립

### 4. 문서화
- ✅ 주요 변경사항은 README 업데이트
- ✅ API 영향도 분석
- ✅ 팀원과 공유

## 예시

### 예시 1: 컬럼 추가

```sql
-- V2__add_employee_profile_image.sql

-- ============================================
-- 설명: 근로자 프로필 이미지 URL 추가
-- 관련 이슈: JIRA-123
-- ============================================

ALTER TABLE employee
ADD COLUMN profile_image_url VARCHAR(500) NULL COMMENT '프로필 이미지 URL (S3)';

CREATE INDEX idx_profile_image ON employee(profile_image_url);
```

### 예시 2: 새 테이블 생성

```sql
-- V3__create_notification_table.sql

-- ============================================
-- 설명: 알림 기능을 위한 notification 테이블 생성
-- 관련 이슈: JIRA-456
-- ============================================

CREATE TABLE IF NOT EXISTS notification (
    notification_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '알림 ID',
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    user_type VARCHAR(20) NOT NULL COMMENT '사용자 타입 (MANAGER, EMPLOYEE)',
    title VARCHAR(255) NOT NULL COMMENT '알림 제목',
    message TEXT NOT NULL COMMENT '알림 내용',
    type VARCHAR(50) NOT NULL COMMENT '알림 타입 (INFO, WARNING, ERROR)',
    is_read BOOLEAN NOT NULL DEFAULT FALSE COMMENT '읽음 여부',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    read_at TIMESTAMP NULL COMMENT '읽은 시각',
    INDEX idx_user (user_id, user_type),
    INDEX idx_is_read (is_read),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='알림';
```

### 예시 3: 데이터 마이그레이션

```sql
-- V4__migrate_old_contract_data.sql

-- ============================================
-- 설명: 구 계약 테이블 데이터를 신규 형식으로 마이그레이션
-- 관련 이슈: JIRA-789
-- ============================================

-- 1. 임시 컬럼 추가
ALTER TABLE contract
ADD COLUMN migrated BOOLEAN DEFAULT FALSE;

-- 2. 데이터 변환 및 복사
UPDATE contract
SET
    wage_type = 'HOURLY',
    migrated = TRUE
WHERE wage_type IS NULL AND hourly_rate > 0;

-- 3. 검증
SELECT COUNT(*) as unmigrated_count
FROM contract
WHERE migrated = FALSE;

-- 4. 임시 컬럼 제거 (다음 마이그레이션에서)
-- ALTER TABLE contract DROP COLUMN migrated;
```

## 참고 자료

- [Flyway 공식 문서](https://flywaydb.org/documentation/)
- [Flyway Best Practices](https://flywaydb.org/documentation/bestpractices)
- [Spring Boot Flyway Integration](https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.data-initialization.migration-tool.flyway)
