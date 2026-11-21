package com.example.catalog.vmazz.dto;

import com.example.catalog.vmazz.domain.Item;
import com.example.catalog.vmazz.domain.ItemCategory;
import com.example.catalog.vmazz.domain.RecordStatus;
import com.example.catalog.vmazz.domain.Uom;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Getter
@Builder
public class ItemResponse {

    private final Long id;
    private final String itemCode;
    private final String itemName;
    private final ItemCategory category;
    private final String categoryName;
    private final String subCategory;
    private final String brand;
    private final String description;
    private final String specification;
    private final String hsnCode;
    private final Uom uom;
    private final BigDecimal unitPrice;
    private final BigDecimal mrp;
    /** Discount versus MRP in percent, when an MRP is set. */
    private final Integer discountPercent;
    private final String currency;
    private final BigDecimal taxPercent;
    private final BigDecimal availableQuantity;
    private final BigDecimal minOrderQuantity;
    private final boolean inStock;
    private final String imageUrl;
    private final RecordStatus status;
    private final Long supplierCompanyId;
    private final String supplierName;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static ItemResponse from(Item i) {

        Integer discount = null;
        if (i.getMrp() != null && i.getMrp().signum() > 0 && i.getMrp().compareTo(i.getUnitPrice()) > 0) {
            discount = i.getMrp().subtract(i.getUnitPrice())
                    .multiply(BigDecimal.valueOf(100))
                    .divide(i.getMrp(), 0, RoundingMode.HALF_UP)
                    .intValue();
        }

        return ItemResponse.builder()
                .id(i.getId())
                .itemCode(i.getItemCode())
                .itemName(i.getItemName())
                .category(i.getCategory())
                .categoryName(i.getCategory().getDisplayName())
                .subCategory(i.getSubCategory())
                .brand(i.getBrand())
                .description(i.getDescription())
                .specification(i.getSpecification())
                .hsnCode(i.getHsnCode())
                .uom(i.getUom())
                .unitPrice(i.getUnitPrice())
                .mrp(i.getMrp())
                .discountPercent(discount)
                .currency(i.getCurrency())
                .taxPercent(i.getTaxPercent())
                .availableQuantity(i.getAvailableQuantity())
                .minOrderQuantity(i.getMinOrderQuantity())
                .inStock(i.getAvailableQuantity().compareTo(i.getMinOrderQuantity()) >= 0)
                .imageUrl(i.getImageUrl())
                .status(i.getStatus())
                .supplierCompanyId(i.getCompany().getId())
                .supplierName(i.getCompany().getCompanyName())
                .createdAt(i.getCreatedAt())
                .updatedAt(i.getUpdatedAt())
                .build();
    }
}
