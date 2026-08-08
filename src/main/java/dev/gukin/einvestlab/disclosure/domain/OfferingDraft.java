package dev.gukin.einvestlab.disclosure.domain;

import java.math.BigDecimal;
import java.util.List;

public record OfferingDraft(
        String businessPart,
        String segment,
        String qualifier,
        List<String> products,
        BigDecimal revenueAmount,
        String revenueUnit,
        String revenueBasis,
        BigDecimal revenueShare,
        List<String> customers,
        String entityName,
        Integer fiscalYear
) {

    public OfferingDraft withRevenueBasis(String normalizedBasis) {
        return new OfferingDraft(businessPart, segment, qualifier, products, revenueAmount,
                revenueUnit, normalizedBasis, revenueShare, customers, entityName, fiscalYear);
    }
}
