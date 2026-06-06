package dev.gukin.einvestlab.company.application;

import dev.gukin.einvestlab.company.domain.Company;
import dev.gukin.einvestlab.company.domain.CompanyRegistrySource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanyRegistrySyncService {

    private static final int BATCH_SIZE = 1_000;

    private final CompanyRegistrySource source;
    private final CompanyRegistryBatchWriter writer;

    public CompanyRegistrySyncResult syncAll() {
        SyncAccumulator accumulator = new SyncAccumulator(writer);

        source.streamAll(accumulator::accept);
        accumulator.finish();

        return new CompanyRegistrySyncResult(accumulator.upsertedCount());
    }

    private static class SyncAccumulator {

        private final CompanyRegistryBatchWriter writer;
        private final List<Company> buffer = new ArrayList<>(BATCH_SIZE);
        private int upsertedCount;

        private SyncAccumulator(CompanyRegistryBatchWriter writer) {
            this.writer = writer;
        }

        private void accept(Company company) {
            buffer.add(company);
            if (buffer.size() == BATCH_SIZE) {
                flush();
            }
        }

        private void finish() {
            if (!buffer.isEmpty()) {
                flush();
            }
        }

        private int upsertedCount() {
            return upsertedCount;
        }

        private void flush() {
            upsertedCount += writer.upsert(List.copyOf(buffer));
            buffer.clear();
        }
    }
}
