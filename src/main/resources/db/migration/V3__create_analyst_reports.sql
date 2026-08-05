-- 애널리스트 리포트 목록 메타 (한경 컨센서스 기업 분류).
-- report_idx(한경 리포트 식별자)가 자연키 — 재수집 방지. 목표주가·투자의견은 목록 명시값(없을 수 있음).

CREATE TABLE analyst_reports (
    id             BINARY(16)   NOT NULL,
    report_idx     BIGINT       NOT NULL,
    stock_code     CHAR(6)      NOT NULL,
    company_name   VARCHAR(255) NOT NULL,
    title          VARCHAR(500) NOT NULL,
    broker         VARCHAR(100) NOT NULL,
    authors        VARCHAR(255),
    published_date DATE         NOT NULL,
    target_price   BIGINT,
    opinion        VARCHAR(50),
    collected_at   TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_analyst_reports_report_idx (report_idx),
    KEY idx_analyst_reports_stock_code (stock_code)
) ENGINE = InnoDB;
