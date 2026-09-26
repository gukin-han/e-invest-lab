-- PDF 원본은 파일시스템에, DB 에는 저장 루트 기준 상대 경로만. NULL = 미다운로드(재실행 대상).

ALTER TABLE analyst_reports
    ADD COLUMN pdf_path VARCHAR(300) NULL;
