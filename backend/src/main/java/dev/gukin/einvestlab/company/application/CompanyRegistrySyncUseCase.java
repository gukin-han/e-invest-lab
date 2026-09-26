package dev.gukin.einvestlab.company.application;

import dev.gukin.einvestlab.company.domain.Company;
import dev.gukin.einvestlab.company.domain.CompanyRegistryEntry;
import dev.gukin.einvestlab.company.domain.CompanyRegistrySource;
import dev.gukin.einvestlab.company.domain.CompanyRepository;
import dev.gukin.einvestlab.global.id.Ids;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class CompanyRegistrySyncUseCase {

    private static final int BATCH_SIZE = 1_000;

    private final CompanyRegistrySource source;
    private final CompanyRepository repository;
    private final TransactionTemplate batchTransaction;

    public CompanyRegistrySyncUseCase(CompanyRegistrySource source,
                                      CompanyRepository repository,
                                      PlatformTransactionManager transactionManager) {
        this.source = source;
        this.repository = repository;
        this.batchTransaction = new TransactionTemplate(transactionManager);
        this.batchTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public CompanyRegistrySyncResult syncAll() {
        SyncAccumulator accumulator = new SyncAccumulator();

        source.streamAll(accumulator::accept);
        accumulator.finish();

        return new CompanyRegistrySyncResult(accumulator.upsertedCount);
    }

    private class SyncAccumulator {

        private final List<Company> buffer = new ArrayList<>(BATCH_SIZE);
        private int upsertedCount;

        private void accept(CompanyRegistryEntry entry) {
            buffer.add(toCompany(entry));
            if (buffer.size() == BATCH_SIZE) {
                flush();
            }
        }

        private Company toCompany(CompanyRegistryEntry entry) {
            return Company.builder()
                    .id(Ids.generate())
                    .corpCode(entry.corpCode())
                    .name(entry.name())
                    .englishName(entry.englishName())
                    .stockCode(entry.stockCode())
                    .registryModifiedDate(entry.registryModifiedDate())
                    .build();
        }

        private void finish() {
            if (!buffer.isEmpty()) {
                flush();
            }
        }

        private void flush() {
            List<Company> batch = List.copyOf(buffer);
            upsertedCount += batchTransaction.execute(status -> repository.upsertCompanies(batch));
            buffer.clear();
        }
    }
}
