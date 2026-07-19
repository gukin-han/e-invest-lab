package dev.gukin.einvestlab.disclosure.infra.dart;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.gukin.einvestlab.disclosure.domain.BusinessReportFiling;
import dev.gukin.einvestlab.disclosure.domain.DisclosureSourceException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
record DisclosureSearchResponse(String status, String message, List<Item> list) {

    private static final String STATUS_OK = "000";
    private static final String STATUS_NO_RESULT = "013";
    private static final DateTimeFormatter DART_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    Optional<BusinessReportFiling> toFiling(String corpCode) {
        if (STATUS_NO_RESULT.equals(status)) {
            return Optional.empty();
        }
        if (!STATUS_OK.equals(status)) {
            throw new DisclosureSourceException("DART 공시검색 응답 status " + status + ": " + message);
        }
        if (list == null || list.isEmpty()) {
            return Optional.empty();
        }
        return list.stream()
                .max(Comparator.comparing(Item::filedDate))
                .map(item -> new BusinessReportFiling(
                        corpCode,
                        item.filingNumber(),
                        LocalDate.parse(item.filedDate(), DART_DATE)));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Item(
            @JsonProperty("rcept_no") String filingNumber,
            @JsonProperty("rcept_dt") String filedDate
    ) {
    }
}
