package com.example.catalog.vmazz.service;

import com.example.catalog.vmazz.domain.Item;
import com.example.catalog.vmazz.domain.Uom;
import com.example.catalog.vmazz.exception.ConflictException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PricingAndQuantityRulesTest {

    private Item item(Uom uom, String min, String stock) {
        Item item = TestData.item(1L, TestData.company(1L), "100", "0", stock);
        item.setUom(uom);
        item.setMinOrderQuantity(new BigDecimal(min));
        return item;
    }

    @Test
    void lineSubTotal_andTax_roundToTwoDecimals() {
        BigDecimal sub = Pricing.lineSubTotal(new BigDecimal("145.00"), new BigDecimal("2.5"));
        assertEquals(new BigDecimal("362.50"), sub);
        assertEquals(new BigDecimal("18.13"), Pricing.tax(sub, new BigDecimal("5")));   // 18.125 -> 18.13
        assertEquals(new BigDecimal("0.00"), Pricing.tax(sub, null));
    }

    @Test
    void countableUnit_requiresWholeNumbers() {
        Item pcs = item(Uom.PCS, "1", "100");
        assertDoesNotThrow(() -> QuantityRules.validate(pcs, new BigDecimal("3")));
        assertDoesNotThrow(() -> QuantityRules.validate(pcs, new BigDecimal("3.000")));
        assertThrows(IllegalArgumentException.class, () -> QuantityRules.validate(pcs, new BigDecimal("2.5")));
    }

    @Test
    void measuredUnit_allowsFractions() {
        Item kg = item(Uom.KG, "1", "100");
        assertDoesNotThrow(() -> QuantityRules.validate(kg, new BigDecimal("2.5")));
    }

    @Test
    void quantityMustBePositive_andMeetMinimum() {
        Item kg = item(Uom.KG, "5", "100");
        assertThrows(IllegalArgumentException.class, () -> QuantityRules.validate(kg, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> QuantityRules.validate(kg, new BigDecimal("-1")));
        assertThrows(IllegalArgumentException.class, () -> QuantityRules.validate(kg, new BigDecimal("4")));
        assertDoesNotThrow(() -> QuantityRules.validate(kg, new BigDecimal("5")));
    }

    @Test
    void quantityAboveStock_isAConflict() {
        Item kg = item(Uom.KG, "1", "10");
        ConflictException ex = assertThrows(ConflictException.class,
                () -> QuantityRules.validate(kg, new BigDecimal("10.5")));
        assertTrue(ex.getMessage().contains("Only 10 KG"));
    }
}
