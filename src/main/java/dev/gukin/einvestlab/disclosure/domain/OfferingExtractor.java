package dev.gukin.einvestlab.disclosure.domain;

import java.util.List;

public interface OfferingExtractor {

    List<OfferingDraft> extract(String slicedContent, String model);
}
