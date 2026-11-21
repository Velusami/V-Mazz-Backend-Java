package com.example.catalog.vmazz.domain;

/** Unit of measure. Countable units must be ordered in whole numbers. */
public enum Uom {
    PCS(false), BOX(false), PACK(false), DOZEN(false), BAG(false), BOTTLE(false), SET(false),
    KG(true), GRAM(true), TON(true), LITRE(true), ML(true), METER(true);

    private final boolean allowsFraction;

    Uom(boolean allowsFraction) {
        this.allowsFraction = allowsFraction;
    }

    public boolean allowsFraction() {
        return allowsFraction;
    }
}
