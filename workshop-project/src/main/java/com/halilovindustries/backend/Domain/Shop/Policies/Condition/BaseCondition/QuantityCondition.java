package com.halilovindustries.backend.Domain.Shop.Policies.Condition.BaseCondition;

import java.util.HashMap;

import com.halilovindustries.backend.Domain.Shop.Category;
import com.halilovindustries.backend.Domain.Shop.Item;

public class QuantityCondition extends BaseCondition {
    private int minQuantity;
    private int maxQuantity;

    // Constructor with minQuantity and maxQuantity
    public QuantityCondition(int itemId, Category category, int minQuantity, int maxQuantity) {
        super(itemId, category);
        buildQuantityRange(minQuantity, maxQuantity);
    }

    // Constructor with only minQuantity and maxQuantity
    public QuantityCondition(int minQuantity, int maxQuantity) {
        super();
        buildQuantityRange(minQuantity, maxQuantity);
    }

    // Constructor with itemId and minQuantity/maxQuantity
    public QuantityCondition(int itemId, int minQuantity, int maxQuantity) {
        super(itemId);
        buildQuantityRange(minQuantity, maxQuantity);
    }

    // Constructor with category and minQuantity/maxQuantity
    public QuantityCondition(Category category, int minQuantity, int maxQuantity) {
        super(category);
        buildQuantityRange(minQuantity, maxQuantity);
    }

    // Method to build and validate the quantity range
    private void buildQuantityRange(int minQuantity, int maxQuantity) {
        // If both minQuantity and maxQuantity are -1, throw an exception
        if (minQuantity == -1 && maxQuantity == -1) {
            throw new IllegalArgumentException("Both minQuantity and maxQuantity cannot be -1");
        }

        // If both minQuantity and maxQuantity are provided, validate them
        if (minQuantity != -1 && maxQuantity != -1) {
            if (minQuantity < 0 || maxQuantity < 0) {
                throw new IllegalArgumentException("minQuantity and maxQuantity must be at least 0");
            }
            if (maxQuantity < minQuantity) {
                throw new IllegalArgumentException("maxQuantity must be greater than or equal to minQuantity");
            }
            this.minQuantity = minQuantity;
            this.maxQuantity = maxQuantity;
            return;
        }

        // If minQuantity is -1 and maxQuantity is provided
        if (minQuantity == -1) {
            if (maxQuantity < 0) {
                throw new IllegalArgumentException("maxQuantity must be at least 0");
            }
            this.minQuantity = 0;
            this.maxQuantity = maxQuantity;
            return;
        }

        // If maxQuantity is -1 and minQuantity is provided
        if (maxQuantity == -1) {
            if (minQuantity < 0) {
                throw new IllegalArgumentException("minQuantity must be at least 0");
            }
            this.minQuantity = minQuantity;
            this.maxQuantity = Integer.MAX_VALUE; // Set maxQuantity to the maximum possible value
        }
    }

    // Getters for minQuantity and maxQuantity
    public int getMinQuantity() {
        return minQuantity;
    }

    public int getMaxQuantity() {
        return maxQuantity;
    }

    @Override
    public boolean checkItemCondition(HashMap<Item, Integer> allItems) {
        for (Item item : allItems.keySet()) {
            if (item.getId() == getItemId()) {
                int quantity = allItems.get(item);
                return quantity >= minQuantity && quantity <= maxQuantity;
            }
        }
        return false;
    }

    @Override
    public boolean checkCategoryCondition(HashMap<Item, Integer> allItems) {
        int totalQuantity = 0;
        for (Item item : allItems.keySet()) {
            if (item.getCategory().equals(getCategory())) {
                totalQuantity += allItems.get(item);
            }
        }
        return totalQuantity >= minQuantity && totalQuantity <= maxQuantity;
    }

    @Override
    public boolean checkShopCondition(HashMap<Item, Integer> allItems) {
        int totalQuantity = 0;
        for (Item item : allItems.keySet()) {
            totalQuantity += allItems.get(item);
        }
        return totalQuantity >= minQuantity && totalQuantity <= maxQuantity;
    }

    @Override
    public String toString() {
        return String.format("QuantityCondition [minQuantity=%d, maxQuantity=%d]", minQuantity, maxQuantity);
    }
}
