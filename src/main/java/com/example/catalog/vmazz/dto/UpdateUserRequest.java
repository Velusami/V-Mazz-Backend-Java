package com.example.catalog.vmazz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @NotBlank(message = "Full name is required") @Size(max = 150) String fullName,
        @Pattern(regexp = ValidationRules.PHONE, message = ValidationRules.PHONE_MESSAGE) String mobile
) { }
