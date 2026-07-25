# 트랜잭션 경계 룰

트랜잭션의 범위와 표현 방식을 정한다.

- 원칙: **경계의 범위는 업무적 원자 단위가 정하고, 경계의 결정은 유스케이스가 소유한다.**

## 1. 기본값: 유스케이스 메서드 = 트랜잭션 경계

- 규칙
  - 유스케이스가 업무적으로 원자적이면(대부분) 유스케이스 메서드에 경계를 둔다.
- 이유
  - 트랜잭션의 존재 이유가 "중간에 실패하면 전부 없던 일"인데, 그 범위를 정하는 건 기술이 아니라 업무다. 절반만 성공한 주문은 성립하지 않는다.

## 2. 예외: 업무 원자 단위가 유스케이스보다 작으면 그 단위로 내린다

- 규칙
  - 판단 질문: "이 작업이 중간에 실패하면 전부 없던 일이 되어야 하는가?" 아니오라면(대량 배치·스트리밍 동기화) 원자 단위(묶음)별로 커밋한다.
- 이유
  - 전체를 한 트랜잭션으로 묶으면 롱 트랜잭션(락·undo 부담)이 되고, 실패 시 멀쩡한 진행분까지 롤백된다. 멱등 저장(upsert)과 묶음 커밋의 조합이면 재실행이 이어달리기가 된다.

## 3. 유스케이스보다 작은 경계는 TransactionTemplate 으로 명시한다

- 규칙
  - `@Transactional` 을 별도 래퍼 빈으로 빼서 우회하지 않는다. 유스케이스가 `PlatformTransactionManager` 로 자신의 `TransactionTemplate` 을 구성하고(전파 속성 포함), 커밋 지점에서 명시적으로 호출한다.
- 이유
  - `@Transactional` 은 프록시를 거쳐야 작동해 자기 호출에서 무력화된다. 이를 래퍼 빈으로 우회하면 포트 모양의 가짜 협력자가 생겨 도메인 포트(진짜 계약)와 나란히 놓이고, 경계 결정이 유스케이스 밖으로 흩어진다.
  - Template 방식은 경계·전파 설정이 유스케이스 코드에 그대로 보이고, 프록시가 없으니 자기 호출 함정 자체가 없다.
  - Template 은 빈으로 빼지 않고 유스케이스 생성자에서 조립한다. 커밋 정책은 그 유스케이스의 지식이고, 생성자 조립이어야 단위 테스트가 프로덕션 정책(전파 속성)을 검증할 수 있다. 같은 정책을 쓰는 유스케이스가 둘 이상 생기면 그때 이름 있는 빈으로 승격한다.

Good (묶음 단위 커밋 — 유스케이스가 경계를 소유):
```java
this.batchTransaction = new TransactionTemplate(transactionManager);
this.batchTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
...
upsertedCount += batchTransaction.execute(status -> repository.upsertCompanies(batch));
```

Bad (트랜잭션 래퍼 빈 — 프록시 우회 흔적이 가짜 포트가 됨):
```java
public interface CompanyRegistryBatchWriter { int upsert(List<Company> companies); }

@Component
class TransactionalCompanyRegistryBatchWriter implements CompanyRegistryBatchWriter {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int upsert(List<Company> companies) { return repository.upsertCompanies(companies); }
}
```
