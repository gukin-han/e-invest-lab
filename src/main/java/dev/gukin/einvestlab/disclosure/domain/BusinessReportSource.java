package dev.gukin.einvestlab.disclosure.domain;

import java.time.Instant;
import java.util.Optional;

public interface BusinessReportSource {

    Optional<BusinessReportFiling> findLatest(String corpCode, Instant baseTime);

    String fetchBusinessContent(BusinessReportFiling filing);
}
