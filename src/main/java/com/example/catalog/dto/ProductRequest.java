package com.example.catalog.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductRequest {

    @NotBlank(message = "MPN is required")
    private String mpn;

    @NotBlank(message = "Item name is required")
    private String itemName;

    @NotBlank(message = "Manufacturer is required")
    private String manufacturer;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price cannot be negative")
    private BigDecimal price;

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "INR|USD", message = "Currency must be INR or USD")
    private String currency;

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity cannot be negative")
    private Integer quantity;

    // Optional technical data (used for alternate-part matching)
    @Size(max = 100, message = "Category must be at most 100 characters")
    private String category;

    @Size(max = 50, message = "Package type must be at most 50 characters")
    private String packageType;

    @Size(max = 500, message = "Specifications must be at most 500 characters")
    private String specifications;
}
