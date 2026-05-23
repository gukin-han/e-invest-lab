# V3 스트리밍 메모리 실험 — DB 쓰기 포함 end-to-end

> 측정 기간: 2026-05-21 ~ 2026-05-23
> 코드: [`CorpCodeFetchSmokeTestV3.java`](../../../src/test/java/dev/gukin/einvestlab/company/CorpCodeFetchSmokeTestV3.java)

## Summary

```
메모리 footprint ≈ (1 row당 메모리 단가) × batch 사이즈 + JDBC 인터널
                ≈ 0.0025 MB/row × batch + 2.2 MB
```

- **lever 변수**: batch 사이즈 (정거장 크기)
- **무관 변수**: heap 한도, 입력 크기(단일 / 누적), 누적 호출 수
- **상수 가산**: JDBC 인터널 ~2.2MB
- 측정 한계 사례: 100배 큰 입력(3GB XML, 11.8M rows)도 -Xmx64m 안에서 처리

## 배경

V1/V2가 검증한 것은 "파싱 → 도메인 객체 생성 → 즉시 폐기" 흐름의 메모리 일정성. V3는 DB upsert를 포함한 end-to-end 흐름에서도 동일 패턴이 유지되는지, 어떤 차원이 footprint를 실제로 움직이는지 측정한다.

"정거장 모델"은 이 프로젝트 내 비유로, 스트리밍 파이프라인의 메모리 footprint가 처리 데이터 크기가 아니라 한 번에 머무는 객체 수(= batch 사이즈)에만 비례한다는 가설을 가리킨다.

## 실험 설계

5개 축으로 sweep:

| 차원 | 변수 | 값 |
|---|---|---|
| A | heap 한도 | 32m / 64m / 128m / 256m (batch=1000, iter=1, input=1x) |
| B | 누적 반복 횟수 | iter=10 × heap ∈ {32m, 64m, 128m} (batch=1000, input=1x) |
| C | batch 사이즈 | 100 / 500 / 1000 / 5000 / 10000 (heap=64m, iter=1, input=1x) — 5 trial씩 |
| D | **단일 입력 크기** | 1x / 10x / 100x (heap=64m, batch=1000, iter=1) — 합성 zip |
| F | **DB 호출 on/off** | dbon / dboff (heap=64m, batch=1000, iter=1) — 5 trial씩 |

DART 호출은 한 번뿐 (`~/.cache/e-invest-lab/corpCode.zip`에 캐싱). 10x/100x는 [`CorpCodeSyntheticZip`](../../../src/test/java/dev/gukin/einvestlab/company/CorpCodeSyntheticZip.java)으로 합성.

## 셋업

- Testcontainers `MySQLContainer<>("mysql:8.0").withReuse(true)` — 첫 실행만 ~30초, 이후 재사용
- `JdbcTemplate.batchUpdate(...)` + `ON DUPLICATE KEY UPDATE`
- JDBC URL에 `?rewriteBatchedStatements=true` (없으면 batch가 N번 INSERT로 풀림)
- 매 batch flush 직후 `Runtime.totalMemory() - freeMemory()` 측정 → CSV에 기록

코드: [`CorpCodeFetchSmokeTestV3.java`](../../../src/test/java/dev/gukin/einvestlab/company/CorpCodeFetchSmokeTestV3.java)
build task: `memTest{32m,...,256m}`, `memTestB-{32m,64m,128m}-iter10`, `memTestC-batch{100,...,10000}`

## A — heap 한도 sweep

같은 워크로드(118,122 rows), -Xmx만 8배 차이.

| -Xmx | heap 범위 | 평균 | 시간 |
|---|---|---|---|
| 32m | **26~30MB** | 27.4 | 4.47s |
| 64m | 27~58MB | 42.0 | 3.54s |
| 128m | 28~99MB | 61.7 | 3.67s |
| 256m | 28~170MB | 93.0 | 4.50s |

![A — heap 한도 비교](v3-heap-compare.png)

**핵심:** 한도를 8배 키워도 heap **최저점은 26~28MB로 묶임**. 늘어나는 건 톱니의 진폭(GC가 게을러지면서 쌓이는 garbage). 즉:
- 실제 필요 메모리(정거장 크기) ≈ 28MB
- heap 한도는 GC slack만 키움
- "필요한 메모리"와 "한도"는 직교

