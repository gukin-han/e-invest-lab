-- PDF 롤링 보존(발행일 기준 1년). GC 는 파일을 지우고 pdf_path 를 비우는데, "파일 없음 = 미다운로드" 규칙 그대로면
-- 다음 배치가 전부 다시 받는다. 그래서 "지웠음"을 별도 상태로 남긴다 — pdf_purged_at 이 있으면 다운로드·추출 대상에서 빠진다.
-- 정형 행(리포트 메타·EPS 추정치)은 지우지 않는다. 지우는 건 재추출 원료(PDF)뿐이다.

ALTER TABLE analyst_reports
    ADD COLUMN pdf_purged_at TIMESTAMP(6) NULL,
    ADD KEY idx_analyst_reports_published_date (published_date);
