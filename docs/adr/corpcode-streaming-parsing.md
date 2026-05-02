# DART corpCode.xml 스트리밍 파싱 결정

## ZipInputStream + StAX 정거장 체인 채택
- **날짜**: 2026-05-02
- **상태**: 채택 (스모크 테스트로 검증 완료)

### 컨텍스트
- DART `corpCode.xml` API: 회사 마스터 전체 (~117k 회사) 일괄 다운로드
- 응답: zip 3.6MB → 풀린 XML 약 30MB
- 일 단위 동기화 (전체 갱신) — 매일 처리해야 하는 양

### 후보 비교

| 방식 | 메모리 | 비고 |
|---|---|---|
| **DOM 파싱** (`DocumentBuilder`) | 30MB XML이 객체 트리로 200MB+ 점유 | ❌ 큰 파일에 부적합 |
| **JAXB unmarshal** | DOM 유사 — 전체 객체 그래프 메모리 적재 | ❌ 동일 한계 |
| **StAX 스트리밍** (`XMLStreamReader`) | KB 수준의 정거장 체인 | ✅ 채택 |

### 결정 — 정거장 체인 모델

```
HttpResponse
  → ZipInputStream      (압축 해제, 디스크 안 거침)
  → XMLStreamReader     (한 <list> 단위로 파싱하고 GC)
  → 도메인 객체 (CompanyRow)
  → 즉시 처리 또는 배치 버퍼 (1,000건)
```

각 단계가 **작은 버퍼만 들고 다음 단계로 흘려보냄**. 누적되지 않음.

### 핵심 사고 — 정거장 vs 창고

```
창고 모델 (DOM): 모든 데이터가 들어와서 쌓임 → 메모리 = 데이터 크기
정거장 모델 (StAX): 데이터가 통과만 함 → 메모리 = 정거장 크기 합
```

처리량(throughput)과 동시 메모리 점유량(memory footprint)이 **분리됨**. 117k 행 처리하든 1억 행 처리하든 메모리는 같음.

### 주요 API 패턴

**1. ZipInputStream — `getNextEntry()` 명시 호출 필수**
- `ZipInputStream.read()`만 호출하면 `-1` 반환
- 반드시 `getNextEntry()` 호출해서 entry 진입 후 read 가능
- archive 포맷 특성 (gzip은 transparent, zip은 명시적 entry 이동)

**2. StAX — 이벤트 기반 커서**
- `reader.next()` = 다음 의미 있는 노드로 이동 + 이벤트 종류 반환
- 핵심 이벤트: `START_ELEMENT`, `END_ELEMENT`, `CHARACTERS`, `END_DOCUMENT`
- 우리 코드는 `START_ELEMENT` + `END_ELEMENT(list)`만 명시 처리, 나머진 자연 통과
- `getElementText()` — 단순 element의 텍스트를 한 번에 추출 + 커서를 종료 태그 다음으로 자동 이동

**3. 자원 관리**
- `try-with-resources`: `InputStream body`, `ZipInputStream zipStream` (둘 다 AutoCloseable)
- `XMLStreamReader`는 AutoCloseable 미구현 → 별도 `try-finally`로 close
  - 또는 `record AutoCloseableXmlReader(XMLStreamReader inner) implements AutoCloseable {...}` 같은 wrapper로 try-with-resources 가능

### Pull-based + 자동 backpressure

자바 `InputStream` 체인은 **소비자가 끌어오는** 모양.
- 소비자가 read 안 하면 그 위 모든 층이 대기
- TCP 소켓 수신 버퍼 차면 → ACK에 window=0 → 송신자(DART) 자동 멈춤
- **별도 backpressure 코드 없이도 가장 느린 노드에 전체가 정렬됨**

push 기반 시스템(Reactor 등)과 대비되는 장점.

### 검증 실험 (2026-05-02)

**조건:** `-Xms32m -Xmx32m`, 117k 회사 전체 처리 (스모크 테스트의 5개 break 제거)

**결과:**
```
processed=5000   heap=26MB / 32MB
processed=10000  heap=24MB / 32MB
processed=15000  heap=20MB / 32MB
...
processed=110000 heap=24MB / 32MB
processed=115000 heap=19MB / 32MB
총 처리: 117,496
```

**해석:**
- heap이 19~26MB 범위에서 **카운트와 무관하게 등락**
- GC가 임시 객체(CompanyRow, String 등) 주기적으로 회수
- 32MB 한도 안에서 117k 행 처리 완료 → **스트리밍 작동 확정**

만약 통째 적재였다면 30MB 데이터 + 객체 오버헤드(5~10배) = 150~300MB 필요 → OOM.

### 적용 — 운영 코드 동기화 흐름

```
매일 cron (새벽 1시) →
  corpCode.xml 다운로드 (3.6MB zip 그대로) →
  ZipInputStream + StAX 스트리밍 파싱 →
  modify_date 변경분만 골라서 (증분 갱신) →
  배치 1,000건씩 묶어 JdbcTemplate.batchUpdate
```

이 흐름에서 메모리 일정 보장 — 117k 행이든 1M 행이든 동일한 footprint.

### 다른 외부 데이터에 적용 가능

같은 정거장 체인 사고가 적용되는 케이스:

| 포맷 | 변경 지점 |
|---|---|
| CSV | StAX → `BufferedReader.readLine()` 또는 OpenCSV streaming |
| JSON Lines | StAX → Jackson `JsonParser` (streaming mode) |
| **xlsx** (OOXML) | StAX 그대로 + Apache POI `XSSFReader` (sheet1.xml 위치) |
| Parquet | 압축/포맷 파서만 교체 |

`xlsx`도 사실 zip 안의 XML들이라 본질적으로 같은 패턴.

### 모니터링 (운영 시)

힙 사용량 일정성 검증을 운영에도 적용:

```java
if (count % 5000 == 0) {
    log.info("processed={} heap={}MB",
             count,
             (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024);
}
```

운영 로그에서 heap이 시간 따라 선형 증가하면 누적 발생 → 회귀 즉시 감지.

### 참고

- 스펙 문서: [docs/specs/company.md](../specs/company.md) — 응답 구조, 변경 빈도 분류, 적재 전략
- HTTP 클라이언트 선택: [http-client.md](./http-client.md) — RestClient는 JSON 객체용, 우리는 zip 스트리밍이라 JDK `HttpClient.send(... BodyHandlers.ofInputStream())` 사용
