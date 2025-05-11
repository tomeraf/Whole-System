package com.halilovindustries.backend.Domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.halilovindustries.backend.Domain.DTOs.ItemDTO;

public class ShoppingCart {
    private List<ShoppingBasket> baskets;
    private int cartID;


    public ShoppingCart(List<ShoppingBasket> baskets, int cartID) {
        this.baskets = baskets;
        this.cartID = cartID;
    }


    public ShoppingCart(int cartID) {
        this.cartID = cartID;
        this.baskets = new java.util.ArrayList<>();
    }


    public int getCartID() {
        return cartID;
    }


    // Use case #2.3: Add item to cart
    public boolean addItems(List<ItemDTO> items) {
        // Check if all items are not in the baskets (cant add any item if even one already in the cart)
        for (ItemDTO item : items) {
            for (ShoppingBasket basket : baskets) {
                if (basket.isItemIn(item.getItemID()) && basket.getShopID() == item.getShopId())
                return false; // there is item that already exists, nothing added
            }
        }
        
        List<Integer> shopsIds = new ArrayList<>(); // List to store baskets ids
        for (ShoppingBasket basket : baskets) {
            shopsIds.add(basket.getShopID()); // Add the shopID to the list
        }

        // Add items to all baskets
        boolean basketFound = false;
        for (ItemDTO item : items) {
            for (ShoppingBasket basket : baskets) {
                if (basket.getShopID() == item.getShopId()) {
                    if (!basket.addItem(item)) {
                        return false; // Item already exists, not added
                    }
                    else {
                        basketFound = true; // Item added successfully
                        break; // Item added successfully, exit the loop
                    }
                } 
            }
            if (!basketFound) {
                // If the basket is not found, create a new one and add the item
                addBasket(item.getShopId()); // Create a new basket for the shopID
                ShoppingBasket newBasket = baskets.get(baskets.size() - 1); // Get the newly created basket
                newBasket.addItem(item); // Add the item to the new basket
            }
            basketFound = false; // Reset the flag for the next item
        }
        return true;
    }


    // Use case #2.4.a: Check cart content
    // Use case #2.5: Buy items in cart - get all items in cart
    // map<Integer, Integer> items: itemID, basketID
    public List<ItemDTO> getItems() {
        List<ItemDTO> items = new ArrayList<>(); // List to store items

        // Iterate through each basket and add items to the List
        for (int i = 0; i < baskets.size(); i++) {
            ShoppingBasket basket = baskets.get(i);
            for (ItemDTO item : basket.getItems()) {
                items.add(item);
            }
        }
        return items;
    }


    // Use case #2.4.b: Change cart content
    // map<Integer, Integer> items: shopID, List<ItemID>
    public boolean deleteItems(HashMap<Integer, List<Integer>> items) {
        // Check if all items exist in the relevant baskets
        for (Map.Entry<Integer, List<Integer>> entry : items.entrySet()) {
            int shopID = entry.getKey();
            List<Integer> itemIDs = entry.getValue();

            // Find the basket for the given shopID
            ShoppingBasket basket = null;
            for (ShoppingBasket b : baskets) {
                if (b.getShopID() == shopID) {
                    basket = b;
                    break;
                }
            }

            // If no basket exists for the shopID, return false
            if (basket == null) {
                throw new IllegalArgumentException("Error: no basket exists for the shopID:" + shopID);
            }

            // Check if all itemIDs exist in the basket
            for (int itemID : itemIDs) {
                if (!basket.isItemIn(itemID)) {
                    throw new IllegalArgumentException("Error: item " + itemID + " not found in basket for shop ID " + shopID);
                }
            }
        }

        // Remove items from the relevant baskets
        for (Map.Entry<Integer, List<Integer>> entry : items.entrySet()) {
            int shopID = entry.getKey();
            List<Integer> itemIDs = entry.getValue();

            // Find the basket for the given shopID
            for (ShoppingBasket basket : baskets) {
                if (basket.getShopID() == shopID) {
                    for (int itemID : itemIDs) {
                        basket.removeItem(itemID);
                    }
                    break;
                }
            }
        }

        return true;
    }


    // Use case #2.5: Buy items in cart - after confirmation, the items are removed from the cart
    public void clearCart() {
        // Clear all items from all baskets
        for (ShoppingBasket basket : baskets) {
            basket.clearBasket(); // Clear the basket
        }
    }
    

    private void addBasket(int shopID) {
        ShoppingBasket basket = new ShoppingBasket(shopID);
        baskets.add(basket);
    }

    public List<ShoppingBasket> getBaskets()
    {
        return baskets;
    }
}


