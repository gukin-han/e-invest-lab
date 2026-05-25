-- 살아있는 도메인의 초기 스키마.
-- disclosure / analysis_report / market_reaction / stock_price
-- Company 도메인은 V2 에서 별도로 추가.

CREATE TABLE disclosure (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    receipt_number  VARCHAR(255) NOT NULL,
    corporate_name  VARCHAR(255) NOT NULL,
    stock_code      VARCHAR(255),
    title           VARCHAR(255) NOT NULL,
    disclosed_at    DATETIME(6)  NOT NULL,
    document_url    VARCHAR(255),
    status          VARCHAR(255) NOT NULL,
    category        VARCHAR(255),
    created_at      DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_disclosure_receipt_number (receipt_number)
) ENGINE = InnoDB;

CREATE TABLE analysis_report (
    id              BIGINT         NOT NULL AUTO_INCREMENT,
    disclosure_id   BIGINT         NOT NULL,
    receipt_number  VARCHAR(255)   NOT NULL,
    corporate_name  VARCHAR(255)   NOT NULL,
    title           VARCHAR(255)   NOT NULL,
    sentiment       VARCHAR(255)   NOT NULL,
    score           INT            NOT NULL,
    summary         VARCHAR(2000)  NOT NULL,
    analyzed_at     DATETIME(6)    NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB;

CREATE TABLE market_reaction (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    disclosure_id   BIGINT          NOT NULL,
    stock_code      VARCHAR(255)    NOT NULL,
    prior_close     BIGINT          NOT NULL,
    current_close   BIGINT          NOT NULL,
    change_rate     DECIMAL(10, 4)  NOT NULL,
    tracked_at      DATETIME(6)     NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB;

CREATE TABLE stock_price (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    stock_code    VARCHAR(255) NOT NULL,
    trading_date  DATE         NOT NULL,
    open_price    BIGINT       NOT NULL,
    high_price    BIGINT       NOT NULL,
    low_price     BIGINT       NOT NULL,
    close_price   BIGINT       NOT NULL,
    volume        BIGINT       NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_stock_price_code_date (stock_code, trading_date)
) ENGINE = InnoDB;
