package com.halilovindustries.backend.Domain.DTOs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

@Entity
public class Order {
    @Id
    private final int orderID;
    private final int userId;
    private final double totalPrice;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<ItemDTO> items;

    public Order(int orderID, int userId, double totalPrice, List<ItemDTO> items) {
        this.orderID = orderID;
        this.totalPrice = totalPrice;
        this.items = items;
        this.userId = userId;
    }

    public List<ItemDTO> getItems() {
        return items; // Flatten the list of lists into a single list
    }

    public List<ItemDTO> getShopItems(int shopId) {
        return items.stream().filter(item->item.getShopId()==shopId).toList(); // Return the list of items for the specified shop ID or null if not found
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
        Map<Integer, List<ItemDTO>> itemsByShop = items.stream()
        .collect(Collectors.groupingBy(ItemDTO::getShopId));
        for (int shopId : itemsByShop.keySet()) {
            details.append("Shop ID: ").append(shopId).append("\nItems:\n");
            for (ItemDTO item : itemsByShop.get(shopId)) {
                details.append(item.toString()).append("\n");
            }
        }
        return details.toString();
    }
}