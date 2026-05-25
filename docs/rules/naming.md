# 네이밍 룰

## 원칙
- **타입과 이름이 일치한다.** 이름만 봐도 타입을 짐작할 수 있고, 타입만 봐도 이름이 자연스러워야 한다.

## 날짜·시각

| 타입 | 접미사 | 의미 | 예시 |
|---|---|---|---|
| `LocalDateTime` / `Instant` / `OffsetDateTime` | **`xxxAt`** | 특정 시각 (날짜 + 시간) | `createdAt`, `updatedAt`, `disclosedAt`, `analyzedAt` |
| `LocalDate` | **`xxxDate`** 또는 `xxxOn` | 날짜만 | `birthDate`, `expiryDate`, `tradingDate`, `masterModifiedDate` |
| `LocalTime` | `xxxTime` 또는 `xxxAt` | 시각만 (날짜 없음) | `openTime` |
| `Duration` / `Period` | `xxxDuration` / `xxxPeriod` | 기간 | `timeoutDuration` |

### 피할 패턴
- `xxxDateTime` — 타입을 이름에 노출. `xxxAt` 이 자연스럽다.
- `xxxTime` (`LocalDateTime` 용) — 시각인지 기간인지 모호.
- 타입 ↔ 이름 불일치: `LocalDate createdAt`, `LocalDateTime registerDate`.

### Spring Data Auditing 어노테이션 주의
`@CreatedDate` / `@LastModifiedDate` 이름은 "Date" 지만 실제 필드 타입은 `LocalDateTime` 이 흔하다. 어노테이션 이름과 무관하게 **타입에 맞는 변수명**을 쓴다.

```java
@CreatedDate
private LocalDateTime createdAt;     // 어노테이션 이름과 달라도 OK — 타입과 일치가 우선

@LastModifiedDate
private LocalDateTime updatedAt;
```
