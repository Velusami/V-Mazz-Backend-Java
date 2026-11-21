package com.example.catalog.dto;

import java.math.BigDecimal;
import java.util.List;

public record AlternatesResult(
        String sourceMpn,
        String sourceItemName,
        String sourceManufacturer,
        BigDecimal sourcePrice,
        String sourceCurrency,
        String sourceCategory,
        String sourcePackageType,
        String sourceSpecifications,
        List<AlternateProductResponse> alternates,
        String note
) { }
