package com.example.catalog.vmazz.controller;

import com.example.catalog.vmazz.dto.AuthResponse;
import com.example.catalog.vmazz.dto.LoginRequest;
import com.example.catalog.vmazz.dto.RegisterCompanyRequest;
import com.example.catalog.vmazz.dto.RegisterCustomerRequest;
import com.example.catalog.vmazz.dto.UserResponse;
import com.example.catalog.vmazz.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "1. Auth", description = "Sign in and register (customers and supplier companies)")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register-customer")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Customer sign up on the portal (returns a token)")
    public AuthResponse registerCustomer(@Valid @RequestBody RegisterCustomerRequest request) {
        return authService.registerCustomer(request);
    }

    @PostMapping("/register-company")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a supplier company with its first admin user (returns a token)")
    public AuthResponse registerCompany(@Valid @RequestBody RegisterCompanyRequest request) {
        return authService.registerCompany(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Sign in with email and password (returns a token)")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    @Operation(summary = "The logged-in user")
    public UserResponse me() {
        return authService.me();
    }
}
