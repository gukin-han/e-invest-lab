# 로깅 룰

로그 메시지의 형식과 필드 이름을 통일한다. 로그 레벨을 어느 상황에 쓰는지는 [예외 처리 룰](exception.md)의 분류별 정책을 따른다.

## 1. 메시지는 영어 소문자 이벤트 문장으로 쓴다

- 규칙
  - 로그 메시지는 영어 소문자 이벤트 문장으로 통일한다.
  - "무슨 일이 일어났는가"를 문장으로 적고, 뒤에 `key=value`로 맥락을 붙인다.
  - 한국어 메시지를 쓰지 않는다.
- 이유
  - 이벤트명이 일정하면 로그 검색·집계·알림 규칙을 걸기 쉽다.
  - 코드·이벤트명·검색성을 함께 보면 영어가 일관성이 높다.

Bad:
```java
log.info("회사 등록부 동기화 완료: " + count + "건");
```

Good:
```java
log.info("company registry sync completed. upsertedCount={}", count);
```

## 2. 변수값을 문자열로 결합하지 않고 placeholder를 쓴다

- 규칙
  - 메시지에 변수값을 `+`로 결합하지 않는다.
  - SLF4J placeholder `{}`로 값을 전달한다.
  - 예외는 마지막 인자로 넘겨 스택을 남긴다.
- 이유
  - placeholder는 해당 레벨이 꺼져 있을 때 문자열 생성을 건너뛴다.
  - 포맷이 깔끔하고 메시지와 값의 대응이 분명하다.

Bad:
```java
log.info("memberId: " + memberId + " registry synced");
log.error("sync failed " + e.getMessage());
```

Good:
```java
log.info("registry synced. memberId={}", memberId);
log.error("registry sync failed. memberId={}", memberId, e);
```

## 3. 로그 필드 이름을 통일한다

- 규칙
  - 같은 의미의 식별자는 항상 같은 키 이름으로 적는다.
  - 키는 camelCase로 쓴다.
  - 도메인 용어가 다른 경우가 아니면 로그 필드는 최대한 통일한다.
- 이유
  - `userId`, `memberNo`, `empId`처럼 섞이면 한 주체를 로그로 추적하기 어렵다.
  - 통일된 키는 로그 상관관계(correlation) 분석의 전제다.

기준 키 예시:

```text
clientId
memberId
requestId
jobId
targetYmd
elapsedMs
```

Bad:
```java
log.info("job done. user={}, ymd={}", memberId, date);
log.info("job done. memberNo={}, targetDate={}", memberId, date);
```

Good:
```java
log.info("job done. memberId={}, targetYmd={}", memberId, date);
```
