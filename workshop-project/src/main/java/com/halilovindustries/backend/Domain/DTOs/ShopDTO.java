package com.halilovindustries.backend.Domain.DTOs;


import java.util.HashMap;

public class ShopDTO {
    private int id;
    private String name;
    private String description;
    private HashMap<Integer, ItemDTO> items; // itemId -> item
    private double rating;
    private int ratingCount;

    public ShopDTO(int id, String name, String description, HashMap<Integer,ItemDTO> items, double rating,int ratingCount) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.items = items;
        this.rating = rating; 
        this.ratingCount = ratingCount;

    }
    public int getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public String getDescription() {
        return description;
    }
    public HashMap<Integer, ItemDTO> getItems() {
        return items;
    }
    public double getRating() {
        return rating;
    }
    public int getRatingCount() {
        return ratingCount;
    }
}
