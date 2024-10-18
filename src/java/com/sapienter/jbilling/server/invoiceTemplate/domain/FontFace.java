package com.sapienter.jbilling.server.invoiceTemplate.domain;

/**
 * @author elmot
 */
public enum FontFace {
    SansSerif,
    Serif,
    Monospaced;

    public static final FontFace DEFAULT = SansSerif;
    public final String INTERNAL_NAME;

    private FontFace() {
        INTERNAL_NAME = name();
    }
}
