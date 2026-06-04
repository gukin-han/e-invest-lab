# CompanyRegistrySource — DART 회사 등록부 스트리밍 공급원

> 단계 2 — V3 실험으로 검증된 스트리밍 파싱 로직을 운영 코드로 승격.
> 관련: `docs/specs/company.md` (도메인 spec), `docs/adr/corpcode-streaming-parsing.md` (스트리밍 결정), `docs/adr/http-client.md` (JDK HttpClient 결정)

## 목적

DART `/api/corpCode.xml` 호출 → 3.6MB zip → 30MB XML → 117k 회사 행을 **메모리 누적 없이** 호출자에게 흘려보냄.

이미 V3 실험에서 검증됨:
- 입력 크기와 무관한 일정 메모리 (3GB 입력 → 64MB 힙)
- batch 사이즈만 정거장 크기 결정 (~2MB 단가)
- 정거장 체인: HttpResponse → ZipInputStream → XMLStreamReader → 도메인 row

단계 2 의 가치는 **그 검증된 흐름을 어댑터로 굳혀서 호출자 (단계 3 동기화 서비스) 가 쓰기 좋게 만드는 것**.

## 위치

```
company/
├── domain/
│   ├── Company.java
│   └── CompanyRegistrySource.java
└── infra/
    ├── db/
    │   ├── CompanyJpaRepository.java
    │   └── CompanyRepositoryAdapter.java
    └── http/
        ├── DartCompanyRegistryAdapter.java (DART HTTP 어댑터)
        ├── DartCompanyRegistryReader.java  (corpCode zip + XML 스트리밍 reader)
        ├── DartApiProperties.java        (DART API 접속 설정)
        └── DartClientException.java      (DART 클라이언트 처리 예외)
```

`CompanyRegistrySource` 는 호출자 관점에서 "회사 등록부를 공급받는다"는 의도를 드러내는 도메인 포트다. DART 출처는 구현체인 `DartCompanyRegistryAdapter` 이름에만 둔다.

## 시그니처 결정

```java
public interface CompanyRegistrySource {

    /** DART 응답을 회사 단위로 스트리밍. 호출자가 한 행씩 처리. */
    void streamAll(Consumer<Company> handler);
}
```

### 대안과 트레이드오프

이름: `streamAll` 대 `fetchAll` 대 `forEach`

선택지 비교 (구현 비용 무시, 의도 명료성만):

대안 A — `void streamAll(Consumer<Company>)`
- 호출자가 한 행씩 받음. 어댑터가 응답 자원 (HttpResponse/Zip/StAX) 의 close 책임 가짐.
- 호출자 코드 단순: `client.streamAll(company -> { ... });`
- 테스트 단순: 합성 zip 만든 후 `streamAll(rows::add)` 식

대안 B — `Stream<Company> stream()` (try-with-resources 필수)
- 호출자가 표준 Stream API 활용 가능 (filter, map, batch 처리)
- close 책임이 호출자로 이동 — try-with-resources 누락 시 자원 누수
- Spring Data 흐름 (Stream + Transactional) 과 자연스러움

대안 C — `Iterable<Company>` (또는 Iterator)
- A 와 B 중간. 표준 for-each 가능. close 책임 어색함 (Iterator 가 자원 보유).

추천: **A**. 자원 close 책임이 어댑터 안에 갇혀 누수 위험 0. Stream API 가 필요해지면 그때 B 로 변형 가능 (호출자가 `Collectors.batch` 같은 묶음 처리 필요해질 때).

## 정규화 위치 결정

DART 원형과 도메인 사이에 어디서 정규화하는가:

선택지 비교:

대안 A — 어댑터 안에서 직접 `Company` 발급
- 한 곳에서 끝. 호출자 단순.
- `Company` 가 빌더 노출이라 `Company.builder()...build()` 호출 가능. ID 는 어떻게? 어댑터가 `Ids.generate()` 호출 — 명시 주입 컨벤션과 정합.
- 테스트는 합성 zip → `Company` 검증 (필드 + ID 발급)

대안 B — 중간 record `CorpCodeRow` 발급, 호출자가 매핑
- 어댑터는 파싱만, 매핑은 application 레이어.
- 정규화 (stock_code 공백→null 등) 위치 모호.

추천: **A**. 정규화 규칙은 어댑터 안에 격리. 호출자 (동기화 서비스) 는 `Company` 만 받음.

다만 raw 추적이 필요하면 (단계 3 의 modify_date 비교 등) 어떤 식으로 노출할지 검토 필요. 현재 안 — `Company` 가 `registryModifiedDate` 를 이미 가지므로 충분.

## 정규화 규칙 (spec company.md 표 참조)

