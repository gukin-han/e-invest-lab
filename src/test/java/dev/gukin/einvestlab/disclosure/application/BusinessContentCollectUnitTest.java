package dev.gukin.einvestlab.disclosure.application;

import dev.gukin.einvestlab.disclosure.domain.BusinessContent;
import dev.gukin.einvestlab.disclosure.domain.BusinessContentRepository;
import dev.gukin.einvestlab.disclosure.domain.BusinessReportFiling;
import dev.gukin.einvestlab.disclosure.domain.BusinessReportSource;
import dev.gukin.einvestlab.disclosure.domain.DisclosureDocumentMissingException;
import dev.gukin.einvestlab.disclosure.domain.DisclosureSourceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("사업의 내용 수집 유스케이스 단위 테스트")
class BusinessContentCollectUnitTest {

    private static final Instant BASE_TIME = Instant.parse("2026-07-19T03:00:00Z");
    private static final BusinessReportFiling LATEST =
            new BusinessReportFiling("00126380", "20260310002820", LocalDate.of(2026, 3, 10));
    private static final BusinessReportFiling OLDER =
            new BusinessReportFiling("00126380", "20250311001234", LocalDate.of(2025, 3, 11));

    private final StubSource source = new StubSource();
    private final StubRepository repository = new StubRepository();
    private final BusinessContentCollectUseCase useCase =
            new BusinessContentCollectUseCase(source, repository);

    @Test
    @DisplayName("최신 사업보고서를 수집해 raw 그대로 저장한다")
    void shouldCollectAndSaveRawContent() {
        source.filings = List.of(LATEST, OLDER);
        source.businessContent = "II. 사업의 내용 원문";

        BusinessContentCollectResult result = useCase.collect("00126380", BASE_TIME);

        assertThat(result).isEqualTo(BusinessContentCollectResult.COLLECTED);
        assertThat(repository.saved).hasSize(1);
        BusinessContent saved = repository.saved.getFirst();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCorpCode()).isEqualTo("00126380");
        assertThat(saved.getFilingNumber()).isEqualTo("20260310002820");
        assertThat(saved.getFiledDate()).isEqualTo(LocalDate.of(2026, 3, 10));
        assertThat(saved.getContent()).isEqualTo("II. 사업의 내용 원문");
        assertThat(saved.getCollectedAt()).isEqualTo(BASE_TIME);
    }

    @Test
    @DisplayName("최신 건의 원문 파일이 없으면 다음 접수번호로 폴백해 저장한다")
    void shouldFallBackToOlderFilingWhenDocumentMissing() {
        source.filings = List.of(LATEST, OLDER);
        source.missingDocuments = Set.of(LATEST.filingNumber());
        source.businessContent = "이전 접수번호 원문";

        BusinessContentCollectResult result = useCase.collect("00126380", BASE_TIME);

        assertThat(result).isEqualTo(BusinessContentCollectResult.COLLECTED);
        assertThat(repository.saved).hasSize(1);
        assertThat(repository.saved.getFirst().getFilingNumber()).isEqualTo(OLDER.filingNumber());
    }

    @Test
    @DisplayName("모든 후보의 원문 파일이 없으면 예외를 던진다")
    void shouldThrowWhenAllDocumentsMissing() {
        source.filings = List.of(LATEST, OLDER);
        source.missingDocuments = Set.of(LATEST.filingNumber(), OLDER.filingNumber());

        assertThatThrownBy(() -> useCase.collect("00126380", BASE_TIME))
                .isInstanceOf(DisclosureSourceException.class)
                .hasMessageContaining("00126380");
        assertThat(repository.saved).isEmpty();
    }

    @Test
    @DisplayName("최근 2년 내 사업보고서가 없으면 저장 없이 NO_REPORT 를 돌려준다")
    void shouldReturnNoReportWhenFilingAbsent() {
        source.filings = List.of();

        BusinessContentCollectResult result = useCase.collect("00126380", BASE_TIME);

        assertThat(result).isEqualTo(BusinessContentCollectResult.NO_REPORT);
        assertThat(repository.saved).isEmpty();
    }

    @Test
    @DisplayName("이미 수집한 접수번호를 만나면 원문을 받지 않고 ALREADY_COLLECTED 를 돌려준다")
    void shouldSkipAlreadyCollectedFiling() {
        source.filings = List.of(LATEST, OLDER);
        repository.existing = LATEST.filingNumber();

        BusinessContentCollectResult result = useCase.collect("00126380", BASE_TIME);

        assertThat(result).isEqualTo(BusinessContentCollectResult.ALREADY_COLLECTED);
        assertThat(source.fetchCount).isZero();
        assertThat(repository.saved).isEmpty();
    }

    @Test
    @DisplayName("최신 건 파일이 없고 폴백 대상이 이미 저장돼 있으면 ALREADY_COLLECTED 를 돌려준다")
    void shouldReturnAlreadyCollectedWhenFallbackTargetStored() {
        source.filings = List.of(LATEST, OLDER);
        source.missingDocuments = Set.of(LATEST.filingNumber());
        repository.existing = OLDER.filingNumber();

        BusinessContentCollectResult result = useCase.collect("00126380", BASE_TIME);

        assertThat(result).isEqualTo(BusinessContentCollectResult.ALREADY_COLLECTED);
        assertThat(repository.saved).isEmpty();
    }

    private static class StubSource implements BusinessReportSource {

        private List<BusinessReportFiling> filings = List.of();
        private Set<String> missingDocuments = new HashSet<>();
        private String businessContent = "";
        private int fetchCount;

        @Override
        public List<BusinessReportFiling> findRecent(String corpCode, Instant baseTime) {
            return filings;
        }

        @Override
        public String fetchBusinessContent(BusinessReportFiling filing) {
            if (missingDocuments.contains(filing.filingNumber())) {
                throw new DisclosureDocumentMissingException("파일 없음: " + filing.filingNumber());
            }
            fetchCount++;
            return businessContent;
        }
    }

    private static class StubRepository implements BusinessContentRepository {

        private final List<BusinessContent> saved = new ArrayList<>();
        private String existing;

        @Override
        public BusinessContent save(BusinessContent businessContent) {
            saved.add(businessContent);
            return businessContent;
        }

        @Override
        public boolean existsByFilingNumber(String filingNumber) {
            return filingNumber.equals(existing);
        }

        @Override
        public List<BusinessContent> findAllPendingOfferingExtraction() {
            return List.of();
        }

        @Override
        public java.util.Optional<BusinessContent> findByFilingNumber(String filingNumber) {
            return java.util.Optional.empty();
        }
    }
}
