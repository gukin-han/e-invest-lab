# LLM Client 결정 기록

## Spring AI + ChatClient 채택
- **날짜**: 2026-02-12
- **상태**: **폐기 (superseded, 2026-08-08)** — 아래 "Offering 추출 클라이언트" 결정이 대체. 미구현 상태였으므로 코드 변경 없음. 감성 분석 구현 시에도 아래 방식(직접 HTTP + 포트)을 따른다.

### 컨텍스트
- 공시 본문에 대한 감성 분석(호재/악재) 필요
- LLM 프로바이더(OpenAI, Gemini 등) 교체 가능해야 함
- 구조화된 응답(JSON → record) 파싱 필요

### 후보 비교

| 방식 | 추상화 | 프로바이더 교체 | 비고 |
|---|---|---|---|
| **Spring AI ChatClient** | 프레임워크 레벨 | starter만 교체 | `.entity()` 구조화 출력 지원 |
| OpenAI SDK 직접 사용 | 없음 | 코드 전면 수정 | 프로바이더 종속 |
| LangChain4j | 라이브러리 레벨 | 설정 변경 | Spring 통합 미성숙 |

### 결정
- **Spring AI 2.0.0-M2** (`spring-ai-starter-model-openai`)
- `ChatClient.entity(Class)` → 구조화 출력으로 `AnalysisResult` record 직접 매핑
- domain 포트: `LlmClient` 인터페이스 → infrastructure에서 `SpringAiLlmClient`로 구현
- 프로바이더 교체: `spring-ai-starter-model-openai` → 다른 starter로 교체 + properties 변경

### 프롬프트 설계
- 입력: 기업명, 공시 제목, 본문 텍스트
- 출력: `AnalysisResult(sentiment, score, summary)`
  - sentiment: POSITIVE / NEUTRAL / NEGATIVE
  - score: -100 ~ +100
  - summary: 한국어 2-3문장

### 설정
```properties
spring.ai.openai.api-key=${OPENAI_API_KEY:}
spring.ai.openai.chat.options.model=gpt-4o-mini
spring.ai.openai.chat.options.temperature=0.3
```

---

## Offering 추출 클라이언트: 직접 HTTP + 모델 체인 에스컬레이션
- **날짜**: 2026-07-25 (제안) → **2026-08-08 채택** (연동 방식은 리뷰에서 직접 HTTP 로 조정, 상위 모델 gpt-5 확정)

### 컨텍스트
- stage 2a: 슬라이서 출력(회사당 6~21K자)에서 Offering 행 추출. 스키마는 우리가 고정하고 서식 해석만 LLM에 위임
- 추출 제1원칙(보고서 명시값만, 없으면 null) → 출력 스키마 강제 + 기계 검증이 가능해야 함
- 비용 무시 불가: 46개사 실험 후 전수(~2,600개사) 반복 실행이 전제
- 단일 모델·단일 벤더에 고정할 이유 없음 — 작업 난이도 분포가 넓음(정형 표 vs 산문 수치)
- **실측 근거** (2026-07-25, `gpt-5-mini`, few-shot 없이 규칙만): 삼성전자 33행·삼성생명 22행, 환각 0(스팟 체크 수치 전부 원문 실재), 연도 분리·당기만 비중·수주 행·단위 보존 정확. 비용 2건 $0.035. 발견된 흠: 합계 행 미필터, basis 어휘 표류, 종속회사 귀속 자리 없음(→ 스키마에 entity 추가)

### 결정 1 — 모델 전략: 설정 기반 체인 + 가드 트리거 에스컬레이션

```
슬라이스 → 1차 모델(gpt-5-mini) → 검증 가드 통과? ─ 예 → 저장
                                     └ 아니오 → 상위 모델 재시도 → 통과? ─ 예 → 저장
                                                                    └ 아니오 → 예외(기록)
```

