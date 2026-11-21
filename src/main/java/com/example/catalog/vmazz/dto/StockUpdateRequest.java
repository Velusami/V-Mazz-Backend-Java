package com.example.catalog.vmazz.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record StockUpdateRequest(
        @NotNull(message = "Available quantity is required")
        @DecimalMin(value = "0", message = "Available quantity cannot be negative")
        @Digits(integer = 15, fraction = 3) BigDecimal availableQuantity
) { }
