# 아키텍처 규칙

## 패키지 구조
```
dev.gukin.einvestlab
├── global                          // 전역 공통 (config, id, error)
└── company                         // 도메인 예
    ├── domain                      // 엔티티, Repository 인터페이스
    ├── application                 // 서비스, DTO
    ├── infra                       // DB/JPA, 외부 HTTP API 어댑터
    │   ├── db                      // DB/JPA 어댑터
    │   └── http                    // HTTP 기반 외부 API 어댑터
    └── interfaces                  // 컨트롤러, 스케줄러, 이벤트 리스너
```

각 도메인: `interfaces → application → domain ← infra`

기본은 평탄하게 둔다. 단, `infra` 는 기술 경계가 섞이면 `db/`, `http/` 까지만 나눌 수 있다.
`model/`, `persistence/`, `api/`, `mapper/`, `exception/` 같은 세부 하위 폴더는 만들지 않는다.

## 의존 방향
- **domain** → 어디에도 의존하지 않음 (단, JPA 어노테이션은 허용 — 엔티티 = 도메인 모델 컨벤션)
- **application** → domain만
- **infra / interfaces** → application, domain

## DO
- Repository 인터페이스 → `domain`, 구현체 → `infra/db`
- HTTP 기반 외부 API 클라이언트 → `infra/http`
- DTO → `application`
- 스케줄러, 이벤트 리스너 → `interfaces`

## 클래스 네이밍
- Repository 구현: `{Domain}{Tech}Repository` 순서
  - ✅ `CompanyJpaRepository`
  - ❌ `JpaCompanyRepository`
  - 이유: 도메인 기준 정렬·검색 친화. `Company`로 시작하는 클래스가 한 도메인 안에 모임.
- Repository 어댑터: `{Domain}RepositoryAdapter`
  - ✅ `CompanyRepositoryAdapter`
  - ❌ `CompanyRepositoryImpl`
  - 이유: `Impl` 은 구현 기술을 설명하지 못한다. 어댑터 역할을 이름에 드러낸다.
- 외부 공급원 포트: 출처보다 도메인 자원을 먼저 드러낸다.
  - ✅ `CompanyRegistrySource`
  - ❌ `CorpCodeClient`
  - 이유: 호출자는 DART 원문 용어보다 "무엇을 받는가" 를 먼저 알아야 한다.
- 외부 어댑터 구현체: `{Provider}{Resource}Adapter`
  - ✅ `DartCompanyRegistryAdapter`
  - ❌ `DartCorpCodeClient`
  - 이유: 구현체에서만 출처를 드러내고, 리소스 이름은 도메인 언어로 유지한다.
- 외부 API 설정: `{Provider}ApiProperties`
  - ✅ `DartApiProperties`
  - ❌ `DartProperties`
  - 이유: provider 전체 설정인지 API 접속 설정인지 구분한다.
- 외부 클라이언트 예외: `{Provider}ClientException`
  - ✅ `DartClientException`
  - ❌ `DartApiException`
  - 이유: HTTP/API 오류뿐 아니라 응답 스트림, 압축, 파싱 실패를 함께 감싼다.

## DON'T
- domain에서 Spring 어노테이션(@Service, @Component) 사용
- domain에서 infra 직접 참조
- 도메인 간 Entity 직접 참조 (ID로만)
- 도메인 간 직접 의존 (이벤트 또는 application 간 연동 사용)
