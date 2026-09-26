# CLAUDE.md

## Project Overview
Spring Boot 4.0.2 / Java 21 / Gradle (Kotlin DSL)
Spring Data JPA + MySQL + Spring Web MVC + Lombok

## Repository Layout
루트는 제품이고, `backend`/`frontend` 는 각자 자기 빌드 시스템을 가진 형제다.

```
backend/    Gradle 프로젝트 (gradlew, settings.gradle.kts 가 이 안에 있다)
frontend/   npm 프로젝트 (Vite + React). Gradle 모듈이 아니다
docs/       rules, specs, adr
Dockerfile  frontend 빌드 → backend 빌드 → 런타임 3단계
```

## Build Commands
Gradle 명령은 `backend/` 안에서 실행한다.
```bash
cd backend
./gradlew build          # 빌드 + 테스트
./gradlew bootRun        # 실행 (port 8080)
./gradlew test           # 테스트만
./gradlew clean build    # 클린 빌드
./gradlew test --tests "dev.gukin.einvestlab.SomeTest"  # 단일 테스트
```

프론트는 별도 세계다.
```bash
cd frontend
npm run dev              # Vite 개발 서버, /api 는 :8080 으로 프록시
npm run build
```

## Architecture
- 패키지 루트: `dev.gukin.einvestlab`
- 진입점: `EInvestLabApplication.java`
- 설정: `backend/src/main/resources/application.yml`
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
