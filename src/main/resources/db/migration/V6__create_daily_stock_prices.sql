-- 일별 주가 raw (금융위 주식시세정보). stock_code + trade_date 가 자연키 — 재수집은 upsert 로 멱등.
-- 가격은 원 단위 정수. 거래정지 종목은 원천이 시/고/저가를 0 으로 주며 그대로 저장(해석하지 않음).

CREATE TABLE daily_stock_prices (
    id              BINARY(16)  NOT NULL,
    stock_code      CHAR(6)     NOT NULL,
    trade_date      DATE        NOT NULL,
    market_category VARCHAR(20) NOT NULL,
    open_price      INT         NOT NULL,
    high_price      INT         NOT NULL,
    low_price       INT         NOT NULL,
    close_price     INT         NOT NULL,
    volume          BIGINT      NOT NULL,
    collected_at    TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_daily_stock_prices_stock_code_trade_date (stock_code, trade_date),
    KEY idx_daily_stock_prices_trade_date (trade_date)
) ENGINE = InnoDB;
