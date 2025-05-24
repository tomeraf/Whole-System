package com.halilovindustries.backend.Domain.DTOs;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int orderID;
    private int userId;
    private double totalPrice;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemDTO> items = new ArrayList<>();

    public Order(){}

    public Order(int userId, double totalPrice, List<ItemDTO> items) {
        this.userId = userId;
        this.totalPrice = totalPrice;
        for (ItemDTO item : items) {
            item.setOrder(this);
        }
    }

    public List<ItemDTO> getShopItems(int shopId) {
        List<ItemDTO> result = new ArrayList<>();
        for (ItemDTO item : items) {
            if (item.getShopId() == shopId) {
                result.add(item);
            }
        }
        return result;
    }

    public String getOrderDetails() {
        StringBuilder details = new StringBuilder(
                "Order ID: " + orderID +
                        "\nUserId: " + userId +
                        "\nTotal Price: " + totalPrice +
                        "\nItems:\n"
        );

        // Group items by shopId
        Map<Integer, List<ItemDTO>> itemsByShop = new HashMap<>();
        for (ItemDTO item : items) {
            itemsByShop.computeIfAbsent(item.getShopId(), k -> new ArrayList<>()).add(item);
        }

        // Append details for each shop
        for (int shopId : itemsByShop.keySet()) {
            details.append("Shop ID: ").append(shopId).append("\nItems:\n");
            for (ItemDTO item : itemsByShop.get(shopId)) {
                details.append(item.toString()).append("\n");
            }
        }

        return details.toString();
    }


    public Integer getOrderID() {
        return orderID;
    }

    public Integer getUserId() {
        return userId;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public List<ItemDTO> getItems() {
        return items;
    }

    public void setId(Integer id) {
        this.orderID = id;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public void setItems(List<ItemDTO> items) {
        this.items = items;
        for (ItemDTO item : items) {
            item.setOrder(this);
        }
    }
}


