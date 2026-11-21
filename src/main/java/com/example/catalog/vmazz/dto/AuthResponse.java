package com.example.catalog.vmazz.dto;

public record AuthResponse(String accessToken, String tokenType, long expiresInSeconds, UserResponse user) { }
