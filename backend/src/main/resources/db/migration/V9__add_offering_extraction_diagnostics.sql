-- 가드 판정 사유와 실패 시 LLM 원출력을 영속화 — 실패 분석과 가드 개선 후 재검증(LLM 재호출 없이)을 무료로.
-- note: 가드 이슈 요약 (성공 시 교정 내역, 실패 시 실패 사유). drafts: 실패 건의 LLM 출력 JSON (성공 시 NULL — offerings 에 이미 반영).

ALTER TABLE business_contents
    ADD COLUMN offering_extraction_note TEXT NULL,
    ADD COLUMN offering_extraction_drafts JSON NULL;
