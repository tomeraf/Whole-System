package com.halilovindustries.backend.Domain.Shop.Policies.Discount.BaseDiscount;

import java.util.HashMap;

import com.halilovindustries.backend.Domain.Shop.Category;
import com.halilovindustries.backend.Domain.Shop.Item;
import com.halilovindustries.backend.Domain.Shop.Policies.Discount.Discount;

public class BaseDiscount extends Discount {
    private int percentage;
    private int itemId;
    private Category category;

        public BaseDiscount(int percentage,Category category,int itemId) {
        this(percentage);
        if(itemId != -1&& category != null){
            throw new IllegalArgumentException("Cannot have both itemId and category");
        }
        this.category = category;
        this.itemId = itemId;

    }
    public BaseDiscount(int percentage,int itemId) {
        this.percentage = percentage;
        this.itemId = itemId;
        this.category = null;
    }
    public BaseDiscount(int percentage,Category category) {
        this.itemId = -1;
        this.category = category;
    }
    public BaseDiscount(int percentage) {
        this.percentage = percentage;
        this.itemId = -1;
        this.category = null;
    }
    public int getItemId() {
        return itemId;
    }
    public Category getCategory() {
        return category;
    }
    public int getPercentage() {
        return percentage;
    }
    public String getType(){
        if (itemId != -1) {
            return "Item";
        } else if (category != null) {
            return "Category";
        } else {
            return "Shop";
        }
    }
    public double calculateDiscount(HashMap<Item,Integer> allItems){
        switch (getType()) {
            case "Item":
                return calculateItemDiscount(allItems);
            case "Category":
                return calculateCategoryDiscount(allItems);
            case "Shop":
                return calculateShopDiscount(allItems);
            default:
                return 0;
        }
    }
    
    protected double calculateShopDiscount(HashMap<Item, Integer> allItems) {
        double totalDiscount = 0;
        for (Item item : allItems.keySet()) {
            totalDiscount += item.getPrice() * getPercentage() / 100.0 * allItems.get(item);
        }
        return totalDiscount;
    }
    protected double calculateCategoryDiscount(HashMap<Item, Integer> allItems) {
        double totalDiscount = 0;
        for (Item item : allItems.keySet()) {
            if (item.getCategory().equals(getCategory())) {
                totalDiscount += item.getPrice() * getPercentage() / 100.0 * allItems.get(item);
            }
        }
        return totalDiscount;
    }
    protected double calculateItemDiscount(HashMap<Item, Integer> allItems) {
        double totalDiscount = 0;
        for (Item item : allItems.keySet()) {
            if (item.getId() == getItemId()) {
                totalDiscount += item.getPrice() * getPercentage() / 100.0 * allItems.get(item);
            }
        }
        return totalDiscount;
    }

}
