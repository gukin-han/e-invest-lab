# 아키텍처 규칙

## 1. 도메인 기준 패키지 구조를 사용한다

- 규칙
  - 패키지 루트는 `dev.gukin.einvestlab`이다.
  - 전역 공통 코드는 `global`에 둔다.
  - 도메인 코드는 도메인 이름 아래에 둔다.
  - 각 도메인은 `domain`, `application`, `infra`, `interfaces`를 기본 경계로 사용한다.
- 이유
  - 도메인별 변경 범위를 패키지 구조에서 바로 확인할 수 있다.
  - 레이어 기준 최상위 구조보다 도메인 응집도가 높다.

Bad:
```text
dev.gukin.einvestlab
├── controller
├── service
├── repository
└── entity
```

Good:
```text
dev.gukin.einvestlab
├── global
└── company
    ├── domain
    ├── application
    ├── infra
    │   ├── db
    │   └── http
    └── interfaces
```

## 2. 도메인 내부 의존 방향을 지킨다

- 규칙
  - 기본 의존 방향: `interfaces -> application -> domain <- infra`
  - 레이어 간 의존 방향과 도메인의 프레임워크 격리는 `DependencyArchTest`가 강제한다. 구체 규칙은 테스트를 본다.
  - 엔티티는 도메인 모델로 사용하므로 `domain`의 JPA 어노테이션은 허용한다. (`jakarta.persistence`는 허용, `org.springframework`는 금지)
- 이유
  - 도메인 규칙을 프레임워크와 외부 기술 변경으로부터 분리한다.
  - 의존 방향이 고정되면 순환 참조와 레이어 침범을 빠르게 발견할 수 있다.
  - 정확한 강제는 테스트가, 근거와 예시는 이 문서가 맡는다.

Bad:
```java
package dev.gukin.einvestlab.company.domain;

import org.springframework.stereotype.Service;

@Service
class CompanyPolicy {
}
```

Good:
```java
package dev.gukin.einvestlab.company.domain;

class CompanyPolicy {
}
```

## 3. application의 외부 진입 동작은 UseCase로 이름 짓는다

- 규칙
  - controller, scheduler, runner 같은 외부 adapter가 직접 호출하는 application 동작은 `{Behavior}UseCase`로 이름 짓는다.
  - UseCase 클래스에는 `@Service`를 붙인다.
  - UseCase 내부 협력 객체는 역할 이름을 쓰고 `@Component`를 붙인다.
  - UseCase 메서드는 `execute`보다 도메인 동작이 드러나는 이름을 우선한다.
- 이유
  - application layer의 진입점과 내부 협력 객체를 구분한다.
  - 같은 유스케이스를 HTTP, scheduler, runner가 재사용할 수 있게 한다.
  - `execute`만 쓰면 호출부에서 동작 의미가 약해진다.

Bad:
```java
@Service
class CompanyRegistrySyncService {

    CompanyRegistrySyncResult execute() {
        ...
    }
}
```

Good:
```java
@Service
class CompanyRegistrySyncUseCase {

    CompanyRegistrySyncResult syncAll() {
        ...
    }
}
```

## 4. 패키지는 기본적으로 평탄하게 유지한다

- 규칙
  - 기본은 `domain`, `application`, `infra`, `interfaces` 아래를 평탄하게 둔다.
  - `infra`는 기술 경계가 섞일 때 `db`, `http`까지만 나눌 수 있다.
  - `model`, `persistence`, `api`, `mapper`, `exception` 같은 세부 하위 폴더는 만들지 않는다.
- 이유
  - 작은 도메인에서 폴더를 과하게 나누면 탐색 비용이 더 커진다.
  - 패키지 깊이는 실제 복잡도가 생겼을 때만 늘린다.

Bad:
```text
company
└── infra
    ├── persistence
    ├── mapper
    ├── api
    └── exception
```

Good:
```text
company
└── infra
    ├── db
    └── http
```

## 5. Repository 인터페이스는 domain에 두고 구현체는 infra/db에 둔다

- 규칙
  - Repository 인터페이스: `domain`
  - Repository 구현체: `infra/db`
  - Repository 구현 이름: `{Domain}{Tech}Repository`
  - Repository 어댑터 이름: `{Domain}RepositoryAdapter`
- 이유
  - 도메인은 저장 기술을 몰라도 된다.
  - 도메인 이름으로 시작하면 같은 도메인의 클래스가 검색과 정렬에서 모인다.
  - `Impl`은 구현 기술이나 역할을 설명하지 못한다.

Bad:
```java
class JpaCompanyRepository {
}

class CompanyRepositoryImpl {
}
```

