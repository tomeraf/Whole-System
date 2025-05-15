package com.halilovindustries.backend.Domain.DTOs;

import com.halilovindustries.backend.Domain.Shop.Category;
import com.halilovindustries.backend.Domain.Shop.Policies.Condition.ConditionLimits;
import com.halilovindustries.backend.Domain.Shop.Policies.Condition.ConditionType;


public class ConditionDTO {
    private ConditionType condition_type;
    private int itemId;
    private Category category;
    private ConditionLimits conditionLimits;
    private int minPrice;
    private int maxPrice;
    private int minQuantity;
    private int maxQuantity;
    private ConditionLimits conditionLimits2;
    private int itemId2;
    private int minPrice2;
    private int maxPrice2;
    private int minQuantity2;
    private int maxQuantity2;
    private Category category2;


    public ConditionDTO(ConditionType condition_type,int itemId,Category category,ConditionLimits conditionLimits,int minPrice,int maxPrice,int minQuantity,int maxQuantity,ConditionLimits conditionLimits2,int itemId2,int minPrice2,int maxPrice2,int minQuantity2,int maxQuantity2,Category category2) {
        this.condition_type = condition_type;
        this.itemId = itemId;
        this.category = category;
        this.conditionLimits = conditionLimits;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.minQuantity = minQuantity;
        this.maxQuantity = maxQuantity;
        this.conditionLimits2 = conditionLimits2;
        this.itemId2 = itemId2;
        this.minPrice2 = minPrice2;
        this.maxPrice2 = maxPrice2;
        this.minQuantity2 = minQuantity2;
        this.maxQuantity2 = maxQuantity2;
        this.category2 = category2;
    }

    public ConditionDTO(int itemId, Category category, ConditionLimits conditionLimits, int minPrice, int maxPrice, int minQuantity, int maxQuantity) {
        this.condition_type = ConditionType.BASE;
        this.itemId = itemId;
        this.category = category;
        this.conditionLimits = conditionLimits;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.minQuantity = minQuantity;
        this.maxQuantity = maxQuantity;
        this.conditionLimits2 = null;
        this.itemId2 = -1;
        this.minPrice2 = -1;
        this.maxPrice2 = -1;
        this.minQuantity2 = -1;
        this.maxQuantity2 = -1;
        this.category2 = null;
    }

    public ConditionType getConditionType() {
        return condition_type;
    }

    public int getItemId() {
        return itemId;
    }

    public Category getCategory() {
        return category;
    }

    public ConditionLimits getConditionLimits() {
        return conditionLimits;
    }

    public int getMinPrice() {
        return minPrice;
    }

    public int getMaxPrice() {
        return maxPrice;
    }

    public int getMinQuantity() {
        return minQuantity;
    }

    public int getMaxQuantity() {
        return maxQuantity;
    }

    public ConditionLimits getConditionLimits2() {
        return conditionLimits2;
    }

    public int getItemId2() {
        return itemId2;
    }

    public int getMinPrice2() {
        return minPrice2;
    }

    public int getMaxPrice2() {
        return maxPrice2;
    }

    public int getMinQuantity2() {
        return minQuantity2;
    }

    public int getMaxQuantity2() {
        return maxQuantity2;
    }

    public Category getCategory2() {
        return category2;
    }
}
