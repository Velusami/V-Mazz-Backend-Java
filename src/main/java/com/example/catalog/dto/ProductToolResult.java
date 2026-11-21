package com.example.catalog.dto;

import com.example.catalog.document.ProductDocument;

import java.math.BigDecimal;

public record ProductToolResult(
        Long id,
        String mpn,
        String itemName,
        String manufacturer,
        BigDecimal price,
        String currency,
        Integer quantity,
        String status
) {
    public static ProductToolResult from(ProductDocument product) {
        return new ProductToolResult(
                product.getId(),
                product.getMpn(),
                product.getItemName(),
                product.getManufacturer(),
                product.getPrice(),
                product.getCurrency(),
                product.getQuantity(),
                product.getStatus()
        );
    }
}