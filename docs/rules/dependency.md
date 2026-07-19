# 의존성 선언 룰

Gradle 의존성을 **어떤 configuration(버킷)에, 어떤 종류로** 선언할지 정한다.

- 버킷 선택 = "언제 필요한가 / 누가 보는가"
- 종류 선택 = "코드 jar인가, 버전 명세(BOM)인가"

## 1. configuration은 필요 시점과 노출 범위로 고른다

- 규칙

| 버킷 | 컴파일 시 | 런타임 시 | 다운스트림 노출 | 용도 / 예시 |
|---|---|---|---|---|
| implementation | 필요 | 필요 | 숨김 | 기본값. `spring-boot-starter-data-jpa` |
| api | 필요 | 필요 | 노출 | 공개 API에 타입이 드러날 때(java-library 필요) |
| compileOnly | 필요 | 불필요 | 숨김 | 컴파일에만. `lombok` |
| runtimeOnly | 불필요 | 필요 | — | 코드에서 직접 안 부름. `mysql-connector-j` |
| annotationProcessor | 코드 생성 | 불필요 | — | 처리기 등록. `lombok` |
| testImplementation | 테스트만 | 테스트만 | — | 테스트 전용. `testcontainers:mysql` |

- 이유
  - 버킷이 필요 시점을 좁히면 런타임 산출물과 빌드 클래스패스가 가벼워진다.
  - 노출 범위를 좁히면(implementation) 내부 구현 변경이 다운스트림 재컴파일로 번지지 않는다.

## 2. implementation을 기본으로, 공개 API에 드러날 때만 api

- 규칙
  - 기본은 `implementation` -> 의존성이 모듈 안에 갇혀 바깥 모듈이 못 본다.
  - 반환 타입·파라미터 등 공개 API에 그 라이브러리 타입이 드러나면 `api`.
- 이유
  - `implementation`은 결합도를 낮추고 incremental 빌드를 빠르게 한다.
  - 멀티모듈에서 이 구분이 빌드 속도와 모듈 경계를 가른다.

## 3. 코드에서 직접 부르지 않는 건 runtimeOnly

- 규칙
  - JDBC 드라이버처럼 추상화(JPA/JDBC) 뒤에 있어 컴파일에 안 쓰는 건 `runtimeOnly`.
- 이유
  - 컴파일 클래스패스에서 빼면 드라이버 클래스를 직접 부르는 실수를 막는다.

Good:
```kotlin
runtimeOnly("com.mysql:mysql-connector-j")
```

## 4. 애너테이션 처리기는 compileOnly와 annotationProcessor 짝으로 둔다

- 규칙
  - Lombok 등은 컴파일 때 타입·애너테이션이 필요(`compileOnly`) + 코드 생성을 위해 처리기 등록(`annotationProcessor`).
  - 런타임엔 불필요하므로 runtime 버킷에 넣지 않는다.
- 이유
  - 생성 결과만 산출물에 남기고 처리기 자체는 빼서 런타임을 가볍게 한다.

Good:
```kotlin
compileOnly("org.projectlombok:lombok")
annotationProcessor("org.projectlombok:lombok")
```

## 5. BOM은 platform()으로 들이고 버전은 BOM에 맡긴다

- 규칙
  - `platform(...)`은 좌표를 "코드 jar"가 아니라 "BOM(버전 제약)"으로 해석시키는 표시다.
  - BOM은 코드가 없다 -> 들여와도 jar는 안 끌려오고, 이름을 직접 부른 의존성만 버전을 채워 받는다.
  - BOM이 관리하는 묶음은 버전을 직접 박지 않는다. 업그레이드는 BOM 버전 한 줄로 끝낸다.
  - Spring Boot 플러그인은 Boot BOM을 자동 임포트 -> 대부분의 spring 의존성에 버전을 안 적는 이유.
- 이유
  - 한 제품이 여러 artifact로 쪼개져도 버전 정렬을 한곳에서 보장한다.
  - 전이 의존성 버전 충돌 시 BOM이 통일 기준이 된다.

Bad:
```kotlin
implementation("org.springframework.ai:spring-ai-starter-mcp-server-webmvc:2.0.0")
implementation("org.springframework.ai:spring-ai-core:1.1.0") // 버전 손수 핀 -> 어긋남 위험
```

Good:
```kotlin
implementation(platform("org.springframework.ai:spring-ai-bom:2.0.0"))
implementation("org.springframework.ai:spring-ai-starter-mcp-server-webmvc") // 버전은 BOM이 채움
```
