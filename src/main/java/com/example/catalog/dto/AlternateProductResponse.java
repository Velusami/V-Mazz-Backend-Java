package com.example.catalog.dto;

import java.math.BigDecimal;

/**
 * One suggested alternate part.
 *
 * @param similarity             Elasticsearch relevance score (higher = closer match to the source part)
 * @param sameManufacturer       true when the alternate is from the same manufacturer as the source
 * @param priceDifferencePercent alternate price vs source price, in %; null when currencies differ or are unknown
 */
public record AlternateProductResponse(
        String mpn,
        String itemName,
        String manufacturer,
        BigDecimal price,
        String currency,
        Integer quantity,
        String status,
        String category,
        String packageType,
        String specifications,
        double similarity,
        boolean sameManufacturer,
        Double priceDifferencePercent
) { }
