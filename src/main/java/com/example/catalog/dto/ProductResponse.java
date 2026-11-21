package com.example.catalog.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private Long id;
    private String mpn;
    private String itemName;
    private String manufacturer;
    private BigDecimal price;
    private String currency;
    private Integer quantity;
    private String category;
    private String packageType;
    private String specifications;
    private String status;
    private Long version;
}
