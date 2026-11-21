package com.example.catalog.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BomMatchRequest(
        @NotEmpty(message = "rows must not be empty")
        @Size(max = 200, message = "A BOM can have at most 200 rows per request")
        List<BomLine> rows,

        /** Suggest alternates when stock is short or the part is inactive. Default true. */
        Boolean includeAlternates
) { }
