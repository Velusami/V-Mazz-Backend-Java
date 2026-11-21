package com.example.catalog.vmazz.domain;

public enum ItemCategory {
    FOOD("Food"),
    GROCERIES("Groceries"),
    GOODS("Goods"),
    RAW_MATERIAL("Raw Material");

    private final String displayName;

    ItemCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
