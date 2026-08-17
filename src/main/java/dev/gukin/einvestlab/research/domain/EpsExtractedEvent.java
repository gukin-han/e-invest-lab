package dev.gukin.einvestlab.research.domain;

import java.util.List;

public record EpsExtractedEvent(long reportIdx, String stockCode, String companyName,
                                List<EpsFigure> figures) {

    public static final String TYPE = "EPS_EXTRACTED";
}
