# e-invest-lab

**매일 나오는 증권사 리포트를 사람이 읽지 않아도, 종목별 EPS 추정치가 어떻게 바뀌었는지 슬랙으로 받는다.** 한국 상장사의 공시·컨센서스·시세를 자동 수집해 정형화하고, 그 위에서 "기업의 변화" 신호(EPS 리비전, 컨센서스 갭, 주식수 변화, 밸류에이션)를 계산하는 프로젝트. 홈서버에서 24시간 돌고 있다.

```
[하향] HMM (011200) · LS증권 · 8/14
2026E EPS  2,235
　　직전 2,424 → -7.8%
　　컨센서스 2,051.5 대비 +8.9%
목표주가  30,000 유지 · Buy
현재가  21,050 (8/13) · PER 9.4배 (LS증권 2026E 기준)

EPS 경로
2024A  4,222
2025A  1,951  -53.8%
2026E  2,235  +14.6%
2027E  1,142  -48.9%
2028E  1,024  -10.3%
리포트: https://consensus.hankyung.com/analysis/downpdf?report_idx=651717
```

원칙 두 가지 — **수집 시점에는 해석하지 않고 원본만 쌓는다**, **LLM 은 추출(데이터화)에만 쓰고 신호 판정에는 쓰지 않는다**. 통계·신호는 전부 저장된 원본 위의 조회 시 계산이라, 계산법이 바뀌어도 재적재가 없다.

## 무엇을 수집하고 무엇을 만드는가

| 도메인 | 원천 | 수집물 | 그 위의 후작업 |
|---|---|---|---|
| `research` | 한경 컨센서스 (스크래핑) | 애널리스트 리포트 목록·PDF, 요약표 EPS 추정치 (규칙 파서) | EPS 컨센서스·리비전 추이, forward PER, 리비전 시그널 슬랙 알림 |
| `market` | 금융위원회 시세 API | 전 종목 일별 시세 5년, 상장주식수·시가총액 | 주식수 변화 랭킹, 감자·기계적 증가 필터 |
| `disclosure` | DART 공시 원문 | 사업의 내용(사업보고서), 제품·매출 구성(Offering, LLM 추출) | 기업 설명력, 같은 제품군 기준 Peer |
| `company` | DART corpCode | 전 법인 등록부, 섹터 분류 | 다른 도메인의 종목 조인 기준 |

수집은 스케줄러가 돌린다 — 리포트는 평일 07~16시 10분마다 + 매일 18시 안전망, 시세 18:10, 등록부 01:00 (KST). 같은 유스케이스를 `/internal/...` 로 수동 실행할 수 있고, 조회 API 는 `/api/stocks/...` 아래에 있다.

## 주요 설계 결정

각 결정은 자체 트레이드오프로 정당화한다. 근거는 링크한 문서에 있다.

