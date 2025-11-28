-- signed_device 컬럼 길이 확장 (100 -> 500)
-- User-Agent 문자열이 100자를 초과할 수 있음

ALTER TABLE contract_sign_logs 
    MODIFY COLUMN signed_device VARCHAR(500) COMMENT '서명 기기 정보 (User-Agent)';

ALTER TABLE safety_education_sign_logs 
    MODIFY COLUMN signed_device VARCHAR(500) COMMENT '서명 기기 정보 (User-Agent)';