- 1차: `gpt-5-mini` (실측 근거). 상위: `gpt-5` — 같은 벤더 상위 티어라 어댑터 하나로 체인 완성, 상위 호출은 가드 실패분에만 발생해 비용 상한이 작음
- 에스컬레이션 트리거 = 검증 가드(벤더 중립 행위 클래스): 수치 정규화 대조("3조 8,542억원" ↔ 38542 억원 — 단순 문자열 대조는 한국어 수 표기에서 깨짐), 합계·소계류 필터, 비중 합 검사. 스키마 준수 자체는 API가 강제하므로 가드 대상 아님
- 체인은 설정값(`application.yml` 목록) — 교체·연장이 배포 없이 끝남
- 에스컬레이션 비율 자체가 실험 데이터(1차 모델 통과율 축적)
- 비용: 46개사 1회 $1 미만, 전수 ~$45 수준. 추가 레버: 프롬프트 캐싱(few-shot 반복분), 전수 시 Batch API(전 요청 50% 할인, 지연 무관 작업에 적합)

### 결정 2 — 연동 방식: 직접 HTTP (기존 어댑터 패턴), 벤더별 어댑터

| 방식 | 스키마 강제 보장 | 추상화 | 판정 |
|---|---|---|---|
| Spring AI ChatClient | `.entity()`가 네이티브 strict json_schema 를 쓰는지 버전 의존 — 와이어 불투명 | 프레임워크 (포트 위에 한 겹 더) | 기각 |
| 공식 벤더 SDK (`com.openai:openai-java`) | 재현 가능하나 요청이 SDK 내부에서 조립 — 의존성 추가 | 없음 — 포트가 유일한 추상화 | 기각 (리뷰에서 조정) |
| **직접 HTTP** (`java.net.http` + 와이어 DTO) | 실험에서 검증한 요청 본문(strict json_schema)을 문자 단위로 통제 | 없음 | **채택** |

- 리뷰(2026-08-08)에서 SDK → 직접 HTTP 로 조정. 근거: 이 프로젝트의 외부 연동 4개(DART·한경·금융위)가 전부 `HttpClient + 와이어 DTO + ObjectMapper` 패턴 — 같은 패턴이면 재시도·파싱은 이미 갖춰진 부품이고, SDK 는 의존성 1개와 요청 조립 불투명성만 더한다. 단발 non-streaming JSON 호출 1종이라 SDK 의 부가 기능(스트리밍·타입 빌더)을 쓸 일이 없음
- 벤더 경계는 이미 도메인 포트가 담당 — Spring AI의 "프로바이더 스왑" 이점은 포트 뒤에서 이중 추상화가 됨
- 이 유스케이스는 추출 정확도가 전부 → 실측으로 검증한 와이어 형태를 정확히 재현하는 쪽 우선
- 벤더 추가 = `infrastructure.{vendor}` 어댑터 추가 (boundary-naming 룰의 대상명 패키지). 실험으로 교차 벤더 이식성 확인됨(같은 스키마·규칙이 OpenAI에서 그대로 작동)
- **체인 오케스트레이션은 application** — 체인이 벤더를 넘을 수 있으므로 어댑터 안이 아니라 유스케이스가 포트 구현 목록을 순서대로 시도 (DART 정정본 폴백과 동형)

### 결정 3 — 구조화 출력: strict json_schema + few-shot

- OpenAI `response_format: json_schema (strict)` — 필드 누락·타입 오류·잡담 혼입 원천 차단 (실험 검증)
- 스키마 = 스펙의 확정 Offering 스키마(entity 포함) → Java record 와 1:1
- 시스템 프롬프트 = 추출 규칙 8개(명시값만·행=사실 하나·segment 명시 배정만·qualifier·명시 단위·연도 분리·customers·합계 제외)
- few-shot = 수동 시뮬레이션 5개사 기대 출력 (합계 미필터 재발 방지가 1차 목적)
- basis 어휘 표류는 후처리 정규화로 흡수

### 기존 결정("Spring AI + ChatClient 채택")과의 관계

2026-02-12 결정은 감성 분석 맥락이었고 미구현 상태. 본 결정 채택(2026-08-08)으로 LLM 연동 방식 전반을 대체(supersede)했으며, 감성 분석 구현 시에도 동일 방식(직접 HTTP + 포트)을 적용한다.