1. **트랜잭셔널 아웃박스 + 폴링 릴레이** — 추정치 저장과 이벤트 행이 같은 트랜잭션. 유스케이스에서 직접 슬랙 호출(원자성 없음), Spring 이벤트(프로세스 죽으면 유실, 리스너 실패가 데이터에 안 남음), 브로커(운영 의존성) 대비 선택. 재시도 정책은 `next_attempt_at` 컬럼으로 데이터에 두어 릴레이 한 곳에 중앙화, 어댑터는 재시도하지 않는다. → [ADR](docs/adr/transactional-outbox.md)
2. **정형 행은 영구, PDF 는 롤링 1년** — 리포트 한 건은 몇백 바이트라 지워서 얻는 게 없고 리비전·컨센서스·모멘텀이 전부 이력 위에서 나온다. "낡음"은 조회 조건(컨센서스 6개월 유효기간)으로 처리하지 삭제로 처리하지 않는다. PDF 만 재파싱 가치가 남는 1년 보존. → [스펙](docs/specs/analyst-report.md)
3. **LLM 은 추출에만** — 사업보고서의 제품·매출 구성처럼 서식이 무한한 곳에만 LLM 을 쓰고(가드 + 재검증, 95.7%), 요약표 EPS 처럼 관례가 강한 숫자 표는 규칙 파서가 1차(비용 0, 환각 불가). 신호 판정은 결정적 SQL. → [스펙](docs/specs/product-direction.md), [ADR](docs/adr/llm-client.md)
4. **Flyway** — 스키마는 코드의 부산물이 아니라 데이터의 계약. 데이터는 코드보다 오래 산다. 회사에서 Liquibase 를 쓰므로 사이드에서는 의도적으로 다른 도구. → [ADR](docs/adr/flyway-migration.md)
5. **도메인 패키지 + `support` / `global` 3분류, ArchUnit 강제** — 최상위는 업무 지도여야 한다. 자기 테이블·스케줄을 가진 기술 모듈(아웃박스)은 `global` 이 아니라 `support`. 의존 방향과 엔티티 위치는 테스트가 막는다. → [규칙](docs/rules/architecture.md)
6. **이력 백필을 하지 않기로** — 원천의 `robots.txt` 가 전체 크롤링 금지. 일일 폴링은 개인 분석 한정의 선 안에 두고, 수천 건 백필은 그 선을 넘는다고 판단. 알림 품질을 4개월 양보하고 자연 축적. → [스펙](docs/specs/analyst-report.md)
7. **규칙 파서 2전략 + 가드, 실측으로 올림** — 세로형·가로형 요약표, 연도 범위·중복 가드. 라이브 실측을 반복해 추출률 49% → 82% → 90%. 실패 26건은 롱테일로 남기고 LLM 폴백은 결손이 비치명적이라 보류. → [스펙](docs/specs/analyst-report.md)
8. **HikariCP 커넥션 고갈** — Virtual Thread 700개가 풀 10개에 막힌 장애를 HikariCP 소스로 추적, 트랜잭션 경계 분리로 해결. → [아티클](https://gukin-han.tistory.com/67)

## 실측 숫자

- EPS 추출률 90.2% (597건: 추출 538 / 표 없음 33 / 실패 26), 추정치 2,502행, 커버 226종목 — 파서 3회 개정의 결과
- Offering(제품·매출 구성) LLM 추출 95.7% (44개사, 1,017행) — 프롬프트 규칙 3차 개정
- 일별 시세 3,388,756행 (전 종목 5년), 등록부 118,712 법인
- DART corpCode 스트리밍 파싱: 3GB XML(11.8M rows) 을 `-Xmx64m` 에서 처리, `footprint ≈ 0.0025MB/row × batch + 2.2MB` → [실험 보고서](docs/adr/streaming-memory-experiment/README.md)
- 슬랙 알림 첫날: 아웃박스 36건 SENT / 실패 0 / DEAD 0

## 운영

- **배포**: `main` 푸시 → GitHub 러너에서 전체 테스트 → 홈서버 self-hosted 러너가 `docker compose --profile app up -d --build` → 헬스체크. 이미지 태그는 커밋 SHA. → [워크플로](.github/workflows/deploy.yml)
- **스케줄**: 리포트 폴링 두 케이던스(촘촘 10분 / 안전망 일 1회), 아웃박스 릴레이 1분, 시세·등록부 일 1회. 실행이 겹치면 다음 회차는 건너뛴다.
- **실패 처리**: 원천 실패는 건별 격리 + 다음 실행 멱등 재시도. 알림 실패는 백오프 1분 → 5분 → 30분 → 2시간 → 12시간, 6회 후 DEAD + 마지막 오류 기록. HTTP 는 연결 3초·응답 10~30초 타임아웃.
- **보존**: PDF 롤링 1년 GC (파일 삭제 → DB 마킹, 멱등, 삭제 상태를 미다운로드와 구분).
- **테스트**: 단위 + ArchUnit + Testcontainers(MySQL) 통합, 실물 HTML/PDF 픽스처 회귀. 규칙은 known-violation 으로 검증한다.
- **글**: [HikariCP 커넥션 고갈 추적기](https://gukin-han.tistory.com/67), [Virtual Thread 도입](https://gukin-han.tistory.com/55)

## 코드 구조

```
dev.gukin.einvestlab
├── global/          무상태 헬퍼·설정 (config, id, web)
├── support/         업무 언어 없는 기술 모듈 — 자기 테이블·스케줄을 가짐
│   └── outbox/      트랜잭셔널 아웃박스 + 폴링 릴레이
├── company/
├── disclosure/
├── research/        각 도메인은 같은 4계층
└── market/            domain        엔티티(=도메인 모델), 포트 인터페이스, 도메인 예외
                       application   {Behavior}UseCase — 스케줄러·컨트롤러가 재사용
                       infrastructure 포트 어댑터 (persistence / 외부 API / pdf / slack …)
                       interfaces    web 컨트롤러, scheduler
```

- 의존 방향 `interfaces → application → domain ← infrastructure`, ArchUnit 이 강제. 도메인끼리는 ID 로만 참조하고 연동은 아웃박스 이벤트로.
- 스키마 진본은 `src/main/resources/db/migration/V{n}__{설명}.sql`. 각 파일 머리말에 설계 근거.
- 읽는 순서: [product-direction.md](docs/specs/product-direction.md)(왜) → [rules/architecture.md](docs/rules/architecture.md)(어떻게 나누나) → 관심 도메인 spec → 결정은 `docs/adr/`.

## 문서

- `docs/specs/` — 도메인별 스펙과 실측 기록. 무엇을 수집하고 어떤 판단을 했는지, 숫자까지.
- `docs/rules/` — 컨벤션. 아키텍처, 경계 네이밍, 수집기, 예외, 트랜잭션, 시간 처리 등.
- `docs/adr/` — 기술 결정 기록.

## 기술 스택

Java 21, Spring Boot 4 (Spring Data JPA, Virtual Thread), MySQL 8, Flyway, HikariCP, Docker Compose, GitHub Actions. 외부 연동: DART Open API, 한경 컨센서스, 금융위원회 시세 API, OpenAI API, Slack Incoming Webhook. 테스트: JUnit 5, Testcontainers, ArchUnit.

## 로컬 실행

사전 준비: Docker, JDK 21, `pdftotext`(poppler — EPS 파서가 사용).

```bash
cp .env.example .env      # 키 채우기 (아래 표)
docker compose up -d      # MySQL (.env 자동 인식)
set -a; source .env; set +a
./gradlew bootRun         # http://localhost:8080
./gradlew test            # 단위 + ArchUnit + Testcontainers 통합 테스트
```

`bootRun` 은 `.env` 를 스스로 읽지 않으므로 셸에 export 해 둔다.

| 환경변수 | 용도 | 없으면 |
|---|---|---|
| `DB_PASSWORD` | MySQL root 비밀번호 | 기동 실패 |
| `DART_API_KEY` | 공시·등록부 수집 | company/disclosure 수집 실패 |
| `STOCK_PRICE_API_KEY` | 일별 시세 수집 | market 수집 실패 |
| `OPENAI_API_KEY` | Offering LLM 추출 | offering 추출만 실패 |
| `SLACK_WEBHOOK_URL` | EPS 알림 | 알림 행이 백오프 후 DEAD 로 남음 (다른 기능 무관). 로컬에서는 비워 둔다 — 서버와 이중 알림 |

## 배포 (홈서버)

`main` 푸시 → `.github/workflows/deploy.yml`: GitHub 러너 테스트 → 홈서버 self-hosted 러너(라벨 `einvestlab`)가 소스를 `~/e-invest-lab` 에 동기화하고 `docker compose --profile app up -d --build` → `/api/stocks/recently-covered` 헬스체크. 홈서버는 LAN 안이라 GitHub 가 들어오는 대신 러너가 당겨온다.

- 시크릿은 GitHub Actions Secrets → 배포 시 서버 `.env`. 데이터는 볼륨(MySQL `mysql-data`, PDF `data/analyst-report-pdfs`) — 소스 동기화가 건드리지 않는다.
- 확인: `ssh homeserver 'cd ~/e-invest-lab && docker compose --profile app ps && docker compose --profile app logs --tail 50 app'`

## 만든 방식

구현은 AI(Claude Code)와 페어링했다. 요구사항 정의, 도메인·패키지 구조, 각 설계 결정과 그 트레이드오프, 리뷰와 검증은 내가 했고, 코드는 그 결정을 따라 함께 썼다. 위 "주요 설계 결정"은 전부 내가 설명할 수 있는 것만 적었다.
