-- 리포트가 명시한 연도별 EPS. report_idx + fiscal_year 가 자연키 — 한 리포트는 같은 연도를 두 번 말하지 않는다.
-- 추출 상태는 analyst_reports.eps_extraction_status: NULL=미시도, EXTRACTED/NO_SUMMARY_TABLE/FAILED.
-- NULL 과 FAILED 만 재추출 대상 — NO_SUMMARY_TABLE 은 "원래 없음" 확정이라 재시도하지 않는다.

ALTER TABLE analyst_reports
    ADD COLUMN eps_extraction_status VARCHAR(30) NULL;

CREATE TABLE eps_estimates (
    id           BINARY(16)    NOT NULL,
    report_idx   BIGINT        NOT NULL,
    fiscal_year  INT           NOT NULL,
    estimated    TINYINT(1)    NOT NULL,
    eps          DECIMAL(15,2) NOT NULL,
    extracted_at TIMESTAMP(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_eps_estimates_report_idx_fiscal_year (report_idx, fiscal_year)
) ENGINE = InnoDB;
