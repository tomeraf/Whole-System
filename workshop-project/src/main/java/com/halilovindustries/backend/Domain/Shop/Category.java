package com.halilovindustries.backend.Domain.Shop;

public enum Category {
    ELECTRONICS,
    CLOTHING,
    FOOD,
    FURNITURE,
    TOYS,
    BOOKS,
    BEAUTY,
    SPORTS,
    AUTOMOTIVE,
    HEALTH,
    GARDEN,
    PET_SUPPLIES,
    OFFICE_SUPPLIES,
    MUSIC,
    MOVIES,
    VIDEO_GAMES;

    public boolean equalsIgnoreCase(String expectedCategory) {
        return this.name().equalsIgnoreCase(expectedCategory);
    }
}
