package dev.gukin.einvestlab.disclosure.application;

import dev.gukin.einvestlab.disclosure.domain.BusinessContent;
import dev.gukin.einvestlab.disclosure.domain.BusinessContentRepository;
import dev.gukin.einvestlab.disclosure.domain.BusinessReportFiling;
import dev.gukin.einvestlab.disclosure.domain.BusinessReportSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("사업의 내용 수집 유스케이스 단위 테스트")
class BusinessContentCollectUnitTest {

    private static final Instant BASE_TIME = Instant.parse("2026-07-19T03:00:00Z");
    private static final BusinessReportFiling FILING =
            new BusinessReportFiling("00126380", "20260310002820", LocalDate.of(2026, 3, 10));

    private final StubSource source = new StubSource();
    private final StubRepository repository = new StubRepository();
    private final BusinessContentCollectUseCase useCase =
            new BusinessContentCollectUseCase(source, repository);

    @Test
    @DisplayName("새 사업보고서를 수집해 raw 그대로 저장한다")
    void shouldCollectAndSaveRawContent() {
        source.filing = Optional.of(FILING);
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
    @DisplayName("최근 2년 내 사업보고서가 없으면 저장 없이 NO_REPORT 를 돌려준다")
    void shouldReturnNoReportWhenFilingAbsent() {
        source.filing = Optional.empty();

        BusinessContentCollectResult result = useCase.collect("00126380", BASE_TIME);

        assertThat(result).isEqualTo(BusinessContentCollectResult.NO_REPORT);
        assertThat(repository.saved).isEmpty();
    }

    @Test
    @DisplayName("이미 수집한 접수번호면 원문을 다시 받지 않고 ALREADY_COLLECTED 를 돌려준다")
    void shouldSkipAlreadyCollectedFiling() {
        source.filing = Optional.of(FILING);
        repository.existing = "20260310002820";

        BusinessContentCollectResult result = useCase.collect("00126380", BASE_TIME);

        assertThat(result).isEqualTo(BusinessContentCollectResult.ALREADY_COLLECTED);
        assertThat(source.fetchCount).isZero();
        assertThat(repository.saved).isEmpty();
    }

    private static class StubSource implements BusinessReportSource {

        private Optional<BusinessReportFiling> filing = Optional.empty();
        private String businessContent = "";
        private int fetchCount;

        @Override
        public Optional<BusinessReportFiling> findLatest(String corpCode, Instant baseTime) {
            return filing;
        }

        @Override
        public String fetchBusinessContent(BusinessReportFiling filing) {
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
    }
}