V1/V2가 본 메모리 일정성이 DB 쓰기를 포함해도 깨지지 않음. batch flush 직전이 가장 위험한 순간인데도 30MB(32m), 58MB(64m)에서 안전.

![A — 32m 단일 패턴](v3-heap-32m.png)

32m 케이스 단독으로 보면 4.47s 내내 26~30MB 좁은 밴드. 119번의 batch flush 사이 모두 같은 범위.

## B — 입력 크기 sweep (×10 누적)

같은 zip을 10번 반복 처리 → 1,181,220 rows. 정거장 모델이 누적 처리량에서도 유지되는지.

| -Xmx | heap 범위 | 평균 | 시간 | 처리량 |
|---|---|---|---|---|
| 32m | 25~30MB | 27.4 | 32.7s | 36.1k/s |
| 64m | 28~58MB | 43.2 | 25.5s | 46.4k/s |
| 128m | 28~100MB | 63.3 | 24.1s | 49.1k/s |

![B — iteration sweep](v3-heap-iterations.png)

**핵심:** 입력 크기를 10배로 늘려도(118k → 1.18M) heap 패턴 동일. A에서 본 한도별 밴드가 그대로 유지됨. 다른 말로:
- footprint는 batch size에만 의존, 입력 크기 / heap 한도 양쪽 모두와 독립
- 더 큰 워크로드에서도 OOM 위험 동일하게 없음

throughput은 한도가 클수록 약간씩 증가 (32m: 36k/s → 128m: 49k/s). GC가 덜 도는 만큼.

## C — batch 사이즈 sweep

`-Xmx64m` 고정, batch만 변경. 5 trial 평균 (각 case마다 5번 반복 측정).

| batch | flush 횟수 | heap 범위 (mean) | 시간 (mean) | 처리량 (mean) |
|---|---|---|---|---|
| 100 | 1182 | 25~58MB | 12.36s | 9.6k/s |
| 500 | 237 | 26~58MB | 4.60s | 25.7k/s |
| 1000 | 119 | 27~57MB | 3.72s | 31.9k/s |
| **5000** | **24** | **39~49MB** | **2.72s** | **43.5k/s** |
| 10000 | 12 | 46~57MB | 2.87s | 41.2k/s |

5 trial std는 대부분 ≤ 0.5MB / ≤ 0.3s. 측정 안정적.

![C — batch sweep (overlay)](v3-heap-batch.png)

![C — batch summary (5 trial scatter + mean)](v3-heap-batch-summary.png)

**핵심 1 — footprint 모양:** batch가 커질수록 heap의 **최저점**이 올라감 (27MB → 46MB). 정거장 안에 더 많이 쌓아두니까 당연. 즉 **정거장 크기 = f(batch)**.

**핵심 2 — throughput:** batch=100은 9.6k/s로 4.5배 느림 (JDBC round trip 1182번). batch=5000에서 43.5k/s로 정점, 10000은 약간 떨어짐 — JDBC round trip 감소 효과 포화 + 큰 batch의 overhead 시작.

**핵심 3 — sweet spot:** batch=5000이 throughput 최고, peak heap도 49MB로 가장 낮음 (band 10MB로 가장 좁음 — GC가 mixed mode로 자주 돌면서 한도까지 안 올라감). batch=1000은 안전한 기본값이지만 **5000으로 가면 36% 빠름**.

**핵심 4 — 1000 → 5000 사이 promote 임계:** floor가 27→39MB로 +12MB 점프 (같은 5배 변화인데 100→500은 +1MB뿐). 임시 객체가 young gen 안에서 회수되던 영역(≤1000)에서 old gen으로 promote되는 영역(≥5000)으로 GC 모드가 전환된 것으로 보임.

## D — 단일 입력 크기 sweep

같은 zip의 `<list>` 블록을 N번 복제한 합성 zip 사용. 1x/10x/100x = 30MB / 300MB / 3GB XML.
한 번의 streaming pass로 처리. (반복 호출 아닌 단일 입력 크기 자체를 키움.)

