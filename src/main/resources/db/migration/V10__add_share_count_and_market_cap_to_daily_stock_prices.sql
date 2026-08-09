-- 원천(금융위 시세)이 주는 lstgStCnt(상장주식수)·mrktTotAmt(시가총액) 승격.
-- 기존 행은 값 없이 존재하므로 NULL 허용 — 백필 재수집(upsert)이 채운다.

ALTER TABLE daily_stock_prices
    ADD COLUMN listed_share_count BIGINT NULL,
    ADD COLUMN market_cap BIGINT NULL;
