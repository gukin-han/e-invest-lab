# 마이그레이션 도구 도입 — Flyway 채택

## 결정 요약
- **날짜**: 2026-05-25
- **상태**: 채택
- **결과**: `ddl-auto=update` → `validate`, 스키마 진본은 `src/main/resources/db/migration/V{n}__{설명}.sql`

## 컨텍스트
- 회사 마스터 도메인 작업을 시작하면서 스키마가 4개에서 5개로 늘어남
- 운영 배포는 아직 안 했지만 가까운 시일 안 예정. 운영 환경에서 ddl-auto는 사실상 금기 — 인스턴스 다중 시작 시 충돌, 컬럼 의도치 않은 변경 등 사고 사례 다수
- 회사에서는 Liquibase 사용 중. 사이드는 의도적으로 Flyway 차별 학습 자리로 활용

## 트레이드오프 (구현 비용 무시, 설계 가치만)

| 관점 | ddl-auto (기존) | Flyway (채택) |
|---|---|---|
| 스키마 진본 | `@Entity` 어노테이션 | `.sql` 파일 |
| 변경 추적 | git diff 어노테이션 변화로 숨음 | `V{n}__{설명}.sql` 명시적 |
| 환경 일관성 | drift 가능 | 동일 시퀀스 보장 |
| 컬럼 삭제·rename | 사실상 안 됨 | 명시적 가능 |
| 데이터 마이그레이션 | 불가능 | 가능 |
| 인스턴스 다중 시작 | 충돌 위험 | 락 기반 안전 |
| **데이터는 코드보다 오래 산다** 원칙 | 위배 | 부합 |
| 동기화 책임 | 어노테이션이 알아서 | 어노테이션 + `.sql` 둘 다 유지 |

핵심: **ddl-auto는 "스키마가 코드의 부산물"이라는 관점, Flyway는 "스키마가 데이터의 계약"이라는 관점**. 데이터가 코드보다 오래 살아남기 때문에 후자가 운영 시점에 정합.

## 검토한 대안

### Liquibase
- 회사 경험 있음. 차별 학습 가치 측면에서 사이드는 Flyway 선호
- 추상화·이식성 강하지만 사이드 프로젝트엔 MySQL 단일 → 이식성 가치 적음

### ddl-auto 유지
- 운영 시점에 마이그레이션 도구 없이 가는 건 비현실적
- 도메인 수가 늘어날수록 도입 비용 증가 → 지금 시점이 합리적

## 적용 방식

### 파일 구조
```
src/main/resources/db/migration/
  V1__init.sql              # 살아있는 도메인 4개 (disclosure, analysis_report, market_reaction, stock_price)
  V2__create_companies.sql  # 회사 마스터
```

### Spring Boot 통합
```properties
# application.properties
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
```

- 앱 시작 시 Flyway가 `flyway_schema_history` 조회 → 미적용 `V{n}` 순서대로 실행
- Hibernate가 이어서 `validate` 모드로 엔티티-스키마 일치 검증
- 불일치 시 시작 실패 → 회귀 즉시 감지

### Testcontainers
- 매 테스트마다 새 MySQL 컨테이너 → 빈 DB → Flyway 첫 실행 → 성공
- 별도 설정 없이 Spring Boot Flyway 자동 구성이 처리

## Liquibase 와의 차별점 — 이번 작업에서 직접 체감

| 항목 | Liquibase | Flyway |
|---|---|---|
| 변경 표현 | XML/YAML/SQL changeset (한 파일 안 여러 changeset) | `.sql` 파일 하나 = 변경 하나 |
| DB 추상화 | DB 독립 (XML → 각 DB 방언 변환) | SQL 그대로 (MySQL 기능 그대로) |
| 롤백 | 명시적 `rollback` 작성 가능 | undo 기능 유료 (Community 엔 없음) |
| 철학 | 추상화·이식성 | 단순성·SQL 친화 |

## 운영 룰

- **불변 원칙**: 한 번 적용된 `V{n}` 파일은 수정 금지. 체크섬 검증으로 변경 시 시작 실패.
- 변경 필요 시 **새 `V{n+1}__{설명}.sql` 추가**.
- 어노테이션과 `.sql` 동기화 책임은 PR 작성자에게. 컴파일 + 시작 시 validate가 자동 검증.

## 후속 작업 (남음)

- [ ] `src/test/resources/application.properties` 의 `ddl-auto=create-drop` 도 `validate` 로 통일 (Flyway 일관성)
- [ ] UUID v7 생성기 도입 (회사 마스터 영속화 단계에서)

## 참고

- 스펙: [docs/specs/company.md](../specs/company.md)
- 회사 도메인 작업 진행: 단계 0 (이 ADR) → 단계 1a (Company 엔티티) → 1b 프로필 컬럼 → 2~5
