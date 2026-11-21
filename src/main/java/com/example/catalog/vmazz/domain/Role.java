package com.example.catalog.vmazz.domain;

public enum Role {
    /** Company administrator: manages company profile, sub users, items and orders. */
    ADMIN,
    /** Company sub user: manages items and orders, cannot manage users or delete items. */
    SUB_ADMIN,
    /** Buyer who registered on the portal. */
    CUSTOMER
}
