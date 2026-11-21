package com.example.catalog.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class BomMatchedProduct {
    private final String mpn;
    private final String itemName;
    private final String manufacturer;
    private final String category;
    private final String packageType;
    private final String specifications;
    private final BigDecimal price;
    private final String currency;
    private final Integer quantityAvailable;
    private final String status;
}
