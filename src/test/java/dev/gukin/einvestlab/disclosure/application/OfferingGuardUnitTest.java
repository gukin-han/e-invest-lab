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

    private OfferingDraft dupShare(String share) {
        return new OfferingDraft(null, "DX", null, List.of("TV"), null, null, "매출액",
                new BigDecimal(share), List.of(), null, 2025);
    }
}
