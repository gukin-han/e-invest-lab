# 경계 네이밍 룰

경계 계층(interfaces, infra)의 클래스·패키지 이름을 정한다.

- 원칙: **소속·위치 정보는 패키지가, 역할 정보는 클래스 이름이 담당한다.** 같은 정보를 두 자리에 새기지 않는다.

## 1. 벤더는 패키지가 말한다

- 규칙
  - 외부 시스템 연동 코드는 벤더명 패키지에 둔다: `infrastructure.dart`. 전송 방식(`http`)이 아니라 벤더가 패키지 이름이다.
  - 그 패키지 안의 클래스 이름에는 벤더 접두사를 붙이지 않는다.
  - 벤더 스코프 패키지가 없는 곳(`global.config`)에서는 클래스 이름이 벤더를 말한다: `DartApiProperties`.
  - 도메인 객체에는 벤더명을 어떤 형태로도 넣지 않는다.
- 이유
  - 벤더는 역할이 아니라 소속이다. 소속을 클래스 이름에 새기면 패키지와 이중 기록이 되고, 이름은 길어지는데 역할 정보는 늘지 않는다.
  - 벤더 정보가 사라지면 안 되므로 "접두사 제거"가 아니라 "패키지로 이동"이다. 스택트레이스·import에는 전체 경로가 찍혀 정보 손실이 없다.
  - 두 번째 벤더가 생기면 `infra.krx` 가 옆에 나란히 선다.
- 예시
  - `disclosure.infrastructure.dart.BusinessReportAdapter` (벤더는 경로가, 역할은 이름이)
  - `company.infrastructure.dart.CompanyRegistryReader`

## 2. 포트 구현체 = {포트 대상}Adapter

- 규칙
  - 도메인 포트의 경계 구현체는 `{포트 대상}Adapter`: `BusinessReportAdapter` (implements `BusinessReportSource`), `CompanyRepositoryAdapter` (implements `CompanyRepository`).
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

## 5. 응답 해석은 응답 자신의 메서드로

- 규칙
  - status 매핑·선택·도메인 변환처럼 "그 응답을 우리 것으로 해석하는 지식"은 응답 record의 메서드로 둔다: `DisclosureSearchResponse.toFiling(corpCode)`.
  - 별도 매퍼 클래스는 해석이 상태를 가지거나(스트림 순회) 여러 응답에 걸칠 때만 만든다.
- 이유
  - 해석 지식이 서식 정의 옆에 붙어 응집된다. 매핑만 하는 중간 클래스는 어휘와 파일 수만 늘린다.
  - 단위 테스트는 JSON 문자열 → ObjectMapper → 해석 메서드로 HTTP 없이 가능하다.
