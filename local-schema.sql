
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `attendance_records` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `is_deleted` bit(1) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `attendance_type` enum('CHECK_IN','CHECK_OUT') COLLATE utf8mb4_unicode_ci NOT NULL,
  `captured_face_image_url` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `employee_id` bigint NOT NULL,
  `failure_reason` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `similarity_score` double DEFAULT NULL,
  `site_id` bigint NOT NULL,
  `state` enum('CONFIRMED','PENDING_REVIEW','REJECTED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `timestamp` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_employee_timestamp` (`employee_id`,`timestamp`),
  KEY `idx_site_timestamp` (`site_id`,`timestamp`),
  KEY `idx_state` (`state`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `attendances` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '근태 ID',
  `contract_id` bigint NOT NULL COMMENT '계약 ID',
  `employee_id` bigint NOT NULL COMMENT '근로자 ID',
  `site_id` bigint NOT NULL COMMENT '현장 ID',
  `search_date` date NOT NULL COMMENT '근무일',
  `emp_type` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '근로자 유형',
  `emp_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '근로자명 (스냅샷)',
  `resident_num` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `attendance_status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PRESENT' COMMENT '근태 상태 (PRESENT, ABSENT, LATE, EARLY_LEAVE)',
  `check_in_time` datetime DEFAULT NULL COMMENT '출근 시각',
  `check_out_time` datetime DEFAULT NULL COMMENT '퇴근 시각',
  `total_work_hour` decimal(6,2) DEFAULT NULL COMMENT '총 근무 시간',
  `night_work_hour` decimal(6,2) DEFAULT '0.00' COMMENT '야간 근무 시간',
  `additional_work_hour` decimal(6,2) DEFAULT '0.00' COMMENT '연장 근무 시간',
  `holiday_work_hour` decimal(6,2) DEFAULT '0.00' COMMENT '휴일 근무 시간',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
  `is_deleted` bit(1) NOT NULL,
  `is_late` bit(1) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_attendance` (`site_id`,`employee_id`,`search_date`),
  KEY `idx_contract_id` (`contract_id`),
  KEY `idx_employee_id` (`employee_id`),
  KEY `idx_site_id` (`site_id`),
  KEY `idx_search_date` (`search_date`),
  KEY `idx_attendance_status` (`attendance_status`),
  KEY `idx_employee_search_date` (`employee_id`,`search_date`),
  CONSTRAINT `attendances_ibfk_1` FOREIGN KEY (`contract_id`) REFERENCES `contracts` (`id`) ON DELETE CASCADE,
  CONSTRAINT `attendances_ibfk_2` FOREIGN KEY (`employee_id`) REFERENCES `employees` (`id`) ON DELETE CASCADE,
  CONSTRAINT `attendances_ibfk_3` FOREIGN KEY (`site_id`) REFERENCES `sites` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=26 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='근태';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `contract_details` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `is_deleted` bit(1) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `additional_holiday_pay` decimal(15,2) DEFAULT NULL,
  `additional_hour_pay` decimal(15,2) DEFAULT NULL,
  `additional_night_pay` decimal(15,2) DEFAULT NULL,
  `break_end_time` time(6) DEFAULT NULL,
  `break_start_time` time(6) DEFAULT NULL,
  `corp_address` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `corp_ceo_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `corp_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `emp_address` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `emp_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `is_eoi_applicable` bit(1) DEFAULT NULL,
  `is_nhi_applicable` bit(1) DEFAULT NULL,
  `is_nps_applicable` bit(1) DEFAULT NULL,
  `is_wci_applicable` bit(1) DEFAULT NULL,
  `pay_period` enum('DAILY','MONTHLY','WEEKLY') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `pay_type` enum('CASH','TRANSFER') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `payday` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `work_bonus` decimal(15,2) DEFAULT NULL,
  `work_end_time` time(6) DEFAULT NULL,
  `work_off_day` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `work_on_day` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `work_pay` decimal(15,2) DEFAULT NULL,
  `work_place` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `work_start_time` time(6) DEFAULT NULL,
  `work_type` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `contract_id` bigint NOT NULL,
  `pay_day` int DEFAULT NULL,
  `work_off_days` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `work_on_days` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKaijajpntnrwg71jl6fipbw7g9` (`contract_id`),
  KEY `idx_contract_id` (`contract_id`),
  CONSTRAINT `FK14k2jxoi7cb9xo6dyd3v9cq6` FOREIGN KEY (`contract_id`) REFERENCES `contracts` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `contract_sign_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `is_deleted` bit(1) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `signature_hash` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `signature_height` decimal(10,2) DEFAULT NULL,
  `signature_image_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `signature_width` decimal(10,2) DEFAULT NULL,
  `signature_x` decimal(10,2) DEFAULT NULL,
  `signature_y` decimal(10,2) DEFAULT NULL,
  `signed_at` datetime(6) DEFAULT NULL,
  `signed_device` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `signed_ip` varchar(45) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `signer_id` bigint DEFAULT NULL,
  `signer_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `signer_role` enum('CORPORATION','EMPLOYEE','MANAGER') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `verification_status` enum('FAILED','PENDING','VERIFIED') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `verified_at` datetime(6) DEFAULT NULL,
  `contract_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_contract_id` (`contract_id`),
  KEY `idx_signer_id` (`signer_id`),
  KEY `idx_contract_signer_role` (`contract_id`,`signer_role`),
  KEY `idx_verification_status` (`verification_status`),
  CONSTRAINT `FKmpup8fp1qd1hb9yx2uxx8cu3y` FOREIGN KEY (`contract_id`) REFERENCES `contracts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `contracts` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '계약 ID',
  `employee_id` bigint NOT NULL COMMENT '근로자 ID',
  `corporation_id` bigint NOT NULL COMMENT '법인 ID',
  `manager_id` bigint NOT NULL COMMENT '관리자 ID',
  `role` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '역할/직책',
  `contract_state` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT' COMMENT '계약 상태 (DRAFT, SIGNED, ACTIVE, TERMINATED)',
  `employee_start_date` date DEFAULT NULL COMMENT '근무 시작일',
  `employee_end_date` date DEFAULT NULL COMMENT '근무 종료일',
  `written_at` datetime DEFAULT NULL COMMENT '작성 시각',
  `corp_signed_at` datetime DEFAULT NULL COMMENT '법인 서명 시각',
  `emp_signed_at` datetime DEFAULT NULL COMMENT '근로자 서명 시각',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
  `is_deleted` bit(1) NOT NULL,
  `final_pdf_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '최종 PDF S3 URL',
  `final_pdf_hash` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '최종 PDF SHA-256 해시값',
  `pdf_generated_at` datetime DEFAULT NULL COMMENT 'PDF 생성 시각',
  `emp_type` enum('DAILY','PERMANENT') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_employee_id` (`employee_id`),
  KEY `idx_corporation_id` (`corporation_id`),
  KEY `idx_manager_id` (`manager_id`),
  KEY `idx_contract_state` (`contract_state`),
  KEY `idx_dates` (`employee_start_date`,`employee_end_date`),
  KEY `idx_employee_start_date` (`employee_start_date`),
  CONSTRAINT `contracts_ibfk_1` FOREIGN KEY (`employee_id`) REFERENCES `employees` (`id`) ON DELETE CASCADE,
  CONSTRAINT `contracts_ibfk_2` FOREIGN KEY (`corporation_id`) REFERENCES `corporations` (`id`) ON DELETE CASCADE,
  CONSTRAINT `contracts_ibfk_3` FOREIGN KEY (`manager_id`) REFERENCES `managers` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=31 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='계약서';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `corporations` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '법인 ID',
  `user_id` bigint NOT NULL COMMENT '사용자 ID',
  `corp_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '법인명',
  `corp_address` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '법인 주소',
  `corp_ceo_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '대표자명',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
  `is_deleted` bit(1) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK4u83xnbrftrwppxw630aemdlu` (`user_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_corp_name` (`corp_name`),
  CONSTRAINT `corporations_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=198 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='법인';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `employees` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '근로자 ID',
  `user_id` bigint NOT NULL COMMENT '사용자 ID',
  `emp_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '근로자 이름',
  `sub_phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '보조 전화번호',
  `resident_num` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `emp_address` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '주소',
  `emp_type` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '근로자 유형 (정규직, 계약직 등)',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
  `is_deleted` bit(1) NOT NULL,
  `profile_image_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKj2dmgsma6pont6kf7nic9elpd` (`user_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_emp_name` (`emp_name`),
  KEY `idx_emp_type` (`emp_type`),
  CONSTRAINT `employees_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=83 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='근로자';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flyway_schema_history` (
  `installed_rank` int NOT NULL,
  `version` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `script` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `checksum` int DEFAULT NULL,
  `installed_by` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`),
  KEY `flyway_schema_history_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `managers` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '관리자 ID',
  `user_id` bigint NOT NULL COMMENT '사용자 ID',
  `manager_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '관리자 이름',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
  `is_deleted` bit(1) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6sl3ig444d4qy4c2kq6ju96pf` (`user_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_manager_name` (`manager_name`),
  CONSTRAINT `managers_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='관리자';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payrolls` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '급여 ID',
  `employee_id` bigint NOT NULL COMMENT '근로자 ID',
  `contract_id` bigint NOT NULL COMMENT '계약 ID',
  `corporation_id` bigint NOT NULL COMMENT '법인 ID',
  `search_date` date NOT NULL COMMENT '급여 기준일',
  `pay_due_date` date DEFAULT NULL COMMENT '급여 지급 예정일',
  `emp_type` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '근로자 유형',
  `emp_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '근로자명 (스냅샷)',
  `resident_num` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `total_work_hour` decimal(8,2) DEFAULT NULL COMMENT '총 근무 시간',
  `total_pay` decimal(15,2) NOT NULL COMMENT '총 급여',
  `total_pay_by_day` decimal(15,2) DEFAULT NULL COMMENT '일일 급여',
  `none_tax_income` decimal(15,2) DEFAULT '0.00' COMMENT '비과세 소득',
  `income_tax` decimal(15,2) DEFAULT '0.00' COMMENT '소득세',
  `resident_tax` decimal(15,2) DEFAULT '0.00' COMMENT '주민세',
  `pay_cycle` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '급여 주기',
  `pay_status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT '지급 상태 (PENDING, PAID)',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
  `is_deleted` bit(1) NOT NULL,
  `generated_at` datetime(6) DEFAULT NULL,
  `s3_key` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `salary_day` date NOT NULL,
  `salary_month` int NOT NULL,
  `salary_week` int NOT NULL,
  `salary_year` int NOT NULL,
  `site_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_payroll_unique` (`employee_id`,`salary_year`,`salary_month`,`pay_cycle`,`salary_week`,`salary_day`),
  KEY `idx_employee_id` (`employee_id`),
  KEY `idx_contract_id` (`contract_id`),
  KEY `idx_corporation_id` (`corporation_id`),
  KEY `idx_search_date` (`search_date`),
  KEY `idx_pay_status` (`pay_status`),
  KEY `idx_s3_key` (`s3_key`),
  KEY `idx_site_id` (`site_id`),
  KEY `idx_period_search` (`site_id`,`salary_year`,`salary_month`,`emp_type`,`pay_cycle`),
  CONSTRAINT `payrolls_ibfk_1` FOREIGN KEY (`employee_id`) REFERENCES `employees` (`id`) ON DELETE CASCADE,
  CONSTRAINT `payrolls_ibfk_2` FOREIGN KEY (`contract_id`) REFERENCES `contracts` (`id`) ON DELETE CASCADE,
  CONSTRAINT `payrolls_ibfk_3` FOREIGN KEY (`corporation_id`) REFERENCES `corporations` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='급여';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payslip_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `is_deleted` bit(1) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `amount` decimal(15,2) NOT NULL,
  `effective_date` date NOT NULL,
  `item_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `item_type` enum('DEDUCTION','EARNING') COLLATE utf8mb4_unicode_ci NOT NULL,
  `payroll_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_payroll_id` (`payroll_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `roles` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '역할 ID',
  `role_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '역할명',
  `description` text COLLATE utf8mb4_unicode_ci COMMENT '역할 설명',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
  `is_deleted` bit(1) NOT NULL,
  `updated_at` datetime(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `role_name` (`role_name`),
  UNIQUE KEY `UK716hgxp60ym1lifrdgp67xt5k` (`role_name`),
  KEY `idx_role_name` (`role_name`)
) ENGINE=InnoDB AUTO_INCREMENT=32 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='역할';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `safety_doc_attendees` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '참석자 ID',
  `safety_doc_id` bigint NOT NULL COMMENT '안전교육 문서 ID',
  `employee_id` bigint NOT NULL COMMENT '근로자 ID',
  `emp_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '근로자명 (스냅샷)',
  `emp_type` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '근로자 유형',
  `attendance_status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ATTENDED' COMMENT '참석 상태 (ATTENDED, ABSENT)',
  `signed_at` datetime DEFAULT NULL COMMENT '서명 시각',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_attendee` (`safety_doc_id`,`employee_id`),
  KEY `idx_safety_doc_id` (`safety_doc_id`),
  KEY `idx_employee_id` (`employee_id`),
  KEY `idx_attendance_status` (`attendance_status`),
  CONSTRAINT `safety_doc_attendees_ibfk_1` FOREIGN KEY (`safety_doc_id`) REFERENCES `safety_docs` (`id`) ON DELETE CASCADE,
  CONSTRAINT `safety_doc_attendees_ibfk_2` FOREIGN KEY (`employee_id`) REFERENCES `employees` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='안전교육 참석자';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `safety_docs` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '안전교육 문서 ID',
  `site_id` bigint NOT NULL COMMENT '현장 ID',
  `manager_id` bigint NOT NULL COMMENT '관리자 ID',
  `safetydoc_title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '안전교육 제목',
  `safetydoc_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '안전교육 유형',
  `safetydoc_context` text COLLATE utf8mb4_unicode_ci COMMENT '안전교육 내용',
  `safetydoc_employee_cnt` int DEFAULT '0' COMMENT '참석 인원 수',
  `safetydoc_status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT' COMMENT '문서 상태 (DRAFT, PUBLISHED, COMPLETED)',
  `safetydoc_created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '교육 생성 시각',
  `safetydoc_updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '교육 수정 시각',
  PRIMARY KEY (`id`),
  KEY `idx_site_id` (`site_id`),
  KEY `idx_manager_id` (`manager_id`),
  KEY `idx_safetydoc_status` (`safetydoc_status`),
  KEY `idx_safetydoc_created_at` (`safetydoc_created_at`),
  CONSTRAINT `safety_docs_ibfk_1` FOREIGN KEY (`site_id`) REFERENCES `sites` (`id`) ON DELETE CASCADE,
  CONSTRAINT `safety_docs_ibfk_2` FOREIGN KEY (`manager_id`) REFERENCES `managers` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='안전교육 문서';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `safety_education_attendees` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `is_deleted` bit(1) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `is_signed` bit(1) NOT NULL,
  `signature_image_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `signed_at` datetime(6) DEFAULT NULL,
  `employee_id` bigint NOT NULL,
  `safety_education_log_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_safety_attendee_log_id` (`safety_education_log_id`),
  KEY `idx_safety_attendee_employee_id` (`employee_id`),
  CONSTRAINT `FKdf5vfu5rsv7th4qwg0sm777l4` FOREIGN KEY (`employee_id`) REFERENCES `employees` (`id`),
  CONSTRAINT `FKt63tufsvqmrvkhuvm4uoitnu7` FOREIGN KEY (`safety_education_log_id`) REFERENCES `safety_education_logs` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `safety_education_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `is_deleted` bit(1) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `education_content` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `education_location` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `education_subject` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `education_type` enum('ETC','HIRING','REGULAR','SPECIAL','WORK_CHANGE') COLLATE utf8mb4_unicode_ci NOT NULL,
  `final_pdf_hash` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `final_pdf_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `instructor_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `manager_signed_at` datetime(6) DEFAULT NULL,
  `pdf_generated_at` datetime(6) DEFAULT NULL,
  `pdf_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` enum('COMPLETED','DRAFT','MANAGER_SIGNED','MANAGER_SIGNING_PENDING') COLLATE utf8mb4_unicode_ci NOT NULL,
  `corporation_id` bigint NOT NULL,
  `manager_id` bigint NOT NULL,
  `site_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_safety_edu_site_id` (`site_id`),
  KEY `idx_safety_edu_manager_id` (`manager_id`),
  KEY `idx_safety_edu_corporation_id` (`corporation_id`),
  KEY `idx_safety_edu_status` (`status`),
  CONSTRAINT `FKebka2y4xvcp3q2s6u66jkdhdd` FOREIGN KEY (`manager_id`) REFERENCES `managers` (`id`),
  CONSTRAINT `FKhhixw6sbfcrd4tsttci0sl9jl` FOREIGN KEY (`corporation_id`) REFERENCES `corporations` (`id`),
  CONSTRAINT `FKmrjhjgy2ap4kd5idf5vq92dqy` FOREIGN KEY (`site_id`) REFERENCES `sites` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `safety_education_sign_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `is_deleted` bit(1) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `signature_hash` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `signature_height` decimal(10,2) DEFAULT NULL,
  `signature_image_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `signature_width` decimal(10,2) DEFAULT NULL,
  `signature_x` decimal(10,2) DEFAULT NULL,
  `signature_y` decimal(10,2) DEFAULT NULL,
  `signed_at` datetime(6) DEFAULT NULL,
  `signed_device` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `signed_ip` varchar(45) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `signer_id` bigint NOT NULL,
  `signer_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `signer_role` enum('CORPORATION','EMPLOYEE','MANAGER') COLLATE utf8mb4_unicode_ci NOT NULL,
  `verification_status` enum('FAILED','PENDING','VERIFIED') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `verified_at` datetime(6) DEFAULT NULL,
  `safety_education_log_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_safety_sign_log_id` (`safety_education_log_id`),
  KEY `idx_safety_sign_signer_id` (`signer_id`),
  KEY `idx_safety_sign_verification` (`verification_status`),
  CONSTRAINT `FKsmew35aej35sjnmojuqd7gbdh` FOREIGN KEY (`safety_education_log_id`) REFERENCES `safety_education_logs` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `safety_sign_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '서명 로그 ID',
  `attendee_id` bigint NOT NULL COMMENT '참석자 ID',
  `signer_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '서명자 유형 (EMPLOYEE, MANAGER)',
  `signature_hash` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '서명 해시값',
  `signature_image_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '서명 이미지 URL (S3)',
  `signed_device` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '서명 기기 정보',
  `signed_ip` varchar(45) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '서명 IP 주소',
  `signed_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '서명 시각',
  `verified_at` datetime DEFAULT NULL COMMENT '검증 시각',
  `verification_status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING' COMMENT '검증 상태 (PENDING, VERIFIED, FAILED)',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
  PRIMARY KEY (`id`),
  KEY `idx_attendee_id` (`attendee_id`),
  KEY `idx_signer_type` (`signer_type`),
  KEY `idx_verification_status` (`verification_status`),
  KEY `idx_signed_at` (`signed_at`),
  CONSTRAINT `safety_sign_logs_ibfk_1` FOREIGN KEY (`attendee_id`) REFERENCES `safety_doc_attendees` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='안전교육 서명 로그';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `signing_sessions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `is_deleted` bit(1) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `callback_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `expires_at` datetime(6) DEFAULT NULL,
  `session_token_hash` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `signer_role` enum('CORPORATION','EMPLOYEE','MANAGER') COLLATE utf8mb4_unicode_ci NOT NULL,
  `signer_user_id` bigint DEFAULT NULL,
  `state` enum('CANCELED','EXPIRED','PENDING','SIGNED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `contract_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_session_token_hash` (`session_token_hash`),
  KEY `idx_contract_id` (`contract_id`),
  KEY `idx_signer_user_id` (`signer_user_id`),
  KEY `idx_contract_signer_role` (`contract_id`,`signer_role`),
  KEY `idx_state` (`state`),
  KEY `idx_expires_at` (`expires_at`),
  CONSTRAINT `FKipmv60swyxo7vnjinuda6sa2q` FOREIGN KEY (`contract_id`) REFERENCES `contracts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sites` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '현장 ID',
  `site_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '현장명',
  `site_address` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '현장 주소',
  `corporation_id` bigint NOT NULL COMMENT '법인 ID',
  `manager_id` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
  `is_deleted` bit(1) NOT NULL,
  `employee_secret_key` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `manager_secret_key` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `secret_key_expires_at` datetime(6) DEFAULT NULL,
  `client_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `end_date` date DEFAULT NULL,
  `start_date` date DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK9d448y0f08xts72mrel5ubvhw` (`employee_secret_key`),
  UNIQUE KEY `UK7r6et0vmuk3iyjxtglk6kck3x` (`manager_secret_key`),
  KEY `idx_corporation_id` (`corporation_id`),
  KEY `idx_manager_id` (`manager_id`),
  KEY `idx_site_name` (`site_name`),
  CONSTRAINT `sites_ibfk_1` FOREIGN KEY (`corporation_id`) REFERENCES `corporations` (`id`) ON DELETE CASCADE,
  CONSTRAINT `sites_ibfk_2` FOREIGN KEY (`manager_id`) REFERENCES `managers` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=181 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='현장';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `temporary_registrations` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `is_deleted` bit(1) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `emp_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '근로자 이름 (1단계)',
  `expires_at` datetime(6) NOT NULL COMMENT '만료 시간 (기본 30분)',
  `password_hash` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '암호화된 비밀번호 (BCrypt)',
  `registration_token` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '등록 토큰 (UUID, 2단계 인증용)',
  `secret_key` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '시크릿키 (1단계, nullable)',
  `user_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용자 ID (1단계)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK21wlpufwlverv2y5s8mookrtc` (`registration_token`),
  UNIQUE KEY `UKq2ldnwgkq8phuijtki7i02xpe` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='2단계 회원가입 임시 저장';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '사용자 ID',
  `user_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '사용자 아이디 (로그인 ID)',
  `password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '암호화된 비밀번호',
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '전화번호',
  `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '이메일',
  `secret_key` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '비밀 키 (2FA 등)',
  `role_id` bigint NOT NULL COMMENT '역할 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
  `is_deleted` bit(1) NOT NULL,
  `profile_completed` bit(1) NOT NULL,
  `profile_token` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `profile_token_expires_at` datetime(6) DEFAULT NULL,
  `refresh_token` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `refresh_token_expires_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id` (`user_id`),
  UNIQUE KEY `UK6efs5vmce86ymf5q7lmvn2uuf` (`user_id`),
  UNIQUE KEY `UKjdbcdsefx5q1x9fo27h6d2js` (`profile_token`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_email` (`email`),
  KEY `idx_role_id` (`role_id`),
  CONSTRAINT `users_ibfk_1` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=524 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `work_report_employees` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '작업일보 근로자 ID',
  `work_report_id` bigint NOT NULL COMMENT '작업일보 ID',
  `employee_id` bigint NOT NULL COMMENT '근로자 ID',
  `emp_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '근로자명 (스냅샷)',
  `emp_type` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '근로자 유형',
  `work_hours` decimal(6,2) DEFAULT NULL COMMENT '작업 시간',
  `role_in_section` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '작업 구역 내 역할',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
  PRIMARY KEY (`id`),
  KEY `idx_work_report_id` (`work_report_id`),
  KEY `idx_employee_id` (`employee_id`),
  CONSTRAINT `work_report_employees_ibfk_1` FOREIGN KEY (`work_report_id`) REFERENCES `work_reports` (`id`) ON DELETE CASCADE,
  CONSTRAINT `work_report_employees_ibfk_2` FOREIGN KEY (`employee_id`) REFERENCES `employees` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='작업일보 근로자';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `work_report_materials` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '작업일보 자재 ID',
  `work_report_id` bigint NOT NULL COMMENT '작업일보 ID',
  `material_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '자재명',
  `material_standard` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '규격',
  `material_unit` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `material_today` decimal(10,2) DEFAULT NULL COMMENT '금일 사용량',
  `material_sum` decimal(10,2) DEFAULT NULL COMMENT '누적 사용량',
  `note` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '비고',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
  `is_deleted` bit(1) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_work_report_id` (`work_report_id`),
  KEY `idx_material_name` (`material_name`),
  CONSTRAINT `work_report_materials_ibfk_1` FOREIGN KEY (`work_report_id`) REFERENCES `work_reports` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='작업일보 자재';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `work_reports` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '작업일보 ID',
  `site_id` bigint NOT NULL COMMENT '현장 ID',
  `manager_id` bigint NOT NULL COMMENT '관리자 ID',
  `corporation_id` bigint NOT NULL COMMENT '법인 ID',
  `work_sections` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `work_report_title` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `work_report_created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '작성 시각',
  `work_report_started_at` datetime DEFAULT NULL COMMENT '작업 시작 시각',
  `work_report_ended_at` datetime DEFAULT NULL COMMENT '작업 종료 시각',
  `work_section_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '작업 구역명',
  `work_report_status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT' COMMENT '작업일보 상태 (DRAFT, SUBMITTED, APPROVED)',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
  `is_deleted` bit(1) NOT NULL,
  `pdf_generated_at` datetime(6) DEFAULT NULL,
  `pdf_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_site_id` (`site_id`),
  KEY `idx_manager_id` (`manager_id`),
  KEY `idx_corporation_id` (`corporation_id`),
  KEY `idx_work_report_status` (`work_report_status`),
  KEY `idx_work_report_created_at` (`work_report_created_at`),
  CONSTRAINT `work_reports_ibfk_1` FOREIGN KEY (`site_id`) REFERENCES `sites` (`id`) ON DELETE CASCADE,
  CONSTRAINT `work_reports_ibfk_2` FOREIGN KEY (`manager_id`) REFERENCES `managers` (`id`) ON DELETE CASCADE,
  CONSTRAINT `work_reports_ibfk_3` FOREIGN KEY (`corporation_id`) REFERENCES `corporations` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='작업일보';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

