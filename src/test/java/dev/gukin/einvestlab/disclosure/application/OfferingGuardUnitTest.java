package dev.gukin.einvestlab.disclosure.application;

import dev.gukin.einvestlab.disclosure.domain.OfferingDraft;
import dev.gukin.einvestlab.disclosure.domain.OfferingExtractionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Offering 검증 가드 단위 테스트")
class OfferingGuardUnitTest {

    private static final String CONTENT = """
            DX부문은 TV, 모니터, 냉장고 등을 판매하며 매출액은 3조 8,542억원으로 전체의 42.8%를 차지한다.
            DS부문은 DRAM, NAND Flash 등을 생산하며 매출액 2조 1,000억원(23.3%)을 기록했다.
            주요 매출처는 Apple, Tesla 등이다.
            """;

    private final OfferingGuard guard = new OfferingGuard();

    @Test
    @DisplayName("모든 값이 원문에 실재하면 통과(EXTRACTED)다")
    void shouldPassWhenAllEvidencePresent() {
        OfferingGuardVerdict verdict = guard.verify(List.of(
                draft("DX", List.of("TV", "모니터"), "38542", "억원", "매출액", "42.8", List.of("Apple")),
                draft("DS", List.of("DRAM"), "21000", "억원", "매출액", "23.3", List.of())), CONTENT);

        assertThat(verdict.status()).isEqualTo(OfferingExtractionStatus.EXTRACTED);
        assertThat(verdict.accepted()).hasSize(2);
    }

    @Test
    @DisplayName("조 단위 표기 원문(3조 8,542억원)과 억원 환산 명시값(38542)을 대조로 잇는다")
    void shouldMatchTrillionNotationAgainstFlatAmount() {
        OfferingGuardVerdict verdict = guard.verify(List.of(
                draft("DX", List.of("TV"), "38542", "억원", "매출액", null, List.of())), CONTENT);

        assertThat(verdict.passed()).isTrue();
    }

    @Test
    @DisplayName("합계류 행은 제거하고 교정 통과(CORRECTED)로 판정한다")
    void shouldDropTotalRowsAsCorrection() {
        OfferingGuardVerdict verdict = guard.verify(List.of(
                draft("DX", List.of("TV"), null, null, "매출액", null, List.of()),
                draft("합계", List.of(), null, null, "매출액", null, List.of())), CONTENT);

        assertThat(verdict.status()).isEqualTo(OfferingExtractionStatus.CORRECTED);
        assertThat(verdict.accepted()).hasSize(1);
        assertThat(verdict.issues()).anySatisfy(issue -> assertThat(issue).contains("합계류"));
    }

    @Test
    @DisplayName("basis 어휘 표류(매출/매출실적)는 매출액으로 정규화하고 교정으로 판정한다")
    void shouldNormalizeBasisDrift() {
        OfferingGuardVerdict verdict = guard.verify(List.of(
                draft("DX", List.of("TV"), null, null, "매출실적", null, List.of())), CONTENT);

        assertThat(verdict.status()).isEqualTo(OfferingExtractionStatus.CORRECTED);
        assertThat(verdict.accepted().getFirst().revenueBasis()).isEqualTo("매출액");
    }

    @Test
    @DisplayName("원문에 없는 수치·이름(환각 의심)은 실패로 판정한다")
    void shouldFailOnEvidenceMissingFromSource() {
        OfferingGuardVerdict hallucinatedAmount = guard.verify(List.of(
                draft("DX", List.of("TV"), "99999", "억원", "매출액", null, List.of())), CONTENT);
        OfferingGuardVerdict hallucinatedProduct = guard.verify(List.of(
                draft("DX", List.of("세탁기"), null, null, "매출액", null, List.of())), CONTENT);

        assertThat(hallucinatedAmount.status()).isEqualTo(OfferingExtractionStatus.FAILED);
        assertThat(hallucinatedProduct.status()).isEqualTo(OfferingExtractionStatus.FAILED);
    }

    @Test
    @DisplayName("기준(basis)이 다른 100% 파이 여러 개는 각자 그룹으로 세어 통과시킨다 — 금융 다중 구성비 표")
    void shouldGroupShareSumsByRevenueBasis() {
        String financialContent = """
                수신 구성은 예수금 60.0%, 적금 40.0%이다.
                대출채권 구성은 신용대출 70.0%, 담보대출 30.0%이다.
                """;
        OfferingGuardVerdict verdict = guard.verify(List.of(
                basisShare("수신", "예수금", "60.0"),
                basisShare("수신", "적금", "40.0"),
                basisShare("대출채권", "신용대출", "70.0"),
                basisShare("대출채권", "담보대출", "30.0")), financialContent);

        assertThat(verdict.status()).isEqualTo(OfferingExtractionStatus.EXTRACTED);
        assertThat(verdict.accepted()).hasSize(4);
    }

