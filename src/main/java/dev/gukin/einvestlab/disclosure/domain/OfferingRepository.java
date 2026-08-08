package dev.gukin.einvestlab.disclosure.domain;

import java.util.List;

public interface OfferingRepository {

    void saveAll(List<Offering> offerings);

    void deleteAllByFilingNumber(String filingNumber);
}
