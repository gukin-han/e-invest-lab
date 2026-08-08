package dev.gukin.einvestlab.disclosure.infrastructure.persistence;

import dev.gukin.einvestlab.disclosure.domain.Offering;
import dev.gukin.einvestlab.disclosure.domain.OfferingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class OfferingRepositoryAdapter implements OfferingRepository {

    private final OfferingJpaRepository jpa;

    @Override
    public void saveAll(List<Offering> offerings) {
        jpa.saveAll(offerings);
    }

    @Override
    public void deleteAllByFilingNumber(String filingNumber) {
        jpa.deleteAllByFilingNumber(filingNumber);
    }
}
