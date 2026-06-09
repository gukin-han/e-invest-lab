package dev.gukin.einvestlab.company.interfaces;

import dev.gukin.einvestlab.company.application.CompanyRegistrySyncResult;
import dev.gukin.einvestlab.company.application.CompanyRegistrySyncUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompanyRegistryScheduler {

    private final CompanyRegistrySyncUseCase syncUseCase;

    @Scheduled(cron = "0 0 1 * * *", zone = "Asia/Seoul")
    void syncDaily() {
        log.info("company registry sync started.");
        CompanyRegistrySyncResult result = syncUseCase.syncAll();
        log.info("company registry sync completed. upsertedCount={}", result.upsertedCount());
    }
}
