package dev.gukin.einvestlab.disclosure.application;

import dev.gukin.einvestlab.global.id.Ids;
import dev.gukin.einvestlab.disclosure.domain.BusinessContent;
import dev.gukin.einvestlab.disclosure.domain.BusinessContentRepository;
import dev.gukin.einvestlab.disclosure.domain.Offering;
import dev.gukin.einvestlab.disclosure.domain.OfferingDraft;
import dev.gukin.einvestlab.disclosure.domain.OfferingExtractionStatus;
import dev.gukin.einvestlab.disclosure.domain.OfferingRepository;
import dev.gukin.einvestlab.support.RecordingTransactionManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Offering 무료 재검증 유스케이스 단위 테스트")
class OfferingReverifyUnitTest {

    private static final Instant BASE_TIME = Instant.parse("2026-08-08T03:00:00Z");
    private static final String CONTENT = "DX부문은 TV를 판매하며 매출액 3조 8,542억원(42.8%)을 기록했다.";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final StubContentRepository contentRepository = new StubContentRepository();
    private final StubOfferingRepository offeringRepository = new StubOfferingRepository();
    private final OfferingResultRecorder recorder = new OfferingResultRecorder(
            contentRepository, offeringRepository, objectMapper, new RecordingTransactionManager());
    private final OfferingReverifyUseCase useCase =
            new OfferingReverifyUseCase(contentRepository, new OfferingGuard(), recorder);

    @Test
    @DisplayName("가드 개선 후 저장된 LLM 출력만으로(재호출 없이) 실패 건을 회복한다")
    void shouldRecoverFailedContentFromStoredDrafts() {
        BusinessContent content = contentWithDrafts(List.of(
                new OfferingDraft(null, "DX", null, List.of("TV"),
                        new BigDecimal("38542"), "억원", "매출액", new BigDecimal("42.8"),
                        List.of(), null, 2025),
                new OfferingDraft(null, "DX", null, List.of("TV"),
                        null, null, "매출액", null, List.of(), null, 2024)));
        contentRepository.failedWithDrafts = List.of(content);

        OfferingReverifyResult result = useCase.reverifyAll(BASE_TIME);

        assertThat(result).isEqualTo(new OfferingReverifyResult(1, 0));
        assertThat(offeringRepository.saved).hasSize(2);
        assertThat(content.getOfferingExtractionStatus()).isEqualTo(OfferingExtractionStatus.EXTRACTED);
    }

    @Test
    @DisplayName("여전히 가드에 걸리는 건은 실패로 남고 사유가 갱신된다")
    void shouldKeepFailingContentWithUpdatedNote() {
        BusinessContent content = contentWithDrafts(List.of(
                new OfferingDraft(null, "DX", null, List.of("세탁기"),
                        null, null, "매출액", null, List.of(), null, 2025)));
        contentRepository.failedWithDrafts = List.of(content);

        OfferingReverifyResult result = useCase.reverifyAll(BASE_TIME);

        assertThat(result).isEqualTo(new OfferingReverifyResult(0, 1));
        assertThat(offeringRepository.saved).isEmpty();
        assertThat(content.getOfferingExtractionNote()).contains("원문 부재");
    }

    private BusinessContent contentWithDrafts(List<OfferingDraft> drafts) {
        BusinessContent content = BusinessContent.builder()
                .id(Ids.generate())
                .corpCode("00126380")
                .filingNumber("F1")
                .filedDate(LocalDate.of(2026, 3, 10))
                .content(CONTENT)
                .collectedAt(BASE_TIME)
                .build();
        content.recordOfferingExtraction(OfferingExtractionStatus.FAILED, "이전 사유",
                objectMapper.writeValueAsString(new OfferingResultRecorder.StoredDrafts(drafts)));
        return content;
    }

    private static class StubContentRepository implements BusinessContentRepository {

        private List<BusinessContent> failedWithDrafts = List.of();
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
            return List.of();
        }

        @Override
        public List<BusinessContent> findAllFailedWithDrafts() {
            return failedWithDrafts;
        }

        @Override
        public Optional<BusinessContent> findByFilingNumber(String filingNumber) {
            return Optional.empty();
        }
    }

    private static class StubOfferingRepository implements OfferingRepository {

        private final List<Offering> saved = new ArrayList<>();

        @Override
        public void saveAll(List<Offering> offerings) {
            saved.addAll(offerings);
        }

        @Override
        public void deleteAllByFilingNumber(String filingNumber) {
        }
    }
}
