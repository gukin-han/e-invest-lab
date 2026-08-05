# 경계 네이밍 룰

경계 계층(interfaces, infra)의 클래스·패키지 이름을 정한다.

- 원칙: **소속·위치 정보는 패키지가, 역할 정보는 클래스 이름이 담당한다.** 같은 정보를 두 자리에 새기지 않는다.

## 1. infrastructure 하위 패키지 = 연동 대상 또는 책임

- 규칙
  - **상대가 고유명이면 대상명**: `infrastructure.dart` (DART 전자공시). 전송 방식(`http`)·접근 채널(`opendart`)이 아니라 상대 세계의 이름이다.
  - **상대가 고유명이 아니면 책임명**: `infrastructure.persistence` (저장). 나중에 생기면 `messaging`(발행), `cache`, `lock` 도 같은 축.
  - 고유명 패키지는 그 안의 지식이 실제로 그 상대 고유일 때만 쓴다. 저장소를 `mysql` 로 이름 짓지 않는 이유 — JPA 코드는 엔진 중립이라 패키지 전체가 MySQL 지식이라는 주장이 과대다.
  - 기술 이름은 클래스명에서 구분한다: `CompanyJpaRepository`, `CompanyJdbcRepository`. 같은 책임 아래 기술이 둘 이상일 때만 구분하고, 하나뿐이면 기술 단계 패키지를 만들지 않는다.
  - 대상명 패키지 안의 클래스에는 벤더 접두사를 붙이지 않는다. 벤더 스코프 패키지가 없는 곳(`global.config`)에서는 클래스 이름이 벤더를 말한다: `DartApiProperties`.
  - 도메인 객체에는 벤더명을 어떤 형태로도 넣지 않는다.
  - **엔티티는 domain 에 남는다.** JPA 엔티티가 곧 도메인 모델이므로(영속 모델 분리 안 함) `persistence` 에는 저장 구현(Repository 구현·쿼리)만 들어간다.
- 이유
  - 패키지가 답할 질문은 "어떤 기술을 쓰는가"가 아니라 "무엇과 연결되는가 / 어떤 외부 책임을 수행하는가"다.
  - 벤더는 역할이 아니라 소속이다. 소속을 클래스 이름에 새기면 패키지와 이중 기록이 된다. 반대로 책임명 패키지에서는 기술 구분이 클래스명의 몫이 된다.
  - 채널명(`http`, `opendart`)은 채널이 바뀌면 죽지만 상대명(`dart`)은 살아남는다.
  - 두 번째 상대·책임이 생기면 옆에 나란히 선다: `infrastructure.krx`, `infrastructure.messaging`.
- 예시
  - `disclosure.infrastructure.dart.BusinessReportSourceAdapter` (대상은 경로가, 역할은 이름이)
  - `disclosure.infrastructure.persistence.BusinessContentRepositoryAdapter` (책임은 경로가, 기술은 클래스가)

## 2. 포트 구현체 = {포트 대상}Adapter

- 규칙
  - 도메인 포트의 경계 구현체는 **`{포트명}Adapter`** — 포트 이름 전체에 기계적으로 Adapter 를 붙인다: `BusinessReportSourceAdapter` (implements `BusinessReportSource`), `CompanyRepositoryAdapter` (implements `CompanyRepository`). 접미사 일부(Source 등)를 떨구지 않는다 — 규칙이 둘로 갈라지고, 이름만으로 어떤 계약의 구현인지 읽을 수 없게 된다.
- 이유
  - Adapter는 포트-어댑터 아키텍처의 용어로, "도메인이 정한 계약과 외부 기술 사이의 변환기"라는 뜻이다. 이 룰 문서로 어휘를 고정했으므로 팀 안에서 정보를 전달하는 이름이다.
  - `~Impl` 은 무엇으로 구현했는지 말하지 않고, 두 번째 구현이 생기는 순간 파탄 난다. `~Client` 는 전송 메커니즘만 말하고 어떤 계약을 이행하는지 말하지 않는다.
  - 변환 없이 행위가 본체인 보조 클래스는 행위로 명명한다: `CompanyRegistryReader` (스트림 파싱).

