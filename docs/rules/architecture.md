# 아키텍처 규칙

## 패키지 구조
```
dev.gukin.einvestlab
├── global                          // 전역 공통 (config, id, error)
└── company                         // 도메인 예
    ├── domain                      // 엔티티, Repository 인터페이스
    ├── application                 // 서비스, DTO
    ├── infra                       // Repository 구현, 외부 API 클라이언트
    └── interfaces                  // 컨트롤러, 스케줄러, 이벤트 리스너
```

각 도메인: `interfaces → application → domain ← infra`

**내부 세분화(`model/`, `persistence/`, `api/` 같은 하위 폴더) 안 함**. 평탄하게.
필요해지면 그때 도입.

## 의존 방향
- **domain** → 어디에도 의존하지 않음 (단, JPA 어노테이션은 허용 — 엔티티 = 도메인 모델 컨벤션)
- **application** → domain만
- **infra / interfaces** → application, domain

## DO
- Repository 인터페이스 → `domain`, 구현체 → `infra`
- 외부 API 클라이언트 → `infra`
- DTO → `application`
- 스케줄러, 이벤트 리스너 → `interfaces`

## 클래스 네이밍
- Repository 구현: `{Domain}{Tech}Repository` 순서
  - ✅ `CompanyJpaRepository`
  - ❌ `JpaCompanyRepository`
  - 이유: 도메인 기준 정렬·검색 친화. `Company`로 시작하는 클래스가 한 도메인 안에 모임.

## DON'T
- domain에서 Spring 어노테이션(@Service, @Component) 사용
- domain에서 infra 직접 참조
- 도메인 간 Entity 직접 참조 (ID로만)
- 도메인 간 직접 의존 (이벤트 또는 application 간 연동 사용)
