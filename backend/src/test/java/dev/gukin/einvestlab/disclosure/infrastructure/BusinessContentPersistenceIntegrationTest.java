package dev.gukin.einvestlab.disclosure.infrastructure;

import dev.gukin.einvestlab.disclosure.domain.BusinessContent;
import dev.gukin.einvestlab.disclosure.infrastructure.persistence.BusinessContentJpaRepository;
import dev.gukin.einvestlab.global.id.Ids;
import dev.gukin.einvestlab.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("사업의 내용 영속화 통합 테스트")
class BusinessContentPersistenceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private BusinessContentJpaRepository repository;

    @AfterEach
    void tearDown() {
        repository.deleteAllInBatch();
    }

    @Nested
    @DisplayName("사업의 내용을 저장하고 다시 읽을 때")
    class WhenSavingAndReading {

        @Test
        @DisplayName("대용량 섹션 텍스트와 수집 시각이 그대로 보존된다")
        void shouldRoundTripLargeContentAndCollectedAt() {
            UUID id = Ids.generate();
            Instant collectedAt = Instant.parse("2026-07-19T03:00:00Z");
            String largeContent = "II. 사업의 내용\n" + "가나다라마바사아자차카타파하 ".repeat(20_000);

            repository.save(BusinessContent.builder()
                    .id(id)
                    .corpCode("00126380")
                    .filingNumber("20260310002820")
                    .filedDate(LocalDate.of(2026, 3, 10))
                    .content(largeContent)
                    .collectedAt(collectedAt)
                    .build());

            BusinessContent found = repository.findById(id).orElseThrow();
            assertThat(found.getContent()).isEqualTo(largeContent);
            assertThat(found.getCollectedAt()).isEqualTo(collectedAt);
            assertThat(repository.existsByFilingNumber("20260310002820")).isTrue();
        }
    }

    @Nested
    @DisplayName("같은 접수번호로 두 번 저장할 때")
    class WhenSavingDuplicateFilingNumber {

        @Test
        @DisplayName("두 번째 저장은 중복으로 거부된다")
        void shouldRejectDuplicateFilingNumber() {
            saveSample("20260310002820");

            assertThatThrownBy(() -> saveSample("20260310002820"))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        private void saveSample(String filingNumber) {
            repository.saveAndFlush(BusinessContent.builder()
                    .id(Ids.generate())
                    .corpCode("00126380")
                    .filingNumber(filingNumber)
                    .filedDate(LocalDate.of(2026, 3, 10))
                    .content("II. 사업의 내용")
                    .collectedAt(Instant.parse("2026-07-19T03:00:00Z"))
                    .build());
        }
    }
}
