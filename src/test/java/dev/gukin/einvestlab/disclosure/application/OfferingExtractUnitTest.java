package dev.gukin.einvestlab.disclosure.application;

import dev.gukin.einvestlab.global.config.OfferingExtractionProperties;
import dev.gukin.einvestlab.global.id.Ids;
import dev.gukin.einvestlab.disclosure.domain.BusinessContent;
import dev.gukin.einvestlab.disclosure.domain.BusinessContentRepository;
import dev.gukin.einvestlab.disclosure.domain.BusinessContentSlicer;
import dev.gukin.einvestlab.disclosure.domain.Offering;
import dev.gukin.einvestlab.disclosure.domain.OfferingDraft;
import dev.gukin.einvestlab.disclosure.domain.OfferingExtractionException;
import dev.gukin.einvestlab.disclosure.domain.OfferingExtractionStatus;
import dev.gukin.einvestlab.disclosure.domain.OfferingExtractor;
import dev.gukin.einvestlab.disclosure.domain.OfferingRepository;
import dev.gukin.einvestlab.support.RecordingTransactionManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Offering 추출 유스케이스 단위 테스트")
class OfferingExtractUnitTest {

    private static final Instant BASE_TIME = Instant.parse("2026-08-08T03:00:00Z");
    private static final String CONTENT = "DX부문은 TV를 판매하며 매출액 3조 8,542억원(42.8%)을 기록했다.";

    private final StubContentRepository contentRepository = new StubContentRepository();
    private final StubOfferingRepository offeringRepository = new StubOfferingRepository();
    private final StubExtractor extractor = new StubExtractor();
    private final OfferingResultRecorder recorder = new OfferingResultRecorder(
            contentRepository, offeringRepository, new RecordingTransactionManager());
    private final OfferingExtractUseCase useCase = new OfferingExtractUseCase(
            contentRepository, recorder, content -> content, extractor, new OfferingGuard(),
            new OfferingExtractionProperties(List.of("mini", "full")));

    @Test
    @DisplayName("1차 모델이 가드를 통과하면 교체 저장하고 상태를 기록한다")
    void shouldSaveWhenFirstModelPasses() {
        contentRepository.pending = List.of(content("F1"));
        extractor.results.put("mini", List.of(goodDraft()));

        OfferingExtractResult result = useCase.extractAll(BASE_TIME);

        assertThat(result).isEqualTo(new OfferingExtractResult(1, 0, 0, 0));
        assertThat(offeringRepository.deletedFilingNumbers).containsExactly("F1");
        assertThat(offeringRepository.saved)
                .extracting(Offering::getFilingNumber, Offering::getCorpCode, Offering::getExtractedAt)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("F1", "00126380", BASE_TIME));
        assertThat(contentRepository.saved.getFirst().getOfferingExtractionStatus())
                .isEqualTo(OfferingExtractionStatus.EXTRACTED);
    }

    @Test
    @DisplayName("1차가 가드에 걸리면 상위 모델로 에스컬레이션한다")
    void shouldEscalateWhenFirstModelFailsGuard() {
        contentRepository.pending = List.of(content("F1"));
        extractor.results.put("mini", List.of(hallucinatedDraft()));
        extractor.results.put("full", List.of(goodDraft()));

        OfferingExtractResult result = useCase.extractAll(BASE_TIME);

        assertThat(result).isEqualTo(new OfferingExtractResult(1, 0, 0, 1));
        assertThat(extractor.calledModels).containsExactly("mini", "full");
    }

    @Test
    @DisplayName("호출 예외가 나면 다음 모델을 시도하고, 전부 실패하면 FAILED 로 기록한다")
    void shouldRecordFailureWhenAllModelsFail() {
        contentRepository.pending = List.of(content("F1"));
        extractor.failingModels.add("mini");
        extractor.results.put("full", List.of(hallucinatedDraft()));

        OfferingExtractResult result = useCase.extractAll(BASE_TIME);

        assertThat(result).isEqualTo(new OfferingExtractResult(0, 0, 1, 1));
        assertThat(offeringRepository.saved).isEmpty();
        assertThat(contentRepository.saved.getFirst().getOfferingExtractionStatus())
                .isEqualTo(OfferingExtractionStatus.FAILED);
    }

    private BusinessContent content(String filingNumber) {
        return BusinessContent.builder()
                .id(Ids.generate())
                .corpCode("00126380")
                .filingNumber(filingNumber)
                .filedDate(LocalDate.of(2026, 3, 10))
                .content(CONTENT)
                .collectedAt(BASE_TIME)
                .build();
    }

    private OfferingDraft goodDraft() {
        return new OfferingDraft(null, "DX", null, List.of("TV"),
                new BigDecimal("38542"), "억원", "매출액", new BigDecimal("42.8"),
                List.of(), null, 2025);
    }

    private OfferingDraft hallucinatedDraft() {
        return new OfferingDraft(null, "DX", null, List.of("세탁기"),
                null, null, "매출액", null, List.of(), null, 2025);
    }

    private static class StubContentRepository implements BusinessContentRepository {

        private List<BusinessContent> pending = List.of();
        private final List<BusinessContent> saved = new ArrayList<>();

        @Override
        public BusinessContent save(BusinessContent businessContent) {
            saved.add(businessContent);
            return businessContent;
        }

        @Override
        public boolean existsByFilingNumber(String filingNumber) {
            return false;
        }

        @Override
        public List<BusinessContent> findAllPendingOfferingExtraction() {
            return pending;
        }

        @Override
        public java.util.Optional<BusinessContent> findByFilingNumber(String filingNumber) {
            return pending.stream()
                    .filter(content -> content.getFilingNumber().equals(filingNumber))
                    .findFirst();
        }
    }

    private static class StubOfferingRepository implements OfferingRepository {

        private final List<Offering> saved = new ArrayList<>();
        private final List<String> deletedFilingNumbers = new ArrayList<>();

        @Override
        public void saveAll(List<Offering> offerings) {
            saved.addAll(offerings);
        }

        @Override
        public void deleteAllByFilingNumber(String filingNumber) {
            deletedFilingNumbers.add(filingNumber);
        }
    }

    private static class StubExtractor implements OfferingExtractor {

        private final Map<String, List<OfferingDraft>> results = new HashMap<>();
        private final List<String> failingModels = new ArrayList<>();
        private final List<String> calledModels = new ArrayList<>();

        @Override
        public List<OfferingDraft> extract(String slicedContent, String model) {
            calledModels.add(model);
            if (failingModels.contains(model)) {
                throw new OfferingExtractionException("호출 실패");
            }
            return results.getOrDefault(model, List.of());
        }
    }
}
