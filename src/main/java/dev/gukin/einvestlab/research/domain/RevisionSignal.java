package dev.gukin.einvestlab.research.domain;

import java.math.BigDecimal;

public enum RevisionSignal {
    NEW, STRONG_UP, UP, FLAT, DOWN, STRONG_DOWN;

    private static final BigDecimal STRONG = new BigDecimal("10");
    private static final BigDecimal MILD = new BigDecimal("3");

    public static RevisionSignal of(BigDecimal revisionRate) {
        if (revisionRate == null) {
            return NEW;
        }
        if (revisionRate.compareTo(STRONG) >= 0) {
            return STRONG_UP;
        }
        if (revisionRate.compareTo(MILD) >= 0) {
            return UP;
        }
        if (revisionRate.compareTo(MILD.negate()) > 0) {
            return FLAT;
        }
        if (revisionRate.compareTo(STRONG.negate()) > 0) {
            return DOWN;
        }
        return STRONG_DOWN;
    }
}
