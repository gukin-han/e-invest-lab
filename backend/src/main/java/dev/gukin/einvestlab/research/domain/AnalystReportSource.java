package dev.gukin.einvestlab.research.domain;

import java.time.LocalDate;
import java.util.List;

public interface AnalystReportSource {

    List<AnalystReportListing> fetchListings(LocalDate start, LocalDate end);
}
