-- 상장주식수 계단(변화 이벤트) 파생 테이블. daily_stock_prices 에서 전량 재계산 가능한 캐시 —
-- 원본이 아니므로 자연키 (stock_code, trade_date) 그대로 PK. 랭킹의 LAG 전량 스캔(십수 초)을 대체.

CREATE TABLE share_count_changes (
    stock_code  CHAR(6)       NOT NULL,
    trade_date  DATE          NOT NULL,
    prev_count  BIGINT        NOT NULL,
    new_count   BIGINT        NOT NULL,
    change_pct  DECIMAL(10,2) NOT NULL,
    computed_at TIMESTAMP(6)  NOT NULL,
    PRIMARY KEY (stock_code, trade_date),
    KEY idx_share_count_changes_trade_date (trade_date)
) ENGINE = InnoDB;
