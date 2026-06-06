# 네이밍 룰

## 1. 타입과 이름은 서로 일치하게 작성한다

- 규칙
  - 이름만 봐도 타입을 짐작할 수 있어야 한다.
  - 타입만 봐도 이름이 자연스러워야 한다.
  - Java 타입 이름을 기계적으로 변수명에 반복하지 않는다.
  - 도메인 의미를 분명히 하는 접미사는 허용한다.
- 이유
  - 타입과 이름이 어긋나면 코드를 읽을 때 의미를 다시 추론해야 한다.
  - 타입 변경이나 리팩토링 과정에서 잘못된 이름이 남는 것을 줄인다.

Bad:
```java
private LocalDate createdAt;
private LocalDateTime registerDate;
private LocalDateTime disclosedDateTime;
private List<Company> companyList;
```

Good:
```java
private LocalDate createdDate;
private LocalDateTime registeredAt;
private LocalDateTime disclosedAt;
private List<Company> companies;
```

## 2. 날짜와 시각 필드는 타입에 맞는 접미사를 사용한다

- 규칙
  - `LocalDateTime`, `Instant`, `OffsetDateTime`: `xxxAt`
  - `LocalDate`: `xxxDate` 또는 `xxxOn`
  - `LocalTime`: `xxxTime` 또는 `xxxAt`
  - `Duration`, `Period`: `xxxDuration`, `xxxPeriod`
- 이유
  - 날짜만 있는 값과 특정 시각을 나타내는 값을 이름으로 구분한다.
  - 시간 관련 필드는 도메인 의미가 비슷해 오해가 쉽게 생긴다.

| 타입 | 접미사 | 의미 | 예시 |
|---|---|---|---|
| `LocalDateTime` / `Instant` / `OffsetDateTime` | `xxxAt` | 특정 시각, 날짜 + 시간 | `createdAt`, `updatedAt`, `disclosedAt`, `analyzedAt` |
| `LocalDate` | `xxxDate` 또는 `xxxOn` | 날짜만 | `birthDate`, `expiryDate`, `tradingDate`, `registryModifiedDate` |
| `LocalTime` | `xxxTime` 또는 `xxxAt` | 시각만, 날짜 없음 | `openTime` |
| `Duration` / `Period` | `xxxDuration` / `xxxPeriod` | 기간 | `timeoutDuration` |

Bad:
```java
private LocalDateTime createdDate;
private LocalDateTime updatedTime;
private Duration timeoutAt;
```

Good:
```java
private LocalDateTime createdAt;
private LocalDateTime updatedAt;
private Duration timeoutDuration;
```

## 3. Spring Data Auditing 필드도 타입 기준으로 이름을 정한다

- 규칙
  - `@CreatedDate`, `@LastModifiedDate`의 `Date`라는 어노테이션 이름을 변수명 기준으로 삼지 않는다.
  - 실제 필드 타입이 `LocalDateTime`이면 `createdAt`, `updatedAt`처럼 `xxxAt`을 사용한다.
- 이유
  - Spring 어노테이션 이름보다 코드 안에서 사용하는 값의 타입과 의미가 더 중요하다.
  - 감사 필드는 여러 엔티티에 반복되므로 일관된 이름이 필요하다.

Bad:
```java
@CreatedDate
private LocalDateTime createdDate;

@LastModifiedDate
private LocalDateTime modifiedDate;
```

Good:
```java
@CreatedDate
private LocalDateTime createdAt;

@LastModifiedDate
private LocalDateTime updatedAt;
```
