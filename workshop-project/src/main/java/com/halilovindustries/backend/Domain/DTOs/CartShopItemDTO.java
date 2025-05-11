package com.halilovindustries.backend.Domain.DTOs;

public class CartShopItemDTO {
    public int cartID; // User ID
    public int shopID;
    public int itemID;
    public CartShopItemDTO(int cartID, int shopID, int itemID) {
        this.cartID = cartID;
        this.shopID = shopID;
        this.itemID = itemID;
    }
}
