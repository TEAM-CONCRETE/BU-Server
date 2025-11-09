# 🏗️ Build-Up Platform — Backend Developer Docs

## 📘 Overview

건설현장 노무관리를 위한 AI 기반 통합 플랫폼의 백엔드 아키텍처 및 도메인 설계 문서입니다.

주요 도메인: 인증, 근로자, 계약, 근태, 급여, 안전, 작업일보, 파일/S3 관리

⸻

## 📂 Domain Overview

| Domain |	Description |	Key APIs |
|---|---|---|
|Auth & Access|	사용자 인증 / 권한 관리|	/auth/*, /users/*|
|Employee|	근로자 정보 / 암호화 관리|	/employees, /employees_private|
|Contract|	근로계약 생성 및 서명 관리|	/contracts/*|
|Attendance|	근태 기록 / 출퇴근|	/attendances/*|
|Payroll|	급여 계산 및 명세서 관리|	/payrolls/*, /payslip_items/*|
|Safety|	안전교육 / 서명|	/safety-docs/*|
|Work Report|	작업일보 / 자재 / 인원|	/work-reports/*|
|File & S3|	문서 PDF / Presigned URL|	/uploads/*, /contracts/{id}/pdf|


---

## ⚙️ Domain Responsibility Map

### 1️⃣ Auth & Access Management
- Controllers: AuthController, UserController, RoleController 
- Functions
  - 회원가입 (근로자, 관리자, 기업)
  - 로그인 / 로그아웃 / 토큰 재발급 
  - Role 기반 접근 제어 (SuperAdmin, Corp, Manager, Worker)
  - Key Tables: users, roles

---

### 2️⃣ Employee Management
- Tables: employees, employees_private 
- Controllers: EmployeeController, PrivateEmployeeController
- Functions:
  - 근로자 등록 / 조회 / 검색 
  - 개인정보 분리 저장 (employees_private)
  - AES256 암호화 복호화 via KMS

---

### 3️⃣ Contract Management
- Tables: contracts, contract_details 
- Controllers: ContractController, SignController, PDFController 
- Endpoints:
  - POST /{siteId}/contracts 근로계약 생성 
  - POST /{siteId}/contracts/{id}/sign/employee 
  - POST /uploads/signatures → S3 Presigned URL 생성 
- Workflow:
  - DRAFT → PDF_GENERATED → CORP_SIGNED → FULLY_SIGNED 
- S3 Lifecycle: retention_until 컬럼과 Object Tag 연동

---

### 4️⃣ Attendance Management
- Tables: attendances 
- Controllers: AttendanceController, FaceRecognitionController 
- Functions:
  - 얼굴인식 기반 출퇴근 기록 
  - 근로시간/야간/연장 근로시간 계산 
  - 근태 → 급여 연동

---

### 5️⃣ Payroll Management
- Tables: payrolls, payslip_items, payroll_ledgers 
- Controllers: PayrollController, PayslipController, LedgerController 
- Functions:
  - 급여 자동 계산 (근태 기준)
  - 항목별 급여명세 (payslip_items) 관리 
  - 스냅샷(payroll_ledgers) 보존 
  - Lamda 기반 PDF 자동 생성

---

### 6️⃣ Safety Management
- Tables: safety_docs, safety_doc_attendees, safety_sign_logs 
- Controllers: SafetyDocController, SafetyAttendeeController, SafetySignController 
- Functions:
  - 안전교육 일지 생성 및 참석자 등록 
  - 전자서명 관리 (AI 서명 검증 포함)
  - 안전 일지 PDF 생성 및 보존

---

### 7️⃣ Work Report Management
- Tables: work_reports, work_report_employees, work_report_materials 
- Controllers: WorkReportController, MaterialController, EmployeeReportController 
- Endpoints:
  - POST /{siteId}/work-reports 작업일보 생성 
  - GET /{siteId}/work-reports/{id}/pdf PDF 조회 
- Features:
  - 현장별 작업일보 기록 관리 
  - 자재/인력 투입 이력 저장

---

### 8️⃣ File & S3 Service
- Controllers: FileController, S3Controller 
- Functions:
  - Presigned URL 발급 
  - S3 Object Tag(retention_until)로 자동 삭제 관리 
  - PDF, 서명 이미지 파일 업로드 및 관리

---

## 🔐 RBAC Role Matrix

|Role|	Permission Scope|
| --- | --- |
|Super Admin|	모든 도메인 접근|
|Company Admin|	Payroll, Contract, Employee, Site|
|Site Manager|	Work Report, Safety, Contract|
|Worker|	본인 계약, 근태, 급여 조회|
|AI System |	내부 데이터 처리 (안면인식, 서명 검증 등)|


---

## 🔄 Data Flow Summary

```markdown
Employee → Contract → Attendance → Payroll → Ledger  
↘ Safety Docs ↘ Work Reports  
↘ PDF & S3 ↘ Notification
```


---

## 🚀 Implementation Notes
- Framework: Spring Boot 3, JPA, QueryDSL, JWT 
- Infra: AWS S3, Lambda, EC2, CloudWatch 
- AI Service: FastAPI, YOLOv8, OpenCV 
- Database: MySQL 8.0 (InnoDB, utf8mb4)
- Security: AES256 + KMS, JWT, HTTPS, CORS 
- Versioning: RESTful + /v1 Prefix 
- S3 Lifecycle: retention_until 컬럼 기반 만료 정책 관리

---

## 📚 References
- 🗂️ ERD & Schema.md

⸻

### Developer Note:
- 각 도메인별 구현 시 Controller → Service → Repository → Entity 구조를 유지합니다. 
- DB 연관관계 및 retention_until, employees_private 암호화 구조 참고 필수.
