# AGENTS.md

## Project Overview
Spring Boot 4.0.2 / Java 21 / Gradle (Kotlin DSL)
Spring Data JPA + MySQL + Spring Web MVC + Lombok

## Build Commands
```bash
./gradlew build          # 빌드 + 테스트
./gradlew bootRun        # 실행 (port 8080)
./gradlew test           # 테스트만
./gradlew clean build    # 클린 빌드
./gradlew test --tests "dev.gukin.einvestlab.SomeTest"  # 단일 테스트
```

## Architecture
- 패키지 루트: `dev.gukin.einvestlab`
- 진입점: `EInvestLabApplication.java`
- 설정: `src/main/resources/application.yml`
- DB: MySQL 8.0 (Docker), 테스트: Testcontainers MySQL
- 테스트: JUnit 5 + `@SpringBootTest`

## Documentation
- `docs/rules/` — 컨벤션, 규칙
- `docs/specs/` — 도메인 스펙
- `docs/adr/` — 기술 결정 기록

### 문서 최신화 규칙
- 기술 결정이 포함된 대화 → `docs/adr/`에 반영
- 컨벤션/규칙 관련 대화 → `docs/rules/`에 반영
- 도메인 구조 변경 대화 → `docs/specs/`에 반영

## Key Dependencies
- Spring Boot 4.0.2 (web, data-jpa)
- MySQL 8.0 (runtime) + Testcontainers (test)
- Lombok (compile-only)
- Gradle 9.4.1
