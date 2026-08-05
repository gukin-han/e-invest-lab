# 테스트 룰

## 1. 테스트 도구는 JUnit 5와 AssertJ를 사용한다

- 규칙
  - 테스트 프레임워크: JUnit 5
  - 검증 라이브러리: AssertJ
  - DB · 영속화 테스트: Testcontainers MySQL (`MySqlTestContainerConfig`)
- 이유
  - 테스트 작성 방식과 assertion 문법을 통일한다.
  - 실패 메시지를 읽기 쉽게 유지한다.

Bad:
```java
import static org.junit.jupiter.api.Assertions.assertEquals;

assertEquals("삼성전자", company.getName());
```

Good:
```java
import static org.assertj.core.api.Assertions.assertThat;

assertThat(company.getName()).isEqualTo("삼성전자");
```

## 2. 테스트 클래스명은 자원과 동작으로 작성하고 자원을 먼저 쓴다

- 규칙
  - 형식: `<Resource><Action><Kind>Test`
  - `<Resource>`: 검증 대상 자원·주체 (명사)
  - `<Action>`: 검증하려는 동작·시나리오
  - `<Kind>`: `Unit`, `Integration`, `E2E`, `Smoke`, `Arch` 중 하나
  - 자원을 먼저, 동작을 나중에 쓴다 (`CompanyRegistrySync`, `SyncCompanyRegistry`가 아님).
  - 동작마다 클래스를 쪼개지 않는다. 세부 시나리오는 규칙 3의 `@Nested`로 묶는다.
  - 아키텍처 테스트는 예외다. 구조 규칙(의존 방향·네이밍 등)을 검증해 특정 자원·동작이 없으므로 `<구조 측면>Arch` 형식으로 쓴다 (예: `DependencyArchTest`). DisplayName도 붙이지 않는다(규칙 4).
- 이유
  - 클래스 이름은 명사구이므로 자원이 앞서야 클래스답게 읽힌다.
  - 자원이 먼저면 같은 도메인 테스트가 알파벳순으로 모여 탐색이 쉽다.
  - 클래스는 자원(명사), 메서드는 동작(`shouldXxx`)으로 역할이 갈린다.
  - 검증 대상에 이름을 묶으면 코드 구조가 바뀌어도 의미가 유지된다.

| Kind | 의미 |
|---|---|
| `Unit` | 단위 테스트. 외부 의존 없음, Spring 컨텍스트 안 띄움 |
| `Integration` | 통합 테스트. Testcontainers, 실제 DB·HTTP 사용 |
| `E2E` | 종단간. 여러 도메인을 가로지르는 시나리오 |
| `Smoke` | 외부 API 응답 형식 회귀. 네트워크 한정 사용 |
| `Arch` | 아키텍처 적합성. 의존 방향·레이어·네이밍 등 구조 규칙 검증. Spring 컨텍스트 안 띄움 |

Bad:
```java
class CompanyTest {
}

class SyncCompanyRegistryUnitTest {
}

class BuildUnitTest {
}
```

Good:
```java
class CompanyCreationUnitTest {
}

class CompanyRegistrySyncIntegrationTest {
}

class DartCompanyRegistryFixtureSmokeTest {
}
```

## 3. 테스트 메서드명은 shouldXxx로 작성한다

- 규칙
  - 메서드명: `shouldXxx` 카멜케이스, 영어
  - 세부 컨텍스트는 `With...` 절로 붙일 수 있다.
  - `With...` 절은 underscore 없이 카멜케이스로 붙인다.
  - 컨텍스트 묶음은 `@Nested` 내부 클래스로 표현한다.
  - `@Nested` 클래스명은 `When...` 또는 `With...`로 시작한다.
  - `When...`: “무엇을 할 때?”를 표현한다. 행위, 이벤트, 상태 전환, 유스케이스 실행 중심.
  - `With...`: “어떤 조건/입력으로?”를 표현한다. 데이터 상태, 옵션, 누락 필드, 예외 입력, 환경 조건 중심.
  - 조건을 설명하지 않는 의례적인 `@Nested`는 만들지 않는다.
