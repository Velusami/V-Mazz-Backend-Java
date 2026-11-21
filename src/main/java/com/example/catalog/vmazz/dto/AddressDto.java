package com.example.catalog.vmazz.dto;

import com.example.catalog.vmazz.domain.Address;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddressDto(
        @NotBlank(message = "Address line 1 is required") @Size(max = 200) String addressLine1,
        @Size(max = 200) String addressLine2,
        @NotBlank(message = "City is required") @Size(max = 100) String city,
        @NotBlank(message = "State is required") @Size(max = 100) String state,
        @NotBlank(message = "Postal code is required") @Size(min = 4, max = 10) String postalCode,
        @NotBlank(message = "Country is required") @Size(max = 100) String country
) {

    public static AddressDto from(Address a) {
        if (a == null) {
            return null;
        }
        return new AddressDto(a.getAddressLine1(), a.getAddressLine2(), a.getCity(),
                a.getState(), a.getPostalCode(), a.getCountry());
    }

    public Address toEntity() {
        return Address.builder()
                .addressLine1(trim(addressLine1))
                .addressLine2(trim(addressLine2))
                .city(trim(city))
                .state(trim(state))
                .postalCode(trim(postalCode))
                .country(trim(country))
                .build();
    }

    private static String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
