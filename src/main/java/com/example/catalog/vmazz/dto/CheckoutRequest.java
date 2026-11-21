package com.example.catalog.vmazz.dto;

import com.example.catalog.vmazz.domain.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CheckoutRequest(
        @NotBlank(message = "Contact name is required") @Size(max = 150) String contactName,
        @NotBlank(message = "Contact phone is required")
        @Pattern(regexp = ValidationRules.PHONE, message = ValidationRules.PHONE_MESSAGE) String contactPhone,
        @NotNull(message = "Shipping address is required") @Valid AddressDto shippingAddress,
        /** Optional. When missing, the shipping address is used for billing. */
        @Valid AddressDto billingAddress,
        @NotNull(message = "Payment method is required") PaymentMethod paymentMethod,
        @FutureOrPresent(message = "Required delivery date cannot be in the past") LocalDate requiredDeliveryDate,
        @Size(max = 50) String buyerReference,
        @Size(max = 500) String notes
) { }
