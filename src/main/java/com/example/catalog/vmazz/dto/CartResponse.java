package com.example.catalog.vmazz.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class CartResponse {

    private final List<CartItemResponse> items;
    private final int totalLines;
    private final String currency;
    private final BigDecimal subTotal;
    private final BigDecimal taxTotal;
    private final BigDecimal grandTotal;
    private final boolean hasUnavailableItems;
}
