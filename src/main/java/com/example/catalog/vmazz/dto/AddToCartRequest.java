package com.example.catalog.vmazz.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AddToCartRequest(
        @NotNull(message = "Item id is required") Long itemId,
        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.001", message = "Quantity must be greater than zero")
        @Digits(integer = 15, fraction = 3) BigDecimal quantity
) { }
