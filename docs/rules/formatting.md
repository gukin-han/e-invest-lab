# 포맷팅 룰

## 1. 기본 포맷은 .editorconfig를 따른다

- 규칙
  - 기준 파일: `.editorconfig`
  - 공백: space 4칸
  - Java continuation indent: 8칸
  - wildcard import 금지
- 이유
  - IDE와 개발자 환경이 달라도 같은 포맷을 유지한다.
  - 포맷 차이로 생기는 diff 노이즈를 줄인다.

Bad:
```java
import java.util.*;

class Company {
  private String name;
}
```

Good:
```java
import java.util.List;

class Company {
    private String name;
}
```

## 2. IDE 저장 시 자동 포맷을 적용한다

- 규칙
  - IntelliJ IDEA의 EditorConfig support를 활성화한다.
  - Actions on Save에서 Reformat code를 활성화한다.
  - Actions on Save에서 Optimize imports를 활성화한다.
- 이유
  - 포맷팅을 수동 작업으로 두면 누락되기 쉽다.
  - 저장 시점에 정리하면 리뷰에서는 코드 의미에 집중할 수 있다.

Bad:
```text
Actions on Save
- Reformat code: off
- Optimize imports: off
```

Good:
```text
Actions on Save
- Reformat code: on
- Optimize imports: on
```

## 3. Gradle 포맷 태스크는 팀 또는 CI 단계에서 도입한다

- 규칙
  - 현재 단계는 IDE 자동 포맷과 `.editorconfig` 기준으로 관리한다.
  - 필요 시 Spotless 같은 Gradle formatter를 도입한다.
  - 팀/CI 단계 진입 시 `format`, `formatCheck` 태스크를 추가한다.
- 이유
  - 초기에는 설정 비용보다 개발 속도가 중요하다.
  - 여러 사람이 같은 코드를 수정하거나 CI 검증이 필요해지면 자동 검증 태스크가 필요하다.

Bad:
```kotlin
// 기준 없이 개발자마다 IDE 포맷만 다르게 사용
```

Good:
```kotlin
// 현재 단계
// .editorconfig + IDE 자동 포맷

// 팀/CI 단계
// ./gradlew formatCheck
```
