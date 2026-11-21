package com.example.catalog.dto;

/** A runner-up (or closest) catalog part for a BOM line. */
public record BomCandidate(BomMatchedProduct product, int confidence) { }
