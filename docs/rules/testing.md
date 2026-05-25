# 테스트 룰

## 도구
- JUnit 5 + AssertJ
- DB · 영속화 테스트: Testcontainers MySQL (`MySqlTestContainerConfig`)

## 네이밍

### 클래스
- 형식: `<Behavior><Kind>Test`
- `<Behavior>`: **검증하려는 동작·시나리오**. 클래스명·메서드명을 그대로 따라가지 않음.
  - 클래스명/메서드명 기반 네이밍은 테스트를 코드 구조에 묶음 → 리팩토링에 취약
  - 동작 기반 네이밍은 "무엇이 보장되어야 하는가"가 분명해짐
- `<Kind>`: 테스트 종류 — 아래 중 하나

| Kind | 의미 |
|---|---|
| `Unit` | 단위 테스트. 외부 의존 없음, Spring 컨텍스트 안 띄움 |
| `Integration` | 통합 테스트. Testcontainers, 실제 DB·HTTP 사용 |
| `E2E` | 종단간 (여러 도메인을 가로지르는 시나리오) |
| `Smoke` | 외부 API 응답 형식 회귀. 네트워크 한정 사용 |

좋은 예:
- `CompanyCreationUnitTest` — 회사 도메인 생성·필드 보존 동작
- `CompanyMasterSyncIntegrationTest` — 마스터 동기화 시나리오
- `CorpCodeStreamingParsingSmokeTest` — corpCode.xml 스트리밍 파싱 회귀

나쁜 예:
- `CompanyTest`, `CompanyUnitTest` — 클래스 자체가 기준 (동작 불명)
- `BuildUnitTest`, `SyncUnitTest` — 메서드명만 떼옴 (어디 메서드?)

### 메서드 + @Nested
- 메서드명: `shouldXxx` 카멜케이스, 영어. 세부 컨텍스트는 메서드명에 `with...` 절로 붙일 수 있음
  - 예: `shouldReturnNullWithMissingEnglishName`
- 컨텍스트 묶음은 `@Nested` 내부 클래스로. 이름은 `When...` 또는 `With...` 로 시작
  - 예: `class WhenBuilt`, `class WithUnlistedCompany`

### @DisplayName — 한국어 비즈니스 표현
모든 테스트 레벨에 `@DisplayName` 부착. **비즈니스 요구사항 관점**으로 작성.

| 레벨 | 표현 기준 |
|---|---|
| 아우터 클래스 | 검증 대상 도메인 영역 |
| `@Nested` 이너 클래스 | 시나리오·컨텍스트 |
| `@Test` 메서드 | 요구사항 한 줄 |

코드명(`Company`, `CorpCode`)·기술 용어(`null`, `entity`)는 가능한 자제. "회사", "DART 에서 받은 마스터 필드" 같은 비즈니스 표현 사용.

```java
@DisplayName("회사 마스터 도메인 생성")
class CompanyCreationUnitTest {

    @Nested
    @DisplayName("마스터 필드로 회사를 등록할 때")
    class WhenBuiltFromMasterFields {

        @Test
        @DisplayName("DART 에서 받은 마스터 필드 5개를 그대로 보존한다")
        void shouldHoldAllProvidedFields() { ... }

        @Test
        @DisplayName("비상장 회사도 등록할 수 있다 — 종목코드 없음 허용")
        void shouldAcceptNullStockCodeWithUnlistedCompany() { ... }
    }
}
```

## 검증 묶기

| 상황 | 쓰는 것 |
|---|---|
| 단일 값 검증 | `assertThat(x).isEqualTo(y)` |
| **한 객체의 여러 필드 묶음** | `assertThat(obj).extracting(...).containsExactly(...)` |
| 서로 다른 객체·상태를 한 시나리오에서 묶음 | `assertSoftly(softly -> { ... })` |

가장 간단한 쪽이 항상 우선. soft assertion 은 한 객체 필드 비교에는 쓰지 않음.

## 외부 의존
- 외부 API: 네트워크 안 탐. mock 또는 `src/test/resources/fixtures/...`의 고정 응답.
- 키 의존 클라이언트 (LLM, KIS 등): 키 미설정 시 NoOp 빈으로 대체 (`NoOpLlmClient` 패턴).

## 픽스처 파일
- 위치: `src/test/resources/fixtures/<도메인>/...`
- 크기 가이드:
  - 1MB 미만: 자유
  - 1~10MB: 거의 안 바뀌는 응답 1회분만 (예: `CORPCODE.zip`)
  - 10MB 이상: 별도 스토리지 고려 (git LFS 또는 외부)

## 단위 vs 통합 구분
- 도메인 모델·서비스 단위 테스트: 순수 JUnit, Spring 컨텍스트 안 띄움
- 영속화·웹 레이어: `@DataJpaTest` 또는 `@SpringBootTest` + Testcontainers
