package dev.gukin.einvestlab.disclosure.domain;

import java.time.Instant;
import java.util.List;

public interface BusinessReportSource {

    List<BusinessReportFiling> findRecent(String corpCode, Instant baseTime);

    String fetchBusinessContent(BusinessReportFiling filing);
}