- 이유
  - 메서드명은 테스트 실행 결과에서 바로 읽힌다.
  - `@Nested`로 시나리오를 묶으면 메서드명에 모든 조건을 밀어 넣지 않아도 된다.
  - `When...`과 `With...`를 구분하면 테스트 리포트가 대상 -> 주요 시나리오 -> 세부 조건 순서로 읽힌다.

Bad:
```java
@Test
void build() {
}

@Nested
class UnlistedCompany {
}

@Test
void shouldFlushRemainingCompanies_withPartialBatch() {
}
```

Good:
```java
@Nested
class WhenSyncingRegistry {

    @Test
    void shouldFlushRemainingCompaniesWithPartialBatch() {
    }
}

@Nested
class WithUnlistedCompany {
}
```

## 4. DisplayName은 한국어 비즈니스 표현으로 작성한다

- 규칙
  - 모든 테스트 레벨에 `@DisplayName`을 붙인다.
  - 아우터 클래스: 검증 대상 도메인 영역 + 테스트 종류
  - `@Nested` 이너 클래스: 시나리오·컨텍스트
  - `@Test` 메서드: 요구사항 한 줄
  - 코드명(`Company`, `CorpCode`)과 기술 용어(`null`, `entity`)는 가능한 자제한다.
  - 단, 아키텍처 테스트(Kind `Arch`)는 예외다. 구조 규칙은 비즈니스 표현이 없어 DisplayName 없이 메서드명을 단일 출처로 쓴다.
- 이유
  - 테스트 리포트는 개발자뿐 아니라 도메인 요구사항을 확인하는 문서 역할도 한다.
  - 비즈니스 표현은 구현 변경보다 오래 유지된다.

Bad:
```java
@DisplayName("Company build test")
class CompanyUnitTest {

    @Test
    @DisplayName("stockCode null")
    void shouldAcceptNullStockCode() {
    }
}
```

Good:
```java
@DisplayName("회사 등록부 도메인 생성 단위 테스트")
class CompanyCreationUnitTest {

    @Nested
    @DisplayName("등록부 필드로 회사를 등록할 때")
    class WhenBuiltFromRegistryFields {

        @Test
        @DisplayName("비상장 회사도 등록할 수 있다 - 종목코드 없음 허용")
        void shouldAcceptNullStockCodeWithUnlistedCompany() {
        }
    }
}
```

## 5. 테스트 본문은 Arrange-Act-Assert 순서로 작성한다

- 규칙
  - 테스트 본문은 준비, 실행, 검증 순서로 작성한다.
  - 세 단계는 빈 줄로 구분한다.
  - 단계가 길거나 의도가 흐려질 때만 `// arrange`, `// act`, `// assert` 주석을 사용한다.
  - `// given`, `// when`, `// then` 주석은 사용하지 않는다.
- 이유
  - AAA는 JUnit 테스트에서 가장 보편적이고 중립적인 구조다.
  - `@Nested class When...`과 본문 `// when`이 섞이면 용어가 중복된다.
  - 모든 테스트에 주석을 강제하면 반복 소음이 된다.

Bad:
```java
assertThat(result.upsertedCount()).isEqualTo(2_500);
CompanyRegistrySyncResult result = service.syncAll();
CompanyRegistrySyncService service = serviceWithCompanies(2_500);
```

Good:
```java
CompanyRegistrySyncService service = serviceWithCompanies(2_500);

CompanyRegistrySyncResult result = service.syncAll();

assertThat(result.upsertedCount()).isEqualTo(2_500);
```

## 6. 단일 값은 assertThat으로 바로 검증한다

- 규칙
  - 단일 값은 `assertThat(actual).isEqualTo(expected)`처럼 바로 검증한다.
  - 불필요한 helper나 soft assertion으로 감싸지 않는다.
- 이유
  - 가장 작은 검증은 가장 직접적인 표현이 읽기 쉽다.
  - 과한 구조화는 테스트 의도를 흐린다.

Bad:
```java
assertSoftly(softly -> {
    softly.assertThat(upsertedCount).isEqualTo(1);
});
```