Good:
```java
class CompanyJpaRepository {
}

class CompanyRepositoryAdapter {
}
```

## 6. JDBC 기반 저장 로직은 도메인 JDBC 저장소에 둔다

- 규칙
  - 단순 CRUD와 일반 조회는 `{Domain}JpaRepository`에 둔다.
  - batch upsert, bulk write, DB 전용 SQL은 `{Domain}JdbcRepository`에 둔다.
  - `{Domain}RepositoryAdapter`는 domain Repository 포트를 구현하고 JPA/JDBC 저장소를 조합한다.
  - 쿼리마다 DAO 클래스를 만들지 않는다.
- 이유
  - JPA와 native SQL 책임이 하나의 adapter에 섞이지 않게 한다.
  - 성능 때문에 JDBC가 필요한 작업만 분리한다.
  - 쿼리 단위 클래스를 남발하지 않고 도메인 저장소 단위로 응집시킨다.

Bad:
```java
@Repository
class CompanyRepositoryAdapter implements CompanyRepository {

    private static final String UPSERT_SQL = "...";

    private final CompanyJpaRepository jpa;
    private final JdbcTemplate jdbc;
}
```

Good:
```java
@Repository
class CompanyRepositoryAdapter implements CompanyRepository {

    private final CompanyJpaRepository jpa;
    private final CompanyJdbcRepository jdbc;

    @Override
    public int upsertCompanies(List<Company> companies) {
        return jdbc.upsertCompanies(companies);
    }
}
```

## 7. HTTP endpoint prefix는 접근 주체와 운영 성격으로 구분한다

- 규칙
  - `/api/...`: 일반 사용자·클라이언트 기능
  - `/internal/...`: 운영자·서버 내부·배치 트리거
  - `/admin/...`: 관리자 화면·관리자 기능
  - `/actuator/...`: 헬스체크, 메트릭, 운영 관측 엔드포인트
  - 비용이 크거나 상태를 변경하는 운영 작업은 `/api/...`에 두지 않는다.
- 이유
  - URL만 봐도 접근 권한과 노출 범위를 추정할 수 있다.
  - 운영 작업이 일반 사용자 API와 섞이면 인증, rate limit, 관측 정책이 흐려진다.
  - 같은 application UseCase를 controller, scheduler, runner 같은 여러 adapter에서 재사용할 수 있다.

Bad:
```java
@PostMapping("/api/companies/registry-sync")
void syncRegistry() {
}
```

Good:
```java
@PostMapping("/internal/company-registry/sync")
void syncRegistry() {
}
```

## 8. 외부 API 포트와 어댑터 이름은 도메인 자원을 먼저 드러낸다

- 규칙
  - 외부 공급원 포트는 출처보다 도메인 자원을 먼저 드러낸다.
  - 외부 어댑터 구현체는 `{Provider}{Resource}Adapter` 형식으로 작성한다.
  - 구현체에서만 provider 이름을 드러낸다.
- 이유
  - 호출자는 외부 원문 용어보다 무엇을 받는지를 먼저 알아야 한다.
  - provider 교체 가능성은 포트 이름이 아니라 어댑터 이름에만 남긴다.

Bad:
```java
interface CorpCodeClient {
}

class DartCorpCodeClient {
}
```

Good:
```java
interface CompanyRegistrySource {
}

class DartCompanyRegistryAdapter {
}
```

## 9. 외부 API 설정과 예외 이름은 범위를 명확히 드러낸다

- 규칙
  - 외부 API 설정 이름: `{Provider}ApiProperties`
  - 외부 클라이언트 예외 이름: `{Provider}ClientException`
  - provider 전체 설정인지 API 접속 설정인지 구분한다.
- 이유
  - `Properties`만 붙이면 설정 범위가 모호하다.
  - 클라이언트 예외는 HTTP 오류뿐 아니라 응답 스트림, 압축, 파싱 실패까지 감쌀 수 있다.

Bad:
```java
class DartProperties {
}

class DartApiException extends RuntimeException {
}
```

Good:
```java
class DartApiProperties {
}

class DartClientException extends RuntimeException {
}
```

## 10. 도메인 간 직접 의존하지 않는다

- 규칙
  - 도메인 간 Entity를 직접 참조하지 않는다.
  - 다른 도메인의 객체는 ID로만 참조한다.
  - 도메인 간 연동은 이벤트 또는 application 간 연동을 사용한다.
- 이유
  - 도메인 간 Entity 참조는 변경 전파 범위를 키운다.
  - ID 참조와 application 경계는 도메인 간 결합을 낮춘다.

Bad:
```java
class Disclosure {
    private Company company;
}
```

Good:
```java
class Disclosure {
    private UUID companyId;
}
```
