package com.example.catalog.vmazz.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateSubUserRequest(
        @NotBlank(message = "Full name is required") @Size(max = 150) String fullName,
        @NotBlank(message = "Email is required") @Email(message = "Enter a valid email") @Size(max = 150) String email,
        @Pattern(regexp = ValidationRules.PHONE, message = ValidationRules.PHONE_MESSAGE) String mobile,
        @NotBlank(message = "Password is required")
        @Pattern(regexp = ValidationRules.PASSWORD, message = ValidationRules.PASSWORD_MESSAGE) String password
) { }