Good:
```java
assertThat(upsertedCount).isEqualTo(1);
```

## 7. 한 객체의 여러 필드는 extracting으로 묶는다

- 규칙
  - 한 객체의 여러 필드는 `assertThat(obj).extracting(...).containsExactly(...)`로 검증한다.
  - 한 객체의 필드 비교에는 soft assertion을 쓰지 않는다.
- 이유
  - 필드 묶음 검증은 “하나의 객체 상태”를 한 번에 보여준다.
  - soft assertion으로 getter를 나열하면 어떤 객체 상태를 검증하는지 흩어진다.

Bad:
```java
assertSoftly(softly -> {
    softly.assertThat(company.getCorpCode()).isEqualTo("00126380");
    softly.assertThat(company.getName()).isEqualTo("삼성전자");
    softly.assertThat(company.getStockCode()).isEqualTo("005930");
});
```

Good:
```java
assertThat(company)
        .extracting(Company::getCorpCode, Company::getName, Company::getStockCode)
        .containsExactly("00126380", "삼성전자", "005930");
```

## 8. 컬렉션 원소의 여러 필드는 tuple로 묶는다

- 규칙
  - 리스트·컬렉션 원소의 여러 필드는 `extracting(...).containsExactly(tuple(...), tuple(...))`로 검증한다.
  - 순서가 의미 있으면 `containsExactly`, 순서가 의미 없으면 `containsExactlyInAnyOrder`를 사용한다.
- 이유
  - 원소별 기대값이 한 줄에 모이면 컬렉션의 전체 모양을 비교하기 쉽다.
  - 인덱스로 꺼내 여러 번 검증하면 실패 위치와 기대 구조가 흩어진다.

Bad:
```java
assertThat(companies.get(0).getCorpCode()).isEqualTo("00126380");
assertThat(companies.get(0).getName()).isEqualTo("삼성전자");
assertThat(companies.get(1).getCorpCode()).isEqualTo("00434003");
assertThat(companies.get(1).getName()).isEqualTo("다코");
```

Good:
```java
assertThat(companies)
        .extracting(Company::getCorpCode, Company::getName)
        .containsExactly(
                tuple("00126380", "삼성전자"),
                tuple("00434003", "다코")
        );
```

## 9. 한 시나리오의 서로 다른 결과는 assertSoftly로 묶는다

- 규칙
  - 서로 다른 객체, DB 상태, 반환값, 이벤트 발행 여부를 한 시나리오에서 함께 검증할 때 `assertSoftly`를 사용한다.
  - `assertSoftly`는 `org.assertj.core.api.SoftAssertions.assertSoftly`를 static import 한다.
  - `assertSoftly` 안에서도 한 객체의 여러 필드는 `extracting(...).containsExactly(...)`로 묶는다.
- 이유
  - 한 시나리오의 여러 결과가 함께 실패할 수 있으면 실패 정보를 한 번에 보는 편이 좋다.
  - soft assertion은 “여러 결과”를 묶는 도구이지 “한 객체 필드”를 나열하는 도구가 아니다.

Bad:
```java
assertThat(jpaRepository.count()).isEqualTo(1);
assertThat(upsertedCount).isEqualTo(1);
assertThat(found.getName()).isEqualTo("삼성전자변경");
```

Good:
```java
assertSoftly(softly -> {
    softly.assertThat(jpaRepository.count()).isEqualTo(1);
    softly.assertThat(upsertedCount).isEqualTo(1);
    softly.assertThat(found)
            .extracting(Company::getName, Company::getStockCode)
            .containsExactly("삼성전자변경", null);
});
```

## 10. 반복되는 검증 묶음은 private assertion helper로 뺀다

- 규칙
  - 같은 검증 묶음이 여러 테스트에서 반복되면 private assertion helper로 뺀다.
  - 한 테스트에서만 쓰는 검증은 인라인으로 둔다.
- 이유
  - 반복되는 assertion helper는 테스트 본문을 요구사항 중심으로 유지한다.
  - 한 번만 쓰는 helper는 오히려 테스트를 읽을 때 이동 비용을 만든다.

