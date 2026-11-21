package com.example.catalog.dto;

public enum BomMatchStatus {
    /** The MPN exists in the catalog and the part is active. */
    EXACT,
    /** Matched by description / specifications with high confidence. */
    MATCHED,
    /** A likely match, but a person should confirm it (or the part is inactive). */
    REVIEW,
    /** Nothing in the catalog matches well enough. */
    NOT_FOUND
}
