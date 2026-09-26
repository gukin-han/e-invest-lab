package dev.gukin.einvestlab.disclosure.infrastructure.openai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.gukin.einvestlab.disclosure.domain.OfferingDraft;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record OfferingPayload(@JsonProperty("offerings") List<Item> offerings) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Item(
            @JsonProperty("business_part") String businessPart,
            @JsonProperty("segment") String segment,
            @JsonProperty("qualifier") String qualifier,
            @JsonProperty("products") List<String> products,
            @JsonProperty("revenue_amount") BigDecimal revenueAmount,
            @JsonProperty("revenue_unit") String revenueUnit,
            @JsonProperty("revenue_basis") String revenueBasis,
            @JsonProperty("revenue_share") BigDecimal revenueShare,
            @JsonProperty("customers") List<String> customers,
            @JsonProperty("entity") String entityName,
            @JsonProperty("fiscal_year") Integer fiscalYear
    ) {
    }

    List<OfferingDraft> toDrafts() {
        if (offerings == null) {
            return List.of();
        }
        return offerings.stream()
                .map(item -> new OfferingDraft(
                        item.businessPart(),
                        item.segment(),
                        item.qualifier(),
                        item.products() == null ? List.of() : item.products(),
                        item.revenueAmount(),
                        item.revenueUnit(),
                        item.revenueBasis(),
                        item.revenueShare(),
                        item.customers() == null ? List.of() : item.customers(),
                        item.entityName(),
                        item.fiscalYear()))
                .toList();
    }
}
