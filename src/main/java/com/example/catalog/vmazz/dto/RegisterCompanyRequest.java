package com.example.catalog.vmazz.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Creates a company together with its first (ADMIN) user. */
public record RegisterCompanyRequest(
        @NotBlank(message = "Company name is required") @Size(max = 200) String companyName,
        @Pattern(regexp = ValidationRules.GSTIN, message = ValidationRules.GSTIN_MESSAGE) String gstNumber,
        @NotBlank(message = "Company email is required") @Email @Size(max = 150) String companyEmail,
        @Pattern(regexp = ValidationRules.PHONE, message = ValidationRules.PHONE_MESSAGE) String companyPhone,
        @NotNull(message = "Company address is required") @Valid AddressDto address,

        @NotBlank(message = "Admin name is required") @Size(max = 150) String adminFullName,
        @NotBlank(message = "Admin email is required") @Email @Size(max = 150) String adminEmail,
        @Pattern(regexp = ValidationRules.PHONE, message = ValidationRules.PHONE_MESSAGE) String adminMobile,
        @NotBlank(message = "Admin password is required")
        @Pattern(regexp = ValidationRules.PASSWORD, message = ValidationRules.PASSWORD_MESSAGE) String adminPassword
) { }
