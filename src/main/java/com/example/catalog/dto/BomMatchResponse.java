package com.example.catalog.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class BomMatchResponse {

    private final int totalLines;
    private final int exactMatches;
    private final int matched;
    private final int needsReview;
    private final int notFound;

    /** Cost of the EXACT and MATCHED lines only (REVIEW / NOT_FOUND are not included), per currency. */
    private final Map<String, BigDecimal> estimatedTotalByCurrency;

    private final List<BomLineResult> lines;
    private final String note;
}