| 입력 | rows | zip 크기 | 풀린 XML | heap 범위 | 시간 | 처리량 |
|---|---|---|---|---|---|---|
| 1x | 118,122 | 3.6MB | 30MB | 27~57MB | 3.7s | 32.0k/s |
| 10x | 1,181,220 | 34.5MB | 300MB | 28~57MB | 24.9s | 47.4k/s |
| 100x | **11,812,200** | 345MB | **3GB** | **29~57MB** | 226.8s | **52.1k/s** |

![D — input sweep](v3-heap-input.png)

![D — input summary](v3-heap-input-summary.png)

**핵심:** **3GB XML이 30MB XML과 동일한 64MB 힙에 들어감.** Peak heap은 57MB로 입력 크기와 정확히 무관. 시간은 입력에 선형 (오른쪽 그래프의 점선이 ideal linear). 100x에서 throughput이 살짝 더 좋은 건 JVM warm-up + JIT 효과.

B 차원(반복)과 메시지가 다른 점: B는 "10번 호출해도 누적 메모리 일정", D는 "한 번에 100배 큰 입력도 같은 메모리". 정거장 모델이 두 시나리오 모두에 적용됨을 분리 확인.

## F — DB on/off 비교

V3-B의 floor(28MB)가 V2의 floor(~25MB)보다 약간 높았던 게 DB가 정거장에 들고 있는 메모리 때문인지 정량 측정.

코드에 `EXPERIMENT_SKIP_DB=true` 한 줄 분기만 추가. `batchUpdate()` 호출만 토글, `batch.clear()` + CSV 측정은 그대로 → **DB 호출 단변수**. 각 조건 5 trial.

| | floor (min) mean±std | peak (max) mean±std | 시간 |
|---|---|---|---|
| DB off | 25.2±0.4MB | 57.4±0.8MB | 0.48s |
| DB on | 27.4±0.5MB | 57.4±0.5MB | 4.48s |
| **Δ** | **+2.2MB** | **+0.0MB** | **+4.00s** |

![F — DB on/off](v3-heap-db.png)

**핵심:**
1. **Floor +2.2MB** — JDBC 인터널(PreparedStatement params, MySQL driver packet buffer 등)이 정거장에 잔존
2. **Peak 동일** — DB가 garbage 생성 속도는 안 키움. GC는 한도 근처에서 동일 동작
3. **시간의 9/10이 DB 쓰기** — 메모리 비용보다 시간 비용이 압도적
4. **V2와 정확히 일치** — V2 minimum ~25MB ≈ dboff 25.2MB. V2/V3 비교 신뢰성 확인

정거장 모델은 깨지지 않음. JDBC 상수 ~2MB만 추가.

## 종합

V1/V2의 "정거장 모델" 명제를 네 축에서 더 확인:

1. **heap 한도와 무관** — 한도는 GC slack만 키움 (A)
2. **누적 호출 수와 무관** — 같은 워크 10번 반복해도 패턴 동일 (B)
3. **batch 사이즈에만 비례** — flush 사이 쌓이는 양이 정거장 크기 결정 (C)
4. **단일 입력 크기와 무관** — 100배 큰 zip을 한 번에 처리해도 peak heap 동일 (D)
5. **DB 호출은 상수만 추가** — floor에 ~2.2MB 가산, 비례 항 아님 (F)

다른 말로 ADR의 정리:

> **메모리 footprint ≈ (1 row당 메모리 단가) × batch 사이즈 + JDBC 인터널(~2MB)**
>
> 입력 크기·heap 한도는 footprint에 영향 없음. batch 사이즈만이 정거장 크기를 비례적으로 결정. 1 row당 메모리 단가는 도메인 객체(`CompanyRow`)로 V3-C에서 대략 0.0025MB/row로 측정됨. JDBC 인터널은 V3-F에서 약 2.2MB의 floor 상수로 측정됨.

## 후속 결정 후보

- spec의 batch=1000은 안전한 기본값이지만 운영 적용 시 batch=5000 검토 가치 있음. 35% 처리량 향상, 64MB 한도 안에서 안전.
- 모니터링은 ADR 본문 권고대로 5000건 단위 heap 로그 — 운영에서 minimum이 정상 범위(28~50MB)에 머무는지 회귀 감지용.
