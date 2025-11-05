# Build-Up Platform ERD

## 엔티티 관계도 (Entity Relationship Diagram)

### 1. 사용자 및 인증 (Auth Domain)

#### User (사용자 기본 정보)
| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 사용자 ID |
| phone | VARCHAR(20) | NOT NULL, UNIQUE | 전화번호 (로그인 ID) |
| password | VARCHAR(255) | NOT NULL | 비밀번호 (암호화) |
| role | VARCHAR(20) | NOT NULL | 권한 (MANAGER/EMPLOYEE) |
| created_at | DATETIME | NOT NULL | 생성일시 |
| updated_at | DATETIME | | 수정일시 |

#### Manager (관리자 상세 정보)
| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 관리자 ID |
| user_id | BIGINT | FK, NOT NULL, UNIQUE | 사용자 ID (User) |
| manager_name | VARCHAR(50) | NOT NULL | 관리자 이름 |
| corporation_id | BIGINT | FK | 소속 업체 ID (Corporation) |
| created_at | DATETIME | NOT NULL | 생성일시 |
| updated_at | DATETIME | | 수정일시 |

#### Employee (근로자 상세 정보)
| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 근로자 ID |
| user_id | BIGINT | FK, NOT NULL, UNIQUE | 사용자 ID (User) |
| emp_name | VARCHAR(50) | NOT NULL | 근로자 이름 |
| sub_phone | VARCHAR(20) | | 비상 연락망 |
| resident_num | VARCHAR(500) | | **주민등록번호 (AES-256 암호화, API 마스킹 처리)** |
| emp_address | VARCHAR(255) | | 주소 |
| emp_type | VARCHAR(30) | | 근로자 유형 (DAILY/PERMANENT) |
| created_at | DATETIME | NOT NULL | 생성일시 |
| updated_at | DATETIME | | 수정일시 |

#### Corporation (업체 정보)
| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 업체 ID |
| corp_name | VARCHAR(100) | NOT NULL | 업체명 |
| corp_type | VARCHAR(50) | | 업체 유형 |
| corp_address | VARCHAR(255) | | 업체 주소 |
| corp_phone | VARCHAR(20) | | 업체 전화번호 |
| business_num | VARCHAR(20) | | 사업자등록번호 |
| created_at | DATETIME | NOT NULL | 생성일시 |
| updated_at | DATETIME | | 수정일시 |

---

## 보안 정책 (Security Policy)

### 1. 개인정보 암호화

#### AES-256 암호화 적용 대상
- **주민등록번호 (resident_num)**
  - 알고리즘: AES-256 (ECB 모드, PKCS5Padding)
  - 저장 형식: Base64 인코딩된 암호문
  - 컬럼 길이: VARCHAR(500) (암호화 후 데이터 길이 고려)
  - 적용 레이어: JPA AttributeConverter (자동 암호화/복호화)

#### 암호화 키 관리
- 암호화 키는 **환경변수**로 관리
- application.yml 설정:
  ```yaml
  security:
    encryption:
      key: ${ENCRYPTION_KEY:default_32_byte_key_value}
  ```
- 운영 환경에서는 반드시 `ENCRYPTION_KEY` 환경변수를 설정해야 함
- 키 길이: 32바이트 (256비트)

### 2. API 응답 데이터 마스킹

#### 주민등록번호 마스킹 규칙
- **마스킹 형식**: `901234-1******`
  - 앞 6자리: 생년월일 (공개)
  - 하이픈: 구분자
  - 뒤 첫 자리: 성별 구분 (공개)
  - 나머지 6자리: 마스킹 (`******`)
- 적용 레이어: Jackson Serializer (API 응답 시 자동 마스킹)
- 적용 대상: Employee 엔티티의 `residentNum` 필드

#### 구현 방식
```java
// Entity 필드에 적용
@Convert(converter = ResidentNumConverter.class)        // DB 암호화/복호화
@JsonSerialize(using = ResidentNumMaskingSerializer.class)  // API 마스킹
@Column(name = "resident_num", length = 500)
private String residentNum;
```

### 3. 보안 처리 흐름

```
[사용자 입력]
    ↓
[평문 주민등록번호: 901234-1234567]
    ↓
[JPA AttributeConverter] ← AES-256 암호화
    ↓
[DB 저장: Base64 암호문]
    ↓
[DB 조회: Base64 암호문]
    ↓
[JPA AttributeConverter] ← AES-256 복호화
    ↓
[평문 주민등록번호: 901234-1234567]
    ↓
[Jackson Serializer] ← 마스킹 처리
    ↓
[API 응답: 901234-1******]
```

### 4. 주의사항

1. **암호화 키 보안**
   - 암호화 키는 절대 코드에 하드코딩하지 않음
   - 환경변수 또는 보안 저장소(AWS Secrets Manager 등)에서 관리
   - 키 유출 시 즉시 교체 및 재암호화 필요

2. **데이터 마이그레이션**
   - 기존 평문 데이터가 있는 경우 암호화 마이그레이션 필요
   - 마이그레이션 전 백업 필수

3. **성능 고려사항**
   - 암호화/복호화는 CPU 연산이 필요하므로 대량 조회 시 성능 영향 가능
   - 필요 시 조회 쿼리 최적화 (인덱스, 페이징 등)

4. **규정 준수**
   - 개인정보보호법에 따른 암호화 조치
   - PIPA(개인정보보호법) 준수

---

## 관계 설정

### User ↔ Manager (1:1)
- User.id ← Manager.user_id (FK)
- OneToOne 양방향 관계

### User ↔ Employee (1:1)
- User.id ← Employee.user_id (FK)
- OneToOne 양방향 관계

### Corporation ↔ Manager (1:N)
- Corporation.id ← Manager.corporation_id (FK)
- OneToMany 양방향 관계

---

## 변경 이력

| 날짜 | 버전 | 변경 내용 |
|------|------|-----------|
| 2025-11-05 | 1.0 | 초기 ERD 작성 및 보안 정책 수립 |
| 2025-11-05 | 1.1 | Employee.resident_num 암호화/마스킹 처리 추가 |
