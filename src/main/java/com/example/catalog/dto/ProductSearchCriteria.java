
package com.example.catalog.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductSearchCriteria {

    private String manufacturer;
    private String itemName;

    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    private String currency;
    private Integer minimumQuantity;
    private String status;
}
