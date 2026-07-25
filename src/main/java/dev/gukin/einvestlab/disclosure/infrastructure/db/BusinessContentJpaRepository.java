package dev.gukin.einvestlab.disclosure.infrastructure.db;

import dev.gukin.einvestlab.disclosure.domain.BusinessContent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BusinessContentJpaRepository extends JpaRepository<BusinessContent, UUID> {

    boolean existsByFilingNumber(String filingNumber);
}
