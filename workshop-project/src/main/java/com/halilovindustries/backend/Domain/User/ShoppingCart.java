package com.halilovindustries.backend.Domain.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.halilovindustries.backend.Domain.DTOs.*;

public class ShoppingCart {
    private HashMap<Integer,ShoppingBasket> baskets;//<shopId, basket>
    private int cartID;


    public ShoppingCart(HashMap<Integer,ShoppingBasket> baskets, int cartID) {
        this.baskets = baskets;
        this.cartID = cartID;
    }


    public ShoppingCart(int cartID) {
        this.cartID = cartID;
        this.baskets = new HashMap();
    }


    public int getCartID() {
        return cartID;
    }


    // Use case #2.3: Add item to cart
    public void addItem(int shopID,int itemID,int quantity) {
        // Check if the basket for the given shopID already exists
        ShoppingBasket basket = baskets.get(shopID);
        if (basket == null) {
            // If not, create a new basket for the shopID
            basket = new ShoppingBasket(shopID);
            baskets.put(shopID, basket);
        }

        // Add the item to the basket
        basket.addItem(itemID, quantity);
    }


    // Use case #2.4.a: Check cart content
    // Use case #2.5: Buy items in cart - get all items in cart
    public HashMap<Integer,HashMap<Integer,Integer>> getItems() {//<shopID, <itemID, quantity>>
        HashMap<Integer,HashMap<Integer,Integer>> items = new HashMap<>();

        // Iterate through each basket
        for (Map.Entry<Integer, ShoppingBasket> entry : baskets.entrySet()) {
            int shopID = entry.getKey();
            ShoppingBasket basket = entry.getValue();

            // Get the items in the basket
            HashMap<Integer,Integer> itemList = basket.getItems();

            // Add the items to the result map
            items.put(shopID, itemList);
        }

        return items;
    }


    // Use case #2.4.b: Change cart content
    // map<Integer, Integer> items: shopID, List<ItemID>
    public boolean deleteItem(int shopID, int itemID) {
        // Check if the basket for the given shopID exists
        ShoppingBasket basket = baskets.get(shopID);
        if (basket != null) {
            // Remove the item from the basket
            return basket.removeItem(itemID);
        }
        return false; // Basket not found, nothing removed
    }


    // Use case #2.5: Buy items in cart - after confirmation, the items are removed from the cart
    public void clearCart() {
        // Clear all items from all baskets
        for (ShoppingBasket basket : baskets.values()) {
            basket.clearBasket(); // Clear the basket
        }
    }

    public List<ShoppingBasket> getBaskets() {
        return baskets.values().stream().toList();
    }
    public List<Integer> getShopIDs() {
        return new ArrayList<>(baskets.keySet());
    }
}


