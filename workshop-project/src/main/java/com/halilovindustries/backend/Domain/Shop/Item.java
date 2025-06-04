package com.halilovindustries.backend.Domain.Shop;


import java.util.HashMap;
import java.util.Map;
import jakarta.persistence.*;

@Entity
public class Item {
    @Id
    private int id;

    private String name;

    @Enumerated(EnumType.STRING)
    private Category category;

    private double price;
    private int quantity;
    private String description;

    // private int shopId;
    
    private int numOfOrders;

    @ElementCollection
    @CollectionTable(name = "item_ratings", joinColumns = @JoinColumn(name = "item_id"))
    @MapKeyColumn(name = "rater_id")
    @Column(name = "rating")
    private Map<Integer, Double> ratedIds = new HashMap<>();

    public Item() {} // JPA requires a no-args constructor

    public Item(int id,String name, Category category, double price, int shopId, String description) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
        // this.shopId = shopId;
        this.description = description;
        this.quantity = 0;
        this.numOfOrders = 0;
        this.ratedIds = new HashMap<>();
    }

    public void updateRating(int id, double newRating) {
        if (newRating < 0 || newRating > 5) {
            throw new IllegalArgumentException("Rating must be between 0 and 5.");
        }
        if (numOfOrders == 0) {
            throw new IllegalStateException("No orders have been made yet. Cannot update rating.");
        } 
        ratedIds.put(id, newRating);
    }
    public void updateQuantity(int quantity) {
        if (quantity >= 0) {
            this.quantity = quantity;
        } else {
            throw new IllegalArgumentException("quantity cannot be negative");
        }
    }
    public boolean quantityCheck(int quantity) {
        if (this.quantity >= quantity) {
            return true;
        } else {
            return false;
        }
    }
    public void buyItem(int quantity) {
        if(this.quantity >= quantity) {
            this.quantity -= quantity;
            this.numOfOrders += 1;
        }
        else
            throw new IllegalArgumentException("not enough quantity.");
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public int getQuantity() {
        return quantity;
    }
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
    // public int getShopId() {
    //     return shopId;
    // }
    public double getPrice() {
        return price;
    }
    public void setPrice(double price){
        this.price=price;
    }
    public int getId() {
        return id;
    }

    public double getRating() {
        double rating = 0.0;
        for (double r : ratedIds.values()) {
            rating += r;
        }
        return rating / ratedIds.size();
    }
    public int getNumOfOrders(){
        return numOfOrders;
    }
    public void setNumOfOrders(int numOfOrders) {
        this.numOfOrders = numOfOrders;
    }
    public Category getCategory() {
        return category;
    }
    public void setCategory(Category category) {
        this.category = category;
    }
    public String getDescription(){
        return description;
    }
    public void setDescription(String description){
        this.description = description;
    }

    public String toString() {
        return "Item{" +
                "name='" + name + '\'' +
                ", category=" + category +
                ", price=" + price +
                ", quantity=" + quantity +
                // ", shopId=" + shopId +
                ", id=" + id +
                ", rating=" + getRating() +
                ", numOfOrders=" + numOfOrders +
                '}';
    }
}

