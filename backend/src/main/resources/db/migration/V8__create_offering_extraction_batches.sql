-- OpenAI Batch API 작업 추적. 제출-수거 분리 구조의 장부:
-- SUBMITTED = 제출됨(수거 대상), COLLECTED = 결과 반영 완료, FAILED = 배치 자체 실패(만료·검증 실패).
-- 건별 추출 성패는 여기가 아니라 business_contents.offering_extraction_status 가 담당.

CREATE TABLE offering_extraction_batches (
    id                BINARY(16)   NOT NULL,
    provider_batch_id VARCHAR(100) NOT NULL,
    model             VARCHAR(50)  NOT NULL,
    request_count     INT          NOT NULL,
    status            VARCHAR(30)  NOT NULL,
    submitted_at      TIMESTAMP(6) NOT NULL,
    collected_at      TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_offering_extraction_batches_provider_batch_id (provider_batch_id)
) ENGINE = InnoDB;
