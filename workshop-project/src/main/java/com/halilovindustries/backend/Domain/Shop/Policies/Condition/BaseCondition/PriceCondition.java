package com.halilovindustries.backend.Domain.Shop.Policies.Condition.BaseCondition;

import java.util.HashMap;
import com.halilovindustries.backend.Domain.Shop.Category;

import com.halilovindustries.backend.Domain.Shop.Item;
import com.halilovindustries.backend.Domain.Shop.Policies.Condition.ConditionLimits;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("PRICE")
public class PriceCondition extends BaseCondition {
    private int minPrice;
    private int maxPrice;

    public PriceCondition() { // Default constructor for JPA
    }

    public PriceCondition(int minPrice, int maxPrice) {
        super();
        buildPriceRange(minPrice, maxPrice);
    }

    public PriceCondition(int itemID, Category category, int minPrice, int maxPrice) {
        super(itemID, category);
        buildPriceRange(minPrice, maxPrice);
    }

    private void buildPriceRange(int minPrice, int maxPrice) {
        // If both minPrice and maxPrice are -1, throw an exception
        if (minPrice == -1 && maxPrice == -1) {
            throw new IllegalArgumentException("Both minPrice and maxPrice cannot be -1");
        }

        // If both minPrice and maxPrice are provided, validate them
        if (minPrice != -1 && maxPrice != -1) {
            if (minPrice < 0 || maxPrice < 0) {
                throw new IllegalArgumentException("minPrice and maxPrice must be at least 0");
            }
            if (maxPrice < minPrice) {
                throw new IllegalArgumentException("maxPrice must be greater than or equal to minPrice");
            }
            this.minPrice = minPrice;
            this.maxPrice = maxPrice;
            return;
        }

        // If minPrice is -1 and maxPrice is provided
        if (minPrice == -1) {
            if (maxPrice < 0) {
                throw new IllegalArgumentException("maxPrice must be at least 0");
            }
            this.minPrice = 0;
            this.maxPrice = maxPrice;
            return;
        }

        // If maxPrice is -1 and minPrice is provided
        if (maxPrice == -1) {
            if (minPrice < 0) {
                throw new IllegalArgumentException("minPrice must be at least 0");
            }
            this.minPrice = minPrice;
            this.maxPrice = Integer.MAX_VALUE; // Set maxPrice to the maximum possible value
        }
    }

    public int getMinPrice() {
        return minPrice;
    }

    public int getMaxPrice() {
        return maxPrice;
    }

    @Override
    public ConditionLimits getConditionLimits() {
        return ConditionLimits.PRICE;
    }

    @Override
    public boolean checkItemCondition(HashMap<Item, Integer> allItems) {
        for (Item item : allItems.keySet()) {
            if (item.getId() == getItemId()) {
                return item.getPrice() * allItems.get(item) >= minPrice && item.getPrice() * allItems.get(item) <= maxPrice;
            }
        }
        return false;
    }

    @Override
    public boolean checkCategoryCondition(HashMap<Item, Integer> allItems) {
        double totalPrice = 0;
        for (Item item : allItems.keySet()) {
            if (item.getCategory().equals(getCategory())) {
                totalPrice += item.getPrice() * allItems.get(item);
            }
        }
        return totalPrice >= minPrice && totalPrice <= maxPrice;
    }

    @Override
    public boolean checkShopCondition(HashMap<Item, Integer> allItems) {
        double totalPrice = 0;
        for (Item item : allItems.keySet()) {
            totalPrice += item.getPrice() * allItems.get(item);
        }
        return totalPrice >= minPrice && totalPrice <= maxPrice;
    }

    @Override
    public String toString() {
        return String.format("PriceCondition[minPrice=%d, maxPrice=%d]", 
                              minPrice, maxPrice);
    }

}
