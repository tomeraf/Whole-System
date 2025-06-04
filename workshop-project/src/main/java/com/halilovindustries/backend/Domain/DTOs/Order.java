package com.halilovindustries.backend.Domain.DTOs;

import java.util.HashMap;
import java.util.List;

public class Order {
    private final int orderID;
    private final int userId;
    private final double totalPrice;
    private final HashMap<Integer, List<ItemDTO>> items; // <Integer, List<ItemDTO> = shopId, List<ItemDTO> = items in the shop

    public Order(int orderID, int userId, double totalPrice, HashMap<Integer, List<ItemDTO>> items) {
        this.orderID = orderID;
        this.totalPrice = totalPrice;
        this.items = items;
        this.userId = userId;
    }

    public List<ItemDTO> getItems() {
        return items.values().stream().flatMap(List::stream).toList(); // Flatten the list of lists into a single list
    }

    public List<ItemDTO> getShopItems(int shopId) {
        return items.get(shopId); // Return the list of items for the specified shop ID or null if not found
    }
    
    public int getId() {
        return orderID;
    }
    public int getUserID() {
        return userId;
    }

    public double getTotalPrice() {
        return totalPrice;
    }
    public String getOrderDetails() {
        StringBuilder details = new StringBuilder("Order ID: " + orderID + "\nUserId: " + userId + "\nTotal Price: " + totalPrice + "\nItems:\n");
        for (int shopId : items.keySet()) {
            details.append("Shop ID: ").append(shopId).append("\nItems:\n");
            for (ItemDTO item : items.get(shopId)) {
                details.append(item.toString()).append("\n");
            }
        }
        return details.toString();
    }
}