Bad:
```java
assertThat(company)
        .extracting(Company::getCorpCode, Company::getName, Company::getStockCode)
        .containsExactly("00126380", "삼성전자", "005930");
```

Good:
```java
assertCompanyRegistryFields(company, "00126380", "삼성전자", "005930");

private void assertCompanyRegistryFields(Company company, String corpCode, String name, String stockCode) {
    assertThat(company)
            .extracting(Company::getCorpCode, Company::getName, Company::getStockCode)
            .containsExactly(corpCode, name, stockCode);
}
```

## 11. 테스트 기대값은 필요한 만큼만 구조화한다

- 규칙
  - 한 테스트에서 한 번만 쓰이고 의미가 바로 보이는 기대값은 인라인으로 둔다.
  - 조건과 검증에 함께 쓰이거나 여러 번 반복되는 값은 `private static final` 상수로 둔다.
  - 값만 봐서 의미가 불분명한 식별자는 상수명으로 의미를 붙인다.
  - 함께 움직이는 기대값 묶음이 반복되면 테스트 내부 `private record` 또는 `enum`으로 묶는다.
  - JPA entity 같은 mutable domain 객체를 enum 상수로 들고 있지 않는다.
  - production VO는 검증 규칙과 재사용 필요가 명확할 때만 도입한다.
- 이유
  - 모든 값을 상수화하면 입력과 기대값이 멀어져 테스트를 읽기 어려워진다.
  - 반복되거나 조건에 쓰이는 값은 이름을 붙여야 의도와 오타 위험이 분명해진다.
  - 값 하나를 위해 VO를 만들면 테스트를 읽을 때 이동 비용이 커진다.
  - 반복되는 값 묶음은 구조화해야 의미와 변경 범위가 분명해진다.
  - 테스트 편의를 위해 production 모델을 복잡하게 만들지 않는다.

Bad:
```java
private enum KnownCompany {
    SAMSUNG(Company.builder()
            .corpCode("00126380")
            .name("삼성전자")
            .stockCode("005930")
            .build());
}
```

Good:
```java
assertThat(company.getName()).isEqualTo("삼성전자");
```

Good:
```java
private static final String SAMSUNG_CORP_CODE = "00126380";
private static final String SAMSUNG_NAME = "삼성전자";
private static final String SAMSUNG_STOCK_CODE = "005930";
```

Good:
```java
private record KnownCompany(String corpCode, String name, String stockCode) {

    void assertMatches(Company company) {
        assertThat(company)
                .extracting(Company::getCorpCode, Company::getName, Company::getStockCode)
                .containsExactly(corpCode, name, stockCode);
    }
}
```

## 12. 외부 API 테스트는 네트워크를 타지 않는다

- 규칙
  - 외부 API는 mock 또는 `src/test/resources/fixtures/...`의 고정 응답으로 테스트한다.
  - 키 의존 클라이언트(LLM, KIS 등)는 키 미설정 시 NoOp 빈으로 대체한다.
- 이유
  - 네트워크 테스트는 느리고 실패 원인이 불안정하다.
  - 키 의존 테스트는 로컬·CI 환경 차이로 쉽게 깨진다.

Bad:
```java
@Test
void shouldReadCompaniesFromDart() {
    String body = restTemplate.getForObject("https://opendart.fss.or.kr/api/corpCode.xml", String.class);

    assertThat(body).contains("corp_code");
}
```

Good:
```java
@Test
void shouldReadCompaniesFromFixture() {
    InputStream body = getClass().getResourceAsStream("/fixtures/company/CORPCODE.zip");

    reader.read(body, companies::add);

    assertThat(companies).isNotEmpty();
}
```

## 13. 픽스처 파일은 도메인별 fixtures 디렉터리에 둔다

- 규칙
  - 위치: `src/test/resources/fixtures/<도메인>/...`
  - 1MB 미만: 자유
  - 1~10MB: 거의 안 바뀌는 응답 1회분만 보관
  - 10MB 이상: git LFS 또는 외부 스토리지 고려
