package com.example.catalog.vmazz.dto;

/** Shared validation patterns. */
public final class ValidationRules {

    private ValidationRules() { }

    public static final String PASSWORD = "^(?=.*[A-Za-z])(?=.*\\d).{8,64}$";
    public static final String PASSWORD_MESSAGE =
            "Password must be 8-64 characters and contain at least one letter and one digit";

    public static final String PHONE = "^[0-9+\\- ]{8,20}$";
    public static final String PHONE_MESSAGE = "Phone number must be 8-20 digits (may include +, - and spaces)";

    public static final String GSTIN = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$";
    public static final String GSTIN_MESSAGE = "Invalid GSTIN";
}
