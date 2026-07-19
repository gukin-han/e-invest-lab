package dev.gukin.einvestlab.company.application;

import dev.gukin.einvestlab.company.domain.Company;
import dev.gukin.einvestlab.company.domain.CompanyRegistrySource;
import dev.gukin.einvestlab.global.id.Ids;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("회사 등록부 동기화 단위 테스트")
class CompanyRegistrySyncUnitTest {

    private final RecordingBatchWriter writer = new RecordingBatchWriter();

    @Nested
    @DisplayName("등록부를 동기화할 때")
    class WhenSyncingRegistry {

        @Test
        @DisplayName("회사를 1000개 단위로 저장한다")
        void shouldWriteCompaniesInBatches() {
            CompanyRegistrySyncUseCase useCase = useCaseWithCompanies(2_500);

            CompanyRegistrySyncResult result = useCase.syncAll();

            assertThat(writer.batches)
                    .extracting(List::size)
                    .containsExactly(1_000, 1_000, 500);
            assertThat(result.upsertedCount()).isEqualTo(2_500);
        }

        @Test
        @DisplayName("등록부가 비어 있으면 저장하지 않는다")
        void shouldSkipWritingWithEmptyRegistry() {
            CompanyRegistrySyncUseCase useCase = useCaseWithCompanies(0);

            CompanyRegistrySyncResult result = useCase.syncAll();

            assertThat(writer.batches).isEmpty();
            assertThat(result.upsertedCount()).isZero();
        }

        @Test
        @DisplayName("정확히 1000개면 한 번만 저장한다")
        void shouldWriteOnceWithFullBatch() {
            CompanyRegistrySyncUseCase useCase = useCaseWithCompanies(1_000);

            CompanyRegistrySyncResult result = useCase.syncAll();

            assertThat(writer.batches)
                    .extracting(List::size)
                    .containsExactly(1_000);
            assertThat(result.upsertedCount()).isEqualTo(1_000);
        }

        @Test
        @DisplayName("저장할 때 원본 버퍼를 외부에 노출하지 않는다")
        void shouldNotExposeMutableBufferWhenWritingBatch() {
            CompanyRegistrySyncUseCase useCase = useCaseWithCompanies(1_001);

            useCase.syncAll();

            assertThat(writer.batches)
                    .allSatisfy(batch -> assertThat(batch).isNotEmpty())
                    .extracting(List::size)
                    .containsExactly(1_000, 1);
        }
    }

    private CompanyRegistrySyncUseCase useCaseWithCompanies(int count) {
        return new CompanyRegistrySyncUseCase(sourceWithCompanies(count), writer);
    }

    private CompanyRegistrySource sourceWithCompanies(int count) {
        List<Company> companies = IntStream.range(0, count)
                .mapToObj(this::company)
                .toList();

        return companies::forEach;
    }

    private Company company(int index) {
        return Company.builder()
                .id(Ids.generate())
                .corpCode("%08d".formatted(index))
                .name("회사%s".formatted(index))
                .registryModifiedDate(LocalDate.of(2026, 6, 6))
                .build();
    }

    private static class RecordingBatchWriter implements CompanyRegistryBatchWriter {

        private final List<List<Company>> batches = new ArrayList<>();

        @Override
        public int upsert(List<Company> companies) {
            batches.add(companies);
            return companies.size();
        }
    }
}