- 이유
  - fixture 위치가 고정되면 테스트 데이터 출처를 찾기 쉽다.
  - 큰 파일을 무분별하게 커밋하면 clone, diff, 리뷰 비용이 커진다.

Bad:
```text
src/test/resources/CORPCODE.zip
src/test/java/dev/gukin/einvestlab/company/CORPCODE.zip
```

Good:
```text
src/test/resources/fixtures/company/CORPCODE.zip
```

## 14. 단위 테스트와 통합 테스트를 분리한다

- 규칙
  - 도메인 모델·서비스 단위 테스트: 순수 JUnit, Spring 컨텍스트 안 띄움
  - 영속화·웹 레이어 테스트: `@DataJpaTest` 또는 `@SpringBootTest` + Testcontainers
- 이유
  - 단위 테스트는 빠르게 도메인 규칙을 검증해야 한다.
  - 통합 테스트는 실제 DB·HTTP 경계에서 깨질 수 있는 설정과 매핑을 검증해야 한다.

Bad:
```java
@SpringBootTest
class CompanyCreationUnitTest {

    @Test
    void shouldCreateCompany() {
        Company company = Company.builder()
                .name("삼성전자")
                .build();

        assertThat(company.getName()).isEqualTo("삼성전자");
    }
}
```

Good:
```java
class CompanyCreationUnitTest {

    @Test
    void shouldCreateCompany() {
        Company company = Company.builder()
                .name("삼성전자")
                .build();

        assertThat(company.getName()).isEqualTo("삼성전자");
    }
}
```

## 15. 파서·추출기는 실물 픽스처 회귀 테스트를 둔다

- 규칙
  - 외부 원천을 파싱하는 코드(등록부 zip, 공시 XML, 리포트 목록 HTML)는 실제 응답을 캡처한 픽스처로 스모크 테스트를 둔다.
  - 검증은 "끝까지 읽힌다 + 알려진 항목이 기대값으로 나온다" 수준이면 충분하다: 전체 건수 하한, 삼성전자 같은 앵커 항목의 필드 값.
  - 손으로 만든 최소 픽스처(규칙 12)와 별개다 — 최소 픽스처는 분기를, 실물 픽스처는 실제 서식과의 계약을 검증한다.
- 이유
  - 손으로 만든 픽스처는 우리가 상상한 서식만 검증한다. 실물에만 있는 변형(빈 필드, 북마크 속성, 인코딩)은 실물로만 잡힌다.
  - 원천 서식이 바뀌면 라이브 수집 전에 픽스처 갱신 시점에 빨간불이 켜진다.

Good:
```java
@Test
@DisplayName("실제 등록부 zip 응답을 끝까지 읽고 알려진 상장 회사를 찾는다")
void shouldReadCapturedCorpCodeZipFixture() {
    reader.read(corpCodeZipFixture(), entry -> { ... });

    assertThat(count.get()).isGreaterThan(100_000);
    assertThat(samsung.get())
            .extracting(CompanyRegistryEntry::name, CompanyRegistryEntry::stockCode)
            .containsExactly("삼성전자", "005930");
}
```

## 16. 가드·규칙 검증은 known-violation 으로 빨간불을 확인한다

- 규칙
  - 불변식 가드나 ArchUnit 규칙을 추가·수정하면, 단언은 그대로 두고 **입력(대상)을 실제 위반 사례로 바꿔** 테스트가 빨간불이 되는지 확인한 뒤 원복한다.
  - 코드 쪽 가드는 버그 패턴을 일시 재현(예: 경계 정규식을 버그 버전으로 되돌림)해 가드가 무는지 본다.
  - 확인은 일회성 절차이고 커밋에는 초록 상태만 남긴다.
- 이유
  - 한 번도 실패해 본 적 없는 가드는 아무것도 안 잡는 가드와 구분되지 않는다. 빨간불을 본 가드만 신뢰할 수 있다.
  - 단언을 약화해 초록을 만드는 실수(항상 통과하는 규칙)를 이 절차가 걸러낸다.
