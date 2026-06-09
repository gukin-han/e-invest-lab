# DTO 룰

계층 경계를 넘나드는 데이터 묶음(DTO)을 **어디에 두고, 어떤 이름으로, 어디까지 흐르게 할지**를 정한다.
패키지 평탄화(`아키텍처 규칙 4`)와 외부 어댑터 네이밍(`아키텍처 규칙 8`)을 DTO에 적용한 세부 규칙이다.

## 1. DTO는 계층 경계의 데이터 모양일 뿐, 도메인 모델이 아니다

- 규칙
  - DTO는 한 계층 경계에서 데이터를 받거나 내보내기 위한 모양 객체다.
  - 비즈니스 규칙이나 불변식을 DTO에 두지 않는다.
  - 도메인 규칙은 Entity(도메인 모델)에 둔다.
- 이유
  - DTO에 로직이 붙으면 도메인 규칙이 경계 객체로 새어 나간다.
  - 경계 모양과 도메인 모델을 분리해야 외부 포맷 변경이 도메인까지 번지지 않는다.

## 2. 계층별로 DTO의 출처와 방향이 다르다

- 규칙
  - 계층마다 DTO의 역할이 다르므로 위치와 이름을 다르게 정한다.

| 계층 | DTO 종류 | 출처/방향 | 이름 형식 |
|---|---|---|---|
| `interfaces` | Request / Response | 우리 API의 입력·출력 | `{Action}{Resource}Request`, `{Resource}Response` |
| `infra/http` | 외부 응답 | 외부 공급원(DART 등) 원문 | `{Provider}{Resource}Response` |
| `application` | Command / Result | 유스케이스 입력·출력 | `{Behavior}Command`, `{Behavior}Result` |
| `domain` | 없음 | — | Entity가 모델 |

- 이유
  - "우리가 노출하는 모양", "외부가 주는 모양", "유스케이스가 주고받는 모양"은 서로 다른 변경 이유를 가진다.
  - 출처가 이름에 드러나면 어떤 변경이 어느 DTO를 흔드는지 바로 보인다.

## 3. DTO도 평탄하게 두고 `dto` 하위 폴더를 만들지 않는다

- 규칙
  - Request/Response/Command/Result는 그것을 쓰는 계층 패키지에 평탄하게 둔다.
  - `dto`, `model`, `api`, `payload` 같은 하위 폴더를 만들지 않는다.
  - `interfaces`, `infra/http`, `application` 바로 아래에 둔다.
- 이유
  - 작은 도메인에서 DTO 전용 폴더는 탐색 비용만 늘린다(`아키텍처 규칙 4`와 동일 근거).
  - 같은 계층에서 쓰는 클래스끼리 한 곳에 모인다.

Bad:
```text
company
├── interfaces
│   └── dto
│       ├── CompanyResponse.java
│       └── CompanySyncRequest.java
└── infra
    └── http
        └── dto
            └── DartCompanyProfileResponse.java
```

Good:
```text
company
├── interfaces
│   ├── CompanyController.java
│   ├── CompanyResponse.java
│   └── CompanyRegistrySyncRequest.java
└── infra
    └── http
        ├── DartCompanyRegistryAdapter.java
        └── DartCompanyProfileResponse.java
```

## 4. 우리 API의 입출력은 interfaces에 Request / Response로 둔다

- 규칙
  - 우리가 노출하는 HTTP 입력: `{Action}{Resource}Request`
  - 우리가 노출하는 HTTP 출력: `{Resource}Response` (동작이 핵심이면 `{Action}{Resource}Response`)
  - 모든 HTTP 성공 응답은 공통 봉투 `ApiResponse<T>`(`global/web`)로 감싼다. `{Resource}Response`는 그 안의 `T`다.
  - 에러는 응답 DTO로 표현하지 않고 ProblemDetail로 내려보낸다 ([예외 처리 룰](exception.md)).
  - Entity를 그대로 직렬화해 응답하지 않는다.
  - `Response`는 Entity로부터 만드는 정적 팩토리(`from`)를 둔다.
- 이유
  - Entity를 직접 응답하면 내부 컬럼·연관관계·식별자 형식이 API 계약에 묶인다.
  - `from`을 두면 도메인 → 응답 변환 위치가 한 곳에 고정된다.
  - 봉투를 통일하면 클라이언트가 성공 응답을 일관된 모양으로 받는다.

Bad:
```java
@GetMapping("/api/companies/{corpCode}")
Company getCompany(@PathVariable String corpCode) {
    return companyQuery.findByCorpCode(corpCode);
}
```

