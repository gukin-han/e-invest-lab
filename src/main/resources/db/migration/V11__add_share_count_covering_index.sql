-- 주식수 변화 랭킹의 LAG 스캔을 인덱스 온리로 만들기 위한 커버링 인덱스.
-- (stock_code, trade_date) 파티션·정렬 순서 그대로라 윈도우 함수가 정렬 없이 소비.

CREATE INDEX idx_daily_stock_prices_share_seq
    ON daily_stock_prices (stock_code, trade_date, listed_share_count);
