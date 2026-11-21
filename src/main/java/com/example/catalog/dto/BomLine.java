package com.example.catalog.dto;

/**
 * One line of a bill of materials. Give an MPN, a description, or both.
 */
public record BomLine(
        String reference,
        String description,
        String mpn,
        String manufacturer,
        Integer quantity
) { }
