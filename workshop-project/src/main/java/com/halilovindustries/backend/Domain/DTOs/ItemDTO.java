package com.halilovindustries.backend.Domain.DTOs;

import com.halilovindustries.backend.Domain.Shop.Category;
import jakarta.persistence.*;


@Entity
@Table(name = "items")
public class ItemDTO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String name;

    @Enumerated(EnumType.STRING)
    private Category category;

    private String description;

    // for single item
    private double price;

    private int shopId;

    private int itemID;

    // quantity in the basket
    private int quantity;

    private double rating;

    private int numOfOrders;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    public ItemDTO(String name,Category category, double price, int shopId, int itemID, int quantity, double rating, String description, int numOfOrders) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.shopId = shopId;
        this.itemID = itemID;
        this.quantity = quantity;
        this.rating = rating;
        this.description = description;
        this.numOfOrders = numOfOrders;
        order = null;
    }

    public ItemDTO() {

    }

    // Getters
    public String getName() { return name; }
    public Category getCategory() { return category; }
    public double getPrice() { return price; }
    public int getShopId() { return shopId; }
    public int getItemID() { return itemID; }
    public int getQuantity() { return quantity; }
    public double getRating() { return rating; }
    public String getDescription() { return description; }
    public int getNumOfOrders() { return numOfOrders; }
    public Order getOrder() { return order; }

    // Setters
    public void setOrder(Order order) { this.order = order; }
    public void setName(String name) { this.name = name; }
    public void setCategory(Category category) { this.category = category; }
    public void setPrice(double price) { this.price = price; }
    public void setShopId(int shopId) { this.shopId = shopId; }
    public void setItemID(int itemID) { this.itemID = itemID; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setRating(double rating) { this.rating = rating; }
    public void setDescription(String description) { this.description = description; }
    public void setNumOfOrders(int numOfOrders) { this.numOfOrders = numOfOrders; }
}
