# 예외 처리 룰

에러를 세 분류로 나누고, 각 분류는 처리 경로를 하나로 고정한다. 새 예외 타입은 클라이언트 대응이 갈릴 때만 만든다.
성공 응답 봉투는 [dto 룰](dto.md) 참고.

## 1. 에러를 세 가지로 분류하고 분류마다 처리 경로를 하나로 고정한다

- 규칙
  - 모든 에러를 아래 세 분류 중 하나로 본다.
  - 분류마다 던지는 곳·매핑하는 곳·HTTP status·로깅이 정해져 있다.
- 이유
  - 분류가 고정되면 새 기능에서 "이 에러를 어떻게 내릴까"를 다시 결정하지 않는다.
  - 외부 의존 실패(502)와 우리 버그(500)를 구분해야 운영 대응이 갈린다.

| 분류 | 무엇 | 던지는 곳 | 매핑하는 곳 | HTTP | 로깅 |
|---|---|---|---|---|---|
| 클라이언트 잘못 | 잘못된 요청·검증 실패·없는 리소스·충돌 | 프레임워크 / (필요 시) 도메인 | problemdetails 자동 / 도메인 advice | 4xx | 안 함 |
| 외부 의존 실패 | DART·KIS·DB 등이 죽음 | infra 어댑터 (포트 예외) | 도메인 advice | 502 / 503 | warn |
| 예상 못한 버그 | 그 외 전부 | 아무도 의도 안 함 | global handler | 500 | error + 스택 |

> HTTP가 아닌 트리거(스케줄러 등)는 매핑 없이 예외가 전파·로깅된다. 재시도는 별도 정책.

## 2. 새 예외 타입은 클라이언트 대응이 갈릴 때만 만든다

- 규칙
  - 새 예외는 "클라이언트가 구분해야 하는 (HTTP status + code) 조합"이 새로 필요할 때만 만든다.
  - 같은 status + code로 떨어질 실패들은 메시지만 다르게 해서 한 예외를 공유한다.
  - 실패 원인마다 예외를 만들지 않는다.
- 이유
  - 예외 타입 폭발을 막는다.
  - 클라이언트가 구분하지 못하는 예외는 분리할 가치가 없다.

Bad:
```java
class DartHttpException extends RuntimeException {}
class DartZipException extends RuntimeException {}
class DartParseException extends RuntimeException {}
```

Good:
```java
// HTTP 실패·zip 아님·파싱 실패 모두 502 + 같은 code → 한 예외, 메시지로 구분
class CompanyRegistrySourceException extends RuntimeException {
}
```

## 3. 예외는 자기가 설명하는 추상이 사는 곳에 두고, 경계를 넘을 때만 끌어올린다

- 규칙
  - 예외는 그것이 설명하는 추상이 있는 곳에 둔다.
  - 여러 계층이 공유해야(경계를 넘어야) 할 때만 상위로 끌어올린다.
  - 한 계층 안에서 잡혀 끝나는 예외는 그 계층에 둔다.
- 이유
  - 던지는 계층과 잡는 계층이 합법적으로 닿을 수 있는 공통 위치여야 의존 방향이 안 깨진다.
  - 경계를 안 넘는 예외를 상위로 올리면 그 계층이 모르는 타입으로 오염된다.

| 예외 성격 | 위치 |
|---|---|
| 포트/도메인 추상의 실패 계약 | domain (포트 옆) |
| 도메인 비즈니스 규칙 위반 | domain |
| infra 안에서 잡혀 끝나는 구현 디테일 | infra |
| 순수 웹 관심사 | interfaces |

Bad:
```java
// 예외가 infra 에 있어 interfaces 가 infra 를 import (의존 방향 위반)
package ...company.infra.http;
class CompanyRegistrySourceException extends RuntimeException {}
```

