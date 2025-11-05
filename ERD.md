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

#### AES-256-GCM 암호화 적용 대상
- **주민등록번호 (resident_num)**
  - 알고리즘: AES-256-GCM (Galois/Counter Mode)
  - IV: 12바이트 랜덤 생성 (SecureRandom.getInstanceStrong())
  - 인증 태그: 128비트
  - 저장 형식: Base64(IV + 암호문)
  - 컬럼 길이: VARCHAR(500) (IV + 암호문 + 인증 태그 고려)
  - 적용 레이어: JPA AttributeConverter (자동 암호화/복호화)

#### 보안 강화 포인트
- **ECB 모드 제거**: 패턴 노출 취약점 해결
- **GCM 모드 적용**: 무결성 검증 + 기밀성 보장
- **랜덤 IV**: 매 암호화마다 새로운 IV 생성 (재사용 공격 방지)
- **키 길이 검증**: 애플리케이션 시작 시 32바이트 검증

#### 암호화 키 관리
- 암호화 키는 **환경변수 필수**
- application.yml 설정:
  ```yaml
  security:
    encryption:
      key: ${ENCRYPTION_KEY}  # 기본값 없음 (필수)
  ```
- **모든 환경**에서 반드시 `ENCRYPTION_KEY` 환경변수를 설정해야 함
- 키 길이: 32바이트 (256비트) - 길이 검증 로직 포함
- 설정 예시:
  ```bash
  export ENCRYPTION_KEY="your_32_byte_secure_key_here!!"
  ```

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
[JPA AttributeConverter] ← AES-256-GCM 암호화
    ├─ 랜덤 IV 생성 (12바이트)
    ├─ 평문 암호화
    └─ IV + 암호문 결합
    ↓
[DB 저장: Base64(IV + 암호문)]
    ↓
[DB 조회: Base64(IV + 암호문)]
    ↓
[JPA AttributeConverter] ← AES-256-GCM 복호화
    ├─ Base64 디코딩
    ├─ IV 추출 (12바이트)
    ├─ 암호문 추출
    └─ 복호화 + 무결성 검증
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
   - 키 길이: 반드시 32바이트 (애플리케이션 시작 시 검증)
   - 키 유출 시 즉시 교체 및 재암호화 필요

2. **데이터 마이그레이션**
   - 기존 평문 데이터가 있는 경우 암호화 마이그레이션 필요
   - **ECB → GCM 전환 시**: 기존 데이터 재암호화 필요
   - 마이그레이션 전 백업 필수
   - 다운타임 최소화 전략 수립 권장

3. **GCM 모드 특성**
   - IV는 절대 재사용하면 안 됨 (SecureRandom으로 매번 새로 생성)
   - 인증 태그를 통한 무결성 검증 자동 수행
   - 복호화 실패 시 데이터 변조 가능성 의심

4. **성능 고려사항**
   - 암호화/복호화는 CPU 연산이 필요하므로 대량 조회 시 성능 영향 가능
   - GCM 모드는 ECB보다 약간 느리지만 보안성이 월등히 높음
   - 필요 시 조회 쿼리 최적화 (인덱스, 페이징 등)

5. **규정 준수**
   - 개인정보보호법에 따른 암호화 조치
   - PIPA(개인정보보호법) 준수
   - NIST 권장 암호화 알고리즘 (AES-256-GCM) 준수

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
| 2025-11-05 | 1.2 | AES-256-GCM 보안 강화 (ECB→GCM, IV 랜덤 생성, 키 검증, 환경변수 필수화) |
