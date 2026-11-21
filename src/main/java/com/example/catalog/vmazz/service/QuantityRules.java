package com.example.catalog.vmazz.service;

import com.example.catalog.vmazz.domain.Item;
import com.example.catalog.vmazz.domain.Uom;
import com.example.catalog.vmazz.exception.ConflictException;

import java.math.BigDecimal;

final class QuantityRules {

    private QuantityRules() { }

    /** Quantity rules for buying an item: positive, whole for countable units, >= minimum, <= stock. */
    static void validate(Item item, BigDecimal quantity) {

        if (quantity == null || quantity.signum() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        requireWholeIfCountable(item.getUom(), quantity, "Quantity");

        if (item.getMinOrderQuantity() != null && quantity.compareTo(item.getMinOrderQuantity()) < 0) {
            throw new IllegalArgumentException("Minimum order quantity for " + item.getItemName()
                    + " is " + plain(item.getMinOrderQuantity()) + " " + item.getUom());
        }

        if (quantity.compareTo(item.getAvailableQuantity()) > 0) {
            throw new ConflictException("Only " + plain(item.getAvailableQuantity()) + " " + item.getUom()
                    + " of " + item.getItemName() + " available");
        }
    }

    static void requireWholeIfCountable(Uom uom, BigDecimal quantity, String field) {
        if (!uom.allowsFraction() && quantity.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException(field + " must be a whole number for unit " + uom);
        }
    }

    static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
