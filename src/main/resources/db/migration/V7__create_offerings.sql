-- Offering: 사업보고서가 명시한 사실 하나 = 행 하나. 유일성 제약을 두지 않는다 —
-- segment 는 유니크 키가 아니고(연도·사실 분리·국내/해외 하위 구분), 서식 변형이 제약을 수집 실패로 만든다.
-- 멱등은 filing_number 단위 교체(선삭제 후 삽입)와 business_contents.offering_extraction_status 로 보장.
-- 상태: NULL=미시도, EXTRACTED=가드 통과, CORRECTED=교정 후 통과, FAILED=재시도 대상.

ALTER TABLE business_contents
    ADD COLUMN offering_extraction_status VARCHAR(30) NULL;

CREATE TABLE offerings (
    id             BINARY(16)    NOT NULL,
    corp_code      CHAR(8)       NOT NULL,
    filing_number  CHAR(14)      NOT NULL,
    business_part  VARCHAR(50),
    segment        VARCHAR(100),
    qualifier      VARCHAR(100),
    products       JSON          NOT NULL,
    revenue_amount DECIMAL(20,2),
    revenue_unit   VARCHAR(30),
    revenue_basis  VARCHAR(50),
    revenue_share  DECIMAL(6,2),
    customers      JSON          NOT NULL,
    entity_name    VARCHAR(100),
    fiscal_year    INT,
    extracted_at   TIMESTAMP(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_offerings_corp_code (corp_code),
    KEY idx_offerings_filing_number (filing_number)
) ENGINE = InnoDB;
