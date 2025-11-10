-- Contract 테이블에 최종 PDF 정보 추가
ALTER TABLE contracts
ADD COLUMN final_pdf_url VARCHAR(500) COMMENT '최종 PDF S3 URL',
ADD COLUMN final_pdf_hash VARCHAR(255) COMMENT '최종 PDF SHA-256 해시값',
ADD COLUMN pdf_generated_at DATETIME COMMENT 'PDF 생성 시각';

