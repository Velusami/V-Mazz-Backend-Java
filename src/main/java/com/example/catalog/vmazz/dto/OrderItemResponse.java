package com.example.catalog.vmazz.dto;

import com.example.catalog.vmazz.domain.ItemCategory;
import com.example.catalog.vmazz.domain.PurchaseOrderItem;
import com.example.catalog.vmazz.domain.Uom;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class OrderItemResponse {

    private final int lineNumber;
    private final Long itemId;
    private final String itemCode;
    private final String itemName;
    private final String imageUrl;
    private final ItemCategory category;
    private final String specification;
    private final String hsnCode;
    private final Uom uom;
    private final BigDecimal quantity;
    private final BigDecimal unitPrice;
    private final BigDecimal taxPercent;
    private final BigDecimal lineSubTotal;
    private final BigDecimal taxAmount;
    private final BigDecimal lineTotal;

    public static OrderItemResponse from(PurchaseOrderItem l) {
        return OrderItemResponse.builder()
                .lineNumber(l.getLineNumber())
                .itemId(l.getItem().getId())
                .itemCode(l.getItemCode())
                .itemName(l.getItemName())
                .imageUrl(l.getItem().getImageUrl())
                .category(l.getCategory())
                .specification(l.getSpecification())
                .hsnCode(l.getHsnCode())
                .uom(l.getUom())
                .quantity(l.getQuantity())
                .unitPrice(l.getUnitPrice())
                .taxPercent(l.getTaxPercent())
                .lineSubTotal(l.getLineSubTotal())
                .taxAmount(l.getTaxAmount())
                .lineTotal(l.getLineTotal())
                .build();
    }
}
