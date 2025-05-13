package com.halilovindustries.backend.Domain.Shop.Purchase;

public enum PurchaseType {
    IMMEDIATE,
    BID,
    AUCTION;

    public static PurchaseType fromString(String type) {
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Type cannot be null or empty");
        }
        try {
            return PurchaseType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid PurchaseType: " + type);
        }
    }
}