Good:
```java
// 포트(CompanyRegistrySource)가 domain 에 사니 실패 계약도 domain 에.
// infra 가 던지고 interfaces 가 잡아도 둘 다 domain 만 의존.
package ...company.domain;
class CompanyRegistrySourceException extends RuntimeException {}
```

## 4. 도메인·인프라 예외는 프레임워크를 모른다. HTTP 매핑은 interfaces에서 한다

- 규칙
  - 예외 클래스에 `HttpStatus`·`@ResponseStatus` 같은 웹 의존을 붙이지 않는다.
  - 예외 → HTTP status 변환은 interfaces의 `@RestControllerAdvice`에서 한다.
  - 도메인 고유 예외는 도메인별 advice가, 안 잡힌 예외는 global handler가 맡는다.
- 이유
  - 도메인·인프라가 웹 프레임워크를 알면 status 정책·프레임워크 교체가 도메인까지 번진다.
  - `@ResponseStatus`로 코드는 줄어도 도메인이 HTTP를 알게 되는 더 큰 결합을 만든다.

Bad:
```java
@ResponseStatus(HttpStatus.BAD_GATEWAY)
class CompanyRegistrySourceException extends RuntimeException {
}
```

Good:
```java
class CompanyRegistrySourceException extends RuntimeException {
}

@RestControllerAdvice(basePackageClasses = CompanyExceptionHandler.class)
class CompanyExceptionHandler {

    @ExceptionHandler(CompanyRegistrySourceException.class)
    ProblemDetail handleRegistrySource(CompanyRegistrySourceException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, e.getMessage());
        problem.setProperty("code", "COMPANY_REGISTRY_SOURCE_ERROR");
        return problem;
    }
}
```

## 5. 에러 응답은 ProblemDetail로 내려보내고 기계용 코드를 함께 준다

- 규칙
  - 에러 본문은 `ProblemDetail`(RFC 7807)로 내려보낸다.
  - 클라이언트가 분기할 기계용 코드는 `setProperty("code", ...)`로 담는다.
  - 성공과 에러를 하나의 봉투로 합치지 않는다 (성공은 [dto 룰](dto.md)의 `ApiResponse`).
- 이유
  - HTTP status가 성공/실패를 말하므로 본문에 `success` 플래그를 두면 중복이다.
  - 에러 스키마를 직접 설계하지 않고 프레임워크가 주는 형식을 쓴다.
  - `detail`은 사람용이라 기계가 분기할 코드가 따로 필요하다.

## 6. 에러 코드는 리터럴로 시작하고, 공유·문서화 필요가 생기면 enum으로 승격한다

- 규칙
  - 에러 코드는 처음엔 문자열 리터럴로 둔다.
  - 같은 코드가 여러 곳에서 참조되거나 클라이언트에 코드 목록을 문서로 줘야 할 때 enum 카탈로그로 올린다.
- 이유
  - 한 곳에서만 쓰는 코드를 위해 enum을 만들면 이동 비용만 늘어난다.
  - 코드가 흩어지거나 외부 계약이 되면 그때 한곳에 모으는 값이 생긴다.

## 7. 로깅은 분류에 맞춘다

- 규칙
  - 예상 못한 버그(500): `error` + 스택 트레이스
  - 외부 의존 실패(502/503): `warn` (우리 버그가 아니라 외부 탓)
  - 클라이언트 잘못(4xx): 로깅하지 않는다
- 이유
  - 4xx까지 error로 남기면 진짜 우리 버그(500)가 로그에 묻힌다.
  - 외부 실패는 추적은 필요하되 우리 코드 결함과 구분돼야 한다.

Bad:
```java
@ExceptionHandler(MethodArgumentNotValidException.class)
ProblemDetail handle(Exception e) {
    log.error("검증 실패", e);
}
```

Good:
```java
@ExceptionHandler(CompanyRegistrySourceException.class)
ProblemDetail handleRegistrySource(CompanyRegistrySourceException e) {
    log.warn("회사 등록부 외부 소스 실패", e);
    ...
}
```
