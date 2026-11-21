package com.example.catalog.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class BomLineResult {

    private final int lineNumber;
    private final String reference;
    private final String requestedDescription;
    private final String requestedMpn;
    private final String requestedManufacturer;
    private final Integer requestedQuantity;

    private final BomMatchStatus matchStatus;
    /** 0-100. EXACT is always 100. */
    private final int confidence;
    private final String reason;

    /** Best match; null when matchStatus is NOT_FOUND. */
    private final BomMatchedProduct match;

    /** null when no quantity was given. */
    private final Boolean stockSufficient;
    private final Integer shortfall;
    /** unit price x quantity, in the product's currency; null when it cannot be computed. */
    private final BigDecimal lineCost;

    /** Runner-up candidates (or the closest parts when NOT_FOUND). */
    private final List<BomCandidate> otherCandidates;
    /** Substitutes for the matched part when stock is short or it is inactive. */
    private final List<AlternateProductResponse> alternates;
}
