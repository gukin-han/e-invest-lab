# 시간 처리 룰

현재 시각을 읽고, 나르고, 해석하는 방식을 정한다.

- 원칙: **원천은 하나, 캡처는 경계에서 한 번, 해석은 사용처에서.**

## 1. Clock 빈은 하나만 등록한다

- 규칙
  - `ClockConfig` 의 `Clock.systemDefaultZone()` 하나가 애플리케이션의 "지금"을 독점한다.
  - `UtcClock`/`KstClock` 처럼 시간대별 빈을 병렬 등록하지 않는다.
- 이유
  - "지금"이라는 순간은 하나다. 시간대는 원천이 아니라 해석의 문제라, 빈을 나누면 같은 원천의 이중 등록 + 주입 실수 여지만 생긴다.

## 2. 캡처는 진입점(interfaces)에서 한 번, 아래로는 값으로 흐른다

- 규칙
  - 트리거(컨트롤러·스케줄러·리스너)가 주입받은 Clock 에서 `clock.instant()` 로 한 번 읽고, 유스케이스 → 포트로 `Instant` 파라미터로 내려보낸다.
  - application·domain·infra 는 Clock 을 주입받지 않는다. 인자 없는 `now()` 는 어디서도 부르지 않는다.
- 이유
  - 한 요청에 "지금"이 하나가 된다 — 요청 중간에 `now()` 를 여러 번 읽어 생기는 시각 불일치(자정 경계 등)가 원천 차단된다.
  - 아래 계층 전체가 시각의 순수 함수가 되어 테스트에 Clock 모킹조차 필요 없다.
  - 기준 시각이 파라미터이므로 과거 시점 재처리가 값 하나로 가능해진다.
  - 캡처는 경계 한 곳에서만 한다 — 재처리 가능성의 동전 뒷면은 호출자가 시각을 조작할 수 있다는 것이므로, 규율로 관리한다.

## 3. 내려보내는 타입은 Instant

- 규칙
  - 계층을 넘는 시각은 `Instant`(시간대 중립인 순간)로 나른다. `LocalDate`/`LocalDateTime` 으로 바꾸는 건 해석이므로 사용처의 일이다.
- 이유
  - "이 순간을 한국 날짜로 읽는다"(DART 날짜 파라미터 = KST)는 벤더 지식이라 어댑터에 남아야 한다. 경계가 미리 `LocalDate` 로 바꿔 내리면 그 지식이 경계로 새어 나간다.

Good (어댑터에서의 해석):
```java
private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");

LocalDate endDate = baseTime.atZone(KOREA).toLocalDate();
```

## 4. 테스트

- 규칙
  - 시각을 값으로 받는 계층은 `Instant.parse(...)` 를 그대로 넘긴다.
  - Clock 을 쓰는 진입점은 `Clock.fixed(instant, zone)` 을 꽂는다. 시간 경과 시나리오는 `Clock.offset` 으로 만든다.
  - `LocalDate.now()` 정적 모킹은 쓰지 않는다 — JVM 전역을 건드려 병렬 테스트에서 샌다.

## 5. 도메인 로직은 시각을 인자로 받는 순수 함수로

- 규칙
  - "기준일보다 오래됐는가" 류의 판단은 `isStaleAt(LocalDate baseDate)` 처럼 시각을 파라미터로 받는다.
- 이유
  - Clock 이 도메인 깊숙이 전파되고 있다면 시각을 값으로 넘기라는 신호다.
