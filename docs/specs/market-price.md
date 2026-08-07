# 일별 주가 수집 (market 도메인)

> 목표: EPS 축적물과 결합해 PER(주가 ÷ EPS)을 보기 위한 일별 종가 수집. 원본만 저장하고 PER 은 조회 시 계산 — 통계 후작업 원칙 동일.

## 소스 결정 (2026-08-07)

- **공공데이터포털 금융위원회_주식시세정보** (`GetStockSecuritiesInfoService/getStockPriceInfo`).
  - 날짜(basDt) 기준 **전 상장 종목 일괄** 조회 — 페이지 5,000행이면 **하루치 전 종목 = 1호출** (실측 2,872행 단일 페이지 확인). 일일 트래픽 한도는 행이 아니라 **호출 수** 기준(기본 10,000/일)이라, 일상 수집(7일 창)은 하루 7호출, 과거 백필도 하루 한도로 약 40년치(연 250거래일)까지 가능.
  - 인증은 서비스 키 1개(`STOCK_PRICE_API_KEY`, DART 패턴). 키는 포털이 준 **인코딩된 원본 그대로** 쿼리에 넣는다 — 재인코딩하면 미등록 키 오류.
  - 당일 종가는 익일 반영 — 비실시간 원칙상 무해.
- 토스증권 Open API(2026-05 출시)는 보류: 시세가 종목 단위 조회 중심이라 전 종목 배치엔 호출 수가 불리하고, 계좌·IP 등록·OAuth 관리 비용 대비 강점(실시간·주문·미국주식)을 지금 안 씀. **즐겨찾기 푸시(실시간 감지)·미국 주식 확장 단계에서 재검토.**

## 실측 탐침 (2026-08-07)

- 2026-08-06 기준 totalCount 2,872 (KOSPI/KOSDAQ/KONEX 포함).
- 필드: basDt, srtnCd(6자리), mrktCtg, clpr/mkp/hipr/lopr(종/시/고/저), trqu(거래량), 그 외 trPrc(거래대금)·mrktTotAmt(시총)·lstgStCnt(상장주식수)는 **미저장** — 필요 시 컬럼 추가 + 기간 재수집(upsert 멱등)으로 과거분 백필 가능.
- 휴일은 totalCount 0 + 빈 배열(정상 응답).
- **거래정지 종목은 시/고/저가 0** 으로 옴 — 해석 없이 그대로 저장(raw 원칙).

## 구조

- `daily_stock_prices`: (stock_code, trade_date) 유니크, 시/고/저/종가·거래량·시장구분·collected_at.
- 수집: `POST /internal/daily-stock-prices/collect?from=&to=` — 기본 최근 7일 창, 날짜별로 전 종목 upsert(날짜 단위 트랜잭션). 창이 겹쳐도 upsert 멱등. 휴일은 0건이라 자연 스킵.
- 스케줄: 매일 18:10 KST (`DailyStockPriceScheduler`) — 7일 창이라 익일 반영 지연·서버 다운을 자체 흡수.
- 포트: `DailyStockPriceSource`(원천 사실 record 반환) / `DailyStockPriceRepository`(upsert + 종목별 최신 조회).

## 다음

1. 라이브 검증 (전 종목 7일 창 수집).
2. **PER 조회** — 컨센서스 응답에 최근 종가와 연도별 forward PER(종가 ÷ 연도 컨센서스 EPS) 추가. 저장하지 않고 조회 시 계산.
