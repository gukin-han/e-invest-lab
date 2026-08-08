package dev.gukin.einvestlab.disclosure.infrastructure.persistence;

import dev.gukin.einvestlab.disclosure.domain.Offering;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface OfferingJpaRepository extends JpaRepository<Offering, UUID> {

    @Modifying
    @Query("delete from Offering o where o.filingNumber = :filingNumber")
    void deleteAllByFilingNumber(@Param("filingNumber") String filingNumber);
}