    @Test
    @DisplayName("같은 그룹의 비중 합이 115를 넘으면 실패로 판정한다")
    void shouldFailWhenShareSumExceedsLimit() {
        OfferingGuardVerdict verdict = guard.verify(List.of(
                draft("DX", List.of("TV"), null, null, "매출액", "42.8", List.of()),
                draft("DS", List.of("DRAM"), null, null, "매출액", "23.3", List.of()),
                dupShare("42.8"), dupShare("23.3")), CONTENT);

        assertThat(verdict.status()).isEqualTo(OfferingExtractionStatus.FAILED);
        assertThat(verdict.issues()).anySatisfy(issue -> assertThat(issue).contains("비중 합"));
    }

    @Test
    @DisplayName("의역 제품명은 원문 앵커 세그먼트가 있으면 교정 통과(CORRECTED)로 수용한다")
    void shouldAcceptParaphrasedProductWithAnchorSegment() {
        OfferingGuardVerdict verdict = guard.verify(List.of(
                draft("DX", List.of("모니터(사무용 및 게이밍 라인업)"), null, null, "매출액", null, List.of())), CONTENT);

        assertThat(verdict.status()).isEqualTo(OfferingExtractionStatus.CORRECTED);
        assertThat(verdict.issues()).anySatisfy(issue -> assertThat(issue).contains("부분 근거"));
    }

    @Test
    @DisplayName("모든 세그먼트가 원문에 없는 제품명은 여전히 실패다")
    void shouldFailWhenNoSegmentAnchorsToSource() {
        OfferingGuardVerdict verdict = guard.verify(List.of(
                draft("DX", List.of("세탁기(드럼, 통돌이)"), null, null, "매출액", null, List.of())), CONTENT);

        assertThat(verdict.status()).isEqualTo(OfferingExtractionStatus.FAILED);
    }

    @Test
    @DisplayName("회계 음수 표기 (567,779)·△16.2 를 음수 값의 근거로 인정한다")
    void shouldMatchAccountingNegativeNotation() {
        String accountingContent = "해외법인 매출은 (567,779)백만원, 비중은 △16.2%로 감소했다. 라면 사업이 주력이다.";
        OfferingGuardVerdict verdict = guard.verify(List.of(
                draft("해외", List.of("라면"), "-567779", "백만원", "매출액", "-16.2", List.of())), accountingContent);

        assertThat(verdict.passed()).isTrue();
    }

    @Test
    @DisplayName("구분 차원이 비어 합쳐진 100% 파이 여러 개(합이 100의 배수 근처)는 교정 통과로 수용한다")
    void shouldAcceptCollapsedMultiplePies() {
        String yearlessContent = """
                당기 서치플랫폼 60.0%, 커머스 40.0%.
                전기 서치플랫폼 55.0%, 커머스 45.0%.
                """;
        OfferingGuardVerdict verdict = guard.verify(List.of(
                yearlessShare("서치플랫폼", "60.0"), yearlessShare("커머스", "40.0"),
                yearlessShare("서치플랫폼", "55.0"), yearlessShare("커머스", "45.0")), yearlessContent);

        assertThat(verdict.status()).isEqualTo(OfferingExtractionStatus.CORRECTED);
        assertThat(verdict.issues()).anySatisfy(issue -> assertThat(issue).contains("복수 파이"));
    }

    @Test
    @DisplayName("빈 결과와 합계 제거 후 빈 결과는 실패다")
    void shouldFailOnEmptyResults() {
        assertThat(guard.verify(List.of(), CONTENT).status())
                .isEqualTo(OfferingExtractionStatus.FAILED);
        assertThat(guard.verify(List.of(
                draft("합계", List.of(), null, null, null, null, List.of())), CONTENT).status())
                .isEqualTo(OfferingExtractionStatus.FAILED);
    }

    private OfferingDraft draft(String segment, List<String> products, String amount, String unit,
                                String basis, String share, List<String> customers) {
        return new OfferingDraft(null, segment, null, products,
                amount == null ? null : new BigDecimal(amount), unit, basis,
                share == null ? null : new BigDecimal(share), customers, null, 2025);
    }

    private OfferingDraft basisShare(String basis, String product, String share) {
        return new OfferingDraft(null, null, null, List.of(product), null, null, basis,
                new BigDecimal(share), List.of(), null, 2025);
    }

    private OfferingDraft dupShare(String share) {
        return new OfferingDraft(null, "DX", null, List.of("TV"), null, null, "매출액",
                new BigDecimal(share), List.of(), null, 2025);
    }

    private OfferingDraft yearlessShare(String segment, String share) {
        return new OfferingDraft(null, segment, null, List.of(), null, null, "영업수익",
                new BigDecimal(share), List.of(), null, null);
    }
}
