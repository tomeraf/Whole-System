package com.halilovindustries.backend.Domain.Shop.Policies.Discount.BaseDiscount;

import java.util.HashMap;

import com.halilovindustries.backend.Domain.Shop.Category;
import com.halilovindustries.backend.Domain.Shop.Item;
import com.halilovindustries.backend.Domain.Shop.Policies.Discount.Discount;
import com.halilovindustries.backend.Domain.Shop.Policies.Discount.DiscountKind;
import com.halilovindustries.backend.Domain.Shop.Policies.Discount.DiscountType;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "base_type", discriminatorType = jakarta.persistence.DiscriminatorType.STRING)
@DiscriminatorValue("BASE")
public class BaseDiscount extends Discount {
    private int percentage;
    private int itemId;
    @Enumerated(EnumType.STRING)
    private Category category;

    public BaseDiscount() {// Default constructor for JPA
    }
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
    public HashMap<Item, Double> getPercentagePerItem(HashMap<Item,Integer> allItems) {
        switch (getType()) {
            case "Item":
                return getItemPercentagePerItem(allItems);
            case "Category":
                return getCategoryPercentagePerItem(allItems);
            case "Shop":
                return getShopPercentagePerItem(allItems);
            default:
                return null;
        }
    }
    
    private HashMap<Item, Double> getItemPercentagePerItem(HashMap<Item,Integer> allItems) {
        HashMap<Item, Double> itemPercentage = new HashMap<>();
        for (Item item : allItems.keySet()) {
            if (item.getId() == getItemId()) {
                itemPercentage.put(item, (double) (100-getPercentage())/100);
            }
        }
        return itemPercentage;
    }
    private HashMap<Item, Double> getCategoryPercentagePerItem(HashMap<Item,Integer> allItems) {
        HashMap<Item, Double> itemPercentage = new HashMap<>();
        for (Item item : allItems.keySet()) {
            if (item.getCategory().equals(getCategory())) {
                itemPercentage.put(item, (double) (100-getPercentage())/100);
            }
        }
        return itemPercentage;
    }
    private HashMap<Item, Double> getShopPercentagePerItem(HashMap<Item,Integer> allItems) {
        HashMap<Item, Double> itemPercentage = new HashMap<>();
        for (Item item : allItems.keySet()) {
            itemPercentage.put(item, (double) (100-getPercentage())/100);
        }
        return itemPercentage;
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
    
    @Override
    public DiscountKind getDiscountKind() {
        return DiscountKind.BASE;
    }
    @Override
    public DiscountType getDiscountType(){
        return DiscountType.BASE;
    }
        @Override
    public int getItemId() {
        return itemId;
    }
    @Override
    public Category getCategory() {
        return category;
    }
    @Override
    public int getPercentage() {
        return percentage;
    }

}
