package com.halilovindustries.backend.Domain.DTOs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "Orders")
public class Order {
    @Id
    private int orderID;
    private int userId;
    private double totalPrice;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true,fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id", referencedColumnName = "orderID")
    private List<ItemDTO> items;
    private int paymentId;
    private int shipmentId;

    public Order(int orderID, int userId, double totalPrice, List<ItemDTO> items, int paymentId, int shipmentId) {
        this.orderID = orderID;
        this.totalPrice = totalPrice;
        this.items = items;
        this.userId = userId;
        this.paymentId = paymentId;
        this.shipmentId = shipmentId;
    }
    public Order(){}

    public List<ItemDTO> getItems() {
        return items; // Flatten the list of lists into a single list
    }
    public List<ItemDTO> getItemsByShopId(int shopId) {
        return items.stream().filter(item -> item.getShopId() == shopId).collect(Collectors.toList()); // Return the list of items for the specified shop ID
    }

    // public List<ItemDTO> getShopItems(int shopId) {
    //     return items.stream().filter(item->item.getShopId()==shopId).toList(); // Return the list of items for the specified shop ID or null if not found
    // }
    
    public int getId() {
        return orderID;
    }
    public int getUserID() {
        return userId;
    }

    public double getTotalPrice() {
        return totalPrice;
    }
    
    public int getPaymentId() {
        return paymentId;
    }

    public int getShipmentId() {
        return shipmentId;
    }
    
    // public String getOrderDetails() {
    //     StringBuilder details = new StringBuilder("Order ID: " + orderID + "\nUserId: " + userId + "\nTotal Price: " + totalPrice + "\nItems:\n");
    //     Map<Integer, List<ItemDTO>> itemsByShop = items.stream()
    //     .collect(Collectors.groupingBy(ItemDTO::getShopId));
    //     for (int shopId : itemsByShop.keySet()) {
    //         details.append("Shop ID: ").append(shopId).append("\nItems:\n");
    //         for (ItemDTO item : itemsByShop.get(shopId)) {
    //             details.append(item.toString()).append("\n");
    //         }
    //     }
    //     return details.toString();
    // }
}