어댑터가 적용:
- `stock_code` 공백 1개 → null
- `corp_eng_name` (xml 필드명) → `englishName` (도메인 필드명)
- `modify_date` `YYYYMMDD` String → `LocalDate`
- ID 발급: `Ids.generate()`

## HTTP 호출

JDK `HttpClient` (RestClient 아님 — ADR 결정):
```java
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create(baseUrl + "/corpCode.xml?crtfc_key=" + apiKey))
    .GET()
    .build();
HttpResponse<InputStream> response =
    httpClient.send(request, BodyHandlers.ofInputStream());
```

응답 body 가 `InputStream` → 그대로 ZipInputStream 으로 감쌈 → StAX 로 파싱.

설정 의존:
- `dart.api.base-url`
- `dart.api.key`

(application.yml 의 dart 블록 — 회사 도메인 청소 시 같이 빠졌으므로 단계 2 진입 시 다시 추가)

## 에러 처리

DART 응답 케이스:
- 200 + zip body — 정상
- 200 + 텍스트 body (error JSON) — 인증 실패 등. status code 만으론 구분 불가
- 4xx / 5xx — 네트워크/서버 에러

처리 위치 결정:
- 어댑터 안에서 `DartClientException` 같은 커스텀 예외로 변환 (호출자가 보기 좋음)
- 또는 그냥 `IOException` / `XMLStreamException` 흘려 보냄

추천: 커스텀 예외 변환. `DartClientException` 으로 HTTP/API 오류뿐 아니라 응답 스트림, 압축, XML 파싱 실패까지 포괄한다.

재시도: 단계 2 범위 밖. 단계 3 또는 단계 4 에서 결정 (DART 4만 건/일 한도 고려).

## 테스트 전략 (testing.md 룰 적용)

세 종류:

종류 1 — **`DartCompanyRegistryReaderUnitTest`** (`infra/http`)
검증 동작:
- `stock_code = " "` (공백 1개) 입력 시 `Company.stockCode == null`
- `corp_eng_name` 입력 시 `Company.englishName` 으로 매핑
- `modify_date = "20251201"` 입력 시 `LocalDate(2025,12,1)` 변환
- 매 매핑마다 `Company.id` 가 발급됨 (UUID v7)

종류 2 — **`DartCompanyRegistryReaderFailureUnitTest`** (`infra/http`)
검증 동작:
- 비-zip 응답 body → `DartClientException`
- xml entry 없는 zip → `DartClientException`
- 손상된 xml → `DartClientException`

종류 3 — **`DartCompanyRegistryFixtureSmokeTest`** (`infra/http`)
- src/test/resources/fixtures/company/dart-corp-code.zip (실제 응답 1회분, 약 3MB)
- reader 가 100k+ 행 전체 처리 + 알려진 회사 포함 검증 (삼성전자 등)
- 메모리 회귀 감지 옵션: V3 의 heap 모니터 코드 재사용

V3 의 `CorpCodeFetchSmokeTest`, `V2`, `V3` 등 실험 파일은 운영 코드 승격 후 삭제 또는 운영 스모크 테스트로 흡수.

## 결정해야 할 것 (사용자)

(1) 시그니처 — A (`void streamAll(Consumer<Company>)`) 추천. 다른 안 선호하면 알려주세요.
(2) 정규화 위치 — A (어댑터 안에서 Company 직접 발급) 추천.
(3) 에러 변환 — `DartClientException` 도입. 위치는 `company/infra/http/DartClientException.java` (글로벌 `global/error/` 가 아닌 도메인 안. 회사 도메인 외엔 DART 안 씀).
(4) fixture zip 어떻게 마련 — 실제 DART 호출 한 번 해서 받기 / V3 실험에서 사용한 파일 재사용. V3 가 이미 fixture 사용 중이면 그대로 옮김.
(5) `CorpCodeRow` 중간 record 정말 안 둘지 확인 (안 둠 추천. 어댑터 안에서 inline 파싱).

## 의도적 미포함 (다른 단계)

- 배치 buffer + upsert 흐름: 단계 3 `CompanyRegistrySyncService`
- modify_date 증분 갱신 (stale 판정): 단계 3
- 새벽 1시 cron: 단계 4 `CompanyRegistryScheduler`
- 회사 프로필 (company.json) 호출: 단계 1b (별도)
- 재시도: 단계 3/4 중 결정

## 다음 첫 한 발 (사용자 확정 후)

위 (1)~(5) 결정 받으면:
1. `DartCompanyRegistryReaderUnitTest` 작성 → 합성 zip 파싱 검증
2. `DartCompanyRegistryReaderFailureUnitTest` 작성 → 응답 형식 오류 검증
3. `DartCompanyRegistryFixtureSmokeTest` 작성 → 실제 fixture zip 회귀 검증
4. 단계 2 완료, 단계 3 진입