Good:
```java
@GetMapping("/api/companies/{corpCode}")
ApiResponse<CompanyResponse> getCompany(@PathVariable String corpCode) {
    return ApiResponse.of(CompanyResponse.from(companyQuery.findByCorpCode(corpCode)));
}

public record CompanyResponse(String corpCode, String name, String stockCode) {

    static CompanyResponse from(Company company) {
        return new CompanyResponse(company.getCorpCode(), company.getName(), company.getStockCode());
    }
}
```

## 5. 외부 공급원 응답은 infra/http에 `{Provider}{Resource}Response`로 두고 밖으로 내보내지 않는다

- 규칙
  - 외부 API 응답 모양: `{Provider}{Resource}Response` (예: `DartCompanyProfileResponse`)
  - 외부 응답 DTO는 `infra/http` 패키지 밖으로 노출하지 않는다(package-private 우선).
  - 외부 원문 필드명(snake_case)은 DTO에서만 다루고, 도메인 변환은 어댑터에서 한다.
  - 스트리밍 파서처럼 중간 객체 없이 Entity를 바로 만들 수 있으면 외부 DTO를 만들지 않는다.
- 이유
  - 포트 이름은 도메인 자원을 드러내고 provider는 어댑터에만 남긴다는 규칙(`아키텍처 규칙 8`)을 DTO에도 맞춘다.
  - 외부 응답 DTO가 application·interfaces로 새면 외부 포맷 변경이 전 계층으로 번진다.
  - `corpCode.xml`처럼 이미 도메인으로 직접 환원되는 경로엔 DTO가 군더더기다.

Bad:
```java
// application 이 외부 응답 DTO 를 직접 받음
CompanyRegistrySyncResult sync(DartCompanyProfileResponse response) {
}
```

Good:
```java
// infra/http
record DartCompanyProfileResponse(
        @JsonProperty("corp_code") String corpCode,
        @JsonProperty("stock_name") String stockName,
        @JsonProperty("corp_cls") String corpClass) {

    Company toCompany() {
        // snake_case 원문 → 도메인 변환은 여기(어댑터 경계)에서
    }
}
```

## 6. 유스케이스 입출력은 application에 Command / Result로 둔다

- 규칙
  - 유스케이스 입력 파라미터가 여러 개거나 구조를 가지면 `{Behavior}Command`로 묶는다.
  - 유스케이스 반환값은 `{Behavior}Result`로 둔다.
  - 인자가 한두 개로 단순하면 Command를 만들지 않고 원시 인자를 그대로 받는다.
  - Command/Result는 interfaces·infra가 의존해도 되지만, 외부 응답 DTO를 품지 않는다.
- 이유
  - 유스케이스 계약을 Request/Response(우리 API 모양)와 분리해, 같은 유스케이스를 controller·scheduler·runner가 함께 쓸 수 있다(`아키텍처 규칙 3`).
  - 단순 인자까지 Command로 감싸면 빈 래퍼만 늘어난다.

Bad:
```java
// 단일 인자를 굳이 Command 로 감쌈
public CompanyRegistrySyncResult syncAll(SyncAllCommand command) {
}
```

Good:
```java
public CompanyRegistrySyncResult syncAll() {
}

public record CompanyRegistrySyncResult(int upsertedCount) {
}
```

## 7. `Dto` 접미사를 쓰지 않고 방향·역할로 이름 짓는다

- 규칙
  - `XxxDto`처럼 역할이 없는 접미사를 쓰지 않는다.
  - 방향과 역할이 드러나는 접미사(`Request`, `Response`, `Command`, `Result`)를 쓴다.
  - 같은 도메인 자원이라도 입력·출력·외부응답은 별도 타입으로 둔다(한 DTO를 양방향으로 재사용하지 않는다).
- 이유
  - `Dto`만으로는 입력인지 출력인지, 우리 것인지 외부 것인지 알 수 없다.
  - 한 타입을 요청·응답에 겸용하면 한쪽 필요 때문에 다른 쪽 필드가 오염된다.

Bad:
```java
class CompanyDto {
}
```

Good:
```java
record CompanyResponse() {}
record CreateCompanyRequest() {}
record DartCompanyProfileResponse() {}
```

## 8. DTO는 record를 기본으로 한다

- 규칙
  - DTO는 불변 데이터 묶음이므로 `record`로 작성한다.
  - getter·equals·toString을 직접 만들지 않는다.
  - 검증·변환이 필요하면 compact constructor 또는 정적 팩토리(`from`, `toCompany`)에 둔다.
- 이유
  - DTO는 상태 변경이 없어 record의 불변·간결함과 맞는다.
  - 보일러플레이트를 줄이고 경계 모양 정의에 집중한다.
