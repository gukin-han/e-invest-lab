-- 도메인 간 공용 아웃박스. 도메인 저장 트랜잭션에 이벤트 행이 합류해 "저장됨 = 알림 예정"의 원자성을 얻고,
-- 릴레이가 PENDING 을 폴링해 전송한다. 발행 측은 소비자를 모른다 — 행이 곧 계약.
-- 재시도 정책은 데이터로 표현: next_attempt_at 이 폴링 조건이라 백오프는 갱신 함수 하나로 중앙화.
-- 이력 보존(사후 분석) 위해 전송 후 삭제 대신 상태 마킹 — 상한 초과는 DEAD + last_error 로 남는다.

CREATE TABLE outbox_events (
    id              BINARY(16)   NOT NULL,
    event_type      VARCHAR(50)  NOT NULL,
    aggregate_id    VARCHAR(50)  NOT NULL,
    payload         JSON         NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    attempt_count   INT          NOT NULL,
    last_error      VARCHAR(500) NULL,
    next_attempt_at TIMESTAMP(6) NOT NULL,
    created_at      TIMESTAMP(6) NOT NULL,
    sent_at         TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    KEY idx_outbox_events_polling (status, next_attempt_at)
) ENGINE = InnoDB;
