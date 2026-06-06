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

## 2. 테스트 클래스명은 동작 기반으로 작성한다

- 규칙
  - 형식: `<Behavior><Kind>Test`
  - `<Behavior>`: 검증하려는 동작·시나리오
  - `<Kind>`: `Unit`, `Integration`, `E2E`, `Smoke` 중 하나
- 이유
  - 클래스명·메서드명 기반 네이밍은 테스트를 코드 구조에 묶는다.
  - 동작 기반 네이밍은 리팩토링 후에도 “무엇이 보장되어야 하는가”를 유지한다.

| Kind | 의미 |
|---|---|
| `Unit` | 단위 테스트. 외부 의존 없음, Spring 컨텍스트 안 띄움 |
| `Integration` | 통합 테스트. Testcontainers, 실제 DB·HTTP 사용 |
| `E2E` | 종단간. 여러 도메인을 가로지르는 시나리오 |
| `Smoke` | 외부 API 응답 형식 회귀. 네트워크 한정 사용 |

Bad:
```java
class CompanyTest {
}

class CompanyUnitTest {
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
  - 세부 컨텍스트는 `with...` 절로 붙일 수 있다.
  - 컨텍스트 묶음은 `@Nested` 내부 클래스로 표현한다.
  - `@Nested` 클래스명은 `When...` 또는 `With...`로 시작한다.
- 이유
  - 메서드명은 테스트 실행 결과에서 바로 읽힌다.
  - `@Nested`로 시나리오를 묶으면 메서드명에 모든 조건을 밀어 넣지 않아도 된다.

Bad:
```java
@Test
void build() {
}

@Nested
class UnlistedCompany {
}
```

Good:
```java
@Nested
class WithUnlistedCompany {

    @Test
    void shouldAcceptNullStockCodeWithUnlistedCompany() {
    }
}
```

## 4. DisplayName은 한국어 비즈니스 표현으로 작성한다

- 규칙
  - 모든 테스트 레벨에 `@DisplayName`을 붙인다.
  - 아우터 클래스: 검증 대상 도메인 영역
  - `@Nested` 이너 클래스: 시나리오·컨텍스트
  - `@Test` 메서드: 요구사항 한 줄
  - 코드명(`Company`, `CorpCode`)과 기술 용어(`null`, `entity`)는 가능한 자제한다.
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
@DisplayName("회사 등록부 도메인 생성")
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

## 5. 검증은 가장 단순한 방식으로 묶는다

- 규칙
  - 단일 값 검증: `assertThat(x).isEqualTo(y)`
  - 한 객체의 여러 필드 검증: `assertThat(obj).extracting(...).containsExactly(...)`
  - 서로 다른 객체·상태를 한 시나리오에서 함께 검증: `assertSoftly(softly -> { ... })`
  - 한 객체 필드 비교에는 soft assertion을 쓰지 않는다.
- 이유
  - assertion 방식이 과하면 테스트 의도가 흐려진다.
  - 필드 묶음 검증은 “하나의 객체 상태”를 한 번에 보여준다.

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

## 6. 외부 API 테스트는 네트워크를 타지 않는다

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

## 7. 픽스처 파일은 도메인별 fixtures 디렉터리에 둔다

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

## 8. 단위 테스트와 통합 테스트를 분리한다

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
