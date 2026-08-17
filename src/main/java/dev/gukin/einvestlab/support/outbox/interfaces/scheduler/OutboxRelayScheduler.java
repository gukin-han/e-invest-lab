package dev.gukin.einvestlab.support.outbox.interfaces.scheduler;

import dev.gukin.einvestlab.support.outbox.application.OutboxRelayResult;
import dev.gukin.einvestlab.support.outbox.application.OutboxRelayUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelayScheduler {

    private final OutboxRelayUseCase relayUseCase;
    private final Clock clock;

    @Scheduled(fixedDelayString = "PT1M", initialDelayString = "PT30S")
    void relay() {
        try {
            OutboxRelayResult result = relayUseCase.relay(clock.instant());
            if (result.sent() + result.failed() + result.dead() > 0) {
                log.info("outbox relay completed. sent={} failed={} dead={}",
                        result.sent(), result.failed(), result.dead());
            }
        } catch (Exception e) {
            log.error("outbox relay failed.", e);
        }
    }
}
