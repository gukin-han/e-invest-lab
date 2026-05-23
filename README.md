# e-invest-lab

> 한국 상장사 공시·재무·시장 데이터를 자동 수집·분석해 개인 투자자에게도 기관 수준의 분석을 제공하는 개인 학습·연구 프로젝트

매일 쏟아지는 주식 공시를 사람이 일일이 읽지 않아도 되도록, DART API에서 공시를 수집하고 LLM으로 주가 영향도를 분석한다.

## 기술 아티클

- [HikariCP 커넥션 고갈 추적기](https://gukin-han.tistory.com/67) — Virtual Thread 700개가 풀 10개에 막힌 문제. HikariCP 소스 분석으로 원인을 파악하고 트랜잭션 경계 분리로 해결.
- [Virtual Thread 도입](https://gukin-han.tistory.com/55) — Virtual Thread 도입 배경과 구조 변경 과정.

## 실험 보고서

- [스트리밍 메모리 footprint 측정 (V3)](docs/adr/streaming-memory-experiment/README.md) — DART corpCode.xml(30MB ~ 3GB) 스트리밍 파싱 + MySQL upsert 흐름의 메모리 footprint를 5개 축으로 sweep. 정량 결과: `footprint ≈ 0.0025MB/row × batch + 2.2MB(JDBC)`. 100배 큰 합성 zip(3GB XML, 11.8M rows)도 -Xmx64m에서 처리됨을 확인.

## 기술 스택

- **Backend**: Java 21, Spring Boot, JPA, Virtual Thread
- **DB**: MySQL, HikariCP
- **외부 연동**: DART Open API, OpenAI API

## 로컬 실행

```bash
docker compose up -d
./gradlew bootRun
```
