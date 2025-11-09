-- ContractSignLog에 서명 좌표 필드 추가
ALTER TABLE contract_sign_logs
ADD COLUMN signature_x DECIMAL(10,2) COMMENT '서명 X 좌표 (PDF pt)',
ADD COLUMN signature_y DECIMAL(10,2) COMMENT '서명 Y 좌표 (PDF pt)',
ADD COLUMN signature_width DECIMAL(10,2) COMMENT '서명 이미지 너비 (PDF pt)',
ADD COLUMN signature_height DECIMAL(10,2) COMMENT '서명 이미지 높이 (PDF pt)';

