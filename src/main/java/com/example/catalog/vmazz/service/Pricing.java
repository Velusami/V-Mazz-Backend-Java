package com.example.catalog.vmazz.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class Pricing {

    private Pricing() { }

    static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    /** unit price x quantity, rounded to 2 decimals. */
    static BigDecimal lineSubTotal(BigDecimal unitPrice, BigDecimal quantity) {
        return unitPrice.multiply(quantity).setScale(2, RoundingMode.HALF_UP);
    }

    /** Tax on a sub total at the given percentage, rounded to 2 decimals. */
    static BigDecimal tax(BigDecimal subTotal, BigDecimal taxPercent) {
        if (taxPercent == null) {
            return ZERO;
        }
        return subTotal.multiply(taxPercent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
