# e-invest-lab

> 한국 상장사의 공시·컨센서스·시세 데이터를 자동 수집해 "기업의 변화"를 탐지하는 개인 학습·연구 프로젝트

사람이 매일 공시와 증권사 리포트를 읽지 않아도 되도록, 원천 데이터를 주기적으로 수집해 정형화하고 그 위에서 변화 신호(EPS 컨센서스 리비전, 주식수 변화, 밸류에이션)를 계산한다. 원칙은 두 가지다 — **수집 시점에는 해석하지 않고 원본만 쌓는다**, **LLM 은 추출(데이터화)에만 쓰고 신호 판정에는 쓰지 않는다**.

## 무엇을 수집하고 무엇을 만드는가

| 도메인 | 원천 | 수집물 | 그 위의 후작업 |
|---|---|---|---|
| `company` | DART corpCode | 전 상장사 등록부, 섹터 분류 | 다른 도메인의 종목 조인 기준 |
| `disclosure` | DART 공시 원문 | 사업의 내용(사업보고서), 제품·매출 구성(Offering, LLM 추출) | 기업 설명력, 진짜 Peer 정의 |
| `research` | 한경 컨센서스 (스크래핑) | 애널리스트 리포트 목록·PDF, 요약표 EPS 추정치 (규칙 파서) | EPS 컨센서스·리비전 추이, trailing/forward PER, 슬랙 알림 |
| `market` | 금융위원회 시세 API | 전 종목 일별 시세 5년, 상장주식수·시가총액 | 주식수 변화 랭킹, 감자·기계적 증가 필터 |

수집은 스케줄러가 매일 돌리고(등록부 01:00, 리포트 체인 18:00, 시세 18:10 KST), 같은 유스케이스를 `/internal/...` 엔드포인트로 수동 백필할 수 있다. 조회 API 는 `/api/stocks/...` 아래에 있다.

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

- 의존 방향은 `interfaces → application → domain ← infrastructure`. ArchUnit 테스트(`architecture/`)가 강제한다.
- 도메인끼리는 엔티티를 직접 참조하지 않고 ID 로만 참조한다. 도메인 간 연동은 아웃박스 이벤트로 — 예: `research` 가 EPS 추출 성공을 `EPS_EXTRACTED` 로 발행하면 릴레이가 슬랙 알림 핸들러에 위임한다.
- 스키마 진본은 `src/main/resources/db/migration/V{n}__{설명}.sql` (Flyway). 각 파일 머리말에 설계 근거를 남긴다.

읽는 순서를 추천하면 — [docs/specs/product-direction.md](docs/specs/product-direction.md)(왜 이걸 만드는가) → [docs/rules/architecture.md](docs/rules/architecture.md)(어떻게 나누는가) → 관심 도메인의 spec.

## 문서

- `docs/specs/` — 도메인별 스펙과 실측 기록. 무엇을 수집하고 어떤 판단을 했는지, 실측 수치까지.
- `docs/rules/` — 컨벤션. 아키텍처, 경계 네이밍, 수집기, 예외, 트랜잭션, 시간 처리 등.
- `docs/adr/` — 기술 결정 기록. Flyway, HTTP 클라이언트, LLM 클라이언트, [트랜잭셔널 아웃박스](docs/adr/transactional-outbox.md) 등.

## 기술 아티클

- [HikariCP 커넥션 고갈 추적기](https://gukin-han.tistory.com/67) — Virtual Thread 700개가 풀 10개에 막힌 문제. HikariCP 소스 분석으로 원인을 파악하고 트랜잭션 경계 분리로 해결.
- [Virtual Thread 도입](https://gukin-han.tistory.com/55) — Virtual Thread 도입 배경과 구조 변경 과정.

## 실험 보고서

- [스트리밍 메모리 footprint 측정 (V3)](docs/adr/streaming-memory-experiment/README.md) — DART corpCode.xml(30MB ~ 3GB) 스트리밍 파싱 + MySQL upsert 흐름의 메모리 footprint를 5개 축으로 sweep. 정량 결과: `footprint ≈ 0.0025MB/row × batch + 2.2MB(JDBC)`. 100배 큰 합성 zip(3GB XML, 11.8M rows)도 -Xmx64m에서 처리됨을 확인.

## 기술 스택

- **Backend**: Java 21, Spring Boot 4, Spring Data JPA, Virtual Thread
- **DB**: MySQL 8, Flyway, HikariCP
- **외부 연동**: DART Open API, 한경 컨센서스, 금융위원회 시세 API, OpenAI API, Slack Incoming Webhook
- **테스트**: JUnit 5, Testcontainers(MySQL), ArchUnit

## 로컬 실행

사전 준비: Docker, JDK 21, `pdftotext`(poppler — EPS 파서가 사용).

```bash
cp .env.example .env      # 키 채우기 (아래 표)
docker compose up -d      # MySQL (.env 자동 인식)
set -a; source .env; set +a
./gradlew bootRun         # http://localhost:8080
```

`bootRun` 은 `.env` 를 스스로 읽지 않으므로 셸에 export 해 둔다 (`set -a; source .env`).

| 환경변수 | 용도 | 없으면 |
|---|---|---|
| `DB_PASSWORD` | MySQL root 비밀번호 | 기동 실패 |
| `DART_API_KEY` | 공시·등록부 수집 | company/disclosure 수집 실패 |
| `STOCK_PRICE_API_KEY` | 일별 시세 수집 | market 수집 실패 |
| `OPENAI_API_KEY` | Offering LLM 추출 | offering 추출만 실패 |
| `SLACK_WEBHOOK_URL` | EPS 추출 슬랙 알림 | 알림 행이 재시도 백오프 후 DEAD 로 남음 (다른 기능 무관) |

슬랙 알림만 바로 보려면: `.env` 에 `SLACK_WEBHOOK_URL` 을 넣고 기동한 뒤 `POST /internal/analyst-reports/collect` → `download-pdfs` → `extract-eps` 순으로 호출하면, 추출 성공 건마다 아웃박스에 이벤트가 쌓이고 릴레이(1분 주기)가 웹훅으로 보낸다. 상태는 `outbox_events` 테이블에서 확인.

```bash
./gradlew test            # 단위 + ArchUnit + Testcontainers 통합 테스트
```

## 배포 (홈서버)

`main` 에 푸시하면 `.github/workflows/deploy.yml` 이 돈다: GitHub 러너에서 테스트 → 통과하면 홈서버의 self-hosted 러너(라벨 `einvestlab`)가 소스를 `~/e-invest-lab` 에 동기화하고 `docker compose --profile app up -d --build` → `/api/stocks/recently-covered` 헬스체크. 홈서버는 LAN 안이라 GitHub 가 들어오는 대신 러너가 당겨오는 구조.

- 시크릿은 GitHub Actions Secrets(`DB_PASSWORD`, `DART_API_KEY`, `STOCK_PRICE_API_KEY`, `OPENAI_API_KEY`, `SLACK_WEBHOOK_URL`) → 배포 시 서버 `.env` 로 기록. 이미지 태그는 커밋 SHA 7자리.
- 데이터는 볼륨: MySQL `mysql-data`, PDF `~/e-invest-lab/data/analyst-report-pdfs`. 소스 동기화는 `data`·`.env` 를 건드리지 않는다.
- 로컬에서 같은 앱을 돌리면 슬랙 알림이 두 번 간다 — 로컬 `.env` 의 `SLACK_WEBHOOK_URL` 은 비워 둔다.
- 수동 확인: `ssh homeserver 'cd ~/e-invest-lab && docker compose --profile app ps && docker compose --profile app logs --tail 50 app'`
