# Build-Up Platform API 테스트 예시

> **주의**: Base URL은 `http://localhost:8080/api` 입니다.
>
> Swagger UI: http://localhost:8080/api/swagger-ui/index.html

---

## 목차

1. [인증/인가 API (Auth)](#1-인증인가-api-auth)
2. [계약 관리 API (Contract)](#2-계약-관리-api-contract)
3. [파일 업로드 API (Upload)](#3-파일-업로드-api-upload)
4. [GlobalExceptionHandler 테스트](#4-globalexceptionhandler-테스트)

---

## 1. 인증/인가 API (Auth)

### 1.1 아이디 중복 확인

**요청**
```bash
curl -X GET 'http://localhost:8080/api/v1/auth/exists?userId=testuser001'
```

**성공 응답 (200)**
```json
{
  "success": true,
  "message": "아이디 중복 확인이 완료되었습니다",
  "code": null,
  "data": {
    "exists": false
  }
}
```

**실패 응답 - 파라미터 누락 (400)**
```json
{
  "success": false,
  "message": "필수 파라미터가 누락되었습니다: userId (String)",
  "code": "COMMON_403",
  "data": null
}
```

---

### 1.2 근로자 회원가입 1단계

**요청 예시**
```bash
curl -X POST http://localhost:8080/api/v1/auth/register/employee/step1 \
  -H "Content-Type: application/json" \
  -d '{
    "empName": "홍길동",
    "userId": "testuser001",
    "password": "Test123@@",
    "confirmPassword": "Test123@@",
    "secretKey": "SECRET-KEY-12345",
    "agreeTerms": true,
    "agreePrivacy": true
  }'
```

**필드 설명**
- `empName` (필수): 한글, 영문, 공백만 가능, 1~50자
- `userId` (필수): 영문 소문자, 숫자만 가능, 6~20자
- `password` (필수): 영문, 숫자, 특수문자(@$!%*#?&) 포함, 8~20자
- `confirmPassword` (필수): password와 일치해야 함
- `secretKey` (선택): 현장 연동용, 최대 100자
- `agreeTerms` (필수): 서비스 이용약관 동의, true여야 함
- `agreePrivacy` (필수): 개인정보 처리방침 동의, true여야 함

**성공 응답 (201)**
```json
{
  "success": true,
  "message": "회원가입이 완료되었습니다",
  "code": null,
  "data": {
    "userId": "testuser001",
    "empName": "홍길동",
    "registrationToken": "550e8400-e29b-41d4-a716-446655440000",
    "expiresAt": "2025-11-11T15:30:00"
  }
}
```

**실패 응답 - Validation 오류 (400)**
```json
{
  "success": false,
  "message": "필드 검증에 실패했습니다.",
  "code": "COMMON_401",
  "data": {
    "password": "비밀번호는 영문, 숫자, 특수문자(@$!%*#?&)를 포함해야 합니다",
    "userId": "로그인 ID는 영문 소문자와 숫자만 입력 가능합니다"
  }
}
```

**실패 응답 - 아이디 중복 (409)**
```json
{
  "success": false,
  "message": "이미 사용 중인 아이디입니다",
  "code": "AUTH_1006",
  "data": null
}
```

---

### 1.3 근로자 회원가입 2단계

**요청 예시**
```bash
curl -X POST http://localhost:8080/api/v1/auth/register/employee/step2 \
  -H "Content-Type: application/json" \
  -d '{
    "registrationToken": "550e8400-e29b-41d4-a716-446655440000",
    "residentNum": "990101-1234567",
    "phone": "01012345678",
    "email": "test@example.com",
    "empAddress": "서울시 강남구 테헤란로 123",
    "emergencyPhone": "01087654321"
  }'
```

**필드 설명**
- `registrationToken` (필수): 1단계 완료 시 발급받은 토큰
- `residentNum` (필수): 주민등록번호 형식 (XXXXXX-XXXXXXX), AES-256-GCM 암호화 저장
- `phone` (필수): 휴대폰 번호 (010XXXXXXXX, 숫자만)
- `email` (선택): 이메일 형식
- `empAddress` (선택): 주소, 최대 255자
- `emergencyPhone` (선택): 비상연락망 (11자리 숫자)

**성공 응답 (200)**
```json
{
  "success": true,
  "message": "근로자 정보가 업데이트되었습니다",
  "code": null,
  "data": {
    "userId": "testuser001",
    "empName": "홍길동",
    "role": "EMPLOYEE",
    "profileData": {
      "phone": "01012345678",
      "email": "test@example.com",
      "empAddress": "서울시 강남구 테헤란로 123"
    }
  }
}
```

**실패 응답 - 토큰 만료/무효 (401)**
```json
{
  "success": false,
  "message": "등록 토큰이 만료되었거나 유효하지 않습니다",
  "code": "AUTH_1012",
  "data": null
}
```

---

### 1.4 현장 관리자 회원가입

**요청 예시**
```bash
curl -X POST http://localhost:8080/api/v1/auth/register/manager \
  -H "Content-Type: application/json" \
  -d '{
    "managerName": "김철수",
    "userId": "manager001",
    "password": "Manager123!@",
    "confirmPassword": "Manager123!@",
    "secretKey": "MANAGER-SECRET-KEY-12345",
    "phone": "010-1234-5678"
  }'
```

**필드 설명**
- `managerName` (필수): 한글, 영문, 공백만 가능, 1~50자
- `userId` (필수): 영문 소문자, 숫자만 가능, 6~20자
- `password` (필수): 영문, 숫자, 특수문자(@$!%*#?&) 포함, 8~20자
- `confirmPassword` (필수): password와 일치해야 함
- `secretKey` (필수): 현장 관리자용 시크릿키, 최대 100자
- `phone` (필수): 전화번호 (010-XXXX-XXXX 또는 01XXXXXXXXX)

**성공 응답 (201)**
```json
{
  "success": true,
  "message": "현장 관리자 회원가입이 완료되었습니다",
  "code": null,
  "data": {
    "userId": "manager001",
    "role": "MANAGER",
    "profileData": {
      "managerName": "김철수",
      "phone": "010-1234-5678"
    }
  }
}
```

---

### 1.5 로그인

**요청 예시 1: 테스트 근로자 계정**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -c cookies.txt \
  -d '{
    "username": "testuser001",
    "password": "Test123@@",
    "rememberMe": false
  }'
```

**요청 예시 2: 테스트 관리자 계정**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -c cookies.txt \
  -d '{
    "username": "manager001",
    "password": "Manager123!@",
    "rememberMe": true
  }'
```

**필드 설명**
- `username` (필수): 로그인 ID
- `password` (필수): 비밀번호
- `rememberMe` (선택): true(30일), false(7일), 기본값 false

**성공 응답 (200)**
```json
{
  "success": true,
  "message": "로그인 성공",
  "code": null,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "userId": "testuser001",
    "role": "EMPLOYEE"
  }
}
```

**응답 헤더**
```
Set-Cookie: refreshToken=<JWT_TOKEN>; Path=/; Max-Age=604800; HttpOnly; Secure; SameSite=Strict
```

**실패 응답 - 인증 실패 (401)**
```json
{
  "success": false,
  "message": "아이디 또는 비밀번호가 올바르지 않습니다",
  "code": "AUTH_1005",
  "data": null
}
```

**실패 응답 - Role 없음 (403)**
```json
{
  "success": false,
  "message": "사용자의 역할이 설정되지 않았습니다. 관리자에게 문의하세요.",
  "code": "AUTH_1013",
  "data": null
}
```

---

### 1.6 내 정보 조회 (인증 필요)

**요청 예시**
```bash
curl -X GET http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

**성공 응답 - 근로자 (200)**
```json
{
  "success": true,
  "message": "사용자 정보 조회 성공",
  "code": null,
  "data": {
    "userId": "testuser001",
    "role": "EMPLOYEE",
    "profileData": {
      "empName": "홍길동",
      "phone": "01012345678",
      "email": "test@example.com",
      "empAddress": "서울시 강남구 테헤란로 123"
    }
  }
}
```

**성공 응답 - 관리자 (200)**
```json
{
  "success": true,
  "message": "사용자 정보 조회 성공",
  "code": null,
  "data": {
    "userId": "manager001",
    "role": "MANAGER",
    "profileData": {
      "managerName": "김철수",
      "phone": "010-1234-5678",
      "siteId": 1
    }
  }
}
```

**실패 응답 - 토큰 없음/만료 (401)**
```json
{
  "success": false,
  "message": "인증 토큰이 유효하지 않습니다",
  "code": "AUTH_1003",
  "data": null
}
```

---

### 1.7 토큰 재발급

**요청 예시**
```bash
curl -X POST http://localhost:8080/api/v1/auth/token/refresh \
  -H "Content-Type: application/json" \
  -b cookies.txt \
  -c cookies.txt
```

**성공 응답 (200)**
```json
{
  "success": true,
  "message": "토큰 재발급 성공",
  "code": null,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "userId": "testuser001",
    "role": "EMPLOYEE"
  }
}
```

**실패 응답 - Refresh Token 없음 (401)**
```json
{
  "success": false,
  "message": "Refresh Token을 찾을 수 없습니다",
  "code": "AUTH_1008",
  "data": null
}
```

---

## 2. 계약 관리 API (Contract)

> **주의**: 모든 계약 API는 인증이 필요하며, 역할별 접근 제어가 적용됩니다.

### 2.1 계약 목록 조회 (MANAGER, CORPORATION, ADMIN만)

**요청 예시 1: 기본 조회**
```bash
curl -X GET 'http://localhost:8080/api/v1/1/contracts' \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

**요청 예시 2: 필터링 + 페이징**
```bash
curl -X GET 'http://localhost:8080/api/v1/1/contracts?employeeId=1&status=FULLY_SIGNED&page=1&size=20' \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

**쿼리 파라미터**
- `siteId` (Path 필수): 현장 ID
- `employeeId` (선택): 근로자 ID 필터
- `empType` (선택): DAILY(일용직) 또는 PERMANENT(상용직)
- `status` (선택): DRAFT(초안), PENDING(대기중), FULLY_SIGNED(완료)
- `from` (선택): 계약 시작일 범위 시작 (YYYY-MM-DD)
- `to` (선택): 계약 시작일 범위 종료 (YYYY-MM-DD)
- `page` (선택): 페이지 번호, 기본값 1
- `size` (선택): 페이지 크기, 기본값 20, 최대 100

**성공 응답 (200)**
```json
{
  "success": true,
  "message": "계약 목록 조회가 완료되었습니다",
  "code": null,
  "data": {
    "contracts": [
      {
        "contractId": 1,
        "empType": "PERMANENT",
        "status": "FULLY_SIGNED",
        "employeeStartDate": "2024-01-01",
        "employeeEndDate": null,
        "employeeName": "홍길동",
        "residentNum": "990101-1******",
        "corporationName": "주식회사 빌드업",
        "createdAt": "2024-01-01T09:00:00"
      }
    ],
    "pageInfo": {
      "currentPage": 1,
      "pageSize": 20,
      "totalElements": 1,
      "totalPages": 1
    }
  }
}
```

**실패 응답 - 권한 없음 (403)**
```json
{
  "success": false,
  "message": "접근 권한이 없습니다",
  "code": "AUTH_1011",
  "data": null
}
```

---

### 2.2 상용직 계약 생성 (MANAGER, ADMIN만)

**요청 예시**
```bash
curl -X POST http://localhost:8080/api/v1/1/contracts/regular \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "employeeId": 1,
    "corporationId": 1,
    "managerId": 1,
    "role": "현장 관리자",
    "empType": "PERMANENT",
    "employeeStartDate": "2024-01-01",
    "employeeEndDate": null,
    "details": {
      "workPlace": "서울시 강남구 테헤란로 123",
      "workType": "일반건설현장근로자",
      "workStartTime": "09:00:00",
      "workEndTime": "18:00:00",
      "breakStartTime": "12:00:00",
      "breakEndTime": "13:00:00",
      "workOnDays": "월~금",
      "workOffDays": "토, 일",
      "workPay": 3000000.00,
      "additionalHourPay": 150000.00,
      "additionalNightPay": 100000.00,
      "additionalHolidayPay": 200000.00,
      "payDay": 25,
      "payPeriod": "MONTHLY",
      "payType": "TRANSFER",
      "isEoiApplicable": true,
      "isWciApplicable": true,
      "isNpsApplicable": true,
      "isNhiApplicable": true
    }
  }'
```

**필드 설명 (기본 정보)**
- `employeeId` (필수): 근로자 ID
- `corporationId` (필수): 기업 ID
- `managerId` (선택): 관리자 ID
- `role` (선택): 계약 시 역할
- `empType` (필수): **PERMANENT만 허용** (상용직)
- `employeeStartDate` (필수): 근로 시작일 (YYYY-MM-DD)
- `employeeEndDate` (선택): 근로 종료일, null이면 계속 근로
- `details` (필수): 계약 상세 정보 (아래 참조)

**필드 설명 (details - 근무 정보)**
- `workPlace` (선택): 근무 장소
- `workType` (선택): 직종
- `workStartTime` (선택): 근무 시작 시간 (HH:MM:SS)
- `workEndTime` (선택): 근무 종료 시간
- `breakStartTime` (선택): 휴게 시작 시간
- `breakEndTime` (선택): 휴게 종료 시간
- `workOnDays` (선택): 근무일
- `workOffDays` (선택): 휴일

**필드 설명 (details - 급여 정보)**
- `workPay` (필수): 기본 임금
- `additionalHourPay` (선택): 시간외 근로 수당
- `additionalNightPay` (선택): 야간 근로 수당
- `additionalHolidayPay` (선택): 휴일 근로 수당

**필드 설명 (details - 지급 정보)**
- `payDay` (필수): 임금 지급일 (1~31)
- `payPeriod` (필수): DAILY(일급), WEEKLY(주급), MONTHLY(월급)
- `payType` (필수): CASH(현금), TRANSFER(계좌이체)

**필드 설명 (details - 4대보험)**
- `isEoiApplicable` (선택): 고용보험 적용 여부
- `isWciApplicable` (선택): 산재보험 적용 여부
- `isNpsApplicable` (선택): 국민연금 적용 여부
- `isNhiApplicable` (선택): 건강보험 적용 여부

**성공 응답 (201)**
```json
{
  "success": true,
  "message": "상용직 계약이 생성되었습니다",
  "code": null,
  "data": {
    "contractId": 1,
    "status": "DRAFT",
    "empType": "PERMANENT",
    "createdAt": "2024-01-01T09:00:00"
  }
}
```

**실패 응답 - 근로자 타입 불일치 (422)**
```json
{
  "success": false,
  "message": "근로자에게 이미 다른 타입의 계약이 존재합니다",
  "code": "CONTRACT_2003",
  "data": null
}
```

---

### 2.3 일용직 계약 생성 (MANAGER, ADMIN만)

**요청 예시**
```bash
curl -X POST http://localhost:8080/api/v1/1/contracts/daily \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "employeeId": 2,
    "corporationId": 1,
    "managerId": 1,
    "role": "현장 관리자",
    "empType": "DAILY",
    "employeeStartDate": "2024-01-01",
    "employeeEndDate": "2024-01-31",
    "details": {
      "workPlace": "서울시 강남구 테헤란로 123",
      "workType": "일반건설현장근로자",
      "workStartTime": "09:00:00",
      "workEndTime": "18:00:00",
      "breakStartTime": "12:00:00",
      "breakEndTime": "13:00:00",
      "workOnDays": "월~토",
      "workOffDays": "일",
      "workPay": 150000.00,
      "additionalHourPay": 0.00,
      "additionalNightPay": 0.00,
      "additionalHolidayPay": 0.00,
      "payDay": 1,
      "payPeriod": "DAILY",
      "payType": "CASH",
      "isEoiApplicable": false,
      "isWciApplicable": true,
      "isNpsApplicable": false,
      "isNhiApplicable": false
    }
  }'
```

**필드 설명**
- 기본적으로 상용직과 동일
- `empType`은 **DAILY만 허용** (일용직)
- 일용직은 일반적으로:
  - `employeeEndDate` 지정 (계약 종료일 명시)
  - `payPeriod`: DAILY (일급)
  - `payType`: CASH 또는 TRANSFER
  - 4대보험: 산재보험만 적용하는 경우가 많음

**성공 응답 (201)**
```json
{
  "success": true,
  "message": "일용직 계약이 생성되었습니다",
  "code": null,
  "data": {
    "contractId": 2,
    "status": "DRAFT",
    "empType": "DAILY",
    "createdAt": "2024-01-01T09:00:00"
  }
}
```

---

## 3. 파일 업로드 API (Upload)

### 3.1 서명 이미지 Presigned URL 발급 (인증 필요)

**요청 예시**
```bash
curl -X POST http://localhost:8080/api/v1/uploads/signatures \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "resourceType": "CONTRACT",
    "resourceId": "123",
    "signerRole": "EMPLOYEE",
    "fileExtension": "png"
  }'
```

**필드 설명**
- `resourceType` (필수): 리소스 타입 (예: CONTRACT)
- `resourceId` (필수): 리소스 ID (계약 ID 등)
- `signerRole` (필수): 서명자 역할 (EMPLOYEE, MANAGER, CORPORATION)
- `fileExtension` (필수): 파일 확장자 (png, jpg, jpeg, pdf)

**성공 응답 (200)**
```json
{
  "success": true,
  "message": "Presigned URL이 발급되었습니다.",
  "code": null,
  "data": {
    "uploadUrl": "https://build-up-contracts.s3.ap-northeast-2.amazonaws.com/uploads/contracts/123/EMPLOYEE.png?signature=xxx",
    "expiresAt": "2025-11-11T15:15:00",
    "s3Key": "uploads/contracts/123/EMPLOYEE.png",
    "bucket": "build-up-contracts"
  }
}
```

**사용 방법**
1. 위 API로 Presigned URL 발급
2. 발급받은 `uploadUrl`로 PUT 요청하여 파일 직접 업로드:
   ```bash
   curl -X PUT "<uploadUrl>" \
     -H "Content-Type: image/png" \
     --data-binary @signature.png
   ```
3. 업로드 완료 후 별도의 complete API 호출 (검증용)

**실패 응답 - Validation 실패 (400)**
```json
{
  "success": false,
  "message": "필드 검증에 실패했습니다.",
  "code": "COMMON_401",
  "data": {
    "fileExtension": "지원하지 않는 파일 형식입니다. (png, jpg, jpeg, pdf만 가능)"
  }
}
```

---

## 4. GlobalExceptionHandler 테스트

### 4.1 필수 파라미터 누락 (400 + COMMON_403)

**요청**
```bash
curl -X GET http://localhost:8080/api/v1/auth/exists
```

**응답 (400)**
```json
{
  "success": false,
  "message": "필수 파라미터가 누락되었습니다: userId (String)",
  "code": "COMMON_403",
  "data": null
}
```

---

### 4.2 JSON 파싱 오류 (400 + COMMON_400)

**요청**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "test", invalid json'
```

**응답 (400)**
```json
{
  "success": false,
  "message": "요청 본문을 읽을 수 없습니다. JSON 형식을 확인해주세요.",
  "code": "COMMON_400",
  "data": null
}
```

---

### 4.3 HTTP 메서드 오류 (405 + COMMON_405)

**요청**
```bash
curl -X DELETE http://localhost:8080/api/v1/auth/login
```

**응답 (405)**
```json
{
  "success": false,
  "message": "지원하지 않는 HTTP 메서드입니다: DELETE (지원하는 메서드: [POST])",
  "code": "COMMON_405",
  "data": null
}
```

---

### 4.4 존재하지 않는 URL (404 + COMMON_404)

**요청**
```bash
curl -X GET http://localhost:8080/api/v1/nonexistent
```

**응답 (404)**
```json
{
  "success": false,
  "message": "요청한 리소스를 찾을 수 없습니다",
  "code": "COMMON_404",
  "data": null
}
```

---

### 4.5 Validation 오류 (400 + COMMON_401)

**요청**
```bash
curl -X POST http://localhost:8080/api/v1/auth/register/employee/step1 \
  -H "Content-Type: application/json" \
  -d '{
    "empName": "홍길동123",
    "userId": "test",
    "password": "weak",
    "confirmPassword": "weak",
    "agreeTerms": false,
    "agreePrivacy": true
  }'
```

**응답 (400)**
```json
{
  "success": false,
  "message": "필드 검증에 실패했습니다.",
  "code": "COMMON_401",
  "data": {
    "empName": "이름은 한글, 영문, 공백만 입력 가능합니다",
    "userId": "로그인 ID는 6자 이상 20자 이하여야 합니다",
    "password": "비밀번호는 8자 이상 20자 이하여야 합니다",
    "agreeTerms": "서비스 이용약관에 동의해야 합니다"
  }
}
```

---

### 4.6 인증 오류 (401)

**요청 - 토큰 없이 인증 필요 엔드포인트 호출**
```bash
curl -X GET http://localhost:8080/api/v1/auth/me
```

**응답 (401)**
```json
{
  "success": false,
  "message": "인증이 필요합니다",
  "code": "AUTH_1001",
  "data": null
}
```

---

### 4.7 권한 오류 (403)

**요청 - EMPLOYEE 권한으로 MANAGER 전용 엔드포인트 호출**
```bash
curl -X POST http://localhost:8080/api/v1/1/contracts/regular \
  -H "Authorization: Bearer <EMPLOYEE_ACCESS_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{...}'
```

**응답 (403)**
```json
{
  "success": false,
  "message": "접근 권한이 없습니다",
  "code": "AUTH_1011",
  "data": null
}
```

---

## 5. 통합 테스트 시나리오

### 시나리오 1: 근로자 회원가입 → 로그인 → 내 정보 조회

```bash
# 1. 아이디 중복 확인
curl -X GET 'http://localhost:8080/api/v1/auth/exists?userId=testuser002'

# 2. 회원가입 1단계
curl -X POST http://localhost:8080/api/v1/auth/register/employee/step1 \
  -H "Content-Type: application/json" \
  -d '{
    "empName": "김테스트",
    "userId": "testuser002",
    "password": "Test123@@",
    "confirmPassword": "Test123@@",
    "secretKey": "SECRET-KEY-67890",
    "agreeTerms": true,
    "agreePrivacy": true
  }'

# 3. 회원가입 2단계 (registrationToken을 2단계 응답에서 복사)
curl -X POST http://localhost:8080/api/v1/auth/register/employee/step2 \
  -H "Content-Type: application/json" \
  -d '{
    "registrationToken": "<복사한 토큰>",
    "residentNum": "950505-2123456",
    "phone": "01098765432",
    "email": "test2@example.com",
    "empAddress": "경기도 성남시 분당구",
    "emergencyPhone": "01011112222"
  }'

# 4. 로그인
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -c cookies.txt \
  -d '{
    "username": "testuser002",
    "password": "Test123@@",
    "rememberMe": false
  }'

# 5. 내 정보 조회 (accessToken을 로그인 응답에서 복사)
curl -X GET http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer <복사한 accessToken>"

# 6. 토큰 재발급
curl -X POST http://localhost:8080/api/v1/auth/token/refresh \
  -b cookies.txt \
  -c cookies.txt
```

---

### 시나리오 2: 현장 관리자 회원가입 → 계약 생성

```bash
# 1. 현장 관리자 회원가입
curl -X POST http://localhost:8080/api/v1/auth/register/manager \
  -H "Content-Type: application/json" \
  -d '{
    "managerName": "이관리",
    "userId": "manager002",
    "password": "Manager123!@",
    "confirmPassword": "Manager123!@",
    "secretKey": "MANAGER-SECRET-999",
    "phone": "010-9999-8888"
  }'

# 2. 로그인
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -c cookies.txt \
  -d '{
    "username": "manager002",
    "password": "Manager123!@",
    "rememberMe": false
  }'

# 3. 상용직 계약 생성 (accessToken 복사, employeeId는 기존 근로자 ID 사용)
curl -X POST http://localhost:8080/api/v1/1/contracts/regular \
  -H "Authorization: Bearer <복사한 accessToken>" \
  -H "Content-Type: application/json" \
  -d '{
    "employeeId": 1,
    "corporationId": 1,
    "managerId": 1,
    "role": "현장 관리자",
    "empType": "PERMANENT",
    "employeeStartDate": "2024-02-01",
    "employeeEndDate": null,
    "details": {
      "workPlace": "인천시 연수구 송도국제도시",
      "workType": "건축공사 현장근로자",
      "workStartTime": "08:00:00",
      "workEndTime": "17:00:00",
      "breakStartTime": "12:00:00",
      "breakEndTime": "13:00:00",
      "workOnDays": "월~금",
      "workOffDays": "토, 일, 공휴일",
      "workPay": 3500000.00,
      "additionalHourPay": 200000.00,
      "additionalNightPay": 150000.00,
      "additionalHolidayPay": 250000.00,
      "payDay": 25,
      "payPeriod": "MONTHLY",
      "payType": "TRANSFER",
      "isEoiApplicable": true,
      "isWciApplicable": true,
      "isNpsApplicable": true,
      "isNhiApplicable": true
    }
  }'

# 4. 계약 목록 조회
curl -X GET 'http://localhost:8080/api/v1/1/contracts?page=1&size=20' \
  -H "Authorization: Bearer <복사한 accessToken>"
```

---

## 6. Postman Collection 임포트 형식

아래 JSON을 복사하여 Postman에서 `Import` → `Raw text`로 임포트할 수 있습니다.

```json
{
  "info": {
    "name": "Build-Up Platform API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "variable": [
    {
      "key": "base_url",
      "value": "http://localhost:8080/api",
      "type": "string"
    },
    {
      "key": "access_token",
      "value": "",
      "type": "string"
    }
  ],
  "item": [
    {
      "name": "Auth",
      "item": [
        {
          "name": "아이디 중복 확인",
          "request": {
            "method": "GET",
            "header": [],
            "url": {
              "raw": "{{base_url}}/v1/auth/exists?userId=testuser001",
              "host": ["{{base_url}}"],
              "path": ["v1", "auth", "exists"],
              "query": [
                {
                  "key": "userId",
                  "value": "testuser001"
                }
              ]
            }
          }
        },
        {
          "name": "근로자 회원가입 1단계",
          "request": {
            "method": "POST",
            "header": [
              {
                "key": "Content-Type",
                "value": "application/json"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"empName\": \"홍길동\",\n  \"userId\": \"testuser001\",\n  \"password\": \"Test123@@\",\n  \"confirmPassword\": \"Test123@@\",\n  \"secretKey\": \"SECRET-KEY-12345\",\n  \"agreeTerms\": true,\n  \"agreePrivacy\": true\n}"
            },
            "url": {
              "raw": "{{base_url}}/v1/auth/register/employee/step1",
              "host": ["{{base_url}}"],
              "path": ["v1", "auth", "register", "employee", "step1"]
            }
          }
        },
        {
          "name": "로그인",
          "event": [
            {
              "listen": "test",
              "script": {
                "exec": [
                  "const response = pm.response.json();",
                  "if (response.success && response.data.accessToken) {",
                  "  pm.collectionVariables.set('access_token', response.data.accessToken);",
                  "}"
                ],
                "type": "text/javascript"
              }
            }
          ],
          "request": {
            "method": "POST",
            "header": [
              {
                "key": "Content-Type",
                "value": "application/json"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"username\": \"testuser001\",\n  \"password\": \"Test123@@\",\n  \"rememberMe\": false\n}"
            },
            "url": {
              "raw": "{{base_url}}/v1/auth/login",
              "host": ["{{base_url}}"],
              "path": ["v1", "auth", "login"]
            }
          }
        },
        {
          "name": "내 정보 조회",
          "request": {
            "method": "GET",
            "header": [
              {
                "key": "Authorization",
                "value": "Bearer {{access_token}}"
              }
            ],
            "url": {
              "raw": "{{base_url}}/v1/auth/me",
              "host": ["{{base_url}}"],
              "path": ["v1", "auth", "me"]
            }
          }
        }
      ]
    },
    {
      "name": "Contract",
      "item": [
        {
          "name": "계약 목록 조회",
          "request": {
            "method": "GET",
            "header": [
              {
                "key": "Authorization",
                "value": "Bearer {{access_token}}"
              }
            ],
            "url": {
              "raw": "{{base_url}}/v1/1/contracts?page=1&size=20",
              "host": ["{{base_url}}"],
              "path": ["v1", "1", "contracts"],
              "query": [
                {
                  "key": "page",
                  "value": "1"
                },
                {
                  "key": "size",
                  "value": "20"
                }
              ]
            }
          }
        },
        {
          "name": "상용직 계약 생성",
          "request": {
            "method": "POST",
            "header": [
              {
                "key": "Authorization",
                "value": "Bearer {{access_token}}"
              },
              {
                "key": "Content-Type",
                "value": "application/json"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"employeeId\": 1,\n  \"corporationId\": 1,\n  \"managerId\": 1,\n  \"role\": \"현장 관리자\",\n  \"empType\": \"PERMANENT\",\n  \"employeeStartDate\": \"2024-01-01\",\n  \"employeeEndDate\": null,\n  \"details\": {\n    \"workPlace\": \"서울시 강남구 테헤란로 123\",\n    \"workType\": \"일반건설현장근로자\",\n    \"workStartTime\": \"09:00:00\",\n    \"workEndTime\": \"18:00:00\",\n    \"breakStartTime\": \"12:00:00\",\n    \"breakEndTime\": \"13:00:00\",\n    \"workOnDays\": \"월~금\",\n    \"workOffDays\": \"토, 일\",\n    \"workPay\": 3000000.00,\n    \"additionalHourPay\": 150000.00,\n    \"additionalNightPay\": 100000.00,\n    \"additionalHolidayPay\": 200000.00,\n    \"payDay\": 25,\n    \"payPeriod\": \"MONTHLY\",\n    \"payType\": \"TRANSFER\",\n    \"isEoiApplicable\": true,\n    \"isWciApplicable\": true,\n    \"isNpsApplicable\": true,\n    \"isNhiApplicable\": true\n  }\n}"
            },
            "url": {
              "raw": "{{base_url}}/v1/1/contracts/regular",
              "host": ["{{base_url}}"],
              "path": ["v1", "1", "contracts", "regular"]
            }
          }
        }
      ]
    }
  ]
}
```

---

## 7. 주요 에러 코드 정리

### 공통 에러 (COMMON_XXX)

| 에러 코드 | HTTP | 설명 |
|----------|------|------|
| COMMON_400 | 400 | 잘못된 요청 (JSON 파싱 오류) |
| COMMON_401 | 400 | Validation 오류 |
| COMMON_402 | 400 | 타입 변환 오류 |
| COMMON_403 | 400 | 필수 파라미터 누락 |
| COMMON_404 | 404 | 리소스를 찾을 수 없음 |
| COMMON_405 | 405 | 지원하지 않는 HTTP 메서드 |
| COMMON_500 | 500 | 서버 내부 오류 |

### 인증 에러 (AUTH_1XXX)

| 에러 코드 | HTTP | 설명 |
|----------|------|------|
| AUTH_1001 | 401 | 인증 실패 |
| AUTH_1003 | 401 | 유효하지 않은 토큰 |
| AUTH_1005 | 401 | 잘못된 아이디/비밀번호 |
| AUTH_1006 | 409 | 아이디 중복 |
| AUTH_1008 | 401 | Refresh Token 없음 |
| AUTH_1011 | 403 | 권한 없음 |
| AUTH_1012 | 401 | 등록 토큰 만료 |
| AUTH_1013 | 403 | Role 없음 |

### 계약 에러 (CONTRACT_2XXX)

| 에러 코드 | HTTP | 설명 |
|----------|------|------|
| CONTRACT_2001 | 404 | 계약을 찾을 수 없음 |
| CONTRACT_2003 | 422 | 근로자 타입 불일치 |

---

## 8. 참고 사항

### 비밀번호 규칙
- 길이: 8~20자
- 포함: 영문, 숫자, 특수문자(@$!%*#?&)
- 예시: `Test123@@`, `Manager123!@`, `Admin@2024`

### 주민등록번호 규칙
- 형식: `XXXXXX-XXXXXXX` (13자리, 하이픈 포함)
- 예시: `990101-1234567`
- **암호화**: AES-256-GCM으로 암호화되어 DB 저장
- **마스킹**: API 응답 시 뒷자리 마스킹 (예: `990101-1******`)

### 휴대폰 번호 규칙
- 근로자: `010XXXXXXXX` (하이픈 없이 11자리 숫자)
- 관리자: `010-XXXX-XXXX` 또는 `010XXXXXXXX` (둘 다 가능)

### JWT 토큰
- **Access Token**: 응답 Body에 포함, 만료 시간 3600초 (1시간)
- **Refresh Token**: HttpOnly 쿠키로 전달, 만료 시간:
  - rememberMe=false: 604800초 (7일)
  - rememberMe=true: 2592000초 (30일)
- **토큰 사용**: `Authorization: Bearer <ACCESS_TOKEN>` 헤더로 전달

### 역할별 접근 권한
- **EMPLOYEE**: 내 정보 조회만 가능
- **MANAGER**: 계약 생성/조회 가능
- **CORPORATION**: 계약 조회 가능
- **ADMIN**: 모든 권한

---

**문서 버전**: 1.0
**최종 수정일**: 2025-11-11
**작성자**: Build-Up Team
