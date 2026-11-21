package com.example.catalog.vmazz.dto;

import java.math.BigDecimal;
import java.util.List;

/** One purchase order is created for every supplier company in the cart. */
public record CheckoutResponse(List<OrderResponse> orders, BigDecimal grandTotal, String message) { }
