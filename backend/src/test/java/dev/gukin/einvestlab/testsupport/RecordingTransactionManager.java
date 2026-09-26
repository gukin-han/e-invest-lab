package dev.gukin.einvestlab.testsupport;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.ArrayList;
import java.util.List;

public class RecordingTransactionManager implements PlatformTransactionManager {

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

    public int startedCount() {
        return startedCount;
    }

    public List<Integer> propagations() {
        return propagations;
    }
}
