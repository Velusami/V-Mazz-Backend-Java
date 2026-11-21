package com.example.catalog.vmazz.dto;

import com.example.catalog.vmazz.domain.ItemCategory;
import com.example.catalog.vmazz.domain.Uom;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class CartItemResponse {

    private final Long itemId;
    private final String itemCode;
    private final String itemName;
    private final ItemCategory category;
    private final Uom uom;
    private final String imageUrl;
    private final Long supplierCompanyId;
    private final String supplierName;
    private final BigDecimal unitPrice;
    private final String currency;
    private final BigDecimal taxPercent;
    private final BigDecimal quantity;
    private final BigDecimal lineSubTotal;
    private final BigDecimal taxAmount;
    private final BigDecimal lineTotal;
    private final BigDecimal availableQuantity;
    /** false when the item was deactivated or stock no longer covers the quantity. */
    private final boolean available;
    private final String issue;
}
