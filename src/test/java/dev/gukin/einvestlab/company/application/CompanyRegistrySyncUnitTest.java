package dev.gukin.einvestlab.company.application;

import dev.gukin.einvestlab.company.domain.Company;
import dev.gukin.einvestlab.company.domain.CompanyRegistrySource;
import dev.gukin.einvestlab.company.domain.CompanyRepository;
import dev.gukin.einvestlab.global.id.Ids;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("회사 등록부 동기화 단위 테스트")
class CompanyRegistrySyncUnitTest {

    private final RecordingRepository repository = new RecordingRepository();
    private final RecordingTransactionManager transactionManager = new RecordingTransactionManager();

    @Nested
    @DisplayName("등록부를 동기화할 때")
    class WhenSyncingRegistry {

        @Test
        @DisplayName("회사를 1000개 단위로 각각 새 트랜잭션에서 저장한다")
        void shouldWriteCompaniesInBatches() {
            CompanyRegistrySyncUseCase useCase = useCaseWithCompanies(2_500);

            CompanyRegistrySyncResult result = useCase.syncAll();

            assertThat(repository.batches)
                    .extracting(List::size)
                    .containsExactly(1_000, 1_000, 500);
            assertThat(result.upsertedCount()).isEqualTo(2_500);
            assertThat(transactionManager.startedCount).isEqualTo(3);
            assertThat(transactionManager.propagations)
                    .containsOnly(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        }

        @Test
        @DisplayName("등록부가 비어 있으면 저장도 트랜잭션도 없다")
        void shouldSkipWritingWithEmptyRegistry() {
            CompanyRegistrySyncUseCase useCase = useCaseWithCompanies(0);

            CompanyRegistrySyncResult result = useCase.syncAll();

            assertThat(repository.batches).isEmpty();
            assertThat(transactionManager.startedCount).isZero();
            assertThat(result.upsertedCount()).isZero();
        }

        @Test
        @DisplayName("정확히 1000개면 한 번만 저장한다")
        void shouldWriteOnceWithFullBatch() {
            CompanyRegistrySyncUseCase useCase = useCaseWithCompanies(1_000);

            CompanyRegistrySyncResult result = useCase.syncAll();

            assertThat(repository.batches)
                    .extracting(List::size)
                    .containsExactly(1_000);
            assertThat(result.upsertedCount()).isEqualTo(1_000);
        }

        @Test
        @DisplayName("저장할 때 원본 버퍼를 외부에 노출하지 않는다")
        void shouldNotExposeMutableBufferWhenWritingBatch() {
            CompanyRegistrySyncUseCase useCase = useCaseWithCompanies(1_001);

            useCase.syncAll();

            assertThat(repository.batches)
                    .allSatisfy(batch -> assertThat(batch).isNotEmpty())
                    .extracting(List::size)
                    .containsExactly(1_000, 1);
        }
    }

    private CompanyRegistrySyncUseCase useCaseWithCompanies(int count) {
        return new CompanyRegistrySyncUseCase(sourceWithCompanies(count), repository, transactionManager);
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

    private static class RecordingRepository implements CompanyRepository {

        private final List<List<Company>> batches = new ArrayList<>();

        @Override
        public Company save(Company company) {
            return company;
        }

        @Override
        public int upsertCompanies(List<Company> companies) {
            batches.add(companies);
            return companies.size();
        }

        @Override
        public Optional<Company> findByCorpCode(String corpCode) {
            return Optional.empty();
        }
    }

    private static class RecordingTransactionManager implements PlatformTransactionManager {

        private int startedCount;
        private final List<Integer> propagations = new ArrayList<>();

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            startedCount++;
            propagations.add(definition.getPropagationBehavior());
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
        }

        @Override
        public void rollback(TransactionStatus status) {
        }
    }
}
