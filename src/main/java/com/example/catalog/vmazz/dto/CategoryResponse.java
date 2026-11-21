package com.example.catalog.vmazz.dto;

import com.example.catalog.vmazz.domain.ItemCategory;

public record CategoryResponse(ItemCategory category, String displayName, long itemCount) { }
