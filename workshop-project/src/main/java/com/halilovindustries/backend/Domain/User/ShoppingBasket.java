package com.halilovindustries.backend.Domain.User;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.halilovindustries.backend.Domain.DTOs.ItemDTO;

public class ShoppingBasket {
    private int shopID;
    private HashMap<Integer,Integer> items; //<itemID, Quantity> 

    public ShoppingBasket(int shopID) {
        this.shopID = shopID;
        this.items = new HashMap<Integer,Integer>();
    }

    public int getShopID() {
        return shopID;
    }

    public HashMap<Integer,Integer> getItems() {
        return items;
    }

    public void addItem(int itemID, int quantity) {
        if(quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }
        if(items.containsKey(itemID)) {
            items.put(itemID, items.get(itemID) + quantity); // Increase quantity if item already exists
        } else {
            items.put(itemID, quantity); // Add new item with its quantity
        }
    }


    public boolean removeItem(int itemID) {
        // Check if the itemID is in the list of items
        if (items.containsKey(itemID)) {
            items.remove(itemID); // Remove the item from the basket
            return true; // Item removed successfully
        }
        return false; // Item not found, nothing removed
    }


    public void clearBasket() {
        items.clear(); // Clear all items from the basket
    }
}
