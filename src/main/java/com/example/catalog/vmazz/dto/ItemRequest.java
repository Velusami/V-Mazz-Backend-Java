package com.example.catalog.vmazz.dto;

import com.example.catalog.vmazz.domain.ItemCategory;
import com.example.catalog.vmazz.domain.RecordStatus;
import com.example.catalog.vmazz.domain.Uom;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ItemRequest(
        /** Optional on create (generated when blank); ignored on update. */
        @Size(max = 50) String itemCode,
        @NotBlank(message = "Item name is required") @Size(max = 200) String itemName,
        @NotNull(message = "Category is required") ItemCategory category,
        @Size(max = 100) String subCategory,
        @Size(max = 100) String brand,
        @Size(max = 1000) String description,
        @Size(max = 2000) String specification,
        @Size(max = 20) String hsnCode,
        @NotNull(message = "Unit of measure is required") Uom uom,
        @NotNull(message = "Unit price is required")
        @DecimalMin(value = "0.01", message = "Unit price must be greater than zero")
        @Digits(integer = 16, fraction = 2) BigDecimal unitPrice,
        @DecimalMin(value = "0.01", message = "MRP must be greater than zero")
        @Digits(integer = 16, fraction = 2) BigDecimal mrp,
        @Size(min = 3, max = 3, message = "Currency must be a 3-letter code") String currency,
        @DecimalMin(value = "0", message = "Tax percent cannot be negative")
        @DecimalMax(value = "100", message = "Tax percent cannot exceed 100")
        @Digits(integer = 3, fraction = 2) BigDecimal taxPercent,
        @NotNull(message = "Available quantity is required")
        @DecimalMin(value = "0", message = "Available quantity cannot be negative")
        @Digits(integer = 15, fraction = 3) BigDecimal availableQuantity,
        @DecimalMin(value = "0.001", message = "Minimum order quantity must be greater than zero")
        @Digits(integer = 15, fraction = 3) BigDecimal minOrderQuantity,
        @Size(max = 500) String imageUrl,
        RecordStatus status
) { }
