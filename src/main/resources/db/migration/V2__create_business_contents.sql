-- 사업의 내용 1차 저장 (raw). granularity A: "II. 사업의 내용" 섹션 텍스트 통째.
-- filing_number(접수번호)가 자연키 — 같은 보고서 재수집 방지. 회사당 연도별 1건이라 시계열 축적 가능.
-- content 는 최대 관측치 157K자(UTF-8 약 470KB) 기준 MEDIUMTEXT.

CREATE TABLE business_contents (
    id            BINARY(16)   NOT NULL,
    corp_code     CHAR(8)      NOT NULL,
    filing_number CHAR(14)     NOT NULL,
    filed_date    DATE         NOT NULL,
    content       MEDIUMTEXT   NOT NULL,
    collected_at  TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_business_contents_filing_number (filing_number),
    KEY idx_business_contents_corp_code (corp_code)
) ENGINE = InnoDB;
