package com.halilovindustries.backend.Domain;

public class Item {
    private String name;
    private Category category;
    private double price;
    private int quantity;
    private String description;
    private int shopId;
    private int id;
    private double rating;
    private int numOfOrders;
    

    public Item(String name,Category category, double price, int shopId, int id, String description) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.shopId = shopId;
        this.id = id;
        this.description=description;
        this.quantity = 0;
        this.rating = 0.0;
        this.numOfOrders = 0;
    }

    public void updateRating(double newRating) {
        if (newRating < 0 || newRating > 5) {
            throw new IllegalArgumentException("Rating must be between 0 and 5.");
        }
        if (numOfOrders == 0) {
            throw new IllegalStateException("No orders have been made yet. Cannot update rating.");
        } 
        this.rating = (this.rating * numOfOrders + newRating) / (numOfOrders);
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
    public int getShopId() {
        return shopId;
    }
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
        return rating;
    }
    public void setRating(double rating) {
        this.rating = rating;
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
                ", shopId=" + shopId +
                ", id=" + id +
                ", rating=" + rating +
                ", numOfOrders=" + numOfOrders +
                '}';
    }
}

