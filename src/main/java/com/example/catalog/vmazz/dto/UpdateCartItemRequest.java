package com.example.catalog.vmazz.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** The new quantity for the cart line. 0 removes the line. */
public record UpdateCartItemRequest(
        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0", message = "Quantity cannot be negative")
        @Digits(integer = 15, fraction = 3) BigDecimal quantity
) { }
