package dev.gukin.einvestlab.disclosure.infrastructure.persistence;

import dev.gukin.einvestlab.disclosure.domain.BusinessContent;
import dev.gukin.einvestlab.disclosure.domain.OfferingExtractionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface BusinessContentJpaRepository extends JpaRepository<BusinessContent, UUID> {

    boolean existsByFilingNumber(String filingNumber);

    @Query("""
            select c from BusinessContent c
            where c.offeringExtractionStatus is null or c.offeringExtractionStatus = :retryable
            """)
    List<BusinessContent> findAllPendingOfferingExtraction(@Param("retryable") OfferingExtractionStatus retryable);
}
