# 포맷팅 룰

## 기본
- 기준 파일: `.editorconfig`
- 공백: space 4칸
- Java continuation indent: 8칸
- wildcard import 금지

## IDE 자동 포맷
- IntelliJ IDEA: EditorConfig support 활성화
- Actions on Save:
  - Reformat code
  - Optimize imports

## Gradle 강제 포맷
- 필요 시 Spotless 같은 Gradle formatter 도입
- 현재 단계: IDE 자동 포맷 + `.editorconfig` 기준
- 팀/CI 단계 진입 시 `format` / `formatCheck` 태스크 추가
