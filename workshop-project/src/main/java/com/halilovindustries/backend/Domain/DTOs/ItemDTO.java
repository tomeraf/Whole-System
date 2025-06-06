package com.halilovindustries.backend.Domain.DTOs;

import com.halilovindustries.backend.Domain.Shop.Category;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "Order_items")
public class ItemDTO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private int itemId;
    private int shopId;

    private String name;
    private Category category;
    private String description;
    
    // for single item
    private double price;

    
    // quantity in the basket
    private int quantity;
    private double rating;
    private int numOfOrders;

    public ItemDTO(String name,Category category, double price, int shopId, int itemID, int quantity, double rating, String description, int numOfOrders) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.shopId = shopId;
        this.itemId = itemID;
        this.quantity = quantity;
        this.rating = rating;
        this.description = description;
        this.numOfOrders = numOfOrders;
    }
    public ItemDTO() {}
    public String getName() {
        return name;
    }
    public Category getCategory() {
        return category;
    }
    public double getPrice() {
        return price;
    }
    public int getShopId() {
        return shopId;
    }
    public int getItemID() {
        return itemId;
    }
    public int getQuantity() {
        return quantity;
    }
    public double getRating() {
        return rating;
    }
    public String getDescription(){
        return description;
    }
    public int getNumOfOrders() {
        return numOfOrders;
    }
}
