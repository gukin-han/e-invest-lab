-- 회사 마스터 (DART corpCode.xml 기준).
-- PK 는 내부용 UUID v7 을 BINARY(16) 으로 저장. corp_code 는 자연키이지만 unique 제약만 둠.
-- 프로필 컬럼은 단계 1b 에서 추가 예정 — 지금은 마스터만.

CREATE TABLE companies (
    id                   BINARY(16)   NOT NULL,
    corp_code            CHAR(8)      NOT NULL,
    name                 VARCHAR(255) NOT NULL,
    english_name         VARCHAR(255),
    stock_code           CHAR(6),
    master_modified_date DATE         NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_companies_corp_code (corp_code)
) ENGINE = InnoDB;