## 3. 와이어 DTO = {자원·기능}{Request|Response}

- 규칙
  - 본체는 상대 API의 자원 이름을 따르되, 그 이름의 정보가가 없으면(`list.json`) 벤더가 부르는 기능명으로 보강한다: 공시검색 → `DisclosureSearchResponse`.
  - 방향은 `Request`/`Response`로 표시한다. `Dto`, `Payload`, `Message`, `ApiResponse` 접미사는 쓰지 않는다.
  - interfaces와 infra가 같은 접미사를 쓰는 중복은 허용한다.
- 이유
  - 경계 클래스는 전부 데이터 운반체다 → `Dto`는 정보가 0. `Payload`/`Message`는 방향을 잃는다. `ApiResponse`는 `global/web/ApiResponse`(우리 응답 봉투)와 정반대로 읽힌다.
  - "어느 경계인가"는 패키지가 이미 말한다. 의존 방향 룰(ArchUnit)이 interfaces와 infra의 상호 참조를 막으므로 두 계열의 Response가 한 파일에서 만나는 상황은 성립하지 않는다.

## 4. 와이어 필드는 두 겹: 이름 고정은 애노테이션, 가독성은 자바 이름

- 규칙
  - 와이어 필드명은 `@JsonProperty`로 고정하고, 자바 필드는 약자 없이 풀어쓴 번역을 쓴다.
  - 직렬화 애노테이션은 경계 DTO에만 허용한다. 도메인 객체에 붙는 순간 와이어 서식 변경이 도메인 수정을 강제한다.
- 이유
  - 벤더의 뭉개진 약자(`rcept_no`)를 코드 가독성 희생 없이 추적 가능하게 남긴다. 보존은 애노테이션이, 가독성은 자바 이름이 담당한다.

Good:
```java
record Item(
        @JsonProperty("rcept_no") String filingNumber,
        @JsonProperty("rcept_dt") String filedDate
) {}
```

## 5. 소스 포트는 원천 사실 record 를 반환한다

- 규칙
  - 외부 원천을 읽는 포트(`~Source`)의 반환·전달 타입은 **원천이 말한 사실만 담은 도메인 record** 다: `CompanyRegistryEntry`, `AnalystReportListing`.
  - 엔티티(저장 모델) 조립은 유스케이스가 한다. id 발급(`Ids.generate()`), `collectedAt` 같은 수집 메타는 저장이 결정된 시점에만 부여한다.
  - record 는 domain 패키지에 둔다 — 포트 계약의 일부이기 때문이다.
- 이유
  - 포트 계약은 "원천이 무엇을 말했는가"이고, 엔티티는 "우리가 무엇을 저장하기로 했는가"다. 두 결정의 시점이 다르다 — 스킵될 후보에 id 를 발급하는 것은 의미 오류다.
  - 어댑터가 엔티티를 조립하면 저장 정책(id 전략, 수집 시각)이 인프라로 새고, 어댑터 교체가 저장 의미를 바꿀 수 있게 된다.
- 예시
  - `CompanyRegistrySource.streamAll(Consumer<CompanyRegistryEntry>)` → 유스케이스의 `toCompany(entry)` 에서 id 발급
  - `AnalystReportSource.fetchListings(...)` → 유스케이스에서 `AnalystReport` 조립 + `collectedAt` 부여

## 6. 응답 해석은 응답 자신의 메서드로

- 규칙
  - status 매핑·선택·도메인 변환처럼 "그 응답을 우리 것으로 해석하는 지식"은 응답 record의 메서드로 둔다: `DisclosureSearchResponse.toFiling(corpCode)`.
  - 별도 매퍼 클래스는 해석이 상태를 가지거나(스트림 순회) 여러 응답에 걸칠 때만 만든다.
- 이유
  - 해석 지식이 서식 정의 옆에 붙어 응집된다. 매핑만 하는 중간 클래스는 어휘와 파일 수만 늘린다.
  - 단위 테스트는 JSON 문자열 → ObjectMapper → 해석 메서드로 HTTP 없이 가능하다.
