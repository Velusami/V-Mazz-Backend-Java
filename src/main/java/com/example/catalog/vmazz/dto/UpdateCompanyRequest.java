package com.example.catalog.vmazz.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateCompanyRequest(
        @NotBlank(message = "Company name is required") @Size(max = 200) String companyName,
        @Pattern(regexp = ValidationRules.GSTIN, message = ValidationRules.GSTIN_MESSAGE) String gstNumber,
        @NotBlank(message = "Company email is required") @Email @Size(max = 150) String email,
        @Pattern(regexp = ValidationRules.PHONE, message = ValidationRules.PHONE_MESSAGE) String phone,
        @NotNull(message = "Company address is required") @Valid AddressDto address
) { }
