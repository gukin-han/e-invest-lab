package dev.gukin.einvestlab.support.outbox.application;

import dev.gukin.einvestlab.support.outbox.domain.OutboxEvent;
import dev.gukin.einvestlab.support.outbox.domain.OutboxEventHandler;
import dev.gukin.einvestlab.support.outbox.domain.OutboxEventRepository;
import dev.gukin.einvestlab.support.outbox.domain.OutboxEventStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;

@Service
@Slf4j
public class OutboxRelayUseCase {

    private static final int BATCH_SIZE = 100;

    private final OutboxEventRepository repository;
    private final List<OutboxEventHandler> handlers;
    private final TransactionTemplate eventTransaction;

    public OutboxRelayUseCase(OutboxEventRepository repository,
                              List<OutboxEventHandler> handlers,
                              PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.handlers = handlers;
        this.eventTransaction = new TransactionTemplate(transactionManager);
    }

    public OutboxRelayResult relay(Instant now) {
        int sent = 0;
        int failed = 0;
        int dead = 0;
        for (OutboxEvent event : repository.findDue(now, BATCH_SIZE)) {
            switch (relayOne(event, now)) {
                case SENT -> sent++;
                case PENDING -> failed++;
                case DEAD -> dead++;
            }
        }
        return new OutboxRelayResult(sent, failed, dead);
    }

    private OutboxEventStatus relayOne(OutboxEvent event, Instant now) {
        OutboxEventHandler handler = findHandler(event.getEventType());
        if (handler == null) {
            log.error("아웃박스 핸들러 없음 (event_type={}, id={})", event.getEventType(), event.getId());
            event.markDead("no handler for " + event.getEventType());
        } else {
            try {
                handler.handle(event);
                event.markSent(now);
            } catch (Exception e) {
                log.warn("아웃박스 전송 실패 (event_type={}, id={}, attempt={}): {}",
                        event.getEventType(), event.getId(), event.getAttemptCount() + 1, e.toString());
                event.markFailed(e.toString(), now);
            }
        }
        eventTransaction.executeWithoutResult(tx -> repository.save(event));
        return event.getStatus();
    }

    private OutboxEventHandler findHandler(String eventType) {
        for (OutboxEventHandler handler : handlers) {
            if (handler.supports(eventType)) {
                return handler;
            }
        }
        return null;
    }
}
